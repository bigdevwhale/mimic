package app.mimic.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY added")
    fun all(): Flow<List<Game>>

    @Query("SELECT * FROM games ORDER BY added")
    suspend fun list(): List<Game>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun get(id: Long): Game?

    @Insert
    suspend fun insert(g: Game): Long

    @Insert
    suspend fun insertAll(g: List<Game>)

    @Update
    suspend fun update(g: Game)

    @Delete
    suspend fun delete(g: Game)

    @Query("SELECT COUNT(*) FROM games")
    suspend fun count(): Int

    @Query("SELECT * FROM sessions ORDER BY at DESC")
    fun sessions(): Flow<List<Session>>

    @Insert
    suspend fun insertSession(s: Session): Long

    @Delete
    suspend fun deleteSession(s: Session)

    @Query("DELETE FROM sessions WHERE game = :id")
    suspend fun deleteSessionsOf(id: Long)

    @Query("SELECT * FROM sessions WHERE game = :id")
    suspend fun sessionsOf(id: Long): List<Session>

    @Query("SELECT * FROM styles ORDER BY name")
    fun styles(): Flow<List<Style>>

    @Insert
    suspend fun insertStyle(s: Style): Long

    @Query("DELETE FROM styles WHERE id IN (:ids)")
    suspend fun deleteStyles(ids: List<Long>)

    @Query("SELECT * FROM styles WHERE parentStyle = :id")
    suspend fun subStyles(id: Long): List<Style>

    @Query("UPDATE games SET style = NULL WHERE style IN (:ids)")
    suspend fun clearStyle(ids: List<Long>)
}

@Database(entities = [Game::class, Session::class, Style::class], version = 4, exportSchema = true, autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)])
abstract class MimicDb : RoomDatabase() {
    abstract fun games(): GameDao

    companion object {
        fun create(context: Context): MimicDb = Room.databaseBuilder(context, MimicDb::class.java, "mimic.db").build()
    }
}
