# NerveCentre

A real-time backend monitoring system with AI-powered anomaly detection and forecasting.

Built with Java, Spring Boot, Redis Streams, TimescaleDB, and Ollama (llama3.2).

## Architecture
POST /metrics → Redis Streams → Stream Consumer → TimescaleDB
→ Z-score Detection → alerts table
→ LLM Enrichment (async)

## Tech Stack

| Layer | Technology |
|---|---|
| HTTP Framework | Spring Boot (Spring Web MVC) |
| Message Broker | Redis Streams |
| Time-Series DB | TimescaleDB (PostgreSQL) |
| Anomaly Detection | Z-score statistical analysis |
| LLM Enrichment | Ollama (llama3.2) — runs locally |
| Forecasting | Linear regression (Apache Commons Math) |
| Async Processing | Spring @Async |

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | /metrics | Ingest a metric data point |
| GET | /forecast/:name | Predicted values for next 10 minutes |

## Running Locally

**Prerequisites:** Docker, Docker Compose

```bash
# Start all infrastructure
docker compose up -d

# Pull the LLM model (first time only)
docker exec -it <ollama-container> ollama pull llama3.2

# Start the app
./mvnw spring-boot:run
```

**Send a metric:**
```bash
curl -X POST http://localhost:8080/metrics \
  -H "Content-Type: application/json" \
  -d '{"name": <metric_name>, "value": <metric_value>}'
```

**Get a forecast:**
```bash
curl "http://localhost:8080/forecast/<metric_name>?threshold=500"
```

## How Anomaly Detection Works

Every metric is analyzed using Z-score statistical detection:

1. Fetch the last 20 values for the metric from TimescaleDB
2. Calculate rolling mean and standard deviation
3. Compute Z-score: `(current - mean) / stddev`
4. Fire a WARNING alert if Z-score ≥ 2.0, CRITICAL if ≥ 3.0
5. Trigger async LLM job to generate plain-English diagnosis

## How Forecasting Works

Linear regression over the last 30 data points extrapolates the next 10 minutes of predicted values with a 95% confidence interval. A proactive alert fires if the forecast crosses a user-defined threshold before it actually does.