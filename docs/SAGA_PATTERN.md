# Food Delivery Saga System - Documentação

Sistema de Delivery usando **Orchestration Saga Pattern** com Kotlin 2.3, Java 25 e Spring Boot 4.0.1.

---

## O que é o Saga Pattern?

O **Saga Pattern** resolve transações distribuídas em microserviços. Em vez de uma transação ACID global, cada serviço executa sua transação local e publica eventos. Se algo falha, **transações compensatórias** desfazem as mudanças anteriores.

### Orchestration vs Choreography

| Aspecto | Orchestration ✓ | Choreography |
|---------|-----------------|--------------|
| Coordenação | Centralizada (Orchestrator) | Descentralizada |
| Fluxo | Fácil de entender | Mais complexo |
| Debugging | Simples | Difícil |
| Acoplamento | Orchestrator conhece todos | Serviços se conhecem |

**Este projeto usa Orchestration** com `OrderService` como orquestrador.

---

## Arquitetura

```
┌──────────────────────────────────────────────────────────────┐
│                    ORDER SERVICE (Orchestrator)               │
│                         Port: 8080                            │
│  ┌─────────────────┐  ┌─────────────┐  ┌──────────────────┐  │
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

## Fluxo da Saga

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

### Compensação (Rollback)

Se a **cozinha rejeita** o pedido após o pagamento:

```
1. KitchenService ──▶ Kafka: TicketRejected ──▶ OrderService
2. OrderService ───▶ REST: /refund ───────────▶ PaymentService
3. Order status: CANCELLED
```

---

## Máquina de Estados

| Estado | Descrição |
|--------|-----------|
| `CREATED` | Pedido criado |
| `PAYMENT_PENDING` | Aguardando autorização |
| `PAID` | Pagamento autorizado |
| `KITCHEN_PENDING` | Aguardando cozinha |
| `DELIVERY_PENDING` | Aguardando entrega |
| `COMPLETED` | ✓ Finalizado |
| `CANCELLED` | ✗ Cancelado (com motivo) |
| `FAILED` | ✗ Erro no processamento |

---

## Como Testar

### 1. Iniciar Infraestrutura

```bash
docker-compose up -d
```

### 2. Rodar Serviços (4 terminais)

```bash
.\gradlew :payment-service:bootRun   # Terminal 1
.\gradlew :kitchen-service:bootRun   # Terminal 2
.\gradlew :delivery-service:bootRun  # Terminal 3
.\gradlew :order-service:bootRun     # Terminal 4
```

### 3. Criar Pedido

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "deliveryAddress": "Rua das Flores, 123",
    "paymentMethod": "CREDIT_CARD",
    "items": [
      {"productId": "pizza", "productName": "Pizza", "quantity": 2, "unitPrice": 45.90}
    ]
  }'
```

### 4. Consultar Pedido

```bash
curl http://localhost:8080/api/v1/orders/{id}
```

---

## Rodar Testes

```bash
# Testes unitários do shared-kernel
.\gradlew :shared-kernel:test

# Todos os testes
.\gradlew test
```

---

## Tópicos Kafka

| Tópico | Producer | Consumer |
|--------|----------|----------|
| `kitchen.commands` | Order | Kitchen |
| `kitchen.events` | Kitchen | Order |
| `delivery.commands` | Order | Delivery |
| `delivery.events` | Delivery | Order |
| `order.events` | Order | Externos |

---

## Referências

- [Saga Pattern - Microservices.io](https://microservices.io/patterns/data/saga.html)
- [Project Loom - Virtual Threads](https://openjdk.org/jeps/444)
