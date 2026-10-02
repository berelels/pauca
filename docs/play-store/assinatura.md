# Assinatura

As versões de lançamento do Pauca e do Pauca Lite são assinadas com a mesma **chave de upload**:

- **A chave:** `~/Documentos/Projetos/Apps/chaves/pauca/pauca-upload.jks`, fora do repositório.
- **As senhas:** `keystore.properties`, na raiz do projeto. O git ignora esse arquivo, e há uma cópia ao lado da chave.
- **A impressão digital (SHA-256):** `0A:18:90:C1:2A:06:2B:D7:F3:3F:7B:0A:F3:C2:B1:39:A0:F4:A2:B0:C1:C6:85:C0:BB:5E:69:B6:25:33:B5:8E`

Sem `keystore.properties`, o Gradle gera o release sem assinatura. Assim, quem compila o código aberto não precisa de nada disso.

## Guarde uma cópia

Copie a pasta `chaves/pauca` para um lugar fora deste computador, como um pendrive ou um gerenciador de senhas. Se a chave de upload se perder, dá para pedir uma nova ao Google pela Play Console, mas isso leva dias.

## Gerar os pacotes

```bash
./gradlew bundleFullRelease bundleLiteRelease
```

Os pacotes saem em `app/build/outputs/bundle/fullRelease/app-full-release.aab` (Pauca) e `app/build/outputs/bundle/liteRelease/app-lite-release.aab` (Pauca Lite).

## Na Play Console: a mesma chave nos dois apps

A Play Store reassina cada app com uma **chave de assinatura** guardada pelo Google. A chave de upload só prova que o pacote veio de você.

Para o Pauca trazer os ajustes do Lite, os dois apps precisam sair da loja com a **mesma** chave de assinatura. Isso vale porque a migração usa uma permissão de assinatura (veja `helper/LiteImport.kt`). Faça assim:

1. Crie o primeiro app (por exemplo, o Pauca) e deixe o Google gerar a chave de assinatura.
2. Ao criar o segundo (o Pauca Lite), em **Integridade do app › Assinatura do app**, escolha **usar a mesma chave de outro app desta conta** e aponte para o Pauca.

Isso só pode ser escolhido antes do primeiro envio do segundo app. Depois, não muda mais.
