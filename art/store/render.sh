#!/bin/bash
# render.sh: gera as imagens da loja dos dois apps a partir das capturas em shots/ e as coloca
# onde a Play Store (via fastlane) e o F-Droid procuram: fastlane/metadata/android/<idioma>/images
# (Pauca) e fastlane/metadata/android-lite/<idioma>/images (Pauca Lite).
# Também gera art/screens.png, a faixa de telas do README.
# Precisa do Electron: ELECTRON=/caminho/para/electron ./render.sh  (ou npx electron)
set -e
cd "$(dirname "$0")"
ELECTRON=${ELECTRON:-"npx --yes electron"}
# Pauca: fastlane/metadata/android; Pauca Lite: fastlane/metadata/android-lite
render() {  # render <idioma> <locale> <pasta de metadados> <slides…> -- <banner> <ícone>
  local L=$1 LOC=$2 META=$3; shift 3
  local IDS=() TMP; while [ "$1" != -- ]; do IDS+=("$1"); shift; done; shift
  local FEATURE=$1 ICON=$2; TMP=$(mktemp -d)
  $ELECTRON render.js $L "$TMP" "${IDS[@]}" $FEATURE:1024x500 $ICON:512x512 2> >(grep -v nss_util >&2)
  rm -rf $META/$LOC/images/phoneScreenshots; mkdir -p $META/$LOC/images/phoneScreenshots
  local n=1; for id in "${IDS[@]}"; do cp "$TMP/$id.png" $META/$LOC/images/phoneScreenshots/$n.png; n=$((n + 1)); done
  cp "$TMP/$FEATURE.png" $META/$LOC/images/featureGraphic.png
  cp "$TMP/$ICON.png" $META/$LOC/images/icon.png
  rm -rf "$TMP"
}
for pair in en:en-US pt:pt-BR es:es-ES; do
  L=${pair%%:*}; LOC=${pair##*:}
  render $L $LOC ../../fastlane/metadata/android 1 2 3 4 5 6 7 -- feature icon-full
  render $L $LOC ../../fastlane/metadata/android-lite l1 l2 l3 l4 l5 -- lfeature icon-lite
done
# Faixa do README: as quatro primeiras telas em inglês, lado a lado
python3 - <<'PY'
from PIL import Image
shots = [Image.open(f"../../fastlane/metadata/android/en-US/images/phoneScreenshots/{i}.png").resize((432, 768), Image.LANCZOS) for i in (1, 2, 3, 4)]
gap = 24
strip = Image.new("RGBA", (len(shots) * 432 + (len(shots) - 1) * gap, 768), (0, 0, 0, 0))
for i, im in enumerate(shots):
    strip.paste(im, (i * (432 + gap), 0))
strip.save("../screens.png", optimize=True)
PY
