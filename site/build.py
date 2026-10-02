#!/usr/bin/env python3
"""
Gera a página do Pauca em docs/ (publicada pelo GitHub Pages em https://berelels.github.io/pauca/):
docs/index.html (inglês), docs/pt/ e docs/es/, com estilo, fontes e imagens em docs/assets/.

As telas vêm das capturas reais em art/store/shots/ (as mesmas da loja). Edite os textos aqui e rode:
    python3 site/build.py
"""
import html
import os
import shutil

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(ROOT, "docs")
ASSETS = os.path.join(DOCS, "assets")
SITE = "https://berelels.github.io/pauca/"
REPO = "https://github.com/berelels/pauca"
PLAY = "https://play.google.com/store/apps/details?id="
SHOTS = ["home_dark", "home_paper", "home_focus", "home_wall", "paper_list_serif", "paper_mono", "paper_terracotta", "drawer"]

EYE = ('<svg viewBox="115 209 282 174" fill="none" stroke="currentColor" stroke-width="34" stroke-linecap="round" '
       'stroke-linejoin="round" aria-hidden="true"><path d="M132 226c40 62 84 90 124 90s84-28 124-90M166 290l-26 40'
       'M256 318v48M346 290l26 40"/></svg>')
CHECK = ('<svg viewBox="0 0 26 26" aria-hidden="true"><circle cx="13" cy="13" r="13" fill="currentColor" opacity=".18"/>'
         '<path d="M8 13.4l3.4 3.4L18.4 9.6" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" '
         'stroke-linejoin="round"/></svg>')

