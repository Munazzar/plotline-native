#!/bin/sh
# Plotline full build: web copies -> Java -> dex -> APK -> signed APK + web zip in out/.
# Before a release: bump versionCode/versionName in app/AndroidManifest.xml AND app/apktool.yml,
# APP_VER in plotline.html and the CACHE name in web/sw.js. Needs JDK 17+, python3, zip, and tools/
# (sh scripts/get-tools.sh downloads them).
# Signing: with plotline-upload.jks + plotline-keypass.txt in this folder (never commit them) the APK is
# release-signed; without them (CI) it is debug-signed. DEBUGGABLE=1 marks the app debuggable (CI emulator).
set -e
cd "$(dirname "$0")"
python3 ../scripts/bw.py
rm -rf classes && mkdir classes
javac -nowarn -source 8 -target 8 -bootclasspath tools/android-30.jar -encoding UTF-8 -d classes $(find src -name '*.java')
(cd classes && jar cf ../app.jar .)
sh tools/d2j/dex-tools-v2.4/d2j-jar2dex.sh -f -o app/classes.dex app.jar
APP=app
if [ "$DEBUGGABLE" = "1" ]; then
  rm -rf /tmp/plotline-dbg && cp -r app /tmp/plotline-dbg
  sed -i 's/<application /<application android:debuggable="true" /' /tmp/plotline-dbg/AndroidManifest.xml
  APP=/tmp/plotline-dbg
fi
java -jar tools/apktool.jar b "$APP" -o plotline-unsigned.apk
rm -rf signed
if [ -f plotline-upload.jks ] && [ "$DEBUGGABLE" != "1" ]; then
  P="$(cat plotline-keypass.txt)"
  java -jar tools/uber-signer.jar --apks plotline-unsigned.apk --ks plotline-upload.jks --ksAlias plotline --ksPass "$P" --ksKeyPass "$P" --out signed
else
  java -jar tools/uber-signer.jar --apks plotline-unsigned.apk --out signed
fi
V=$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' app/AndroidManifest.xml)
mkdir -p out && rm -f out/Plotline-*.apk out/plotline-web.zip
cp signed/*.apk "out/Plotline-$V.apk"
(cd web && zip -qr ../out/plotline-web.zip .)
echo "Built out/Plotline-$V.apk and out/plotline-web.zip"
