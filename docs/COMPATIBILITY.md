# 兼容性与验证边界

## 已验证配置

| 项目 | 配置 |
| --- | --- |
| 设备型号 | MiTV-MTSU0 |
| 机型名称 | 小米电视 S55 Mini LED 2026 至尊版 |
| Android | 14 / API 34 |
| 固件 | OS3.0.119.0.UTSMATV |
| 桌面渲染尺寸 | 1920 × 1080，density 320 |
| 最低 API | 26（Android 8），仅编译要求，不代表完成其他机型实测 |

小米 HDMI 调用使用 `com.xiaomi.mitv.tvplayer.ExternalSourceActivity`，action 为
`com.xiaomi.mitv.tvplayer.EXTSRC_PLAY`，`input=23` / `24` 分别对应已测试固件的 HDMI 1 / 2。
调用前检查机型、组件是否导出及所需权限；失败时提示，不修改系统设置。

小米原桌面必须走公开别名 `com.mitv.tvhome.MainActivity`，直接调用其实际 Activity 可能被签名权限阻止。

## 私人试用版本的实机记录（2026-10-20）

- PS5 / Apple TV 从主列表直接进入对应 HDMI，Apple TV 显示 4K 60 Hz / Dolby Vision，PS5 显示登录画面。
- 哔哩哔哩、投屏入口、相册、小米原桌面、设置与信号源面板可以打开。
- 固定壁纸设置在强制结束后保留；1 分钟自动轮播实际换图，之后恢复默认 5 分钟。
- 右下角操作区、全部应用和壁纸面板的遥控器焦点已验证。
- Home 回到小米原桌面，系统应用没有被禁用。

这些记录针对公开源码的前身。公开版更换了包名、默认背景和签名，公开版验证另见发布变更记录。

## 仍有边界

- 未验证其他电视型号或固件。未匹配机型时保留桌面与应用列表，专用 HDMI / 小米桌面入口会提示不可用，系统设置尝试标准 Android Intent。
- v0.2.0 提供需手动配置的实验性开机自动进入；不改变 Home 键。详见 [设置与验证范围](BOOT-START.md)。
- 私人同源实现已验证完整重启；公开签名 v0.2.0 APK 未重新执行完整重启测试，其他固件及实际待机唤醒未验证。
- 未完成真实小爱语音、手机投屏播放、直播播放、HDMI 音频、游戏延迟和长期待机测试。
- v0.3.0 默认入口为 6 项，可在设置中编辑为 1–9 项。名称与端口映射仍需修改源码。
- 壁纸仅支持预先打包，最多 15 张；不支持联网图库或电视端新增照片。

## 开机自动进入的新增验证（2026-10-21）

在同一配置的私人同源实现上，系统 start_3rd_app 调用一次性入口后成功进入桌面，现场观察与日志一致。Home 仍返回原桌面，重复调用不抢占原桌面或 Apple TV HDMI；关闭、开启开关均通过。没有授予悬浮窗权限，没有禁用系统应用。

公开 v0.2.0 默认关闭，应用不自行写入系统目标。启动入口仅在 MiTV-MTSU0、开关开启、开机十分钟内且本次启动尚未尝试过时转入桌面；系统更新可能改变内部启动接口。普通应用列表入口不受这些条件限制。

## v0.3.0 统一设置与 Dock（2026-10-21）

私人同源 UI 已在上述机型检查分组设置、首页预览、壁纸浏览不保存、逐层返回、Dock / 图标显示、字体与焦点样式、首页排序和撤销、纯色背景暂停轮播。更新保留原壁纸和启动偏好；没有新增系统权限。公开版构建与测试通过，公开签名包的启动验证没有重复进行。六项居中、九项向两侧滚动、首尾停留及恢复原顺序均已在私人同源版本实机验证。本次公开版仍只有实验性完整开机入口，没有合入私人待机唤醒策略。完整边界见[本次变更](changes/2026-10-21-grouped-settings.md)与[设置指南](SETTINGS.md)。

## Win8 磁贴与 L65MB-SP（2026-10-23）

- 新增「磁贴」首页布局，固定五块磁贴（3 个 HDMI 大磁贴 + 电视设置 / 小米原桌面小磁贴），细节见[变更记录](changes/2026-10-23-metro-tiles.md)。
- 机型判断加入 `MiTV-L65MB-SP`（小米电视 S Pro Mini LED 2025 65 英寸）。电视设置、小米原桌面与 HDMI 仍走 MiTV-MTSU0 的调用路径，但**未在 L65MB-SP 实机验证**；失败会提示或退回系统信号源面板。
- HDMI 3 使用 `input=25`（由已验证的 23 / 24 对应 HDMI 1 / 2 推得），未经真机确认。直切异常时自动打开系统「信号源」面板。
- 开机自动进入仍只对 MiTV-MTSU0 放行（`bootVerified()`），避免把未验证的开机入口带到新机型。
- 本次改动在无 JDK / Android SDK 的环境编写，未执行 Gradle 构建、单元测试与 lint；只做了 Java 语法解析、布局数学与资源格式检查。

## 瑞士桌面与 MiTV-MFFU0（2026-11-03，实机）

| 项目 | 配置 |
| --- | --- |
| 设备型号 | MiTV-MFFU0（`finch`） |
| 系统 | HyperOS 2.0.8.0.UFFMATV / Android 14 / API 34 |
| 渲染 | 1920 × 1080，density 320 |
| 应用 | `io.github.op7418.guizangtv` 0.5.0-swiss（versionCode 7，本机调试签名） |

已在实机确认：

- HDMI 直切实测 `input=23 → HDMI 1`、`24 → HDMI 2`、`25 → HDMI 3`；HDMI 3 的 Apple TV 显示 4K 60Hz + Dolby Vision。
- 右下角「退出」通过 `com.mitv.tvhome/.MainActivity` 回到小米原桌面，界面与原桌面一致。
- 默认桌面：`cmd package set-home-activity` 能设置 `android.app.role.HOME`，原生「默认主屏幕应用」也显示本应用，
  但小米 `RootWindowContainer.resolveHomeActivity()` 忽略角色，Home 键仍启动 `com.mitv.tvhome/.MainActivityUserMode`；
  桌面组件被保护，`pm disable-user` 会抛 `SecurityException`。
- 完整重启后 /data 第三方应用会被置为 stopped，`BOOT_COMPLETED` 不投递（日志 `PackageManager: stop: <pkg>`）。
- 采用无障碍桥接管：完整重启直接进入瑞士桌面（用户现场确认）；从其他应用按 Home 会先闪小米桌面约 0.3–0.5 秒再回到瑞士桌面。
- 开机广告、网络推荐、屏保原本已关闭；本次禁用商城 / 商城插件 / 推送广告服务，推荐位设为 0。
- `tvapp_one_touch_play=1`、`mitv.settings.db.video.auto_switch=1` 未改动；`boot_auto_play` 由用户手动设为 0。

边界：

- 只在这一台 MiTV-MFFU0 上验证；其他小米固件、其他品牌不保证，尤其无障碍桥的窗口类名与桌面组件名。
- 已停留在小米桌面时再按 Home 不会跳回瑞士桌面（没有新的窗口状态事件）。
- 真实「信号源接入自动进入」未在本次操作中触发，只核对了开关值。
- 未 root；所有改动可逆，回滚见 [瑞士桌面文档](SWISS-HOME.md) 与 `scripts/tv-restore.ps1`。
