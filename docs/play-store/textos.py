"""Textos da loja dos dois apps (título ≤30, descrição curta ≤80, completa ≤4000), nos três
idiomas. Edite aqui e rode: python3 docs/play-store/textos.py  (grava em fastlane/metadata e confere os limites)"""
import os
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "fastlane", "metadata")
L = {}

L[("android", "en-US")] = dict(
title="Pauca: Minimalist Launcher",
short="A calm, text-only home screen and a focus mode that really silences your phone.",
full="""Pauca turns your smartphone into something closer to a dumbphone. Your apps become words in calm cards, with the names you choose. No icons, no red badges, nothing pulling at you.

The name comes from the Latin pauca: "few things".

YOUR HOME SCREEN
• Group your apps in cards and drag to reorder them, even from one card to another
• Rename any app with whatever you like: "mom", "bank", "work chat"
• Profiles such as Personal, Work and Night, each with its own cards
• A clock and date the way you like them: format, font, size, weight and alignment

APPEARANCE
• Five themes: Pauca, Paper, Graphite, Black and Wallpaper
• Your own photo as the background, with blur and brightness, on the lock screen too
• Accent colors: olive, terracotta, ochre, blue, plum, rose, none, or any color you type
• Fonts, weight, size, alignment, lowercase or UPPERCASE
• Hide the buttons, the clock, even the status bar

FOCUS MODE
Turn it on with one tap, or let a profile turn it on for you. It works in layers, each with its own permission:
• Do Not Disturb through Pauca's own rule, choosing who can still call
• Hold notifications from other apps, with a summary when focus ends
• A drawer that shows only the apps you allow
• Pause other apps: if one opens, you go back home
• A grayscale screen (needs one command from a computer, once)

EVERYTHING ELSE
• Swipe up for all your apps, with search and categories
• Swipe down for notifications, sideways to open an app you choose
• Double tap to lock the screen
• English, Portuguese and Spanish
• A short guided tour when you start

PRIVATE BY DESIGN
Pauca has no internet permission and collects no data. Everything stays on your phone.

Accessibility: Pauca uses the accessibility service only to lock the screen with a double tap and, in focus mode, to go back home when an app outside your list opens. It doesn't read what's on your screen.

TRY IT FIRST
Pauca Lite is a smaller version you can try at no cost. When you get Pauca, your cards and settings come along.

OPEN SOURCE
Pauca is open source under the GPLv3, based on Olauncher by Tanuj. One-time purchase, no subscription, no ads.""")

L[("android-lite", "en-US")] = dict(
title="Pauca Lite: Minimal Launcher",
short="A calm, text-only home screen. Your apps as words in cards, with no icons.",
full="""Pauca Lite turns your smartphone into something closer to a dumbphone. Your apps become words in calm cards, with the names you choose. No icons, no red badges, nothing pulling at you.

The name comes from the Latin pauca: "few things".

WHAT YOU GET
• Group your apps in cards and drag to reorder them, even from one card to another
• Rename any app with whatever you like: "mom", "bank", "work chat"
• Two profiles, each with its own cards
• Light or dark: the Pauca and Paper themes, with an olive, blue or no accent color
• Text and clock size, and the clock and date shown or hidden
• Swipe up for all your apps, with search and categories
• Swipe down for notifications, sideways to open an app you choose
• Double tap to lock the screen
• English, Portuguese and Spanish, and a short guided tour

PRIVATE BY DESIGN
Pauca Lite has no internet permission and collects no data. Everything stays on your phone.

Accessibility: Pauca Lite uses the accessibility service only to lock the screen with a double tap. It doesn't read what's on your screen.

WANT MORE? GET PAUCA
Pauca, the full version, is a one-time purchase with no subscription. It adds:
• Focus mode that really silences: Do Not Disturb, held notifications with a summary, paused apps and a grayscale screen
• Unlimited profiles, and profiles that turn focus mode on by themselves
• Five themes, including your own photo with blur and brightness, on the lock screen too
• Every accent color, or any color you type
• Fonts, weight, alignment and letter case
• Hide the buttons, the clock, even the status bar

When you install Pauca, your cards and settings from Lite come along.

OPEN SOURCE
Pauca is open source under the GPLv3, based on Olauncher by Tanuj. No ads.""")

