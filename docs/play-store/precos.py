#!/usr/bin/env python3
"""
Preço do Pauca em cada moeda, a partir de US$ 1, com a cotação do dia (open.er-api.com).

Regra:
- O valor convertido é arredondado para baixo.
- Abaixo de 1 na moeda local, ou quando cortar até o inteiro derrubaria o preço em mais de 15%,
  o final é ,x9 para baixo. Exemplos: € 0,89 → € 0,79 e NZ$ 1,78 → NZ$ 1,69 (o inteiro, NZ$ 1,
  seria 44% a menos).
- O resto vai para o inteiro de baixo. Exemplo: R$ 5,19 → R$ 5.

Uso: python3 precos.py  (imprime a tabela em Markdown)
"""
import json
import math
import urllib.request

PAISES = [
    ("Brasil", "BRL"), ("Zona do euro", "EUR"), ("Reino Unido", "GBP"), ("Suíça", "CHF"),
    ("Canadá", "CAD"), ("Austrália", "AUD"), ("Nova Zelândia", "NZD"), ("México", "MXN"),
    ("Chile", "CLP"), ("Colômbia", "COP"), ("Peru", "PEN"), ("Japão", "JPY"),
    ("Coreia do Sul", "KRW"), ("Índia", "INR"), ("Indonésia", "IDR"), ("Turquia", "TRY"),
    ("Polônia", "PLN"), ("Suécia", "SEK"), ("Noruega", "NOK"), ("Dinamarca", "DKK"),
    ("Rep. Tcheca", "CZK"), ("Hungria", "HUF"), ("África do Sul", "ZAR"), ("Singapura", "SGD"),
    ("Hong Kong", "HKD"), ("Taiwan", "TWD"), ("Tailândia", "THB"), ("Filipinas", "PHP"),
    ("Malásia", "MYR"), ("Vietnã", "VND"), ("Arábia Saudita", "SAR"), ("Emirados", "AED"),
    ("Israel", "ILS"), ("Egito", "EGP"), ("Nigéria", "NGN"), ("Paquistão", "PKR"),
]


def preco(valor: float) -> str:
    if valor < 1 or math.floor(valor) < valor * 0.85:
        # ,x9 para baixo: 0,89 → 0,79; 1,78 → 1,69; 3,89 → 3,79
        return f"{math.floor(valor * 10) / 10 - 0.01:.2f}".replace(".", ",")
    return f"{math.floor(valor):,}".replace(",", ".")


def main():
    with urllib.request.urlopen("https://open.er-api.com/v6/latest/USD") as r:
        dados = json.load(r)
    print(f"Cotação de {dados['time_last_update_utc']}\n")
    print("| País | Moeda | US$ 1 = | Preço |")
    print("|---|---|---|---|")
    for pais, moeda in PAISES:
        v = dados["rates"][moeda]
        print(f"| {pais} | {moeda} | {v:,.2f} | {preco(v)} |")


if __name__ == "__main__":
    main()
