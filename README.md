# Marcai

API de agendamentos com lembrete automático e confirmação pelo WhatsApp (Cloud API da Meta).

- Cadastro de **clientes**, **serviços** (com duração) e **agendamentos**.
- O banco impede horários sobrepostos (constraint `EXCLUDE` do Postgres), mesmo com requisições simultâneas.
- A cada 5 min, quem tem horário nas próximas 24h recebe um **lembrete** por template do WhatsApp.
- O cliente responde `1`/`sim` (confirma) ou `2`/`não` (cancela). O **webhook** valida a assinatura da Meta e
  processa cada mensagem uma única vez.

Stack: Java 27, Spring Boot 4.2, PostgreSQL 17, Flyway, Spring Security (OAuth2), Testcontainers.

## Rodando localmente

Requisitos: JDK 27 e Docker.

```bash
cp .env.example .env      # opcional em dev
./mvnw spring-boot:run    # sobe o Postgres do docker-compose.yml automaticamente
```

Pela IDE, rode `MarcaiApplication` com o perfil `dev` (no IntelliJ: *Active profiles* = `dev`).
Sem perfil a aplicação exige todos os segredos de produção e não sobe — de propósito.

- Swagger: http://localhost:8080/swagger-ui.html (botão *Authorize*: `marcai-dev` / `dev-secret`)
- Health: http://localhost:8080/actuator/health

### Autenticação

A própria aplicação emite tokens (grant `client_credentials`):

```bash
TOKEN=$(curl -s -u marcai-dev:dev-secret -d grant_type=client_credentials \
  http://localhost:8080/oauth2/token | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"nome":"Ana","telefone":"5511999998888"}' http://localhost:8080/api/clientes
```

| Método | Rota | |
|---|---|---|
| POST/GET | `/api/clientes`, `/api/clientes/{id}` | telefone no formato `5511999998888` |
| POST/GET | `/api/servicos`, `/api/servicos/{id}` | `duracaoMinutos` define o fim do horário |
| POST | `/api/agendamentos` | `{"clienteId":1,"servicoId":1,"inicio":"2026-12-01T14:00:00-03:00"}` — 409 se ocupado |
| GET | `/api/agendamentos?de=…&ate=…` | início em `[de, ate)` |
| GET | `/api/agendamentos/{id}` | |
| POST | `/api/agendamentos/{id}/cancelamento` | libera o horário |
| GET/POST | `/webhook/whatsapp` | público, usado pela Meta |

## Testando sem o WhatsApp real

Em dev, a aplicação fala com um **WhatsApp falso** (WireMock em `localhost:8089`, sobe junto pelo
`docker-compose.yml`) que imita a Graph API da Meta e guarda tudo que foi "enviado". O agendador roda a cada 30s.

```bash
./mvnw spring-boot:run
# crie cliente, serviço e um agendamento para daqui a poucas horas (Swagger ou curl)
scripts/mensagens-enviadas.sh                      # ~30s depois: o lembrete que "saiu"
scripts/simular-resposta.sh 5511999998888 1        # cliente responde "1" → CONFIRMADO
scripts/simular-resposta.sh 5511999998888 "não"    # ou cancela → CANCELADO
scripts/mensagens-enviadas.sh --limpar
```

`simular-resposta.sh` monta o mesmo JSON que a Meta manda e o **assina com o App Secret**, então passa pela
mesma validação de produção. Com `WHATSAPP_APP_SECRET=errado` ele recebe 401.

Nos testes automatizados, `LembreteFluxoCompletoTest` faz a jornada inteira (lembrete → resposta →
confirmação, e falha da Meta → nova tentativa) com uma Graph API falsa em memória.

Para testar com a Meta de verdade sem número próprio, use o **número de teste** que o painel de
desenvolvedor da Meta oferece e coloque `WHATSAPP_API_URL=https://graph.facebook.com/v23.0` no `.env`.

## WhatsApp

1. No painel da Meta, pegue o token, o *Phone Number ID* e o *App Secret* e coloque no `.env`.
2. Crie e aprove um template (nome em `WHATSAPP_TEMPLATE_LEMBRETE`, idioma `pt_BR`) com 3 variáveis, por exemplo:
   > Olá {{1}}! Lembrete do seu {{2}} em {{3}}. Responda **1** para confirmar ou **2** para cancelar.

   Botões de resposta rápida com payload `CONFIRMAR` / `CANCELAR` também funcionam.
3. Configure o webhook para `https://SEU_HOST/webhook/whatsapp`, com o mesmo `WHATSAPP_VERIFY_TOKEN`, e assine
   o campo `messages`.

## Testes

```bash
./mvnw verify    # unitários + integração com Postgres real via Testcontainers (precisa de Docker)
```

O CI (`.github/workflows/ci.yml`) roda o mesmo comando a cada push.

## Produção

Tudo em container, com o perfil `prod`:

```bash
docker compose --profile app up --build
```

No perfil `prod` todas as variáveis do `.env.example` são obrigatórias — sem elas a aplicação não sobe —, além de `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` (o compose já define essas três).

Limitações atuais, a resolver antes de escalar:

- A chave que assina os tokens é gerada a cada inicialização: reiniciar invalida os tokens emitidos, e
  várias instâncias não compartilham a chave.
- Com mais de uma instância, o agendador de lembretes roda em todas (seria preciso um lock, ex.: ShedLock).
- A agenda é única (um profissional/recurso). Para vários, inclua `profissional_id` na constraint de exclusão
  (aí sim usando a extensão `btree_gist`).
