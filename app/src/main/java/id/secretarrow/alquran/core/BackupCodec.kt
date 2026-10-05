package id.secretarrow.alquran.core

import id.secretarrow.alquran.data.model.Bookmark
import id.secretarrow.alquran.data.model.LastRead
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Kodek backup/restore bookmark & penanda bacaan terakhir ke/dari file JSON. */
object BackupCodec {
    @Serializable
    data class BackupBookmark(
        val s: Int,
        val a: Int,
        val t: Long
    )

    @Serializable
    data class BackupLastRead(
        val s: Int,
        val a: Int,
        val t: Long
    )

    @Serializable
    data class BackupFile(
        val version: Int = 1,
        val exportedAt: Long = 0L,
        val bookmarks: List<BackupBookmark> = emptyList(),
        val lastRead: BackupLastRead? = null
    )

    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    fun encode(
        bookmarks: List<Bookmark>,
        lastRead: LastRead?,
        now: Long
    ): String {
        val file =
            BackupFile(
                version = 1,
                exportedAt = now,
                bookmarks = bookmarks.map { BackupBookmark(it.surah, it.ayah, it.createdAt) },
                lastRead = lastRead?.let { BackupLastRead(it.surah, it.ayah, it.updatedAt) }
            )
        return json.encodeToString(file)
    }

    fun decode(text: String): Result<Pair<List<Bookmark>, LastRead?>> =
        runCatching {
            val parsed = json.decodeFromString<BackupFile>(text)
            val bookmarks = parsed.bookmarks.map { Bookmark(surah = it.s, ayah = it.a, createdAt = it.t) }
            val lastRead = parsed.lastRead?.let { LastRead(it.s, it.a, it.t) }
            bookmarks to lastRead
        }
}