T = {
    "en": dict(
        html_lang="en", path="", play_hl="en", privacy="privacy/",
        title="Pauca · A quieter Android home screen",
        description="Pauca turns your Android home screen into a calm list of words. No icons, no badges, and a focus mode that really silences. One-time purchase, no internet permission.",
        nav=["Why", "Features", "Pricing", "FAQ"], get="Get Pauca", try_lite="Try Pauca Lite", price="US$ 1",
        eyebrow="A minimalist launcher for Android",
        h1="Your phone, quieter.",
        lead="Pauca turns your home screen into a calm list of words. No icons, no red badges, nothing pulling at you. Just the apps you need, with the names you choose.",
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
        lite_price="Free", full_price="US$ 1", per_full="one-time, or the equivalent in your currency", per_lite="forever",
        lite_list=["App cards with your own names", "Drawer with search and categories", "Gestures and double tap to lock",
                   "Two profiles", "Light and dark themes", "Olive, blue or no accent color"],
        full_intro="Everything in Lite, plus:",
        full_list=["Focus mode that really silences", "Unlimited profiles", "Five themes, your own photo too",
                   "Every accent color, or your own", "Fonts, weight and alignment", "Hide buttons, clock and status bar"],
        get_lite="Get Pauca Lite",
        carry="Started with Lite? When you install Pauca, your cards and settings come along.",
        open_eyebrow="Open source",
        open_h2="Open, and made with care.",
        open_p=f'Pauca is open source under the GPLv3, built on Olauncher by Tanuj. Made in Brazil by Gabriel Dias.',
        open_btn="See the code",
        faq_h2="Questions",
        faq=[("Will it work on my phone?", "Pauca works on Android 7 or newer, on any brand, including Samsung Galaxy and Google Pixel."),
             ("Can I go back to my old home screen?", "Anytime. Pauca is just a home screen app: choose another one in your phone's settings, or uninstall it. Nothing else on your phone changes."),
             ("Does it block my apps?", "Only if you want. In focus mode, Pauca can pause the apps outside your list. Outside focus, every app is one swipe away."),
             ("What's the difference between Pauca and Pauca Lite?", "Lite is free and covers the essentials. Pauca adds focus mode, unlimited profiles and full customization, for a one-time price. If you start with Lite, Pauca brings your setup along."),
             ("Is there a subscription?", "No. You pay once, and updates come with it."),
             ("Does it support widgets or icon packs?", "No, on purpose. Pauca is about having fewer things on your screen."),
             ("Why does it ask for accessibility?", "Only to lock the screen with a double tap and, in focus mode, to pause apps outside your list. It's optional and it doesn't read your screen.")],
        final_h2="Fewer things. Better ones.",
        footer_privacy="Privacy policy", footer_code="Source code", footer_license="License (GPLv3)",
        legal="Google Play is a trademark of Google LLC. Pauca is not affiliated with Google.",
    ),
    "pt": dict(
        html_lang="pt-BR", path="pt/", play_hl="pt_BR", privacy="privacy/pt/",
        title="Pauca · Uma tela inicial mais calma para Android",
        description="O Pauca transforma a tela inicial do Android numa lista calma de palavras. Sem ícones, sem bolinhas vermelhas e com um modo foco que silencia de verdade. Compra única, sem permissão de internet.",
        nav=["Por quê", "Recursos", "Preço", "Dúvidas"], get="Comprar o Pauca", try_lite="Experimentar o Lite", price="R$ 5",
        eyebrow="Um launcher minimalista para Android",
        h1="Seu celular, mais calmo.",
        lead="O Pauca transforma a sua tela inicial numa lista calma de palavras. Sem ícones, sem bolinhas vermelhas, nada chamando sua atenção. Só os apps que você usa, com os nomes que você escolher.",
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
        lite_price="Grátis", full_price="R$ 5", per_full="pagamento único", per_lite="para sempre",
        lite_list=["Cartões de apps com os seus nomes", "Gaveta com busca e categorias", "Gestos e toque duplo para bloquear",
                   "Dois perfis", "Temas claro e escuro", "Cor oliva, azul ou nenhuma"],
        full_intro="Tudo do Lite, e mais:",
        full_list=["Modo foco que silencia de verdade", "Perfis ilimitados", "Cinco temas, até a sua foto",
                   "Todas as cores de destaque, ou a sua", "Fontes, peso e alinhamento", "Esconda botões, relógio e barra de status"],
        get_lite="Baixar o Pauca Lite",
        carry="Começou pelo Lite? Quando você instala o Pauca, seus cartões e ajustes vão junto.",
        open_eyebrow="Código aberto",
        open_h2="Aberto, e feito com cuidado.",
        open_p="O Pauca é código aberto, sob a GPLv3, feito a partir do Olauncher, de Tanuj. Feito no Brasil por Gabriel Dias.",
        open_btn="Ver o código",
        faq_h2="Dúvidas",
        faq=[("Funciona no meu celular?", "O Pauca funciona no Android 7 ou mais novo, de qualquer marca, inclusive Samsung Galaxy e Google Pixel."),
             ("Dá para voltar para a minha tela inicial de antes?", "A qualquer momento. O Pauca é só um app de tela inicial: escolha outro nos ajustes do celular, ou desinstale. Nada mais no seu celular muda."),
             ("Ele bloqueia meus apps?", "Só se você quiser. No modo foco, o Pauca pode pausar os apps fora da sua lista. Fora do foco, todo app está a um deslize."),
             ("Qual a diferença entre o Pauca e o Pauca Lite?", "O Lite é grátis e cobre o essencial. O Pauca traz o modo foco, perfis ilimitados e toda a personalização, por um preço único. Se você começar pelo Lite, o Pauca leva a sua configuração junto."),
             ("Tem assinatura?", "Não. Você paga uma vez, e as atualizações vêm junto."),
             ("Tem widgets ou pacotes de ícones?", "Não, de propósito. O Pauca é sobre ter menos coisas na tela."),
             ("Por que ele pede acessibilidade?", "Só para bloquear a tela com o toque duplo e, no modo foco, pausar os apps fora da sua lista. É opcional e não lê a sua tela.")],
        final_h2="Menos coisas. Melhores.",
        footer_privacy="Política de privacidade", footer_code="Código-fonte", footer_license="Licença (GPLv3)",
        legal="Google Play é uma marca da Google LLC. O Pauca não tem ligação com o Google.",
    ),
    "es": dict(
        html_lang="es", path="es/", play_hl="es", privacy="privacy/es/",
        title="Pauca · Una pantalla de inicio más tranquila para Android",
        description="Pauca convierte la pantalla de inicio de Android en una lista tranquila de palabras. Sin iconos, sin globos rojos y con una concentración que silencia de verdad. Pago único, sin permiso de internet.",
        nav=["Por qué", "Funciones", "Precio", "Preguntas"], get="Comprar Pauca", try_lite="Probar Pauca Lite", price="US$ 1",
        eyebrow="Un launcher minimalista para Android",
        h1="Tu teléfono, más tranquilo.",
        lead="Pauca convierte tu pantalla de inicio en una lista tranquila de palabras. Sin iconos, sin globos rojos, nada que te distraiga. Solo las apps que usas, con los nombres que elijas.",
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
        lite_price="Gratis", full_price="US$ 1", per_full="pago único, o su equivalente en tu moneda", per_lite="para siempre",
        lite_list=["Tarjetas de apps con tus nombres", "Cajón con búsqueda y categorías", "Gestos y doble toque para bloquear",
                   "Dos perfiles", "Temas claro y oscuro", "Color oliva, azul o ninguno"],
        full_intro="Todo lo de Lite, y además:",
        full_list=["Concentración que silencia de verdad", "Perfiles ilimitados", "Cinco temas, incluso tu foto",
                   "Todos los colores de acento, o el tuyo", "Fuentes, grosor y alineación", "Oculta botones, reloj y barra de estado"],
        get_lite="Descargar Pauca Lite",
        carry="¿Empezaste con Lite? Cuando instalas Pauca, tus tarjetas y ajustes vienen contigo.",
        open_eyebrow="Código abierto",
        open_h2="Abierto, y hecho con cuidado.",
        open_p="Pauca es de código abierto, bajo la GPLv3, basado en Olauncher, de Tanuj. Hecho en Brasil por Gabriel Dias.",
        open_btn="Ver el código",
        faq_h2="Preguntas",
        faq=[("¿Funciona en mi teléfono?", "Pauca funciona en Android 7 o más reciente, de cualquier marca, incluidos Samsung Galaxy y Google Pixel."),
             ("¿Puedo volver a mi pantalla de inicio de antes?", "Cuando quieras. Pauca es solo una app de pantalla de inicio: elige otra en los ajustes del teléfono, o desinstálala. Nada más cambia en tu teléfono."),
             ("¿Bloquea mis apps?", "Solo si quieres. En el modo concentración, Pauca puede pausar las apps fuera de tu lista. Fuera de él, todas están a un deslizamiento."),
             ("¿Qué diferencia hay entre Pauca y Pauca Lite?", "Lite es gratis y cubre lo esencial. Pauca añade el modo concentración, perfiles ilimitados y toda la personalización, por un único pago. Si empiezas con Lite, Pauca se lleva tu configuración."),
             ("¿Tiene suscripción?", "No. Pagas una vez, y las actualizaciones vienen incluidas."),
             ("¿Tiene widgets o paquetes de iconos?", "No, a propósito. Pauca trata de tener menos cosas en la pantalla."),
             ("¿Por qué pide accesibilidad?", "Solo para bloquear la pantalla con el doble toque y, en el modo concentración, pausar las apps fuera de tu lista. Es opcional y no lee tu pantalla.")],
        final_h2="Menos cosas. Mejores.",
        footer_privacy="Política de privacidad", footer_code="Código fuente", footer_license="Licencia (GPLv3)",
        legal="Google Play es una marca de Google LLC. Pauca no está afiliada a Google.",
    ),
}


