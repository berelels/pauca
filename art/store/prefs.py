"""Gera as SharedPreferences do Pauca para as capturas de divulgação.
Uso: python3 prefs.py <idioma> [chave=valor ...]  (valores: true/false, inteiros ou texto)"""
import json, re, sys, uuid, subprocess
from xml.sax.saxutils import escape, quoteattr

E = ["adb", "-s", "emulator-5554"]
PKG = "app.pauca.debug"
LABELS = {
    "en": {"phone": "phone", "messages": "messages", "camera": "camera", "photos": "photos",
           "music": "music", "maps": "maps", "calendar": "calendar", "mail": "mail",
           "personal": "Personal", "focus": "Focus", "work": "Work"},
    "pt": {"phone": "telefone", "messages": "mensagens", "camera": "câmera", "photos": "fotos",
           "music": "música", "maps": "mapas", "calendar": "agenda", "mail": "e-mail",
           "personal": "Pessoal", "focus": "Foco", "work": "Trabalho"},
    "es": {"phone": "teléfono", "messages": "mensajes", "camera": "cámara", "photos": "fotos",
           "music": "música", "maps": "mapas", "calendar": "agenda", "mail": "correo",
           "personal": "Personal", "focus": "Concentración", "work": "Trabajo"},
}
APPS = {
    "phone": ("com.google.android.dialer", "com.google.android.dialer.extensions.GoogleDialtactsActivity"),
    "messages": ("com.google.android.apps.messaging", "com.google.android.apps.messaging.ui.ConversationListActivity"),
    "camera": ("com.android.camera2", "com.android.camera.CameraLauncher"),
    "photos": ("com.google.android.apps.photos", "com.google.android.apps.photos.home.HomeActivity"),
    "music": ("com.google.android.apps.youtube.music", "com.google.android.apps.youtube.music.activities.MusicActivity"),
    "maps": ("com.google.android.apps.maps", "com.google.android.maps.MapsActivity"),
    "calendar": ("com.google.android.calendar", "com.android.calendar.AllInOneActivity"),
    "mail": ("com.google.android.gm", "com.google.android.gm.ConversationListActivityGmail"),
}

def item(lang, key):
    pkg, act = APPS[key]
    return {"id": uuid.uuid4().hex[:8], "label": LABELS[lang][key], "pkg": pkg, "activity": act,
            "user": "UserHandle{0}", "shortcutId": ""}

def group(lang, keys):
    return {"id": uuid.uuid4().hex[:8], "items": [item(lang, k) for k in keys]}

def home(lang):
    L = LABELS[lang]
    personal = {"id": "personal", "name": L["personal"], "focusOnSwitch": False,
                "groups": [group(lang, ["phone", "messages", "camera", "photos"]),
                           group(lang, ["music", "maps", "calendar"])]}
    work = {"id": "work", "name": L["work"], "focusOnSwitch": False,
            "groups": [group(lang, ["mail", "calendar", "messages"])]}
    focus = {"id": "focus", "name": L["focus"], "focusOnSwitch": True,
             "groups": [group(lang, ["phone", "messages", "maps"])]}
    return {"version": 1, "activeId": ACTIVE, "profiles": [personal, work, focus]}

def xml_value(k, v):
    if isinstance(v, bool):
        return f'    <boolean name={quoteattr(k)} value="{str(v).lower()}" />'
    if isinstance(v, Long):
        return f'    <long name={quoteattr(k)} value="{int(v)}" />'
    if isinstance(v, int):
        return f'    <int name={quoteattr(k)} value="{v}" />'
    return f'    <string name={quoteattr(k)}>{escape(str(v))}</string>'

def write(name, values):
    body = "\n".join(xml_value(k, v) for k, v in values.items())
    xml = f"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n{body}\n</map>\n"
    tmp = f"/data/local/tmp/{name}.xml"
    subprocess.run(E + ["shell", f"cat > {tmp}"], input=xml.encode(), check=True)
    subprocess.run(E + ["shell", f"cp {tmp} /data/data/{PKG}/shared_prefs/{name}.xml && "
                    f"chown $(stat -c %u:%g /data/data/{PKG}) /data/data/{PKG}/shared_prefs/{name}.xml && "
                    f"chmod 660 /data/data/{PKG}/shared_prefs/{name}.xml"], check=True)

class Long(int):
    pass

def parse(v):
    if re.fullmatch(r"\d+L", v): return Long(int(v[:-1]))
    if v in ("true", "false"): return v == "true"
    try: return int(v)
    except ValueError: return v

lang = sys.argv[1]
import os
ACTIVE = os.environ.get("ACTIVE", "personal")
prefs = {"FIRST_OPEN": False, "FIRST_SETTINGS_OPEN": False, "FIRST_HIDE": False, "TUTORIAL_DONE": True,
         "HIDE_SET_DEFAULT_LAUNCHER": True, "KEYBOARD_MESSAGE": True, "LANGUAGE": lang if lang != "pt" else "pt-BR",
         "TOP_BAR_MODE": 1, "BOTTOM_BAR_MODE": 1, "STATUS_BAR": True}
for kv in sys.argv[2:]:
    k, v = kv.split("=", 1); prefs[k] = parse(v)
subprocess.run(E + ["shell", f"am force-stop {PKG}; mkdir -p /data/data/{PKG}/shared_prefs"], check=True)
write("app.pauca", prefs)
write("pauca_home", {"HOME_DATA": json.dumps(home(lang), ensure_ascii=False)})
subprocess.run(E + ["shell", "date 100609422026.00"], check=True, capture_output=True)
subprocess.run(E + ["shell", "am start -a android.intent.action.MAIN -c android.intent.category.HOME"], check=True, capture_output=True)
