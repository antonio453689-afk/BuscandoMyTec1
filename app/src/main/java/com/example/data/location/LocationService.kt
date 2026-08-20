package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class UserLocationData(
  val latitude: Double,
  val longitude: Double,
  val address: String = "",
  val district: String = "",
  val isGpsActive: Boolean = true
)

class LocationService(private val context: Context) {
  private val fusedLocationClient: FusedLocationProviderClient =
    LocationServices.getFusedLocationProviderClient(context)

  @SuppressLint("MissingPermission")
  suspend fun getCurrentLocation(): UserLocationData? {
    return withContext(Dispatchers.IO) {
      try {
        val cancellationTokenSource = CancellationTokenSource()
        val location: Location? = suspendCancellableCoroutine { continuation ->
          fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
          ).addOnSuccessListener { loc ->
            continuation.resume(loc)
          }.addOnFailureListener {
            continuation.resume(null)
          }

          continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
          }
        }

        if (location != null) {
          val (address, district) = getAddressFromCoordinates(location.latitude, location.longitude)
          UserLocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            address = address,
            district = district,
            isGpsActive = true
          )
        } else {
          // Fallback to last known location
          val lastLocation: Location? = suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
              .addOnSuccessListener { loc -> continuation.resume(loc) }
              .addOnFailureListener { continuation.resume(null) }
          }
          if (lastLocation != null) {
            val (address, district) = getAddressFromCoordinates(lastLocation.latitude, lastLocation.longitude)
            UserLocationData(
              latitude = lastLocation.latitude,
              longitude = lastLocation.longitude,
              address = address,
              district = district,
              isGpsActive = true
            )
          } else {
            null
          }
        }
      } catch (e: Exception) {
        null
      }
    }
  }

  private fun getAddressFromCoordinates(latitude: Double, longitude: Double): Pair<String, String> {
    return try {
      val geocoder = Geocoder(context, Locale("es", "PE"))
      val addresses = geocoder.getFromLocation(latitude, longitude, 1)
      if (!addresses.isNullOrEmpty()) {
        val addr = addresses[0]
        val featureName = addr.thoroughfare ?: addr.featureName ?: "Av. Principal"
        val subThoroughfare = addr.subThoroughfare ?: ""
        val addressLine = if (subThoroughfare.isNotBlank()) "$featureName $subThoroughfare" else featureName
        val subLocality = addr.subLocality ?: addr.locality ?: "San Isidro"
        Pair(addressLine, subLocality)
      } else {
        Pair("Ubicación GPS detectada", "Lima")
      }
    } catch (e: Exception) {
      Pair("Ubicación GPS actual", "Lima")
    }
  }

  /**
   * Calculates the exact distance in Kilometers between two coordinates using Android Location SDK.
   */
  fun calculateDistanceBetweenKm(
    startLat: Double,
    startLon: Double,
    endLat: Double,
    endLon: Double
  ): Double {
    val results = FloatArray(1)
    Location.distanceBetween(startLat, startLon, endLat, endLon, results)
    val meters = results[0]
    val km = meters / 1000.0
    return (Math.round(km * 10.0) / 10.0).coerceAtLeast(0.1)
  }
}
