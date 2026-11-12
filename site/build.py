#!/usr/bin/env python3
"""
Gera a página do Pauca em docs/ (publicada pelo GitHub Pages em https://cattastudios.com.br/pauca/):
docs/index.html (inglês), docs/pt/ e docs/es/, com estilo, fontes e imagens em docs/assets/,
além de docs/sitemap.xml e docs/llms.txt. As páginas de privacidade e segurança vêm de docs/play-store/privacidade.py.

As telas vêm das capturas reais em art/store/shots/ (as mesmas da loja). Edite os textos aqui e rode:
    python3 site/build.py
"""
import hashlib
import html
import json
import os
import re
import shutil

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(ROOT, "docs")
ASSETS = os.path.join(DOCS, "assets")
SITE = "https://cattastudios.com.br/pauca/"
REPO = "https://github.com/berelels/pauca"
PLAY = "https://play.google.com/store/apps/details?id="
EMAIL = "gabrieldiasdev@proton.me"
# Vire para True quando a Play aprovar o Pauca Lite (hoje o link dá 404): religa os botões, os textos e o JSON-LD do Lite
LITE_LIVE = True
# Data da última mudança de conteúdo: atualize junto com "updated" de cada idioma
UPDATED = "2026-11-09"
# Muda a cada build para o navegador e a CDN não servirem CSS/JS antigos
VERSION = hashlib.sha1(b"".join(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), f), "rb").read() for f in ("site.css", "site.js"))).hexdigest()[:8]
COLORS = ["#7E8C54", "#C4673F", "#B8893A", "#5E7FA3", "#8E5E7E", "#C2707A"]
SHOTS = ["home_dark", "home_paper", "home_focus", "home_wall", "paper_list_serif", "paper_mono", "paper_terracotta", "drawer"]

EYE = ('<svg viewBox="115 209 282 174" fill="none" stroke="currentColor" stroke-width="34" stroke-linecap="round" '
       'stroke-linejoin="round" aria-hidden="true"><path d="M132 226c40 62 84 90 124 90s84-28 124-90M166 290l-26 40'
       'M256 318v48M346 290l26 40"/></svg>')
CHECK = ('<svg viewBox="0 0 26 26" aria-hidden="true"><circle cx="13" cy="13" r="13" fill="currentColor" opacity=".18"/>'
         '<path d="M8 13.4l3.4 3.4L18.4 9.6" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" '
         'stroke-linejoin="round"/></svg>')

