#!/bin/bash
# capture_all.sh <idioma> [full|lite]   (idiomas: veja a lista LANGS em render.sh): troca o idioma do emulador e captura as telas usadas nos
# slides de uma das versões (o padrão é a completa).
# Precisa do emulador (imagem google_apis, com root) e do build de debug instalado; veja README.md.
set -e
cd "$(dirname "$0")"
L=$1; EDITION=${2:-full}; E="adb -s emulator-5554"
if [ $EDITION = lite ]; then PKG=app.pauca.lite.debug; OUT=shots/lite/$L; else PKG=app.pauca.debug; OUT=shots/$L; fi
export PKG
# Troca o launcher padrão e confere que ele é mesmo o que está na tela (ao alternar entre completo e Lite
# o papel às vezes demora a valer, e a captura sairia do app errado)
home_up() {
  for i in 1 2 3 4 5; do
    $E shell cmd role add-role-holder --user 0 android.app.role.HOME $PKG >/dev/null; sleep 2
    $E shell input keyevent KEYCODE_HOME; sleep 2
    $E shell dumpsys activity activities | grep -q "topResumedActivity.* $PKG/" && return 0
  done
  echo "launcher $PKG não ficou em primeiro plano" >&2; exit 1
}
home_up
# Espanhol latino-americano: o da Espanha mostra "9:42" na barra de status, sem o zero
# Idiomas com região própria no Android; os demais usam o próprio código (zh-CN, fr-CA, pt-PT, es-419…)
case $L in en) LOC=en-US;; pt) LOC=pt-BR;; es) LOC=es-419;; fr) LOC=fr-FR;; ja) LOC=ja-JP;; ko) LOC=ko-KR;; hi) LOC=hi-IN;;
  de) LOC=de-DE;; ru) LOC=ru-RU;; it) LOC=it-IT;; nl) LOC=nl-NL;; bn) LOC=bn-BD;; ta) LOC=ta-IN;; te) LOC=te-IN;; mr) LOC=mr-IN;;
  ca) LOC=ca-ES;; gl) LOC=gl-ES;; eu) LOC=eu-ES;; *) LOC=$L;; esac
