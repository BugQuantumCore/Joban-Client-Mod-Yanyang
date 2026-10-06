# Yanyang Native LCD — 开箱即用资源包

MTR 4.0 + Joban Client Mod (Yanyang fork) 的 **C++ 原生脚本**演示资源包。
放入 `resourcepacks/` 即可获得两台自带原生 C++ 脚本渲染的 SP1900 列车——
无需编写任何 Java/JavaScript。

## 前置条件

| 组件 | 版本 | 下载 |
| --- | --- | --- |
| Minecraft | 1.20.4（其它 1.17–1.20.4 版本可用同版本 JAR） | — |
| Minecraft Transit Railway | 4.0.5+ | [Modrinth](https://modrinth.com/mod/minecraft-transit-railway) |
| Joban Client Mod **Yanyang** | v2.3.0-beta.1-yanyang.1 | [Release](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/releases/tag/v2.3.0-beta.1-yanyang.1)（Forge / Fabric 双版本） |

> Yanyang fork 的 JAR 内置 `NativeScriptManager`（JNI 宿主），
> 这是原生脚本能被资源包声明的唯一前提。上游原版 JCM 不含此能力。

## 安装

1. 把本资源包（整个 zip）放进 `.minecraft/resourcepacks/`；
2. 将 `assets/yanyang/natives/<你的平台>/` 中的库文件**留在原地**——
   `NativeScriptManager` 会在加载时自动解包到临时文件并 `dlopen`，
   平台目录由 `nativeLibrary` 声明自动匹配（linux-x64 / windows-x64 /
   macos-x64 / macos-arm64）；
3. 游戏内启用本资源包（若提示 pack_format 过旧选择「仍然启用」）；
4. `/give @s mtr:train_schedule` 打开列车菜单，搜索
   `SP1900 · C++ 路线图 LCD` 或 `SP1900 · C++ 车号牌`。

## 你会看到什么

| 车辆（创造菜单可搜 "C++"） | 脚本 | 行为 |
| --- | --- | --- |
| SP1900 · C++ 路线图 LCD | `jslcd_vehicle` | 车侧 2800×800 LCD：顶栏线路名/开往/环线方向/车号液态玻璃卡 · 环线环形图或直线图（奇偶交替站名/换乘徽章/拼音排序）· 开门大站名页 · full/partial 10s 轮播 · v3 出口面板 · 1s 门箭头闪烁；repaint-on-change 稳态 0 纹理上传 |
| SP1900 · C++ 车号牌 | `jslcd_train_num` | 侧线名驱动的头尾牌 + 每车厢双侧号牌，侧线名不变零重绘（FNV 签名短路） |

两台车的模型/贴图直接引用 MTR 内置 `mtr:` 资源（SP1900 原样克隆），
本包不含任何模型与贴图文件——纯脚本演示。

## 声明方式（mtr_custom_resources.json）

```jsonc
{
  "vehicles": [{
    "id": "yanyang:sp1900_jslcd",
    // ...SP1900 完整克隆（模型引用 mtr: 内置资源）...
    "scriptId": "yanyang:jslcd_vehicle",   // JCM 扩展：指向脚本条目
    "hideDisplayParts": true               // JCM 扩展：隐藏原版显示方块
  }],
  "vehicleScripts": [{
    "id": "yanyang:jslcd_vehicle",
    "language": "cpp",                     // 关键字段：交给 NativeScriptManager
    "nativeLibrary": "yanyang:natives/jslcd_vehicle"  // 平台无关，自动解析
  }]
}
```

`nativeLibrary` 平台解析约定（`NativeScriptManager.resolveNativeLibraryPath`）：

```
yanyang:natives/jslcd_vehicle
  → yanyang:natives/linux-x64/libjslcd_vehicle.so     (Linux x64)
  → yanyang:natives/windows-x64/jslcd_vehicle.dll     (Windows x64)
  → yanyang:natives/macos-x64/libjslcd_vehicle.dylib  (macOS Intel)
  → yanyang:natives/macos-arm64/libjslcd_vehicle.dylib (macOS Apple Silicon)
```

直接写带扩展名的完整路径同样合法（单平台包场景）。

## 扩展

包内 4 个平台目录各带全部 5 个示例库，写几行 JSON 即可接线其余脚本：

- `vehicle_lcd` — 128×32 点阵车头牌（车次号/目的地轮播/NEXT 站/速度）
- `pids_arrivals` — PIDS 到站信息屏（走 JCM PIDS preset，非本文件）
- `eyecandy_signal` — 红石信号灯 + 数码时钟（`objects[]` + `objectScripts[]`，
  需自备模型；参见 JCM 文档）

对照源码：`native/examples/jslcd_vehicle.cpp`（约 1500 行，含全部布局数学）
与 `native/include/mtr/`（SDK 头文件）。完整开发文档见
[mtr-native-scripting-v3-sdk.zip](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/releases/tag/v2.3.0-beta.1-yanyang.1)。

## 已知边界

- `pack_format: 22` 对应 1.20.4；旧版客户端会提示版本不匹配但可强制启用；
- 车辆克隆自 MTR 4.0.5 内置 SP1900，`legacy*` 声音字段一并保留；
- 原生脚本与 JS 脚本可在同一世界共存（同一 mixin 分发，互不影响）。

---
License: 库与 JSON 声明随 [Yanyang fork](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang)（JCM LGPL-3.0）。
