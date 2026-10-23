#!/usr/bin/env python3
"""Use one disposable CI-only certificate for the previous and current test APKs."""
from pathlib import Path
import os, subprocess, sys, zipfile
source = next(Path(sys.argv[1]).glob('*.apk'))
output = Path(sys.argv[2]).resolve()
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools' / '34.0.0'
key = output.parent/'upgrade-test.jks'
subprocess.run(['keytool','-genkeypair','-keystore',str(key),'-storepass','android','-keypass','android','-alias','upgrade-test','-keyalg','RSA','-keysize','2048','-validity','2','-dname','CN=Disposable CI Upgrade Test'], check=True)
def sign(source, output):
    unsigned = output.with_suffix('.unsigned.apk')
    aligned = output.with_suffix('.aligned.apk')
    with zipfile.ZipFile(source) as src, zipfile.ZipFile(unsigned, 'w', zipfile.ZIP_DEFLATED) as dst:
        for item in src.infolist():
            name = item.filename.upper()
            if name.startswith('META-INF/') and (name.endswith(('.SF', '.RSA', '.DSA', '.EC')) or name == 'META-INF/MANIFEST.MF'): continue
            dst.writestr(item, src.read(item.filename))
    subprocess.run([str(tools/'zipalign'), '-f', '4', str(unsigned), str(aligned)], check=True)
    subprocess.run([str(tools/'apksigner'), 'sign', '--ks', str(key), '--ks-pass', 'pass:android', '--ks-key-alias', 'upgrade-test', '--out', str(output), str(aligned)], check=True)
    subprocess.run([str(tools/'apksigner'), 'verify', str(output)], check=True)
    unsigned.unlink(); aligned.unlink()
try:
    sign(source, output)
    current = next(Path('assembly/build/outputs/apk/completeFdroid/debug').glob('*.apk'))
    sign(current, current)
finally:
    key.unlink()
