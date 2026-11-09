#!/usr/bin/env python3
"""
Gera as políticas de privacidade (docs/privacy/) e de segurança (docs/security/), publicadas pelo GitHub Pages em
https://cattastudios.com.br/pauca/privacy/ e .../security/ (inglês), mais pt/ e es/ dentro de cada uma.
Edite os textos aqui e rode: python3 docs/play-store/privacidade.py

As páginas não carregam nada de fora (fontes, scripts, contadores): são só HTML e CSS.
"""
import html
import os

DOCS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SITE = "https://cattastudios.com.br/pauca/"
REPO = "https://github.com/berelels/pauca"
EMAIL = "gabrieldiasdev@proton.me"
GH_PRIVACY = "https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement"

# Cada página: lista de (título da seção, [parágrafos ou listas], âncora opcional). Uma lista é uma tupla.
PAGES = {
    "en": dict(
        lang="en", path="", title="Privacy policy", other="Security policy",
        description="How Pauca, Pauca Lite and this website handle your data: nothing is collected, the apps have no internet permission and the site uses no cookies.",
        sub="Pauca, Pauca Lite and this website · Last updated November 9, 2026",
        sections=[
            ("In short", [
                "Pauca doesn't collect, sell or share any data about you. The app has no internet permission, so it can't send anything anywhere. There are no ads, no analytics, no accounts and no trackers. This website uses no cookies either.",
            ]),
            ("Who makes it", [
                f'Pauca and Pauca Lite are Android home screen apps made by Gabriel Dias (berelels, Catta Studios), an independent developer in Brazil. The code is open source: <a href="{REPO}">github.com/berelels/pauca</a>.',
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
                 "<b>Accessibility service</b> (optional, Pauca only): only to lock the screen with a double tap and, in focus mode, to go back home when an app outside your list opens. It only checks which app is in front. It doesn't read what's on the screen or what you type. You turn it on yourself in Android's settings, after the app explains it.",
                 "<b>Notification access</b> (optional, Pauca only): to hold notifications from apps outside your list during focus mode and count them for the summary.",
                 "<b>Do Not Disturb access</b> (optional, Pauca only): to turn Pauca's own Do Not Disturb rule on and off during focus mode.",
                 "<b>Usage access</b> (optional): to show today's screen time next to the date, if you turn it on. It's calculated on the phone.",
                 "<b>Device administrator</b> (optional, Pauca only, older Android versions): only to lock the screen with a double tap.",
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
            ("This website and cookies", [
                "This part covers the pages under cattastudios.com.br/pauca/. Other pages of cattastudios.com.br are a separate site and aren't covered here.",
                ("<b>No cookies.</b> These pages set none, and there are no analytics, ads or trackers. That's why there's no button to accept anything.",
                 "<b>Nothing from other sites.</b> Fonts, images and code all come from here, so opening a page doesn't contact any third party. Google Play and GitHub only come in if you follow a link to them.",
                 "<b>Two small notes in your browser.</b> <code>pauca-veil</code> (sessionStorage, gone when you close the tab) stops the opening animation from repeating. <code>pauca-notice</code> (localStorage, until you clear your browser data) remembers that you closed the cookie notice. Neither holds anything about you, and neither leaves your browser.",
                 f'<b>Hosting.</b> The site runs on GitHub Pages. GitHub may keep technical logs, such as visitors\' IP addresses, for security and legal reasons, under <a href="{GH_PRIVACY}">its privacy statement</a>. We don\'t receive those logs.'),
            ], "cookies"),
            ("Children", [
                "Pauca isn't made for children under 13 and doesn't collect data from anyone.",
            ]),
            ("Changes", [
                "If this policy changes, the new version will be on this page with a new date. As the app collects no data, a change would only explain something better, never start collecting.",
            ]),
            ("Contact", [
                f'Questions, or a request about your data under Brazil\'s LGPD or a similar law? Write to <a href="mailto:{EMAIL}">{EMAIL}</a>, or open an issue at <a href="{REPO}/issues">github.com/berelels/pauca/issues</a> (issues are public).',
                "If you email us, your address and message are used only to reply, and we'll delete them if you ask.",
            ]),
        ],
    ),
    "pt": dict(
        lang="pt-BR", path="pt/", title="Política de privacidade", other="Política de segurança",
        description="Como o Pauca, o Pauca Lite e este site tratam os seus dados: nada é coletado, os apps não têm permissão de internet e o site não usa cookies.",
        sub="Pauca, Pauca Lite e este site · Atualizada em 9 de novembro de 2026",
        sections=[
            ("Em resumo", [
                "O Pauca não coleta, não vende e não compartilha nenhum dado sobre você. O app não tem permissão de internet, então não consegue enviar nada para lugar nenhum. Não tem anúncios, estatísticas de uso, contas nem rastreadores. Este site também não usa cookies.",
            ]),
            ("Quem faz", [
                f'O Pauca e o Pauca Lite são apps de tela inicial para Android feitos por Gabriel Dias (berelels, Catta Studios), desenvolvedor independente no Brasil. O código é aberto: <a href="{REPO}">github.com/berelels/pauca</a>.',
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
                 "<b>Serviço de acessibilidade</b> (opcional, só no Pauca): só para bloquear a tela com o toque duplo e, no modo foco, voltar para o início quando um app fora da sua lista abre. Ele só confere qual app está na frente. Não lê o que está na tela nem o que você digita. Você mesmo o liga nos ajustes do Android, depois de o app explicar.",
                 "<b>Acesso às notificações</b> (opcional, só no Pauca): para segurar as notificações dos apps fora da sua lista durante o modo foco e contá-las para o resumo.",
                 "<b>Acesso ao Não Perturbe</b> (opcional, só no Pauca): para ligar e desligar a regra de Não Perturbe do próprio Pauca durante o modo foco.",
                 "<b>Acesso ao uso</b> (opcional): para mostrar o tempo de tela do dia ao lado da data, se você ligar. A conta é feita no celular.",
                 "<b>Administrador do dispositivo</b> (opcional, só no Pauca, em versões antigas do Android): só para bloquear a tela com o toque duplo.",
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
            ("Este site e os cookies", [
                "Esta parte vale para as páginas em cattastudios.com.br/pauca/. As outras páginas de cattastudios.com.br são um site separado e não entram aqui.",
                ("<b>Sem cookies.</b> Estas páginas não criam nenhum, e não há estatísticas, anúncios nem rastreadores. Por isso não há botão para aceitar nada.",
                 "<b>Nada de outros sites.</b> Fontes, imagens e código vêm todos daqui, então abrir uma página não contata nenhum terceiro. A Google Play e o GitHub só entram se você seguir um link para eles.",
                 "<b>Duas anotações pequenas no seu navegador.</b> <code>pauca-veil</code> (sessionStorage, some quando você fecha a aba) evita que a animação de abertura se repita. <code>pauca-notice</code> (localStorage, até você limpar os dados do navegador) lembra que você fechou o aviso de cookies. Nenhuma guarda nada sobre você, e nenhuma sai do seu navegador.",
                 f'<b>Hospedagem.</b> O site roda no GitHub Pages. O GitHub pode manter registros técnicos, como o endereço IP de quem visita, por segurança e por obrigações legais, conforme a <a href="{GH_PRIVACY}">declaração de privacidade dele</a>. Nós não recebemos esses registros.'),
            ], "cookies"),
            ("Crianças", [
                "O Pauca não é feito para menores de 13 anos e não coleta dados de ninguém.",
            ]),
            ("Mudanças", [
                "Se esta política mudar, a nova versão estará nesta página, com uma nova data. Como o app não coleta dados, uma mudança só explicaria algo melhor, nunca passaria a coletar.",
            ]),
            ("Contato", [
                f'Dúvidas, ou algum pedido sobre os seus dados pela LGPD? Escreva para <a href="mailto:{EMAIL}">{EMAIL}</a>, ou abra uma issue em <a href="{REPO}/issues">github.com/berelels/pauca/issues</a> (as issues são públicas).',
                "Se você mandar um e-mail, seu endereço e sua mensagem servem só para responder, e nós os apagamos se você pedir.",
            ]),
        ],
    ),
    "es": dict(
        lang="es", path="es/", title="Política de privacidad", other="Política de seguridad",
        description="Cómo tratan tus datos Pauca, Pauca Lite y este sitio: no se recopila nada, las apps no tienen permiso de internet y el sitio no usa cookies.",
        sub="Pauca, Pauca Lite y este sitio · Actualizada el 9 de noviembre de 2026",
        sections=[
            ("En resumen", [
                "Pauca no recopila, no vende y no comparte ningún dato sobre ti. La app no tiene permiso de internet, así que no puede enviar nada a ningún sitio. No tiene anuncios, estadísticas de uso, cuentas ni rastreadores. Este sitio tampoco usa cookies.",
            ]),
            ("Quién la hace", [
                f'Pauca y Pauca Lite son apps de pantalla de inicio para Android hechas por Gabriel Dias (berelels, Catta Studios), desarrollador independiente en Brasil. El código es abierto: <a href="{REPO}">github.com/berelels/pauca</a>.',
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
                 "<b>Servicio de accesibilidad</b> (opcional, solo en Pauca): solo para bloquear la pantalla con el doble toque y, en el modo concentración, volver al inicio cuando se abre una app que no está en tu lista. Solo comprueba qué app está delante. No lee lo que hay en la pantalla ni lo que escribes. Lo activas tú en los ajustes de Android, después de que la app lo explique.",
                 "<b>Acceso a las notificaciones</b> (opcional, solo en Pauca): para retener las notificaciones de las apps fuera de tu lista durante el modo concentración y contarlas para el resumen.",
                 "<b>Acceso a No molestar</b> (opcional, solo en Pauca): para activar y desactivar la regla de No molestar propia de Pauca durante el modo concentración.",
                 "<b>Acceso al uso</b> (opcional): para mostrar el tiempo de pantalla del día junto a la fecha, si lo activas. Se calcula en el teléfono.",
                 "<b>Administrador del dispositivo</b> (opcional, solo en Pauca, en versiones antiguas de Android): solo para bloquear la pantalla con el doble toque.",
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
            ("Este sitio y las cookies", [
                "Esta parte cubre las páginas de cattastudios.com.br/pauca/. Las demás páginas de cattastudios.com.br son un sitio aparte y no entran aquí.",
                ("<b>Sin cookies.</b> Estas páginas no crean ninguna, y no hay estadísticas, anuncios ni rastreadores. Por eso no hay ningún botón para aceptar nada.",
                 "<b>Nada de otros sitios.</b> Las fuentes, las imágenes y el código vienen todos de aquí, así que abrir una página no contacta con ningún tercero. Google Play y GitHub solo entran si sigues un enlace hacia ellos.",
                 "<b>Dos pequeñas notas en tu navegador.</b> <code>pauca-veil</code> (sessionStorage, desaparece al cerrar la pestaña) evita que se repita la animación de apertura. <code>pauca-notice</code> (localStorage, hasta que borres los datos del navegador) recuerda que cerraste el aviso de cookies. Ninguna guarda nada sobre ti, y ninguna sale de tu navegador.",
                 f'<b>Alojamiento.</b> El sitio funciona en GitHub Pages. GitHub puede conservar registros técnicos, como la dirección IP de quien visita, por seguridad y obligaciones legales, según <a href="{GH_PRIVACY}">su declaración de privacidad</a>. Nosotros no recibimos esos registros.'),
            ], "cookies"),
            ("Niños", [
                "Pauca no está hecha para menores de 13 años y no recopila datos de nadie.",
            ]),
            ("Cambios", [
                "Si esta política cambia, la nueva versión estará en esta página, con una nueva fecha. Como la app no recopila datos, un cambio solo explicaría algo mejor, nunca empezaría a recopilar.",
            ]),
            ("Contacto", [
                f'¿Preguntas, o una solicitud sobre tus datos según la LGPD de Brasil u otra ley parecida? Escribe a <a href="mailto:{EMAIL}">{EMAIL}</a>, o abre una issue en <a href="{REPO}/issues">github.com/berelels/pauca/issues</a> (las issues son públicas).',
                "Si nos escribes, tu dirección y tu mensaje solo se usan para responder, y los borramos si lo pides.",
            ]),
        ],
    ),
}

SECURITY = {
    "en": dict(
        lang="en", path="", title="Security policy", other="Privacy policy",
        description="How to report a security problem in Pauca, Pauca Lite or this website, what's covered and what to expect.",
        sub="Pauca, Pauca Lite and this website · Last updated November 9, 2026",
        sections=[
            ("Report a vulnerability", [
                f'Found a security problem? Email <a href="mailto:{EMAIL}">{EMAIL}</a> with what you found and the steps to reproduce it. If it\'s about the app, include its version and your phone model.',
                "Please don't open a public GitHub issue for it, and allow reasonable time for a fix before sharing details publicly.",
            ]),
            ("What this covers", [
                ("The latest versions of Pauca and Pauca Lite on Google Play.",
                 f'The source code at <a href="{REPO}">github.com/berelels/pauca</a>.',
                 "The pages under cattastudios.com.br/pauca/."),
            ]),
            ("What to expect", [
                "Pauca is made by one person, so there's no fixed deadline and no paid bug bounty. Reports are read and valid ones are fixed as soon as possible. If you'd like, you'll be credited once the fix is out.",
            ]),
            ("How Pauca is built", [
                ("The apps have no internet permission, so they can't send data anywhere on their own.",
                 "Your cards and settings stay on your phone. Moving from Lite to Pauca happens on the phone, and only apps signed by the same developer can read that data.",
                 "Sensitive permissions, such as accessibility and notification access, are optional and you turn them on yourself. The privacy policy explains each one.",
                 "Release signing keys are kept outside the public repository.",
                 "This website is made of static files, with no forms, accounts, cookies or third-party scripts."),
            ]),
        ],
    ),
    "pt": dict(
        lang="pt-BR", path="pt/", title="Política de segurança", other="Política de privacidade",
        description="Como relatar um problema de segurança no Pauca, no Pauca Lite ou neste site, o que está coberto e o que esperar.",
        sub="Pauca, Pauca Lite e este site · Atualizada em 9 de novembro de 2026",
        sections=[
            ("Relate uma vulnerabilidade", [
                f'Encontrou um problema de segurança? Escreva para <a href="mailto:{EMAIL}">{EMAIL}</a> contando o que achou e os passos para reproduzir. Se for no app, inclua a versão e o modelo do seu celular.',
                "Por favor, não abra uma issue pública no GitHub sobre isso, e dê um tempo razoável para a correção antes de divulgar os detalhes.",
            ]),
            ("O que está coberto", [
                ("As versões mais recentes do Pauca e do Pauca Lite na Google Play.",
                 f'O código-fonte em <a href="{REPO}">github.com/berelels/pauca</a>.',
                 "As páginas em cattastudios.com.br/pauca/."),
            ]),
            ("O que esperar", [
                "O Pauca é feito por uma pessoa só, então não há prazo fixo nem recompensa em dinheiro. Os relatos são lidos e os válidos são corrigidos o quanto antes. Se quiser, você recebe o crédito quando a correção sair.",
            ]),
            ("Como o Pauca é feito", [
                ("Os apps não têm permissão de internet, então não conseguem enviar dados por conta própria.",
                 "Seus cartões e ajustes ficam no seu celular. A passagem do Lite para o Pauca acontece no celular, e só apps assinados pelo mesmo desenvolvedor conseguem ler esses dados.",
                 "As permissões sensíveis, como acessibilidade e acesso às notificações, são opcionais e você mesmo as liga. A política de privacidade explica cada uma.",
                 "As chaves de assinatura ficam fora do repositório público.",
                 "Este site é feito de arquivos estáticos, sem formulários, contas, cookies nem scripts de terceiros."),
            ]),
        ],
    ),
    "es": dict(
        lang="es", path="es/", title="Política de seguridad", other="Política de privacidad",
        description="Cómo informar de un problema de seguridad en Pauca, Pauca Lite o este sitio, qué está cubierto y qué esperar.",
        sub="Pauca, Pauca Lite y este sitio · Actualizada el 9 de noviembre de 2026",
        sections=[
            ("Informa de una vulnerabilidad", [
                f'¿Encontraste un problema de seguridad? Escribe a <a href="mailto:{EMAIL}">{EMAIL}</a> contando lo que encontraste y los pasos para reproducirlo. Si es en la app, incluye la versión y el modelo de tu teléfono.',
                "Por favor, no abras una issue pública en GitHub sobre esto, y da un tiempo razonable para la corrección antes de publicar los detalles.",
            ]),
            ("Qué está cubierto", [
                ("Las versiones más recientes de Pauca y Pauca Lite en Google Play.",
                 f'El código fuente en <a href="{REPO}">github.com/berelels/pauca</a>.',
                 "Las páginas de cattastudios.com.br/pauca/."),
            ]),
            ("Qué esperar", [
                "Pauca lo hace una sola persona, así que no hay plazo fijo ni recompensa en dinero. Los informes se leen y los válidos se corrigen lo antes posible. Si quieres, recibes el crédito cuando salga la corrección.",
            ]),
            ("Cómo está hecho Pauca", [
                ("Las apps no tienen permiso de internet, así que no pueden enviar datos por su cuenta.",
                 "Tus tarjetas y ajustes se quedan en tu teléfono. El paso de Lite a Pauca ocurre en el teléfono, y solo las apps firmadas por el mismo desarrollador pueden leer esos datos.",
                 "Los permisos sensibles, como la accesibilidad y el acceso a las notificaciones, son opcionales y los activas tú. La política de privacidad explica cada uno.",
                 "Las claves de firma se guardan fuera del repositorio público.",
                 "Este sitio está hecho de archivos estáticos, sin formularios, cuentas, cookies ni scripts de terceros."),
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
code { font-size: .9em; }
.also { margin-top: 44px; color: var(--muted); font-size: 15px; }
"""

EYE = ('<svg class="mark" viewBox="115 209 282 174" fill="none" stroke="currentColor" stroke-width="34" '
       'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M132 226c40 62 84 90 124 '
       '90s84-28 124-90M166 290l-26 40M256 318v48M346 290l26 40"/></svg>')
NAV = [("en", "English", ""), ("pt", "Português", "pt/"), ("es", "Español", "es/")]


def render(key: str, page: dict, base: str) -> str:
    depth = "../" if page["path"] else ""
    home = "../../" if page["path"] else "../"
    nav = (f'<a href="{home}{page["path"]}" style="margin-right:auto">← Pauca</a> '
           + " ".join(f"<span>{label}</span>" if k == key else f'<a href="{depth}{p}">{label}</a>' for k, label, p in NAV))
    body = []
    for i, (title, blocks, *anchor) in enumerate(page["sections"]):
        parts = [f"<h2>{html.escape(title)}</h2>"]
        for b in blocks:
            parts.append("<ul>" + "".join(f"<li>{x}</li>" for x in b) + "</ul>" if isinstance(b, tuple) else f"<p>{b}</p>")
        inner = "\n".join(parts)
        attrs = (' class="short"' if i == 0 else "") + (f' id="{anchor[0]}"' if anchor else "")
        body.append(f"<section{attrs}>{inner}</section>")
    alternates = "\n".join(f'<link rel="alternate" hreflang="{PAGES[k]["lang"]}" href="{SITE}{base}/{p}">' for k, _, p in NAV)
    alternates += f'\n<link rel="alternate" hreflang="x-default" href="{SITE}{base}/">'
    other_base = "security" if base == "privacy" else "privacy"
    return f"""<!doctype html>
<html lang="{page['lang']}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Pauca · {html.escape(page['title'])}</title>
<meta name="description" content="{html.escape(page['description'])}">
<link rel="canonical" href="{SITE}{base}/{page['path']}">
{alternates}
<link rel="icon" href="{home}assets/icon.svg" type="image/svg+xml">
<style>{CSS}</style>
</head>
<body>
<main>
<nav>{nav}</nav>
{EYE}
<h1>{html.escape(page['title'])}</h1>
<p class="sub">{html.escape(page['sub'])}</p>
{chr(10).join(body)}
<p class="also"><a href="{home}{other_base}/{page['path']}">{html.escape(page['other'])}</a></p>
</main>
</body>
</html>
"""


for base, pages in (("privacy", PAGES), ("security", SECURITY)):
    for key, page in pages.items():
        folder = os.path.join(DOCS, base, page["path"])
        os.makedirs(folder, exist_ok=True)
        with open(os.path.join(folder, "index.html"), "w") as f:
            f.write(render(key, page, base))
        print(os.path.relpath(os.path.join(folder, "index.html")))
