# 开机加固：守护、复活监听与 root 复核

- Change-Id: `guizang-tv-20261109-home-guard`
- Agent: DeepSeek Harness（deepseek-flash）；Agent-Session：本机会话，未记录会话 ID
- 授权：用户反馈「重启后又回到小米桌面」，并要求 root 后强制默认这个启动器。
- 来源：用户现场反馈；同型号 [Aerial TV](https://github.com/pengchujin/aerial-tv) 的
  `docs/devices/xiaomi-mitv-mffu0.md` 与 `tools/install.sh` 做法。

## 问题

2026-11-03 部署的 0.5.0 靠无障碍服务接管小米桌面。使用几天后，应用被系统从
`enabled_accessibility_services` 里移除，重启后没有任何接管，落回 PatchWall；同时电视的
「ADB 调试」在完整重启后会关闭，用户需要手动打开。

## 本次修改

- `SwissGuard`：拿到 `WRITE_SECURE_SETTINGS` 后，在应用启动、无障碍服务绑定、开机广播时
  把本应用追加回无障碍列表，并把 `adb_enabled` 写回 1。
- `SwissRevivalListener`：新增通知监听服务，系统在开机约 20 秒时绑定；连接后修复守护并拉起
  瑞士桌面（`SYSTEM_ALERT_WINDOW` app-op 允许后台启动 Activity）。这是无障碍被清掉时的第二条开机路径。
- 开机窗口：重启后 3 分钟内，`com.mitv.tvhome` 的任何页面（包括桌面恢复的「我的应用」）都会被覆盖；
  3 分钟后只接管两个主界面。
- 退出 hand-off 增加 30 秒过期，避免上次残留的标记在开机时跳过一次接管。
- 回到桌面时如果焦点停在「退出」，自动回到上一个信号源，避免开机第一下 OK 就退回小米桌面。
- 版本 0.5.1（versionCode 8）。

## 电视端授权（一次性，已执行）

```sh
pm grant io.github.op7418.guizangtv android.permission.WRITE_SECURE_SETTINGS
appops set io.github.op7418.guizangtv WRITE_SETTINGS allow
appops set io.github.op7418.guizangtv SYSTEM_ALERT_WINDOW allow
cmd notification allow_listener io.github.op7418.guizangtv/io.github.op7418.guizangtv.SwissRevivalListener
settings put system str_killbackgroud_pkg_white_list io.github.op7418.guizangtv
```

## 验证

- 两次完整重启（不触碰电视）：开机约 20.8 秒通知监听拉起桌面，无障碍随后接管；
  「我的应用」页面约 0.3 秒后被覆盖；最终前台为 `SwissHomeActivity`。
- ADB 端口在开机约 60–70 秒后自行恢复，`adb_enabled=1`，无需手动打开。
- `enabled_accessibility_services`、`enabled_notification_listeners`、待机白名单在重启后保留。
- `pm grant` 前必须先安装声明了 `WRITE_SECURE_SETTINGS` 的版本，否则报
  「Package has not requested permission」。

## root 复核

- `ro.boot.verifiedbootstate=green`、`ro.build.type=user`、`ro.secure=1`、`ro.debuggable=0`、
  SELinux Enforcing、无 `su`、无 `ro.oem_unlock_supported`；MT9655（`ro.board.platform=mt5877`）。
- 电脑只通过 Wi-Fi ADB 连接；fastboot / MediaTek BROM 需要 USB 直连，MT9655 无公开可用解锁漏洞。
- Android 14 近年的内核 CVE 均需本地恶意应用或物理 USB 设备，且没有针对本固件的公开利用链。
- 结论：**不做 root**，改用守护 + 通知监听达到「强制默认启动器」的等价效果。
