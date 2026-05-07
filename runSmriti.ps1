param(
    [switch]$Logs,
    [switch]$FinalUi
)

$ErrorActionPreference = "Stop"

$Project = "C:\Umich\Projects\Smriti"
$Package = "com.smriti.clinicalscribe"
$ModelSource = "C:\Umich\Projects\Models\gemma-4-E2B-it.litertlm"
$ModelTargetName = "gemma-4-E2B-it-int4.litertlm"
$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

cd $Project

if (!(Test-Path $Adb)) {
    throw "adb not found at $Adb"
}

if ($Logs) {
    Write-Host "Showing recent Smriti crash/startup logs..."
    & $Adb logcat -d -t 300 | Select-String "FATAL EXCEPTION|AndroidRuntime|ANR|Smriti|LiteRt|RealGemma"
    exit
}

Write-Host "Checking emulator/device..."
$devices = & $Adb devices
Write-Host $devices

if (-not ($devices -match "`tdevice")) {
    throw "No emulator/device connected. Start emulator first."
}

Write-Host "Building Smriti with RealGemma submission mode..."
$gradleProps = @("-Psmriti.realGemmaSubmissionMode=true")
if ($FinalUi) {
    Write-Host "Final recording UI enabled: demo-only controls will be hidden."
    $gradleProps += "-Psmriti.finalRecordingUi=true"
}
.\gradlew.bat assembleDebug @gradleProps

Write-Host "Installing APK..."
& $Adb install -r app/build/outputs/apk/debug/app-debug.apk

Write-Host "Preparing app-private RealGemma setup..."
& $Adb shell run-as $Package mkdir -p files/models
& $Adb shell run-as $Package mkdir -p files/dev
& $Adb shell run-as $Package touch files/dev/enable_real_gemma_text_mode

Write-Host "Checking model..."
$modelCheck = ""
try {
    $modelCheck = & $Adb shell run-as $Package ls -lh "files/models/$ModelTargetName"
} catch {
    $modelCheck = ""
}

if ($modelCheck -match $ModelTargetName) {
    Write-Host "Model already present:"
    Write-Host $modelCheck
} else {
    Write-Host "Model missing. Sideloading model..."

    if (!(Test-Path $ModelSource)) {
        throw "Model not found at $ModelSource"
    }

    & $Adb push $ModelSource /data/local/tmp/gemma-4-E2B-it.litertlm
    & $Adb shell chmod 644 /data/local/tmp/gemma-4-E2B-it.litertlm
    & $Adb shell run-as $Package cp /data/local/tmp/gemma-4-E2B-it.litertlm "files/models/$ModelTargetName"
}

Write-Host "Final model/dev status:"
& $Adb shell run-as $Package ls -lh files/models
& $Adb shell run-as $Package ls -lh files/dev

Write-Host "Launching Smriti cleanly..."
& $Adb shell am force-stop $Package
& $Adb logcat -c
& $Adb shell monkey -p $Package 1

Write-Host ""
Write-Host "Smriti launched."
Write-Host "Wait for the first screen before tapping anything."
Write-Host "For final recording mode, run: .\runSmriti.ps1 -FinalUi"
Write-Host "If black screen appears, run: .\runSmriti.ps1 -Logs"
Write-Host "Do not use Android Studio Run button for filming."