T = {
    "en": dict(
        html_lang="en", path="", play_hl="en", privacy="privacy/", security="security/",
        title="Pauca: minimalist Android launcher, text only, no icons",
        description="Minimalist Android launcher: a text-only home screen with no icons or badges and a focus mode that really silences. One-time purchase, no internet permission.",
        updated="Page updated November 9, 2026", offer=("0.99", "USD"),
        nav=["Why", "Features", "Pricing", "FAQ"], get="Get Pauca", try_lite="Try Pauca Lite", see_lite="See Pauca Lite, free", price="US$ 0.99",
        eyebrow="A minimalist launcher for Android",
        h1="Your phone, quieter.",
        lead="Pauca is a minimalist Android launcher that turns your home screen into a calm list of words. No icons, no red badges, nothing pulling at you. Just the apps you need, with the names you choose.",
        fine="One-time purchase · No subscription · No ads · No internet permission",
        why_eyebrow="Why Pauca",
        why_h2="Your phone was built to be picked up. Pauca is built to let you put it down.",
        why_p=["Bright icons, red dots and endless feeds are designed to catch your eye. Pauca takes them away. What's left is a quiet page of words: you open what you came for, and that's it.",
               "The name comes from the Latin <em>pauca</em>, “few things”, as in Gauss's motto: <em>pauca sed matura</em>, few, but ripe. A phone with fewer things on it, chosen well."],
        steps_eyebrow="How it works",
        steps_h2="Three steps to a calmer phone.",
        steps=[("Choose your few", "Put the apps you really use in cards. Everything else stays one swipe away."),
               ("Name them your way", "“mom” instead of a messaging app, “bank” instead of a long name. Lowercase, as typed or UPPERCASE."),
               ("Swipe for the rest", "Swipe up to search all your apps, down for notifications. Double tap to lock the screen.")],
        focus_eyebrow="Focus mode",
        focus_h2="Focus that really silences.",
        focus_p="One tap and your phone goes quiet. Or let a profile do it: switch to “Night” and focus starts on its own.",
        focus_list=["Do Not Disturb with its own rule, and you choose who can still call",
                    "Notifications held, with a summary when you're back",
                    "A drawer with only the apps you allow",
                    "Other apps paused: open one and you're back home",
                    "A grayscale screen, if you want it"],
        yours_eyebrow="Appearance",
        yours_h2="Make it yours.",
        yours_p="Five themes, any accent color, and your own fonts, sizes, weight and alignment. Hide the clock, the buttons, even the status bar.",
        swatch_label='Try an accent color', swatch_names=['Olive', 'Terracotta', 'Ochre', 'Blue', 'Plum', 'Rose'],
        wall_eyebrow="Wallpaper",
        wall_h2="Your photo, softened.",
        wall_p="Use any photo as the background, with blur and brightness, so it stays in the background. On the lock screen too, with its own settings.",
        more_h2="And everything else you'd expect.",
        more=[("Profiles", "Personal, Work, Night: each with its own cards, one tap away."),
              ("A real app drawer", "Search, categories and a floating search button. No icons."),
              ("Gestures", "Swipe up, swipe down, swipe sideways to open an app you choose."),
              ("Guided tour", "A short tour shows every part of the screen the first time."),
              ("Three languages", "English, Portuguese and Spanish."),
              ("Private space", "Works with Android's private space and hidden apps.")],
        private_eyebrow="Privacy",
        private_h2="Private by design, not by promise.",
        private_p="Pauca has no internet permission. It isn't that we choose not to send your data: the app simply can't. No accounts, no analytics, no ads, no trackers.",
        private_link="Read the privacy policy",
        pricing_eyebrow="Pricing",
        pricing_h2="Start free. Keep it forever.",
        pricing_p="Try the essentials with Pauca Lite. When you want everything, Pauca is a single purchase.",
        pricing_h2_soon="One payment. Yours to keep.",
        pricing_p_soon="Pauca is a single purchase. Pauca Lite, the free version with the essentials, is on its way to Google Play.",
        lite_soon="Coming soon to Google Play",
        lite_note=("Pauca Lite is coming soon to Google Play.", "Pauca Lite is on Google Play now."),
        lite_price="Free", full_price="US$ 0.99", per_full="one-time, shown in your currency on Google Play", per_lite="forever",
        lite_list=["App cards with your own names", "Drawer with search and categories", "Gestures for apps and notifications",
                   "Two profiles", "Light and dark themes", "Olive, blue or no accent color"],
        full_intro="Everything in Lite, plus:",
        full_list=["Focus mode that really silences", "Unlimited profiles", "Five themes, your own photo too",
                   "Every accent color, or your own", "Fonts, weight and alignment", "Hide buttons, clock and status bar", "Double tap to lock"],
        get_lite="Get Pauca Lite",
        carry="Started with Lite? When you install Pauca, your cards and settings come along.",
        open_eyebrow="Open source",
        open_h2="Open, and made with care.",
        open_p=f'Pauca is open source under the GPLv3, built on Olauncher by Tanuj. Made in Brazil by Gabriel Dias.',
        open_btn="See the code",
        faq_h2="Questions",
        faq=[("What is Pauca?", "Pauca is a minimalist launcher for Android. It replaces your home screen with a calm, text-only list of the apps you choose, with no icons, badges or widgets, and adds a focus mode that really silences your phone. It's made by Gabriel Dias, of Catta Studios, and it's open source under the GPLv3."),
             ("Does Pauca turn my phone into a dumbphone?", "In spirit, yes: your home screen becomes as simple as a dumbphone's, only words. But it's still your Android phone, and every app stays one swipe away unless you pause it in focus mode."),
             ("How much does Pauca cost?", "US$ 0.99 as a one-time purchase on Google Play, which shows the price in your currency. There's no subscription and no ads, and updates are included. {lite}"),
             ("Does Pauca collect my data?", "No. Pauca has no internet permission, so it can't send anything anywhere. There are no accounts, analytics, ads or trackers, and your cards and settings stay on your phone."),
             ("Will it work on my phone?", "Pauca works on Android 7 or newer, on any brand, including Samsung Galaxy and Google Pixel."),
             ("Can I go back to my old home screen?", "Anytime. Pauca is just a home screen app: choose another one in your phone's settings, or uninstall it. Nothing else on your phone changes."),
             ("Does it block my apps?", "Only if you want. In focus mode, Pauca can pause the apps outside your list. Outside focus, every app is one swipe away."),
             ("What's the difference between Pauca and Pauca Lite?", "Lite is free and covers the essentials. Pauca adds focus mode, unlimited profiles and full customization, for a one-time price. If you start with Lite, Pauca brings your setup along. {lite}"),
             ("Is there a subscription?", "No. You pay once, and updates come with it."),
             ("Does it support widgets or icon packs?", "No, on purpose. Pauca is about having fewer things on your screen."),
             ("Why does it ask for accessibility?", "Only Pauca asks, Pauca Lite doesn't. It's only to lock the screen with a double tap and, in focus mode, to pause apps outside your list. It's optional and it doesn't read your screen.")],
        final_h2="Fewer things. Better ones.",
        footer_privacy="Privacy policy", footer_security="Security", footer_cookies="Cookies", footer_code="Source code", footer_license="License (GPLv3)",
        notice_label="Cookies", notice_close="Close notice", notice_link="Privacy policy",
        notice="No cookies, no trackers. Your browser only keeps two small notes, so the opening animation and this notice don't repeat.",
        alt=dict(
            paper_terracotta="Pauca in the light Paper theme: a large terracotta clock above a plain list of app names",
            home_dark="Pauca home screen in the dark theme: clock, date and two cards listing apps by name, with no icons",
            home_paper="Pauca home screen in the light Paper theme: clock, date and two cards of app names",
            home_focus="Pauca in focus mode: a label saying when focus started and one card with only phone, messages and maps",
            paper_list_serif="Pauca with centered serif app names and no clock",
            paper_mono="Pauca with centered app names in uppercase monospace below the clock",
            home_wall="Pauca with a blurred sunset photo as wallpaper behind the clock and the cards of app names"),
        legal="Google Play is a trademark of Google LLC. Pauca is not affiliated with Google.",
    ),
    "pt": dict(
        html_lang="pt-BR", path="pt/", play_hl="pt_BR", privacy="privacy/pt/", security="security/pt/",
        title="Pauca: launcher minimalista para Android, só texto, sem ícones",
        description="Launcher minimalista para Android: tela inicial só de palavras, sem ícones nem bolinhas, com modo foco de verdade. Compra única, sem permissão de internet.",
        updated="Página atualizada em 9 de novembro de 2026", offer=("4.99", "BRL"),
        nav=["Por quê", "Recursos", "Preço", "Dúvidas"], get="Comprar o Pauca", try_lite="Experimentar o Lite", see_lite="Conhecer o Lite, grátis", price="R$ 4,99",
        eyebrow="Um launcher minimalista para Android",
        h1="Seu celular, mais calmo.",
        lead="O Pauca é um launcher minimalista para Android que transforma a sua tela inicial numa lista calma de palavras. Sem ícones, sem bolinhas vermelhas, nada chamando sua atenção. Só os apps que você usa, com os nomes que você escolher.",
        fine="Compra única · Sem assinatura · Sem anúncios · Sem permissão de internet",
        why_eyebrow="Por que o Pauca",
        why_h2="O celular foi feito para ser pego na mão. O Pauca foi feito para você poder largá-lo.",
        why_p=["Ícones coloridos, bolinhas vermelhas e feeds sem fim são desenhados para chamar o seu olhar. O Pauca tira tudo isso. O que sobra é uma página calma de palavras: você abre o que veio buscar, e pronto.",
               "O nome vem do latim <em>pauca</em>, “poucas coisas”, como no lema de Gauss: <em>pauca sed matura</em>, poucas, mas maduras. Um celular com menos coisas, bem escolhidas."],
        steps_eyebrow="Como funciona",
        steps_h2="Três passos para um celular mais calmo.",
        steps=[("Escolha os poucos", "Coloque nos cartões os apps que você usa de verdade. O resto fica a um deslize."),
               ("Dê os seus nomes", "“mãe” em vez do app de mensagens, “banco” em vez de um nome comprido. Em minúsculas, como escrito ou MAIÚSCULAS."),
               ("Deslize para o resto", "Para cima, você busca entre todos os apps; para baixo, as notificações. Toque duplo bloqueia a tela.")],
        focus_eyebrow="Modo foco",
        focus_h2="Foco que silencia de verdade.",
        focus_p="Um toque e o celular fica quieto. Ou deixe um perfil fazer isso: troque para “Noite” e o foco começa sozinho.",
        focus_list=["Não Perturbe com uma regra própria, e você escolhe quem ainda pode ligar",
                    "Notificações seguradas, com um resumo quando você volta",
                    "Uma gaveta só com os apps permitidos",
                    "Os outros apps pausados: abriu um, voltou para o início",
                    "Tela em tons de cinza, se você quiser"],
        yours_eyebrow="Aparência",
        yours_h2="Do seu jeito.",
        yours_p="Cinco temas, qualquer cor de destaque, e as suas fontes, tamanhos, peso e alinhamento. Esconda o relógio, os botões e até a barra de status.",
        swatch_label='Experimente uma cor de destaque', swatch_names=['Oliva', 'Terracota', 'Ocre', 'Azul', 'Ameixa', 'Rosa'],
        wall_eyebrow="Papel de parede",
        wall_h2="Sua foto, suavizada.",
        wall_p="Use qualquer foto como fundo, com desfoque e brilho, para ela ficar no fundo mesmo. Também na tela de bloqueio, com ajustes próprios.",
        more_h2="E tudo o mais que você espera.",
        more=[("Perfis", "Pessoal, Trabalho, Noite: cada um com os seus cartões, a um toque."),
              ("Uma gaveta de verdade", "Busca, categorias e uma lupa flutuante. Sem ícones."),
              ("Gestos", "Deslize para cima, para baixo e para os lados para abrir um app que você escolher."),
              ("Tour guiado", "Um tour rápido mostra cada parte da tela na primeira vez."),
              ("Três idiomas", "Português, inglês e espanhol."),
              ("Espaço privado", "Funciona com o espaço privado do Android e com apps ocultos.")],
        private_eyebrow="Privacidade",
        private_h2="Privado por projeto, não por promessa.",
        private_p="O Pauca não tem permissão de internet. Não é que a gente escolha não enviar seus dados: o app simplesmente não consegue. Sem contas, sem estatísticas, sem anúncios, sem rastreadores.",
        private_link="Ler a política de privacidade",
        pricing_eyebrow="Preço",
        pricing_h2="Comece de graça. Fique para sempre.",
        pricing_p="Experimente o essencial com o Pauca Lite. Quando quiser tudo, o Pauca é uma compra só.",
        pricing_h2_soon="Um pagamento. É seu para sempre.",
        pricing_p_soon="O Pauca é uma compra só. O Pauca Lite, a versão grátis com o essencial, está a caminho da Google Play.",
        lite_soon="Em breve na Google Play",
        lite_note=("O Pauca Lite chega em breve à Google Play.", "O Pauca Lite já está na Google Play."),
        lite_price="Grátis", full_price="R$ 4,99", per_full="pagamento único", per_lite="para sempre",
        lite_list=["Cartões de apps com os seus nomes", "Gaveta com busca e categorias", "Gestos para apps e notificações",
                   "Dois perfis", "Temas claro e escuro", "Cor oliva, azul ou nenhuma"],
        full_intro="Tudo do Lite, e mais:",
        full_list=["Modo foco que silencia de verdade", "Perfis ilimitados", "Cinco temas, até a sua foto",
                   "Todas as cores de destaque, ou a sua", "Fontes, peso e alinhamento", "Esconda botões, relógio e barra de status", "Toque duplo para bloquear"],
        get_lite="Baixar o Pauca Lite",
        carry="Começou pelo Lite? Quando você instala o Pauca, seus cartões e ajustes vão junto.",
        open_eyebrow="Código aberto",
        open_h2="Aberto, e feito com cuidado.",
        open_p="O Pauca é código aberto, sob a GPLv3, feito a partir do Olauncher, de Tanuj. Feito no Brasil por Gabriel Dias.",
        open_btn="Ver o código",
        faq_h2="Dúvidas",
        faq=[("O que é o Pauca?", "O Pauca é um launcher minimalista para Android. Ele troca a sua tela inicial por uma lista calma, só de texto, com os apps que você escolher, sem ícones, bolinhas nem widgets, e traz um modo foco que silencia o celular de verdade. É feito por Gabriel Dias, da Catta Studios, e tem código aberto sob a GPLv3."),
             ("O Pauca transforma meu celular num dumbphone?", "Na ideia, sim: a tela inicial fica simples como a de um dumbphone, só com palavras. Mas continua sendo o seu Android, e todo app segue a um deslize, a não ser que você o pause no modo foco."),
             ("Quanto custa o Pauca?", "R$ 4,99 no Brasil, em compra única na Google Play. Não tem assinatura nem anúncios, e as atualizações vêm junto. {lite}"),
             ("O Pauca coleta meus dados?", "Não. O Pauca não tem permissão de internet, então não consegue enviar nada para lugar nenhum. Não tem contas, estatísticas, anúncios nem rastreadores, e seus cartões e ajustes ficam no seu celular."),
             ("Funciona no meu celular?", "O Pauca funciona no Android 7 ou mais novo, de qualquer marca, inclusive Samsung Galaxy e Google Pixel."),
             ("Dá para voltar para a minha tela inicial de antes?", "A qualquer momento. O Pauca é só um app de tela inicial: escolha outro nos ajustes do celular, ou desinstale. Nada mais no seu celular muda."),
             ("Ele bloqueia meus apps?", "Só se você quiser. No modo foco, o Pauca pode pausar os apps fora da sua lista. Fora do foco, todo app está a um deslize."),
             ("Qual a diferença entre o Pauca e o Pauca Lite?", "O Lite é grátis e cobre o essencial. O Pauca traz o modo foco, perfis ilimitados e toda a personalização, por um preço único. Se você começar pelo Lite, o Pauca leva a sua configuração junto. {lite}"),
             ("Tem assinatura?", "Não. Você paga uma vez, e as atualizações vêm junto."),
             ("Tem widgets ou pacotes de ícones?", "Não, de propósito. O Pauca é sobre ter menos coisas na tela."),
             ("Por que ele pede acessibilidade?", "Só o Pauca pede, o Pauca Lite não. É só para bloquear a tela com o toque duplo e, no modo foco, pausar os apps fora da sua lista. É opcional e não lê a sua tela.")],
        final_h2="Menos coisas. Melhores.",
        footer_privacy="Política de privacidade", footer_security="Segurança", footer_cookies="Cookies", footer_code="Código-fonte", footer_license="Licença (GPLv3)",
        notice_label="Cookies", notice_close="Fechar aviso", notice_link="Política de privacidade",
        notice="Sem cookies e sem rastreadores. Seu navegador só guarda duas anotações pequenas, para a abertura e este aviso não se repetirem.",
        alt=dict(
            paper_terracotta="Pauca no tema claro Papel: um relógio grande em terracota sobre uma lista simples de nomes de apps",
            home_dark="Tela inicial do Pauca no tema escuro: relógio, data e dois cartões com os apps por nome, sem ícones",
            home_paper="Tela inicial do Pauca no tema claro Papel: relógio, data e dois cartões com nomes de apps",
            home_focus="Pauca no modo foco: um aviso de quando o foco começou e um cartão só com telefone, mensagens e mapas",
            paper_list_serif="Pauca com nomes de apps centralizados em fonte serifada e sem relógio",
            paper_mono="Pauca com nomes de apps centralizados, em maiúsculas e fonte monoespaçada, abaixo do relógio",
            home_wall="Pauca com uma foto de pôr do sol desfocada como fundo, atrás do relógio e dos cartões de apps"),
        legal="Google Play é uma marca da Google LLC. O Pauca não tem ligação com o Google.",
    ),
    "es": dict(
        html_lang="es", path="es/", play_hl="es", privacy="privacy/es/", security="security/es/",
        title="Pauca: launcher minimalista para Android, solo texto, sin iconos",
        description="Launcher minimalista para Android: pantalla de inicio solo con palabras, sin iconos ni globos y con concentración real. Pago único, sin permiso de internet.",
        updated="Página actualizada el 9 de noviembre de 2026", offer=("0.99", "USD"),
        nav=["Por qué", "Funciones", "Precio", "Preguntas"], get="Comprar Pauca", try_lite="Probar Pauca Lite", see_lite="Ver Pauca Lite, gratis", price="US$ 0,99",
        eyebrow="Un launcher minimalista para Android",
        h1="Tu teléfono, más tranquilo.",
        lead="Pauca es un launcher minimalista para Android que convierte tu pantalla de inicio en una lista tranquila de palabras. Sin iconos, sin globos rojos, nada que te distraiga. Solo las apps que usas, con los nombres que elijas.",
        fine="Pago único · Sin suscripción · Sin anuncios · Sin permiso de internet",
        why_eyebrow="Por qué Pauca",
        why_h2="Tu teléfono está hecho para que lo tomes. Pauca está hecho para que puedas dejarlo.",
        why_p=["Los iconos brillantes, los globos rojos y los feeds infinitos están diseñados para atrapar tu mirada. Pauca los quita. Lo que queda es una página tranquila de palabras: abres lo que viniste a buscar, y listo.",
               "El nombre viene del latín <em>pauca</em>, “pocas cosas”, como en el lema de Gauss: <em>pauca sed matura</em>, pocas, pero maduras. Un teléfono con menos cosas, bien elegidas."],
        steps_eyebrow="Cómo funciona",
        steps_h2="Tres pasos hacia un teléfono más tranquilo.",
        steps=[("Elige las pocas", "Pon en tarjetas las apps que de verdad usas. El resto queda a un deslizamiento."),
               ("Ponles tus nombres", "“mamá” en lugar de la app de mensajes, “banco” en lugar de un nombre largo. En minúsculas, como lo escribas o en MAYÚSCULAS."),
               ("Desliza para el resto", "Hacia arriba buscas entre todas tus apps; hacia abajo, las notificaciones. Doble toque bloquea la pantalla.")],
        focus_eyebrow="Modo concentración",
        focus_h2="Concentración que silencia de verdad.",
        focus_p="Un toque y tu teléfono se queda en silencio. O deja que lo haga un perfil: cambia a “Noche” y la concentración empieza sola.",
        focus_list=["No molestar con una regla propia, y tú eliges quién puede seguir llamando",
                    "Notificaciones retenidas, con un resumen cuando vuelves",
                    "Un cajón solo con las apps permitidas",
                    "Las demás apps en pausa: si abres una, vuelves al inicio",
                    "Pantalla en escala de grises, si la quieres"],
        yours_eyebrow="Apariencia",
        yours_h2="A tu manera.",
        yours_p="Cinco temas, cualquier color de acento, y tus fuentes, tamaños, grosor y alineación. Oculta el reloj, los botones e incluso la barra de estado.",
        swatch_label='Prueba un color de acento', swatch_names=['Oliva', 'Terracota', 'Ocre', 'Azul', 'Ciruela', 'Rosa'],
        wall_eyebrow="Fondo de pantalla",
        wall_h2="Tu foto, suavizada.",
        wall_p="Usa cualquier foto de fondo, con desenfoque y brillo, para que se quede en segundo plano. También en la pantalla de bloqueo, con sus propios ajustes.",
        more_h2="Y todo lo demás que esperas.",
        more=[("Perfiles", "Personal, Trabajo, Noche: cada uno con sus tarjetas, a un toque."),
              ("Un cajón de verdad", "Búsqueda, categorías y una lupa flotante. Sin iconos."),
              ("Gestos", "Desliza hacia arriba, hacia abajo y hacia los lados para abrir la app que elijas."),
              ("Recorrido guiado", "Un recorrido breve muestra cada parte de la pantalla la primera vez."),
              ("Tres idiomas", "Español, inglés y portugués."),
              ("Espacio privado", "Funciona con el espacio privado de Android y con apps ocultas.")],
        private_eyebrow="Privacidad",
        private_h2="Privado por diseño, no por promesa.",
        private_p="Pauca no tiene permiso de internet. No es que elijamos no enviar tus datos: la app simplemente no puede. Sin cuentas, sin estadísticas, sin anuncios, sin rastreadores.",
        private_link="Leer la política de privacidad",
        pricing_eyebrow="Precio",
        pricing_h2="Empieza gratis. Quédatelo para siempre.",
        pricing_p="Prueba lo esencial con Pauca Lite. Cuando lo quieras todo, Pauca es un único pago.",
        pricing_h2_soon="Un pago. Tuyo para siempre.",
        pricing_p_soon="Pauca es un único pago. Pauca Lite, la versión gratis con lo esencial, está en camino a Google Play.",
        lite_soon="Próximamente en Google Play",
        lite_note=("Pauca Lite llegará pronto a Google Play.", "Pauca Lite ya está en Google Play."),
        lite_price="Gratis", full_price="US$ 0,99", per_full="pago único, en tu moneda en Google Play", per_lite="para siempre",
        lite_list=["Tarjetas de apps con tus nombres", "Cajón con búsqueda y categorías", "Gestos para apps y notificaciones",
                   "Dos perfiles", "Temas claro y oscuro", "Color oliva, azul o ninguno"],
        full_intro="Todo lo de Lite, y además:",
        full_list=["Concentración que silencia de verdad", "Perfiles ilimitados", "Cinco temas, incluso tu foto",
                   "Todos los colores de acento, o el tuyo", "Fuentes, grosor y alineación", "Oculta botones, reloj y barra de estado", "Doble toque para bloquear"],
        get_lite="Descargar Pauca Lite",
        carry="¿Empezaste con Lite? Cuando instalas Pauca, tus tarjetas y ajustes vienen contigo.",
        open_eyebrow="Código abierto",
        open_h2="Abierto, y hecho con cuidado.",
        open_p="Pauca es de código abierto, bajo la GPLv3, basado en Olauncher, de Tanuj. Hecho en Brasil por Gabriel Dias.",
        open_btn="Ver el código",
        faq_h2="Preguntas",
        faq=[("¿Qué es Pauca?", "Pauca es un launcher minimalista para Android. Cambia tu pantalla de inicio por una lista tranquila, solo de texto, con las apps que elijas, sin iconos, globos ni widgets, y añade una concentración que silencia tu teléfono de verdad. Lo hace Gabriel Dias, de Catta Studios, y es de código abierto bajo la GPLv3."),
             ("¿Pauca convierte mi teléfono en un dumbphone?", "En espíritu, sí: tu pantalla de inicio queda tan simple como la de un dumbphone, solo palabras. Pero sigue siendo tu Android, y todas las apps siguen a un deslizamiento, salvo que las pauses en el modo concentración."),
             ("¿Cuánto cuesta Pauca?", "US$ 0,99 en un pago único en Google Play, que muestra el precio en tu moneda. No tiene suscripción ni anuncios, y las actualizaciones vienen incluidas. {lite}"),
             ("¿Pauca recopila mis datos?", "No. Pauca no tiene permiso de internet, así que no puede enviar nada a ningún sitio. No tiene cuentas, estadísticas, anuncios ni rastreadores, y tus tarjetas y ajustes se quedan en tu teléfono."),
             ("¿Funciona en mi teléfono?", "Pauca funciona en Android 7 o más reciente, de cualquier marca, incluidos Samsung Galaxy y Google Pixel."),
             ("¿Puedo volver a mi pantalla de inicio de antes?", "Cuando quieras. Pauca es solo una app de pantalla de inicio: elige otra en los ajustes del teléfono, o desinstálala. Nada más cambia en tu teléfono."),
             ("¿Bloquea mis apps?", "Solo si quieres. En el modo concentración, Pauca puede pausar las apps fuera de tu lista. Fuera de él, todas están a un deslizamiento."),
             ("¿Qué diferencia hay entre Pauca y Pauca Lite?", "Lite es gratis y cubre lo esencial. Pauca añade el modo concentración, perfiles ilimitados y toda la personalización, por un único pago. Si empiezas con Lite, Pauca se lleva tu configuración. {lite}"),
             ("¿Tiene suscripción?", "No. Pagas una vez, y las actualizaciones vienen incluidas."),
             ("¿Tiene widgets o paquetes de iconos?", "No, a propósito. Pauca trata de tener menos cosas en la pantalla."),
             ("¿Por qué pide accesibilidad?", "Solo Pauca la pide, Pauca Lite no. Es solo para bloquear la pantalla con el doble toque y, en el modo concentración, pausar las apps fuera de tu lista. Es opcional y no lee tu pantalla.")],
        final_h2="Menos cosas. Mejores.",
        footer_privacy="Política de privacidad", footer_security="Seguridad", footer_cookies="Cookies", footer_code="Código fuente", footer_license="Licencia (GPLv3)",
        notice_label="Cookies", notice_close="Cerrar aviso", notice_link="Política de privacidad",
        notice="Sin cookies y sin rastreadores. Tu navegador solo guarda dos pequeñas notas, para que la animación de apertura y este aviso no se repitan.",
        alt=dict(
            paper_terracotta="Pauca en el tema claro Papel: un reloj grande en terracota sobre una lista sencilla de nombres de apps",
            home_dark="Pantalla de inicio de Pauca en el tema oscuro: reloj, fecha y dos tarjetas con las apps por nombre, sin iconos",
            home_paper="Pantalla de inicio de Pauca en el tema claro Papel: reloj, fecha y dos tarjetas con nombres de apps",
            home_focus="Pauca en el modo concentración: un aviso de cuándo empezó y una tarjeta solo con teléfono, mensajes y mapas",
            paper_list_serif="Pauca con nombres de apps centrados en fuente con serifa y sin reloj",
            paper_mono="Pauca con nombres de apps centrados, en mayúsculas y fuente monoespaciada, debajo del reloj",
            home_wall="Pauca con una foto de atardecer desenfocada como fondo, detrás del reloj y de las tarjetas de apps"),
        legal="Google Play es una marca de Google LLC. Pauca no está afiliada a Google.",
    ),
}


