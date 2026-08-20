package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.TechnicianEntity
import com.example.data.model.PaymentType
import com.example.ui.TecniRedViewModel
import com.example.ui.theme.*

@Composable
fun PaymentModal(
  viewModel: TecniRedViewModel,
  serviceId: String,
  totalAmount: Double,
  onDismiss: () -> Unit
) {
  var selectedPaymentType by remember { mutableStateOf(PaymentType.YAPE) }
  var voucherCode by remember { mutableStateOf("OPER-849204") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp)),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
      Column(
        modifier = Modifier
          .padding(22.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "PAGAR SERVICIO",
              style = MaterialTheme.typography.labelSmall,
              color = Slate400,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = "S/ ${String.format("%.2f", totalAmount)}",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = BluePrimary
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Slate500)
          }
        }

        Divider(color = Slate100)

        // Payment Method Selectors
        Text(
          text = "Selecciona el método de pago:",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
          color = Slate800
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(PaymentType.YAPE, PaymentType.PLIN, PaymentType.TRANSFERENCIA, PaymentType.EFECTIVO).forEach { type ->
            val isSelected = selectedPaymentType == type
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) BluePrimary else Slate50)
                .border(1.dp, if (isSelected) BluePrimary else Slate200, RoundedCornerShape(12.dp))
                .clickable { selectedPaymentType = type }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = type.displayName,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Slate700
              )
            }
          }
        }

        // Details based on payment type
        when (selectedPaymentType) {
          PaymentType.YAPE, PaymentType.PLIN -> {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = Slate50,
              border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                // QR Mock Display
                Box(
                  modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(2.dp, if (selectedPaymentType == PaymentType.YAPE) YapePurple else PlinCyan, RoundedCornerShape(12.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "Código QR",
                    tint = if (selectedPaymentType == PaymentType.YAPE) YapePurple else PlinCyan,
                    modifier = Modifier.size(100.dp)
                  )
                }
                Text(
                  text = "Número: 987 654 321",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = Slate900
                )
                Text(
                  text = "Titular: Roberto García (Técnico)",
                  fontSize = 12.sp,
                  color = Slate500
                )
              }
            }
          }
          PaymentType.TRANSFERENCIA -> {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = Slate50,
              border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Banco de Crédito (BCP)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                Text("Cuenta Soles: 193-98765432-0-12", fontSize = 12.sp, color = Slate700)
                Text("CCI: 002-1930098765432012-14", fontSize = 11.sp, color = Slate500)
                Text("Titular: Roberto García", fontSize = 11.sp, color = Slate500)
              }
            }
          }
          PaymentType.EFECTIVO -> {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = GreenContainer,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "💵 Pagarás en efectivo directamente al técnico al finalizar la inspección y trabajo.",
                modifier = Modifier.padding(14.dp),
                fontSize = 12.sp,
                color = GreenText
              )
            }
          }
          else -> {}
        }

        // Voucher Simulation Input
        if (selectedPaymentType != PaymentType.EFECTIVO) {
          OutlinedTextField(
            value = voucherCode,
            onValueChange = { voucherCode = it },
            label = { Text("Número de Operación / Voucher") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = Slate400) }
          )
        }

        Button(
          onClick = {
            viewModel.submitPaymentVoucher(serviceId, selectedPaymentType, voucherCode)
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
        ) {
          Text("Enviar Comprobante al Técnico", fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
fun ReviewModal(
  viewModel: TecniRedViewModel,
  serviceId: String,
  technicianId: String,
  onDismiss: () -> Unit
) {
  var punctuality by remember { mutableStateOf(5f) }
  var quality by remember { mutableStateOf(5f) }
  var cleanliness by remember { mutableStateOf(5f) }
  var comment by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp)),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
      Column(
        modifier = Modifier
          .padding(22.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "Calificar al Especialista",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Slate900
        )
        Text(
          text = "Tu opinión ayuda a mantener una comunidad confiable.",
          style = MaterialTheme.typography.bodySmall,
          color = Slate500
        )

        Divider(color = Slate100)

        // Criteria 1: Puntualidad
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Puntualidad en la llegada", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
            Text("${punctuality.toInt()} ★", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StarAmber)
          }
          Slider(
            value = punctuality,
            onValueChange = { punctuality = it },
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(thumbColor = StarAmber, activeTrackColor = StarAmber)
          )
        }

        // Criteria 2: Calidad
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Calidad del trabajo técnico", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
            Text("${quality.toInt()} ★", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StarAmber)
          }
          Slider(
            value = quality,
            onValueChange = { quality = it },
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(thumbColor = StarAmber, activeTrackColor = StarAmber)
          )
        }

        // Criteria 3: Limpieza y Trato
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Trato respetuoso y limpieza", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
            Text("${cleanliness.toInt()} ★", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StarAmber)
          }
          Slider(
            value = cleanliness,
            onValueChange = { cleanliness = it },
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(thumbColor = StarAmber, activeTrackColor = StarAmber)
          )
        }

        OutlinedTextField(
          value = comment,
          onValueChange = { comment = it },
          label = { Text("Escribe un comentario sobre el trabajo...") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 3,
          shape = RoundedCornerShape(14.dp)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(14.dp)
          ) {
            Text("Cancelar")
          }

          Button(
            onClick = {
              viewModel.submitReview(serviceId, technicianId, punctuality, quality, cleanliness, comment.ifEmpty { "Excelente servicio y trabajo impecable." })
            },
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
          ) {
            Text("Publicar Reseña", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

@Composable
fun QuotationDialog(
  viewModel: TecniRedViewModel,
  serviceId: String,
  onDismiss: () -> Unit
) {
  var title by remember { mutableStateOf("Reparación Tablero Eléctrico") }
  var laborCost by remember { mutableStateOf("80.00") }
  var materialsCost by remember { mutableStateOf("65.00") }
  var estimatedTime by remember { mutableStateOf("2.5 Horas") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp)),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
      Column(
        modifier = Modifier
          .padding(22.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Emitir Presupuesto Formal",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Slate900
        )

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Descripción del Trabajo") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
          value = laborCost,
          onValueChange = { laborCost = it },
          label = { Text("Mano de Obra (S/)") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
          value = materialsCost,
          onValueChange = { materialsCost = it },
          label = { Text("Materiales Estimados (S/)") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )

        OutlinedTextField(
          value = estimatedTime,
          onValueChange = { estimatedTime = it },
          label = { Text("Tiempo Estimado") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )

        Button(
          onClick = {
            val labor = laborCost.toDoubleOrNull() ?: 0.0
            val materials = materialsCost.toDoubleOrNull() ?: 0.0
            viewModel.createQuotation(serviceId, title, labor, materials, estimatedTime)
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
        ) {
          Text("Enviar Presupuesto al Chat", fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
fun TechnicianProfileDialog(
  tech: TechnicianEntity,
  viewModel: TecniRedViewModel,
  onDismiss: () -> Unit
) {
  var requestTitle by remember { mutableStateOf("") }
  var requestDesc by remember { mutableStateOf("") }
  var showRequestForm by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp)),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
      Column(
        modifier = Modifier
          .padding(22.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TechnicianAvatar(name = tech.name, isVerified = tech.isVerified, size = 60)
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Slate500)
          }
        }

        Column {
          Text(tech.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
          Text("${tech.specialty} • ${tech.yearsExperience} años de experiencia", style = MaterialTheme.typography.bodySmall, color = Slate500)
          Spacer(modifier = Modifier.height(4.dp))
          StarRatingBadge(rating = tech.rating, reviewsCount = tech.reviewsCount)
        }

        // Bio
        Text(tech.bio, style = MaterialTheme.typography.bodySmall, color = Slate700, lineHeight = 18.sp)

        // Verified Certificate Box
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Slate50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = BluePrimary)
            Column {
              Text("Certificación Validada", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
              Text(tech.cvTitle, fontSize = 11.sp, color = Slate600)
            }
          }
        }

        // Portfolio Sample "Antes y Después"
        Text("Portafolio de Trabajos Anteriores", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            modifier = Modifier.weight(1f).height(80.dp),
            shape = RoundedCornerShape(12.dp),
            color = Slate200
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text("📸 Foto: Antes", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
            }
          }
          Surface(
            modifier = Modifier.weight(1f).height(80.dp),
            shape = RoundedCornerShape(12.dp),
            color = GreenContainer
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text("✨ Foto: Después", fontSize = 11.sp, color = GreenText, fontWeight = FontWeight.Bold)
            }
          }
        }

        if (!showRequestForm) {
          Button(
            onClick = { showRequestForm = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
          ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Solicitar Cotización a ${tech.name.split(" ").first()}", fontWeight = FontWeight.SemiBold)
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = requestTitle,
              onValueChange = { requestTitle = it },
              label = { Text("Problema / Título del trabajo") },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
              value = requestDesc,
              onValueChange = { requestDesc = it },
              label = { Text("Detalla lo que necesitas reparar...") },
              modifier = Modifier.fillMaxWidth(),
              maxLines = 3,
              shape = RoundedCornerShape(12.dp)
            )
            Button(
              onClick = {
                if (requestTitle.isNotBlank()) {
                  viewModel.requestNewService(tech, requestTitle, requestDesc)
                  onDismiss()
                }
              },
              modifier = Modifier.fillMaxWidth().height(46.dp),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
              Text("Enviar Solicitud Inmediata", fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }
  }
}

@Composable
fun InspectionProposalDialog(
  viewModel: TecniRedViewModel,
  serviceId: String,
  onDismiss: () -> Unit
) {
  var feeInput by remember { mutableStateOf("30.00") }
  var dateInput by remember { mutableStateOf("Hoy, 4:00 PM") }
  var notesInput by remember { mutableStateOf("Inspección física en sitio para diagnosticar fallas ocultas antes del presupuesto final.") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp)),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
      Column(
        modifier = Modifier
          .padding(22.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "VISITA TÉCNICA DE DIAGNÓSTICO",
              style = MaterialTheme.typography.labelSmall,
              color = Slate400,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = "Tarifa Fija de Revisión",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Slate500)
          }
        }

        // Information banner
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = BlueContainer.copy(alpha = 0.5f),
          border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.2f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
            Text(
              text = "Protege tu trabajo: Acuerda una visita con costo fijo para ver la magnitud real del problema antes de fijar el presupuesto.",
              fontSize = 11.sp,
              color = BluePrimary,
              lineHeight = 16.sp
            )
          }
        }

        OutlinedTextField(
          value = feeInput,
          onValueChange = { feeInput = it },
          label = { Text("Tarifa fija de visita / movilidad (S/)") },
          leadingIcon = { Text("S/", fontWeight = FontWeight.Bold, color = Slate600) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
          value = dateInput,
          onValueChange = { dateInput = it },
          label = { Text("Fecha y hora propuesta") },
          leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = Slate600) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
          value = notesInput,
          onValueChange = { notesInput = it },
          label = { Text("Observaciones / Alcance de la visita") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 3,
          shape = RoundedCornerShape(12.dp)
        )

        Button(
          onClick = {
            val fee = feeInput.toDoubleOrNull() ?: 30.0
            viewModel.proposeInspectionVisit(
              serviceId = serviceId,
              fee = fee,
              scheduledDate = dateInput,
              notes = notesInput
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
        ) {
          Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Proponer Visita de Diagnóstico", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun DiagnosticReportDialog(
  viewModel: TecniRedViewModel,
  serviceId: String,
  inspectionFee: Double = 30.0,
  onDismiss: () -> Unit
) {
  var diagnosticFindings by remember {
    mutableStateOf("Inspección en sitio completada: Se identificó recalentamiento de cableado y llave termomagnética averiada.")
  }
  var laborCostInput by remember { mutableStateOf("80.00") }
  var materialsCostInput by remember { mutableStateOf("65.00") }
  var estimatedTimeInput by remember { mutableStateOf("2.5 Horas") }
  var deductInspectionFee by remember { mutableStateOf(true) }

  val labor = laborCostInput.toDoubleOrNull() ?: 0.0
  val materials = materialsCostInput.toDoubleOrNull() ?: 0.0
  val rawTotal = labor + materials
  val netTotal = if (deductInspectionFee) (rawTotal - inspectionFee).coerceAtLeast(0.0) else rawTotal

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp)),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
      Column(
        modifier = Modifier
          .padding(22.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "EVALUACIÓN EN SITIO",
              style = MaterialTheme.typography.labelSmall,
              color = Slate400,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = "Diagnóstico & Presupuesto Final",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Slate900
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Slate500)
          }
        }

        OutlinedTextField(
          value = diagnosticFindings,
          onValueChange = { diagnosticFindings = it },
          label = { Text("Hallazgos técnicos reales del problema") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 3,
          shape = RoundedCornerShape(12.dp)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = laborCostInput,
            onValueChange = { laborCostInput = it },
            label = { Text("Mano de Obra (S/)") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
          )
          OutlinedTextField(
            value = materialsCostInput,
            onValueChange = { materialsCostInput = it },
            label = { Text("Materiales (S/)") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
          )
        }

        OutlinedTextField(
          value = estimatedTimeInput,
          onValueChange = { estimatedTimeInput = it },
          label = { Text("Tiempo Estimado") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )

        // Inspection fee deduction option
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Slate50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { deductInspectionFee = !deductInspectionFee }
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Descontar tarifa de visita previa",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
              )
              Text(
                text = "Abonar los S/ ${String.format("%.2f", inspectionFee)} de la visita al costo total",
                fontSize = 11.sp,
                color = Slate600
              )
            }
            Checkbox(
              checked = deductInspectionFee,
              onCheckedChange = { deductInspectionFee = it }
            )
          }
        }

        // Cost Summary Box
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Slate900,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Subtotal Trabajos:", fontSize = 12.sp, color = Slate400)
              Text("S/ ${String.format("%.2f", rawTotal)}", fontSize = 12.sp, color = SurfaceWhite)
            }
            if (deductInspectionFee) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Descuento Visita Diagnóstico:", fontSize = 12.sp, color = VerifiedGreen)
                Text("- S/ ${String.format("%.2f", inspectionFee)}", fontSize = 12.sp, color = VerifiedGreen)
              }
            }
            Divider(color = Slate800, modifier = Modifier.padding(vertical = 8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("PRESUPUESTO FINAL:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
              Text(
                text = "S/ ${String.format("%.2f", netTotal)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SurfaceWhite
              )
            }
          }
        }

        Button(
          onClick = {
            viewModel.completeSiteDiagnosticAndSubmitFinalBudget(
              serviceId = serviceId,
              diagnosticFindings = diagnosticFindings,
              laborCost = labor,
              materialsCost = materials,
              estimatedTime = estimatedTimeInput,
              deductInspectionFee = deductInspectionFee
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Emitir Presupuesto Definitivo", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
