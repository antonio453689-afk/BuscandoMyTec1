package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entities.ServiceRequestEntity
import com.example.data.local.entities.TechnicianEntity
import com.example.data.model.UserRole
import com.example.ui.AppTab
import com.example.ui.TecniRedViewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        TecniRedApp()
      }
    }
  }
}

@Composable
fun TecniRedApp(viewModel: TecniRedViewModel = viewModel()) {
  val currentTab by viewModel.currentTab.collectAsState()
  val activeRole by viewModel.activeRole.collectAsState()
  val activeService by viewModel.activeService.collectAsState()
  val notification by viewModel.notification.collectAsState()

  // Modals state
  val showPaymentModal by viewModel.showPaymentModal.collectAsState()
  val showReviewModal by viewModel.showReviewModal.collectAsState()
  val showQuotationDialog by viewModel.showQuotationDialog.collectAsState()
  val showInspectionDialog by viewModel.showInspectionDialog.collectAsState()
  val showDiagnosticReportDialog by viewModel.showDiagnosticReportDialog.collectAsState()
  val showRequestBroadcastDialog by viewModel.showRequestBroadcastDialog.collectAsState()
  val showRadarSearchSheet by viewModel.showRadarSearchSheet.collectAsState()
  val incomingUberAlert by viewModel.incomingUberAlert.collectAsState()
  var selectedTechForProfile by remember { mutableStateOf<TechnicianEntity?>(null) }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    bottomBar = {
      TecniRedBottomNav(
        currentTab = currentTab,
        onTabSelected = { viewModel.selectTab(it) }
      )
    },
    contentWindowInsets = WindowInsets.safeDrawing
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(CanvasBackground)
    ) {
      // Screen Content Router
      when (currentTab) {
        AppTab.Inicio -> {
          DirectoryScreen(
            viewModel = viewModel,
            onOpenProfile = { selectedTechForProfile = it }
          )
        }
        AppTab.Servicios -> {
          ServiceDetailScreen(
            viewModel = viewModel,
            onNavigateToChat = { viewModel.selectTab(AppTab.Chats) }
          )
        }
        AppTab.Chats -> {
          ChatScreen(viewModel = viewModel)
        }
        AppTab.Perfil -> {
          FinancesScreen(viewModel = viewModel)
        }
      }

      // Notification Toast Overlay
      AnimatedVisibility(
        visible = notification != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 16.dp, start = 16.dp, end = 16.dp)
      ) {
        notification?.let { notif ->
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (notif.isSuccess) Slate900 else ErrorRed,
            shadowElevation = 8.dp,
            modifier = Modifier.clickable { viewModel.clearNotification() }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = if (notif.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (notif.isSuccess) VerifiedGreen else Color.White,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = notif.message,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }

      // Modal Dialogs
      if (showPaymentModal && activeService != null) {
        PaymentModal(
          viewModel = viewModel,
          serviceId = activeService!!.id,
          totalAmount = activeService!!.totalCost,
          onDismiss = { viewModel.setPaymentModalVisible(false) }
        )
      }

      if (showReviewModal && activeService != null) {
        ReviewModal(
          viewModel = viewModel,
          serviceId = activeService!!.id,
          technicianId = activeService!!.technicianId,
          onDismiss = { viewModel.setReviewModalVisible(false) }
        )
      }

      if (showQuotationDialog && activeService != null) {
        QuotationDialog(
          viewModel = viewModel,
          serviceId = activeService!!.id,
          onDismiss = { viewModel.setQuotationDialogVisible(false) }
        )
      }

      if (showInspectionDialog && activeService != null) {
        InspectionProposalDialog(
          viewModel = viewModel,
          serviceId = activeService!!.id,
          onDismiss = { viewModel.setInspectionDialogVisible(false) }
        )
      }

      if (showDiagnosticReportDialog && activeService != null) {
        DiagnosticReportDialog(
          viewModel = viewModel,
          serviceId = activeService!!.id,
          inspectionFee = activeService!!.inspectionFee,
          onDismiss = { viewModel.setDiagnosticReportDialogVisible(false) }
        )
      }

      // Proximity & Radar Dialogs
      if (showRequestBroadcastDialog) {
        RequestServiceBroadcastDialog(
          viewModel = viewModel,
          onDismiss = { viewModel.setRequestBroadcastDialogVisible(false) }
        )
      }

      if (showRadarSearchSheet) {
        RadarSearchSheet(
          viewModel = viewModel,
          onDismiss = { viewModel.setRadarSearchSheetVisible(false) }
        )
      }

      // Uber/inDrive style Incoming Order Alert for Technicians
      incomingUberAlert?.let { request ->
        UberStyleIncomingRequestDialog(
          request = request,
          onAccept = { viewModel.acceptBroadcastAsTechnician(request) },
          onDecline = { viewModel.dismissIncomingUberAlert() }
        )
      }

      selectedTechForProfile?.let { tech ->
        TechnicianProfileDialog(
          tech = tech,
          viewModel = viewModel,
          onDismiss = { selectedTechForProfile = null }
        )
      }
    }
  }
}

@Composable
fun TecniRedBottomNav(
  currentTab: AppTab,
  onTabSelected: (AppTab) -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.navigationBars),
    color = SurfaceWhite,
    border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
    shadowElevation = 4.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(72.dp)
        .padding(horizontal = 20.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 1. Inicio
      NavTabItem(
        title = "Inicio",
        icon = if (currentTab == AppTab.Inicio) Icons.Filled.Home else Icons.Outlined.Home,
        isSelected = currentTab == AppTab.Inicio,
        onClick = { onTabSelected(AppTab.Inicio) }
      )

      // 2. Detalle
      NavTabItem(
        title = "Detalle",
        icon = if (currentTab == AppTab.Servicios) Icons.Filled.Assignment else Icons.Outlined.Assignment,
        isSelected = currentTab == AppTab.Servicios,
        onClick = { onTabSelected(AppTab.Servicios) }
      )

      // 3. Chats
      NavTabItem(
        title = "Chats",
        icon = if (currentTab == AppTab.Chats) Icons.Filled.Mail else Icons.Outlined.Mail,
        isSelected = currentTab == AppTab.Chats,
        hasBadge = true,
        onClick = { onTabSelected(AppTab.Chats) }
      )

      // 4. Perfil / Finanzas
      NavTabItem(
        title = "Finanzas",
        icon = if (currentTab == AppTab.Perfil) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
        isSelected = currentTab == AppTab.Perfil,
        onClick = { onTabSelected(AppTab.Perfil) }
      )
    }
  }
}

@Composable
fun NavTabItem(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  hasBadge: Boolean = false,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Box(contentAlignment = Alignment.TopEnd) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = if (isSelected) BluePrimary else Slate400,
        modifier = Modifier.size(22.dp)
      )
      if (hasBadge && !isSelected) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(ErrorRed)
            .border(1.dp, SurfaceWhite, CircleShape)
        )
      }
    }

    Text(
      text = title.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      fontSize = 10.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      color = if (isSelected) BluePrimary else Slate400,
      letterSpacing = 0.5.sp
    )
  }
}
