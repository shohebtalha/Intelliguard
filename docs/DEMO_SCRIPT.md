# Demo Script

Use this flow to present IntelliGuard in 8-10 minutes.

## 1. Start With The Architecture

Open [PRODUCTION_ARCHITECTURE.md](PRODUCTION_ARCHITECTURE.md).

Talk track:

- IntelliGuard is a modular monolith with clear extraction boundaries.
- The synchronous path returns a fraud decision immediately.
- Kafka publishing is handled through a transactional outbox.
- Suspicious decisions create analyst cases.
- Audit logs are hash-chained for tamper evidence.
- Model governance tracks registry entries, inference metrics, and drift snapshots.

## 2. Show The Running System

```bash
cp .env.example .env
docker-compose up --build
```

Open:

- Frontend: http://localhost:3000
- Swagger: http://localhost:8080/swagger-ui.html
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001

If using local demo users, set `DEMO_SEED_DATA=true`.

## 3. Submit A Fraudulent Transaction

Use a high-risk country or high amount:

```json
{
  "senderId": "USER_001",
  "receiverId": "USER_002",
  "amount": 98000.00,
  "currency": "INR",
  "country": "NG",
  "paymentMethod": "NET_BANKING",
  "deviceType": "UNKNOWN",
  "ipAddress": "197.210.84.21"
}
```

Explain:

- Redis updates velocity windows.
- Rules evaluate deterministic signals.
- ONNX model inference runs if available.
- The final decision is persisted.
- Outbox rows are created atomically.
- A case is opened for `REVIEW` or `BLOCK`.

## 4. Open The Case Queue

Go to the Cases page.

Show:

- queue filtering by status;
- assignment to an analyst;
- investigation notes;
- resolving as confirmed fraud or false positive.

Talk track:

- Fraud platforms are operational systems, not just scoring APIs.
- Analysts need ownership, notes, and resolution state.
- Tenant-scoped queries prevent cross-customer data exposure.

## 5. Show Audit And Governance

Open audit endpoints or database records.

Explain:

- `previous_hash` and `record_hash` create a tamper-evident chain.
- The model registry records the champion model.
- Inference metrics capture latency, fallback, score, and errors.
- Drift snapshots watch fallback rate and high-risk score distribution.

## 6. Show Observability

Open Grafana and Prometheus.

Show:

- HTTP request rate;
- p95 API latency;
- fraud decisions by outcome;
- fraud decision p95 latency;
- outbox backlog alert;
- open case backlog alert;
- model fallback alert.

## 7. Show Verification

```bash
mvn test -B
cd frontend
npm run test:e2e
```

Talk track:

- Unit tests cover rules, transaction flow, refresh-token rotation, outbox behavior, case workflow, audit hash chain, and model governance.
- Playwright smoke tests cover protected routing, login error handling, and case-navigation shell.
- CI runs backend, frontend, Docker builds, CodeQL, dependency review, and Playwright.

## 8. Be Honest About Tradeoffs

Say explicitly:

- This is a modular monolith by design.
- OAuth2/OIDC is a planned integration boundary.
- The current explainability is feature-importance-style, not a full SHAP production pipeline.
- At high scale, model serving, case management, audit, and analytics become separately owned services.

