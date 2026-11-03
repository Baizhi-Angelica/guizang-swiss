# 瑞士桌面（Swiss Home）

2026-11-03 新增，用于客厅的小米电视 **S Pro 65 2025（MiTV-MFFU0）**（HyperOS 2.0.8.0.UFFMATV，Android 14）。
目标是「只有三个 HDMI 和一个退出按钮」的极简电视桌面，而不是原项目的列表 / Dock / 磁贴三套布局。

实现是一个独立入口 `SwissHomeActivity`：整屏 Canvas 绘制，无 AndroidX、无网络、无壁纸素材。
原来的 `FrameHomeActivity`（列表 / Dock / 磁贴 / 设置 / 天气 / Codex）仍然保留在 APK 里，
但不再有桌面入口，只供回退和对照使用。

## 界面

| 元素 | 说明 |
| --- | --- |
| 左上 | 红色方块 + **小米电视**（粗体）+ 信号源选择（灰） |
| 左侧 | 超大时钟（冒号是瑞士红），下方日期与星期 |
| 右侧 | 01 Switch / HDMI 1，02 有线电视 / HDMI 2，03 Apple TV / HDMI 3 |
| 右下 | 小方框「退出」，回到小米原桌面 |
| 底部 | `↑ ↓ 选择   OK 进入   1 2 3 直达` |

- 遥控器：上 / 下在三个信号源之间移动，第三个再往下进入「退出」，向上回到信号源；
  退出框按左回到第三个信号源。数字键 1 / 2 / 3 直接切到对应 HDMI。
- 焦点行整行填充瑞士红（`#D92318`），文字反白，填充从左向右扫过；未聚焦行只有细线。
- 入场：顶部细线拉出，时钟、日期、三行信号源依次淡入。
- 触屏也可点按（方便模拟器或手机调试）。

### 日夜配色

默认**跟随日出日落**：白天米色（`#F4EFE2 → #E9E2D1`），日落后深色（`#131315 → #0A0A0B`）。
日出日落用 NOAA 太阳方程在本地计算，默认坐标是**上海市中心**（31.2304N, 121.4737E），
按设备时区和当天日期换算；改城市只需改 `SwissSun.LATITUDE / LONGITUDE`。
每分钟检查一次，越过日出或日落会自动重建界面。

临时切换（便于对比，设置会保存，但会被下一次手动切换覆盖）：

```sh
# 跟随日出日落（默认）
adb shell am broadcast -n io.github.op7418.guizangtv/.SwissThemeReceiver \
  -a io.github.op7418.guizangtv.SWISS_THEME --ei theme 2
# 强制米色 / 强制深色
adb shell am broadcast -n io.github.op7418.guizangtv/.SwissThemeReceiver \
  -a io.github.op7418.guizangtv.SWISS_THEME --ei theme 0
adb shell am broadcast -n io.github.op7418.guizangtv/.SwissThemeReceiver \
  -a io.github.op7418.guizangtv.SWISS_THEME --ei theme 1
```

## 信号源

直接调用小米信号源播放器，与旧版 `XiaomiControls` 相同，但不再限制机型白名单：

```
组件  com.xiaomi.mitv.tvplayer/.ExternalSourceActivity
action com.xiaomi.mitv.tvplayer.EXTSRC_PLAY
extra  input = 23 / 24 / 25
```

在本机实测：`23 → HDMI 1`、`24 → HDMI 2`、`25 → HDMI 3`（Apple TV，4K 60Hz + Dolby Vision）。
组件不存在、未导出或调用异常时，回退到系统信号源面板（`INPUTSOURCE_POPUP` 广播），
再失败才提示「暂时无法切换信号源」。不会黑屏，也不会修改系统设置。

## 退出

右下角「退出」通过公开的 `com.mitv.tvhome/.MainActivity` 打开小米原桌面。
小米把真正的桌面组件 `MainActivityUserMode` 设为 HOME 且禁止 shell 修改，所以退出走的是公开入口，
显示效果与原桌面一致。

## 开机与 Home 键（重点）

这台固件对第三方桌面是**锁死**的，实测结论：

1. `android.intent.category.HOME` 声明正常，原生「默认主屏幕应用」里也能选中本应用
   （`cmd package set-home-activity` 后 RoleManager 的 HOME holder 就是本应用）。
2. 但 `RootWindowContainer.resolveHomeActivity()` 是小米改过的实现：它只读 `boot_auto_play`
   和 `tv_user_setup_complete`，Home 键最终启动的仍是 `com.mitv.tvhome/.MainActivityUserMode`。
3. `pm disable-user` 改不动桌面的组件状态（`SecurityException: Shell cannot change component state`），
   非 root 无法禁用或替换。
4. 普通 `BOOT_COMPLETED` 也不可用：完整重启时系统会把 /data 下的第三方应用置为 stopped，
   `BOOT_COMPLETED` 不会投递给 stopped 应用。

因此采用**无障碍桥** `SwissAccessibilityService`：

- 只监听 `TYPE_WINDOW_STATE_CHANGED`，只读事件的**包名和类名**（`canRetrieveWindowContent=false`，
  不读屏幕内容）；
