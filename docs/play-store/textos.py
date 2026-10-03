"""Textos da loja dos dois apps (título ≤30, descrição curta ≤80, completa ≤4000), nos três
idiomas. Edite aqui e rode: python3 docs/play-store/textos.py  (grava em fastlane/metadata e confere os limites)"""
import os
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "fastlane", "metadata")
L = {}

L[("android", "en-US")] = dict(
title="Pauca: Minimalist Launcher",
short="A calm, text-only home screen and a focus mode that really silences your phone.",
full="""Pauca turns your phone into something closer to a dumbphone: your apps become words in calm cards, with the names you choose. No icons, no red badges.

HOME SCREEN
• Cards of apps you can rename and reorder
• Profiles like Personal, Work and Night
• A clock, themes, fonts and colors your way, or hide it all

FOCUS MODE
One tap, or automatic per profile:
• Do Not Disturb, choosing who can still call
• Notifications held, with a summary at the end
• Only allowed apps in the drawer; others send you back home
• Grayscale screen (one computer command, once)

PRIVATE
No internet permission, no data collected. The accessibility service only locks the screen with a double tap and, in focus mode, returns home when a blocked app opens.

Try Pauca Lite first at no cost; your cards and settings come along. Open source (GPLv3), based on Olauncher. One-time purchase, no ads.""")

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
full="""O Pauca deixa seu celular mais perto de um dumbphone: seus apps viram palavras em cartões calmos, com os nomes que você escolher. Sem ícones, sem bolinhas vermelhas.

TELA INICIAL
• Cartões de apps que você renomeia e reordena
• Perfis como Pessoal, Trabalho e Noite
• Relógio, temas, fontes e cores do seu jeito, ou tudo escondido

MODO FOCO
Com um toque, ou automático por perfil:
• Não Perturbe, escolhendo quem ainda pode ligar
• Notificações seguradas, com um resumo no fim
• Só os apps permitidos na gaveta; os outros voltam para o início
• Tela em tons de cinza (um comando no computador, uma vez)

PRIVADO
Sem permissão de internet, nenhum dado coletado. O serviço de acessibilidade só bloqueia a tela com o toque duplo e, no modo foco, volta para o início quando um app bloqueado abre.

Experimente antes o Pauca Lite, sem pagar; seus cartões e ajustes vêm junto. Código aberto (GPLv3), feito a partir do Olauncher. Compra única, sem anúncios.""")

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
full="""Pauca acerca tu teléfono a un dumbphone: tus apps se convierten en palabras dentro de tarjetas tranquilas, con los nombres que elijas. Sin iconos, sin globos rojos.

PANTALLA DE INICIO
• Tarjetas de apps que renombras y reordenas
• Perfiles como Personal, Trabajo y Noche
• Reloj, temas, fuentes y colores a tu manera, u ocúltalo todo

MODO CONCENTRACIÓN
Con un toque, o automático por perfil:
• No molestar, eligiendo quién puede seguir llamando
• Notificaciones retenidas, con un resumen al final
• Solo las apps permitidas en el cajón; las demás te devuelven al inicio
• Pantalla en escala de grises (un comando desde un ordenador, una vez)

PRIVADO
Sin permiso de internet, sin recopilar datos. El servicio de accesibilidad solo bloquea la pantalla con el doble toque y, en concentración, vuelve al inicio cuando se abre una app bloqueada.

Prueba antes Pauca Lite sin pagar; tus tarjetas y ajustes vienen contigo. Código abierto (GPLv3), basado en Olauncher. Pago único, sin anuncios.""")

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
