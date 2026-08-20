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
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentType
import com.example.data.model.ServiceState
import com.example.ui.TecniRedViewModel
import com.example.ui.components.AppHeader
import com.example.ui.components.TechnicianAvatar
import com.example.ui.theme.*

@Composable
fun FinancesScreen(viewModel: TecniRedViewModel) {
  val services by viewModel.services.collectAsState()

  // Calculate financial balances
  val completedServices = services.filter { it.state == ServiceState.COMPLETADO }
  val totalCollected = completedServices.sumOf { it.totalCost }
  val yapePlinTotal = completedServices.filter { it.paymentType == PaymentType.YAPE || it.paymentType == PaymentType.PLIN }.sumOf { it.totalCost }
  val cashTotal = completedServices.filter { it.paymentType == PaymentType.EFECTIVO }.sumOf { it.totalCost }
  val bankTotal = completedServices.filter { it.paymentType == PaymentType.TRANSFERENCIA }.sumOf { it.totalCost }

  val pendingConfirmationServices = services.filter { it.state == ServiceState.PAGO_PENDIENTE }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CanvasBackground)
  ) {
    AppHeader(
      title = "Panel Financiero y Cobros",
      actionContent = {
        IconButton(onClick = { viewModel.showNotification("Configuración de cobros guardada.") }) {
          Icon(Icons.Default.Tune, contentDescription = "Configuración", tint = Slate700)
        }
      }
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
      // 1. Total Balance Statement Card
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
          colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
          shape = RoundedCornerShape(24.dp)
        ) {
          Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Text(
              text = "BALANCE TOTAL COBRADO",
              style = MaterialTheme.typography.labelSmall,
              color = Slate400,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp
            )

            Text(
              text = "S/ ${String.format("%.2f", totalCollected)}",
              style = MaterialTheme.typography.displayLarge,
              fontWeight = FontWeight.Light,
              color = Color.White
            )

            Divider(color = Color.White.copy(alpha = 0.1f))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text("Billeteras (Yape/Plin)", fontSize = 11.sp, color = Slate400)
                Text("S/ ${String.format("%.2f", yapePlinTotal)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
              }
              Column {
                Text("Transferencias", fontSize = 11.sp, color = Slate400)
                Text("S/ ${String.format("%.2f", bankTotal)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
              }
              Column {
                Text("Efectivo", fontSize = 11.sp, color = Slate400)
                Text("S/ ${String.format("%.2f", cashTotal)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
              }
            }
          }
        }
      }

      // 2. Pending Confirmations
      if (pendingConfirmationServices.isNotEmpty()) {
        item {
          Text(
            text = "Cobros por Validar (${pendingConfirmationServices.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Slate900
          )
        }

        items(pendingConfirmationServices, key = { it.id }) { s ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, BlueBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(20.dp)
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(s.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Slate900)
                  Text("Cliente: ${s.clientName} • Método: ${s.paymentType?.displayName ?: "Yape"}", fontSize = 12.sp, color = Slate500)
                }
                Text("S/ ${String.format("%.2f", s.totalCost)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BluePrimary)
              }

              Button(
                onClick = { viewModel.confirmPaymentAsTechnician(s.id) },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerifiedGreen)
              ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirmar Pago Recibido", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }

      // 3. Payment Methods Configuration
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate100, RoundedCornerShape(20.dp)),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          shape = RoundedCornerShape(20.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
              text = "Mis Datos de Cobro Configurados",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = Slate900
            )

            // Yape & Plin
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Slate50)
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(YapePurple),
                contentAlignment = Alignment.Center
              ) {
                Text("Y", color = Color.White, fontWeight = FontWeight.Bold)
              }
              Column(modifier = Modifier.weight(1f)) {
                Text("Yape / Plin Asociado", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Slate900)
                Text("987 654 321 (Roberto García)", fontSize = 12.sp, color = Slate500)
              }
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerifiedGreen, modifier = Modifier.size(20.dp))
            }

            // Bank Account
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Slate50)
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(BluePrimary),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              }
              Column(modifier = Modifier.weight(1f)) {
                Text("BCP Cuenta Soles", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Slate900)
                Text("193-98765432-0-12 • CCI: 002-1930098765432012-14", fontSize = 11.sp, color = Slate500)
              }
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerifiedGreen, modifier = Modifier.size(20.dp))
            }
          }
        }
      }
    }
  }
}
