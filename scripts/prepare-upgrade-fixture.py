#!/usr/bin/env python3
"""Re-sign the previous public APK using only the CI runner's disposable debug key."""
from pathlib import Path
import os, subprocess, sys, zipfile
source = next(Path(sys.argv[1]).glob('*.apk'))
output = Path(sys.argv[2]).resolve()
sdk = Path(os.environ['ANDROID_HOME'])
tools = sdk / 'build-tools' / '34.0.0'
unsigned = output.with_suffix('.unsigned.apk')
aligned = output.with_suffix('.aligned.apk')
with zipfile.ZipFile(source) as src, zipfile.ZipFile(unsigned, 'w', zipfile.ZIP_DEFLATED) as dst:
    for item in src.infolist():
        name = item.filename.upper()
        if name.startswith('META-INF/') and (name.endswith(('.SF', '.RSA', '.DSA', '.EC')) or name == 'META-INF/MANIFEST.MF'): continue
        dst.writestr(item, src.read(item.filename))
subprocess.run([str(tools/'zipalign'), '-f', '4', str(unsigned), str(aligned)], check=True)
subprocess.run([str(tools/'apksigner'), 'sign', '--ks', str(Path.home()/'.android/debug.keystore'), '--ks-pass', 'pass:android', '--ks-key-alias', 'androiddebugkey', '--out', str(output), str(aligned)], check=True)
subprocess.run([str(tools/'apksigner'), 'verify', str(output)], check=True)
unsigned.unlink(); aligned.unlink()
