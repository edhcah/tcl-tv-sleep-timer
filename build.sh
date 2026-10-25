#!/bin/sh
# Builds out/sleep.apk. Needs Java 17 and the Android SDK (platform 34, build-tools 34):
#   brew install openjdk@17 && brew install --cask android-commandlinetools
#   sdkmanager "platforms;android-34" "build-tools;34.0.0"
set -e
cd "$(dirname "$0")"
JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}; export PATH="$JAVA_HOME/bin:$PATH"
SDK=${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}
BT=$SDK/build-tools/34.0.0; JAR=$SDK/platforms/android-34/android.jar

rm -rf out; mkdir -p out/classes out/dex
"$BT/aapt2" compile --dir res -o out/res.zip
# Target 22 is deliberate: see the comment in AndroidManifest.xml.
"$BT/aapt2" link -o out/base.apk -I "$JAR" --manifest AndroidManifest.xml --min-sdk-version 21 \
  --target-sdk-version 22 --version-code 1 --version-name 1.0 out/res.zip
javac -nowarn -Xlint:-options -encoding UTF-8 -source 1.8 -target 1.8 \
  -bootclasspath "$JAR:$BT/core-lambda-stubs.jar" -d out/classes $(find src -name "*.java")
"$BT/d8" --min-api 21 --release --lib "$JAR" --output out/dex $(find out/classes -name '*.class')
cp out/base.apk out/unsigned.apk; (cd out/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 out/unsigned.apk out/aligned.apk

# Made on first build. Keep key.jks: updates must be signed with the same key.
KS=key.jks; PASS=${KEYSTORE_PASS:-tvsleep123}
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass "$PASS" -keypass "$PASS" -alias tvsleep \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=tvsleep" >/dev/null 2>&1
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$PASS" --out out/sleep.apk out/aligned.apk
echo "Built out/sleep.apk"
