# Food Delivery Saga System - Documentation

A food delivery system implementing the **Orchestration Saga Pattern** with Kotlin 2.3, Java 25, and Spring Boot 4.0.1.

---

## What is the Saga Pattern?

The **Saga Pattern** solves distributed transactions in microservices. Instead of a global ACID transaction, each service executes its local transaction and publishes events. If something fails, **compensating transactions** undo previous changes.

### Orchestration vs Choreography

| Aspect | Orchestration ✓ | Choreography |
|--------|-----------------|--------------|
| Coordination | Centralized (Orchestrator) | Decentralized |
| Flow | Easy to understand | More complex |
| Debugging | Simple | Difficult |
| Coupling | Orchestrator knows all | Services know each other |

**This project uses Orchestration** with `OrderService` as the orchestrator.

---

## Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                    ORDER SERVICE (Orchestrator)               │
│                         Port: 8080                            │
│  ┌─────────────────┐  ┌─────────────┐  ┌──────────────────┐   │
│  │SagaOrchestrator │─▶│PaymentClient│  │  KafkaProducer   │  │
│  └────────┬────────┘  └──────┬──────┘  └────────┬─────────┘  │
└───────────┼──────────────────┼──────────────────┼────────────┘
            │ Kafka            │ REST             │ Kafka
            ▼                  ▼                  ▼
┌───────────────────┐  ┌─────────────┐  ┌────────────────────┐
│  KITCHEN SERVICE  │  │   PAYMENT   │  │  DELIVERY SERVICE  │
│    Port: 8082     │  │    8081     │  │     Port: 8083     │
└───────────────────┘  └─────────────┘  └────────────────────┘
```

---

## Saga Flow

```
1. Client POST /orders ──────────────────▶ OrderService
   
2. OrderService ─── REST POST /authorize ──▶ PaymentService
   ◀─────────────── AUTHORIZED ────────────
   
3. OrderService ─── Kafka: PrepareOrder ──▶ KitchenService
   ◀─────────────── TicketAccepted ────────
   
4. OrderService ─── Kafka: ScheduleDelivery ──▶ DeliveryService
   ◀─────────────── DeliveryCompleted ─────────
   
5. Order status: COMPLETED ✓
```

### Compensation (Rollback)

If the **kitchen rejects** the order after payment:

```
1. KitchenService ──▶ Kafka: TicketRejected ──▶ OrderService
2. OrderService ───▶ REST: /refund ───────────▶ PaymentService
3. Order status: CANCELLED
```

---

## State Machine

| State | Description |
|-------|-------------|
| `CREATED` | Order created |
| `PAYMENT_PENDING` | Waiting for authorization |
| `PAID` | Payment authorized |
| `KITCHEN_PENDING` | Waiting for kitchen |
| `DELIVERY_PENDING` | Waiting for delivery |
| `COMPLETED` | ✓ Finished |
| `CANCELLED` | ✗ Cancelled (with reason) |
| `FAILED` | ✗ Processing error |

---

## How to Test

### 1. Start Infrastructure

```bash
docker-compose up -d
```

### 2. Run Services (4 terminals)

```bash
.\gradlew :payment-service:bootRun   # Terminal 1
.\gradlew :kitchen-service:bootRun   # Terminal 2
.\gradlew :delivery-service:bootRun  # Terminal 3
.\gradlew :order-service:bootRun     # Terminal 4
```

### 3. Create Order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "deliveryAddress": "123 Main Street",
    "paymentMethod": "CREDIT_CARD",
    "items": [
      {"productId": "pizza", "productName": "Pizza", "quantity": 2, "unitPrice": 45.90}
    ]
  }'
```

### 4. Check Order

```bash
curl http://localhost:8080/api/v1/orders/{id}
```

---

## Response Examples

### ✅ Success Scenario

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "deliveryAddress": "123 Main Street",
    "paymentMethod": "CREDIT_CARD",
    "items": [
      {"productId": "pizza", "productName": "Pizza Margherita", "quantity": 2, "unitPrice": 45.90}
    ]
  }'
