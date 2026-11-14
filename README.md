# StyleDesk Agent — Sofia

**Sofia is your personal AI stylist.**

A conversational fashion stylist who remembers your preferences, gives tailored style advice, and helps you negotiate the best deals. Sofia connects to fashion-agent for specialist outfit recommendations.

---

## Architecture

```
Browser / Agent / API Client
        │
        ▼
Java Backend (Quarkus)
        │
        ├── /chat              — Conversational with per-user memory
        ├── /a2a               — A2A JSON-RPC (agent-to-agent)
        ├── /request-access    — Self-service API keys
        ├── /agentfacts.json   — NANDA discovery
        ├── /.well-known/agent-card.json — A2A discovery
        ├── /skill.md          — NEST discovery
        └── /health            — Health check
        │
        ▼
Gemini 2.5 Flash (Vertex AI — global)
        │
        ▼
fashion-agent (A2A) + Google Shopping + Amazon (via SerpAPI)
```

---

## Endpoints

| Endpoint | Purpose | Auth |
|---|---|---|
| `POST /chat` | Conversational with memory | X-API-Key |
| `POST /a2a` | A2A JSON-RPC | X-API-Key |
| `POST /request-access` | Self-service API keys | Public |
| `GET /.well-known/agent-card.json` | A2A discovery | Public |
| `GET /agentfacts.json` | NANDA discovery | Public |
| `GET /skill.md` | NEST discovery | Public |
| `GET /health` | Health check | Public |

---

## Run Locally

### Prerequisites
- Java 21+
- Maven 3.9+
- Google Cloud SDK with ADC credentials
- Vertex AI enabled on GCP project `fashion-agent-494222`
- SerpAPI key for product search, supplied as the `SERPAPI_API_KEY` environment variable (never commit it)

### Start

```bash
cd ~/styledesk-agent

GOOGLE_GENAI_USE_VERTEXAI=true \
GOOGLE_CLOUD_PROJECT=fashion-agent-494222 \
GOOGLE_CLOUD_LOCATION=global \
GOOGLE_APPLICATION_CREDENTIALS=$HOME/.config/gcloud/application_default_credentials.json \
mvn quarkus:dev
```

> ⚠️ ADC credentials expire after 1 hour. Refresh with:
> ```bash
> gcloud auth application-default login
> ```

---

## Deploy to Cloud Run

```bash
cd ~/styledesk-agent

mvn package -DskipTests && \
gcloud builds submit \
  --tag gcr.io/fashion-agent-494222/styledesk-agent \
  --project=fashion-agent-494222 . && \
gcloud run deploy styledesk-agent \
  --image gcr.io/fashion-agent-494222/styledesk-agent \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --service-account fashion-agent-sa@fashion-agent-494222.iam.gserviceaccount.com \
  --set-env-vars GOOGLE_GENAI_USE_VERTEXAI=true,GOOGLE_CLOUD_PROJECT=fashion-agent-494222,GOOGLE_CLOUD_LOCATION=global \
  --update-secrets SERPAPI_API_KEY=serpapi-key:latest \
  --project=fashion-agent-494222
```

---

## Usage

### Request Access

```bash
curl -X POST https://styledesk-agent-246551819686.us-central1.run.app/request-access \
  -H 'Content-Type: application/json' \
  -d '{"principal":"user123","name":"My Name","reason":"style advice"}'
```

### Chat (with memory)

```bash
curl -X POST https://styledesk-agent-246551819686.us-central1.run.app/chat \
  -H 'X-API-Key: YOUR_KEY' \
  -H 'Content-Type: application/json' \
  -d '{"userId":"user123","message":"Hi Sofia! I need a winter gala outfit under $300"}'
```

### A2A (Agent-to-Agent)

```bash
curl -X POST https://styledesk-agent-246551819686.us-central1.run.app/a2a \
  -H 'Content-Type: application/json' \
  -H 'X-API-Key: YOUR_KEY' \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tasks/send",
    "params": {
      "message": {
        "parts": [{"type": "text", "text": "Find me a cocktail dress under $200"}]
      }
    }
  }'
```

---

## Agent Capabilities

- **Multi-turn memory** — remembers style preferences across conversations per userId
- **Outfit recommendations** — calls fashion-agent via A2A for specialist advice
- **Product search** — real products with buy links from Google Shopping and Amazon
- **Price negotiation** — tactics for boutiques, online retailers, and markets
- **Price comparison** — finds best deals across retailers
- **Amazon affiliate** — tag `fashionagent2-20`

---

## Agent Discovery

| Registry | Handle | Status |
|---|---|---|
| NANDA Index | `@styledesk-agent` | ✅ Registered |
| NEST | `@styledesk-agent` | ✅ Registered |

---

## GCP Configuration

| Config | Value |
|---|---|
| GCP Project | `fashion-agent-494222` (246551819686) |
| GCP Region | `us-central1` |
| Service Account | `fashion-agent-sa@fashion-agent-494222.iam.gserviceaccount.com` |
| Model | `gemini-2.5-flash` @ global |
| Amazon Affiliate | `fashionagent2-20` |

---

## Links

- Live URL: [styledesk-agent-246551819686.us-central1.run.app](https://styledesk-agent-246551819686.us-central1.run.app)
- Fashion Agent: [fashion-agent-246551819686.us-central1.run.app](https://fashion-agent-246551819686.us-central1.run.app)
