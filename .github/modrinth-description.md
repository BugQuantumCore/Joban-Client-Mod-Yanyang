# Joban Client Mod — Yanyang（晏阳）

Joban Client Mod 的晏阳技术组维护分支：为 **Minecraft Transit Railway 4.0** 带来 **JS + C++ 混合脚本**、**零 JNI 原生运行时**、**服务端开门方向记忆**与大量**渲染性能修复**；已全量同步原版 **v2.3.0-prerelease.2**（61 个提交）。

## ✨ 核心特性

- **JS + C++ 混合脚本**：vehicle / PIDS / lift / eyecandy 四类脚本均可用 C++ 编写，与 JavaScript 同条目混用、同时渲染；C++ 侧可调用 JS 能访问的全部公开 Java API，SDK 与上游 JS API 面同步（`mtr::Rect`、`full_aa()` 抗锯齿、`StateTracker`、`renderType(String)` 等）。
- **零 JNI 原生运行时**：JDK 16–21 共享内存 poller 平面、JDK 22+ Panama FFM 平面；渲染与图形处理全部留在 C++，热路径零 Java 对象分配、不参与 JVM GC。
- **8 平台原生桥开箱即用**：windows-x64 / windows-arm64 / linux-x64 / linux-arm64 / macos-x64 / macos-arm64 / android-arm64 / ios-arm64，无需手动安装任何库文件。
- **服务端开门方向记忆**：`vehicle.getDirectionOfDoorOpening()`（JS / C++）识别当前站与下一站开门侧，跨视距、跨会话生效；WorldEdit / Axiom 批量编辑自动增量重扫。
- **帧稳定性工程**：捕获→回放稳态零分配、原生车辆视锥剔除（双加载器）、实例热保持与销毁预算；脚本化 PIDS 文字随相机漂移等回归已修复。
- **脚本执行防护（上游 prerelease.2）**：`scriptRateLimit` 速率限制、无限循环检测中止、解析超时保护与脚本缓存；Iris / OptiFine 阴影 pass 修复覆盖全部脚本渲染面，BVE 电机声音卡死等 MTR 4 著名缺陷一并修复。

## 📦 安装

- **Minecraft 1.17.1 – 1.20.4**（Fabric / Forge）· 前置 **MTR 4.0.5+**（Fabric 端另需 Fabric API）· Java 16+
- **与原版 Joban Client Mod 互斥**（同一 mod id，只能安装其一）
- 专用服务器请同时安装本模组，开门方向预测才能跨视距生效
- C++ 脚本开发文档（7 种语言）：https://bugquantumcore.github.io/Joban-Client-Mod-Yanyang/

## 🔗 链接

- CurseForge：https://www.curseforge.com/projects/1736483
- GitHub（源码 / 全部版本 / 完整更新日志）：https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang

## 🤝 致谢

原版 Joban Client Mod（AmberFrost 及贡献者）· Minecraft Transit Railway（jonafanho）· MAGIC MTR Addon（JustNoone）· 晏阳技术组
