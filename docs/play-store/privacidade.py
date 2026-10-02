#!/usr/bin/env python3
"""
Gera a política de privacidade (docs/privacy/), publicada pelo GitHub Pages em
https://berelels.github.io/pauca/privacy/ (inglês), .../privacy/pt/ e .../privacy/es/.
Edite os textos aqui e rode: python3 docs/play-store/privacidade.py

A página não carrega nada de fora (fontes, scripts, contadores): é só HTML e CSS.
"""
import html
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "privacy")
REPO = "https://github.com/berelels/pauca"

# Cada página: lista de (título da seção, [parágrafos ou listas]). Uma lista é uma tupla.
PAGES = {
    "en": dict(
        lang="en", path="", title="Privacy policy",
        sub="Pauca and Pauca Lite · Last updated October 2, 2026",
        sections=[
            ("In short", [
                "Pauca doesn't collect, sell or share any data about you. The app has no internet permission, so it can't send anything anywhere. There are no ads, no analytics, no accounts and no trackers.",
            ]),
            ("Who makes it", [
                f'Pauca and Pauca Lite are Android home screen apps made by Gabriel Dias (berelels), an independent developer in Brazil. The code is open source: <a href="{REPO}">github.com/berelels/pauca</a>.',
            ]),
            ("What stays on your phone", [
                "To work, the app keeps these on your phone, and only there:",
                ("Your home screen: cards, the names you gave your apps, and profiles.",
                 "Your settings: theme, colors, fonts, clock and gestures.",
                 "The image you pick as your wallpaper, if you pick one (a copy, inside the app).",
                 "In focus mode, how many notifications each app sent, to show the summary when focus ends. The content of notifications is never saved."),
                "Uninstalling the app deletes all of it.",
                "If Android's backup is on, your settings may go into your phone's backup in your Google account, like any other app's. That backup is handled by Android and Google; we can't see it.",
            ]),
            ("Permissions, and why", [
                ("<b>See your installed apps</b>: to list them on the home screen and in the drawer. The list never leaves your phone.",
                 "<b>Accessibility service</b> (optional): only to lock the screen with a double tap and, in focus mode, to go back home when an app outside your list opens. It only checks which app is in front. It doesn't read what's on the screen or what you type. You turn it on yourself in Android's settings, after the app explains it.",
                 "<b>Notification access</b> (optional, Pauca only): to hold notifications from apps outside your list during focus mode and count them for the summary.",
                 "<b>Do Not Disturb access</b> (optional, Pauca only): to turn Pauca's own Do Not Disturb rule on and off during focus mode.",
                 "<b>Usage access</b> (optional): to show today's screen time next to the date, if you turn it on. It's calculated on the phone.",
                 "<b>Device administrator</b> (optional, older Android versions): only to lock the screen with a double tap.",
                 "<b>Set wallpaper</b> (Pauca only): to put the image you chose on the lock screen, when you ask for it.",
                 "<b>Change secure settings</b> (optional, Pauca only): for the grayscale screen in focus mode. Android only allows it if you grant it yourself, with a command from a computer.",
                 "<b>Others</b>: open the notification panel, open the alarm app, ask Android to uninstall an app you chose, and show apps from Android's private space."),
            ]),
            ("From Pauca Lite to Pauca", [
                "When you install Pauca on a phone that has Pauca Lite, Pauca copies Lite's cards and settings straight from one app to the other, on the phone. Only apps signed by the same developer can read them.",
            ]),
            ("Purchases", [
                "Pauca is sold through Google Play, and Google handles the payment under its own privacy policy. We never see your card. Google shares with developers only what sales reports need, such as the country of the purchase.",
            ]),
            ("Children", [
                "Pauca isn't made for children under 13 and doesn't collect data from anyone.",
            ]),
            ("Changes", [
                "If this policy changes, the new version will be on this page with a new date. As the app collects no data, a change would only explain something better, never start collecting.",
            ]),
            ("Contact", [
                f'Questions? Open an issue at <a href="{REPO}/issues">github.com/berelels/pauca/issues</a>, or write to the email on the app\'s Google Play page.',
            ]),
        ],
    ),
    "pt": dict(
        lang="pt-BR", path="pt/", title="Política de privacidade",
        sub="Pauca e Pauca Lite · Atualizada em 2 de outubro de 2026",
        sections=[
            ("Em resumo", [
                "O Pauca não coleta, não vende e não compartilha nenhum dado sobre você. O app não tem permissão de internet, então não consegue enviar nada para lugar nenhum. Não tem anúncios, estatísticas de uso, contas nem rastreadores.",
            ]),
            ("Quem faz", [
                f'O Pauca e o Pauca Lite são apps de tela inicial para Android feitos por Gabriel Dias (berelels), desenvolvedor independente no Brasil. O código é aberto: <a href="{REPO}">github.com/berelels/pauca</a>.',
            ]),
            ("O que fica no seu celular", [
                "Para funcionar, o app guarda no seu celular, e só nele:",
                ("Sua tela inicial: cartões, os nomes que você deu aos apps e os perfis.",
                 "Seus ajustes: tema, cores, fontes, relógio e gestos.",
                 "A imagem que você escolher como fundo, se escolher (uma cópia, dentro do app).",
                 "No modo foco, quantas notificações cada app mandou, para mostrar o resumo quando o foco acaba. O conteúdo das notificações nunca é guardado."),
                "Desinstalar o app apaga tudo isso.",
                "Se o backup do Android estiver ligado, seus ajustes podem entrar no backup do celular na sua conta Google, como os de qualquer outro app. Esse backup é feito pelo Android e pelo Google; nós não temos acesso a ele.",
            ]),
            ("Permissões, e para quê", [
                ("<b>Ver os apps instalados</b>: para listá-los na tela inicial e na gaveta. A lista nunca sai do seu celular.",
                 "<b>Serviço de acessibilidade</b> (opcional): só para bloquear a tela com o toque duplo e, no modo foco, voltar para o início quando um app fora da sua lista abre. Ele só confere qual app está na frente. Não lê o que está na tela nem o que você digita. Você mesmo o liga nos ajustes do Android, depois de o app explicar.",
                 "<b>Acesso às notificações</b> (opcional, só no Pauca): para segurar as notificações dos apps fora da sua lista durante o modo foco e contá-las para o resumo.",
                 "<b>Acesso ao Não Perturbe</b> (opcional, só no Pauca): para ligar e desligar a regra de Não Perturbe do próprio Pauca durante o modo foco.",
                 "<b>Acesso ao uso</b> (opcional): para mostrar o tempo de tela do dia ao lado da data, se você ligar. A conta é feita no celular.",
                 "<b>Administrador do dispositivo</b> (opcional, em versões antigas do Android): só para bloquear a tela com o toque duplo.",
                 "<b>Definir papel de parede</b> (só no Pauca): para pôr a imagem escolhida na tela de bloqueio, quando você pedir.",
                 "<b>Alterar configurações seguras</b> (opcional, só no Pauca): para a tela em tons de cinza do modo foco. O Android só permite se você mesmo conceder, com um comando no computador.",
                 "<b>Outras</b>: abrir o painel de notificações, abrir o app de alarme, pedir ao Android para desinstalar um app que você escolheu e mostrar os apps do espaço privado do Android."),
            ]),
            ("Do Pauca Lite para o Pauca", [
                "Quando você instala o Pauca num celular que tem o Pauca Lite, o Pauca copia os cartões e ajustes do Lite direto de um app para o outro, no celular. Só apps assinados pelo mesmo desenvolvedor conseguem lê-los.",
            ]),
            ("Compras", [
                "O Pauca é vendido pela Google Play, e o pagamento é feito pelo Google, sob a política de privacidade dele. Nunca vemos o seu cartão. O Google compartilha com desenvolvedores só o necessário para os relatórios de vendas, como o país da compra.",
            ]),
            ("Crianças", [
                "O Pauca não é feito para menores de 13 anos e não coleta dados de ninguém.",
            ]),
            ("Mudanças", [
                "Se esta política mudar, a nova versão estará nesta página, com uma nova data. Como o app não coleta dados, uma mudança só explicaria algo melhor, nunca passaria a coletar.",
            ]),
            ("Contato", [
                f'Dúvidas? Abra uma issue em <a href="{REPO}/issues">github.com/berelels/pauca/issues</a>, ou escreva para o e-mail que aparece na página do app na Google Play.',
            ]),
        ],
    ),
    "es": dict(
        lang="es", path="es/", title="Política de privacidad",
        sub="Pauca y Pauca Lite · Actualizada el 2 de octubre de 2026",
        sections=[
            ("En resumen", [
                "Pauca no recopila, no vende y no comparte ningún dato sobre ti. La app no tiene permiso de internet, así que no puede enviar nada a ningún sitio. No tiene anuncios, estadísticas de uso, cuentas ni rastreadores.",
            ]),
            ("Quién la hace", [
                f'Pauca y Pauca Lite son apps de pantalla de inicio para Android hechas por Gabriel Dias (berelels), desarrollador independiente en Brasil. El código es abierto: <a href="{REPO}">github.com/berelels/pauca</a>.',
            ]),
            ("Lo que se queda en tu teléfono", [
                "Para funcionar, la app guarda en tu teléfono, y solo allí:",
                ("Tu pantalla de inicio: tarjetas, los nombres que diste a tus apps y los perfiles.",
                 "Tus ajustes: tema, colores, fuentes, reloj y gestos.",
                 "La imagen que elijas como fondo, si eliges una (una copia, dentro de la app).",
                 "En el modo concentración, cuántas notificaciones envió cada app, para mostrar el resumen al terminar. El contenido de las notificaciones nunca se guarda."),
                "Desinstalar la app lo borra todo.",
                "Si la copia de seguridad de Android está activada, tus ajustes pueden entrar en la copia del teléfono en tu cuenta de Google, como los de cualquier otra app. Esa copia la hacen Android y Google; nosotros no tenemos acceso.",
            ]),
            ("Permisos, y para qué", [
                ("<b>Ver las apps instaladas</b>: para mostrarlas en la pantalla de inicio y en el cajón. La lista nunca sale de tu teléfono.",
                 "<b>Servicio de accesibilidad</b> (opcional): solo para bloquear la pantalla con el doble toque y, en el modo concentración, volver al inicio cuando se abre una app que no está en tu lista. Solo comprueba qué app está delante. No lee lo que hay en la pantalla ni lo que escribes. Lo activas tú en los ajustes de Android, después de que la app lo explique.",
                 "<b>Acceso a las notificaciones</b> (opcional, solo en Pauca): para retener las notificaciones de las apps fuera de tu lista durante el modo concentración y contarlas para el resumen.",
                 "<b>Acceso a No molestar</b> (opcional, solo en Pauca): para activar y desactivar la regla de No molestar propia de Pauca durante el modo concentración.",
                 "<b>Acceso al uso</b> (opcional): para mostrar el tiempo de pantalla del día junto a la fecha, si lo activas. Se calcula en el teléfono.",
                 "<b>Administrador del dispositivo</b> (opcional, en versiones antiguas de Android): solo para bloquear la pantalla con el doble toque.",
                 "<b>Establecer fondo de pantalla</b> (solo en Pauca): para poner la imagen elegida en la pantalla de bloqueo, cuando lo pidas.",
                 "<b>Cambiar ajustes seguros</b> (opcional, solo en Pauca): para la pantalla en escala de grises del modo concentración. Android solo lo permite si lo concedes tú, con un comando desde un ordenador.",
                 "<b>Otros</b>: abrir el panel de notificaciones, abrir la app de alarmas, pedir a Android que desinstale una app que elegiste y mostrar las apps del espacio privado de Android."),
            ]),
            ("De Pauca Lite a Pauca", [
                "Cuando instalas Pauca en un teléfono que tiene Pauca Lite, Pauca copia las tarjetas y los ajustes de Lite directamente de una app a la otra, en el teléfono. Solo las apps firmadas por el mismo desarrollador pueden leerlos.",
            ]),
            ("Compras", [
                "Pauca se vende a través de Google Play, y el pago lo gestiona Google, según su propia política de privacidad. Nunca vemos tu tarjeta. Google comparte con los desarrolladores solo lo necesario para los informes de ventas, como el país de la compra.",
            ]),
            ("Niños", [
                "Pauca no está hecha para menores de 13 años y no recopila datos de nadie.",
            ]),
            ("Cambios", [
                "Si esta política cambia, la nueva versión estará en esta página, con una nueva fecha. Como la app no recopila datos, un cambio solo explicaría algo mejor, nunca empezaría a recopilar.",
            ]),
            ("Contacto", [
                f'¿Preguntas? Abre una issue en <a href="{REPO}/issues">github.com/berelels/pauca/issues</a>, o escribe al correo que aparece en la página de la app en Google Play.',
            ]),
        ],
    ),
}

