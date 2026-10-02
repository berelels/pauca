# Play Console: o que responder

Respostas para os formulários da Play Console, para o **Pauca** e o **Pauca Lite**. Onde os dois diferem, está indicado.

## Antes de tudo

- **Pago ou grátis:** crie o Pauca já como **pago**. Um app publicado como grátis nunca mais pode virar pago (o contrário pode). O Pauca Lite é grátis.
- **Assinatura do app:** ao criar o segundo app, escolha usar a mesma chave de assinatura do primeiro (veja `assinatura.md`).
- **Política de privacidade:** use estes endereços:
  - inglês: https://berelels.github.io/pauca/privacy/
  - português: https://berelels.github.io/pauca/privacy/pt/
  - espanhol: https://berelels.github.io/pauca/privacy/es/

## Detalhes do app

- **Categoria:** Personalização.
- **Site:** https://github.com/berelels/pauca
- **E-mail de contato:** é público na página do app, então use um e-mail que você queira mostrar.

## Acesso ao app

Marque que **todas as funcionalidades estão disponíveis sem acesso especial**. Não há login.

## Anúncios

**Não**, o app não tem anúncios.

## Classificação de conteúdo

No questionário IARC, a categoria é **Todos os outros tipos de app** (utilitário). Responda **não** a tudo: violência, conteúdo sexual, linguagem, drogas, jogos de azar, interação entre usuários, compartilhamento de localização, compras digitais.

O Pauca é pago, mas a pergunta sobre compras digitais se refere a compras **dentro** do app, que ele não tem. O resultado deve ser Livre / Everyone.

## Público-alvo

Marque **13 anos ou mais**: 13 a 15, 16 a 17 e 18 ou mais. Não marque menores de 13; isso colocaria o app no programa Famílias, com outras regras. Na pergunta se o app pode atrair crianças, responda **não**.

## Segurança dos dados

- **O app coleta ou compartilha dados do usuário?** **Não.**
- **Por quê:** o Google só considera "coletado" o que sai do aparelho. O Pauca não tem permissão de internet, então nada sai. A lista de apps, os ajustes, a imagem de fundo e a contagem de notificações do modo foco ficam só no celular.
- **Criptografia em trânsito:** não se aplica, porque nada é transmitido.
- **Exclusão de dados:** desinstalar o app apaga tudo. Não há conta.

## Declaração de permissões

### Ver todos os apps (QUERY_ALL_PACKAGES): os dois apps

- **Uso permitido:** escolha "launcher / tela inicial do dispositivo".
- **Texto:**

  > Pauca is a home screen (launcher) app. It needs to see all installed apps to show them on the home screen and in its app drawer, which is its core function. The list never leaves the device: the app has no internet permission.

  Em português:

  > O Pauca é um app de tela inicial (launcher). Ele precisa ver todos os apps instalados para mostrá-los na tela inicial e na gaveta de apps, que é a sua função principal. A lista nunca sai do aparelho: o app não tem permissão de internet.

### API de acessibilidade: os dois apps

O app **não** é uma ferramenta de acessibilidade. Responda que não é; a declaração pede uma descrição e um vídeo.

**Pauca:**

> Pauca uses the AccessibilityService API for two features the user turns on explicitly: (1) locking the screen with a double tap on the home screen (performGlobalAction GLOBAL_ACTION_LOCK_SCREEN), and (2) in focus mode, if the user enables "block other apps", going back to the home screen when an app outside the user's allowed list is opened (GLOBAL_ACTION_HOME). Normally the service only receives click events from Pauca's own home screen, to detect the double tap. Only while focus mode with app blocking is on, it also receives window changes from other apps, and it reads just their package name. It does not read screen content or what the user types, and nothing leaves the device (the app has no internet permission). Before sending the user to Android's settings, the app shows a disclosure explaining exactly this.

**Pauca Lite:**

> Pauca Lite uses the AccessibilityService API for one feature the user turns on explicitly: locking the screen with a double tap on the home screen (performGlobalAction GLOBAL_ACTION_LOCK_SCREEN). The service only receives click events from Pauca Lite's own home screen, to detect the double tap. It does not read other apps, screen content or what the user types, and nothing leaves the device (the app has no internet permission). Before sending the user to Android's settings, the app shows a disclosure explaining exactly this.

**O vídeo** pode ser gravado com a gravação de tela do próprio celular e enviado ao YouTube como "não listado". Uns 30 a 60 segundos bastam:

1. Abra Ajustes › Gestos e ligue **Toque duplo para bloquear**.
2. Mostre o aviso do app que explica o uso da acessibilidade e toque para continuar.
3. Nos ajustes do Android, ligue o serviço do Pauca.
4. Volte à tela inicial e dê um toque duplo: a tela bloqueia.
5. Só no Pauca: ligue o modo foco com "Bloquear outros apps", abra um app fora da lista e mostre o celular voltando para o início.

## Outras permissões

Não precisam de formulário, mas estão explicadas na política de privacidade:
- acesso às notificações e ao Não Perturbe (só no Pauca);
- acesso ao uso, administrador do dispositivo e papel de parede;
- tons de cinza, que é concedido pelo adb.

## Teste fechado (conta pessoal)

Antes de liberar a produção, cada app precisa de um teste fechado com **12 testadores inscritos por 14 dias seguidos**. Confira o número exato na Play Console.

1. Envie o `.aab` para uma faixa de teste fechado.
2. Adicione a lista de e-mails.
3. Mande o link de inscrição para os testadores.

Dá para usar as mesmas pessoas nos dois apps.

Confira na Play Console, ao montar o teste do Pauca, se os testadores de um app pago precisam comprá-lo. Se precisarem, não use cartão de ninguém só para isso. Pergunte no suporte da Play Console qual é a saída indicada para o seu caso.
