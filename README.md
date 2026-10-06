<p align="center">
  <img src="assets/JCM_logo.png" width="180px" alt="JCM Logo">
</p>

<h1 align="center">
  Joban Client Mod
</h1>

<p align="center">Bring your transit world to life with various decoration blocks!</p>

<a align="center" href="https://weblate.ziyuesinicization.site/engage/joban-client-mod/"><img src="https://weblate.ziyuesinicization.site/widget/joban-client-mod/svg-badge.svg" alt="Translation status"></a>

> [!IMPORTANT]
> **Joban-Client-Mod-Yanyang** — this fork adds **C++ native scripting** for
> Minecraft Transit Railway 4.0 `vehicle` / `eye_candy` / `pids` resources,
> ported 1:1 from the v2.3 JavaScript (Rhino) scripting surface.
> See [`native/README.md`](native/README.md) for the SDK, examples
> (including the full port of the community JS LCD + 车号 pack), benchmarks
> (C++ ~6.8 µs/frame vs Rhino ~280 µs) and the JNI integration
> ([NativeScriptManager.java](fabric/src/main/java/com/lx862/jcm/nativeapi/NativeScriptManager.java)).
> Resource packs declare `"language": "cpp"` and ship a prebuilt library —
> JS and C++ scripts coexist on the same capture-and-replay pipeline.


<p align="center">
  <a href="https://modrinth.com/mod/jcm">
    <img alt="Available on Modrinth" height="50" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg">
  </a>
  <a href="https://discord.com/invite/FNc2rgWmP2">
    <img alt="Chat with us on Discord" height="50" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg">
  </a>
</p>

Joban Client Mod is an addon mod for the **Minecraft Transit Railway 4.0**, adding various blocks from **Emergency stop button**, **Helpline** to fully customizable Passenger Information Display System and more!  

This repository contains the source code for Joban Client Mod.

## Links
- [Website & Wiki](https://jcm.joban.org)

## License
This project is licensed under the MIT License.
