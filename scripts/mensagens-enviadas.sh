#!/usr/bin/env bash
# Lista as mensagens que a aplicação "enviou" ao WhatsApp falso (WireMock do docker-compose).
# Uso: scripts/mensagens-enviadas.sh          — lista
#      scripts/mensagens-enviadas.sh --limpar — apaga o histórico
set -euo pipefail

ADMIN="${WHATSAPP_FAKE_URL:-http://localhost:8089}/__admin/requests"

if [[ "${1:-}" == "--limpar" ]]; then
  curl -s -X DELETE "$ADMIN" >/dev/null && echo "histórico apagado"
  exit 0
fi

curl -s "$ADMIN" | jq '[.requests[] | .request | select(.url | endswith("/messages")) | (.body | fromjson) as $b | {
  quando: .loggedDateString,
  para: $b.to,
  template: $b.template.name,
  variaveis: [$b.template.components[]?.parameters[]?.text]
}]'
