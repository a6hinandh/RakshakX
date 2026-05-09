package com.rakshakx.callanalysis.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Entity for blocked phone numbers.
 */
@Entity(tableName = "blocked_numbers")
data class BlockedNumber(
    @PrimaryKey val phoneNumber: String,
    val reason: String = "User blocked",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * DAO for blocked numbers.
 */
@Dao
interface BlockedNumberDao {
    @Insert
    suspend fun insert(number: BlockedNumber)

    @Query("SELECT * FROM blocked_numbers WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getByPhoneNumber(phoneNumber: String): BlockedNumber?

    @Query("SELECT * FROM blocked_numbers ORDER BY timestamp DESC")
    suspend fun getAllBlocked(): List<BlockedNumber>

    @Query("DELETE FROM blocked_numbers WHERE phoneNumber = :phoneNumber")
    suspend fun deleteByPhoneNumber(phoneNumber: String)
}

/**
 * Room database for blocked numbers.
 */
@Database(entities = [BlockedNumber::class], version = 1, exportSchema = false)
abstract class BlockedNumberDatabase : RoomDatabase() {
    abstract fun blockedNumberDao(): BlockedNumberDao
}

/**
 * Repository for managing blocked phone numbers.
 */
class BlockedNumbersRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        BlockedNumberDatabase::class.java,
        "blocked_numbers.db"
    ).build()

    private val dao = db.blockedNumberDao()

    suspend fun addBlockedNumber(phoneNumber: String, reason: String = "User blocked") {
        withContext(Dispatchers.IO) {
            dao.insert(BlockedNumber(phoneNumber, reason))
        }
    }

    suspend fun isBlocked(phoneNumber: String): Boolean {
        return withContext(Dispatchers.IO) {
            dao.getByPhoneNumber(phoneNumber) != null
        }
    }

    suspend fun removeBlocked(phoneNumber: String) {
        withContext(Dispatchers.IO) {
            dao.deleteByPhoneNumber(phoneNumber)
        }
    }

    suspend fun getAllBlocked(): List<BlockedNumber> {
        return withContext(Dispatchers.IO) {
            dao.getAllBlocked()
        }
    }
}

