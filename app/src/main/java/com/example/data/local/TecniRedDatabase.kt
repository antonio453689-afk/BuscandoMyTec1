package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.ReviewDao
import com.example.data.local.dao.ServiceDao
import com.example.data.local.dao.ServiceRequestDao
import com.example.data.local.dao.TechnicianDao
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ReviewEntity
import com.example.data.local.entities.ServiceEntity
import com.example.data.local.entities.ServiceRequestEntity
import com.example.data.local.entities.TechnicianEntity
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentType
import com.example.data.model.QuotationStatus
import com.example.data.model.ServiceState

class Converters {
  @TypeConverter
  fun fromServiceState(value: ServiceState): String = value.name

  @TypeConverter
  fun toServiceState(value: String): ServiceState = try {
    ServiceState.valueOf(value)
  } catch (e: Exception) {
    ServiceState.SOLICITADO
  }

  @TypeConverter
  fun fromPaymentType(value: PaymentType?): String? = value?.name

  @TypeConverter
  fun toPaymentType(value: String?): PaymentType? = value?.let {
    try {
      PaymentType.valueOf(it)
    } catch (e: Exception) {
      null
    }
  }

  @TypeConverter
  fun fromPaymentStatus(value: PaymentStatus): String = value.name

  @TypeConverter
  fun toPaymentStatus(value: String): PaymentStatus = try {
    PaymentStatus.valueOf(value)
  } catch (e: Exception) {
    PaymentStatus.PENDIENTE
  }

  @TypeConverter
  fun fromQuotationStatus(value: QuotationStatus): String = value.name

  @TypeConverter
  fun toQuotationStatus(value: String): QuotationStatus = try {
    QuotationStatus.valueOf(value)
  } catch (e: Exception) {
    QuotationStatus.PENDING
  }
}

@Database(
  entities = [
    TechnicianEntity::class,
    ServiceEntity::class,
    ChatMessageEntity::class,
    ReviewEntity::class,
    ServiceRequestEntity::class
  ],
  version = 4,
  exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TecniRedDatabase : RoomDatabase() {
  abstract fun technicianDao(): TechnicianDao
  abstract fun serviceDao(): ServiceDao
  abstract fun chatMessageDao(): ChatMessageDao
  abstract fun reviewDao(): ReviewDao
  abstract fun serviceRequestDao(): ServiceRequestDao

  companion object {
    @Volatile
    private var INSTANCE: TecniRedDatabase? = null

    fun getDatabase(context: Context): TecniRedDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          TecniRedDatabase::class.java,
          "tecnired_database"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
