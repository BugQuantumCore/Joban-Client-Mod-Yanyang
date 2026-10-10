# JCM v2.3.0-beta.1-yanyang.3 — Native Vehicle Scripts Actually Render

**New in yanyang.3:** the C++ (native) path is now **fully wired end to end** — native
vehicle scripts are no longer just loaded, they draw. This closes the two gaps left in
yanyang.1/.2 (no per-frame driver, no host resource callbacks) and makes the native route a
drop-in replacement for the Rhino/JS route.

## What now works

- **Per-frame driver** — `RenderVehiclesMixin` drives every vehicle's `language: "cpp"`
  script id through `NativeVehicleDriver.render(...)` at the same injection point the JS
  path uses: build a `VehicleWrapper`, marshal it ONCE into the flat `JcmVehicleSnapshot`
  POD (`NativeSnapshot`), run every module registered for that id, then translate the
  returned draw-call records into `ScriptRenderManager` calls.
- **Draw replay** — `VehicleResourceMixin` replays those calls per car through
  `NativeDrawRegistry` + `NativeModelDrawCall`, so a native LCD quad receives the exact same
  `StoredMatrixTransformations` and light as a JS one and follows the car body.
- **Host resource callbacks** — the JNI bridge records the textures/quads a script asks for
  during `mtrCreate` and hands the batch to `NativeHost.createResources(...)` on the render
  thread, which builds real `GraphicsTexture` objects and `RawMeshBuilderJS(4)`/`ModelJS`
  quads (the C++ equivalent of the community `DisplayHelper`), returning handle arrays that
  the bridge rewrites into every frame record.
- **Pixel uploads** — `NativeHost.uploadPixels(...)` blits a dirty rect straight into the
  `GraphicsTexture`'s `DataBufferInt`, so per-pixel work never crosses the JVM boundary.
- Native and JS scripts **coexist**: a native id simply never creates a
  `VehicleScriptInstance`, so the JS branches no-op for it.

## `nativeLibraries` — one script id, several libraries

MTR's vehicle schema carries a single `scriptId`, but a pack often needs more than one
native library for one train (e.g. a route-map LCD *and* a car-number system, which the JS
route expressed as two `scriptLocations` entries). `vehicleScripts` entries now accept an
array alongside the existing single field:

```json
"vehicleScripts": [{
    "id": "wr2a03",
    "language": "cpp",
    "nativeLibraries": [
        "mtr:wr2a03/natives/wr2a03_lcd",
        "mtr:wr2a03/natives/wr2a03_train_num"
    ]
}]
```

Every declared library runs for that id, in declaration order; per-platform resolution
(`windows` / `linux` / `macos-arm64` …, extension or stem form) applies to each entry. The
single `nativeLibrary` field keeps working unchanged.

## ABI 4 / 5

- **v4** — `JcmHostServices.acquire_quad_model`: the host builds the textured quad from the
  script's vertices + UVs and owns mesh/texture lifetime.
- **v5** — `JcmStop.route_circular_state`: every stop now carries its route's
  `CircularState`, the equivalent of `stop.route.getCircularState()`. The LCD port's loop-line
  detection walks the stop list exactly like `circular.js`; the previous shape-based
  heuristic mis-classified *any* multi-stop route as a loop.

## Fixed

