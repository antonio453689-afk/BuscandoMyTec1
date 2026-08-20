package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TecniRedDatabase
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ReviewEntity
import com.example.data.local.entities.ServiceEntity
import com.example.data.local.entities.ServiceRequestEntity
import com.example.data.local.entities.TechnicianEntity
import com.example.data.location.LocationService
import com.example.data.location.UserLocationData
import com.example.data.model.PaymentType
import com.example.data.model.ServiceState
import com.example.data.model.UserRole
import com.example.data.repository.TecniRedRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed class AppTab(val title: String) {
  object Inicio : AppTab("Inicio")
  object Servicios : AppTab("Detalle")
  object Chats : AppTab("Chats")
  object Perfil : AppTab("Finanzas")
}

data class UiNotification(
  val message: String,
  val isSuccess: Boolean = true,
  val id: Long = System.currentTimeMillis()
)

class TecniRedViewModel(application: Application) : AndroidViewModel(application) {
  private val repository: TecniRedRepository
  val locationService: LocationService = LocationService(application)

  init {
    val db = TecniRedDatabase.getDatabase(application)
    repository = TecniRedRepository(db)
    viewModelScope.launch {
      repository.seedInitialDataIfEmpty()
      // Automatically calculate initial distances based on default / current client coords
      repository.updateTechnicianDistances(_clientLatitude.value, _clientLongitude.value)
    }
  }

  // Live Location & GPS state via Play Services Location
  private val _isGpsLocating = MutableStateFlow(false)
  val isGpsLocating: StateFlow<Boolean> = _isGpsLocating.asStateFlow()

  private val _isGpsActive = MutableStateFlow(false)
  val isGpsActive: StateFlow<Boolean> = _isGpsActive.asStateFlow()

  private val _clientLatitude = MutableStateFlow(-12.0969)
  val clientLatitude: StateFlow<Double> = _clientLatitude.asStateFlow()

  private val _clientLongitude = MutableStateFlow(-77.0345)
  val clientLongitude: StateFlow<Double> = _clientLongitude.asStateFlow()

  private val _userLocationData = MutableStateFlow<UserLocationData?>(null)
  val userLocationData: StateFlow<UserLocationData?> = _userLocationData.asStateFlow()

  /**
   * Fetches real client coordinates using Google Play Services Location (FusedLocationProviderClient)
   * and dynamically updates distances for all technicians in the Room database.
   */
  fun detectUserLocationWithPlayServices(onComplete: ((Boolean) -> Unit)? = null) {
    viewModelScope.launch {
      _isGpsLocating.value = true
      try {
        val locationData = locationService.getCurrentLocation()
        if (locationData != null) {
          _userLocationData.value = locationData
          _clientLatitude.value = locationData.latitude
          _clientLongitude.value = locationData.longitude
          _isGpsActive.value = true

          if (locationData.district.isNotBlank() && locationData.district != "Lima") {
            _clientDistrict.value = locationData.district
          }
          if (locationData.address.isNotBlank()) {
            _clientAddress.value = locationData.address
          }

          // Recalculate distance between client and all database technicians using play services / android location
          repository.updateTechnicianDistances(locationData.latitude, locationData.longitude)
          showNotification("📍 GPS Activo: Ubicación actualizada con Play Services. Distancias calculadas.")
          onComplete?.invoke(true)
        } else {
          // Fallback to default district coordinates
          repository.updateTechnicianDistances(_clientLatitude.value, _clientLongitude.value)
          onComplete?.invoke(false)
        }
      } catch (e: Exception) {
        onComplete?.invoke(false)
      } finally {
        _isGpsLocating.value = false
      }
    }
  }

  fun setDistrictManual(district: String) {
    _clientDistrict.value = district
    val (lat, lon) = when (district) {
      "San Isidro" -> Pair(-12.0969, -77.0345)
      "Miraflores" -> Pair(-12.1215, -77.0298)
      "Surco" -> Pair(-12.1380, -76.9940)
      "San Borja" -> Pair(-12.0872, -77.0011)
      "San Miguel" -> Pair(-12.0770, -77.0850)
      "La Molina" -> Pair(-12.0833, -76.9333)
      "Barranco" -> Pair(-12.1488, -77.0210)
      "Jesús María" -> Pair(-12.0744, -77.0494)
      "Magdalena" -> Pair(-12.0917, -77.0694)
      else -> Pair(-12.0969, -77.0345)
    }
    _clientLatitude.value = lat
    _clientLongitude.value = lon
    viewModelScope.launch {
      repository.updateTechnicianDistances(lat, lon)
    }
  }

