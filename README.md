# DocFinder 2.0 — FTS + Modern UI

Upgrade dari versi awal.

## Fitur
- SQLite **FTS5** untuk pencarian full-text cepat pada seluruh isi dokumen.
- Indeks otomatis memakai trigger SQLite saat dokumen ditambah/dihapus/diubah.
- Bottom navigation Material 3 dengan dua halaman: **Cari** dan **Dokumen**.
- UI card, outlined search field, Material buttons, dan layout yang responsif.
- Upload multi-file.
- Ekstraksi teks: PDF, DOCX, TXT, CSV, MD.
- Semua data tetap lokal di perangkat.

## Build
Buka folder project dengan Android Studio, lakukan Gradle Sync, lalu:
**Build > Build APK(s)**.

Build pertama membutuhkan internet untuk mengunduh dependency Gradle/PDFBox jika belum tersedia di cache.