def esc(s: str) -> str:
    return html.escape(s, quote=False)


def phone(lang: str, shot: str, cls: str, alt: str, eager: bool = False) -> str:
    # As do topo carregam já; as outras, quando chegam perto da tela
    loading = 'loading="eager" fetchpriority="high"' if eager else 'loading="lazy"'
    return f'<div class="phone {cls}"><img src="{{root}}assets/img/{lang}/{shot}.webp" alt="{html.escape(alt)}" width="600" height="1333" {loading} decoding="async"></div>'


def checks(items) -> str:
    return '<ul class="checks">' + "".join(f"<li>{CHECK}<span>{esc(i)}</span></li>" for i in items) + "</ul>"


def faq_items(t: dict) -> list:
    return [(q, a.replace("{lite}", t["lite_note"][LITE_LIVE])) for q, a in t["faq"]]


def app_node(t: dict, name: str, package: str, description: str, offer: tuple) -> dict:
    return {
        "@type": "SoftwareApplication", "@id": f"{SITE}#{package}", "name": name, "description": description, "url": SITE + t["path"],
        "applicationCategory": "UtilitiesApplication", "applicationSubCategory": "Launcher", "operatingSystem": "Android 7.0 or newer",
        "inLanguage": [T[c]["html_lang"] for c in T], "installUrl": f"{PLAY}{package}&hl={t['play_hl']}",
        "image": f"{SITE}assets/og-{t['html_lang'][:2]}.png", "license": "https://www.gnu.org/licenses/gpl-3.0.html",
        "isBasedOn": "https://github.com/tanujnotes/Olauncher", "sameAs": [f"{PLAY}{package}", REPO],
        "author": {"@id": "https://cattastudios.com.br/#organization"}, "publisher": {"@id": "https://cattastudios.com.br/#organization"},
        "offers": {"@type": "Offer", "price": offer[0], "priceCurrency": offer[1], "url": f"{PLAY}{package}&hl={t['play_hl']}", "availability": "https://schema.org/InStock"},
    }