  // Active user role switcher (to test full two-sided marketplace flow!)
  private val _activeRole = MutableStateFlow(UserRole.CLIENTE)
  val activeRole: StateFlow<UserRole> = _activeRole.asStateFlow()

  fun setRole(role: UserRole) {
    _activeRole.value = role
  }

  // Navigation tab
  private val _currentTab = MutableStateFlow<AppTab>(AppTab.Servicios)
  val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

  fun selectTab(tab: AppTab) {
    _currentTab.value = tab
  }

  // Selected Service
  private val _selectedServiceId = MutableStateFlow("serv_1")
  val selectedServiceId: StateFlow<String> = _selectedServiceId.asStateFlow()

  fun selectService(id: String) {
    _selectedServiceId.value = id
  }

  // Selected Technician (for modal / details)
  private val _selectedTechId = MutableStateFlow<String?>("tech_1")
  val selectedTechId: StateFlow<String?> = _selectedTechId.asStateFlow()

  fun selectTechnician(id: String?) {
    _selectedTechId.value = id
  }

  // Filter for Directory
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedSpecialty = MutableStateFlow("Todos")
  val selectedSpecialty: StateFlow<String> = _selectedSpecialty.asStateFlow()

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setSpecialty(specialty: String) {
    _selectedSpecialty.value = specialty
  }

  // Notification / Toast
  private val _notification = MutableStateFlow<UiNotification?>(null)
  val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

  fun showNotification(msg: String, isSuccess: Boolean = true) {
    _notification.value = UiNotification(msg, isSuccess)
  }

  fun clearNotification() {
    _notification.value = null
  }

  // Modals state
  private val _showPaymentModal = MutableStateFlow(false)
  val showPaymentModal: StateFlow<Boolean> = _showPaymentModal.asStateFlow()

  fun setPaymentModalVisible(visible: Boolean) {
    _showPaymentModal.value = visible
  }

  private val _showReviewModal = MutableStateFlow(false)
  val showReviewModal: StateFlow<Boolean> = _showReviewModal.asStateFlow()

  fun setReviewModalVisible(visible: Boolean) {
    _showReviewModal.value = visible
  }

  private val _showQuotationDialog = MutableStateFlow(false)
  val showQuotationDialog: StateFlow<Boolean> = _showQuotationDialog.asStateFlow()

  fun setQuotationDialogVisible(visible: Boolean) {
    _showQuotationDialog.value = visible
  }

  // Inspection and Diagnostic Modals
  private val _showInspectionDialog = MutableStateFlow(false)
  val showInspectionDialog: StateFlow<Boolean> = _showInspectionDialog.asStateFlow()

  fun setInspectionDialogVisible(visible: Boolean) {
    _showInspectionDialog.value = visible
  }

  private val _showDiagnosticReportDialog = MutableStateFlow(false)
  val showDiagnosticReportDialog: StateFlow<Boolean> = _showDiagnosticReportDialog.asStateFlow()

  fun setDiagnosticReportDialogVisible(visible: Boolean) {
    _showDiagnosticReportDialog.value = visible
  }

  // --- UBER / INDRIVE PROXIMITY REQUESTS & RADAR STATE ---
  private val _showRequestBroadcastDialog = MutableStateFlow(false)
  val showRequestBroadcastDialog: StateFlow<Boolean> = _showRequestBroadcastDialog.asStateFlow()

  fun setRequestBroadcastDialogVisible(visible: Boolean) {
    _showRequestBroadcastDialog.value = visible
  }

  private val _showRadarSearchSheet = MutableStateFlow(false)
  val showRadarSearchSheet: StateFlow<Boolean> = _showRadarSearchSheet.asStateFlow()

  fun setRadarSearchSheetVisible(visible: Boolean) {
    _showRadarSearchSheet.value = visible
  }

  private val _clientDistrict = MutableStateFlow("San Isidro")
  val clientDistrict: StateFlow<String> = _clientDistrict.asStateFlow()

  fun setClientDistrict(district: String) {
    _clientDistrict.value = district
  }

  private val _clientAddress = MutableStateFlow("Av. Javier Prado Este 1420, Dpto 502")
  val clientAddress: StateFlow<String> = _clientAddress.asStateFlow()

  fun setClientAddress(address: String) {
    _clientAddress.value = address
  }

  private val _clientRadiusKm = MutableStateFlow(5.0)
  val clientRadiusKm: StateFlow<Double> = _clientRadiusKm.asStateFlow()

  fun setClientRadius(radius: Double) {
    _clientRadiusKm.value = radius
  }