def esc(s: str) -> str:
    return html.escape(s, quote=False)


def phone(lang: str, shot: str, cls: str, alt: str = "") -> str:
    return f'<div class="phone {cls}"><img src="{{root}}assets/img/{lang}/{shot}.webp" alt="{esc(alt)}" loading="lazy" decoding="async"></div>'


def checks(items) -> str:
    return '<ul class="checks">' + "".join(f"<li>{CHECK}<span>{esc(i)}</span></li>" for i in items) + "</ul>"


def page(lang: str, t: dict) -> str:
    root = "../" if t["path"] else ""
    play_full = f'{PLAY}app.pauca&amp;hl={t["play_hl"]}'
    play_lite = f'{PLAY}app.pauca.lite&amp;hl={t["play_hl"]}'
    langs = " ".join(
        f"<b>{code.upper()}</b>" if code == lang else f'<a href="{root}{T[code]["path"]}" hreflang="{T[code]["html_lang"]}">{code.upper()}</a>'
        for code in T)
    alternates = "\n".join(f'<link rel="alternate" hreflang="{T[c]["html_lang"]}" href="{SITE}{T[c]["path"]}">' for c in T)
    ph = lambda shot, cls: phone(lang, shot, cls)
    body = f"""
<header class="top">
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
    <h1>{esc(t['h1'])}</h1>
    <p class="lead">{esc(t['lead'])}</p>
    <div class="ctas">
      <a class="btn primary" href="{play_full}">{esc(t['get'])} <span class="price">· {esc(t['price'])}</span></a>
      <a class="btn ghost" href="{play_lite}">{esc(t['try_lite'])}</a>
    </div>
    <p class="fine">{esc(t['fine'])}</p>
    <div class="stage" aria-hidden="true">
      {ph('paper_terracotta', 'p1')}{ph('home_dark', 'p2')}{ph('home_paper', 'p3')}
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
    <div class="visual reveal" aria-hidden="true">{ph('home_focus', '')}</div>
  </div>
</section>

<section class="feature flip">
  <div class="wrap">
    <div class="text reveal">
      <div class="eyebrow">{esc(t['yours_eyebrow'])}</div>
      <h2>{esc(t['yours_h2'])}</h2>
      <p class="lead">{esc(t['yours_p'])}</p>
    </div>
    <div class="visual trio reveal" aria-hidden="true">{ph('paper_list_serif', 'a')}{ph('paper_terracotta', 'b')}{ph('paper_mono', 'c')}</div>
  </div>
</section>

<section class="feature tint">
  <div class="wrap">
    <div class="text reveal">
      <div class="eyebrow">{esc(t['wall_eyebrow'])}</div>
      <h2>{esc(t['wall_h2'])}</h2>
      <p class="lead">{esc(t['wall_p'])}</p>
    </div>
    <div class="visual reveal" aria-hidden="true">{ph('home_wall', '')}</div>
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
    <h2>{esc(t['pricing_h2'])}</h2>
    <p class="lead">{esc(t['pricing_p'])}</p>
    <div class="plans">
      <div class="plan">
        <h3>Pauca <span class="tag">LITE</span></h3>
        <div class="amount">{esc(t['lite_price'])}</div><div class="per">{esc(t['per_lite'])}</div>
        {checks(t['lite_list'])}
        <a class="btn ghost" href="{play_lite}">{esc(t['get_lite'])}</a>
      </div>
      <div class="plan full">
        <h3>Pauca</h3>
        <div class="amount">{esc(t['full_price'])}</div><div class="per">{esc(t['per_full'])}</div>
        <p style="margin:24px 0 -12px;font-weight:600">{esc(t['full_intro'])}</p>
        {checks(t['full_list'])}
        <a class="btn primary" href="{play_full}">{esc(t['get'])}</a>
      </div>
    </div>
    <p class="fine">{esc(t['carry'])}</p>
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
    {''.join(f'<details><summary>{esc(q)}</summary><p>{esc(a)}</p></details>' for q, a in t['faq'])}
  </div>
</section>

<section class="final on-dark">
  <div class="wrap reveal">
    <h2>{esc(t['final_h2'])}</h2>
    <div class="ctas">
      <a class="btn primary" href="{play_full}">{esc(t['get'])} <span class="price">· {esc(t['price'])}</span></a>
      <a class="btn ghost" href="{play_lite}">{esc(t['try_lite'])}</a>
    </div>
  </div>
</section>
</main>

<footer>
  <div class="wrap">
    <a class="brand" href="{root}{t['path']}">{EYE}<span>Pauca</span></a>
    <a href="{root}{t['privacy']}">{esc(t['footer_privacy'])}</a>
    <a href="{REPO}">{esc(t['footer_code'])}</a>
    <a href="{REPO}/blob/main/LICENSE">{esc(t['footer_license'])}</a>
    <div class="legal">{esc(t['legal'])} © 2026 Gabriel Dias.</div>
  </div>
</footer>
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
<meta property="og:type" content="website">
<meta property="og:title" content="{html.escape(t['title'])}">
<meta property="og:description" content="{html.escape(t['description'])}">
<meta property="og:image" content="{SITE}assets/og-{lang}.png">
<meta name="twitter:card" content="summary_large_image">
<meta name="theme-color" content="#F4EFE4">
<link rel="icon" href="{root}assets/icon.svg" type="image/svg+xml">
<link rel="apple-touch-icon" href="{root}assets/apple-touch-icon.png">
<link rel="stylesheet" href="{root}assets/site.css">
<script>document.documentElement.classList.add("js")</script>
</head>
<body>
{body}
<script src="{root}assets/site.js" defer></script>
</body>
</html>
"""