```

**Response (HTTP 202 Accepted):**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "deliveryAddress": "123 Main Street",
  "totalAmount": 91.80,
  "status": "KITCHEN_PENDING",
  "transactionId": "TXN-A8B2C4D1",
  "items": [
    {
      "productId": "pizza",
      "productName": "Pizza Margherita",
      "quantity": 2,
      "unitPrice": 45.90,
      "totalPrice": 91.80
    }
  ],
  "createdAt": "2026-01-02T15:30:00Z",
  "updatedAt": "2026-01-02T15:30:01Z"
}
```

**Log Output (Success Flow):**
```
OrderService:    Initiating saga for new order from customer 550e8400...
PaymentService:  Authorizing payment for order a1b2c3d4..., amount: 91.80
PaymentService:  Payment AUTHORIZED, transaction: TXN-A8B2C4D1
OrderService:    Payment authorized, sending to kitchen
KitchenService:  Processing PrepareOrder for a1b2c3d4... with 1 items
KitchenService:  Ticket accepted for order a1b2c3d4..., ETA: 15 min
OrderService:    Kitchen accepted, scheduling delivery
DeliveryService: Scheduling delivery for order a1b2c3d4...
DeliveryService: Delivery scheduled, driver: Driver 42, ETA: 45 min
DeliveryService: Order a1b2c3d4... delivered successfully!
OrderService:    Order a1b2c3d4... completed!
```

**Final Status:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "COMPLETED",
  "completedAt": "2026-01-02T16:15:00Z"
}
```

---

### ❌ Failure Scenario (Kitchen Rejection)

**Response (After Kitchen Rejects):**
```json
{
  "id": "x9y8z7w6-v5u4-3210-fedc-ba0987654321",
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "CANCELLED",
  "cancellationReason": "Kitchen rejected: Kitchen at full capacity",
  "transactionId": "TXN-X1Y2Z3W4"
}
```

**Log Output (Compensation Flow):**
```
OrderService:    Initiating saga for new order from customer 550e8400...
PaymentService:  Authorizing payment for order x9y8z7w6..., amount: 91.80
PaymentService:  Payment AUTHORIZED, transaction: TXN-X1Y2Z3W4
OrderService:    Payment authorized, sending to kitchen
KitchenService:  Processing PrepareOrder for x9y8z7w6...
KitchenService:  ⚠️ Ticket REJECTED for order x9y8z7w6...: Kitchen at full capacity
OrderService:    Kitchen rejected order x9y8z7w6...: Kitchen at full capacity
OrderService:    ⚠️ Starting compensation - refunding payment
PaymentService:  Processing refund for order x9y8z7w6..., transaction: TXN-X1Y2Z3W4
PaymentService:  Refund successful
OrderService:    Order x9y8z7w6... cancelled and refunded
```

---

### ❌ Failure Scenario (Payment Rejected)

**Response (HTTP 202 Accepted, but order cancelled):**
```json
{
  "id": "p1q2r3s4-t5u6-7890-mnop-qr1234567890",
  "status": "CANCELLED",
  "cancellationReason": "Payment REJECTED: Insufficient funds",
  "transactionId": null
}
```

**Log Output:**
```
OrderService:    Initiating saga for new order from customer 550e8400...
PaymentService:  Authorizing payment for order p1q2r3s4..., amount: 91.80
PaymentService:  ❌ Payment REJECTED: Insufficient funds
OrderService:    Payment failed for order p1q2r3s4..., saga cancelled
```

---

## Run Tests

```bash
# Shared kernel unit tests
.\gradlew :shared-kernel:test

# All tests
.\gradlew test
```

---

## Kafka Topics

| Topic | Producer | Consumer |
|-------|----------|----------|
| `kitchen.commands` | Order | Kitchen |
| `kitchen.events` | Kitchen | Order |
| `delivery.commands` | Order | Delivery |
| `delivery.events` | Delivery | Order |
| `order.events` | Order | External |

---

## References

- [Saga Pattern - Microservices.io](https://microservices.io/patterns/data/saga.html)
- [Project Loom - Virtual Threads](https://openjdk.org/jeps/444)
