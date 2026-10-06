# MTR Native Scripting (C++)

为 **Minecraft Transit Railway 4.0** 的 `vehicle` / `eye_candy` / `pids`
三类自定义资源提供 **C++ 原生脚本**能力——与 Joban Client Mod v2.3 的
JavaScript (Rhino) 脚本 1:1 语义对齐（同样的 `create/render/dispose`
生命周期、同样的捕获-回放渲染管线、同样的 API 表面），但以原生
共享库执行，消除解释执行与跨语言反射开销。

## 目录结构

```
native/
├── include/mtr/            # 脚本 SDK（header-only）
│   ├── mtr_native.h        #   C ABI：导出、POD 快照、draw call 记录（v2）
│   ├── script.hpp          #   注册宏 + 生命周期适配（ScriptBox）
│   ├── frame.hpp           #   bump-arena 帧录制器（像素 arena 按需增长）
│   ├── vehicle.hpp         #   Train/Car/Stop 包装（对应 VehicleWrapper）
│   ├── pids.hpp            #   Pids/Arrival 包装 + Text/Texture 构建器
│   ├── eyecandy.hpp        #   EyeCandy 包装（对应 EyeCandyScriptContext）
│   ├── gfx.hpp             #   GraphicsTexture（脏矩形纹理）
│   ├── gfx2d.hpp           #   java.awt.Graphics2D 等价软件光栅化器 ★
│   ├── font5x7.hpp         #   5x7 公版点阵字体（ASCII 文本）
│   ├── matrices.hpp        #   矩阵栈（对应 JS Matrices）
│   ├── text.hpp            #   Text/Texture 流式 API（对应 jsblock Text/Texture）
│   └── util.hpp            #   CycleTracker / PidsUtil（对应 pids_util.js）
├── examples/               # 示例脚本（各编译为一个 .so/.dll）
│   ├── vehicle_lcd.cpp     #   车侧 LCD + 车次号（对应 display_helper.js 用法）
│   ├── pids_arrivals.cpp   #   到站信息屏（pids_1a.js 的逐行移植）
│   ├── eyecandy_signal.cpp #   红石信号灯 + 数码时钟
│   ├── jslcd_common.hpp    #   社区 JS LCD 包共享移植层 ★
│   ├── jslcd_vehicle.cpp   #   完整车侧路线图 LCD（13 个 JS 文件的全量移植）★
│   └── jslcd_train_num.cpp #   车号系统（侧线名解析/侧牌/头尾牌）★
├── java/                   # Java 侧 JNI 桥接参考实现
│   └── com/lx862/jcm/nativeapi/NativeScriptManager.java
├── bench/                  # 真实微基准（C++ vs bun-JSC vs Rhino）
│   ├── lcd_bench.cpp       #   原生驱动：mtrCreate/mtrRender x N
│   ├── lcd_bench.js        #   JS 孪生（同算法同字体, bun 运行）
│   ├── lcd_bench_rhino.js  #   Rhino 兼容孪生（同算法, ES5 风格）
│   ├── RhinoBench.java     #   以 JCM 同款引擎/调用模型驱动上者
│   ├── jslcd_smoke.cpp     #   jslcd 端到端冒烟（快照构造→渲染→PPM 重建→断言）★
│   └── gfx2d_bench.cpp     #   Gfx2D 基元微基准 ★
└── CMakeLists.txt
```

## 快速上手（vehicle 示例）

```cpp
#include <mtr/script.hpp>
#include <mtr/gfx.hpp>

struct LcdState {
    mtr::CycleTracker destination{"TSUEN WAN|KWUN TONG", 120};
};

struct LcdScript : mtr::VehicleScript<LcdState> {
    static constexpr auto ID = "demo:kcx_lcd";

    void create(mtr::VehicleContext& ctx, LcdState& s, const mtr::Train& t) override {
        lcd.create(ctx.input(), 128, 32);        // JS: new GraphicsTexture(128, 32)
    }
    void render(mtr::VehicleContext& ctx, LcdState& s, const mtr::Train& t) override {
        paint(s, t);                              // 纯 C++ 点阵绘制热循环
        lcd.upload(ctx.frame());                  // JS: texture.upload()
        mtr::Matrices m{ctx.frame()};
        ctx.draw_car_model(model, 0, &m);         // JS: ctx.drawCarModel(...)
    }
    mtr::GraphicsTexture lcd{};

    /* ... paint 省略，见 examples/vehicle_lcd.cpp ... */
};

MTR_REGISTER_VEHICLE_SCRIPT(LcdScript)
```

