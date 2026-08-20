package com.example.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.ServiceRequestEntity
import com.example.ui.TecniRedViewModel
import com.example.ui.theme.*

/**
 * Dialog to broadcast a service request with problem details.
 * Starts search at 5 km for 20 seconds, then auto-escalates to 10 km.
 */
@Composable
fun RequestServiceBroadcastDialog(
  viewModel: TecniRedViewModel,
  onDismiss: () -> Unit
) {
  val technicians by viewModel.technicians.collectAsState()
  val clientDistrictState by viewModel.clientDistrict.collectAsState()
  val clientAddressState by viewModel.clientAddress.collectAsState()
  val isGpsLocating by viewModel.isGpsLocating.collectAsState()
  val isGpsActive by viewModel.isGpsActive.collectAsState()

  var title by remember { mutableStateOf("Cortocircuito en sala y llaves saltadas") }
  var description by remember { mutableStateOf("El interruptor termomagnético principal salta inmediatamente al conectar cualquier electrodoméstico. Se percibe leve olor a recalentamiento en el tablero eléctrico.") }
  var selectedCategory by remember { mutableStateOf("Electricista") }
  var address by remember(clientAddressState) { mutableStateOf(clientAddressState) }
  var selectedDistrict by remember(clientDistrictState) { mutableStateOf(clientDistrictState) }
  var urgency by remember { mutableStateOf("⚡ Inmediato (< 1 hr)") }
  var inspectionFee by remember { mutableStateOf("30.00") }
  var isZeroVisitSelected by remember { mutableStateOf(true) }
  var clientAgreedTerms by remember { mutableStateOf(true) }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (fineGranted || coarseGranted) {
      viewModel.detectUserLocationWithPlayServices { success ->
        if (success) {
          viewModel.userLocationData.value?.let { loc ->
            if (loc.district.isNotBlank()) selectedDistrict = loc.district
            if (loc.address.isNotBlank()) address = loc.address
          }
        }
      }
    }
  }

  val categories = listOf("Electricista", "Gasfitero", "Carpintería", "Pintura", "Cerrajería")
  val districts = listOf("San Isidro", "Miraflores", "Surco", "San Borja", "San Miguel", "La Molina", "Barranco")
  val urgencies = listOf("⚡ Inmediato (< 1 hr)", "Hoy en la tarde", "Mañana")

  val techsInRange5km = remember(technicians) {
    technicians.filter { it.distanceKm <= 5.0 }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp)),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BluePrimary),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
            Column {
              Text(
                text = "Emitir Alerta de Servicio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
              )
              Text(
                text = "Radar 5 km (20s) ➔ Expansión a 10 km",
                fontSize = 11.sp,
                color = Slate500
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Slate500)
          }
        }

        // Radar Escalation Info Card
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          color = BlueContainer.copy(alpha = 0.6f),
          border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f))
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
            Column {
              Text(
                text = "Alerta Inmediata a Técnicos Cercanos",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
              )
              Text(
                text = "Se notificará a técnicos a 5 km por 20 seg. Si nadie acepta, el radar se ampliará automáticamente a 10 km.",
                fontSize = 10.sp,
                color = Slate600,
                lineHeight = 14.sp
              )
            }
          }
        }

        // Category Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(text = "Especialidad requerida:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            categories.forEach { cat ->
              val isSelected = selectedCategory == cat
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) BluePrimary else Slate100,
                modifier = Modifier.clickable { selectedCategory = cat }
              ) {
                Text(
                  text = cat,
                  color = if (isSelected) Color.White else Slate700,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }
          }
        }

        // Title
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Título de la solicitud", fontSize = 13.sp) },
          placeholder = { Text("Ej: Cortocircuito, Fuga de agua, Llave rota...", fontSize = 12.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        // Problem Description
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Detalle exhaustivo de la problemática", fontSize = 13.sp) },
          placeholder = { Text("Describa la avería con detalle para que el técnico prepare sus herramientas y repuestos antes de aceptar...", fontSize = 12.sp) },
          modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
          shape = RoundedCornerShape(12.dp),
          maxLines = 4
        )

        // Location & District
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = "Distrito y Dirección:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
            
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isGpsActive) VerifiedGreen.copy(alpha = 0.15f) else BlueContainer,
              modifier = Modifier.clickable {
                locationPermissionLauncher.launch(
                  arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                  )
                )
              }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                if (isGpsLocating) {
                  CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = BluePrimary)
                } else {
                  Icon(
                    imageVector = if (isGpsActive) Icons.Default.GpsFixed else Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = if (isGpsActive) VerifiedGreen else BluePrimary,
                    modifier = Modifier.size(14.dp)
                  )
                }
                Text(
                  text = if (isGpsLocating) "Obteniendo GPS..." else if (isGpsActive) "GPS Activo" else "Usar mi GPS",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isGpsActive) VerifiedGreen else BluePrimary
                )
              }
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            districts.forEach { dist ->
              val isSelected = selectedDistrict == dist
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) BluePrimary else Slate100,
                modifier = Modifier.clickable {
                  selectedDistrict = dist
                  viewModel.setDistrictManual(dist)
                }
              ) {
                Text(
                  text = dist,
                  color = if (isSelected) Color.White else Slate700,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }

          OutlinedTextField(
            value = address,
            onValueChange = {
              address = it
              viewModel.setClientAddress(it)
            },
            label = { Text("Dirección exacta", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = BluePrimary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          )
        }

        // Urgency
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(text = "Urgencia de atención:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            urgencies.forEach { urg ->
              val isSelected = urgency == urg
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) StarAmber else Slate100,
                modifier = Modifier.clickable { urgency = urg }
              ) {
                Text(
                  text = urg,
                  color = if (isSelected) Color.Black else Slate700,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }
        }

        // Proposed Inspection Fee & Zero Fee Option Mode
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = "Modalidad de Visita Técnica:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
          
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Option 1: Visita S/ 0 Condicional
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isZeroVisitSelected) Color(0xFFEFF6FF) else Slate100,
              border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isZeroVisitSelected) BluePrimary else Color.Transparent
              ),
              modifier = Modifier
                .weight(1f)
                .clickable { isZeroVisitSelected = true }
            ) {
              Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  RadioButton(
                    selected = isZeroVisitSelected,
                    onClick = { isZeroVisitSelected = true },
                    modifier = Modifier.size(18.dp)
                  )
                  Text("⚡ Visita S/ 0", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = BluePrimary)
                }
                Text("Válida si la avería coincide 100% con tu detalle.", fontSize = 10.sp, color = Slate600, lineHeight = 13.sp)
              }
            }

            // Option 2: Tarifa Fija
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (!isZeroVisitSelected) Color(0xFFF0FDF4) else Slate100,
              border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (!isZeroVisitSelected) VerifiedGreen else Color.Transparent
              ),
              modifier = Modifier
                .weight(1f)
                .clickable { isZeroVisitSelected = false }
            ) {
              Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  RadioButton(
                    selected = !isZeroVisitSelected,
                    onClick = { isZeroVisitSelected = false },
                    modifier = Modifier.size(18.dp)
                  )
                  Text("Tarifa Fija", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                }
                Text("Monto estándar pactado por diagnóstico en sitio.", fontSize = 10.sp, color = Slate600, lineHeight = 13.sp)
              }
            }
          }

          if (isZeroVisitSelected) {
            // Prominent notice & conditions card for client
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFFEF3C7),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                  Text(
                    text = "CONDICIONES DE LA VISITA S/ 0 (CLIENTE):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF92400E)
                  )
                }
                Text(
                  text = "• La visita y diagnóstico será 100% GRATIS (S/ 0.00) si y solo si la avería física en sitio coincide exactamente con lo indicado en esta solicitud.\n• ⚠️ Si el técnico detecta daños ocultos, tuberías no especificadas o problemas distintos a los descritos, se aplicará una tarifa estándar de diagnóstico de S/ 25.00.",
                  fontSize = 11.sp,
                  color = Color(0xFF78350F),
                  lineHeight = 15.sp
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { clientAgreedTerms = !clientAgreedTerms }
                ) {
                  Checkbox(
                    checked = clientAgreedTerms,
                    onCheckedChange = { clientAgreedTerms = it },
                    modifier = Modifier.size(24.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Acepto las condiciones de la Visita S/ 0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                  )
                }
              }
            }
          } else {
            OutlinedTextField(
              value = inspectionFee,
              onValueChange = { inspectionFee = it },
              label = { Text("Tarifa fija de visita de diagnóstico (S/)", fontSize = 13.sp) },
              leadingIcon = { Text("S/", fontWeight = FontWeight.Bold, color = Slate700, modifier = Modifier.padding(start = 12.dp)) },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp)
            )
          }
        }

        // Submit Button
        Button(
          onClick = {
            val fee = if (isZeroVisitSelected) 0.0 else (inspectionFee.toDoubleOrNull() ?: 30.0)
            viewModel.createBroadcastServiceRequest(
              title = title,
              description = description,
              category = selectedCategory,
              address = address,
              district = selectedDistrict,
              radiusKm = 5.0,
              urgency = urgency,
              proposedInspectionFee = fee,
              isZeroVisitCondition = isZeroVisitSelected
            )
          },
          enabled = !isZeroVisitSelected || clientAgreedTerms,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = if (isZeroVisitSelected) BluePrimary else BluePrimary)
        ) {
          Icon(Icons.Default.Sensors, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isZeroVisitSelected) "Emitir Alerta (Visita S/ 0 Condicional)" else "Emitir Alerta al Radar (${techsInRange5km.size} en 5 km)",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }
      }
    }
  }
}

