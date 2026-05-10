package com.security.rakshakx.web.storage

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "threats")
data class ThreatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val domain: String,
    val level: String,
    val action: String,
    val reasons: String,
    val fraudScore: Int,
    val fraudCategory: String,
    val recommendedAction: String,
    val blockReason: String,
    val visibleSignals: String,
    val correlationData: String,
    val browserPackage: String,
    val url: String,
    val destinationIp: String,
    val sniHost: String
)

@Entity(tableName = "browser_sessions")
data class BrowserSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val browserPackage: String,
    val url: String,
    val pageTitle: String,
    val visibleText: String,
    val passwordFieldDetected: Boolean,
    val otpFieldDetected: Boolean,
    val emailFieldDetected: Boolean,
    val paymentFieldDetected: Boolean
)

@Entity(tableName = "vpn_traffic")
data class VpnTrafficEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val protocol: String,
    val sourceIp: String,
    val destinationIp: String,
    val sourcePort: Int,
    val destinationPort: Int,
    val dnsQuery: String,
    val sniHost: String,
    val redirectChain: String
)

@Dao
interface ThreatDao {
    @Insert
    suspend fun insert(entity: ThreatEntity)

    @Query("SELECT * FROM threats ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ThreatEntity>
}

@Dao
interface BrowserSessionDao {
    @Insert
    suspend fun insert(entity: BrowserSessionEntity)

    @Query("SELECT * FROM browser_sessions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<BrowserSessionEntity>
}

@Dao
interface VpnTrafficDao {
    @Insert
    suspend fun insert(entity: VpnTrafficEntity)

    @Query("SELECT * FROM vpn_traffic ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<VpnTrafficEntity>
}

@Database(entities = [ThreatEntity::class, BrowserSessionEntity::class, VpnTrafficEntity::class], version = 4)
abstract class ThreatDatabase : RoomDatabase() {
    abstract fun threatDao(): ThreatDao
    abstract fun browserSessionDao(): BrowserSessionDao
    abstract fun vpnTrafficDao(): VpnTrafficDao

    companion object {
        @Volatile
        private var instance: ThreatDatabase? = null

        fun getInstance(context: Context): ThreatDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): ThreatDatabase {
            System.loadLibrary("sqlcipher")
            val passphrase = CryptoKeyManager.getOrCreateDatabasePassphrase(context)
            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(context, ThreatDatabase::class.java, "rakshakx_threats.db")
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS browser_sessions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "browserPackage TEXT NOT NULL, " +
                        "url TEXT NOT NULL, " +
                        "pageTitle TEXT NOT NULL, " +
                        "visibleText TEXT NOT NULL, " +
                        "passwordFieldDetected INTEGER NOT NULL, " +
                        "otpFieldDetected INTEGER NOT NULL, " +
                        "emailFieldDetected INTEGER NOT NULL, " +
                        "paymentFieldDetected INTEGER NOT NULL"
                        + ")"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS vpn_traffic (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "protocol TEXT NOT NULL, " +
                        "sourceIp TEXT NOT NULL, " +
                        "destinationIp TEXT NOT NULL, " +
                        "sourcePort INTEGER NOT NULL, " +
                        "destinationPort INTEGER NOT NULL, " +
                        "dnsQuery TEXT NOT NULL, " +
                        "sniHost TEXT NOT NULL, " +
                        "redirectChain TEXT NOT NULL"
                        + ")"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE threats ADD COLUMN fraudScore INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE threats ADD COLUMN fraudCategory TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE threats ADD COLUMN recommendedAction TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE threats ADD COLUMN blockReason TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE threats ADD COLUMN visibleSignals TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE threats ADD COLUMN correlationData TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