资源包声明（与 JS 脚本共存）：

```json
// mtr_custom_resources.json
{
  "vehicles": [ { "id": "demo:kcx", "scriptId": "demo:kcx_lcd", "hideDisplayParts": true } ],
  "vehicleScripts": [
    { "id": "demo:kcx_lcd", "language": "cpp",
      "nativeLibrary": "mtr:natives/libvehicle_lcd.so" }
  ]
}
```

构建：

```bash
# 方式一：CMake（推荐，自动带上 -fvisibility=hidden）
cmake -B build && cmake --build build -j
# 产物: build/vehicle_lcd.so 等三个脚本库 + lcd_bench

# 方式二：直接 g++（注意 -fvisibility=hidden 不可省略——
# 否则多个模块的 inline 静态 recorder 会被动态链接器符号合并）
g++ -O2 -std=c++17 -fPIC -shared -fvisibility=hidden \
    -Iinclude examples/vehicle_lcd.cpp -o libvehicle_lcd.so
```

验证（bench/ 下已提供）：

```bash
g++ -O2 -std=c++17 -Wall -Iinclude bench/lcd_bench.cpp \
    examples/vehicle_lcd.cpp -o bench/lcd_bench && ./bench/lcd_bench
g++ -O2 -std=c++17 -fPIC -shared -fvisibility=hidden \
    -Iinclude examples/pids_arrivals.cpp -o bench/libpids_arrivals.so
g++ -O2 -std=c++17 -fPIC -shared -fvisibility=hidden \
    -Iinclude examples/eyecandy_signal.cpp -o bench/libeyecandy_signal.so
g++ -O2 -std=c++17 -Iinclude bench/smoke_test.cpp -ldl \
    -o bench/smoke_test && ./bench/smoke_test   # → PIDS/EYECANDY SMOKE OK
```

## 基准（本仓库 bench/，g++ -O2 vs Rhino 1.7.14）

同一 LCD 渲染算法（128×32 点阵 + 17 条 draw call + 8 车 × 2 侧模型绘制）：

| 引擎 | 每帧 | 相对原生 |
|---|---|---|
| C++ (g++ -O2) | 5.45 µs | 1× |
| bun JavaScriptCore（纯算法对照组） | 4.03 µs | ≈1×（现代 JIT 确实快） |
| Rhino opt9（JCM 最乐观配置） | 230 µs | 42× |
| **Rhino opt0（JCM v2.3 实际默认）** | **271 µs** | **50×** |

注意：三种实现都**未计入** JCM 真实管线中 JS 侧的
NativeJavaObject 反射取值、装箱与每条 draw call 的 Java 对象分配
（原生路径本来就没有这些开销），因此游戏内的差距只会更大。
以 50 辆车同屏、20fps 脚本频率计：Rhino ≈ 13.5 ms/帧 纯脚本开销，
原生 ≈ 0.27 ms/帧。

## ★ 社区 JS LCD 包的完整移植（jslcd_vehicle / jslcd_train_num）

`examples/jslcd_vehicle.cpp` + `examples/jslcd_train_num.cpp` 是一套
社区 JS 资源包（13 个文件、约 3000 行：main.js / config.js / data.js /
circular.js / util.js / mtr_util.js / train_num_util.js / draw_header.js /
draw_common.js / draw_circular.js / draw_linear.js / train_num.js /
draw_num.js）的**全量 C++ 移植**——车侧路线图 LCD + 车号系统：

- **顶部信息栏**：线路色带 + 线路名块（数字号线大号数字版式 / 命名线
  双语居中）+ logo 徽章 + 开往终点站 / 环线方向（内环/外环）+
  下一站/到达 + 车号「液态玻璃」卡片（6 层阴影 + 双层高光 + 描边）；
- **环线**：完整环形线路图（圆角矩形轨道 + 站点圆点 + 自适应字号 +
  换乘徽章 + 进度闪烁叠加 + 弧段箭头）/ 部分图（5 站胶囊，环绕取模）；
- **直线**：完整线路图（灰轨 + 已过/当前段着色 + 奇偶交替站名 +
  字形度量垂直定位）/ 部分图（边界圆 + 已过站置灰）；
- **开门页**：大站名（线路色）+ 终点站提示 + 换乘/Transfer 面板；
- **状态机**：full/partial 10s 轮播、1s 闪烁、门检测阈值、到站去抖、
  行驶方向左右屏（isReversed 翻转）——语义与 main.js 一致；
