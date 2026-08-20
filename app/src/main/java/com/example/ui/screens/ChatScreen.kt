package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.model.QuotationStatus
import com.example.data.model.UserRole
import com.example.ui.AppTab
import com.example.ui.TecniRedViewModel
import com.example.ui.components.AppHeader
import com.example.ui.components.TechnicianAvatar
import com.example.ui.theme.*

@Composable
fun ChatScreen(viewModel: TecniRedViewModel) {
  val activeService by viewModel.activeService.collectAsState()
  val messages by viewModel.chatMessages.collectAsState()
  val activeRole by viewModel.activeRole.collectAsState()

  var inputText by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CanvasBackground)
  ) {
    // Header
    AppHeader(
      title = activeService?.technicianName ?: "Chat de Cotización",
      onBackClick = { viewModel.selectTab(AppTab.Servicios) },
      actionContent = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Button to offer diagnostic inspection visit
          IconButton(onClick = { viewModel.setInspectionDialogVisible(true) }) {
            Icon(
              imageVector = Icons.Default.Engineering,
              contentDescription = "Ofrecer Visita de Diagnóstico",
              tint = BluePrimary
            )
          }

          // Button to submit diagnostic report / final budget
          IconButton(onClick = { viewModel.setDiagnosticReportDialogVisible(true) }) {
            Icon(
              imageVector = Icons.Default.Checklist,
              contentDescription = "Diagnóstico en Sitio",
              tint = VerifiedGreen
            )
          }

          IconButton(onClick = { viewModel.setQuotationDialogVisible(true) }) {
            Icon(
              imageVector = Icons.Default.RequestQuote,
              contentDescription = "Emitir Presupuesto",
              tint = Slate700
            )
          }
        }
      }
    )

    // Service Context Mini-Bar
    if (activeService != null) {
      Surface(
        color = BlueContainer,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = activeService!!.title,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = OnBlueContainer
            )
            Text(
              text = "Estado: ${activeService!!.state.label}",
              style = MaterialTheme.typography.labelSmall,
              color = BluePrimaryDark
            )
          }

          if (activeService!!.totalCost > 0) {
            Text(
              text = "S/ ${String.format("%.2f", activeService!!.totalCost)}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = BluePrimaryDark
            )
          }
        }
      }
    }

    // Chat Message Thread
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(messages, key = { it.id }) { msg ->
        ChatMessageBubble(
          message = msg,
          activeRole = activeRole,
          onAccept = {
            if (activeService != null) {
              viewModel.acceptQuotation(activeService!!.id, msg.id)
            }
          },
          onReject = {
            if (activeService != null) {
              viewModel.rejectQuotation(activeService!!.id, msg.id)
            }
          }
        )
      }
    }

    // Input Bar
    Surface(
      color = SurfaceWhite,
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .imePadding()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Quick Action: Send Photo / Voucher Simulation
        IconButton(
          onClick = {
            if (activeService != null) {
              viewModel.setPaymentModalVisible(true)
            }
          }
        ) {
          Icon(Icons.Default.AttachFile, contentDescription = "Adjuntar", tint = Slate500)
        }

        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          modifier = Modifier.weight(1f),
          placeholder = { Text("Escribe un mensaje o consulta...", fontSize = 14.sp, color = Slate400) },
          maxLines = 3,
          shape = RoundedCornerShape(20.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Slate50,
            unfocusedContainerColor = Slate50,
            focusedBorderColor = BluePrimary,
            unfocusedBorderColor = Slate200
          )
        )

        IconButton(
          onClick = {
            if (inputText.isNotBlank() && activeService != null) {
              viewModel.sendTextMessage(activeService!!.id, inputText)
              inputText = ""
            }
          },
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(BluePrimary)
        ) {
          Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
fun ChatMessageBubble(
  message: ChatMessageEntity,
  activeRole: UserRole,
  onAccept: () -> Unit,
  onReject: () -> Unit
) {
  val isClientMessage = message.senderId == "CLIENT"

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isClientMessage) Alignment.End else Alignment.Start
  ) {
    if (message.isInspectionOffer) {
      // Visita Técnica de Diagnóstico Proposal Card
      Card(
        modifier = Modifier
          .fillMaxWidth(0.92f)
          .shadow(4.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BluePrimary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(20.dp)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.Engineering, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
              Text(
                text = "VISITA DE DIAGNÓSTICO",
                style = MaterialTheme.typography.labelSmall,
                color = BluePrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              )
            }

            Text(
              text = "S/ ${String.format("%.2f", message.inspectionFee)}",
              style = MaterialTheme.typography.titleLarge,
              color = Slate900,
              fontWeight = FontWeight.Bold
            )
          }

          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = Slate800,
            lineHeight = 20.sp
          )

          if (message.inspectionDate.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Slate50,
              border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Slate600, modifier = Modifier.size(16.dp))
                Text(
                  text = "Fecha propuesta: ${message.inspectionDate}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  color = Slate800
                )
              }
            }
          }

          Button(
            onClick = onAccept,
            modifier = Modifier
              .fillMaxWidth()
              .height(42.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aceptar Visita de Revisión (S/ ${String.format("%.2f", message.inspectionFee)})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    } else if (message.isQuotation) {
      // In-Chat Formal Quotation Card in Dark Slate style
      Card(
        modifier = Modifier
          .fillMaxWidth(0.92f)
          .shadow(4.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        shape = RoundedCornerShape(20.dp)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
          ) {
            Column {
              Text(
                text = if (message.isDiagnosticReport) "DIAGNÓSTICO EN SITIO & PRESUPUESTO" else "PRESUPUESTO FORMAL",
                style = MaterialTheme.typography.labelSmall,
                color = if (message.isDiagnosticReport) VerifiedGreen else Slate400,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              )
              Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
              )
            }
            Text(
              text = "S/ ${String.format("%.2f", message.totalCost)}",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
              fontWeight = FontWeight.Light
            )
          }

          Divider(color = Color.White.copy(alpha = 0.1f))

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Mano de obra", style = MaterialTheme.typography.bodySmall, color = Slate400)
              Text("S/ ${String.format("%.2f", message.laborCost)}", style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Materiales", style = MaterialTheme.typography.bodySmall, color = Slate400)
              Text("S/ ${String.format("%.2f", message.materialsCost)}", style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Tiempo estimado", style = MaterialTheme.typography.bodySmall, color = Slate400)
              Text(message.estimatedTime.ifEmpty { "2.5 Horas" }, style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
          }

          // Status & Buttons
          when (message.quotationStatus) {
            QuotationStatus.PENDING -> {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Button(
                  onClick = onReject,
                  modifier = Modifier.weight(1f).height(40.dp),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f))
                ) {
                  Text("Rechazar", fontSize = 12.sp, color = Color.White)
                }

                Button(
                  onClick = onAccept,
                  modifier = Modifier.weight(1f).height(40.dp),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                  Text("Aceptar Trabajo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
            QuotationStatus.ACCEPTED -> {
              Surface(
                color = GreenContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "✓ Presupuesto Aceptado",
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = GreenText
                )
              }
            }
            QuotationStatus.REJECTED -> {
              Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "✕ Presupuesto Rechazado",
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium,
                  color = Color.White
                )
              }
            }
          }
        }
      }
    } else {
      // Standard Chat Bubble
      Surface(
        shape = RoundedCornerShape(
          topStart = 18.dp,
          topEnd = 18.dp,
          bottomStart = if (isClientMessage) 18.dp else 4.dp,
          bottomEnd = if (isClientMessage) 4.dp else 18.dp
        ),
        color = if (isClientMessage) BluePrimary else SurfaceWhite,
        shadowElevation = 1.dp,
        border = if (isClientMessage) null else androidx.compose.foundation.BorderStroke(1.dp, Slate100),
        modifier = Modifier.widthIn(max = 280.dp)
      ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isClientMessage) Color.White else Slate900
          )
        }
      }
    }
  }
}
