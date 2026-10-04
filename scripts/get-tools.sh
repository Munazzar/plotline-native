#!/bin/sh
# Downloads the Android build tools into apkbuild/tools (all from GitHub).
set -e
T="$(cd "$(dirname "$0")/.." && pwd)/apkbuild/tools"; mkdir -p "$T"; cd "$T"
curl -sSL -o apktool.jar https://github.com/iBotPeaches/Apktool/releases/download/v2.10.0/apktool_2.10.0.jar
curl -sSL -o uber-signer.jar https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar
curl -sSL -o android-30.jar https://raw.githubusercontent.com/Sable/android-platforms/master/android-30/android.jar
curl -sSL -o dex2jar.zip https://github.com/pxb1988/dex2jar/releases/download/v2.4/dex-tools-v2.4.zip
rm -rf d2j && mkdir d2j && unzip -q dex2jar.zip -d d2j && rm dex2jar.zip
chmod +x d2j/dex-tools-v2.4/*.sh
ls -la "$T"