- **车号系统**：侧线名解析（`"10010/01-02-…"` / `"10010 Tc1 A1-1"`）、
  每车厢双侧牌 + 头尾牌、透明清空（AlphaComposite.CLEAR 等价）、
  车厢数不匹配时的红色双语错误提示。

### 性能策略（比 JS 更快的三层设计）

1. **repaint-on-change**：JS 每帧重绘全部 16 块屏；原生版用内容签名
   （页面模式/闪烁/门状态/站序/线路/侧线名的 FNV 哈希）判定，
   内容不变则 0 光栅化、0 上传——稳态实测 **18.7 µs/帧**；
2. **分帧重绘预算**：内容变化时每帧最多重绘 2 节车厢（脏矩形让
   延迟上传零成本），全列车重绘分摊到 4 帧（每帧 ≈10 ms）而不是
   一帧 40 ms——帧预算友好，JS 则每帧都全量重绘；
3. **Gfx2D 快速路径**：不透明色行走行级 `std::fill`（整屏白底
   186 µs vs 修复前 4213 µs，23×），像素基址缓存消除逐像素引用链。

### 已修复的上游 JS bug（移植时发现）

`computeLcdAspectFromSlot` 对 LCD_POS_L/R 四边形（顶点序 TL→BL→BR→TR）
返回的是**长宽比的倒数**（0.286 而非注释声称的 ≈3.5），会把 SCR_H
吹到 11564px。移植版取长边/短边（3.5），与 config.js 全部布局常量
（TEX 2800×800、LAYOUT_K 1.667、SCR 3304×944）一致。

冒烟验证（`bench/jslcd_smoke.cpp`，33 项断言全过）：合成 8 车 / 10 站 /
换乘/环线快照 → dlopen 真实 ABI 驱动 → 帧记录重建纹理 → PPM 落盘 →
像素级断言（顶栏线路色、红绿站点圆点、玻璃卡字形、环线色环、开门大
站名、车牌墨迹）+ 性能断言（稳态 <100µs、重绘预算帧 <20ms）。

## ABI v2 变更

`JcmVehicleSnapshot` 新增（宿主在 marshalling 时写入，脚本端零成本读取）：

| 字段 | JS 来源 |
|---|---|
| `route_name_offset/len` | `thisRouteStops.get(0).route.name` |
| `route_color` | 同上 `.color`（ARGB） |
| `circular_state` | 同上 `.getCircularState()`（0/1/2 = NONE/CLOCKWISE/ANTICLOCKWISE） |
| `siding_name_offset/len` | `vehicle.getSiding().getName()`（车号来源） |

`JcmFrameOutput` 像素 arena 支持**按需增长**（初始 16MB 静态 BSS，
超出时切换到堆缓冲并保留复用）——多车厢 LCD 重绘突发的 50MB 帧不再
受固定容量限制。

## 与 JCM JS API 的映射

| JS (JCM v2.3) | C++ (本 SDK) |
|---|---|
| `function create(ctx, state, wrapper)` | `void create(Context&, State&, const Wrapper&)` |
| `function render(ctx, state, wrapper)` | `void render(Context&, State&, const Wrapper&)` |
| `function dispose(ctx, state, wrapper)` | `void dispose(Context&, State&, const Wrapper&)` |
| `ctx.drawCarModel(model, car, matrices)` | `ctx.draw_car_model(handle, car, &matrices)` |
| `ctx.playAnnSound(sound, vol, pitch)` | `ctx.play_ann_sound(sound, vol, pitch)` |
| `Text.create().text().pos().draw(ctx)` | `mtr::Text::create().text().pos().draw(ctx)` |
| `pids.arrivals().get(i).destination()` | `pids.arrivals().at(i).destination()` |
| `new GraphicsTexture(w, h)` + Java2D | `mtr::GraphicsTexture` + `mtr::Gfx2D`（纯 C++ 光栅化） |
| `new DisplayHelper(slotCfg)` | jslcd 移植：宿主 `acquire_model`（同槽位四边形）+ `GraphicsTexture` |
| `g.fillRoundRect / fillOval / drawLine / GeneralPath` | `Gfx2D::fill_round_rect / fill_oval / draw_line / fill_polygon` |
| `TextUtil.cycleString("A|B")` | `mtr::CycleTracker{"A|B"}` / `pids_util` |
| `be.redstoneLevel()` | `ec.redstone_level()` |
| `ctx.events().onBlockUse(cb)` | `ec.block_use_events()`（快照事件位） |

License: MIT（与 Joban Client Mod 一致）。
