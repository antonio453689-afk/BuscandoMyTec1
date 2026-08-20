package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ServiceEntity
import com.example.data.local.entities.TechnicianEntity
import com.example.data.model.PaymentStatus
import com.example.data.model.ServiceState
import com.example.data.model.UserRole
import com.example.ui.AppTab
import com.example.ui.TecniRedViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ServiceDetailScreen(
  viewModel: TecniRedViewModel,
  onNavigateToChat: () -> Unit
) {
  val activeService by viewModel.activeService.collectAsState()
  val allServices by viewModel.services.collectAsState()
  val activeRole by viewModel.activeRole.collectAsState()
  val technicians by viewModel.technicians.collectAsState()

  val currentService = activeService ?: allServices.firstOrNull()
  val currentTech = technicians.find { it.id == currentService?.technicianId } ?: technicians.firstOrNull()

  var showServiceSelector by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CanvasBackground)
  ) {
    // Header
    AppHeader(
      title = "Detalle del Servicio",
      onBackClick = { viewModel.selectTab(AppTab.Inicio) },
      actionContent = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Switch Service Dropdown
          Box {
            IconButton(onClick = { showServiceSelector = !showServiceSelector }) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opciones",
                tint = Slate700
              )
            }
            DropdownMenu(
              expanded = showServiceSelector,
              onDismissRequest = { showServiceSelector = false }
            ) {
              allServices.forEach { s ->
                DropdownMenuItem(
                  text = {
                    Column {
                      Text(s.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                      Text(
                        "${s.technicianName} • ${s.state.label}",
                        fontSize = 11.sp,
                        color = Slate500
                      )
                    }
                  },
                  onClick = {
                    viewModel.selectService(s.id)
                    showServiceSelector = false
                  }
                )
              }
            }
          }
        }
      }
    )

    if (currentService == null) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No hay servicios activos.", color = Slate500)
      }
      return
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Status Tracker Section
      StatusProgressBar(state = currentService.state)

      // 2. Technician Info Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(2.dp, RoundedCornerShape(24.dp))
          .clip(RoundedCornerShape(24.dp))
          .background(SurfaceWhite)
          .border(1.dp, Slate100, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            TechnicianAvatar(
              name = currentService.technicianName,
              isVerified = currentTech?.isVerified ?: true,
              size = 64
            )

            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Text(
                text = currentService.technicianName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Slate900
              )
              Text(
                text = "${currentTech?.specialty ?: "Especialista"} • ${currentTech?.yearsExperience ?: 8} años exp.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
              )
              Spacer(modifier = Modifier.height(2.dp))
              StarRatingBadge(
                rating = currentTech?.rating ?: 4.9f,
                reviewsCount = currentTech?.reviewsCount ?: 124
              )
            }
          }

          // Payment Badges
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            PaymentMethodPill("Yape")
            PaymentMethodPill("Plin")
            PaymentMethodPill("Efectivo")
            PaymentMethodPill("BCP / CCI")
          }
        }
      }

      // 2.5 Inspection / Diagnostic Feature Guarantee Banner & Zero Visit Condition Card
      if (currentService.isZeroVisitCondition) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = when (currentService.workMatchConfirmed) {
            true -> Color(0xFFF0FDF4)
            false -> Color(0xFFFEF2F2)
            null -> Color(0xFFEFF6FF)
          },
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (currentService.workMatchConfirmed) {
              true -> VerifiedGreen
              false -> Color(0xFFEF4444)
              null -> BluePrimary
            }
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = when (currentService.workMatchConfirmed) {
                    true -> Icons.Default.CheckCircle
                    false -> Icons.Default.Warning
                    null -> Icons.Default.Gavel
                  },
                  contentDescription = null,
                  tint = when (currentService.workMatchConfirmed) {
                    true -> VerifiedGreen
                    false -> Color(0xFFDC2626)
                    null -> BluePrimary
                  },
                  modifier = Modifier.size(20.dp)
                )
                Text(
                  text = when (currentService.workMatchConfirmed) {
                    true -> "VISITA S/ 0 CONSOLIDADA (100% CONFORME)"
                    false -> "TARIFA DE REVISIÓN APLICADA (DIFIERE)"
                    null -> "MODALIDAD: VISITA S/ 0 CONDICIONAL"
                  },
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = when (currentService.workMatchConfirmed) {
                    true -> VerifiedGreen
                    false -> Color(0xFFDC2626)
                    null -> BluePrimary
                  },
                  letterSpacing = 0.5.sp
                )
              }

              Surface(
                shape = RoundedCornerShape(50),
                color = when (currentService.workMatchConfirmed) {
                  true -> VerifiedGreen.copy(alpha = 0.15f)
                  false -> Color(0xFFEF4444).copy(alpha = 0.15f)
                  null -> StarAmber.copy(alpha = 0.2f)
                }
              ) {
                Text(
                  text = when (currentService.workMatchConfirmed) {
                    true -> "Visita S/ 0.00"
                    false -> "Tarifa S/ ${String.format("%.2f", currentService.inspectionFee)}"
                    null -> "Validación en Sitio"
                  },
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = when (currentService.workMatchConfirmed) {
                    true -> VerifiedGreen
                    false -> Color(0xFFDC2626)
                    null -> Color(0xFFB45309)
                  },
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            Text(
              text = when (currentService.workMatchConfirmed) {
                true -> "✅ El técnico inspeccionó la avería y confirmó que coincide 100% con lo detallado por el cliente en la app. La visita técnica es 100% gratuita."
                false -> "⚠️ El técnico constató que el trabajo real difiere de lo indicado en la solicitud (${currentService.workMatchDifferenceReason ?: "averías o complicaciones adicionales"}). Se activó la tarifa de diagnóstico técnico."
                null -> "⚖️ *Aviso de condiciones:* La visita técnica será S/ 0.00 si y solo si la avería física en sitio coincide tal cual lo indicado por el cliente en la app. Si la avería difiere o hay fallas ocultas no descritas, aplicará la tarifa de revisión."
              },
              fontSize = 12.sp,
              color = Slate800,
              lineHeight = 17.sp
            )

            if (currentService.diagnosticNotes != null) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = "🔍 Informe de Diagnóstico en Sitio:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                  )
                  Text(
                    text = currentService.diagnosticNotes,
                    fontSize = 12.sp,
                    color = Slate700
                  )
                }
              }
            }
          }
        }
      } else {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = BlueContainer.copy(alpha = 0.6f),
          border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
              Text(
                text = "GARANTÍA DE DIAGNÓSTICO EN SITIO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BluePrimary,
                letterSpacing = 0.5.sp
              )
            }

            Text(
              text = "Para evitar presupuestos inexactos ante fallas ocultas, el técnico puede realizar una revisión física previa con tarifa fija (S/ ${String.format("%.2f", currentService.inspectionFee)}) antes de cotizar el trabajo final.",
              fontSize = 12.sp,
              color = Slate800,
              lineHeight = 17.sp
            )

            if (currentService.diagnosticNotes != null) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = "🔍 Hallazgo en Visita de Diagnóstico:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                  )
                  Text(
                    text = currentService.diagnosticNotes,
                    fontSize = 12.sp,
                    color = Slate700
                  )
                }
              }
            }
          }
        }
      }

      // 3. Dark Slate Card (#1A1C1E) - Budget Breakdown & Direct Actions
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(8.dp, RoundedCornerShape(24.dp))
          .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface)
      ) {
        Column(
          modifier = Modifier.padding(22.dp),
          verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
          // Card Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .border(0.dp, Color.Transparent)
              .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (currentService.state == ServiceState.VISITA_PENDIENTE || currentService.state == ServiceState.VISITA_AGENDADA)
                  "TARIFA DE VISITA TÉCNICA" else "PRESUPUESTO ESTIMADO",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = currentService.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
              )
            }

            Text(
              text = if (currentService.state == ServiceState.VISITA_PENDIENTE || currentService.state == ServiceState.VISITA_AGENDADA)
                "S/ ${String.format("%.2f", currentService.inspectionFee)}"
              else if (currentService.totalCost > 0)
                "S/ ${String.format("%.2f", currentService.totalCost)}"
              else "S/ 0.00",
              style = MaterialTheme.typography.displayLarge,
              fontWeight = FontWeight.Light,
              color = Color.White,
              fontSize = 24.sp
            )
          }

          Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

          // Cost Breakdown List
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (currentService.state == ServiceState.VISITA_PENDIENTE || currentService.state == ServiceState.VISITA_AGENDADA) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Tarifa fija de visita / movilidad",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFFCBD5E1)
                )
                Text(
                  text = "S/ ${String.format("%.2f", currentService.inspectionFee)}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = Color.White
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Fecha acordada",
                  style = MaterialTheme.typography.bodySmall,
                  color = Slate400
                )
                Text(
                  text = currentService.inspectionDate ?: "Por coordinar",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = StarAmber
                )
              }
            } else {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Mano de obra",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFFCBD5E1)
                )
                Text(
                  text = "S/ ${String.format("%.2f", currentService.laborCost)}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = Color.White
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Materiales y repuestos",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFFCBD5E1)
                )
                Text(
                  text = "S/ ${String.format("%.2f", currentService.materialsCost)}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = Color.White
                )
              }

              if (currentService.isInspectionDeductedFromTotal && currentService.isInspectionActive) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "Abono Visita Diagnóstico",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VerifiedGreen
                  )
                  Text(
                    text = "- S/ ${String.format("%.2f", currentService.inspectionFee)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = VerifiedGreen
                  )
                }
              }

              Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Tiempo estimado",
                  style = MaterialTheme.typography.bodySmall,
                  color = Slate400,
                  fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Text(
                  text = currentService.estimatedTime.ifEmpty { "2.5 Horas" },
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = Color.White
                )
              }
            }
          }

          // Action Buttons according to current state
          when (currentService.state) {
            ServiceState.SOLICITADO -> {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = { viewModel.setInspectionDialogVisible(true) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                  shape = RoundedCornerShape(16.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = BluePrimary,
                    contentColor = Color.White
                  )
                ) {
                  Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Ofrecer Visita de Diagnóstico (Fija)",
                    fontWeight = FontWeight.SemiBold
                  )
                }

                OutlinedButton(
                  onClick = { viewModel.setQuotationDialogVisible(true) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                  shape = RoundedCornerShape(16.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                  Text("Cotizar Directo sin Visita", fontSize = 12.sp)
                }
              }
            }

            ServiceState.VISITA_PENDIENTE -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Button(
                  onClick = { viewModel.rejectQuotation(currentService.id) },
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                  shape = RoundedCornerShape(16.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White
                  )
                ) {
                  Text("Rechazar", fontWeight = FontWeight.Medium)
                }

                Button(
                  onClick = { viewModel.acceptInspectionVisit(currentService.id) },
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                  shape = RoundedCornerShape(16.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = BluePrimary,
                    contentColor = Color.White
                  )
                ) {
                  Text("Aceptar Visita", fontWeight = FontWeight.SemiBold)
                }
              }
            }

            ServiceState.VISITA_AGENDADA -> {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = { viewModel.setDiagnosticReportDialogVisible(true) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                  shape = RoundedCornerShape(16.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = BluePrimary,
                    contentColor = Color.White
                  )
                ) {
                  Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Registrar Diagnóstico & Costo Real",
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            ServiceState.PRESUPUESTADO, ServiceState.DIAGNOSTICO_REALIZADO -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Button(
                  onClick = { viewModel.rejectQuotation(currentService.id) },
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                  shape = RoundedCornerShape(16.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White
                  )
                ) {
                  Text(
                    text = "Rechazar",
                    fontWeight = FontWeight.Medium
                  )
                }

                Button(
                  onClick = { viewModel.acceptQuotation(currentService.id) },
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                  shape = RoundedCornerShape(16.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = BluePrimary,
                    contentColor = Color.White
                  )
                ) {
                  Text(
                    text = "Aceptar Trabajo",
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }

            ServiceState.EN_PROCESO -> {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                  onClick = { viewModel.setPaymentModalVisible(true) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                  shape = RoundedCornerShape(16.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = BluePrimary,
                    contentColor = Color.White
                  )
                ) {
                  Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Pagar Ahora (Yape / Plin / BCP)",
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }

            ServiceState.PAGO_PENDIENTE -> {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = Color.White.copy(alpha = 0.08f),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.ReceiptLong,
                      contentDescription = null,
                      tint = StarAmber,
                      modifier = Modifier.size(20.dp)
                    )
                    Text(
                      text = "Comprobante enviado. Esperando validación del técnico.",
                      color = Color.White,
                      fontSize = 12.sp
                    )
                  }
                }

                // If testing as technician or client
                Button(
                  onClick = { viewModel.confirmPaymentAsTechnician(currentService.id) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                  shape = RoundedCornerShape(16.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = VerifiedGreen,
                    contentColor = Color.White
                  )
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Confirmar Pago Recibido (Técnico)",
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }

            ServiceState.COMPLETADO -> {
              Button(
                onClick = { viewModel.setReviewModalVisible(true) },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = StarAmber,
                  contentColor = Slate900
                )
              ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Calificar Servicio (3 Criterios)",
                  fontWeight = FontWeight.Bold
                )
              }
            }

            ServiceState.SOLICITADO -> {
              Button(
                onClick = { viewModel.setQuotationDialogVisible(true) },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = BluePrimary,
                  contentColor = Color.White
                )
              ) {
                Icon(Icons.Default.EditNote, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Emitir Presupuesto Formal (Técnico)",
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            ServiceState.CANCELADO -> {
              Text(
                text = "Este servicio fue cancelado.",
                color = Slate400,
                fontSize = 13.sp
              )
            }
          }
        }
      }

      // 4. Problem Description Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(SurfaceWhite)
          .border(1.dp, Slate100, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = BluePrimary,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "Descripción del Requerimiento",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = Slate900
            )
          }
          Text(
            text = currentService.description,
            style = MaterialTheme.typography.bodyMedium,
            color = Slate600,
            lineHeight = 20.sp
          )
        }
      }

      // Quick Action to Chat
      OutlinedButton(
        onClick = onNavigateToChat,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = SurfaceWhite,
          contentColor = Slate900
        )
      ) {
        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Abrir Chat con el Especialista", fontWeight = FontWeight.SemiBold)
      }

      Spacer(modifier = Modifier.height(60.dp))
    }
  }
}