def jsonld(lang: str, t: dict) -> str:
    url = SITE + t["path"]
    full = app_node(t, "Pauca", "app.pauca", t["description"], t["offer"])
    full["featureList"] = t["full_list"]
    full["screenshot"] = [f"{SITE}assets/img/{lang}/{s}.webp" for s in ("home_dark", "home_paper", "home_focus")]
    graph = [
        {"@type": "Organization", "@id": "https://cattastudios.com.br/#organization", "name": "Catta Studios", "url": "https://cattastudios.com.br/",
         "email": EMAIL, "founder": {"@type": "Person", "name": "Gabriel Dias", "sameAs": "https://github.com/berelels"}},
        {"@type": "WebSite", "@id": f"{SITE}#website", "name": "Pauca", "url": SITE, "publisher": {"@id": "https://cattastudios.com.br/#organization"}},
        {"@type": "WebPage", "@id": f"{url}#webpage", "url": url, "name": t["title"], "description": t["description"], "inLanguage": t["html_lang"],
         "isPartOf": {"@id": f"{SITE}#website"}, "about": {"@id": f"{SITE}#app.pauca"}, "dateModified": UPDATED},
        full,
        {"@type": "FAQPage", "@id": f"{url}#faq", "inLanguage": t["html_lang"], "mainEntity": [
            {"@type": "Question", "name": q, "acceptedAnswer": {"@type": "Answer", "text": a}} for q, a in faq_items(t)]},
    ]
    if LITE_LIVE:
        graph.append(app_node(t, "Pauca Lite", "app.pauca.lite", t["pricing_p"], ("0", t["offer"][1])))
    # "</" escapado: nenhum texto pode fechar o <script> antes da hora
    return json.dumps({"@context": "https://schema.org", "@graph": graph}, ensure_ascii=False).replace("</", "<\\/")


