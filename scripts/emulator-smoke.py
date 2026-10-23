#!/usr/bin/env python3
"""Exercise the delivered APK on a disposable Android emulator; retain logs on failure."""
from pathlib import Path
import hashlib, os, re, subprocess, sys, time, traceback
import xml.etree.ElementTree as ET

package = 'com.oasisfeng.island.fdroid'
activity = package + '/com.oasisfeng.island.MainActivity'
source = Path(sys.argv[1])
apk = next(source.glob('*.apk')) if source.is_dir() else source
out = Path(sys.argv[2]); out.mkdir(parents=True, exist_ok=True)

def adb(*args, timeout=45, check=True):
    p = subprocess.run(['adb', *args], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
    value = p.stdout.decode('utf-8', errors='replace')
    if check and p.returncode: raise RuntimeError(value)
    return value

def alive():
    if not adb('shell', 'pidof', package, check=False).strip():
        raise AssertionError('Application process exited. See logcat.txt for the crash.')

def capture(name):
    adb('shell', 'uiautomator', 'dump', '/sdcard/smoke-window.xml', check=False)
    xml = adb('shell', 'cat', '/sdcard/smoke-window.xml', check=False)
    (out / (name + '.xml')).write_text(xml)
    # Secure windows may be black; the XML and logcat remain diagnostic evidence.
    p = subprocess.run(['adb','exec-out','screencap','-p'], stdout=subprocess.PIPE, timeout=30)
    (out / (name + '.png')).write_bytes(p.stdout)
    return ET.fromstring(xml)

def bounds(node):
    return list(map(int, re.findall(r'\d+', node.attrib['bounds'])))

def tap(x,y):
    adb('shell','input','tap',str(round(x)),str(round(y)))

try:
    (out/'apk-sha256.txt').write_text(hashlib.sha256(apk.read_bytes()).hexdigest()+'\n')
    (out/'device.txt').write_text(adb('shell','getprop'))
    if os.environ.get('EXTRA_PROFILE', '').lower() != 'false':
        creation=adb('shell','pm','create-user','--profileOf','0','--managed','Existing profile')
        (out/'profile-creation.txt').write_text(creation)
        match=re.search(r'id (\d+)',creation)
        assert match, 'Could not create the unrelated profile regression fixture'
        adb('shell','am','start-user','-w',match.group(1))
        (out/'users.txt').write_text(adb('shell','pm','list','users'))
        print('Created unrelated profile '+match.group(1),flush=True)
    previous = os.environ.get('PREVIOUS_APK')
    if previous:
        print(adb('install', previous), flush=True)
        print('Installed previous APK for update compatibility check', flush=True)
    print(adb('install','-r',str(apk)),flush=True)
    if previous: print('PASS: update from previous APK without uninstall', flush=True)
    adb('logcat','-c')
    adb('shell','input','keyevent','KEYCODE_WAKEUP')
    adb('shell','wm','dismiss-keyguard',check=False)
    (out/'launch.txt').write_text(adb('shell','am','start','-W','-n',activity))
    time.sleep(3)
    alive()
    ui = capture('01-launcher')
    board = next((n for n in ui.iter('node') if n.attrib.get('content-desc','').startswith('2048 board.')), None)
    assert board is not None, '2048 board was not displayed after launch'
    print('PASS: startup displays the puzzle',flush=True)
    # Real gameplay must keep the gate locked.
    direction = next(n for n in ui.iter('node') if n.attrib.get('content-desc') == 'Move left')
    x1,y1,x2,y2=bounds(direction);tap((x1+x2)/2,(y1+y2)/2)
    ui=capture('02-gameplay');alive()
    assert any(n.attrib.get('content-desc','').startswith('2048 board.') for n in ui.iter('node'))
    print('PASS: normal move stays in the game',flush=True)
    # Exercise the exact fixed corner sequence on the Canvas board.
    board=next(n for n in ui.iter('node') if n.attrib.get('content-desc','').startswith('2048 board.'))
    x1,y1,x2,y2=bounds(board);size=min(x2-x1,y2-y1)
    left=x1+(x2-x1-size)/2;top=y1+(y2-y1-size)/2
    gap=size*.025;tile=(size-5*gap)/4
    for col,row in [(0,0),(3,0),(3,3),(0,3)]:
        tap(left+gap+col*(tile+gap)+tile/2,top+gap+row*(tile+gap)+tile/2)
    title=next(n for n in ui.iter('node') if n.attrib.get('text') == '2048')
    x1,y1,x2,y2=bounds(title);x=round((x1+x2)/2);y=round((y1+y2)/2)
    adb('shell','input','swipe',str(x),str(y),str(x),str(y),'1000')
    time.sleep(3);alive()
    (out/'unlocked-activities.txt').write_text(adb('shell','dumpsys','activity','activities'))
    assert 'com.oasisfeng.island.setup.SetupActivity' in (out/'unlocked-activities.txt').read_text(), 'Expected setup activity after unlock'
    ui=capture('03-unlocked')
    assert not any(n.attrib.get('content-desc','').startswith('2048 board.') for n in ui.iter('node')), 'Secret sequence did not unlock'
    print('PASS: secret sequence reaches the original application',flush=True)
    # A fresh launcher invocation must return to the game.
    adb('shell','am','force-stop',package)
    adb('shell','am','start','-W','-n',activity)
    time.sleep(2);alive();ui=capture('04-relaunch')
    assert any(n.attrib.get('content-desc','').startswith('2048 board.') for n in ui.iter('node'))
    print('PASS: relaunch restores the puzzle gate',flush=True)
    (out/'result.txt').write_text('PASS: update compatibility, startup, gameplay, secret unlock and relaunch\n')
except Exception:
    (out/'failure.txt').write_text(traceback.format_exc())
    traceback.print_exc()
    sys.exit(1)
finally:
    (out/'logcat.txt').write_text(adb('logcat','-b','all','-d','-v','threadtime',check=False))
    (out/'activities.txt').write_text(adb('shell','dumpsys','activity','activities',check=False))
    (out/'package.txt').write_text(adb('shell','dumpsys','package',package,check=False))
