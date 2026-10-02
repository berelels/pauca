#!/bin/bash
# capture_all.sh <en|pt|es>: troca o idioma do emulador e captura todas as telas usadas nos slides.
# Precisa do emulador (imagem google_apis, com root) e do build de debug instalado; veja README.md.
set -e
cd "$(dirname "$0")"
L=$1; E="adb -s emulator-5554"
# Espanhol latino-americano: o da Espanha mostra "9:42" na barra de status, sem o zero
case $L in en) LOC=en-US;; pt) LOC=pt-BR;; es) LOC=es-419;; esac
if [ "$($E shell getprop persist.sys.locale | tr -d '\r')" != "$LOC" ]; then
  $E shell setprop persist.sys.locale $LOC; $E shell setprop ctl.restart zygote; sleep 15
  until [ "$($E shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done; sleep 5
fi
# 24 horas em todos os idiomas, para o horário aparecer como 09:42 (antes do demo.sh, que fixa a barra)
$E shell settings put system time_12_24 24
./demo.sh
# Papel de parede de exemplo para o tema "Fundo"
$E push wallpaper.jpg /data/local/tmp/w.jpg >/dev/null
$E shell 'mkdir -p /data/data/app.pauca.debug/files && cp /data/local/tmp/w.jpg /data/data/app.pauca.debug/files/wallpaper.jpg && chown -R $(stat -c %u:%g /data/data/app.pauca.debug) /data/data/app.pauca.debug/files'
OUT=shots/$L; mkdir -p $OUT
snap() { sleep 3; $E exec-out screencap -p > $OUT/$1.png; }
P="AUTO_SHOW_KEYBOARD=false"
python3 prefs.py $L $P; snap home_dark
python3 prefs.py $L $P PALETTE=papel; snap home_paper
python3 prefs.py $L $P PALETTE=grafite; snap home_grafite
python3 prefs.py $L $P PALETTE=wallpaper WALLPAPER_SOURCE=image WALLPAPER_BLUR=true WALLPAPER_BLUR_RADIUS=14 WALLPAPER_BRIGHTNESS=85; snap home_wall
ACTIVE=focus python3 prefs.py $L $P FOCUS_ACTIVE=true FOCUS_SINCE=1791290520000L; snap home_focus
python3 prefs.py $L $P PALETTE=papel DATE_TIME_VISIBILITY=0 HOME_STYLE=1 APP_FONT=newsreader APP_WEIGHT=400 APP_TEXT_SIZE=36 HOME_ALIGNMENT=1 APP_LABEL_ALIGNMENT=1 HOME_VERTICAL=1; snap paper_list_serif
python3 prefs.py $L $P PALETTE=papel HOME_STYLE=1 CLOCK_ALIGNMENT=1 HOME_ALIGNMENT=1 APP_LABEL_ALIGNMENT=1 TEXT_CASE=2 APP_FONT=mono APP_WEIGHT=500 APP_TEXT_SIZE=24 CLOCK_FONT=mono CLOCK_SIZE=64 HOME_VERTICAL=1; snap paper_mono
python3 prefs.py $L $P PALETTE=papel ACCENT=#C4673F CLOCK_ACCENT=true CLOCK_WEIGHT=500 HOME_STYLE=1 HOME_VERTICAL=2 DATE_TIME_VISIBILITY=3; snap paper_terracotta
python3 prefs.py $L $P PALETTE=papel DATE_TIME_VISIBILITY=0 HOME_STYLE=1 APP_FONT=newsreader APP_WEIGHT=400 APP_TEXT_SIZE=40 HOME_VERTICAL=1 TOP_BAR_MODE=0 BOTTOM_BAR_MODE=0 STATUS_BAR=false; snap paper_zen
python3 prefs.py $L $P; sleep 3; $E shell input swipe 540 1900 540 500 250; snap drawer
$E shell input keyevent BACK; sleep 1; $E shell input tap 70 165; snap settings
$E shell input keyevent BACK
