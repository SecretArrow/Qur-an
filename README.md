# Al-Qur'an Indonesia (Replika)

Replika open source dari aplikasi Android **Al Quran Indonesia** (com.andi.alquran.id) —
fitur, alur, dan nuansa UI/UX direplikasi sedekat mungkin namun dibangun ulang dari nol
dengan stack modern.

> Aplikasi ini proyek replikasi independen dan tidak berafiliasi dengan developer asli.
> ApplicationId berbeda (`id.secretarrow.alquran`) agar tidak menimpa/mengimitasi aplikasi asli.

## Fitur

- Menu utama bergaya referensi (Baca Qur'an, Terakhir Baca, Pencarian, Jadwal Sholat, Pengaturan)
- Surah Index + Juz Index + Bookmark (tab SURAH / JUZ / BOOKMARK)
- Layar baca: swipe antar-surah (RTL), header ornamen, basmalah, selang-seling baris ayat
- **Tajwid berwarna** (ikhfa, idgham, iqlab, izhar, qalqalah, mad)
- Rasm **Utsmani** & **IndoPak**, tulisan **latin (transliterasi)**
- Terjemahan **Kemenag-RI** dan **Tafsir Al-Jalalain Indonesia**
- Salin & bagikan ayat, bookmark, penanda bacaan terakhir
- Audio murattal **8 qori** (per-ayat, play/pause/stop/next/prev/repeat), notifikasi media,
  **audio manager** (unduh/hapus per surah)
- Pencarian ayat di terjemahan (mode **Mendekati**/**Terperinci**, hasil dikelompokkan per surah,
  kata kunci disorot merah)
- **Jadwal sholat & imsakiyah** (konvensi Kemenag: Subuh 20°, Isya 18°) + **alarm adzan**
- **Kalendar jadwal 30 hari** (tabel Tanggal, Hijriah, Imsak … Isya)
- **Arah kiblat** (kompas + sudut + jarak ke Ka'bah)
- **Kalender Hijriah** (+ koreksi -2..+2 hari)
- **Widget jadwal sholat** (gratis)
- Tema **terang/gelap**, responsif portrait & landscape, **offline penuh** (kecuali streaming/unduh audio)
- Backup & restore bookmark/terakhir baca ke file JSON

## Teknologi

Kotlin • Jetpack Compose (Material 3) • Room • DataStore • Media3/ExoPlayer (MediaSessionService)
• Adhan (waktu sholat & kiblat) • kotlinx.serialization • MVVM tanpa framework DI (build cepat).

## Struktur

```
app/src/main/assets/data/     Dataset offline (Utsmani, IndoPak, latin, Kemenag, Jalalain, metadata 114 surah)
app/src/main/java/...         Kode aplikasi (data, core, prayer, audio, widget, ui)
.github/workflows/ci.yml      CI: ktlint (+autofix), detekt, Android Lint, unit test, build, E2E emulator, auto-release
.github/workflows/autofix.yml Auto-fix format terjadwal
```

## Build via GitHub Actions (tidak perlu build lokal)

Push ke `main` otomatis menjalankan pipeline:

1. **quality** — ktlint (auto-fix + re-run untuk push ke main), resolusi dependensi,
   detekt, Android Lint, unit test, `assembleDebug`
2. **e2e** — instrumented test Compose di emulator Android API 30 (KVM)
3. **release** — setelah semua hijau: `assembleRelease` + `bundleRelease` (signed),
   buat tag `v1.0.x` + **GitHub Release** berisi APK & AAB + changelog otomatis

Secrets signing (repo Settings → Secrets): `ANDROID_KEYSTORE_BASE64`,
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
Tanpa secret, build release memakai tanda tangan debug (peringatan muncul di log).

## Sumber data & lisensi

- Teks Qur'an Utsmani & IndoPak: [Tanzil](https://tanzil.net) (via [fawazahmed0/quran-api](https://github.com/fawazahmed0/quran-api))
- Terjemahan Kemenag-RI & Tafsir Al-Jalalain (id): fawazahmed0/quran-api
- Audio murattal per-ayat: [everyayah.com](https://everyayah.com)
- Font: Amiri Quran & Noto Naskh Arabic (SIL OFL)
- Logika waktu sholat: [Adhan](https://github.com/batoulapps/adhan) (MIT)

Kode aplikasi: MIT.
