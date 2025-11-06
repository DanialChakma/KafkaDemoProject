🧩 Distributed Order Management System — Kafka-Based Microservices Saga

📘 Overview

This project demonstrates how loosely coupled microservices can coordinate complex business workflows using Apache Kafka as a message broker.
It simulates a distributed Order Management System where each service (Order, Inventory, Payment, and Saga Orchestrator) operates independently while ensuring data consistency across the entire system through the Saga pattern.

Instead of relying on direct REST calls or shared databases, services communicate asynchronously via Kafka topics, making the architecture fault-tolerant, scalable, and resilient to partial failures.

🧠 Key Architectural Concepts

🧵 1. Saga Orchestration Pattern

Coordinates the overall transaction flow (e.g., order creation → inventory reservation → payment → order completion).

Ensures event-driven state transitions and compensating actions in case of failures.

Maintains a Saga log for event idempotency and recovery.


⚙️ 2. Event-Driven Communication via Kafka

Each microservice produces and consumes events through dedicated Kafka topics.

Example topics:

order.created

inventory.reserve.request / inventory.reserved / inventory.failed

payment.process / payment.succeeded / payment.failed

order.completed / order.cancelled

This pattern eliminates tight coupling between services — they don’t call each other directly.


🧾 3. Outbox Pattern for Reliability

Each service stores outgoing events in an outbox table before publishing to Kafka.

A background scheduler (or Kafka retry job) ensures event delivery even if Kafka is temporarily unavailable.

Prevents data loss and guarantees at-least-once delivery.


🛡️ 4. Asynchronous Validation Pattern

Before order creation, Order Service asynchronously validates:

Customer existence (via Customer Service)

Product availability (via Inventory Service)

Kafka topics like customer.validate.request and inventory.validate.request decouple this process — Order Service continues without blocking network calls.


🔁 5. Compensation & Failure Handling

If a downstream service fails (e.g., payment failure), Saga orchestrator triggers compensation actions:

Refund payment (via payment.refund)

Release reserved inventory (via inventory.release)

Update order status to CANCELLED

This ensures eventual consistency across services.

🧮 High-Level Flow Example
Customer places Order
        │
        ▼
Order Service → publishes → order.created
        │
        ▼
Saga Orchestrator → sends → inventory.reserve.request
        │
        ▼
Inventory Service → reserves → emits inventory.reserved
        │
        ▼
Saga Orchestrator → triggers → payment.process
        │
        ▼
Payment Service → confirms → emits payment.succeeded
        │
        ▼
Saga Orchestrator → publishes → order.completed ✅


If any step fails (e.g., payment), Saga orchestrator automatically:

→ payment.refund
→ inventory.release
→ order.cancelled ❌
