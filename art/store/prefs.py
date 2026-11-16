"""Gera as SharedPreferences do Pauca para as capturas de divulgação.
Uso: python3 prefs.py <idioma> [chave=valor ...]  (valores: true/false, inteiros ou texto)"""
import json, os, re, sys, uuid, subprocess
from xml.sax.saxutils import escape, quoteattr

E = ["adb", "-s", "emulator-5554"]
PKG = os.environ.get("PKG", "app.pauca.debug")
KEYS = ["phone", "messages", "camera", "photos", "music", "maps", "calendar", "mail", "personal", "focus", "work"]
_L = {
    "en": "phone|messages|camera|photos|music|maps|calendar|mail|Personal|Focus|Work",
    "pt": "telefone|mensagens|câmera|fotos|música|mapas|agenda|e-mail|Pessoal|Foco|Trabalho",
    "es": "teléfono|mensajes|cámara|fotos|música|mapas|agenda|correo|Personal|Concentración|Trabajo",
    "zh-CN": "电话|信息|相机|相册|音乐|地图|日历|邮件|个人|专注|工作",
    "zh-TW": "電話|訊息|相機|相簿|音樂|地圖|日曆|郵件|個人|專注|工作",
    "zh-HK": "電話|訊息|相機|相簿|音樂|地圖|日曆|電郵|個人|專注|工作",
    "fr": "téléphone|messages|caméra|photos|musique|plans|agenda|e-mail|Personnel|Concentration|Travail",
    "fr-CA": "téléphone|messages|caméra|photos|musique|cartes|agenda|courriel|Personnel|Concentration|Travail",
    "ar": "الهاتف|الرسائل|الكاميرا|الصور|الموسيقى|الخرائط|التقويم|البريد|شخصي|تركيز|عمل",
    "ja": "電話|メッセージ|カメラ|写真|音楽|マップ|カレンダー|メール|個人|集中|仕事",
    "ko": "전화|메시지|카메라|사진|음악|지도|캘린더|메일|개인|집중|업무",
    "hi": "फ़ोन|संदेश|कैमरा|फ़ोटो|संगीत|मानचित्र|कैलेंडर|मेल|निजी|फ़ोकस|काम",
    "de": "Telefon|Nachrichten|Kamera|Fotos|Musik|Karten|Kalender|E-Mail|Privat|Fokus|Arbeit",
    "ru": "телефон|сообщения|камера|фото|музыка|карты|календарь|почта|Личное|Фокус|Работа",
    "it": "telefono|messaggi|fotocamera|foto|musica|mappe|calendario|posta|Personale|Concentrazione|Lavoro",
    "nl": "telefoon|berichten|camera|foto's|muziek|kaarten|agenda|e-mail|Persoonlijk|Focus|Werk",
    "pt-PT": "telemóvel|mensagens|câmara|fotografias|música|mapas|agenda|e-mail|Pessoal|Foco|Trabalho",
    "es-419": "teléfono|mensajes|cámara|fotos|música|mapas|calendario|correo|Personal|Concentración|Trabajo",
    "bn": "ফোন|বার্তা|ক্যামেরা|ছবি|সংগীত|মানচিত্র|ক্যালেন্ডার|মেইল|ব্যক্তিগত|ফোকাস|কাজ",
    "ta": "தொலைபேசி|செய்திகள்|கேமரா|புகைப்படங்கள்|இசை|வரைபடம்|நாட்காட்டி|அஞ்சல்|தனிப்பட்ட|கவனம்|வேலை",
    "te": "ఫోన్|సందేశాలు|కెమెరా|ఫోటోలు|సంగీతం|మ్యాప్స్|క్యాలెండర్|మెయిల్|వ్యక్తిగతం|ఫోకస్|పని",
    "mr": "फोन|संदेश|कॅमेरा|फोटो|संगीत|नकाशे|दिनदर्शिका|मेल|वैयक्तिक|फोकस|काम",
    "ca": "telèfon|missatges|càmera|fotos|música|mapes|calendari|correu|Personal|Concentració|Feina",
    "gl": "teléfono|mensaxes|cámara|fotos|música|mapas|calendario|correo|Persoal|Concentración|Traballo",
    "eu": "telefonoa|mezuak|kamera|argazkiak|musika|mapak|egutegia|posta|Pertsonala|Kontzentrazioa|Lana",
}
LABELS = {k: dict(zip(KEYS, v.split("|"))) for k, v in _L.items()}
# Cidade do clima de cada idioma: nome, latitude, longitude, graus Celsius (leitura já guardada, sem internet)
WEATHER = {
    "en": ("London", "51.51", "-0.13", "21"), "pt": ("São Paulo", "-23.55", "-46.63", "24"),
    "es": ("Madrid", "40.42", "-3.70", "26"), "zh-CN": ("北京", "39.90", "116.41", "28"),
    "zh-TW": ("台北", "25.03", "121.57", "31"), "zh-HK": ("香港", "22.32", "114.17", "30"),
    "fr": ("Paris", "48.86", "2.35", "24"), "fr-CA": ("Montréal", "45.50", "-73.57", "24"),
    "ar": ("القاهرة", "30.04", "31.24", "34"), "ja": ("東京", "35.68", "139.69", "29"),
    "ko": ("서울", "37.57", "126.98", "28"), "hi": ("दिल्ली", "28.61", "77.21", "33"),
    "de": ("Berlin", "52.52", "13.40", "23"), "ru": ("Москва", "55.76", "37.62", "22"),
    "it": ("Roma", "41.90", "12.50", "29"), "nl": ("Amsterdam", "52.37", "4.90", "21"),
    "pt-PT": ("Lisboa", "38.72", "-9.14", "27"), "es-419": ("Ciudad de México", "19.43", "-99.13", "23"),
    "bn": ("ঢাকা", "23.81", "90.41", "32"), "ta": ("சென்னை", "13.08", "80.27", "33"),
    "te": ("హైదరాబాద్", "17.39", "78.49", "30"), "mr": ("मुंबई", "19.08", "72.88", "30"),
    "ca": ("Barcelona", "41.39", "2.17", "27"), "gl": ("Santiago de Compostela", "42.88", "-8.54", "22"),
    "eu": ("Bilbo", "43.26", "-2.93", "22"),
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
    # O Lite não tem modo foco e guarda no máximo dois perfis
    profiles = [personal, work] if ".lite" in PKG else [personal, work, focus]
    return {"version": 1, "activeId": ACTIVE, "profiles": profiles}

def xml_value(k, v):
    if isinstance(v, bool):
        return f'    <boolean name={quoteattr(k)} value="{str(v).lower()}" />'
    if isinstance(v, list):
        items = "".join(f"<string>{escape(x)}</string>" for x in v)
        return f'    <set name={quoteattr(k)}>{items}</set>'
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
    # set:a,b = conjunto de textos; now = hora do emulador em ms, 5 min atrás (cache do clima recente)
    if v.startswith("set:"): return v[4:].split(",")
    if v == "now": return Long(int(subprocess.run(E + ["shell", "date +%s"], capture_output=True, text=True).stdout) * 1000 - 300000)
    if re.fullmatch(r"\d+L", v): return Long(int(v[:-1]))
    if v in ("true", "false"): return v == "true"
    try: return int(v)
    except ValueError: return v

lang = sys.argv[1]
ACTIVE = os.environ.get("ACTIVE", "personal")
prefs = {"FIRST_OPEN": False, "FIRST_SETTINGS_OPEN": False, "FIRST_HIDE": False, "TUTORIAL_DONE": True,
         "HIDE_SET_DEFAULT_LAUNCHER": True, "KEYBOARD_MESSAGE": True, "LANGUAGE": {"pt": "pt-BR"}.get(lang, lang),
         "TOP_BAR_MODE": 1, "BOTTOM_BAR_MODE": 1, "STATUS_BAR": True}
# O relógio do emulador vem antes dos valores "now"
subprocess.run(E + ["shell", "date 072609422026.00"], check=True, capture_output=True)
for kv in sys.argv[2:]:
    k, v = kv.split("=", 1); prefs[k] = parse(v)
if prefs.get("WEATHER_ON"):  # leitura guardada: o clima aparece sem internet
    city, la, lo, t = WEATHER[lang]
    prefs.update(WEATHER_CITY=city, WEATHER_LAT=la, WEATHER_LON=lo, WEATHER_TEMP=t, WEATHER_CODE=2,
                 WEATHER_AT=Long(int(subprocess.run(E + ["shell", "date +%s"], capture_output=True, text=True).stdout) * 1000 - 300000))
subprocess.run(E + ["shell", f"am force-stop {PKG}; mkdir -p /data/data/{PKG}/shared_prefs"], check=True)
# Fechar o app logo depois de abrir conta como queda para o CrashGuard, que então zera fonte e peso e avisa na tela
write("crash_guard", {"PENDING_STARTS": 0})
write("app.pauca", prefs)
write("pauca_home", {"HOME_DATA": json.dumps(home(lang), ensure_ascii=False)})
subprocess.run(E + ["shell", "date 072609422026.00"], check=True, capture_output=True)
subprocess.run(E + ["shell", "am start -a android.intent.action.MAIN -c android.intent.category.HOME"], check=True, capture_output=True)
