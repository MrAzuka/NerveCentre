# NerveCentre

A real-time backend monitoring system with AI-powered anomaly detection and forecasting.

Built with Java, Spring Boot, Redis Streams, TimescaleDB, and Ollama (llama3.2).

---

## What It Does

NerveCentre watches your backend infrastructure in real time. Every metric that
flows through the system is statistically analyzed for anomalies, enriched
with an AI-generated diagnosis, and surfaced on a live WebSocket dashboard,
all without blocking the ingestion pipeline.

---
## Architecture
POST /metrics →
Redis Streams (message broker) →
Stream Consumer
→ TimescaleDB (time-series storage)
→ WebSocket Dashboard (live push)
→ Z-score Anomaly Detection →
Cooldown Check → Alert saved to DB
→ Async LLM Enrichment (Ollama / llama3.2)
→ Dashboard notified with AI diagnosis

**Two-speed AI design:** Statistical detection fires in microseconds.
LLM diagnosis runs in the background and attaches to the alert after the fact.
The ingestion pipeline is never blocked.
---
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
| Live Dashboard | WebSocket (STOMP + SockJS) + Chart.js |
| Containerization | Docker Compose |
---
## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | /metrics | Ingest a metric data point |
| GET | /forecast/:name | Predicted values for next 10 minutes |
| GET | /forecast/:name?threshold=500 | Forecast with proactive alert threshold |
| GET | /alerts | Paginated alert history with filters |
| GET | /alerts?severity=CRITICAL | Filter alerts by severity |
| GET | /alerts?metricName=cpu_usage | Filter alerts by metric name |
| GET | /simulator/status | Check if simulator is running |
| POST | /simulator/toggle | Toggle simulator on or off |
| POST | /simulator/start | Start the metric simulator |
| POST | /simulator/stop | Stop the metric simulator |
---

## How Anomaly Detection Works

Every incoming metric is analyzed using Z-score statistical detection:
```Z = (current_value - rolling_mean) / rolling_stddev```

- Z ≥ 2.0 → WARNING alert
- Z ≥ 3.0 → CRITICAL alert
- Requires at least 5 data points to compute meaningful statistics
- Once an alert fires, a configurable cooldown window suppresses
  duplicates for the same metric (default: 5 minutes)
- Each alert triggers an async Ollama job that generates a plain-English
  diagnosis, likely cause, and recommended action

---
## How Forecasting Works

`GET /forecast/:metricName?threshold=500`

- Fits a simple linear regression over the last 30 data points
- Extrapolates the next 10 minutes of predicted values
- Returns a 95% confidence interval (upper and lower bounds)
- If the forecast crosses your threshold before the metric actually does,
  a proactive WARNING is returned in the response

---

## Live Dashboard

Open `http://localhost:8080` after starting the app.

- Real-time metric cards updating every 5 seconds
- Line chart with switchable metric views
- Live alert feed with severity badges and AI explanations
- Metric cards flash red when an anomaly is detected
- Simulator toggle button to start/stop simulated traffic

---
## Running Locally

- Z ≥ 2.0 → WARNING alert
- Z ≥ 3.0 → CRITICAL alert
- Requires at least 5 data points to compute meaningful statistics
- Once an alert fires, a configurable cooldown window suppresses
  duplicates for the same metric (default: 5 minutes)
- Each alert triggers an async Ollama job that generates a plain-English
  diagnosis, likely cause, and recommended action

---

## How Forecasting Works

`GET /forecast/:metricName?threshold=500`

- Fits a simple linear regression over the last 30 data points
- Extrapolates the next 10 minutes of predicted values
- Returns a 95% confidence interval (upper and lower bounds)
- If the forecast crosses your threshold before the metric actually does,
  a proactive WARNING is returned in the response

---

## Live Dashboard

Open `http://localhost:8080` after starting the app.

- Real-time metric cards updating every 5 seconds
- Line chart with switchable metric views
- Live alert feed with severity badges and AI explanations
- Metric cards flash red when an anomaly is detected
- Simulator toggle button to start/stop simulated traffic

---

## Running Locally

**Prerequisites:** Docker, Docker Compose, Java 21+

**1. Clone the repository**
```bash
git clone https://github.com/MrAzuka/NerveCentre.git
cd NerveCentre
```

**2. Create your `application.properties`**

Copy the example file and fill in your values:
```bash
cp src/main/resources/application.properties.example \
   src/main/resources/application.properties
```

**3. Start infrastructure**
```bash
docker compose up -d
```

**4. Pull the LLM model (first time only)**
```bash
docker exec -it <ollama-container-name> ollama pull llama3.2
```

**5. Set up the database**