if [ "$($E shell getprop persist.sys.locale | tr -d '\r')" != "$LOC" ]; then
  $E shell setprop persist.sys.locale $LOC; $E shell setprop ctl.restart zygote; sleep 15
  until [ "$($E shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done; sleep 5
fi
home_up
# 24 horas em todos os idiomas, para o horário aparecer como 09:42 (antes do demo.sh, que fixa a barra)
$E shell settings put system time_12_24 24
./demo.sh
mkdir -p $OUT
snap() { sleep 3; $E exec-out screencap -p > $OUT/$1.png; }
# Esconde da gaveta os apps instalados que não são esta edição do Pauca (a outra edição, apps de teste)
H=$($E shell pm list packages -3 | tr -d '\r' | sed 's/^package://' | grep -vx "$PKG" | sed 's/$/|UserHandle{0}/' | paste -sd, -)
P="AUTO_SHOW_KEYBOARD=false"; [ -n "$H" ] && P="$P HIDDEN_APPS=set:$H"
# No árabe a tela é espelhada: o botão de ajustes fica à direita
SX=70; [ $L = ar ] && SX=1010

if [ $EDITION = lite ]; then
  python3 prefs.py $L $P; snap home_dark
  python3 prefs.py $L $P PALETTE=papel ACCENT=#5E7FA3; snap home_paper
  python3 prefs.py $L $P; sleep 3; $E shell input swipe 540 1900 540 500 250; snap drawer
  $E shell input keyevent BACK; sleep 1; $E shell input tap $SX 165; snap settings
  # Aparência: o texto da linha vem do strings.xml do idioma (a altura muda com as quebras de linha do cartão de cima)
  $E shell uiautomator dump /data/local/tmp/ui.xml >/dev/null
  Y=$($E shell cat /data/local/tmp/ui.xml | L=$L python3 -c "
import os, re, sys
res = '../../app/src/main/res/'
q = {'pt': 'pt-rBR', 'es': 'es', 'es-419': 'b+es+419', 'zh-CN': 'zh-rCN', 'zh-TW': 'zh-rTW', 'zh-HK': 'zh-rHK', 'fr-CA': 'fr-rCA', 'pt-PT': 'pt-rPT'}.get(os.environ['L'], os.environ['L'])
name = None
for d in ('values-' + q, 'values-' + q.split('-')[0], {'zh-rHK': 'values-zh-rTW', 'b+es+419': 'values-es'}.get(q, 'values'), 'values'):
    try: m = re.search(r'name=\"appearance\">([^<]*)<', open(res + d + '/strings.xml').read())
    except OSError: continue
    if m: name = m.group(1); break
xml = sys.stdin.read()
rows = re.findall(r'text=\"([^\"]*)\"[^>]*bounds=\"\[\d+,(\d+)\]\[\d+,(\d+)\]', xml)
print(next((int(a) + int(b)) // 2 for t, a, b in rows if t == name))")
  $E shell input tap 400 $Y; snap appearance
  $E shell input keyevent BACK; $E shell input keyevent BACK
  exit 0
fi

# Papel de parede de exemplo para o tema "Fundo"
$E push wallpaper.jpg /data/local/tmp/w.jpg >/dev/null
$E shell 'mkdir -p /data/data/app.pauca.debug/files && cp /data/local/tmp/w.jpg /data/data/app.pauca.debug/files/wallpaper.jpg && chown -R $(stat -c %u:%g /data/data/app.pauca.debug) /data/data/app.pauca.debug/files'
python3 prefs.py $L $P; snap home_dark
python3 prefs.py $L $P PALETTE=papel; snap home_paper
python3 prefs.py $L $P PALETTE=grafite; snap home_grafite
python3 prefs.py $L $P PALETTE=wallpaper WALLPAPER_SOURCE=image WALLPAPER_BLUR=true WALLPAPER_BLUR_RADIUS=14 WALLPAPER_BRIGHTNESS=85; snap home_wall
ACTIVE=focus python3 prefs.py $L $P FOCUS_ACTIVE=true FOCUS_SINCE=1791290520000L; snap home_focus
python3 tap.py --chip $OUT  # posição do selo do modo foco, para o zoom do slide
# Clima na tela inicial (leitura guardada pelo prefs.py, sem internet)
python3 prefs.py $L $P PALETTE=papel ACCENT=#C4673F WEATHER_ON=true; snap home_weather
# Pausa para respirar: o app já passou do limite hoje, então tocar nele abre a pausa
MSG=com.google.android.apps.messaging
python3 prefs.py $L $P PAUSE_ON=true PAUSE_LIMIT=10 PAUSE_APPS=set:$MSG PAUSE_DAY=2026-07-26 "PAUSE_COUNTS={\"$MSG\":11}"
sleep 3; python3 tap.py $L messages; snap pause_idle
# Segurando o círculo: ~4 s de toque é o auge da inspiração
( $E shell input swipe 540 1000 540 1000 9000 & ); sleep 4; $E exec-out screencap -p > $OUT/pause_hold.png; sleep 6
python3 prefs.py $L $P PALETTE=papel DATE_TIME_VISIBILITY=0 HOME_STYLE=1 APP_FONT=newsreader APP_WEIGHT=400 APP_TEXT_SIZE=36 HOME_ALIGNMENT=1 APP_LABEL_ALIGNMENT=1 HOME_VERTICAL=1; snap paper_list_serif
python3 prefs.py $L $P PALETTE=papel HOME_STYLE=1 CLOCK_ALIGNMENT=1 HOME_ALIGNMENT=1 APP_LABEL_ALIGNMENT=1 TEXT_CASE=2 APP_FONT=mono APP_WEIGHT=500 APP_TEXT_SIZE=24 CLOCK_FONT=mono CLOCK_SIZE=64 HOME_VERTICAL=1; snap paper_mono
python3 prefs.py $L $P PALETTE=papel ACCENT=#C4673F CLOCK_ACCENT=true CLOCK_WEIGHT=500 HOME_STYLE=1 HOME_VERTICAL=2 DATE_TIME_VISIBILITY=3; snap paper_terracotta
python3 prefs.py $L $P; sleep 3; $E shell input swipe 540 1900 540 500 250; snap drawer
$E shell input keyevent BACK; sleep 1; $E shell input tap $SX 165; snap settings
$E shell input keyevent BACK
