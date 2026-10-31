#!/usr/bin/env bash
# Snapshot every setting the install touches, plus device info and the package list.
# Usage: tools/backup.sh <ip:port>
source "$(dirname "$0")/lib.sh"
set_device "${1:-}"

OUT="backups/$SERIAL/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"

{
  echo "model=$(dev shell getprop ro.product.model | tr -d '\r')"
  echo "sdk=$(dev shell getprop ro.build.version.sdk | tr -d '\r')"
  echo "fingerprint=$(dev shell getprop ro.build.fingerprint | tr -d '\r')"
  echo "serial=$SERIAL"
} > "$OUT/device.txt"

dev shell pm list packages | tr -d '\r' | sort > "$OUT/packages.txt"

: > "$OUT/settings.txt"
for entry in "${KEYS[@]}"; do
  read -r ns key <<< "$entry"
  value="$(get_setting "$ns" "$key")"
  check_value "$value"
  # "null" means the key was absent; restore deletes it rather than writing the string
  echo "$ns $key $value" >> "$OUT/settings.txt"
done

echo "Snapshot saved to $OUT"
cat "$OUT/device.txt" "$OUT/settings.txt"
echo "$(wc -l < "$OUT/packages.txt" | tr -d ' ') packages recorded"
