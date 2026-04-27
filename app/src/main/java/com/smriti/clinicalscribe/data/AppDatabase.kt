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
        ProtocolChunk::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun visitLogDao(): VisitLogDao
    abstract fun referralFlagDao(): ReferralFlagDao
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
}

@Dao
interface VisitLogDao {
    @Query("SELECT * FROM visit_logs WHERE patientId = :patientId ORDER BY visitDateMillis DESC")
    suspend fun getForPatient(patientId: String): List<VisitLog>

    @Query("SELECT * FROM visit_logs ORDER BY visitDateMillis DESC")
    suspend fun getAll(): List<VisitLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visitLog: VisitLog): Long

    @Query("DELETE FROM visit_logs")
    suspend fun deleteAll()
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
interface ProtocolChunkDao {
    @Query("SELECT * FROM protocol_chunks")
    suspend fun getAll(): List<ProtocolChunk>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(chunks: List<ProtocolChunk>)
}