- **Native dirty-rect underflow (crash).** `GraphicsTexture::mark_dirty` accepted
  out-of-range coordinates. A centred string wider than the texture starts at a negative x,
  producing e.g. `dirty_x=-76, dirty_w=1196`; `upload()` then read 1196 px per row starting
  *before* the pixel buffer — an access violation. Out-of-range samples no longer take part
  in dirty-rect merging (matching `Graphics2D`'s clipping).
- **JNI export names on MSVC.** Nested `##` expansion did not fire, so the bridge exported
  `Java_JNI_CLASS_PATH_nOpen` instead of the real mangled names.
- **MSVC build.** `/utf-8` added to the native CMakeLists — sources contain CJK string
  literals and cl.exe otherwise decodes them with the ANSI code page (GBK on zh-CN), failing
  every literal.
- **Missing bridge no longer throws.** `System.loadLibrary("jcm_native_bridge")` moved into a
  guarded initialiser: a missing optional native library now logs one actionable line and
  marks `cpp` scripts skipped, instead of raising `UnsatisfiedLinkError` from a static block
  (which broke `isLoaded()` for the resource providers).

## Performance note

The ported WR2-A03 LCD had been rasterising at **3304×11564** because the pack's own
`computeLcdAspectFromSlot()` returns the *reciprocal* of the aspect ratio (0.2857) and
overwrites the configured 3.5, stretching every pixel ~14× vertically and costing ~150 MB of
texture per car side. The native port uses the configured 3.5 (**3304×944**, uniform 1.18
scale); `WR2_LCD_USE_JS_ASPECT=1` restores the literal JS behaviour if pixel-parity is ever
needed.

---

# JCM v2.3.0-beta.1-yanyang.2 — Per-Platform Native Script Selection

**New in yanyang.2:** `mtr_custom_resources.json` entries with `"language": "cpp"` can now
declare their native library **per platform** — Windows (`.dll`), Linux (`.so`) and macOS
(`.dylib`) each get their own key, so one resource pack can ship different builds per OS:

```json
"nativeLibrary": {
  "windows": "demo:natives/windows-x64/kcx_lcd.dll",
  "linux":   "demo:natives/linux-x64/libkcx_lcd.so",
  "macos-arm64": "demo:natives/macos-arm64/libkcx_lcd.dylib"
}
```

The mod auto-detects the running OS/arch (`os.name` / `os.arch`) and picks the matching
library — including arch-specific keys (`macos-x64` / `macos-arm64`) and the `win` / `osx` /
`darwin` aliases. When the current system has **no** entry, the script is **not executed**
and a one-line hint goes to `latest.log` plus (with JCM scripting debug mode enabled) the
in-game chat and the new **C++ Native Scripts** section of the debug HUD — loaded modules in
blue, platform-skipped ones in red with the reason. The plain-string forms stay fully
backward compatible (platform-agnostic stems resolve on every OS; an extension-form path
binds to its OS). Vehicle / eye_candy / lift entries in `mtr_custom_resources.json` and JCM
PIDS presets all share the same resolution logic.

Also in this release: the ready-to-use LCD resource pack zip is no longer attached to
GitHub releases (kept private; build it from CI artifacts or `native/resourcepack/` if
needed).

---

# JCM v2.3.0-beta.1-yanyang.1 — C++ Native Scripting Edition

This is the **Yanyang** fork of Joban Client Mod v2.3.0-beta.1. On top of upstream, it ships the
**MTR Native Scripting** project: a C++ alternative to JCM's JavaScript (Rhino) script engine for
`vehicle` / `eye_candy` / `pids` custom resources — the same lifecycle and drawing semantics
(LCD displays, train numbers, PIDS arrival boards), but running as native code with a single
JNI boundary crossing per frame.

**Both Fabric and Forge builds are provided below, for Minecraft 1.17.1 / 1.18.2 / 1.19.2 /
1.19.4 / 1.20.1 / 1.20.4.** The mod itself is fully usable without any native library — the
native path is opt-in per resource pack.

## What's in this fork

### Native scripting bridge (in the mod jar)
- New `com.lx862.jcm.nativeapi.NativeScriptManager` (built into **both** the Fabric and Forge
  jars): loads C++ script modules declared in `mtr_custom_resources.json`
  (`"language": "cpp"`, `"nativeLibrary": "natives/libxxx.so|.dll|.dylib"`), keeps per-instance
  state blocks (same semantics as the JS `state` object), marshals POD snapshots and replays
  draw-call records through the exact same render pipeline the JS route uses.
- ABI v3 snapshots: route name / color, circular state, siding name, and per-stop station
  **exits** (name + destinations) are available to native scripts.

### C++ SDK (source + prebuilt libraries, in the release assets)
- `mtr-native-scripting-v3-sdk.zip` — full SDK: C ABI (`mtr_native.h`), C++ SDK headers
  (vehicle/pids/eyecandy wrappers, `gfx2d.hpp` Java2D-equivalent rasterizer, 5×7 + CJK fonts),
  the JNI bridge (`jni_bridge.cpp`), the smoke-test driver, and **5 example scripts**.
- `mtr-native-scripts-<platform>.zip` × 4 platforms — prebuilt script libraries
  (linux-x64 `.so`, windows-x64 `.dll`, macos-x64 / macos-arm64 `.dylib`), ready to drop into
  a resource pack's `assets/mtr/natives/`.

### Full port of the community JS LCD resource pack
- `jslcd_vehicle.cpp` (~1,400 lines): route header / circular route ring / linear map with
  alternating bilingual station names + transfer badges / door-open station page / 10 s
  full↔partial page cycling / exit panel — all ported from the original JavaScript.
- `jslcd_train_num.cpp`: siding-name train-number plates (head/tail + per-car L/R), with
  zero-redraw when the siding name is unchanged and bilingual error plates on car-count
  mismatch.
- Performance: steady-state **18.7 µs/frame with 0 uploads** (repaint-on-change with FNV
  content signatures), vs the JS version redrawing the full texture every frame; measured
  ~41× faster than JCM's default Rhino configuration, and a `Gfx2D` primitive fast path 23×
  faster than the naive port. An upstream aspect-ratio inversion bug in the original JS
  (`computeLcdAspectFromSlot`) was found and fixed during the port.

### 中文摘要
本 fork 在 JCM v2.3.0-beta.1 基础上加入 MTR 原生 C++ 脚本能力：mod 内置 JNI 桥（Fabric 与
Forge 双版本同步内置），资源包可声明 `"language": "cpp"` 的原生脚本；社区 JS LCD 资源包
（路线图 + 车号）已全量移植为 C++，稳态 0 重绘 0 上传（18.7 µs/帧），约为 Rhino 默认配置的
41 倍。Release 附带 4 平台预编译脚本库与完整 SDK 源码。

---

# JCM v2.3.0-beta.1 for MTR 4.0.5 has been released!

> **Beta Notice**
> 
> This is a beta release and is not recommended for stable deployment, nor is it intended for normal players to use. Please report any mod behaviors you feel could be improved.
> 
> Feature/implementation details may change throughout the beta lifecycle, any content you have made for this beta release may or may not break in the next version.


> This release contains breaking changes to JCM's internal codebase. Common addons have been tested against, however some addon depending on JCM may break.


## General
### New Blocks
- Add **Emergency Train Stop Button (Wall mounted, TML)** and **URL variant** (Thanks **LX9702**!)

### Slab Support for blocks
- The **Spot Lamp** block in JCM will now descend/ascend in accordance to slab blocks attached.
- The **Railway Sign Poles** in MTR now gained the ability to extend the pole according to the slab above.

### Fixes
- Fix JCM having an overly-long keybinding description, causing the Minecraft keybind page to shift outside the game window.
- Previously **Spot Lamp** will always prefer attaching to the top block if available. 
  - Now it will respect attaching to the side the player clicked on.
- **PIDS Projector** can now render further away before disappearing.
- Fix **Automatic Iron Door** detecting players in spectator mode as well.
- The playing mechanism for **Sound Looper** has been revised.
  - In JCM v1, the sounds are played to everyone across the server, which makes it a reliable source for playing all sorts of audio.
  - In JCM v2, it would only play to nearby players. Which means player may not be able to hear anything after getting in-range, until another loop occurs.
  - In this update, the range is now changed dynamically based on the duration. This allows short-form looping audio to play for nearby players, while having further range for long-form audio.
    - This should hopefully make sound looper more reliable.

## Technical

### PIDS Projector UI Improvement
It now supports real-time position/rotation preview, and you may now type the negative sign (-) as the first character.

### Railway Sign Text Coloring
Allow coloring text with the `textColor` field in the mtr_custom_resources.json "sign" section. Format is the same as `backgroundColor`.

### Lift Scripting
Lift Scripting has been added for review and feedback by the public, including rendering and sound playing.

See [JCM Docs](https://jcm.joban.org/v2.3/dev/scripting/type/lift/) for documentation.

### PIDS Scripting
- Add `TextWrapper.measureWidth()` to return the actual text width. Note that this cannot be chained for further usage, and must be invoked separately.
- Add `PIDSWrapper.isPlatformAutoDetected()`, returning whether the selected PIDS platform is manually picked by the user, or if it's automatically detected.
- Add `RectangleWrapper`, which is similar to `TextureWrapper` with the texture id pointing to `mtr:textures/block/white.png`
  - PIDS relying on a white texture previously should change to use `RectangleWrapper` (`Rectangle.`), as it is guaranteed the output will be a solid color, even if such texture is moved/no longer available in future Minecraft/MTR versions.

### General Scripting
#### API-related
- `Vector3f` now accepts TSC's `Position` class as a constructor
- Added the `ctx.setDebugInfo(value: any)` shorthand for temporary, single-value on-screen debug info, without requiring a key.
  - Same as calling `ctx.setDebugInfo("<Untitled>", value)` if called once.
  - Subsequent invocation will result in `<Untitled2>`, `<Untitled3>` and so on...
- Added `PlayerEntity.displayName()` to return the player's name with team prefixes.
- Added `PlayerEntity.isSpectator()` and `PlayerEntity.isCreative()`
- Added `MinecraftClientWrapper.getCurrentWorldId()` and `MinecraftClientWrapper.getWorldBlockState()`, allowing for better environmental context.
- Added the `MTRWrapper` class (Referenced using `MTR` in scripts).
  - This is a wrapper for various MTR utilities and data obtaining functions. (`MTR.ClientConfig`, `MTR.Data`)
  - It aims to reduce dependency on `MTRClientData` and other internal MTR class access, as there may be breaking changes made in MTR 4.1.
  - Unlike `MTRClientData`, backward compatibility will be considered in a best-effort basis, to ensure existing scripts do not break badly.
    - Note: Returned type from TSC is still vulnerable, however from observations it is more stablized than the MTR Mod's code.

#### Surrounding Changes
- The `create()` function is changed to be re-invoked again after an execution error, instead of continuing towards `render()` function, where not all variable may be initialized, obscuring the original error in the create function.
- Added `isScriptRendered` field for eyecandy and lift entry, which allows script to fully take over the rendering, without MTR's default renderer.
  - This is useful for lifts whose model is solely rendered by scripts, as well as eyecandy model that desires a generic model fallback when JCM scripting is not available.
- **Eyecandy**
  - The custom config NBT tags by ANTE will now be preserved.
  - The eyecandy model select UI is now overwritten by JCM in preparation for eyecandy custom config.
    - Though I have other plans for the UI, so it will likely be removed in future releases.
- When script debug mode is enabled, in-game script parsing error messages will now display on the first-time you join the game.
  - Previously this will cause errored scripts to be missed on launch.
- **Script Debug Overlay**
  - "More relevant" instances are now sorted at the top for ease of visualization (Usually as one is closer to an object)
    - If riding a scripted lift or scripted vehicle, it will always be sorted to the top as it's of most-relevance.
  - Order of script debug info is now always arranged according to the script execution order.

### PIDS Textures
Please note that several textures used by PIDS (`rv_door_cls_apg.png`, `rv_door_cls_psd.png`, `rv_door_cls_train.png`, `thumbnail/pids_1a.png`) has been relocated from `jsblock:textures/block/pids` to `jsblock:textures/pids`.

It is done this way to avoid Minecraft including the texture to the block texture atlas, resulting in unnecessary overhead/enlarging of the block texture size.

## Translations
Thanks to the following people who have contributed translations for this release! (No particular order):
- Smile Wood (zh_cn)
- DelphoxOTS (ja_jp)
- Fed (it_it)

Translations for JCM is now hosted on [ZiYue's Weblate](https://weblate.ziyuesinicization.site/projects/joban-client-mod/)

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)