def page(lang: str, t: dict) -> str:
    root = "../" if t["path"] else ""
    play_full = f'{PLAY}app.pauca&amp;hl={t["play_hl"]}'
    play_lite = f'{PLAY}app.pauca.lite&amp;hl={t["play_hl"]}'
    langs = " ".join(
        f"<b>{code.upper()}</b>" if code == lang else f'<a href="{root}{T[code]["path"]}" hreflang="{T[code]["html_lang"]}">{code.upper()}</a>'
        for code in T)
    alternates = "\n".join(f'<link rel="alternate" hreflang="{T[c]["html_lang"]}" href="{SITE}{T[c]["path"]}">' for c in T)
    alternates += f'\n<link rel="alternate" hreflang="x-default" href="{SITE}">'
    locales = "\n".join(f'<meta property="og:locale:alternate" content="{T[c]["html_lang"].replace("-", "_")}">' for c in T if c != lang)
    ph = lambda shot, cls, eager=False: phone(lang, shot, cls, t["alt"][shot], eager)
    # Enquanto o Lite não sai, nada aponta para o 404 da Play nem promete o que ainda não existe
    lite_cta = f'<a class="btn ghost" href="{play_lite}">{esc(t["try_lite"])}</a>' if LITE_LIVE else f'<a class="btn ghost" href="#pricing">{esc(t["see_lite"])}</a>'
    lite_plan = f'<a class="btn ghost" href="{play_lite}">{esc(t["get_lite"])}</a>' if LITE_LIVE else f'<span class="btn ghost soon">{esc(t["lite_soon"])}</span>'
    carry = f'<p class="fine">{esc(t["carry"])}</p>' if LITE_LIVE else ""
    pricing_h2, pricing_p = (t["pricing_h2"], t["pricing_p"]) if LITE_LIVE else (t["pricing_h2_soon"], t["pricing_p_soon"])
    words = t["h1"].split()
    h1 = "".join(f'<span class="w" aria-hidden="true"><span style="--i:{i}">{esc(w)}</span></span> ' for i, w in enumerate(words)).strip()
    swatches = "".join(
        f'<button type="button" class="sw" style="--c:{c}" data-color="{c}" aria-label="{esc(n)}" aria-pressed="{"true" if i == 0 else "false"}"></button>'
        for i, (c, n) in enumerate(zip(COLORS, t["swatch_names"])))
    wordmark = "".join(f'<span style="--i:{i}">{c}</span>' for i, c in enumerate("pauca"))
    body = f"""
<div class="veil" aria-hidden="true"><svg viewBox="115 209 282 174" fill="none" stroke="currentColor" stroke-width="30" stroke-linecap="round" stroke-linejoin="round"><path pathLength="1" d="M132 226c40 62 84 90 124 90s84-28 124-90M166 290l-26 40M256 318v48M346 290l26 40"/></svg></div>
<header class="top">
  <div class="progress" aria-hidden="true"></div>
  <div class="wrap">
    <a class="brand" href="{root}{t['path']}">{EYE}<span>Pauca</span></a>
    <nav>
      <a href="#why">{esc(t['nav'][0])}</a><a href="#features">{esc(t['nav'][1])}</a>
      <a href="#pricing">{esc(t['nav'][2])}</a><a href="#faq">{esc(t['nav'][3])}</a>
    </nav>
    <div class="langs">{langs}</div>
    <a class="btn primary small" href="{play_full}">{esc(t['get'])}</a>
  </div>
</header>

<main>
<section class="hero">
  <div class="wrap">
    <div class="eyebrow">{esc(t['eyebrow'])}</div>
    <h1 aria-label="{esc(t['h1'])}">{h1}</h1>
    <p class="lead">{esc(t['lead'])}</p>
    <div class="ctas">
      <a class="btn primary" href="{play_full}">{esc(t['get'])} <span class="price">· {esc(t['price'])}</span></a>
      {lite_cta}
    </div>
    <p class="fine">{esc(t['fine'])}</p>
    <div class="stage">
      {ph('paper_terracotta', 'p1', True)}{ph('home_dark', 'p2', True)}{ph('home_paper', 'p3', True)}
    </div>
  </div>
</section>


<section class="manifesto" id="why">
  <div class="wrap reveal">
    <div class="eyebrow">{esc(t['why_eyebrow'])}</div>
    <h2 style="margin-top:16px">{esc(t['why_h2'])}</h2>
    <div class="cols"><p>{t['why_p'][0]}</p><p>{t['why_p'][1]}</p></div>
  </div>
</section>

<section class="tint">
  <div class="wrap reveal">
    <div class="eyebrow">{esc(t['steps_eyebrow'])}</div>
    <h2 style="margin-top:16px">{esc(t['steps_h2'])}</h2>
    <div class="steps">
      {''.join(f'<div class="step"><div class="n">{i}</div><h3>{esc(a)}</h3><p>{esc(b)}</p></div>' for i, (a, b) in enumerate(t['steps'], 1))}
    </div>
  </div>
</section>

<section class="feature on-dark" id="features">
  <div class="wrap">
    <div class="text reveal">
      <div class="eyebrow">{esc(t['focus_eyebrow'])}</div>
      <h2>{esc(t['focus_h2'])}</h2>
      <p class="lead">{esc(t['focus_p'])}</p>
      {checks(t['focus_list'])}
    </div>
    <div class="visual reveal">{ph('home_focus', '')}</div>
  </div>
</section>

<section class="feature flip">
  <div class="wrap">
    <div class="text reveal">
      <div class="eyebrow">{esc(t['yours_eyebrow'])}</div>
      <h2>{esc(t['yours_h2'])}</h2>
      <p class="lead">{esc(t['yours_p'])}</p>
      <div class="swatches" role="group" aria-label="{esc(t['swatch_label'])}">
        <p class="sw-label">{esc(t['swatch_label'])}</p>{swatches}
      </div>
    </div>
    <div class="visual trio reveal">{ph('paper_list_serif', 'a')}{ph('paper_terracotta', 'b')}{ph('paper_mono', 'c')}</div>
  </div>
</section>

<section class="feature tint">
  <div class="wrap">
    <div class="text reveal">
      <div class="eyebrow">{esc(t['wall_eyebrow'])}</div>
      <h2>{esc(t['wall_h2'])}</h2>
      <p class="lead">{esc(t['wall_p'])}</p>
    </div>
    <div class="visual reveal">{ph('home_wall', '')}</div>
  </div>
</section>

<section>
  <div class="wrap reveal">
    <h2>{esc(t['more_h2'])}</h2>
    <div class="more">{''.join(f'<div><h3>{esc(a)}</h3><p>{esc(b)}</p></div>' for a, b in t['more'])}</div>
  </div>
</section>

<section class="private on-olive">
  <div class="wrap reveal">
    <div class="eyebrow">{esc(t['private_eyebrow'])}</div>
    <h2>{esc(t['private_h2'])}</h2>
    <p>{esc(t['private_p'])}</p>
    <a class="link" href="{root}{t['privacy']}">{esc(t['private_link'])} →</a>
  </div>
</section>

<section class="pricing" id="pricing">
  <div class="wrap reveal">
    <div class="eyebrow" style="text-align:center">{esc(t['pricing_eyebrow'])}</div>
    <h2>{esc(pricing_h2)}</h2>
    <p class="lead">{esc(pricing_p)}</p>
    <div class="plans">
      <div class="plan">
        <h3>Pauca <span class="tag">LITE</span></h3>
        <div class="amount">{esc(t['lite_price'])}</div><div class="per">{esc(t['per_lite'])}</div>
        {checks(t['lite_list'])}
        {lite_plan}
      </div>
      <div class="plan full">
        <h3>Pauca</h3>
        <div class="amount">{esc(t['full_price'])}</div><div class="per">{esc(t['per_full'])}</div>
        <p style="margin:24px 0 -12px;font-weight:600">{esc(t['full_intro'])}</p>
        {checks(t['full_list'])}
        <a class="btn primary" href="{play_full}">{esc(t['get'])}</a>
      </div>
    </div>
    {carry}
  </div>
</section>

<section class="open tint">
  <div class="wrap reveal">
    <div>
      <div class="eyebrow">{esc(t['open_eyebrow'])}</div>
      <h2 style="margin-top:16px">{esc(t['open_h2'])}</h2>
      <p>{esc(t['open_p'])}</p>
    </div>
    <div><a class="btn ghost" href="{REPO}">{esc(t['open_btn'])} →</a></div>
  </div>
</section>

<section class="faq" id="faq">
  <div class="wrap reveal">
    <h2>{esc(t['faq_h2'])}</h2>
    {''.join(f'<details><summary>{esc(q)}</summary><p>{esc(a)}</p></details>' for q, a in faq_items(t))}
  </div>
</section>

<section class="final on-dark">
  <div class="wrap reveal">
    <h2>{esc(t['final_h2'])}</h2>
    <div class="ctas">
      <a class="btn primary" href="{play_full}">{esc(t['get'])} <span class="price">· {esc(t['price'])}</span></a>
      {lite_cta}
    </div>
  </div>
</section>
</main>

<footer>
  <div class="wordmark" aria-hidden="true">{wordmark}</div>
  <div class="wrap">
    <a class="brand" href="{root}{t['path']}">{EYE}<span>Pauca</span></a>
    <a href="{root}{t['privacy']}">{esc(t['footer_privacy'])}</a>
    <a href="{root}{t['privacy']}#cookies">{esc(t['footer_cookies'])}</a>
    <a href="{root}{t['security']}">{esc(t['footer_security'])}</a>
    <a href="{REPO}">{esc(t['footer_code'])}</a>
    <a href="{REPO}/blob/main/LICENSE">{esc(t['footer_license'])}</a>
    <div class="legal">{esc(t['legal'])} © 2026 Gabriel Dias · Catta Studios. {esc(t['updated'])}.</div>
  </div>
</footer>

<aside class="notice" aria-label="{esc(t['notice_label'])}">
  <p>{esc(t['notice'])} <a href="{root}{t['privacy']}#cookies">{esc(t['notice_link'])}</a></p>
  <button type="button" aria-label="{html.escape(t['notice_close'])}">×</button>
</aside>
"""
    body = body.replace("{root}", root)
    return f"""<!doctype html>
<html lang="{t['html_lang']}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{esc(t['title'])}</title>
<meta name="description" content="{html.escape(t['description'])}">
<link rel="canonical" href="{SITE}{t['path']}">
{alternates}
<meta name="author" content="Gabriel Dias">
<meta property="og:type" content="website">
<meta property="og:site_name" content="Pauca">
<meta property="og:url" content="{SITE}{t['path']}">
<meta property="og:locale" content="{t['html_lang'].replace('-', '_')}">
{locales}
<meta property="og:title" content="{html.escape(t['title'])}">
<meta property="og:description" content="{html.escape(t['description'])}">
<meta property="og:image" content="{SITE}assets/og-{lang}.png">
<meta property="og:image:width" content="1024">
<meta property="og:image:height" content="500">
<meta property="og:image:alt" content="{html.escape(t['alt']['home_dark'])}">
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="{html.escape(t['title'])}">
<meta name="twitter:description" content="{html.escape(t['description'])}">
<meta name="twitter:image" content="{SITE}assets/og-{lang}.png">
<meta name="theme-color" content="#F4EFE4">
<link rel="icon" href="{root}assets/icon.svg" type="image/svg+xml">
<link rel="apple-touch-icon" href="{root}assets/apple-touch-icon.png">
<link rel="preload" href="{root}assets/fonts/newsreader.woff2" as="font" type="font/woff2" crossorigin>
<link rel="stylesheet" href="{root}assets/site.css?v={VERSION}">
<script type="application/ld+json">{jsonld(lang, t)}</script>
<script>document.documentElement.classList.add("js");try{{if(localStorage.getItem("pauca-notice"))document.documentElement.classList.add("notice-off")}}catch(e){{}}try{{if(!location.hash&&!sessionStorage.getItem("pauca-veil")&&!matchMedia("(prefers-reduced-motion: reduce)").matches)document.documentElement.classList.add("veil-on")}}catch(e){{}}</script>
</head>
<body>
{body}
<script src="{root}assets/site.js?v={VERSION}" defer></script>
</body>
</html>
"""



