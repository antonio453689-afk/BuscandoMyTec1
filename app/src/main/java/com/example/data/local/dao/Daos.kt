package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ReviewEntity
import com.example.data.local.entities.ServiceEntity
import com.example.data.local.entities.ServiceRequestEntity
import com.example.data.local.entities.TechnicianEntity
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentType
import com.example.data.model.ServiceState
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceRequestDao {
  @Query("SELECT * FROM service_requests ORDER BY createdAt DESC")
  fun getAllRequests(): Flow<List<ServiceRequestEntity>>

  @Query("SELECT * FROM service_requests WHERE status = 'SEARCHING' ORDER BY createdAt DESC")
  fun getActiveSearchingRequests(): Flow<List<ServiceRequestEntity>>

  @Query("SELECT * FROM service_requests WHERE id = :id")
  fun getRequestById(id: String): Flow<ServiceRequestEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRequest(request: ServiceRequestEntity)

  @Update
  suspend fun updateRequest(request: ServiceRequestEntity)

  @Query("UPDATE service_requests SET status = :status, acceptedByTechId = :techId, acceptedByTechName = :techName WHERE id = :requestId")
  suspend fun markRequestAccepted(requestId: String, status: String, techId: String, techName: String)

  @Query("UPDATE service_requests SET maxRadiusKm = :radius WHERE id = :requestId")
  suspend fun updateRequestRadius(requestId: String, radius: Double)

  @Query("UPDATE service_requests SET status = 'CANCELLED' WHERE id = :requestId")
  suspend fun cancelRequest(requestId: String)
}

@Dao
interface TechnicianDao {
  @Query("SELECT * FROM technicians ORDER BY rating DESC")
  fun getAllTechnicians(): Flow<List<TechnicianEntity>>

  @Query("SELECT * FROM technicians WHERE id = :id")
  suspend fun getTechnicianById(id: String): TechnicianEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTechnicians(technicians: List<TechnicianEntity>)

  @Update
  suspend fun updateTechnicians(technicians: List<TechnicianEntity>)

  @Update
  suspend fun updateTechnician(technician: TechnicianEntity)
}

@Dao
interface ServiceDao {
  @Query("SELECT * FROM services ORDER BY createdAt DESC")
  fun getAllServices(): Flow<List<ServiceEntity>>

  @Query("SELECT * FROM services WHERE id = :id")
  fun getServiceById(id: String): Flow<ServiceEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertService(service: ServiceEntity)

  @Update
  suspend fun updateService(service: ServiceEntity)

  @Query("UPDATE services SET state = :newState WHERE id = :serviceId")
  suspend fun updateServiceState(serviceId: String, newState: ServiceState)

  @Query("UPDATE services SET state = :newState, paymentStatus = :newPaymentStatus, paymentType = :paymentType WHERE id = :serviceId")
  suspend fun updatePaymentInfo(serviceId: String, newState: ServiceState, newPaymentStatus: PaymentStatus, paymentType: PaymentType)

  @Query("UPDATE services SET paymentStatus = :newStatus WHERE id = :serviceId")
  suspend fun updatePaymentStatus(serviceId: String, newStatus: PaymentStatus)
}

@Dao
interface ChatMessageDao {
  @Query("SELECT * FROM chat_messages WHERE serviceId = :serviceId ORDER BY timestamp ASC")
  fun getMessagesForService(serviceId: String): Flow<List<ChatMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: ChatMessageEntity)

  @Query("UPDATE chat_messages SET quotationStatus = :status WHERE id = :messageId")
  suspend fun updateQuotationStatus(messageId: Long, status: com.example.data.model.QuotationStatus)
}

@Dao
interface ReviewDao {
  @Query("SELECT * FROM reviews WHERE technicianId = :technicianId ORDER BY timestamp DESC")
  fun getReviewsForTechnician(technicianId: String): Flow<List<ReviewEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReview(review: ReviewEntity)
}