  // Active broadcast request by client
  private val _currentBroadcastRequestId = MutableStateFlow<String?>(null)
  val currentBroadcastRequestId: StateFlow<String?> = _currentBroadcastRequestId.asStateFlow()

  // Radar Auto-Escalation State (5km for 20s -> 10km)
  private val _radarCountdownSeconds = MutableStateFlow(20)
  val radarCountdownSeconds: StateFlow<Int> = _radarCountdownSeconds.asStateFlow()

  private val _radarCurrentRadiusKm = MutableStateFlow(5.0)
  val radarCurrentRadiusKm: StateFlow<Double> = _radarCurrentRadiusKm.asStateFlow()

  private val _isRadiusEscalated = MutableStateFlow(false)
  val isRadiusEscalated: StateFlow<Boolean> = _isRadiusEscalated.asStateFlow()

  private var radarEscalationJob: kotlinx.coroutines.Job? = null

  // Incoming alert for technician (Uber/InDrive style)
  private val _incomingUberAlert = MutableStateFlow<ServiceRequestEntity?>(null)
  val incomingUberAlert: StateFlow<ServiceRequestEntity?> = _incomingUberAlert.asStateFlow()

  fun dismissIncomingUberAlert() {
    _incomingUberAlert.value = null
  }

  fun triggerTechnicianAlertPreview(request: ServiceRequestEntity) {
    _incomingUberAlert.value = request
  }

  // Selected technician persona when in TECNICO mode
  private val _selectedTechPersona = MutableStateFlow<TechnicianEntity?>(null)
  val selectedTechPersona: StateFlow<TechnicianEntity?> = _selectedTechPersona.asStateFlow()

  fun setTechnicianPersona(tech: TechnicianEntity) {
    _selectedTechPersona.value = tech
  }

  // Broadcast requests pool
  val activeServiceRequests: StateFlow<List<ServiceRequestEntity>> = repository.activeServiceRequests.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  @OptIn(ExperimentalCoroutinesApi::class)
  val currentBroadcastRequest: StateFlow<ServiceRequestEntity?> = _currentBroadcastRequestId.flatMapLatest { id ->
    if (id != null) repository.getRequestById(id) else flowOf(null)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = null
  )

  // Data Flows
  val technicians: StateFlow<List<TechnicianEntity>> = repository.technicians.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val services: StateFlow<List<ServiceEntity>> = repository.services.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  @OptIn(ExperimentalCoroutinesApi::class)
  val activeService: StateFlow<ServiceEntity?> = _selectedServiceId.flatMapLatest { id ->
    repository.getServiceById(id)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = null
  )

