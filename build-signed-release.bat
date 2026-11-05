@echo off
setlocal

rem Runs the normal Gradle build checks and builds all local artifacts:
rem - Google Play and F-Droid handheld and Wear debug APKs
rem - signed Google Play and F-Droid handheld and Wear release APKs
rem - signed Google Play and F-Droid handheld and Wear release Android App Bundles

set "ANDROID_KEYSTORE_PATH=release-keystore.jks"
set /p "ANDROID_KEYSTORE_PASSWORD=Enter keystore password: "
set "ANDROID_KEY_ALIAS=release"
set "ANDROID_KEY_PASSWORD=%ANDROID_KEYSTORE_PASSWORD%"

cls
cd /d "%~dp0"

if not defined ANDROID_KEYSTORE_PATH goto missing_signing_env
if not defined ANDROID_KEYSTORE_PASSWORD goto missing_signing_env
if not defined ANDROID_KEY_ALIAS goto missing_signing_env
if not defined ANDROID_KEY_PASSWORD goto missing_signing_env

for %%I in ("%ANDROID_KEYSTORE_PATH%") do set "ANDROID_KEYSTORE_PATH=%%~fI"

if not exist "%ANDROID_KEYSTORE_PATH%" (
    echo ERROR: ANDROID_KEYSTORE_PATH does not exist:
    echo   %ANDROID_KEYSTORE_PATH%
    pause
    exit /b 1
)

if not exist "%~dp0gradlew.bat" (
    echo ERROR: gradlew.bat was not found next to this script.
    pause
    exit /b 1
)

echo Running checks, building APKs, and building signed release bundles...
call "%~dp0gradlew.bat" clean build lintFdroidHandheldDebug lintFdroidWearDebug lintGplayHandheldDebug lintGplayWearDebug bundleGplayHandheldRelease bundleGplayWearRelease bundleFdroidHandheldRelease bundleFdroidWearRelease
if errorlevel 1 (
    echo.
    echo Build failed.
    pause
    exit /b %errorlevel%
)

echo.
echo Build complete.
echo APKs: %~dp0app\build\outputs\apk\
echo AABs: %~dp0app\build\outputs\bundle\
pause
exit /b 0

:missing_signing_env
echo ERROR: Required signing environment variables are missing.
echo.
echo Required variables:
echo   ANDROID_KEYSTORE_PATH
echo   ANDROID_KEYSTORE_PASSWORD
echo   ANDROID_KEY_ALIAS
echo   ANDROID_KEY_PASSWORD
pause
exit /b 1
