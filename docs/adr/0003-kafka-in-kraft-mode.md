# 0003: Kafka (official image, KRaft mode) as the message broker

- **Status:** accepted
- **Date:** 2026-10-30

## Context
Services communicate asynchronously through events (see `docs/events.md`). We need a message broker locally.

## Options
1. **Apache Kafka** (official `apache/kafka` image)
2. **Redpanda:** Kafka-compatible, a single binary, lighter on RAM
3. **RabbitMQ:** a classic message queue with a different model (queues/exchanges rather than a log)

## Decision
**Option 1: Apache Kafka 4.3, single node, KRaft mode.**

## Why
- Kafka is the industry standard for event-driven microservices, and it's what you'll meet at work.
- **KRaft mode** means Kafka manages its own metadata. Older setups needed a separate **ZooKeeper** container. Kafka 4.x removed ZooKeeper completely, so many older tutorials with a `zookeeper` service are outdated.
- One node is enough locally. Replication factors are set to 1 because there are no other brokers to copy data to.

## Consequences
- Kafka uses more memory than Redpanda. The heap is capped at 512 MB. If the Codespace runs short on RAM, Redpanda is a drop-in replacement, because Spring Kafka code doesn't change.
- The broker exposes **two listeners**: `localhost:9092` for apps running in your IDE, and `kafka:29092` for apps running inside Docker. That's a common source of confusion, and it's explained in `docs/phases/phase-0.md`.
