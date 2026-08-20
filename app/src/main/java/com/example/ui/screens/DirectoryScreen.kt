package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.local.entities.TechnicianEntity
import com.example.ui.TecniRedViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DirectoryScreen(
  viewModel: TecniRedViewModel,
  onOpenProfile: (TechnicianEntity) -> Unit
) {
  val technicians by viewModel.technicians.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val selectedSpecialty by viewModel.selectedSpecialty.collectAsState()
  val clientDistrict by viewModel.clientDistrict.collectAsState()
  val isGpsLocating by viewModel.isGpsLocating.collectAsState()
  val isGpsActive by viewModel.isGpsActive.collectAsState()
  val activeRequests by viewModel.activeServiceRequests.collectAsState()
  val currentRadius by viewModel.radarCurrentRadiusKm.collectAsState()

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (fineGranted || coarseGranted) {
      viewModel.detectUserLocationWithPlayServices()
    } else {
      viewModel.showNotification("Permiso de ubicación denegado. Usando distrito seleccionado.", isSuccess = false)
    }
  }

  val specialties = listOf("Todos", "Electricista", "Gasfitero", "Carpintería", "Pintura")

  val filteredTechs = technicians.filter { tech ->
    val matchesSpecialty = if (selectedSpecialty == "Todos") true else tech.specialty.contains(selectedSpecialty, ignoreCase = true)
    val matchesSearch = searchQuery.isBlank() || tech.name.contains(searchQuery, ignoreCase = true) || tech.specialty.contains(searchQuery, ignoreCase = true) || tech.bio.contains(searchQuery, ignoreCase = true)
    matchesSpecialty && matchesSearch
  }.sortedBy { it.distanceKm }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CanvasBackground)
  ) {
    // Header
    AppHeader(
      title = "Especialistas Locales",
      actionContent = {
        IconButton(
          onClick = {
            locationPermissionLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
              )
            )
          }
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(if (isGpsActive) VerifiedGreen.copy(alpha = 0.15f) else BlueContainer),
            contentAlignment = Alignment.Center
          ) {
            if (isGpsLocating) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = BluePrimary)
            } else {
              Icon(
                imageVector = if (isGpsActive) Icons.Default.GpsFixed else Icons.Default.MyLocation,
                contentDescription = "Geolocalización GPS",
                tint = if (isGpsActive) VerifiedGreen else BluePrimary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    )

    // GPS Location Bar (Play Services Location)
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      shape = RoundedCornerShape(12.dp),
      color = if (isGpsActive) VerifiedGreen.copy(alpha = 0.08f) else BlueContainer.copy(alpha = 0.5f),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (isGpsActive) VerifiedGreen.copy(alpha = 0.25f) else BluePrimary.copy(alpha = 0.2f)
      )
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = if (isGpsActive) Icons.Default.LocationOn else Icons.Default.NearMe,
            contentDescription = null,
            tint = if (isGpsActive) VerifiedGreen else BluePrimary,
            modifier = Modifier.size(18.dp)
          )
          Column {
            Text(
              text = if (isGpsActive) "GPS Play Services: $clientDistrict" else "Ubicación: $clientDistrict",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (isGpsActive) VerifiedGreen else Slate800
            )
            Text(
              text = "Distancias calculadas en tiempo real",
              fontSize = 10.sp,
              color = Slate500
            )
          }
        }

        TextButton(
          onClick = {
            locationPermissionLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
              )
            )
          },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          if (isGpsLocating) {
            Text("Buscando...", fontSize = 11.sp, color = BluePrimary)
          } else {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = BluePrimary)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Actualizar GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
          }
        }
      }
    }

    // Search Box & Specialty Filters
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { viewModel.setSearchQuery(it) },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp),
        placeholder = { Text("Buscar electricista, gasfitero...", color = Slate400, fontSize = 14.sp) },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = Slate400)
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.setSearchQuery("") }) {
              Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Slate400)
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = SurfaceWhite,
          unfocusedContainerColor = SurfaceWhite,
          focusedBorderColor = BluePrimary,
          unfocusedBorderColor = Slate200
        )
      )

      // Category Chips Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        specialties.forEach { spec ->
          val isSelected = selectedSpecialty == spec
          FilterChip(
            selected = isSelected,
            onClick = { viewModel.setSpecialty(spec) },
            label = {
              Text(
                text = spec,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
              )
            },
            shape = RoundedCornerShape(50),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = BluePrimary,
              selectedLabelColor = Color.White,
              containerColor = SurfaceWhite,
              labelColor = Slate700
            ),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = isSelected,
              borderColor = if (isSelected) BluePrimary else Slate200
            )
          )
        }
      }
    }

    // Technician List with Proximity Broadcast Hero Banner
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(bottom = 80.dp)
    ) {
      // Proximity Broadcast Hero Banner
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp)),
          colors = CardDefaults.cardColors(containerColor = Slate900),
          shape = RoundedCornerShape(22.dp)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BluePrimary),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Radar, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text(
                  text = "RADAR EN TIEMPO REAL",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF60A5FA),
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                )
              }

              Surface(
                shape = RoundedCornerShape(50),
                color = BluePrimary.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.4f))
              ) {
                Text(
                  text = "GEOLOCALIZACIÓN GPS",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF93C5FD),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            Text(
              text = "¿Emergencia o avería urgente?",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )

            Text(
              text = "Lanza una petición abierta para que los técnicos disponibles en tu radio de distancia reciban una alerta sonora inmediata y acepten acudir a tu domicilio.",
              fontSize = 12.sp,
              color = Color(0xFFCBD5E1),
              lineHeight = 17.sp
            )

            Button(
              onClick = { viewModel.setRequestBroadcastDialogVisible(true) },
              modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
              Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Transmitir Solicitud Cercana",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            }
          }
        }
      }

      // Active Radar Service Alerts for Technicians
      if (activeRequests.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFEF4444))
              )
              Text(
                text = "ALERTAS DE SERVICIO EN VIVO (${activeRequests.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFDC2626),
                letterSpacing = 0.5.sp
              )
            }

            activeRequests.forEach { req ->
              val isEscalated = req.maxRadiusKm > 5.0
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .shadow(2.dp, RoundedCornerShape(16.dp))
                  .border(
                    width = 1.dp,
                    color = if (isEscalated) Color(0xFFF59E0B) else Color(0xFFEF4444).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                  ),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = (if (isEscalated) Color(0xFFF59E0B) else Color(0xFFEF4444)).copy(alpha = 0.15f)
                    ) {
                      Text(
                        text = if (isEscalated) "⚡ Radio Ampliado: 10 km" else "📍 Radio Cercano: 5 km",
                        color = if (isEscalated) Color(0xFFD97706) else Color(0xFFDC2626),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }

                    Text(
                      text = req.urgency,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = Slate600
                    )
                  }

                  Text(
                    text = req.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                  )

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate50,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text(
                        text = "Detalle de la problemática:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                      )
                      Text(
                        text = req.description,
                        fontSize = 11.sp,
                        color = Slate700,
                        lineHeight = 15.sp,
                        maxLines = 2
                      )
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
                      color = Slate600
                    )
                    Text(
                      text = if (req.isZeroVisitCondition) "Visita: S/ 0.00 (Condicional)" else "Visita: S/ ${String.format("%.2f", req.proposedInspectionFee)}",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = if (req.isZeroVisitCondition) BluePrimary else VerifiedGreen
                    )
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedButton(
                      onClick = { viewModel.triggerTechnicianAlertPreview(req) },
                      modifier = Modifier.weight(1f).height(38.dp),
                      shape = RoundedCornerShape(10.dp)
                    ) {
                      Text("Ver Alerta", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                      onClick = { viewModel.acceptBroadcastAsTechnician(req) },
                      modifier = Modifier.weight(1f).height(38.dp),
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = VerifiedGreen)
                    ) {
                      Text("Aceptar Servicio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }

      item {
        Text(
          text = "${filteredTechs.size} especialistas verificados cerca de ti",
          style = MaterialTheme.typography.labelSmall,
          color = Slate500,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
      }

      items(filteredTechs, key = { it.id }) { tech ->
        TechnicianCard(
          tech = tech,
          onClick = { onOpenProfile(tech) },
          onConsultClick = {
            viewModel.selectTechnician(tech.id)
            onOpenProfile(tech)
          }
        )
      }
    }
  }
}

