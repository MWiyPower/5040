package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "call_history")
data class CallRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val timestamp: Long,
    val durationSeconds: Int,
    val type: String // "OUTGOING", "INCOMING", "MISSED"
)

@Dao
interface CallRecordDao {
    @Query("SELECT * FROM call_history ORDER BY timestamp DESC")
    fun getAllCallRecords(): Flow<List<CallRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecord(record: CallRecord)

    @Query("DELETE FROM call_history")
    suspend fun clearHistory()
}

@Database(entities = [CallRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callRecordDao(): CallRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "call_history_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class CallRepository(private val callRecordDao: CallRecordDao) {
    val allCallRecords: Flow<List<CallRecord>> = callRecordDao.getAllCallRecords()

    suspend fun insert(record: CallRecord) {
        callRecordDao.insertCallRecord(record)
    }

    suspend fun clearHistory() {
        callRecordDao.clearHistory()
    }
}