  @OptIn(ExperimentalCoroutinesApi::class)
  val chatMessages: StateFlow<List<ChatMessageEntity>> = _selectedServiceId.flatMapLatest { id ->
    repository.getMessages(id)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  @OptIn(ExperimentalCoroutinesApi::class)
  val technicianReviews: StateFlow<List<ReviewEntity>> = _selectedTechId.flatMapLatest { techId ->
    if (techId != null) repository.getReviews(techId) else flowOf(emptyList())
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  // Actions
  fun acceptQuotation(serviceId: String, messageId: Long? = null) {
    viewModelScope.launch {
      repository.acceptQuotation(serviceId, messageId)
      showNotification("¡Presupuesto aceptado! El trabajo está En Proceso.")
    }
  }

  fun rejectQuotation(serviceId: String, messageId: Long? = null) {
    viewModelScope.launch {
      repository.rejectQuotation(serviceId, messageId)
      showNotification("Presupuesto rechazado.", isSuccess = false)
    }
  }

  fun submitPaymentVoucher(serviceId: String, paymentType: PaymentType, voucherNote: String) {
    viewModelScope.launch {
      repository.selectPaymentAndSendVoucher(serviceId, paymentType, voucherNote)
      _showPaymentModal.value = false
      showNotification("Comprobante enviado. Esperando confirmación del técnico.")
    }
  }

  fun confirmPaymentAsTechnician(serviceId: String) {
    viewModelScope.launch {
      repository.confirmPaymentByTechnician(serviceId)
      showNotification("¡Pago confirmado con éxito! Servicio completado.")
    }
  }

  fun sendTextMessage(serviceId: String, text: String) {
    if (text.isBlank()) return
    viewModelScope.launch {
      val isClient = _activeRole.value == UserRole.CLIENTE
      val senderId = if (isClient) "CLIENT" else "TECH"
      val senderName = if (isClient) "Cliente" else "Técnico Especialista"
      repository.sendChatMessage(serviceId, senderId, senderName, text.trim())
    }
  }

  fun proposeInspectionVisit(
    serviceId: String,
    fee: Double,
    scheduledDate: String,
    notes: String
  ) {
    viewModelScope.launch {
      repository.proposeInspectionVisit(
        serviceId = serviceId,
        technicianName = "Roberto García",
        fee = fee,
        scheduledDate = scheduledDate,
        notes = notes
      )
      _showInspectionDialog.value = false
      showNotification("Visita de diagnóstico propuesta por S/ ${String.format("%.2f", fee)}.")
    }
  }

  fun acceptInspectionVisit(serviceId: String) {
    viewModelScope.launch {
      repository.acceptInspectionVisit(serviceId)
      showNotification("¡Visita técnica de diagnóstico aceptada!")
    }
  }

  fun completeSiteDiagnosticAndSubmitFinalBudget(
    serviceId: String,
    diagnosticFindings: String,
    laborCost: Double,
    materialsCost: Double,
    estimatedTime: String,
    deductInspectionFee: Boolean,
    workMatchConfirmed: Boolean? = null,
    differenceReason: String? = null,
    adjustedInspectionFee: Double = 0.0
  ) {
    viewModelScope.launch {
      repository.completeSiteDiagnosticAndSubmitFinalBudget(
        serviceId = serviceId,
        technicianName = "Roberto García",
        diagnosticFindings = diagnosticFindings,
        laborCost = laborCost,
        materialsCost = materialsCost,
        estimatedTime = estimatedTime,
        deductInspectionFee = deductInspectionFee,
        workMatchConfirmed = workMatchConfirmed,
        differenceReason = differenceReason,
        adjustedInspectionFee = adjustedInspectionFee
      )
      _showDiagnosticReportDialog.value = false
      val message = if (workMatchConfirmed == true) {
        "Diagnóstico registrado: Trabajo 100% conforme, Visita S/ 0 aplicada."
      } else if (workMatchConfirmed == false) {
        "Diagnóstico registrado: Trabajo difiere, tarifa de revisión aplicada."
      } else {
        "Diagnóstico registrado y presupuesto definitivo emitido sin sorpresas."
      }
      showNotification(message)
    }
  }

  fun createQuotation(
    serviceId: String,
    description: String,
    laborCost: Double,
    materialsCost: Double,
    estimatedTime: String
  ) {
    viewModelScope.launch {
      repository.sendBudgetQuotation(
        serviceId = serviceId,
        senderName = "Roberto García",
        description = description,
        laborCost = laborCost,
        materialsCost = materialsCost,
        estimatedTime = estimatedTime
      )
      _showQuotationDialog.value = false
      showNotification("Presupuesto formal emitido al cliente.")
    }
  }

  fun submitReview(
    serviceId: String,
    technicianId: String,
    punctuality: Float,
    quality: Float,
    cleanliness: Float,
    comment: String
  ) {
    viewModelScope.launch {
      repository.submitReview(
        serviceId = serviceId,
        technicianId = technicianId,
        clientName = "Juan Antonio Ramos",
        punctuality = punctuality,
        quality = quality,
        cleanliness = cleanliness,
        comment = comment
      )
      _showReviewModal.value = false
      showNotification("¡Gracias por calificar el servicio!")
    }
  }

  fun requestNewService(tech: TechnicianEntity, title: String, description: String) {
    viewModelScope.launch {
      val newId = "serv_" + UUID.randomUUID().toString().take(6)
      val newService = ServiceEntity(
        id = newId,
        title = title,
        description = description,
        clientName = "Juan Antonio Ramos",
        technicianId = tech.id,
        technicianName = tech.name,
        state = ServiceState.SOLICITADO,
        laborCost = 0.0,
        materialsCost = 0.0,
        totalCost = 0.0,
        estimatedTime = "Por estimar",
        paymentType = null,
        paymentStatus = com.example.data.model.PaymentStatus.PENDIENTE
      )
      repository.createService(newService)
      repository.sendChatMessage(
        newId,
        "CLIENT",
        "Juan Antonio",
        "Hola ${tech.name}, me gustaría solicitar una cotización para: $title. Descripción: $description"
      )
      _selectedServiceId.value = newId
      _currentTab.value = AppTab.Servicios
      showNotification("Solicitud de servicio enviada a ${tech.name}.")
    }
  }

  /**
   * Broadcasts a service request with problem details.
   * Starts with a 5 km radius for 20 seconds. If not accepted, auto-escalates to 10 km.
   */
  fun createBroadcastServiceRequest(
    title: String,
    description: String,
    category: String,
    address: String,
    district: String,
    radiusKm: Double = 5.0,
    urgency: String,
    proposedInspectionFee: Double,
    isZeroVisitCondition: Boolean = false
  ) {
    viewModelScope.launch {
      val requestId = "req_" + UUID.randomUUID().toString().take(6)
      
      val (lat, lon) = when (district) {
        "San Isidro" -> Pair(-12.0969, -77.0345)
        "Miraflores" -> Pair(-12.1215, -77.0298)
        "Surco" -> Pair(-12.1380, -76.9940)
        "San Borja" -> Pair(-12.0872, -77.0011)
        "San Miguel" -> Pair(-12.0770, -77.0850)
        "La Molina" -> Pair(-12.0833, -76.9333)
        "Barranco" -> Pair(-12.1488, -77.0210)
        "Jesús María" -> Pair(-12.0744, -77.0494)
        "Magdalena" -> Pair(-12.0917, -77.0694)
        else -> Pair(_clientLatitude.value, _clientLongitude.value)
      }

      val initialRadius = 5.0
      val newRequest = com.example.data.local.entities.ServiceRequestEntity(
        id = requestId,
        clientName = "Juan Antonio Ramos",
        title = title,
        description = description,
        category = category,
        clientAddress = address,
        clientDistrict = district,
        clientLatitude = lat,
        clientLongitude = lon,
        maxRadiusKm = initialRadius,
        urgency = urgency,
        proposedInspectionFee = if (isZeroVisitCondition) 0.0 else proposedInspectionFee,
        isZeroVisitCondition = isZeroVisitCondition,
        status = "SEARCHING"
      )

      repository.broadcastServiceRequest(newRequest)
      _currentBroadcastRequestId.value = requestId
      _showRequestBroadcastDialog.value = false
      _showRadarSearchSheet.value = true

      // Initialize radar countdown and radius
      _radarCountdownSeconds.value = 20
      _radarCurrentRadiusKm.value = initialRadius
      _isRadiusEscalated.value = false

      // Trigger high-priority alert for technicians in 5km zone
      _incomingUberAlert.value = newRequest

      showNotification("🚨 Alerta emitida: Notificando a técnicos a 5 km por 20 seg...")

      // Launch auto-escalation timer: 20 seconds at 5 km -> escalates to 10 km
      radarEscalationJob?.cancel()
      radarEscalationJob = viewModelScope.launch {
        for (sec in 20 downTo 1) {
          _radarCountdownSeconds.value = sec
          kotlinx.coroutines.delay(1000)
        }
        _radarCountdownSeconds.value = 0

        // Check if still searching
        val activeReq = repository.getRequestById(requestId).first()
        if (activeReq != null && activeReq.status == "SEARCHING") {
          _isRadiusEscalated.value = true
          _radarCurrentRadiusKm.value = 10.0
          repository.updateRequestRadius(requestId, 10.0)
          val expandedReq = activeReq.copy(maxRadiusKm = 10.0)
          _incomingUberAlert.value = expandedReq
          showNotification("📡 Sin respuesta en 5 km: ¡Radio ampliado automáticamente a 10 km!", isSuccess = true)
        }
      }
    }
  }

  fun cancelBroadcastRequest(requestId: String) {
    radarEscalationJob?.cancel()
    viewModelScope.launch {
      repository.cancelServiceRequest(requestId)
      _showRadarSearchSheet.value = false
      _incomingUberAlert.value = null
      _isRadiusEscalated.value = false
      showNotification("Búsqueda por radar cancelada.", isSuccess = false)
    }
  }

  /**
   * Called when a technician clicks "ACEPTAR SERVICIO" from the alert banner.
   */
  fun acceptBroadcastAsTechnician(request: com.example.data.local.entities.ServiceRequestEntity) {
    radarEscalationJob?.cancel()
    viewModelScope.launch {
      val allTechs = repository.technicians.first()
      val activeTech = _selectedTechPersona.value ?: allTechs.firstOrNull { it.specialty.contains(request.category, ignoreCase = true) } ?: allTechs.first()
      
      val createdService = repository.acceptBroadcastRequest(request, activeTech)
      _incomingUberAlert.value = null
      _showRadarSearchSheet.value = false
      _isRadiusEscalated.value = false
      _selectedServiceId.value = createdService.id
      _currentTab.value = AppTab.Servicios
      showNotification("🎉 ¡Servicio aceptado por ${activeTech.name}! Conectando con el cliente...")
    }
  }

  fun calculateDistance(techLat: Double, techLon: Double, clientLat: Double, clientLon: Double): Double {
    return repository.calculateDistanceKm(techLat, techLon, clientLat, clientLon)
  }
}
