$ErrorActionPreference = "Stop"

$Project = "C:\Umich\Projects\Smriti"
$Package = "com.smriti.clinicalscribe"
$ModelSource = "C:\Umich\Projects\Models\gemma-4-E2B-it.litertlm"
$ModelTargetName = "gemma-4-E2B-it-int4.litertlm"
$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

cd $Project

Write-Host "Checking emulator..."
& $Adb devices

Write-Host "Installing Smriti with RealGemma submission mode..."
.\gradlew.bat :app:installDebug "-Psmriti.realGemmaSubmissionMode=true"

Write-Host "Preparing app-private RealGemma setup..."
& $Adb shell run-as $Package mkdir -p files/models
& $Adb shell run-as $Package mkdir -p files/dev
& $Adb shell run-as $Package touch files/dev/enable_real_gemma_text_mode

$modelCheck = & $Adb shell run-as $Package ls -lh "files/models/$ModelTargetName" 2>$null

if ($modelCheck -match "2.4G|2.3G|2.5G") {
    Write-Host "Model already present:"
    Write-Host $modelCheck
} else {
    Write-Host "Model missing or invalid. Sideloading model..."
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

Write-Host "Launching Smriti..."
& $Adb shell am force-stop $Package
& $Adb logcat -c
& $Adb shell monkey -p $Package 1

Write-Host ""
Write-Host "Smriti launched. Wait 20-30 seconds before first RealGemma call."
Write-Host "Do not use Android Studio Run button for filming unless it also uses -Psmriti.realGemmaSubmissionMode=true."