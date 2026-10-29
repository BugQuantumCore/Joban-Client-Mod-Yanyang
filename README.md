# Develop-Documentation-Website — JCM Yanyang 脚本开发文档（网页版）

本分支存放 **Joban Client Mod — Yanyang**（晏阳技术组修改版）的脚本开发文档静态站点，与仓库
`native/README.md` / `native/include/mtr/` 的 SDK（ABI 11，对应 2.3.0-prerelease.2-yanyang.1）同步维护，
并收录上游 JCM（v2.3，对应 https://jcm.joban.org/v2.3/ 的 For Content Authors）JavaScript 脚本体系的完整开发文档。
- **在线地址**：<https://bugquantumcore.github.io/Joban-Client-Mod-Yanyang/>
- **浅色 / 深色主题**：全站支持，首次访问跟随系统偏好，手动切换后记住选择（`localStorage`）
- **多语言**：简体中文（中国大陆）· 繁體中文（香港特別行政區）· 繁體中文（台灣）· 文言（華夏）· English (US) · English (UK) · Français (France)

## 站点结构

```
index.html              语言选择入口（按浏览器语言标注「推荐」）
assets/style.css        共享样式（浅色 / 深色双主题，CSS 变量驱动）
assets/app.js           主题切换 · 语言切换 · 侧栏开合 · 轻量代码高亮（无外部依赖）
zh-CN/  zh-HK/  zh-TW/  lzh/  en-US/  en-GB/  fr-FR/
  ├── index.html          概览：什么是脚本、架构、何时选 C++
  ├── getting-started.html C++ 快速上手：SDK 获取、模块工程、编译、打包进资源包、游戏内验证
  ├── script-model.html   脚本模型与生命周期：四类基类、State、线程与崩溃防护
  ├── data.html           数据快照：Vehicle / Stop / Arrival / Pids / Lift / EyeCandy
  ├── drawing.html        绘制与画布：Text / Texture、Matrices、GraphicsTexture、Gfx2D、字体
  ├── java-api.html       mtr::Api 与 Java 互操作：对象、回调、JS↔C++ 对照表
  ├── abi.html            C ABI 与宿主服务：导出、帧输入输出、绘制记录、ABI 演进
  ├── performance.html    性能与调试：热路径守则、defer_free、latest.log、FAQ
  ├── js-getting-started.html  JS 脚本入门：注册、生命周期、调试辅助、撰写守则、NTE 关系
  ├── js-api.html         JS 公共 API：资源 / 文件 / 数学 / 渲染 / 模型 / 动态纹理 / 声音 / 网络 / MTR 数据 / Minecraft 环境 / 工具类
  ├── js-types.html       JS 脚本类型：车辆 / 装饰物 / 电梯 / PIDS 专属 API、DisplayHelper
  └── js-yanyang.html     晏阳扩展 API：混合 JS+C++ 条目、开门方向记忆、交通类型细分、直写像素上传
```

术语约定：
- MTR 3 时代的 **train** 在 MTR 4 统一称为 **vehicle**（`vehicleScripts` / `VehicleWrapper`）；SDK 数据包装类为 `mtr::Vehicle`（旧名 `mtr::Train` 保留为源码兼容别名）。
- 每帧输出使用**内存区**（arena）——一大块预先申请的内存，帧内顺序分配、帧末整体回收；表格中「内存区」即上游文档的 arena。
- 晏阳自有扩展（JS 页面中带 <code>晏阳</code> 标记）与上游 JCM 2.3.0-prerelease.2 的 API 一并收录。

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
