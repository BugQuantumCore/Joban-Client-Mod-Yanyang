# Joban Client Mod — Yanyang（晏阳）

Joban Client Mod 的晏阳技术组维护分支：在原版之上为 Minecraft Transit Railway 4.0 带来 **JS + C++ 混合脚本**、**换乘线路类型 API** 与一系列**渲染性能修复**。

## 核心特性

### C++ 原生脚本

车辆、PIDS、电梯、装饰四类脚本均可使用 C++ 编写，并可与 JavaScript 脚本在同一声明条目中混用、同时渲染：

- 与 JS 相同的 `create` / `render` / `dispose` 生命周期与逐实例持久状态；
- 列车、到站、电梯、装饰等常用数据以紧凑快照直读，零 JVM 穿越；
- `mtr::Api` 可调用 JavaScript 脚本能访问的全部公开 Java API；
- 绘制走扁平绘制记录流，热路径零分配，适合高刷新率 LCD / 点阵屏。

### 零 JNI 传输层

Java 与 C++ 之间通过共享内存轮询平面（任意 JDK ≥ 16）或 Panama FFM 平面（JDK 22+）通信，全程不经 JNI；每帧每实例仅一次宿主穿越。传输方式可在模组配置界面切换。

### 7 平台原生桥

jar 内置 windows-x64 / linux-x64 / linux-arm64 / android-arm64 / macos-x64 / macos-arm64 / ios-arm64 七个平台的原生桥，运行时按系统与架构自动匹配（ARM64 Linux 自动回退链）。

### 帧稳定性

捕获→回放管线稳态零分配、离线程内存回收、脏矩形纹理上传、脚本实例热保持（离开视野不销毁、重入镜零重建）。

## 兼容性

| 项目 | 要求 |
| --- | --- |
| Minecraft | 1.17.1 – 1.20.4（Fabric / Forge） |
| 前置模组 | Minecraft Transit Railway 4.0.5+（必需）；Fabric 版另需 Fabric API |
| Java | 16 及以上（22+ 自动启用全程零 JNI 的 FFM 平面） |
| 与原版 JCM | **互斥** —— 同 mod id，二选一，不能共存 |

原版 JCM 资源包与旧版 `language: "cpp"` 格式资源包无需修改即可继续使用。

## 文档与源码

- C++ 脚本开发文档：GitHub 仓库 `Develop-Documentation-Website` 分支
- SDK 说明：仓库 `native/README.md`
- 更新日志：GitHub Releases

---

**English** — A fork of Joban Client Mod maintained by the Yanyang team, adding mixed JavaScript + native C++ scripting (vehicle / PIDS / lift / eyecandy) for Minecraft Transit Railway 4.0, interchange route-type APIs, and extensive render-performance fixes. The Java ↔ C++ runtime is entirely JNI-free (shared-memory poller plane on any JDK ≥ 16, Panama FFM on JDK 22+), and the jar bundles native bridges for seven platforms (windows-x64 / linux-x64 / linux-arm64 / android-arm64 / macos-x64 / macos-arm64 / ios-arm64). Requires MTR 4.0.5+ and replaces — is incompatible with — the original Joban Client Mod. MC 1.17.1–1.20.4, Fabric & Forge.
