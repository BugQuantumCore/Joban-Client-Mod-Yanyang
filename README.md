# Develop-Documentation-Website — JCM Yanyang C++ 脚本开发文档（网页版）

本分支存放 **Joban Client Mod — Yanyang** 的 C++ 原生脚本开发文档静态站点，与仓库
`native/README.md` / `native/include/mtr/` 的 SDK（ABI 10，对应 2.3.0-beta-2.yanyang.7）同步维护。

- **在线地址**：<https://bugquantumcore.github.io/Joban-Client-Mod-Yanyang/>
- **浅色 / 深色主题**：全站支持，首次访问跟随系统偏好，手动切换后记住选择（`localStorage`）
- **多语言**：简体中文（中国大陆）· 繁體中文（香港特別行政區）· 繁體中文（台灣）· 文言（華夏）· English (US) · English (UK) · Français (France)

## 站点结构

```
index.html              语言选择入口（按浏览器语言标注「推荐」）
assets/style.css        共享样式（浅色 / 深色双主题，CSS 变量驱动）
assets/app.js           主题切换 · 语言切换 · 侧栏开合 · 轻量代码高亮（无外部依赖）
zh-CN/  zh-HK/  zh-TW/  lzh/  en-US/  en-GB/  fr-FR/
  ├── index.html          概览：什么是 C++ 原生脚本、架构、何时选 C++
  ├── getting-started.html 快速上手：SDK 获取、模块工程、编译、打包进资源包、游戏内验证
  ├── script-model.html   脚本模型与生命周期：四类基类、State、线程与崩溃隔离
  ├── data.html           数据快照参考：Vehicle / Stop / Arrival / Pids / Lift / EyeCandy
  ├── drawing.html        绘制与画布：Text / Texture、Matrices、GraphicsTexture、Gfx2D、字体
  ├── java-api.html       mtr::Api 与 Java 互操作：对象、回调、JS↔C++ 对照表
  ├── abi.html            C ABI 与宿主服务：导出、帧输入输出、绘制记录、ABI 演进
  └── performance.html    性能与调试：热路径守则、defer_free、latest.log、FAQ
```

术语约定：MTR 3 时代的 **train** 在 MTR 4 统一称为 **vehicle**（`vehicleScripts` /
`VehicleWrapper`）；SDK 数据包装类为 `mtr::Vehicle`（旧名 `mtr::Train` 保留为源码兼容别名）。

纯静态站点：无构建步骤、无外部依赖、可直接从文件系统打开。

## 本地查看

任选其一：

```sh
# 任意静态服务器
python3 -m http.server 8080
# 然后访问 http://localhost:8080/index.html
```

或直接用浏览器打开 `index.html`（站内全部为相对链接，离线可用）。

## GitHub Pages

仓库 Pages 已配置为 *Deploy from a branch* → 分支 `Develop-Documentation-Website`
→ 目录 `/（root）`，站点即从本分支根目录发布。
