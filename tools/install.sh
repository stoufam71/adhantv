#!/usr/bin/env bash
# Install Adzan TV, make it the screensaver, and let it show the adhan over other apps.
# Refuses to run without a snapshot of this TV from tools/backup.sh.
# Usage: tools/install.sh <ip:port> [--apk-only]
source "$(dirname "$0")/lib.sh"
set_device "${1:-}"
MODE="${2:-}"
[[ -z "$MODE" || "$MODE" == "--apk-only" ]] || die "unknown option: $MODE"

SNAP="$(latest_snapshot)"
[[ -n "$SNAP" ]] || die "no snapshot for $SERIAL; run tools/backup.sh $DEV first"
[[ -f "$APK" ]] || die "$APK missing; run ./gradlew assembleDebug first"
echo "Snapshot: $SNAP"

# -r keeps app data on reinstall; never -d, and no other package is touched
dev install -r "$APK"
[[ "$MODE" == "--apk-only" ]] && { echo "APK installed (--apk-only)."; exit 0; }

put() {
  echo "settings put $1 $2 $3"
  dev shell settings put "$1" "$2" "$3"
  [[ "$(get_setting "$1" "$2")" == "$3" ]] || die "$1 $2 did not stick"
}
put secure screensaver_components "$DREAM"
put secure screensaver_enabled 1

echo "appops set $PKG SYSTEM_ALERT_WINDOW allow"
dev shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow
dev shell appops get "$PKG" SYSTEM_ALERT_WINDOW | grep -q allow || die "SYSTEM_ALERT_WINDOW grant did not stick"

# a fresh install stays "stopped" (no BOOT_COMPLETED) until launched once;
# first launch also detects the city and arms the first adhan alarm
dev shell am start -n "$PKG/.settings.SettingsActivity" >/dev/null

added="$(comm -13 "$SNAP/packages.txt" <(dev shell pm list packages | tr -d '\r' | sort))"
removed="$(comm -23 "$SNAP/packages.txt" <(dev shell pm list packages | tr -d '\r' | sort))"
echo "Packages added since snapshot: ${added:-none}"
[[ -z "$removed" ]] || die "packages missing since snapshot: $removed"
echo "Installed. Rollback: tools/restore.sh $DEV"