/**
 * Radar Search Overlay: Shows live pulse radar, 20s countdown, and 5km -> 10km auto-escalation.
 */
@Composable
fun RadarSearchSheet(
  viewModel: TecniRedViewModel,
  onDismiss: () -> Unit
) {
  val activeRequest by viewModel.currentBroadcastRequest.collectAsState()
  val technicians by viewModel.technicians.collectAsState()
  val countdown by viewModel.radarCountdownSeconds.collectAsState()
  val currentRadius by viewModel.radarCurrentRadiusKm.collectAsState()
  val isEscalated by viewModel.isRadiusEscalated.collectAsState()

  val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
  val pulse1 by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulse1"
  )
  val pulse2 by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, delayMillis = 900, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulse2"
  )

  val activeTechsInRange = remember(technicians, currentRadius) {
    technicians.filter { it.distanceKm <= currentRadius }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp)),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Radar Pulse Visual
        Box(
          modifier = Modifier
            .size(150.dp)
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2

            val waveColor = if (isEscalated) Color(0xFFF59E0B) else BluePrimary

            // Static background ring
            drawCircle(
              color = waveColor.copy(alpha = 0.08f),
              radius = maxRadius,
              center = center
            )

            // Animated wave 1
            drawCircle(
              color = waveColor.copy(alpha = (1f - pulse1) * 0.45f),
              radius = maxRadius * pulse1,
              center = center,
              style = Stroke(width = 3.dp.toPx())
            )

            // Animated wave 2
            drawCircle(
              color = waveColor.copy(alpha = (1f - pulse2) * 0.45f),
              radius = maxRadius * pulse2,
              center = center,
              style = Stroke(width = 3.dp.toPx())
            )

            // Center beacon dot
            drawCircle(
              color = waveColor,
              radius = 12.dp.toPx(),
              center = center
            )
          }

          Icon(
            imageVector = Icons.Default.Sensors,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
        }

        // Escalation / Countdown Status Banner
        if (!isEscalated) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = BlueContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                Text(
                  text = "Radio inicial: 5 km (Prioritario)",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Slate900
                )
              }

              // Countdown display
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text(
                  text = "⏱️ Ampliando a 10 km en:",
                  fontSize = 12.sp,
                  color = Slate700
                )
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Slate900
                ) {
                  Text(
                    text = "${countdown}s",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = StarAmber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  )
                }
              }

              LinearProgressIndicator(
                progress = { (20 - countdown) / 20f },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp)),
                color = BluePrimary,
                trackColor = Slate200
              )
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFEF3C7),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFF59E0B)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
              }

              Column {
                Text(
                  text = "⚡ ¡Radio Ampliado a 10 km!",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color(0xFF92400E)
                )
                Text(
                  text = "Ampliamos cobertura automáticamente para encontrar más especialistas en tu zona.",
                  fontSize = 11.sp,
                  color = Color(0xFF78350F),
                  lineHeight = 15.sp
                )
              }
            }
          }
        }

        // Active Request Summary with Problem Detail
        activeRequest?.let { req ->
          Card(
            colors = CardDefaults.cardColors(containerColor = Slate50),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = req.title,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Slate900,
                  modifier = Modifier.weight(1f)
                )
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = BluePrimary.copy(alpha = 0.12f)
                ) {
                  Text(
                    text = "Radio: ${currentRadius.toInt()} km",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              // Problem Details
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "Detalle transmitido a técnicos:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                  )
                  Text(
                    text = req.description,
                    fontSize = 11.sp,
                    color = Slate800,
                    lineHeight = 15.sp
                  )
                }
              }

              if (req.isZeroVisitCondition) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFFFEF3C7),
                  border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                    Text(
                      text = "⚡ Visita S/ 0 (Sujeta a que el trabajo coincida 100% en sitio)",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF92400E)
                    )
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "📍 ${req.clientAddress} (${req.clientDistrict})",
                  fontSize = 11.sp,
                  color = Slate700
                )
                Text(
                  text = if (req.isZeroVisitCondition) "Visita: S/ 0.00 (Condicional)" else "Visita: S/ ${String.format("%.2f", req.proposedInspectionFee)}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (req.isZeroVisitCondition) BluePrimary else VerifiedGreen
                )
              }
            }
          }
        }

        // Live Technicians list in range with 1-click test simulation
        Card(
          colors = CardDefaults.cardColors(containerColor = BlueContainer.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "📡 Técnicos alcanzados por la señal (${activeTechsInRange.size}):",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = BluePrimary
            )
            activeTechsInRange.take(3).forEach { tech ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(SurfaceWhite, RoundedCornerShape(8.dp))
                  .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(tech.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                  Text("A ${String.format("%.1f", tech.distanceKm)} km • ${tech.district}", fontSize = 10.sp, color = Slate600)
                }
                Button(
                  onClick = {
                    activeRequest?.let { req ->
                      viewModel.setTechnicianPersona(tech)
                      viewModel.acceptBroadcastAsTechnician(req)
                    }
                  },
                  modifier = Modifier.height(32.dp),
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = VerifiedGreen),
                  contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                  Text("Aceptar (Como ${tech.name.split(" ").first()})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        // Cancel Button
        OutlinedButton(
          onClick = {
            activeRequest?.let { viewModel.cancelBroadcastRequest(it.id) }
            onDismiss()
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate600)
        ) {
          Text("Cancelar búsqueda")
        }
      }
    }
  }
}

