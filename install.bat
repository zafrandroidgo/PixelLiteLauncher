@echo off
setlocal
if not exist app-debug.apk (
  echo Letakkan app-debug.apk di folder ini terlebih dahulu.
  pause
  exit /b 1
)
adb install -r app-debug.apk
if errorlevel 1 (
  echo Instalasi gagal. Periksa USB debugging dan koneksi ADB.
  pause
  exit /b 1
)
echo Instalasi selesai. Tekan tombol HOME pada tablet dan pilih Pixel Lite.
pause