@Composable
fun TechnicianCard(
  tech: TechnicianEntity,
  onClick: () -> Unit,
  onConsultClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(1.dp, RoundedCornerShape(24.dp))
      .clip(RoundedCornerShape(24.dp))
      .background(SurfaceWhite)
      .border(1.dp, Slate100, RoundedCornerShape(24.dp))
      .clickable { onClick() },
    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
      ) {
        TechnicianAvatar(name = tech.name, isVerified = tech.isVerified, size = 58)

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = tech.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = Slate900
            )
            Text(
              text = "S/ ${tech.baseHourlyRate.toInt()}/h",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = BluePrimary
            )
          }

          Text(
            text = "${tech.specialty} • ${tech.yearsExperience} años exp.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
          )

          // Distance and Online Radar indicator
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 2.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = BlueContainer.copy(alpha = 0.6f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(Icons.Default.Navigation, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(12.dp))
                Text(
                  text = "A ${String.format("%.1f", tech.distanceKm)} km • ${tech.district}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = BluePrimary
                )
              }
            }

            StarRatingBadge(rating = tech.rating, reviewsCount = tech.reviewsCount)
          }
        }
      }

      Text(
        text = tech.bio,
        style = MaterialTheme.typography.bodySmall,
        color = Slate600,
        maxLines = 2,
        lineHeight = 18.sp
      )

      // Verified Certification Badge
      if (tech.cvVerified) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Slate50,
          border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Verified,
              contentDescription = null,
              tint = VerifiedGreen,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = tech.cvTitle,
              style = MaterialTheme.typography.labelSmall,
              color = Slate700,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      // Accepted Payment Methods & Action Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          tech.paymentTypes.split(",").take(3).forEach { method ->
            PaymentMethodPill(method.trim())
          }
        }

        Button(
          onClick = onConsultClick,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier.height(38.dp)
        ) {
          Text("Cotizar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}
