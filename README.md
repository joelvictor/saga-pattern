# Food Delivery Saga System

Sistema de Delivery de Comida implementando o **Orchestration Saga Pattern** com stack bleeding-edge.

## Stack Tecnológica

| Tecnologia | Versão | Descrição |
|------------|--------|-----------|
| **Kotlin** | 2.3.0 | Linguagem principal com Context Receivers e K2 Compiler |
| **Java** | 25 (LTS) | Runtime com Virtual Threads e Project Valhalla |
| **Spring Boot** | 4.0.1 | Framework com suporte nativo a Virtual Threads |
| **Apache Kafka** | 3.9+ | Mensageria assíncrona entre serviços |
| **PostgreSQL** | 17 | Banco de dados (um por serviço) |
| **Gradle** | 9.0 | Build tool com Kotlin DSL |

## Arquitetura

```
┌─────────────────┐     REST      ┌─────────────────┐
│  Order Service  │──────────────▶│ Payment Service │
│  (Orchestrator) │               │   (Integrator)  │
└────────┬────────┘               └─────────────────┘
         │
         │ Kafka
         ▼
┌─────────────────┐               ┌─────────────────┐
│ Kitchen Service │◀─── Kafka ───│ Delivery Service│
│   (Consumer)    │               │    (Consumer)   │
└─────────────────┘               └─────────────────┘
```

## Estrutura de Módulos

```
food-delivery-saga/
├── shared-kernel/       # Domain primitives, events, saga states
├── order-service/       # Saga orchestrator (port 8080)
├── payment-service/     # Payment integration (port 8081)
├── kitchen-service/     # Kitchen processing (port 8082)
└── delivery-service/    # Delivery management (port 8083)
```

## Pré-requisitos

- Java 25 (LTS)
- Docker & Docker Compose
- Gradle 9.0+

## Quick Start

### 1. Iniciar Infraestrutura

```bash
docker-compose up -d postgres kafka
```

### 2. Build do Projeto

```bash
./gradlew build
```

### 3. Executar Serviços

```bash
# Terminal 1 - Order Service
./gradlew :order-service:bootRun

# Terminal 2 - Payment Service
./gradlew :payment-service:bootRun

# Terminal 3 - Kitchen Service
./gradlew :kitchen-service:bootRun

# Terminal 4 - Delivery Service
./gradlew :delivery-service:bootRun
```

### 4. Testar Fluxo

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "items": [
      {"productId": "pizza-margherita", "quantity": 2, "price": 45.00}
    ],
    "deliveryAddress": "Rua Example, 123"
  }'
```

## Fluxo da Saga

1. **Order Created** → Pedido criado com status `CREATED`
2. **Payment Authorization** → Chamada síncrona (REST) ao Payment Service
3. **Kitchen Notification** → Comando assíncrono (Kafka) para Kitchen Service
4. **Delivery Scheduling** → Comando assíncrono (Kafka) para Delivery Service
5. **Order Completed** → Pedido finalizado com status `COMPLETED`

### Compensação (Rollback)

- Se Kitchen rejeitar → Estorno de pagamento automático
- Se Delivery falhar → Notificação ao cliente + reembolso

## Configuração

Cada serviço possui seu próprio banco de dados PostgreSQL:

| Serviço | Database | Porta |
|---------|----------|-------|
| order-service | order_db | 5432 |
| payment-service | payment_db | 5433 |
| kitchen-service | kitchen_db | 5434 |
| delivery-service | delivery_db | 5435 |

## Tópicos Kafka

| Tópico | Producer | Consumer |
|--------|----------|----------|
| `kitchen.commands` | Order Service | Kitchen Service |
| `kitchen.events` | Kitchen Service | Order Service |
| `delivery.commands` | Order Service | Delivery Service |
| `delivery.events` | Delivery Service | Order Service |

## Licença

MIT License