L[("android", "pt-BR")] = dict(
title="Pauca: Launcher Minimalista",
short="Uma tela inicial calma, só de texto, com um modo foco que silencia de verdade.",
full="""O Pauca deixa o seu smartphone mais perto de um dumbphone, um celular simples. Seus apps viram palavras em cartões calmos, com os nomes que você escolher. Sem ícones, sem bolinhas vermelhas, nada chamando sua atenção.

O nome vem do latim pauca: "poucas coisas".

SUA TELA INICIAL
• Agrupe os apps em cartões e arraste para reordenar, inclusive de um cartão para outro
• Renomeie qualquer app como quiser: "mãe", "banco", "trabalho"
• Perfis como Pessoal, Trabalho e Noite, cada um com os seus cartões
• Relógio e data do seu jeito: formato, fonte, tamanho, peso e alinhamento

APARÊNCIA
• Cinco temas: Pauca, Papel, Grafite, Preto e Fundo
• Uma foto sua como fundo, com desfoque e brilho, até na tela de bloqueio
• Cores de destaque: oliva, terracota, ocre, azul, ameixa, rosa, sem cor ou qualquer cor que você digitar
• Fontes, peso, tamanho, alinhamento, minúsculas ou MAIÚSCULAS
• Esconda os botões, o relógio e até a barra de status

MODO FOCO
Ligue com um toque, ou deixe um perfil ligar por você. Ele funciona em camadas, cada uma com a sua permissão:
• Não Perturbe com uma regra própria do Pauca, escolhendo quem ainda pode ligar
• Notificações dos outros apps seguradas, com um resumo quando o foco acaba
• Uma gaveta só com os apps permitidos
• Apps pausados: se um deles abrir, o celular volta para o início
• Tela em tons de cinza (precisa de um comando no computador, uma vez)

E MAIS
• Deslize para cima para ver todos os apps, com busca e categorias
• Deslize para baixo para as notificações e para os lados para abrir um app que você escolher
• Toque duplo para bloquear a tela
• Português, inglês e espanhol
• Um tour rápido quando você começa

PRIVADO DE VERDADE
O Pauca não tem permissão de internet e não coleta nenhum dado. Tudo fica no seu celular.

Acessibilidade: o Pauca usa o serviço de acessibilidade só para bloquear a tela com o toque duplo e, no modo foco, voltar para o início quando um app fora da sua lista abre. Ele não lê o que está na sua tela.

EXPERIMENTE ANTES
O Pauca Lite é uma versão menor, que você pode experimentar sem pagar. Quando você instala o Pauca, seus cartões e ajustes vão junto.

CÓDIGO ABERTO
O Pauca é código aberto, sob a GPLv3, feito a partir do Olauncher, de Tanuj. Compra única, sem assinatura e sem anúncios.""")

L[("android-lite", "pt-BR")] = dict(
title="Pauca Lite: Launcher Minimal",
short="Uma tela inicial calma, só de texto. Seus apps em palavras, em cartões.",
full="""O Pauca Lite deixa o seu smartphone mais perto de um dumbphone, um celular simples. Seus apps viram palavras em cartões calmos, com os nomes que você escolher. Sem ícones, sem bolinhas vermelhas, nada chamando sua atenção.

O nome vem do latim pauca: "poucas coisas".

O QUE TEM
• Agrupe os apps em cartões e arraste para reordenar, inclusive de um cartão para outro
• Renomeie qualquer app como quiser: "mãe", "banco", "trabalho"
• Dois perfis, cada um com os seus cartões
• Claro ou escuro: os temas Pauca e Papel, com a cor de destaque oliva, azul ou nenhuma
• Tamanho do texto e do relógio, e relógio e data à mostra ou escondidos
• Deslize para cima para ver todos os apps, com busca e categorias
• Deslize para baixo para as notificações e para os lados para abrir um app que você escolher
• Toque duplo para bloquear a tela
• Português, inglês e espanhol, e um tour rápido

PRIVADO DE VERDADE
O Pauca Lite não tem permissão de internet e não coleta nenhum dado. Tudo fica no seu celular.

Acessibilidade: o Pauca Lite usa o serviço de acessibilidade só para bloquear a tela com o toque duplo. Ele não lê o que está na sua tela.

QUER MAIS? CONHEÇA O PAUCA
O Pauca, a versão completa, é uma compra única, sem assinatura. Ele traz:
• Modo foco que silencia de verdade: Não Perturbe, notificações seguradas com resumo, apps pausados e tela em tons de cinza
• Perfis ilimitados, e perfis que ligam o modo foco sozinhos
• Cinco temas, inclusive uma foto sua com desfoque e brilho, até na tela de bloqueio
• Todas as cores de destaque, ou qualquer cor que você digitar
• Fontes, peso, alinhamento, minúsculas e maiúsculas
• Esconda os botões, o relógio e até a barra de status

Quando você instala o Pauca, seus cartões e ajustes do Lite vão junto.

CÓDIGO ABERTO
O Pauca é código aberto, sob a GPLv3, feito a partir do Olauncher, de Tanuj. Sem anúncios.""")