Connect to TimescaleDB and run:
```sql
CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS metrics (
    time   TIMESTAMPTZ      NOT NULL,
    name   TEXT             NOT NULL,
    value  DOUBLE PRECISION NOT NULL,
    tags   JSONB
);

CREATE TABLE IF NOT EXISTS alerts (
    id                  BIGSERIAL PRIMARY KEY,
    time                TIMESTAMPTZ      NOT NULL,
    metric_name         TEXT             NOT NULL,
    value               DOUBLE PRECISION NOT NULL,
    z_score             DOUBLE PRECISION NOT NULL,
    mean                DOUBLE PRECISION NOT NULL,
    stddev              DOUBLE PRECISION NOT NULL,
    severity            TEXT             NOT NULL,
    explanation         TEXT,
    likely_cause        TEXT,
    recommended_action  TEXT,
    cooldown_expires_at TIMESTAMPTZ
);

SELECT create_hypertable('metrics', by_range('time'));
```

**6. Start the app**
```bash
./mvnw spring-boot:run
```

**7. Open the dashboard**
```http request
http://localhost:8080/
```

---

## Testing the System

**Send a metric manually:**
```bash
curl -X POST http://localhost:8080/metrics \
  -H "Content-Type: application/json" \
  -d '{"name": "api_response_time_ms", "value": 142.5}'
```

**Trigger an anomaly:**
```bash
curl -X POST http://localhost:8080/metrics \
  -H "Content-Type: application/json" \
  -d '{"name": "api_response_time_ms", "value": 99999}'
```

**Get a forecast:**
```bash
curl "http://localhost:8080/forecast/api_response_time_ms?threshold=500"
```

**Query alerts:**
```bash
curl "http://localhost:8080/alerts?severity=CRITICAL&page=0&size=10"
```

**Control the simulator:**
```bash
curl -X POST http://localhost:8080/simulator/stop
curl -X POST http://localhost:8080/simulator/start
curl http://localhost:8080/simulator/status
```

---

## Project Structure
```text
src/
└── main/
    ├── java/com/pms/nervecentre/
    │   ├── Controller/
    │   │   ├── MetricController.java
    │   │   ├── AlertController.java
    │   │   ├── ForecastController.java
    │   │   └── SimulatorController.java
    │   ├── Service/
    │   │   ├── MetricStreamConsumer.java
    │   │   ├── MetricStreamService.java
    │   │   ├── AnomalyDetectionService.java
    │   │   ├── AlertEnrichmentService.java
    │   │   ├── LlmExplanationService.java
    │   │   ├── ForecastingService.java
    │   │   ├── AlertService.java
    │   │   ├── DashboardPublisher.java
    │   │   └── MetricSimulatorService.java
    │   ├── Model/
    │   │   ├── Metric.java
    │   │   └── Alert.java
    │   ├── Repository/
    │   │   ├── MetricRepository.java
    │   │   └── AlertRepository.java
    │   └── Config/
    │       ├── RedisConfig.java
    │       └── WebSocketConfig.java
    └── resources/
        ├── static/
        │   └── index.html
        ├── application.properties.example
        └── application.properties
```
---

## Build Phases

| Phase | What Was Built |
|---|---|
| Phase 1 | POST /metrics → Redis Streams ingestion pipeline |
| Phase 2 | Stream consumer → TimescaleDB persistence |
| Phase 3 | Z-score anomaly detection → alerts table |
| Phase 4 | Async LLM enrichment via Ollama (llama3.2) |
| Phase 5 | Forecasting + Docker Compose + Live Dashboard |
| Extras | Alert cooldown, metric simulator, simulator toggle API |

---

## Key Design Decisions

**Why Redis Streams over a simple queue?**
Consumer groups give at-least-once delivery guarantees. If the app
restarts mid-processing, unacknowledged messages are redelivered.
A simple pub/sub channel would lose them.

**Why separate statistical detection from LLM diagnosis?**
Statistical detection runs in microseconds synchronously. LLM diagnosis
takes 5-30 seconds. Mixing them would make every alert slow. The async
enrichment pattern keeps alerting instant and diagnosis thorough.

**Why cooldown windows?**
A metric that stays elevated for 5 minutes without cooldown generates
hundreds of duplicate alerts — one per consumer poll. Cooldown suppresses
duplicates and prevents alert fatigue, which is how production systems
like PagerDuty handle this problem.

**Why TimescaleDB over plain PostgreSQL?**
TimescaleDB's hypertables automatically partition data by time, keeping
queries fast as the dataset grows. The time_bucket function turns raw
rows into aggregated windows in a single SQL query.

---

*Built by Olisemelie David Azuka*