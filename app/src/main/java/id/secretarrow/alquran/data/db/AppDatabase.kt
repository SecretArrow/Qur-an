package id.secretarrow.alquran.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val surah: Int,
    val ayah: Int,
    val createdAt: Long
)

@Entity(tableName = "last_read")
data class LastReadEntity(
    @PrimaryKey val id: Int = 1,
    val surah: Int,
    val ayah: Int,
    val updatedAt: Long
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY surah ASC, ayah ASC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE surah = :surah AND ayah = :ayah)")
    fun observeExists(
        surah: Int,
        ayah: Int
    ): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE surah = :surah AND ayah = :ayah)")
    suspend fun exists(
        surah: Int,
        ayah: Int
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE surah = :surah AND ayah = :ayah")
    suspend fun delete(
        surah: Int,
        ayah: Int
    )

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()

    @Query("SELECT * FROM bookmarks")
    suspend fun all(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<BookmarkEntity>)
}

@Dao
interface LastReadDao {
    @Query("SELECT * FROM last_read WHERE id = 1")
    fun observe(): Flow<LastReadEntity?>

    @Query("SELECT * FROM last_read WHERE id = 1")
    suspend fun get(): LastReadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LastReadEntity)

    @Query("DELETE FROM last_read")
    suspend fun clear()
}

@Database(
    entities = [BookmarkEntity::class, LastReadEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    abstract fun lastReadDao(): LastReadDao

    companion object {
        const val NAME = "alquran.db"
    }
}