ICON_SVG = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><rect width="512" height="512" rx="112" fill="#7E8C54"/>
<g fill="none" stroke="#FBF3E6" stroke-width="34" stroke-linecap="round" stroke-linejoin="round" transform="translate(-6 -30) scale(1.02)">
<path d="M132 226c40 62 84 90 124 90s84-28 124-90M166 290l-26 40M256 318v48M346 290l26 40"/></g></svg>
"""


def assets():
    os.makedirs(os.path.join(ASSETS, "fonts"), exist_ok=True)
    # WOFF2 geradas a partir de app/src/main/res/font/*.ttf (mesmas fontes, menos da metade do peso)
    for f in ("jakarta.woff2", "newsreader.woff2"):
        shutil.copy(os.path.join(ROOT, "site/fonts", f), os.path.join(ASSETS, "fonts", f))
    shutil.copy(os.path.join(ROOT, "site/site.css"), os.path.join(ASSETS, "site.css"))
    shutil.copy(os.path.join(ROOT, "site/site.js"), os.path.join(ASSETS, "site.js"))
    open(os.path.join(ASSETS, "icon.svg"), "w").write(ICON_SVG)
    Image.open(os.path.join(ROOT, "fastlane/metadata/android/en-US/images/icon.png")).resize((180, 180), Image.LANCZOS) \
        .save(os.path.join(ASSETS, "apple-touch-icon.png"), optimize=True)
    for lang, loc in (("en", "en-US"), ("pt", "pt-BR"), ("es", "es-ES")):
        shutil.copy(os.path.join(ROOT, f"fastlane/metadata/android/{loc}/images/featureGraphic.png"), os.path.join(ASSETS, f"og-{lang}.png"))
        out = os.path.join(ASSETS, "img", lang)
        os.makedirs(out, exist_ok=True)
        for shot in SHOTS:
            im = Image.open(os.path.join(ROOT, "art/store/shots", lang, f"{shot}.png")).convert("RGB")
            im.resize((600, round(600 * im.height / im.width)), Image.LANCZOS).save(os.path.join(out, f"{shot}.webp"), quality=82, method=6)


def sitemap() -> str:
    # shortcut: fica em /pauca/ e só vale para URLs daqui; os buscadores só o acham com o envio no Search Console ou uma linha no robots.txt da raiz do domínio
    urls = []
    for section in ("", "privacy/", "security/"):
        links = "".join(f'<xhtml:link rel="alternate" hreflang="{T[c]["html_lang"]}" href="{SITE}{section}{T[c]["path"]}"/>' for c in T)
        links += f'<xhtml:link rel="alternate" hreflang="x-default" href="{SITE}{section}"/>'
        urls += [f'<url><loc>{SITE}{section}{T[c]["path"]}</loc><lastmod>{UPDATED}</lastmod>{links}</url>' for c in T]
    return ('<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" '
            'xmlns:xhtml="http://www.w3.org/1999/xhtml">\n' + "\n".join(urls) + "\n</urlset>\n")


def llms() -> str:
    t = T["en"]
    faq = "\n\n".join(f"### {q}\n{a}" for q, a in faq_items(t))
    lite = "free, on Google Play" if LITE_LIVE else "free, not yet published on Google Play"
    return f"""# Pauca

