#!/usr/bin/env bash
# Downloads the Makkah adhan (the "Athan Makkah" voice in Afnon) into app/src/main/res/raw/.
# It comes from https://github.com/abodehq/Athan-MP3, which states no licence, so it is git-ignored
# and never committed. Without it the app plays the bundled CC0 adhan instead.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/app/src/main/res/raw/adhan_makkah.mp3"
[[ -f "$DEST" ]] && { echo "Already there: $DEST"; exit 0; }
curl -fsSL -o "$DEST.part" "https://raw.githubusercontent.com/abodehq/Athan-MP3/master/Sounds/Athan%20Makkah.mp3"
file "$DEST.part" | grep -q "Audio" || { rm -f "$DEST.part"; echo "download is not audio" >&2; exit 1; }
mv "$DEST.part" "$DEST"
echo "Makkah adhan saved to $DEST; rebuild the app to bundle it"
