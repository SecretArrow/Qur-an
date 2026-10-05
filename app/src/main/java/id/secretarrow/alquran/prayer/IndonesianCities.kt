package id.secretarrow.alquran.prayer

/** Kota besar Indonesia (fallback offline) dengan koordinat. */
data class IndonesianCity(
    val id: String,
    val name: String,
    val region: String,
    val lat: Double,
    val lng: Double
)

object IndonesianCities {
    val cities: List<IndonesianCity> =
        listOf(
            IndonesianCity("jakarta", "Jakarta", "DKI Jakarta", -6.2088, 106.8456),
            IndonesianCity("bandung", "Bandung", "Jawa Barat", -6.9175, 107.6191),
            IndonesianCity("semarang", "Semarang", "Jawa Tengah", -6.9667, 110.4167),
            IndonesianCity("surabaya", "Surabaya", "Jawa Timur", -7.2575, 112.7521),
            IndonesianCity("yogyakarta", "Yogyakarta", "DI Yogyakarta", -7.7956, 110.3695),
            IndonesianCity("medan", "Medan", "Sumatera Utara", 3.5952, 98.6722),
            IndonesianCity("palembang", "Palembang", "Sumatera Selatan", -2.9761, 104.7754),
            IndonesianCity("padang", "Padang", "Sumatera Barat", -0.9471, 100.4172),
            IndonesianCity("pekanbaru", "Pekanbaru", "Riau", 0.5071, 101.4478),
            IndonesianCity("batam", "Batam", "Kepulauan Riau", 1.1300, 104.0530),
            IndonesianCity("denpasar", "Denpasar", "Bali", -8.6705, 115.2126),
            IndonesianCity("mataram", "Mataram", "Nusa Tenggara Barat", -8.5833, 116.1167),
            IndonesianCity("kupang", "Kupang", "Nusa Tenggara Timur", -10.1772, 123.6070),
            IndonesianCity("pontianak", "Pontianak", "Kalimantan Barat", -0.0263, 109.3425),
            IndonesianCity("banjarmasin", "Banjarmasin", "Kalimantan Selatan", -3.3186, 114.5944),
            IndonesianCity("samarinda", "Samarinda", "Kalimantan Timur", -0.5019, 117.1536),
            IndonesianCity("balikpapan", "Balikpapan", "Kalimantan Timur", -1.2379, 116.8529),
            IndonesianCity("makassar", "Makassar", "Sulawesi Selatan", -5.1477, 119.4327),
            IndonesianCity("manado", "Manado", "Sulawesi Utara", 1.4748, 124.8421),
            IndonesianCity("palu", "Palu", "Sulawesi Tengah", -0.8917, 119.8707),
            IndonesianCity("kendari", "Kendari", "Sulawesi Tenggara", -3.9985, 122.5131),
            IndonesianCity("gorontalo", "Gorontalo", "Gorontalo", 0.5435, 123.0568),
            IndonesianCity("ambon", "Ambon", "Maluku", -3.6954, 128.1814),
            IndonesianCity("ternate", "Ternate", "Maluku Utara", 0.7900, 127.3840),
            IndonesianCity("jayapura", "Jayapura", "Papua", -2.5916, 140.6690),
            IndonesianCity("sorong", "Sorong", "Papua Barat", -0.8796, 131.2610),
            IndonesianCity("bungo", "Bungo", "Jambi", -1.4700, 101.8580),
            IndonesianCity("bandarlampung", "Bandar Lampung", "Lampung", -5.4292, 105.2611),
            IndonesianCity("serang", "Serang", "Banten", -6.1204, 106.1504),
            IndonesianCity("bogor", "Bogor", "Jawa Barat", -6.5950, 106.8166),
            IndonesianCity("bekasi", "Bekasi", "Jawa Barat", -6.2383, 106.9756),
            IndonesianCity("tangerang", "Tangerang", "Banten", -6.1783, 106.6319),
            IndonesianCity("surakarta", "Surakarta", "Jawa Tengah", -7.5755, 110.8243),
            IndonesianCity("malang", "Malang", "Jawa Timur", -7.9666, 112.6326),
            IndonesianCity("pekalongan", "Pekalongan", "Jawa Tengah", -6.9050, 109.6750)
        )

    fun byId(id: String): IndonesianCity = cities.firstOrNull { it.id == id } ?: cities.first()
}
