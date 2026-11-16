"""Toca num texto da tela (via uiautomator). Uso: tap.py <idioma> <app>  (nome do app em prefs.LABELS)
       tap.py --chip <pasta>  grava em <pasta>/chip.js a posição do selo do modo foco ("·" no texto)."""
import re, subprocess, sys
E = ["adb", "-s", "emulator-5554"]
import time
def dump():
    subprocess.run(E + ["shell", "uiautomator dump /data/local/tmp/ui.xml"], capture_output=True, check=True)
    xml = subprocess.run(E + ["shell", "cat /data/local/tmp/ui.xml"], capture_output=True, text=True).stdout
    return [(t, *map(int, b)) for t, *b in re.findall(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]', xml)]
rows = dump()
if sys.argv[1] == "--chip":
    for _ in range(4):  # a tela às vezes ainda está animando; sem selo, o slide usa as medidas padrão
        hit = next((r for r in rows if "·" in r[0] and r[2] < 400), None)
        if hit: break
        time.sleep(1.5); rows = dump()
    else:
        print("selo do foco não encontrado", file=sys.stderr); sys.exit(0)
    t, x0, y0, x1, y1 = hit
    open(sys.argv[2] + "/chip.js", "w").write(f"window.CHIP = [{x0 - 20}, {x1 - x0 + 40}];\n")
else:
    exec(open(__file__.replace("tap.py", "prefs.py")).read().split("lang = sys.argv[1]")[0])  # só os dados (LABELS)
    label = LABELS[sys.argv[1]][sys.argv[2]].lower()
    t, x0, y0, x1, y1 = next(r for r in rows if r[0].lower() == label)
    subprocess.run(E + ["shell", f"input tap {(x0 + x1) // 2} {(y0 + y1) // 2}"], check=True)