CSS = """
:root { --bg: #F4EFE4; --card: #EAE3D3; --text: #1C1915; --muted: #6B645A; --accent: #5E6A3B; color-scheme: light; }
@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) { --bg: #1C1915; --card: #2A2620; --text: #F4EFE4; --muted: #A79F92; --accent: #A9B77E; color-scheme: dark; }
}
* { box-sizing: border-box; }
body { margin: 0; background: var(--bg); color: var(--text);
  font: 17px/1.6 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
main { max-width: 680px; margin: 0 auto; padding: 48px 16px 72px; }
nav { display: flex; gap: 14px; justify-content: flex-end; font-size: 14px; margin-bottom: 32px; }
nav a, nav span { color: var(--muted); text-decoration: none; }
nav span { color: var(--text); font-weight: 600; }
.mark { width: 44px; height: auto; color: var(--accent); }
h1 { font-family: Georgia, "Times New Roman", serif; font-weight: 400; font-size: 40px; line-height: 1.1; margin: 16px 0 6px; }
.sub { color: var(--muted); font-size: 15px; margin: 0 0 36px; }
h2 { font-family: Georgia, "Times New Roman", serif; font-weight: 400; font-size: 25px; margin: 36px 0 8px; }
p, li { margin: 0 0 12px; }
ul { padding-left: 20px; margin: 0 0 12px; }
a { color: var(--accent); }
.short { background: var(--card); border-radius: 18px; padding: 18px 22px; }
.short h2 { margin-top: 0; }
"""

