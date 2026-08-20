package com.example.data.model

enum class UserRole {
  CLIENTE,
  TECNICO,
  ADMIN
}

enum class ServiceState(val label: String, val progress: Float) {
  SOLICITADO("Solicitado", 0.15f),
  VISITA_PENDIENTE("Visita Diagnóstico Propuesta", 0.3f),
  VISITA_AGENDADA("Visita de Inspección Agendada", 0.45f),
  DIAGNOSTICO_REALIZADO("Diagnóstico en Sitio Realizado", 0.6f),
  PRESUPUESTADO("Presupuesto Definitivo", 0.7f),
  EN_PROCESO("Trabajo en Proceso", 0.85f),
  PAGO_PENDIENTE("Pago Pendiente", 0.95f),
  COMPLETADO("Completado", 1.0f),
  CANCELADO("Cancelado", 0.0f)
}

enum class PaymentType(val displayName: String) {
  YAPE("Yape"),
  PLIN("Plin"),
  TRANSFERENCIA("Transferencia"),
  EFECTIVO("Efectivo"),
  TARJETA("Tarjeta")
}

enum class PaymentStatus(val label: String) {
  PENDIENTE("Pendiente"),
  COMPROBANTE_ENVIADO("Comprobante Enviado"),
  CONFIRMADO_POR_TECNICO("Confirmado por Técnico")
}

enum class QuotationStatus {
  PENDING,
  ACCEPTED,
  REJECTED
}

data class PortfolioItem(
  val id: String,
  val title: String,
  val description: String,
  val beforeLabel: String = "Antes",
  val afterLabel: String = "Después"
)

data class PaymentDetails(
  val yapePhone: String = "987 654 321",
  val yapeQrCodeHolder: String = "Roberto García",
  val plinPhone: String = "987 654 321",
  val bankName: String = "BCP",
  val accountNumber: String = "193-98765432-0-12",
  val cci: String = "002-1930098765432012-14",
  val acceptsCash: Boolean = true
)
