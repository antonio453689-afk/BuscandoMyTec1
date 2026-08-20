package com.example.data.repository

import com.example.data.local.TecniRedDatabase
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ReviewEntity
import com.example.data.local.entities.ServiceEntity
import com.example.data.local.entities.ServiceRequestEntity
import com.example.data.local.entities.TechnicianEntity
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentType
import com.example.data.model.QuotationStatus
import com.example.data.model.ServiceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.math.*

class TecniRedRepository(private val db: TecniRedDatabase) {

  val technicians: Flow<List<TechnicianEntity>> = db.technicianDao().getAllTechnicians()
  val services: Flow<List<ServiceEntity>> = db.serviceDao().getAllServices()
  val activeServiceRequests: Flow<List<ServiceRequestEntity>> = db.serviceRequestDao().getActiveSearchingRequests()

  fun getServiceById(id: String): Flow<ServiceEntity?> = db.serviceDao().getServiceById(id)

  fun getRequestById(id: String): Flow<ServiceRequestEntity?> = db.serviceRequestDao().getRequestById(id)

  fun getMessages(serviceId: String): Flow<List<ChatMessageEntity>> =
    db.chatMessageDao().getMessagesForService(serviceId)

  fun getReviews(technicianId: String): Flow<List<ReviewEntity>> =
    db.reviewDao().getReviewsForTechnician(technicianId)

  suspend fun getTechnician(id: String): TechnicianEntity? =
    db.technicianDao().getTechnicianById(id)

  suspend fun createService(service: ServiceEntity) = db.serviceDao().insertService(service)

  suspend fun broadcastServiceRequest(request: ServiceRequestEntity) {
    db.serviceRequestDao().insertRequest(request)
  }

  suspend fun cancelServiceRequest(requestId: String) {
    db.serviceRequestDao().cancelRequest(requestId)
  }

  suspend fun updateRequestRadius(requestId: String, radius: Double) {
    db.serviceRequestDao().updateRequestRadius(requestId, radius)
  }

  /**
   * Accepts an incoming broadcast request as a Technician (Uber/InDrive style).
   * Creates an active service and initial greeting/diagnostic offer chat.
   */
  suspend fun acceptBroadcastRequest(
    request: ServiceRequestEntity,
    tech: TechnicianEntity
  ): ServiceEntity {
    val newServiceId = "serv_${System.currentTimeMillis()}"
    
    // Mark the request as accepted
    db.serviceRequestDao().markRequestAccepted(
      requestId = request.id,
      status = "ACCEPTED",
      techId = tech.id,
      techName = tech.name
    )

    val actualFee = if (request.isZeroVisitCondition) 0.0 else request.proposedInspectionFee

    // Create active service instance
    val newService = ServiceEntity(
      id = newServiceId,
      title = request.title,
      description = "${request.description}\n📍 Dirección: ${request.clientAddress}, ${request.clientDistrict} (Urgencia: ${request.urgency})",
      clientName = request.clientName,
      technicianId = tech.id,
      technicianName = tech.name,
      state = ServiceState.VISITA_AGENDADA,
      laborCost = 0.0,
      materialsCost = 0.0,
      totalCost = actualFee,
      estimatedTime = "Llegada en 20-30 min",
      paymentType = PaymentType.YAPE,
      paymentStatus = PaymentStatus.PENDIENTE,
      inspectionFee = actualFee,
      isInspectionActive = true,
      inspectionDate = "Inmediata (${request.urgency})",
      diagnosticNotes = null,
      isInspectionDeductedFromTotal = true,
      isZeroVisitCondition = request.isZeroVisitCondition,
      workMatchConfirmed = null
    )
    db.serviceDao().insertService(newService)

    // Send initial messages into chat
    val matchConditionNotice = if (request.isZeroVisitCondition) {
      "\n⚖️ *Condición de Visita S/ 0:* Válida únicamente si el diagnóstico en sitio coincide 100% con lo reportado por el cliente. Si en sitio se detecta una avería distinta o fallas ocultas, aplicará tarifa de diagnóstico técnico."
    } else ""

    db.chatMessageDao().insertMessage(
      ChatMessageEntity(
        serviceId = newServiceId,
        senderId = "SYSTEM",
        senderName = "Radar TecniRed",
        text = "⚡ ¡Match en tu zona! El técnico ${tech.name} ha aceptado tu solicitud de servicio a ${String.format("%.1f", tech.distanceKm)} km de distancia.$matchConditionNotice",
        isZeroVisitCondition = request.isZeroVisitCondition
      )
    )

    val techGreeting = if (request.isZeroVisitCondition) {
      "¡Hola ${request.clientName}! He aceptado tu solicitud para ${request.title}. Estoy en camino hacia ${request.clientAddress}. Validaremos en sitio que el problema sea tal cual lo indicado para aplicar la Visita a S/ 0.00."
    } else {
      "¡Hola ${request.clientName}! He aceptado tu solicitud para ${request.title}. Estoy en camino hacia ${request.clientAddress}, ${request.clientDistrict}. Revisaremos el problema en sitio por la tarifa fija pactada de S/ ${String.format("%.2f", request.proposedInspectionFee)}."
    }

    db.chatMessageDao().insertMessage(
      ChatMessageEntity(
        serviceId = newServiceId,
        senderId = "TECH",
        senderName = tech.name,
        text = techGreeting,
        isInspectionOffer = true,
        inspectionFee = actualFee,
        inspectionDate = "Inmediato - En camino",
        isZeroVisitCondition = request.isZeroVisitCondition
      )
    )

    return newService
  }

  fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    val distanceKm = results[0] / 1000.0
    return (round(distanceKm * 10) / 10.0).coerceAtLeast(0.1)
  }

  suspend fun updateTechnicianDistances(clientLat: Double, clientLon: Double) {
    val currentTechs = db.technicianDao().getAllTechnicians().first()
    val updatedList = currentTechs.map { tech ->
      val dist = calculateDistanceKm(clientLat, clientLon, tech.latitude, tech.longitude)
      tech.copy(distanceKm = dist)
    }
    db.technicianDao().updateTechnicians(updatedList)
  }

  suspend fun updateServiceState(serviceId: String, newState: ServiceState) =
    db.serviceDao().updateServiceState(serviceId, newState)

  suspend fun selectPaymentAndSendVoucher(
    serviceId: String,
    paymentType: PaymentType,
    voucherNote: String
  ) {
    db.serviceDao().updatePaymentInfo(
      serviceId = serviceId,
      newState = ServiceState.PAGO_PENDIENTE,
      newPaymentStatus = PaymentStatus.COMPROBANTE_ENVIADO,
      paymentType = paymentType
    )
    // Add voucher notification into chat
    db.chatMessageDao().insertMessage(
      ChatMessageEntity(
        serviceId = serviceId,
        senderId = "CLIENT",
        senderName = "Cliente",
        text = "📎 He enviado el comprobante de pago vía $paymentType: $voucherNote",
        isVoucher = true
      )
    )
  }

  suspend fun confirmPaymentByTechnician(serviceId: String) {
    db.serviceDao().updatePaymentStatus(serviceId, PaymentStatus.CONFIRMADO_POR_TECNICO)
    db.serviceDao().updateServiceState(serviceId, ServiceState.COMPLETADO)
    db.chatMessageDao().insertMessage(
      ChatMessageEntity(
        serviceId = serviceId,
        senderId = "TECH",
        senderName = "Técnico",
        text = "✅ ¡Pago recibido y confirmado con éxito! El servicio ha sido marcado como Completado."
      )
    )
  }

  suspend fun acceptQuotation(serviceId: String, messageId: Long?) {
    if (messageId != null) {
      db.chatMessageDao().updateQuotationStatus(messageId, QuotationStatus.ACCEPTED)
    }
    db.serviceDao().updateServiceState(serviceId, ServiceState.EN_PROCESO)
    db.chatMessageDao().insertMessage(
      ChatMessageEntity(
        serviceId = serviceId,
        senderId = "CLIENT",
        senderName = "Cliente",
        text = "✅ He aceptado el presupuesto formal. ¡Quedo a la espera del inicio del trabajo!"
      )
    )
  }

  suspend fun rejectQuotation(serviceId: String, messageId: Long?) {
    if (messageId != null) {
      db.chatMessageDao().updateQuotationStatus(messageId, QuotationStatus.REJECTED)
    }
    db.serviceDao().updateServiceState(serviceId, ServiceState.CANCELADO)
    db.chatMessageDao().insertMessage(
      ChatMessageEntity(
        serviceId = serviceId,
        senderId = "CLIENT",
        senderName = "Cliente",
        text = "❌ He rechazado el presupuesto."
      )
    )
  }

  suspend fun sendChatMessage(
    serviceId: String,
    senderId: String,
    senderName: String,
    text: String
  ) {
    db.chatMessageDao().insertMessage(
      ChatMessageEntity(
        serviceId = serviceId,
        senderId = senderId,
        senderName = senderName,
        text = text
      )
    )
  }

  suspend fun proposeInspectionVisit(
    serviceId: String,
    technicianName: String,
    fee: Double,
    scheduledDate: String,
    notes: String
  ) {
    val existing = db.serviceDao().getServiceById(serviceId).first()
    if (existing != null) {
      val updated = existing.copy(
        inspectionFee = fee,
        inspectionDate = scheduledDate,
        isInspectionActive = true,
        state = ServiceState.VISITA_PENDIENTE
      )
      db.serviceDao().updateService(updated)
    }

    db.chatMessageDao().insertMessage(
      ChatMessageEntity(
        serviceId = serviceId,
        senderId = "TECH",
        senderName = technicianName,
        text = "🔎 Propuesta de Visita de Diagnóstico Presencial (Tarifa fija de inspección: S/ ${String.format("%.2f", fee)}). $notes",
        isInspectionOffer = true,
        inspectionFee = fee,
        inspectionDate = scheduledDate
      )
    )
  }

  suspend fun acceptInspectionVisit(serviceId: String) {
    val existing = db.serviceDao().getServiceById(serviceId).first()
    if (existing != null) {
      val updated = existing.copy(state = ServiceState.VISITA_AGENDADA)
      db.serviceDao().updateService(updated)
    }
    db.chatMessageDao().insertMessage(
      ChatMessageEntity(
        serviceId = serviceId,
        senderId = "CLIENT",
        senderName = "Cliente",
        text = "✅ He aceptado la Visita de Diagnóstico con tarifa fija. El técnico vendrá a evaluar físicamente el problema antes de fijar el presupuesto definitivo."
      )
    )
  }

  suspend fun completeSiteDiagnosticAndSubmitFinalBudget(
    serviceId: String,
    technicianName: String,
    diagnosticFindings: String,
    laborCost: Double,
    materialsCost: Double,
    estimatedTime: String,
    deductInspectionFee: Boolean,
    workMatchConfirmed: Boolean? = null,
    differenceReason: String? = null,
    adjustedInspectionFee: Double = 0.0
  ) {
    val existing = db.serviceDao().getServiceById(serviceId).first()
    val isZeroCondition = existing?.isZeroVisitCondition == true
    
    // Determine effective inspection fee
    val effectiveInspectionFee = when {
      isZeroCondition && workMatchConfirmed == true -> 0.0
      isZeroCondition && workMatchConfirmed == false -> adjustedInspectionFee
      else -> existing?.inspectionFee ?: 30.0
    }

    val rawTotal = laborCost + materialsCost
    val netTotal = if (deductInspectionFee && existing?.isInspectionActive == true && effectiveInspectionFee > 0) {
      (rawTotal - effectiveInspectionFee).coerceAtLeast(0.0)
    } else {
      rawTotal
    }

    if (existing != null) {
      val updated = existing.copy(
        diagnosticNotes = diagnosticFindings,
        laborCost = laborCost,
        materialsCost = materialsCost,
        totalCost = netTotal,
        estimatedTime = estimatedTime,
        inspectionFee = effectiveInspectionFee,
        isInspectionDeductedFromTotal = deductInspectionFee,
        workMatchConfirmed = workMatchConfirmed,
        workMatchDifferenceReason = differenceReason,
        state = ServiceState.PRESUPUESTADO
      )
      db.serviceDao().updateService(updated)
    }

    val matchReportText = when {
      isZeroCondition && workMatchConfirmed == true ->
        "\n✅ *Validación de Trabajo:* Se confirmó coincidencia 100% con lo reportado en la app ➔ *Visita S/ 0.00 aplicada.*"
      isZeroCondition && workMatchConfirmed == false ->
        "\n⚠️ *Validación de Trabajo:* El trabajo difiere de lo indicado en la app ($differenceReason) ➔ *Tarifa de revisión técnica: S/ ${String.format("%.2f", effectiveInspectionFee)}.*"
      else -> ""
    }

    val deductionText = if (deductInspectionFee && existing?.isInspectionActive == true && effectiveInspectionFee > 0) {
      "\n💡 *Se descontó la tarifa de visita (S/ ${String.format("%.2f", effectiveInspectionFee)}) del total.*"
    } else ""

    db.chatMessageDao().insertMessage(
      ChatMessageEntity(
        serviceId = serviceId,
        senderId = "TECH",
        senderName = technicianName,
        text = "📋 Informe de Diagnóstico en Sitio y Presupuesto Definitivo:\n• Hallazgo: $diagnosticFindings$matchReportText$deductionText",
        isDiagnosticReport = true,
        diagnosticFindings = diagnosticFindings,
        isQuotation = true,
        laborCost = laborCost,
        materialsCost = materialsCost,
        totalCost = netTotal,
        estimatedTime = estimatedTime,
        quotationStatus = QuotationStatus.PENDING,
        isZeroVisitCondition = isZeroCondition
      )
    )
  }

  suspend fun sendBudgetQuotation(
    serviceId: String,
    senderName: String,
    description: String,
    laborCost: Double,
    materialsCost: Double,
    estimatedTime: String
  ) {
    val total = laborCost + materialsCost
    // Update service record costs and state
    val existing = db.serviceDao().getServiceById(serviceId).first()
    if (existing != null) {
      val updated = existing.copy(
        title = description,
        laborCost = laborCost,
        materialsCost = materialsCost,
        totalCost = total,
        estimatedTime = estimatedTime,
        state = ServiceState.PRESUPUESTADO
      )
      db.serviceDao().updateService(updated)
    }

    // Insert rich quotation message into chat
    db.chatMessageDao().insertMessage(
      ChatMessageEntity(
        serviceId = serviceId,
        senderId = "TECH",
        senderName = senderName,
        text = "Presupuesto formal: $description",
        isQuotation = true,
        laborCost = laborCost,
        materialsCost = materialsCost,
        totalCost = total,
        estimatedTime = estimatedTime,
        quotationStatus = QuotationStatus.PENDING
      )
    )
  }

  suspend fun submitReview(
    serviceId: String,
    technicianId: String,
    clientName: String,
    punctuality: Float,
    quality: Float,
    cleanliness: Float,
    comment: String
  ) {
    val overall = (punctuality + quality + cleanliness) / 3f
    db.reviewDao().insertReview(
      ReviewEntity(
        serviceId = serviceId,
        technicianId = technicianId,
        clientName = clientName,
        overallRating = overall,
        punctualityRating = punctuality,
        qualityRating = quality,
        cleanlinessRating = cleanliness,
        comment = comment
      )
    )
  }

  suspend fun seedInitialDataIfEmpty() {
    val existing = db.technicianDao().getAllTechnicians().first()
    if (existing.isNotEmpty()) return

    val sampleTechs = listOf(
      TechnicianEntity(
        id = "tech_1",
        name = "Roberto García",
        specialty = "Electricista Senior",
        yearsExperience = 8,
        rating = 4.9f,
        reviewsCount = 124,
        bio = "Especialista certificado en tableros eléctricos, cortocircuitos, cableado estructurado e iluminación LED residencial y comercial.",
        baseHourlyRate = 45.0,
        cvTitle = "Técnico Electricista Industrial - SENATI (2016)",
        cvVerified = true,
        isVerified = true,
        paymentTypes = "YAPE,PLIN,EFECTIVO,TRANSFERENCIA",
        phone = "987 654 321",
        location = "San Isidro / San Borja",
        district = "San Isidro",
        latitude = -12.0969,
        longitude = -77.0345,
        isOnline = true,
        distanceKm = 1.2
      ),
      TechnicianEntity(
        id = "tech_2",
        name = "Carlos Mendoza",
        specialty = "Gasfitero e Instalaciones",
        yearsExperience = 10,
        rating = 4.8f,
        reviewsCount = 98,
        bio = "Detección de fugas no visibles con ultrasonido, instalación de termas a gas/eléctricas, cambio de tuberías de agua y desagüe.",
        baseHourlyRate = 40.0,
        cvTitle = "Certificación Gasfitería Avanzada - SENCICO (2014)",
        cvVerified = true,
        isVerified = true,
        paymentTypes = "YAPE,PLIN,EFECTIVO",
        phone = "912 345 678",
        location = "Miraflores / Surco",
        district = "Miraflores",
        latitude = -12.1215,
        longitude = -77.0298,
        isOnline = true,
        distanceKm = 2.8
      ),
      TechnicianEntity(
        id = "tech_3",
        name = "Miguel Ángel Ramos",
        specialty = "Carpintería y Melamina",
        yearsExperience = 6,
        rating = 4.95f,
        reviewsCount = 76,
        bio = "Diseño, fabricación y reparación de muebles de cocina en melamina, puertas de madera maciza, closets empotrados y laqueados.",
        baseHourlyRate = 50.0,
        cvTitle = "Técnico en Carpintería Fina - SENATI (2018)",
        cvVerified = true,
        isVerified = true,
        paymentTypes = "YAPE,TRANSFERENCIA,EFECTIVO",
        phone = "998 877 665",
        location = "San Borja / San Luis",
        district = "San Borja",
        latitude = -12.0872,
        longitude = -77.0011,
        isOnline = true,
        distanceKm = 3.6
      ),
      TechnicianEntity(
        id = "tech_4",
        name = "Lucía Flores",
        specialty = "Pintura y Acabados",
        yearsExperience = 5,
        rating = 4.85f,
        reviewsCount = 62,
        bio = "Pintura de interiores y fachadas, empaste, microcemento, tratamiento antihumedad y acabados de lujo para departamentos.",
        baseHourlyRate = 38.0,
        cvTitle = "Especialista en Revestimientos y Pintura (2019)",
        cvVerified = true,
        isVerified = true,
        paymentTypes = "YAPE,PLIN,EFECTIVO",
        phone = "955 443 322",
        location = "San Miguel / Magdalena",
        district = "San Miguel",
        latitude = -12.0770,
        longitude = -77.0850,
        isOnline = true,
        distanceKm = 6.4
      )
    )
    db.technicianDao().insertTechnicians(sampleTechs)

    val sampleServices = listOf(
      ServiceEntity(
        id = "serv_1",
        title = "Diagnóstico y Reparación Tablero Eléctrico",
        description = "El interruptor termomagnético principal salta cada vez que se enciende la terma eléctrica. Requiere revisión urgente de carga, cables y llaves diferenciales.",
        clientName = "Juan Antonio Ramos",
        technicianId = "tech_1",
        technicianName = "Roberto García",
        state = ServiceState.PRESUPUESTADO,
        laborCost = 80.0,
        materialsCost = 65.0,
        totalCost = 115.0, // (80 + 65) - 30 (visita descontada) = 115
        estimatedTime = "2.5 Horas",
        paymentType = PaymentType.YAPE,
        paymentStatus = PaymentStatus.PENDIENTE,
        inspectionFee = 30.0,
        isInspectionActive = true,
        inspectionDate = "Hoy, 4:00 PM",
        diagnosticNotes = "Inspección física realizada: Se detectó recalentamiento en cable THW 4mm² por falso contacto en bornera y llave de 20A defectuosa.",
        isInspectionDeductedFromTotal = true
      ),
      ServiceEntity(
        id = "serv_2",
        title = "Inspección de Fuga Oculta en Pared",
        description = "Humedad ascendente en pared de baño. Se requiere visita de diagnóstico para localizar la tubería picada.",
        clientName = "María Fernández",
        technicianId = "tech_2",
        technicianName = "Carlos Mendoza",
        state = ServiceState.VISITA_AGENDADA,
        laborCost = 0.0,
        materialsCost = 0.0,
        totalCost = 35.0,
        estimatedTime = "Inspección en sitio",
        paymentType = PaymentType.PLIN,
        paymentStatus = PaymentStatus.PENDIENTE,
        inspectionFee = 35.0,
        isInspectionActive = true,
        inspectionDate = "Mañana, 10:30 AM",
        diagnosticNotes = null
      ),
      ServiceEntity(
        id = "serv_3",
        title = "Instalación de Mueble y Repisas",
        description = "Instalación y anclaje seguro de estantes flotantes en pared.",
        clientName = "Carlos Ruiz",
        technicianId = "tech_3",
        technicianName = "Miguel Ángel Ramos",
        state = ServiceState.COMPLETADO,
        laborCost = 120.0,
        materialsCost = 0.0,
        totalCost = 120.0,
        estimatedTime = "3 Horas",
        paymentType = PaymentType.TRANSFERENCIA,
        paymentStatus = PaymentStatus.CONFIRMADO_POR_TECNICO
      )
    )

    for (s in sampleServices) {
      db.serviceDao().insertService(s)
    }

    val sampleMessages = listOf(
      ChatMessageEntity(
        serviceId = "serv_1",
        senderId = "CLIENT",
        senderName = "Juan Antonio",
        text = "Hola Roberto, el interruptor de mi casa salta cuando prendo la terma. Pensé que era algo simple de 20 soles, ¿puedes venir a verlo?"
      ),
      ChatMessageEntity(
        serviceId = "serv_1",
        senderId = "TECH",
        senderName = "Roberto García",
        text = "Hola Juan Antonio. Para darte un precio exacto y evitar sobrecostos o sorpresas con fallas ocultas, te propongo una Visita Técnica de Diagnóstico con tarifa fija de S/ 30.00. Tras la revisión física te daré el presupuesto final exacto.",
        isInspectionOffer = true,
        inspectionFee = 30.0,
        inspectionDate = "Hoy, 4:00 PM"
      ),
      ChatMessageEntity(
        serviceId = "serv_1",
        senderId = "CLIENT",
        senderName = "Juan Antonio",
        text = "✅ De acuerdo Roberto, acepto la visita de diagnóstico por S/ 30.00 para hoy a las 4:00 PM."
      ),
      ChatMessageEntity(
        serviceId = "serv_1",
        senderId = "TECH",
        senderName = "Roberto García",
        text = "📋 Informe de Diagnóstico en Sitio y Presupuesto Definitivo:\n• Hallazgo en sitio: Se detectó recalentamiento en cable THW 4mm² por falso contacto en bornera y llave de 20A defectuosa.\n💡 *Se descontó la tarifa de visita previa (S/ 30.00) del total.*",
        isDiagnosticReport = true,
        diagnosticFindings = "Recalentamiento en cable THW 4mm² por falso contacto en bornera y llave de 20A defectuosa.",
        isQuotation = true,
        laborCost = 80.0,
        materialsCost = 65.0,
        totalCost = 115.0,
        estimatedTime = "2.5 Horas",
        quotationStatus = QuotationStatus.PENDING
      )
    )

    for (m in sampleMessages) {
      db.chatMessageDao().insertMessage(m)
    }

    val sampleReviews = listOf(
      ReviewEntity(
        serviceId = "serv_prev_1",
        technicianId = "tech_1",
        clientName = "Valeria Gómez",
        overallRating = 5.0f,
        punctualityRating = 5.0f,
        qualityRating = 5.0f,
        cleanlinessRating = 5.0f,
        comment = "Excelente trabajo. Llegó puntual, cambió las llaves térmicas y dejó todo el cableado peinado y limpio."
      ),
      ReviewEntity(
        serviceId = "serv_prev_2",
        technicianId = "tech_1",
        clientName = "Daniel Paredes",
        overallRating = 4.8f,
        punctualityRating = 4.7f,
        qualityRating = 5.0f,
        cleanlinessRating = 4.8f,
        comment = "Muy profesional y transparente con los precios de los materiales. 100% recomendado."
      )
    )

    for (r in sampleReviews) {
      db.reviewDao().insertReview(r)
    }
  }
}
