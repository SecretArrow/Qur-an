package id.secretarrow.alquran.data.repo

import id.secretarrow.alquran.data.db.AppDatabase
import id.secretarrow.alquran.data.db.BookmarkEntity
import id.secretarrow.alquran.data.db.LastReadEntity
import id.secretarrow.alquran.data.model.Bookmark
import id.secretarrow.alquran.data.model.LastRead
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepository(
    private val db: AppDatabase
) {
    fun bookmarks(): Flow<List<Bookmark>> =
        db.bookmarkDao().observeAll().map { list ->
            list.map { Bookmark(it.id, it.surah, it.ayah, it.createdAt) }
        }

    fun isBookmarked(
        surah: Int,
        ayah: Int
    ): Flow<Boolean> = db.bookmarkDao().observeExists(surah, ayah)

    suspend fun toggle(
        surah: Int,
        ayah: Int
    ): Boolean =
        if (db.bookmarkDao().exists(surah, ayah)) {
            db.bookmarkDao().delete(surah, ayah)
            false
        } else {
            db.bookmarkDao().insert(BookmarkEntity(surah = surah, ayah = ayah, createdAt = System.currentTimeMillis()))
            true
        }

    suspend fun all(): List<Bookmark> = db.bookmarkDao().all().map { Bookmark(it.id, it.surah, it.ayah, it.createdAt) }

    suspend fun restore(list: List<Bookmark>) {
        db.bookmarkDao().insertAll(list.map { BookmarkEntity(id = it.id, surah = it.surah, ayah = it.ayah, createdAt = it.createdAt) })
    }

    suspend fun clearAll() = db.bookmarkDao().deleteAll()

    fun lastRead(): Flow<LastRead?> = db.lastReadDao().observe().map { it?.let { e -> LastRead(e.surah, e.ayah, e.updatedAt) } }

    suspend fun currentLastRead(): LastRead? = db.lastReadDao().get()?.let { LastRead(it.surah, it.ayah, it.updatedAt) }

    suspend fun setLastRead(
        surah: Int,
        ayah: Int
    ) {
        db.lastReadDao().upsert(LastReadEntity(id = 1, surah = surah, ayah = ayah, updatedAt = System.currentTimeMillis()))
    }

    suspend fun restoreLastRead(lastRead: LastRead?) {
        if (lastRead != null) {
            db.lastReadDao().upsert(LastReadEntity(id = 1, surah = lastRead.surah, ayah = lastRead.ayah, updatedAt = lastRead.updatedAt))
        }
    }
}