JS = """// Os blocos aparecem suavemente ao entrar na tela (o CSS ignora isso com "menos movimento").
const io = new IntersectionObserver(entries => {
  for (const e of entries) if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
}, { rootMargin: "0px 0px -10% 0px" });
document.querySelectorAll(".reveal").forEach(el => io.observe(el));
"""

ICON_SVG = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><rect width="512" height="512" rx="112" fill="#7E8C54"/>
<g fill="none" stroke="#FBF3E6" stroke-width="34" stroke-linecap="round" stroke-linejoin="round" transform="translate(-6 -30) scale(1.02)">
<path d="M132 226c40 62 84 90 124 90s84-28 124-90M166 290l-26 40M256 318v48M346 290l26 40"/></g></svg>
"""


def assets():
    os.makedirs(os.path.join(ASSETS, "fonts"), exist_ok=True)
    for f in ("jakarta.ttf", "newsreader.ttf"):
        shutil.copy(os.path.join(ROOT, "app/src/main/res/font", f), os.path.join(ASSETS, "fonts", f))
    shutil.copy(os.path.join(ROOT, "site/site.css"), os.path.join(ASSETS, "site.css"))
    open(os.path.join(ASSETS, "site.js"), "w").write(JS)
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


def main():
    assets()
    for lang, t in T.items():
        folder = os.path.join(DOCS, t["path"])
        os.makedirs(folder, exist_ok=True)
        with open(os.path.join(folder, "index.html"), "w") as f:
            f.write(page(lang, t))
        print(os.path.relpath(os.path.join(folder, "index.html"), ROOT))


if __name__ == "__main__":
    main()