- 当 `com.mitv.tvhome` 的 `MainActivityUserMode` / `MainActivity` 出现时，350ms 后把瑞士桌面拉到前台；
- 服务在开机时由系统绑定（本机实测 18.5 秒，一般 20–40 秒），绑定后如果仍在开机后 5 分钟内，会再主动拉两次，
  覆盖「小米桌面已经先画出来」的情况；- 退出按钮会写一次性 hand-off 标记，让小米桌面这一次不被抢回；之后再按 Home 会回到瑞士桌面；
- 如果用户关闭了无障碍服务，`SwissBootReceiver` 会退回「开机 15 秒后拉起桌面」的备用路径
  （需要 `SYSTEM_ALERT_WINDOW` app-op）。

启用（已在本机完成）：

```sh
adb shell settings put secure enabled_accessibility_services \
  "com.xiaomi.voicecontrol/com.xiaomi.voicecontrol.VoiceAccessibilityService:io.github.op7418.guizangtv/io.github.op7418.guizangtv.SwissAccessibilityService"
adb shell settings put secure accessibility_enabled 1
```

注意保留原有的 `com.xiaomi.voicecontrol` 语音服务，只追加我们这一项。

已知限制：

- 从其他应用按 Home 时，小米桌面会先出现约 0.3–0.5 秒，再切到瑞士桌面；
- 如果已经在小米桌面上再按 Home，因为没有新的窗口事件，不会跳回瑞士桌面
  （从小米桌面打开任意应用后按 Home 即可回来，或从「我的应用」打开「归藏电视」）；
- 小米桌面仍然保留，用于偶尔进入应用商店或系统功能；本项目不删除、不禁用任何桌面组件。

## 保留的小米功能

| 设置 | 值 | 说明 |
| --- | --- | --- |
| `tvapp_one_touch_play` | 1 | 一触即播：接入 / 开启信号源设备时自动切换，**未改动** |
| `mitv.settings.db.video.auto_switch` | 1 | 视频自动切换，**未改动** |
| `boot_auto_play` | 0 | 用户手动关闭了「开机直接播放信号源」，否则开机会被信号源抢走 |
| CEC（`hdmi_control_*`） | 原值 | 未改动 |

## 广告与预装清理

只做可逆的禁用，不卸载系统应用，不删除数据：

| 包 / 设置 | 操作 | 作用 |
| --- | --- | --- |
| `com.mitv.shoplugin` | `pm disable-user` | 桌面商城插件 |
| `com.xiaomi.mitv.shop` | `pm disable-user` | 电视商城 |
| `com.xiaomi.mitv.tvpush.tvpushservice` | `pm disable-user` | 推送广告服务 |
| `my_app_recommend_count`（system） | `0` | 「我的应用」推荐位数量 |
| `boot_ad` / `boot_ad_sound_enable` / `boot_ad_switch_show`（system） | 原本就是 0 | 开机广告开关 |
| `network_recommendations_enabled`（global） | 原本就是 0 | 网络推荐 |
| `screensaver_enabled`（secure） | 原本就是 0 | 屏保 |

无法在不 root 的前提下清掉的部分：小米桌面首页的「热播 / 订购信息」内容位、应用商店自身的推荐。
这些只在主动退出到小米桌面时才会看到，瑞士桌面本身不联网、不显示广告。

> 实测持久性：`pm disable-user` 的包状态和 `secure` 里的无障碍设置在完整重启后保留；
> `system` 命名空间的 `start_3rd_app` / `my_app_recommend_count` 会被小米设置应用还原成默认值。
> 这两项只影响可选的备用开机路径和桌面推荐位，`scripts/tv-setup.ps1` 可以随时重新应用。

回滚：

```sh
adb shell pm enable com.mitv.shoplugin
adb shell pm enable com.xiaomi.mitv.shop
adb shell pm enable com.xiaomi.mitv.tvpush.tvpushservice
adb shell settings put system my_app_recommend_count 3
```

## 构建与安装

工具链放在本目录的 `toolchain/`（JDK 17 使用系统已安装的 `C:\Program Files\Java\jdk-17.0.4.1`）：

```
toolchain/
  android-sdk/              platform 34 + build-tools 34.0.0 + platform-tools
  gradle-8.9/               Gradle 8.9
  gradle-home/              独立的 GRADLE_USER_HOME（含代理配置）
  keystore/guizang-debug.jks 本机调试签名
```

```powershell
# 仓库根目录
.\build.ps1            # 只打包
.\build.ps1 -Test      # 打包 + 单元测试
```

产物：`guizang-tv/app/build/outputs/apk/debug/app-debug.apk`（包名 `io.github.op7418.guizangtv`，
versionCode 7 / `0.5.0-swiss`，使用本机调试签名）。

安装：

```powershell
adb connect <电视IP>:5555
adb install -r guizang-tv\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n io.github.op7418.guizangtv/.SwissHomeActivity
```

完整的一键配置（安装、无障碍、开机项、广告清理）见 `scripts/tv-setup.ps1`；
恢复系统原状见 `scripts/tv-restore.ps1`。

## 从旧布局切换回来

旧布局仍在 APK 内：`adb shell am start -n io.github.op7418.guizangtv/.FrameHomeActivity`。
它会打开原来的列表 / Dock / 磁贴界面（设置、天气、Codex 都还在）。恢复桌面入口需要重新构建 manifest。
