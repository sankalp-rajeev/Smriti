package com.smriti.clinicalscribe.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smriti.clinicalscribe.rag.ProtocolChunk

@Database(
    entities = [
        Patient::class,
        VisitLog::class,
        ReferralFlag::class,
        FollowUpTask::class,
        ProtocolChunk::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun visitLogDao(): VisitLogDao
    abstract fun referralFlagDao(): ReferralFlagDao
    abstract fun followUpTaskDao(): FollowUpTaskDao
    abstract fun protocolChunkDao(): ProtocolChunkDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smriti.db"
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY name")
    suspend fun getAll(): List<Patient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(patients: List<Patient>)

    @Query("DELETE FROM patients")
    suspend fun deleteAll()
}

@Dao
interface VisitLogDao {
    @Query("SELECT * FROM visit_logs WHERE patientId = :patientId ORDER BY visitDateMillis DESC")
    suspend fun getForPatient(patientId: String): List<VisitLog>

    @Query("SELECT * FROM visit_logs ORDER BY visitDateMillis DESC")
    suspend fun getAll(): List<VisitLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visitLog: VisitLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(visitLogs: List<VisitLog>)

    @Query("DELETE FROM visit_logs")
    suspend fun deleteAll()

    @Query("DELETE FROM visit_logs WHERE patientId IN (:patientIds)")
    suspend fun deleteForPatients(patientIds: List<String>)

    @Query("UPDATE visit_logs SET followUpCompleted = :completed WHERE id = :visitId")
    suspend fun updateFollowUpCompleted(visitId: Long, completed: Boolean)
}

@Dao
interface ReferralFlagDao {
    @Query("SELECT * FROM referral_flags ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<ReferralFlag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(referralFlag: ReferralFlag): Long

    @Query("DELETE FROM referral_flags")
    suspend fun deleteAll()
}

@Dao
interface FollowUpTaskDao {
    @Query("SELECT * FROM follow_up_tasks WHERE patientId = :patientId AND status IN (:activeStatuses) ORDER BY dueDateMillis ASC")
    suspend fun getOpenForPatient(
        patientId: String,
        activeStatuses: List<String>
    ): List<FollowUpTask>

    @Query("SELECT * FROM follow_up_tasks WHERE status IN (:activeStatuses) ORDER BY dueDateMillis ASC")
    suspend fun getAllOpen(
        activeStatuses: List<String>
    ): List<FollowUpTask>

    @Query("SELECT * FROM follow_up_tasks ORDER BY dueDateMillis ASC")
    suspend fun getAll(): List<FollowUpTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: FollowUpTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<FollowUpTask>)

    @Query("UPDATE follow_up_tasks SET status = :status, completedAtMillis = :completedAtMillis, updatedAtMillis = :updatedAtMillis WHERE id = :taskId")
    suspend fun markCompleted(
        taskId: String,
        status: String,
        completedAtMillis: Long,
        updatedAtMillis: Long
    )

    @Query("UPDATE follow_up_tasks SET dueDateMillis = :dueDateMillis, reason = :reason, status = :status, completedAtMillis = NULL, updatedAtMillis = :updatedAtMillis WHERE id = :taskId")
    suspend fun reschedule(
        taskId: String,
        dueDateMillis: Long,
        reason: String,
        status: String,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM follow_up_tasks")
    suspend fun deleteAll()

    @Query("DELETE FROM follow_up_tasks WHERE source = :source")
    suspend fun deleteBySource(source: String)
}

@Dao
interface ProtocolChunkDao {
    @Query("SELECT * FROM protocol_chunks")
    suspend fun getAll(): List<ProtocolChunk>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(chunks: List<ProtocolChunk>)
}
