# event-contracts

Shared definitions of the events exchanged over Kafka.

Options to consider (an ADR is a good place to record the choice):
1. A small Java library (records) that services depend on. Simple, but couples deployments.
2. JSON Schema / Avro files + code generation. More decoupled.
3. No shared lib, and each service defines its own copy. Most decoupled, but needs contract tests.

Every event should carry an `eventId`, `occurredAt`, `type`, and a payload. Document them in `docs/events.md`.
