# Undo the Swiss home configuration. Keeps the app unless -Uninstall.
# Run from the repository root:  .\scripts\tv-restore.ps1 -Serial <TV_IP>:5555 [-Uninstall]
param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [switch]$Uninstall
)

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$adb = Join-Path $root 'toolchain\android-sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { $adb = 'adb' }
& $adb connect $Serial | Out-Null

$pkg = 'io.github.op7418.guizangtv'
$ourService = "$pkg/$pkg.SwissAccessibilityService"

Write-Host '== remove the accessibility bridge'
$current = (& $adb -s $Serial shell 'settings get secure enabled_accessibility_services').Trim()
if ($current -eq 'null') { $current = '' }
$services = @()
if ($current -ne '') { $services = $current -split ':' }
$services = $services | Where-Object { $_ -ne $ourService }
if ($services.Count -gt 0) {
    & $adb -s $Serial shell "settings put secure enabled_accessibility_services '$($services -join ':')'"
} else {
    & $adb -s $Serial shell 'settings delete secure enabled_accessibility_services'
}
& $adb -s $Serial shell "cmd notification remove_listener $pkg/$pkg.SwissRevivalListener"

Write-Host '== restore the Xiaomi home as the system home'
& $adb -s $Serial shell 'cmd package set-home-activity com.mitv.tvhome/.MainActivityUserMode'

Write-Host '== restore promo packages and the standby whitelist'
foreach ($promo in @('com.mitv.shoplugin', 'com.xiaomi.mitv.shop', 'com.xiaomi.mitv.tvpush.tvpushservice')) {
    & $adb -s $Serial shell "pm enable $promo"
}
& $adb -s $Serial shell 'settings put system my_app_recommend_count 3'
$white = (& $adb -s $Serial shell 'settings get system str_killbackgroud_pkg_white_list').Trim()
if ($white -eq 'null') { $white = '' }
$pkgs = @($white -split ',' | Where-Object { $_ -ne '' -and $_ -ne $pkg })
if ($pkgs.Count -gt 0) {
    & $adb -s $Serial shell "settings put system str_killbackgroud_pkg_white_list '$($pkgs -join ',')'"
} else {
    & $adb -s $Serial shell 'settings delete system str_killbackgroud_pkg_white_list'
}

if ($Uninstall) {
    Write-Host '== uninstall'
    & $adb -s $Serial uninstall $pkg
}

& $adb -s $Serial shell 'input keyevent KEYCODE_HOME'
Write-Host 'done. The stock Xiaomi home should be back.'
