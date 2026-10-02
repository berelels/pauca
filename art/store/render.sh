#!/bin/bash
# render.sh: gera as imagens da loja a partir das capturas em shots/ e as coloca onde a
# Play Store (via fastlane) e o F-Droid procuram: fastlane/metadata/android/<idioma>/images.
# Também gera art/screens.png, a faixa de telas do README.
# Precisa do Electron: ELECTRON=/caminho/para/electron ./render.sh  (ou npx electron)
set -e
cd "$(dirname "$0")"
ELECTRON=${ELECTRON:-"npx --yes electron"}
META=../../fastlane/metadata/android
for pair in en:en-US pt:pt-BR es:es-ES; do
  L=${pair%%:*}; LOC=${pair##*:}; TMP=$(mktemp -d)
  $ELECTRON render.js $L "$TMP" 1 2 3 4 5 6 7 feature:1024x500 2> >(grep -v nss_util >&2)
  mkdir -p $META/$LOC/images/phoneScreenshots
  for i in 1 2 3 4 5 6 7; do cp "$TMP/$i.png" $META/$LOC/images/phoneScreenshots/$i.png; done
  cp "$TMP/feature.png" $META/$LOC/images/featureGraphic.png
  cp ../icon.png $META/$LOC/images/icon.png
  rm -rf "$TMP"
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
