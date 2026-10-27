$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to a JDK 17+ directory' }
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
if (-not $env:ANDROID_HOME) { throw 'Set ANDROID_HOME to the Android SDK directory' }
if (-not $env:RELEASE_KS_PASS) { throw 'Set RELEASE_KS_PASS for the release keystore' }
$tools = Join-Path $env:ANDROID_HOME 'build-tools\34.0.0'
$platform = Join-Path $env:ANDROID_HOME 'platforms\android-34\android.jar'
$ksPass = $env:RELEASE_KS_PASS
Set-Location $PSScriptRoot
function Check-Exit { if ($LASTEXITCODE -ne 0) { throw "Build command failed: $LASTEXITCODE" } }
New-Item -ItemType Directory -Force 'build\classes','build\dex','dist' | Out-Null
$sources = @(
    'src/cn/gct/demo/Ui.java',
'src/cn/gct/demo/LineIconView.java',
    'src/cn/gct/demo/TermCalendar.java',
    'src/cn/gct/demo/CourseData.java',
    'src/cn/gct/demo/HoursData.java',
    'src/cn/gct/demo/PeriodTimes.java',
    'src/cn/gct/demo/CourseParser.java',
    'src/cn/gct/demo/WeekGrid.java',
    'src/cn/gct/demo/CourseDetailDialog.java',
    'src/cn/gct/demo/MainActivity.java'
)
& "$env:JAVA_HOME\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -bootclasspath "$platform;$tools\core-lambda-stubs.jar" -d build/classes @sources
Check-Exit
& "$env:JAVA_HOME\bin\jar.exe" cf build/classes.jar -C build/classes .
Check-Exit
& "$tools\d8.bat" --lib $platform --min-api 26 --output build/dex build/classes.jar
Check-Exit
& "$tools\aapt.exe" package -f -M AndroidManifest.xml -S res -A assets -I $platform -F build/base.apk
Check-Exit
Copy-Item build/base.apk build/unsigned.apk -Force
Push-Location build/dex
& "$tools\aapt.exe" add ../unsigned.apk classes.dex
Check-Exit
Pop-Location
& "$tools\zipalign.exe" -f 4 build/unsigned.apk build/aligned.apk
Check-Exit
if (-not (Test-Path build/release.keystore)) {
    & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -keystore build/release.keystore -storepass $ksPass -keypass $ksPass -alias guangchengtong -dname 'CN=Guangcheng Tong,O=Guangcheng Tong,C=CN' -keyalg RSA -keysize 4096 -validity 10000
    Check-Exit
}
& "$tools\apksigner.bat" sign --ks build/release.keystore --ks-key-alias guangchengtong --ks-pass "pass:$ksPass" --key-pass "pass:$ksPass" --out dist/guangcheng-1.0.1.apk build/aligned.apk
Check-Exit
& "$tools\apksigner.bat" verify --verbose dist/guangcheng-1.0.1.apk
Check-Exit
Write-Output 'APK ready: dist/guangcheng-1.0.1.apk'
