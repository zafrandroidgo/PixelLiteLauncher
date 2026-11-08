@echo off
setlocal
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle tidak ditemukan. Buka project ini melalui Android Studio, atau pasang Gradle 8.7+.
  pause
  exit /b 1
)
gradle assembleDebug
if errorlevel 1 exit /b 1
echo APK: app\build\outputs\apk\debug\app-debug.apk
pause