L[("android", "es-ES")] = dict(
title="Pauca: Launcher Minimalista",
short="Una pantalla de inicio tranquila, solo texto, con concentración que silencia.",
full="""Pauca acerca tu smartphone a un dumbphone, un teléfono sencillo. Tus apps se convierten en palabras dentro de tarjetas tranquilas, con los nombres que elijas. Sin iconos, sin globos rojos, nada que te distraiga.

El nombre viene del latín pauca: "pocas cosas".

TU PANTALLA DE INICIO
• Agrupa tus apps en tarjetas y arrástralas para reordenarlas, incluso de una tarjeta a otra
• Cambia el nombre de cualquier app: "mamá", "banco", "trabajo"
• Perfiles como Personal, Trabajo y Noche, cada uno con sus tarjetas
• Reloj y fecha a tu manera: formato, fuente, tamaño, grosor y alineación

APARIENCIA
• Cinco temas: Pauca, Papel, Grafito, Negro y Fondo
• Una foto tuya de fondo, con desenfoque y brillo, también en la pantalla de bloqueo
• Colores de acento: oliva, terracota, ocre, azul, ciruela, rosa, sin color o cualquier color que escribas
• Fuentes, grosor, tamaño, alineación, minúsculas o MAYÚSCULAS
• Oculta los botones, el reloj e incluso la barra de estado

MODO CONCENTRACIÓN
Actívalo con un toque, o deja que un perfil lo active por ti. Funciona por capas, cada una con su permiso:
• No molestar con una regla propia de Pauca, eligiendo quién puede seguir llamando
• Notificaciones de otras apps retenidas, con un resumen cuando termina
• Un cajón solo con las apps permitidas
• Apps en pausa: si una se abre, vuelves al inicio
• Pantalla en escala de grises (necesita un comando desde un ordenador, una vez)

Y MÁS
• Desliza hacia arriba para ver todas tus apps, con búsqueda y categorías
• Desliza hacia abajo para las notificaciones y hacia los lados para abrir la app que elijas
• Doble toque para bloquear la pantalla
• Español, inglés y portugués
• Un recorrido breve al empezar

PRIVADO DE VERDAD
Pauca no tiene permiso de internet y no recopila ningún dato. Todo se queda en tu teléfono.

Accesibilidad: Pauca usa el servicio de accesibilidad solo para bloquear la pantalla con el doble toque y, en el modo concentración, volver al inicio cuando se abre una app que no está en tu lista. No lee lo que hay en tu pantalla.

PRUÉBALO ANTES
Pauca Lite es una versión más pequeña que puedes probar sin pagar. Cuando instalas Pauca, tus tarjetas y ajustes vienen contigo.

CÓDIGO ABIERTO
Pauca es de código abierto, bajo la GPLv3, basado en Olauncher, de Tanuj. Pago único, sin suscripción y sin anuncios.""")

L[("android-lite", "es-ES")] = dict(
title="Pauca Lite: Launcher Minimal",
short="Una pantalla de inicio tranquila, solo texto. Tus apps en palabras, en tarjetas.",
full="""Pauca Lite acerca tu smartphone a un dumbphone, un teléfono sencillo. Tus apps se convierten en palabras dentro de tarjetas tranquilas, con los nombres que elijas. Sin iconos, sin globos rojos, nada que te distraiga.

El nombre viene del latín pauca: "pocas cosas".

QUÉ INCLUYE
• Agrupa tus apps en tarjetas y arrástralas para reordenarlas, incluso de una tarjeta a otra
• Cambia el nombre de cualquier app: "mamá", "banco", "trabajo"
• Dos perfiles, cada uno con sus tarjetas
• Claro u oscuro: los temas Pauca y Papel, con color de acento oliva, azul o ninguno
• Tamaño del texto y del reloj, y reloj y fecha visibles u ocultos
• Desliza hacia arriba para ver todas tus apps, con búsqueda y categorías
• Desliza hacia abajo para las notificaciones y hacia los lados para abrir la app que elijas
• Doble toque para bloquear la pantalla
• Español, inglés y portugués, y un recorrido breve

PRIVADO DE VERDAD
Pauca Lite no tiene permiso de internet y no recopila ningún dato. Todo se queda en tu teléfono.

Accesibilidad: Pauca Lite usa el servicio de accesibilidad solo para bloquear la pantalla con el doble toque. No lee lo que hay en tu pantalla.

¿QUIERES MÁS? CONOCE PAUCA
Pauca, la versión completa, es un pago único, sin suscripción. Incluye:
• Concentración que silencia de verdad: No molestar, notificaciones retenidas con resumen, apps en pausa y pantalla en escala de grises
• Perfiles ilimitados, y perfiles que activan la concentración solos
• Cinco temas, incluida una foto tuya con desenfoque y brillo, también en la pantalla de bloqueo
• Todos los colores de acento, o cualquier color que escribas
• Fuentes, grosor, alineación, minúsculas y mayúsculas
• Oculta los botones, el reloj e incluso la barra de estado

Cuando instalas Pauca, tus tarjetas y ajustes de Lite vienen contigo.

CÓDIGO ABIERTO
Pauca es de código abierto, bajo la GPLv3, basado en Olauncher, de Tanuj. Sin anuncios.""")

LIMITS = dict(title=30, short=80, full=4000)
FILES = dict(title="title.txt", short="short_description.txt", full="full_description.txt")
bad = False
for (app, loc), t in L.items():
    for k, v in t.items():
        n = len(v)
        flag = "  <-- PASSA DO LIMITE" if n > LIMITS[k] else ""
        bad |= bool(flag)
        print(f"{app:13} {loc}  {k:5} {n:4}/{LIMITS[k]}{flag}")
        d = os.path.join(ROOT, app, loc); os.makedirs(d, exist_ok=True)
        open(os.path.join(d, FILES[k]), "w").write(v + "\n")
print("ERRO" if bad else "tudo dentro dos limites")