/**
 * Incoming Request Alert Dialog: Dispatched to technicians with problem details,
 * GPS distance, and 5km / 10km zone indicator.
 */
@Composable
fun UberStyleIncomingRequestDialog(
  request: ServiceRequestEntity,
  onAccept: () -> Unit,
  onDecline: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "alert_pulse")
  val pulseBorder by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseBorder"
  )

  val isExpandedZone = request.maxRadiusKm > 5.0

  Dialog(onDismissRequest = onDecline) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(16.dp, RoundedCornerShape(26.dp))
        .border(
          width = 2.dp,
          color = (if (isExpandedZone) Color(0xFFF59E0B) else Color(0xFFEF4444)).copy(alpha = pulseBorder),
          shape = RoundedCornerShape(26.dp)
        ),
      colors = CardDefaults.cardColors(containerColor = Slate900),
      shape = RoundedCornerShape(26.dp)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Beacon Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = (if (isExpandedZone) Color(0xFFF59E0B) else Color(0xFFEF4444)).copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isExpandedZone) Color(0xFFF59E0B) else Color(0xFFEF4444)
            )
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(if (isExpandedZone) Color(0xFFF59E0B) else Color(0xFFEF4444))
              )
              Text(
                text = if (isExpandedZone) "RADAR AMPLIADO (10 KM)" else "NUEVA SOLICITUD EN TU ZONA (5 KM)",
                color = if (isExpandedZone) Color(0xFFFDE68A) else Color(0xFFFCA5A5),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.1f)
          ) {
            Text(
              text = request.urgency,
              color = StarAmber,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        // Distance & Location with GPS
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color.White.copy(alpha = 0.06f),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BluePrimary),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Column {
              Text(
                text = "Aprox. ${String.format("%.1f", request.maxRadiusKm / 2.5)} km • ${request.clientDistrict}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = request.clientAddress,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
              )
            }
          }
        }

        // Title and Problem Detail Card
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = request.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )

          // Problem Detail Box (High-Contrast for Technical Diagnosis)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = StarAmber, modifier = Modifier.size(14.dp))
                Text(
                  text = "DETALLE DE LA PROBLEMÁTICA / AVERÍA:",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = StarAmber
                )
              }
              Text(
                text = request.description,
                fontSize = 12.sp,
                color = Color(0xFFE2E8F0),
                lineHeight = 17.sp
              )
            }
          }
        }

        // Guaranteed Inspection Fee Card / Zero Visit Condition Badge
        if (request.isZeroVisitCondition) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E3A8A).copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.Bolt, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                  Text(
                    text = "VISITA S/ 0 (CONDICIONAL 100%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF93C5FD)
                  )
                }
                Text(
                  text = "S/ 0.00",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color(0xFF93C5FD)
                )
              }

              // Technician Protection Terms
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Text(
                    text = "🛡️ GARANTÍA DE PROTECCIÓN AL TÉCNICO:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = StarAmber
                  )
                  Text(
                    text = "• Al llegar, verificarás si la avería física es tal cual lo descrito.\n• ✅ Si coincide 100% ➔ Visita en S/ 0 y presupuestas mano de obra.\n• ⚠️ Si difiere o hay fallas ocultas ➔ Tienes derecho a cobrar tarifa de revisión técnica (S/ 25 - S/ 30) desde tu panel.",
                    fontSize = 10.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 14.sp
                  )
                }
              }
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = VerifiedGreen.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, VerifiedGreen.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "TARIFA FIJA DE VISITA",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF86EFAC)
                )
                Text(
                  text = "Pago seguro garantizado",
                  fontSize = 11.sp,
                  color = Color(0xFFCBD5E1)
                )
              }
              Text(
                text = "S/ ${String.format("%.2f", request.proposedInspectionFee)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = VerifiedGreen
              )
            }
          }
        }

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDecline,
            modifier = Modifier
              .weight(1f)
              .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
          ) {
            Text("Rechazar", fontWeight = FontWeight.Medium)
          }

          Button(
            onClick = onAccept,
            modifier = Modifier
              .weight(1.5f)
              .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = VerifiedGreen,
              contentColor = Color.Black
            )
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ACEPTAR SERVICIO", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
          }
        }
      }
    }
  }
}
