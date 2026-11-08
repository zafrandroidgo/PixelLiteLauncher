# Pixel Lite Launcher — Samsung Galaxy Tab 3 SM-T211

Project launcher Android native tanpa library pihak ketiga, minimum Android 4.4.2 (API 19).

## Fitur
- Launcher HOME dengan jam dan tanggal.
- Tampilan dark bergaya Pixel.
- Grid aplikasi otomatis dari aplikasi yang terpasang.
- Tanpa iklan, analytics, layanan background, atau permission tambahan.
- Tidak memakai AndroidX/AppCompat.
- Minimum SDK 19; target SDK 28.

## Membuat APK tanpa Android Studio (GitHub Actions)
Project ini sudah memiliki workflow GitHub Actions. Kompilasi dilakukan oleh server GitHub, bukan komputer atau tablet.

1. Login atau buat akun di https://github.com.
2. Buat repository baru, misalnya `PixelLiteLauncher`.
3. Ekstrak ZIP ini di komputer.
4. Buka repository baru di GitHub, pilih **Add file > Upload files**.
5. Unggah seluruh isi folder `PixelLiteLauncher` (termasuk folder `.github`; aktifkan tampilan hidden files jika perlu), lalu klik **Commit changes**.
6. Buka tab **Actions** pada repository.
7. Pilih workflow **Build Pixel Lite APK**, lalu klik **Run workflow**. Jika workflow sudah berjalan otomatis setelah commit, buka run terbaru.
8. Setelah status selesai (hijau), buka hasil run dan unduh artifact **PixelLiteLauncher-debug-apk**.
9. Ekstrak artifact ZIP. Di dalamnya ada `app-debug.apk`.

## Instal ke tablet
Hubungkan tablet dengan USB debugging aktif. Dari folder platform-tools di Windows jalankan:

```bat
adb install -r app-debug.apk
```

Tekan tombol Home dan pilih **Pixel Lite**, lalu pilih **Always/Selalu** jika ingin menjadikannya default.

## Kembali ke launcher Samsung
Jangan nonaktifkan launcher Samsung sebelum Pixel Lite diuji. Untuk mengaktifkannya kembali:

```bat
adb shell su -c "pm enable com.sec.android.app.launcher"
```

Kemudian pilih launcher Samsung melalui pengaturan Home/default launcher.

## Catatan
- Source ini ditujukan untuk Android 4.4.2 / API 19. Build dilakukan dengan compile SDK 35 dan minimum SDK 19.
- APK hasil GitHub Actions adalah debug APK untuk instalasi pribadi, bukan rilis Play Store.
- Belum diuji pada perangkat fisik; lakukan pengujian sebelum menonaktifkan launcher bawaan.
