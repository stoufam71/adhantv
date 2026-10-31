#!/usr/bin/env bash
# Undo tools/install.sh: uninstall Adzan TV and write every snapshot setting back.
# Usage: tools/restore.sh <ip:port> [backups/<serial>/<timestamp>]   (default: newest snapshot)
source "$(dirname "$0")/lib.sh"
set_device "${1:-}"

SNAP="${2:-$(latest_snapshot)}"
SNAP="${SNAP%/}"
[[ -n "$SNAP" && -f "$SNAP/settings.txt" ]] || die "no snapshot found for $SERIAL"
[[ "$SNAP" == backups/"$SERIAL"/* ]] || die "snapshot $SNAP is not from this TV ($SERIAL)"
echo "Restoring from $SNAP"

if dev shell pm list packages | tr -d '\r' | grep -qx "package:$PKG"; then
  echo "uninstall $PKG"
  dev uninstall "$PKG" # also clears its appops grants
fi

while read -r ns key value; do
  check_value "$value"
  if [[ "$value" == "null" ]]; then
    echo "settings delete $ns $key"
    dev shell settings delete "$ns" "$key" >/dev/null
  else
    echo "settings put $ns $key $value"
    dev shell settings put "$ns" "$key" "$value"
  fi
  [[ "$(get_setting "$ns" "$key")" == "$value" ]] || die "$ns $key is not back to '$value'"
done < "$SNAP/settings.txt"

if diff <(dev shell pm list packages | tr -d '\r' | sort) "$SNAP/packages.txt" >/dev/null; then
  echo "Package list matches the snapshot."
else
  echo "WARNING: package list differs from the snapshot (apps installed or updated since):"
  diff <(dev shell pm list packages | tr -d '\r' | sort) "$SNAP/packages.txt" || true
fi
echo "Restore complete."
