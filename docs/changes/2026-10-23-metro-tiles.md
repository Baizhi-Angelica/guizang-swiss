# Win8 磁贴首页（布局第三档）

- Change-Id: `guizang-tv-20261023-metro-tiles`
- Agent: DeepSeek Harness（deepseek-flash）；Agent-Session：本机会话，未记录会话 ID
- 授权：用户要求在自己的小米电视（L65MB-SP）上增加 Win8 磁贴样式首页，固定 3 个大磁贴（HDMI1 Switch / HDMI2 江苏有线电视 / HDMI3 Apple TV）与 2 个小磁贴（电视设置 / 小米原桌面），并加入滚动与背景效果。
- 来源：本轮用户指令；现有列表 / Dock 结构、`WallpaperMaterial` 模糊材质与 `DockShelf` 焦点滚动。
- 基线：`op7418/guizang-tv` main 分支 zip 快照（2026-10-23 下载，未含 Git 历史）。

## 行为

`设置 → 布局 → 首页布局` 增加第三档「磁贴」。选中后首页改为 960×540dp 设计画布上的磁贴条：三个 234dp 大方块（2×2 单元）后接一列两个 111dp 小方块，整体按屏幕居中。列表与 Dock 的入口、顺序、字体、选中样式偏好保持不变。

- 磁贴内容固定，不跟随「首页内容」：`hdmi:1` Switch、`hdmi:2` 江苏有线电视、`hdmi:3` Apple TV、`metro:tv-settings` 电视设置、`com.mitv.tvhome` 小米原桌面。名称、说明、强调色与图标集中在 `MetroTiles.java`。
- 大磁贴显示 `HDMI 1/2/3` 说明与名称；小磁贴只显示名称。图标沿用单色矢量，聚焦时整块放大 3.5%、内容上移 2dp。
- 背景：照片模式下叠一层缓慢平移 / 缩放（96 秒往返）的壁纸，磁贴内再裁切同一张模糊壁纸的一段作为照片层，聚焦时视差偏移，并扫过一道 720ms 的斜向流光。
- 进入桌面时 5 个磁贴按 55ms 间隔依次淡入上浮。
- 遥控器：左右在磁贴间循环（首尾相连），两个小磁贴上下互通，最后一个小磁贴向下进入右下角操作区；操作区向上回到当前磁贴。其余按键行为与列表 / Dock 一致。
- 设置预览复用同一 `MetroSurface`，只做静态展示，不启动动画。

## 代码结构

- 新增 `HomeLayer`：列表、Dock、磁贴共用的时钟、天气、壁纸预览位、`wire()` 焦点/点击绑定。
- `HomeSurface` 改为继承 `HomeLayer`；`FrameHomeActivity` 与 `HomeSettings.preview` 持有 `HomeLayer`，按 `layout` 选择实现。
- 新增 `MetroTiles`（磁贴配置）、`MetroLayout`（纯布局数学，可单元测试）、`MetroTile`（单块绘制与动效）、`MetroSurface`（磁贴首页）。
- 新增 `mark_gamepad.xml`、`mark_tv.xml` 两个 24×24 矢量标记。
- `AppearanceValues` 的 `layout` 允许值由 2 档改为 3 档。

## 小米适配

`XiaomiControls.enabled()` 现在匹配 `MiTV-MTSU0` 与 `MiTV-L65MB-SP`（`Build.MODEL` 去掉 `MiTV-` 前缀后比较）。HDMI 直切支持 1–3 路，`input = 22 + port`（23 / 24 / 25）；HDMI 3 的 25 未在真机验证，直切失败时自动退回系统「信号源」弹窗，两处日志都在 `TvHome.Xiaomi` 标签下。开机自动进入仍只在 `MiTV-MTSU0` 放行（新增 `bootVerified()`），避免把未验证的开机入口带到新机型。

## 验证与边界

- 本机没有 JDK / Android SDK，无法执行 `gradle assembleDebug`、单元测试或 lint；已用 Java 语法解析器逐文件解析（36 个文件通过），并用脚本复核 `MetroLayout` 的坐标与不重叠、矢量 pathData 可解析、资源 XML 合法。**首次构建与实机行为待用户回家验证。**
- 未在 L65MB-SP 上实机验证：HDMI 3 直切码 25、电视设置组件名、小米原桌面别名、磁贴在 4K 电视上的实际观感。
- 未新增权限；未改 Home 键、系统启动目标或默认桌面设置。壁纸仍来自打包资源，不做联网图库。
- 列表 / Dock 的默认六项、壁纸与轮播偏好、Codex 与天气开关保持不变；磁贴布局是新增选项，不改变旧安装的已有选择。
