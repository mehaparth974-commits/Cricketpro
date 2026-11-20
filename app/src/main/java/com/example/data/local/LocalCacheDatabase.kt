package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pending_ball_sync")
data class PendingBallSyncEntity(
    @PrimaryKey val ballId: String,
    val matchId: String,
    val sequenceIndex: Int,
    val innings: Int,
    val overNumber: Int,
    val ballInOver: Int,
    val strikerName: String,
    val nonStrikerName: String,
    val bowlerName: String,
    val runsScored: Int,
    val extraType: String,
    val extraRuns: Int,
    val wicket: Boolean,
    val dismissalType: String,
    val dismissedPlayerName: String?,
    val fielderName: String?,
    val shotAngleDeg: Int,
    val shotDistanceNorm: Double,
    val powerplay: Boolean,
    val commentary: String,
    val scorerUid: String,
    val createdAtMillis: Long
)

@Dao
interface LocalCacheDao {
    @Query("SELECT * FROM pending_ball_sync ORDER BY sequenceIndex ASC")
    fun observePendingBalls(): Flow<List<PendingBallSyncEntity>>

    @Query("SELECT COUNT(*) FROM pending_ball_sync")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM pending_ball_sync WHERE matchId = :matchId ORDER BY sequenceIndex ASC")
    suspend fun getPendingForMatch(matchId: String): List<PendingBallSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPendingBall(ball: PendingBallSyncEntity)

    @Query("DELETE FROM pending_ball_sync WHERE ballId = :ballId")
    suspend fun deletePendingBall(ballId: String)

    @Query("DELETE FROM pending_ball_sync WHERE matchId = :matchId")
    suspend fun clearMatchPending(matchId: String)
}

@Database(
    entities = [PendingBallSyncEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocalCacheDatabase : RoomDatabase() {
    abstract fun localCacheDao(): LocalCacheDao

    companion object {
        @Volatile
        private var INSTANCE: LocalCacheDatabase? = null

        fun getInstance(context: Context): LocalCacheDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalCacheDatabase::class.java,
                    "cricketpro_offline_cache.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
