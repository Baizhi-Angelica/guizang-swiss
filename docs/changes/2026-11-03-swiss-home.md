# 瑞士桌面（极简 HDMI 首页）

- Change-Id: `guizang-tv-20261103-swiss-home`
- Agent: DeepSeek Harness（deepseek-flash）；Agent-Session：本机会话，未记录会话 ID
- 授权：用户要求在自己的小米电视（小米电视 S Pro 65 2025，MiTV-MFFU0）上做一个瑞士风格的极简桌面，
  只要 3 个 HDMI 和右下角小退出按钮，开机默认这个桌面，保留小米的信号源自动进入；并清理电视广告。
- 来源：本轮用户指令；上一轮 Win8 磁贴的实现（`MetroSurface` / `MetroTile` / `XiaomiControls`）作为参考。
- 基线：`tv启动器/guizang-tv` 快照（2026-10-23 版本，未含 Git 历史）。

## 行为

新增 `SwissHomeActivity` 作为桌面入口，整屏 Canvas 绘制，不再使用列表 / Dock / 磁贴 / 壁纸。

- 版式：顶部红色方块 + 「小米电视」（粗体）+「信号源选择」（灰），下方细线；左侧 300px 大时钟（红色冒号）、
  日期与星期；右侧三行信号源（01 Switch / 02 有线电视 / 03 Apple TV，各带 HDMI 1–3 小字）；
  右下 180×80 小「退出」框；底部一行遥控器提示。设计画布 1920×1080，按窗口等比缩放。
- 焦点：上下在三个信号源与退出之间移动，第三行向下进入退出，退出向左回第三行；数字键 1/2/3 直达 HDMI。
  聚焦行整行填充 `#D92318`，反白文字，填充从左向右扫过（170ms）；入场依次淡入。
- 日夜：默认跟随日出日落（NOAA 方程，上海坐标，本地计算），白日米色、夜间深色；每分钟检查一次。
  可用 `SWISS_THEME` 广播临时强制米色 / 深色 / 自动，偏好保存在 `swiss-home`。
- 信号源：`com.xiaomi.mitv.tvplayer/.ExternalSourceActivity` + `EXTSRC_PLAY` + `input=22+port`；
  本机实测 23/24/25 分别对应 HDMI 1/2/3，Apple TV（HDMI 3）显示 4K 60Hz + Dolby Vision。
  失败时回退系统信号源面板，再失败才提示。
- 退出：公开入口 `com.mitv.tvhome/.MainActivity`，并写一次性 hand-off，避免被无障碍桥立刻抢回。

## 开机与 Home（小米固件限制）

- 实测 `cmd package set-home-activity` 能拿到 `android.app.role.HOME`（原生「默认主屏幕应用」也显示已选中），
  但小米改过的 `RootWindowContainer.resolveHomeActivity()` 忽略该角色，Home 仍启动
  `com.mitv.tvhome/.MainActivityUserMode`；`pm disable-user` 改桌面组件会抛 `SecurityException`。
- 完整重启会把 /data 第三方应用置为 stopped，`BOOT_COMPLETED` 不投递；因此新增无障碍桥
  `SwissAccessibilityService`（只读窗口包名与类名，`canRetrieveWindowContent=false`）：
  监听 `TYPE_WINDOW_STATE_CHANGED`，小米桌面两个主界面出现时 350ms 后拉起瑞士桌面；
  服务绑定在开机后 5 分钟内时再补两次拉起，覆盖「桌面已先画出」的时机。
- `SwissBootReceiver` 保留为备用：仅当无障碍服务未启用时，开机 15 秒后拉起（需 `SYSTEM_ALERT_WINDOW` app-op）。
- 结果：完整重启直接进入瑞士桌面；从其他应用按 Home 回到瑞士桌面（小米桌面会闪约 0.3–0.5 秒）；
  已经在小米桌面时再按 Home 不会跳回（没有新窗口事件）。

## 代码结构

- 新增：`SwissHomeActivity`、`SwissBoard`（绘制与焦点）、`SwissFrame`（版式与导航数学）、
  `SwissPalette`（米色 / 深色 / 自动偏好）、`SwissSun`（日出日落）、`SwissLinks`（HDMI / 退出入口）、
  `SwissHandoff`（一次性放行）、`SwissAccessibilityService`、`SwissBootReceiver`、`SwissThemeReceiver`、
  `res/xml/swiss_accessibility.xml`。
- 修改：`AndroidManifest.xml`（新入口、无障碍服务、广播、权限）、`BootGateActivity`（改为打开瑞士桌面）、
  `styles.xml`（米色 / 深色主题、无障碍说明）、`app/build.gradle`（versionCode 7 / `0.5.0-swiss`、
  本机调试签名配置）、`MetroSurface`（修复旧布局无法编译的 lambda 捕获）。
- 旧入口 `FrameHomeActivity` 仍可显式启动，但不再声明 LAUNCHER / HOME，不参与桌面选择。

## 电视端配置（已执行）

- 无障碍：在 `enabled_accessibility_services` 中追加本应用（保留 `com.xiaomi.voicecontrol`）。
- 开机目标：`settings put system start_3rd_app io.github.op7418.guizangtv`（备用路径）。
- 广告清理：禁用 `com.mitv.shoplugin`、`com.xiaomi.mitv.shop`、`com.xiaomi.mitv.tvpush.tvpushservice`；
  `my_app_recommend_count=0`；开机广告、网络推荐、屏保原本已关闭。
- 保留：`tvapp_one_touch_play=1`、`mitv.settings.db.video.auto_switch=1`、CEC 设置未改动；
  `boot_auto_play` 由用户手动设为 0（否则开机会被信号源接管）。

## 验证与边界

- 已完成：Gradle 8.9 + AGP 8.5.2 + JDK 17 构建通过；单元测试通过（含新增 `SwissFrameTest`、`SwissSunTest`）；
  HDMI 1/2/3 切换实测通过；退出回到小米桌面通过；Home 键接管通过；完整重启进入瑞士桌面由用户现场确认。
- 未完成：真实「信号源接入自动进入」未在本次操作中触发验证（仅确认开关值未改）；其他机型与固件不保证；
  未测试开机广告实际画面（本机 `boot_ad=0`）。
- 回滚：`scripts/tv-restore.ps1`（恢复桌面角色、关闭无障碍、重新启用被禁用的包、恢复推荐位、卸载应用）。
  详细步骤与已知限制见 [瑞士桌面文档](../SWISS-HOME.md)。
