# Documento de Especificação Técnica: Food Delivery Saga System (v2.0)

**Projeto:** Sistema de Delivery de Comida com Saga Pattern

**Stack Base:** Kotlin 2.3 | Java 25 | Spring Boot 4.0.1

**Estilo:** Orchestration Saga Pattern (Coroutines + Virtual Threads)

---

## 1. Visão Geral da Arquitetura

Mantemos a arquitetura híbrida (REST + Kafka), mas a implementação interna dos serviços muda drasticamente para aproveitar a concorrência estruturada.

* **Runtime:** JVM 25 (ZGC Generation Z como Garbage Collector padrão).
* **Concorrência:** Kotlin Coroutines rodando sobre Java 25 Virtual Threads (Project Loom).

---

## 2. Definição dos Serviços (Kotlin Spec)

### 2.1. Order Service (Orquestrador)

* **Linguagem:** Kotlin 2.3.
* **Paradigma:** Imperativo com Coroutines (suspending functions).
* **Modelagem de Domínio:** Uso de **Sealed Interfaces** para representar os estados da Saga de forma segura (Compile-time safety).
* **Comunicação:** `RestClient` (novo cliente síncrono do Spring com suporte a Virtual Threads) e `Spring for Apache Kafka`.

### 2.2. Payment Service (Integrador)

* **Linguagem:** Kotlin 2.3.
* **API:** Spring MVC (Blocking style, mas rodando em Virtual Threads). Isso simplifica o código, removendo a necessidade de WebFlux complexo, mantendo alta performance.

### 2.3. Kitchen & Delivery Services (Consumers)

* **Linguagem:** Kotlin 2.3.
* **Processamento:** Kafka Listeners usando `runBlocking` ou escopos supervisionados para garantir que falhas não derrubem o consumidor.

---

## 3. Especificação de Contratos (Data Classes & Specs)

### 3.1. Tipos Fortes (Domain Primitives)

```kotlin
@JvmInline
value class OrderId(val value: UUID)

@JvmInline
value class MonetaryAmount(val value: BigDecimal)
```

### 3.2. Integração Financeira (REST Spec - Kotlin DTO)

**Interface:** `POST /api/v1/payments/authorize`

```kotlin
data class PaymentRequest(
    val orderId: OrderId,
    val amount: MonetaryAmount,
    val paymentMethod: PaymentMethod // Enum
)

data class PaymentResponse(
    val transactionId: String,
    val status: PaymentStatus // Enum: AUTHORIZED, REJECTED
)
```

### 3.3. Eventos Kafka (Kotlin Data Classes)

```kotlin
// Kitchen Context
sealed interface KitchenEvent {
    val orderId: OrderId
    
    data class TicketAccepted(
        override val orderId: OrderId, 
        val estimatedPrepTimeMinutes: Int
    ) : KitchenEvent
    
    data class TicketRejected(
        override val orderId: OrderId, 
        val reason: String
    ) : KitchenEvent
}
```

---

## 4. Máquina de Estados da Saga (Kotlin Sealed Approach)

```kotlin
sealed interface SagaState {
    data object Created : SagaState
    data object PaymentPending : SagaState
    data object Paid : SagaState
    data object KitchenPending : SagaState
    data object DeliveryPending : SagaState
    data class Completed(val completedAt: Instant) : SagaState
    data class Cancelled(val reason: String) : SagaState
}
```

---

## 5. Fluxo de Orquestração

```kotlin
suspend fun processOrderSaga(order: Order) = coroutineScope {
    
    // Passo 1: Pagamento (Síncrono via Virtual Thread)
    val paymentResult = paymentClient.authorize(order.toPaymentRequest())
    
    if (paymentResult.status == REJECTED) {
        orderRepository.updateStatus(order.id, SagaState.Cancelled("Payment Rejected"))
        return@coroutineScope
    }

    // Passo 2: Cozinha (Async via Kafka)
    kafkaTemplate.send("kitchen.commands", PrepareOrder(order.id, order.items))
    orderRepository.updateStatus(order.id, SagaState.KitchenPending)
}
```

---

## 6. Stack Tecnológica Detalhada

| Componente | Tecnologia | Versão |
|------------|------------|--------|
| Framework | Spring Boot | 4.0.1 |
| Linguagem | Kotlin | 2.3 |
| JVM | Java | 25 (LTS) |
| Mensageria | Apache Kafka | 3.9+ |
| Build Tool | Gradle | 9.0 |
| Database | PostgreSQL | 17 |
| GC | ZGC Generational | - |

---

## 7. Configuração de Virtual Threads

```yaml
spring:
  threads:
    virtual:
      enabled: true
```
