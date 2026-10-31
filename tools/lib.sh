# Shared by backup.sh, install.sh and restore.sh. Source it; do not run it.
set -euo pipefail

PKG="id.adzantv"
DREAM="$PKG/.dream.AdzanDreamService"
APK="app/build/outputs/apk/debug/app-debug.apk"

# Every setting install.sh may change, as "namespace key".
KEYS=(
  "secure screensaver_enabled"
  "secure screensaver_components"
  "secure screensaver_default_component"
  "secure screensaver_activate_on_sleep"
  "secure screensaver_activate_on_dock"
  "system screen_off_timeout"
  "secure sleep_timeout"
)

cd "$(dirname "$0")/.."

die() { echo "ERROR: $*" >&2; exit 1; }

# $1 = adb target, e.g. 192.168.1.20:37451 (wireless) or a USB serial.
set_device() {
  [[ "${1:-}" =~ ^[A-Za-z0-9._:-]+$ ]] || die "usage: $0 <ip:port> ...   (from Settings > Developer options > Wireless debugging)"
  DEV="$1"
  adb -s "$DEV" get-state >/dev/null 2>&1 || die "device $DEV not connected; run: adb connect $DEV"
  # ip:port changes on every re-pair, so snapshots are keyed by the hardware serial
  SERIAL="$(dev shell getprop ro.serialno | tr -d '\r')"
  [[ "$SERIAL" =~ ^[A-Za-z0-9._-]+$ ]] || die "could not read a usable ro.serialno from $DEV"
}

dev() { adb -s "$DEV" "$@"; }

latest_snapshot() { { ls -1d "backups/$SERIAL"/*/ 2>/dev/null || true; } | sort | tail -n 1 | sed 's:/$::'; }

# settings values are passed through the device shell, so only plain tokens are accepted
check_value() { [[ "$1" =~ ^[A-Za-z0-9._/,:{}+-]*$ ]] || die "refusing unexpected setting value: '$1'"; }

get_setting() { dev shell settings get "$1" "$2" | tr -d '\r'; }
