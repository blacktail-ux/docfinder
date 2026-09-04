# DocFinder — build APK dari HP

Project ini sudah disiapkan untuk dibuild menggunakan GitHub Actions, jadi tidak membutuhkan PC/Android Studio.

Isi aplikasi:
- Tab Cari
- Tab Dokumen
- Import PDF, DOCX, TXT, CSV, MD
- Ekstraksi teks
- Pencarian SQLite FTS5
- Hapus dokumen
- Penyimpanan lokal di HP

Cara build dari HP:
1. Buat repository baru di GitHub.
2. Upload seluruh isi folder project ini ke repository (bukan file ZIP-nya).
3. Buka tab Actions.
4. Pilih workflow "Build DocFinder APK".
5. Tekan "Run workflow".
6. Setelah selesai, buka hasil workflow dan bagian Artifacts.
7. Download "DocFinder-APK", ekstrak ZIP, lalu install app-debug.apk di HP.

Catatan:
- APK debug ini dapat langsung di-install di Android dengan mengizinkan pemasangan dari sumber tersebut bila Android memintanya.
- Build dilakukan oleh GitHub Actions; chat ini tidak memiliki Android SDK/build runner untuk mengompilasi APK secara langsung.
