package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServiceState
import com.example.ui.theme.*

@Composable
fun AppHeader(
  title: String,
  onBackClick: () -> Unit = {},
  actionContent: @Composable () -> Unit = {}
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(56.dp)
      .background(SurfaceWhite.copy(alpha = 0.95f))
      .border(width = 1.dp, color = Slate100)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .clickable { onBackClick() },
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "←",
          fontSize = 20.sp,
          fontWeight = FontWeight.Light,
          color = Slate900
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Slate900,
        fontWeight = FontWeight.SemiBold
      )
    }

    actionContent()
  }
}

@Composable
fun StatusProgressBar(state: ServiceState) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(BlueContainer)
      .border(1.dp, BlueBorder, RoundedCornerShape(16.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Progress Bar
    Box(
      modifier = Modifier
        .weight(1f)
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(Color(0xFFBFDBFE))
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(fraction = state.progress)
          .clip(RoundedCornerShape(4.dp))
          .background(BluePrimary)
      )
    }

    // Status Tag
    Text(
      text = state.label.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      color = OnBlueContainer,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.8.sp
    )
  }
}

@Composable
fun TechnicianAvatar(
  name: String,
  isVerified: Boolean = true,
  size: Int = 60
) {
  val initials = name.split(" ")
    .take(2)
    .mapNotNull { it.firstOrNull()?.uppercase() }
    .joinToString("")

  Box(
    modifier = Modifier.size(size.dp),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .size(size.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(Slate200),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = if (initials.isNotEmpty()) initials else "TR",
        style = MaterialTheme.typography.titleLarge,
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        color = Slate700,
        fontWeight = FontWeight.Bold
      )
    }

    if (isVerified) {
      Box(
        modifier = Modifier
          .size(20.dp)
          .align(Alignment.BottomEnd)
          .clip(CircleShape)
          .background(VerifiedGreen)
          .border(2.dp, SurfaceWhite, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Verificado",
          tint = Color.White,
          modifier = Modifier.size(12.dp)
        )
      }
    }
  }
}

@Composable
fun PaymentMethodPill(methodName: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(50))
      .background(Slate100)
      .padding(horizontal = 10.dp, vertical = 4.dp)
  ) {
    Text(
      text = methodName.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      color = Slate600,
      fontWeight = FontWeight.Medium,
      letterSpacing = 0.4.sp
    )
  }
}

@Composable
fun StarRatingBadge(rating: Float, reviewsCount: Int) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Icon(
      imageVector = Icons.Default.Star,
      contentDescription = "Rating",
      tint = StarAmber,
      modifier = Modifier.size(14.dp)
    )
    Text(
      text = String.format("%.1f", rating),
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
      color = Slate900
    )
    Text(
      text = "($reviewsCount reseñas)",
      style = MaterialTheme.typography.bodySmall,
      color = Slate400,
      fontSize = 11.sp
    )
  }
}