> Pauca is a minimalist launcher for Android: a calm, text-only home screen with no icons, badges or widgets, plus a focus mode that really silences the phone. Made in Brazil by Gabriel Dias (Catta Studios). Open source under the GPLv3, based on Olauncher.

## Facts

- Platform: Android 7.0 or newer, any brand. Languages: English, Portuguese and Spanish.
- Pauca: one-time purchase on Google Play, R$ 4.99 in Brazil and US$ 0.99 in the United States. No subscription, no ads.
- Pauca Lite: {lite}. It has the essentials and doesn't use accessibility or double tap to lock.
- Privacy: the apps have no internet permission and collect no data. No accounts, analytics or trackers.
- Accessibility: optional and only in Pauca, to lock the screen with a double tap and to pause apps outside your list in focus mode.

## Pages

- [Pauca (English)]({SITE})
- [Pauca (Português)]({SITE}pt/)
- [Pauca (Español)]({SITE}es/)
- [Privacy policy]({SITE}privacy/)
- [Security policy]({SITE}security/)
- [Pauca on Google Play]({PLAY}app.pauca)
- [Source code]({REPO})

## Questions

{faq}
"""


def check(lang: str, doc: str):
    data = json.loads(re.search(r'<script type="application/ld\+json">(.*?)</script>', doc, re.S).group(1))
    assert {"Organization", "WebSite", "WebPage", "SoftwareApplication", "FAQPage"} <= {n["@type"] for n in data["@graph"]}
    assert len(next(n for n in data["@graph"] if n["@type"] == "FAQPage")["mainEntity"]) == len(T[lang]["faq"])
    assert "aggregateRating" not in doc and "{lite}" not in doc
    assert LITE_LIVE or "app.pauca.lite" not in doc


def main():
    assets()
    for lang, t in T.items():
        folder = os.path.join(DOCS, t["path"])
        os.makedirs(folder, exist_ok=True)
        doc = page(lang, t)
        check(lang, doc)
        with open(os.path.join(folder, "index.html"), "w") as f:
            f.write(doc)
        print(os.path.relpath(os.path.join(folder, "index.html"), ROOT))
    for name, text in (("sitemap.xml", sitemap()), ("llms.txt", llms())):
        with open(os.path.join(DOCS, name), "w") as f:
            f.write(text)
        print(os.path.join("docs", name))


if __name__ == "__main__":
    main()
