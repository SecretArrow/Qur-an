package id.secretarrow.alquran.data.local

/** Katalog 8 qori (murattal) dengan folder everyayah.com. */
data class Reciter(
    val id: Int,
    val name: String,
    val folder: String
)

object ReciterCatalog {
    val reciters: List<Reciter> =
        listOf(
            Reciter(1, "Mishary Rashid Alafasy", "Alafasy_128kbps"),
            Reciter(2, "Abdurrahman As-Sudais", "Abdurrahmaan_As-Sudais_192kbps"),
            Reciter(3, "Abdul Basit (Murattal)", "Abdul_Basit_Murattal_192kbps"),
            Reciter(4, "Mahmoud Khalil Al-Husary", "Husary_128kbps"),
            Reciter(5, "Mohamed Siddiq Al-Minshawi", "Minshawy_Murattal_128kbps"),
            Reciter(6, "Saad Al-Ghamdi", "Ghamadi_40kbps"),
            Reciter(7, "Ali Al-Hudhaify", "Hudhaify_128kbps"),
            Reciter(8, "Muhammad Ayyub", "Muhammad_Ayyoub_128kbps")
        )

    const val BASE_URL = "https://everyayah.com/data/"

    fun byId(id: Int): Reciter = reciters.firstOrNull { it.id == id } ?: reciters.first()

    /** URL mp3 per-ayat, contoh: 002255.mp3 */
    fun ayahUrl(
        reciter: Reciter,
        surah: Int,
        ayah: Int
    ): String = BASE_URL + reciter.folder + "/" + "%03d%03d.mp3".format(surah, ayah)
}
