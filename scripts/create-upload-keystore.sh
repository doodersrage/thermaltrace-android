#!/usr/bin/env bash
# Creates an upload keystore + keystore.properties for Play App Signing.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

STORE_FILE="${STORE_FILE:-upload-keystore.jks}"
KEY_ALIAS="${KEY_ALIAS:-thermaltrace}"
PROPS_FILE="${PROPS_FILE:-keystore.properties}"

if [[ -f "$STORE_FILE" || -f "$PROPS_FILE" ]]; then
  echo "Refusing to overwrite existing $STORE_FILE / $PROPS_FILE"
  echo "Move them aside first if you really want a new upload key."
  exit 1
fi

STORE_PASSWORD="$(openssl rand -base64 24 | tr -d '/+=' | head -c 24)"
KEY_PASSWORD="$STORE_PASSWORD"

keytool -genkeypair \
  -keystore "$STORE_FILE" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -dname "CN=ThermalTrace, OU=Mobile, O=ThermalTrace, L=Unknown, ST=Unknown, C=US"

cat > "$PROPS_FILE" <<EOF
storeFile=$STORE_FILE
storePassword=$STORE_PASSWORD
keyAlias=$KEY_ALIAS
keyPassword=$KEY_PASSWORD
EOF

chmod 600 "$STORE_FILE" "$PROPS_FILE"

echo
echo "Created:"
echo "  $ROOT/$STORE_FILE"
echo "  $ROOT/$PROPS_FILE"
echo
echo "Back these up somewhere safe (password manager + offline copy)."
echo "Then build: ./gradlew :app:bundleRelease"
