package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentType
import com.example.data.model.QuotationStatus
import com.example.data.model.ServiceState

@Entity(tableName = "technicians")
data class TechnicianEntity(
  @PrimaryKey val id: String,
  val name: String,
  val specialty: String,
  val yearsExperience: Int,
  val rating: Float,
  val reviewsCount: Int,
  val bio: String,
  val baseHourlyRate: Double,
  val cvTitle: String,
  val cvVerified: Boolean,
  val isVerified: Boolean,
  val paymentTypes: String, // Comma separated: YAPE,PLIN,EFECTIVO
  val phone: String,
  val location: String,
  val district: String = "San Isidro",
  val latitude: Double = -12.0969,
  val longitude: Double = -77.0345,
  val isOnline: Boolean = true,
  val distanceKm: Double = 1.2
)

@Entity(tableName = "service_requests")
data class ServiceRequestEntity(
  @PrimaryKey val id: String,
  val clientName: String,
  val title: String,
  val description: String,
  val category: String,
  val clientAddress: String,
  val clientDistrict: String,
  val clientLatitude: Double,
  val clientLongitude: Double,
  val maxRadiusKm: Double = 5.0,
  val urgency: String = "Inmediato",
  val proposedInspectionFee: Double = 30.0,
  val isZeroVisitCondition: Boolean = false,
  val conditionTerms: String = "Visita S/ 0 válida SI Y SOLO SI la avería física en sitio coincide 100% con lo indicado en la app. Si difiere o hay vicios ocultos, aplicará tarifa de diagnóstico técnico.",
  val status: String = "SEARCHING", // "SEARCHING", "ACCEPTED", "CANCELLED"
  val acceptedByTechId: String? = null,
  val acceptedByTechName: String? = null,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "services")
data class ServiceEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String,
  val clientName: String,
  val technicianId: String,
  val technicianName: String,
  val state: ServiceState,
  val laborCost: Double,
  val materialsCost: Double,
  val totalCost: Double,
  val estimatedTime: String,
  val paymentType: PaymentType?,
  val paymentStatus: PaymentStatus,
  val voucherNote: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  // Inspección y Diagnóstico en Sitio
  val inspectionFee: Double = 30.0,
  val isInspectionActive: Boolean = true,
  val inspectionDate: String? = null,
  val diagnosticNotes: String? = null,
  val isInspectionDeductedFromTotal: Boolean = true,
  // Condición de Visita S/ 0 si el trabajo coincide tal cual lo indicado
  val isZeroVisitCondition: Boolean = false,
  val workMatchConfirmed: Boolean? = null, // null = pendiente de validación, true = coincide (S/ 0), false = difiere (tarifa aplicada)
  val workMatchDifferenceReason: String? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val serviceId: String,
  val senderId: String, // "CLIENT" or "TECH"
  val senderName: String,
  val text: String,
  val isQuotation: Boolean = false,
  val laborCost: Double = 0.0,
  val materialsCost: Double = 0.0,
  val totalCost: Double = 0.0,
  val estimatedTime: String = "",
  val quotationStatus: QuotationStatus = QuotationStatus.PENDING,
  val isVoucher: Boolean = false,
  val isInspectionOffer: Boolean = false,
  val inspectionFee: Double = 0.0,
  val inspectionDate: String = "",
  val isDiagnosticReport: Boolean = false,
  val diagnosticFindings: String = "",
  val isZeroVisitCondition: Boolean = false,
  val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class ReviewEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val serviceId: String,
  val technicianId: String,
  val clientName: String,
  val overallRating: Float,
  val punctualityRating: Float,
  val qualityRating: Float,
  val cleanlinessRating: Float,
  val comment: String,
  val timestamp: Long = System.currentTimeMillis()
)
