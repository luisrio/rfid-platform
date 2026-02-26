# RFID Platform Monorepo

## Project Overview

This project simulates and processes RFID events for a retail environment using an event-driven architecture over MQTT.
It models the journey of tagged products through store checkpoints (cashier, fitting room, and exit gate), evaluates business rules, and emits security alerts when needed.

The platform is designed to:

- Generate realistic RFID traffic for testing and demos.
- Process RFID and payment events in near real time.
- Detect unpaid products at store exit.
- Publish security alerts to MQTT topics for downstream systems.
- Run the full environment with a single Docker Compose command.

## Objectives

- Provide a practical reference for event-driven integration with MQTT.
- Separate event production and event consumption responsibilities.
- Enable reproducible local execution for development, QA, and demonstrations.
- Keep infrastructure lightweight (Mosquitto + MQTT Explorer + containerized services).

## Repository Structure

```text
rfid-platform/
  rfid-consumer/
  rfid-producer/
  rfid-network/
```

## Components

### `rfid-producer`

`rfid-producer` is the event generator. It continuously publishes simulated RFID readings and payment confirmations to MQTT topics.

Main responsibilities:

- Simulate product movement scenarios across store readers.
- Publish events for cashier, fitting room, and exit gate readers.
- Publish payment confirmation events.
- Support traffic profiles (for example `low`, `medium`, `high`) to adjust event intensity.

Typical published topics include:

- `tienda/lecturas/caja/CAJA-01`
- `tienda/lecturas/probador/PROBADOR-01`
- `tienda/lecturas/salida/GATE-01`
- `tienda/pagos/confirmados`

### `rfid-consumer`

`rfid-consumer` is the business-processing service. It subscribes to RFID/payment topics, applies filtering and deduplication rules, tracks paid EPCs, and decides whether a product exiting the store is paid.

Main responsibilities:

- Consume RFID and payment events from MQTT.
- Apply quality and deduplication filters.
- Correlate payment confirmations with product EPCs.
- Detect unpaid exit events.
- Publish security alerts when an unpaid EPC is detected at the exit.

Typical alert topic:

- `tienda/alertas/seguridad`

### `rfid-network`

Infrastructure and orchestration layer.

Main responsibilities:

- Run Mosquitto MQTT broker.
- Run MQTT Explorer web UI for topic inspection.
- Orchestrate producer and consumer containers with Docker Compose.

## End-to-End Flow

1. `rfid-producer` emits RFID readings and payment events to MQTT topics.
2. Mosquitto brokers all messages.
3. `rfid-consumer` subscribes to those topics and processes each event.
4. If an exit event references an unpaid EPC, `rfid-consumer` publishes a security alert.
5. MQTT Explorer can be used to observe all topics and payloads in real time.

## Prerequisites

- Docker Desktop (includes Docker Compose)
- Git
- Optional: Java 21 + Maven (only needed to run apps outside Docker)

## One-command Startup

Run from the network folder:

```bash
cd rfid-network
docker compose up --build -d
```

This starts:

- Mosquitto broker on `localhost:1883`
- MQTT Explorer Web UI on `http://localhost:4000`
- `rfid-producer`
- `rfid-consumer`

## Verify Services

```bash
cd rfid-network
docker compose ps
docker compose logs -f rfid-producer rfid-consumer mosquitto mqtt-explorer
```

## MQTT Explorer Connection Settings

If using the Dockerized MQTT Explorer UI (`http://localhost:4000`):

- Host: `mosquitto`
- Port: `1883`
- Protocol: MQTT/TCP (no TLS)
- Username/Password: empty

If using MQTT Explorer Desktop on your machine:

- Host: `localhost`
- Port: `1883`
- Protocol: MQTT/TCP (no TLS)
- Username/Password: empty

## Stop the Platform

```bash
cd rfid-network
docker compose down
```

Remove persisted volumes too:

```bash
docker compose down -v
```

## Rebuild Images After Dockerfile Changes

```bash
cd rfid-network
docker compose up -d --build
```

Force no-cache rebuild:

```bash
docker compose build --no-cache rfid-producer rfid-consumer
docker compose up -d
```