EYE = ('<svg class="mark" viewBox="115 209 282 174" fill="none" stroke="currentColor" stroke-width="34" '
       'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M132 226c40 62 84 90 124 '
       '90s84-28 124-90M166 290l-26 40M256 318v48M346 290l26 40"/></svg>')
NAV = [("en", "English", ""), ("pt", "Português", "pt/"), ("es", "Español", "es/")]


def render(key: str, page: dict) -> str:
    depth = "../" if page["path"] else ""
    home = "../../" if page["path"] else "../"
    nav = (f'<a href="{home}{page["path"]}" style="margin-right:auto">← Pauca</a> '
           + " ".join(f"<span>{label}</span>" if k == key else f'<a href="{depth}{p}">{label}</a>' for k, label, p in NAV))
    body = []
    for i, (title, blocks) in enumerate(page["sections"]):
        parts = [f"<h2>{html.escape(title)}</h2>"]
        for b in blocks:
            parts.append("<ul>" + "".join(f"<li>{x}</li>" for x in b) + "</ul>" if isinstance(b, tuple) else f"<p>{b}</p>")
        inner = "\n".join(parts)
        body.append(f'<section class="short">{inner}</section>' if i == 0 else f"<section>{inner}</section>")
    return f"""<!doctype html>
<html lang="{page['lang']}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Pauca · {html.escape(page['title'])}</title>
<style>{CSS}</style>
</head>
<body>
<main>
<nav>{nav}</nav>
{EYE}
<h1>{html.escape(page['title'])}</h1>
<p class="sub">{html.escape(page['sub'])}</p>
{chr(10).join(body)}
</main>
</body>
</html>
"""


for key, page in PAGES.items():
    folder = os.path.join(OUT, page["path"])
    os.makedirs(folder, exist_ok=True)
    with open(os.path.join(folder, "index.html"), "w") as f:
        f.write(render(key, page))
    print(os.path.relpath(os.path.join(folder, "index.html")))
