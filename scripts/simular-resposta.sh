#!/usr/bin/env bash
# Simula o cliente respondendo pelo WhatsApp: monta o mesmo JSON que a Meta envia,
# assina com o App Secret (header X-Hub-Signature-256) e chama o webhook local.
#
# Uso: scripts/simular-resposta.sh <telefone> <texto>
#   ex: scripts/simular-resposta.sh 5511999998888 1      (confirma)
#       scripts/simular-resposta.sh 5511999998888 "não"  (cancela)
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "uso: $0 <telefone> <texto>" >&2
  exit 1
fi

TELEFONE="$1"
TEXTO="$2"
APP_SECRET="${WHATSAPP_APP_SECRET:-dev-app-secret}"
URL="${WEBHOOK_URL:-http://localhost:8080/webhook/whatsapp}"
ID="wamid.simulado.$(date +%s%N)"

CORPO=$(jq -cn --arg from "$TELEFONE" --arg id "$ID" --arg texto "$TEXTO" '{
  object: "whatsapp_business_account",
  entry: [{ id: "0", changes: [{ field: "messages", value: {
    messaging_product: "whatsapp",
    metadata: { display_phone_number: "550000000000", phone_number_id: "000000000000000" },
    messages: [{ from: $from, id: $id, timestamp: (now | floor | tostring), type: "text", text: { body: $texto } }]
  }}]}]
}')

ASSINATURA="sha256=$(printf '%s' "$CORPO" | openssl dgst -sha256 -hmac "$APP_SECRET" | awk '{print $NF}')"

STATUS=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$URL" \
  -H 'Content-Type: application/json' \
  -H "X-Hub-Signature-256: $ASSINATURA" \
  --data-raw "$CORPO")

echo "webhook respondeu HTTP $STATUS (mensagem $ID)"
