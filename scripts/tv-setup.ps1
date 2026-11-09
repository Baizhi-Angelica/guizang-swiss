# Install and configure the Swiss home over adb.
# Run from the repository root:  .\scripts\tv-setup.ps1 -Serial <TV_IP>:5555
param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [string]$Apk = '',
    [switch]$SkipAds
)

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$adb = Join-Path $root 'toolchain\android-sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { $adb = 'adb' }
if (-not $Apk) { $Apk = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk' }
if (-not (Test-Path $Apk)) { throw "APK not found: $Apk (run gradle :app:assembleDebug first)" }

$pkg = 'io.github.op7418.guizangtv'
$ourService = "$pkg/$pkg.SwissAccessibilityService"
$ourListener = "$pkg/$pkg.SwissRevivalListener"

Write-Host "== connect $Serial"
& $adb connect $Serial | Out-Null
if (-not ((& $adb devices) -match [regex]::Escape($Serial))) { throw "TV not connected: $Serial" }

Write-Host "== install $Apk"
& $adb -s $Serial install -r $Apk
if ($LASTEXITCODE -ne 0) { throw 'install failed (signature conflict? uninstall the old build first)' }

Write-Host '== privileged grants (lets the app repair itself)'
& $adb -s $Serial shell "pm grant $pkg android.permission.WRITE_SECURE_SETTINGS"
& $adb -s $Serial shell "appops set $pkg WRITE_SETTINGS allow"
& $adb -s $Serial shell "appops set $pkg SYSTEM_ALERT_WINDOW allow"

Write-Host '== accessibility bridge'
$current = (& $adb -s $Serial shell 'settings get secure enabled_accessibility_services').Trim()
if ($current -eq 'null') { $current = '' }
$services = @()
if ($current -ne '') { $services = $current -split ':' }
if ($services -notcontains $ourService) { $services += $ourService }
& $adb -s $Serial shell "settings put secure enabled_accessibility_services '$($services -join ':')'"
& $adb -s $Serial shell 'settings put secure accessibility_enabled 1'

Write-Host '== revival listener (second boot entry point)'
& $adb -s $Serial shell "cmd notification allow_listener $ourListener"

Write-Host '== standby whitelist (private setting, adb only)'
$white = (& $adb -s $Serial shell 'settings get system str_killbackgroud_pkg_white_list').Trim()
if ($white -eq 'null') { $white = '' }
$pkgs = @()
if ($white -ne '') { $pkgs = $white -split ',' }
if ($pkgs -notcontains $pkg) { $pkgs += $pkg }
& $adb -s $Serial shell "settings put system str_killbackgroud_pkg_white_list '$($pkgs -join ',')'"

if (-not $SkipAds) {
    Write-Host '== disable promo packages (reversible)'
    foreach ($promo in @('com.mitv.shoplugin', 'com.xiaomi.mitv.shop', 'com.xiaomi.mitv.tvpush.tvpushservice')) {
        & $adb -s $Serial shell "pm disable-user --user 0 $promo"
    }
    & $adb -s $Serial shell 'settings put system my_app_recommend_count 0'
}

Write-Host '== launch'
& $adb -s $Serial shell "am start -n $pkg/.SwissHomeActivity"
Write-Host 'done. Reboot once to verify the boot path.'
