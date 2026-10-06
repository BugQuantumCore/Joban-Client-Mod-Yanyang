package com.lx862.jcm.nativeapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.ResourceManagerHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NativeScriptManager — JNI host for C++ script modules.
 *
 * Drop-in companion to JCM v2.3's ScriptManager/Rhino pipeline:
 * resource packs declare native scripts in mtr_custom_resources.json
 * ...
 *
 *   "vehicleScripts": [{
 *       "id": "demo:kcx_lcd",
 *       "language": "cpp",
 *
 *       // Form A — platform-agnostic stem: resolves on EVERY OS via the
 *       // natives/{platform}/ convention (see resolveNativeLibraryPath):
 *       "nativeLibrary": "demo:natives/kcx_lcd"
 *       //     -> demo:natives/linux-x64/libkcx_lcd.so
 *       //     -> demo:natives/windows-x64/kcx_lcd.dll
 *       //     -> demo:natives/macos-arm64/libkcx_lcd.dylib
 *
 *       // Form B — per-platform map: declare exactly which library runs
 *       // on which OS. Keys: "windows" / "linux" / "macos", optionally
 *       // arch-specific ("windows-x64", "linux-x64", "macos-x64",
 *       // "macos-arm64") which take precedence over the generic key.
 *       // Values may again be full paths (with extension) or stems.
 *       "nativeLibrary": {
 *           "windows":    "demo:natives/windows-x64/kcx_lcd.dll",
 *           "linux":      "demo:natives/linux-x64/libkcx_lcd.so",
 *           "macos-arm64": "demo:natives/macos-arm64/libkcx_lcd.dylib"
 *       }
 *   }]
 *
 * The running OS/arch is detected once from os.name / os.arch
 * (see getPlatformKey / getPlatformKeyExact). When the current platform
 * has no entry (Form B without a matching key, or an extension-form
 * string for another OS), the script is NOT executed on this system:
 * a one-line hint goes to latest.log (JCMLogger) and, when JCM's
 * scripting debug mode is enabled, to the in-game debug HUD/chat via
 * the DebugNotifier hook wired by MtrScriptingResourceManager.
 *
 * ... and the SAME mixins that drive JS scripts (RenderVehiclesMixin,
 * RenderEyeCandyMixin, ScriptPIDSPreset) dispatch to this manager when
 * the script entry is native. Draw-call records produced by the module
 * are translated into ScriptRenderManager draw calls, so the capture-
 * and-replay path on the render thread is byte-for-byte identical to
 * the JS route — JS and native scripts coexist in the same world.
 *
 * Performance model (see native/bench for the measured numbers):
 *   - ONE JNI boundary crossing per frame per instance: the snapshot
 *     is marshalled into a direct ByteBuffer, mtrRender runs, and the
 *     frame records come back in the same call.
 *   - No reflection, no boxing, no per-call Java object allocation on
 *     the script path; the module bump-allocates records internally.
 *
 * This file is reference integration code: it compiles against the
 * MTR/JCM mappings used by JCM v2.3 plus the JNI headers shipped with
 * the JDK (native/jni_bridge.cpp implements the C side).
 */
public final class NativeScriptManager {
    private static final int ABI_VERSION = 3; /* must match mtr_native.h */

    /* Route.CircularState mapping (v2 snapshots). */
    public static final int CIRCULAR_NONE = 0;
    public static final int CIRCULAR_CLOCKWISE = 1;
    public static final int CIRCULAR_ANTICLOCKWISE = 2;

    /* Loaded modules keyed by script id. */
    private static final Map<String, NativeScriptModule> MODULES = new ConcurrentHashMap<>();

    /* Platform-skipped declarations keyed by script id (reason string).
       Filled by loadFromDeclaration when the running OS/arch has no
       nativeLibrary entry — consumed by validate() messages and the
       debug-mode overlay (MTRScriptDebugOverlay). */
    private static final Map<String, String> SKIPPED = new ConcurrentHashMap<>();

    /* Per-instance state blocks (mirrors JCM's per-instance `state`
       JS object): instanceKey -> zeroed direct ByteBuffer. */
    private static final Map<String, ByteBuffer> INSTANCE_STATES = new ConcurrentHashMap<>();

    private static final int SNAPSHOT_CAPACITY = 16 * 1024; /* generous worst case */

    /**
     * DebugNotifier — pluggable sink for "JCM debug mode" hints. The
     * mtrscripting layer wires this to scriptErrorNotifier (yellow chat
     * line) in MtrScriptingResourceManager.reload(); the debug overlay
     * additionally renders SKIPPED entries directly. Keeps this class
     * free of direct client/gui dependencies.
     */
    public interface DebugNotifier {
        void accept(String message);
    }

    private static volatile DebugNotifier debugNotifier;

    private NativeScriptManager() {}

    /* ------------------------------------------------------------------ */
    /* Platform detection                                                  */
    /* ------------------------------------------------------------------ */

    /** Generic platform key: "windows" | "linux" | "macos". */
    public static String getPlatformKey() {
        final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "windows";
        if (os.contains("mac") || os.contains("darwin")) return "macos";
        return "linux";
    }

    /**
     * Arch-specific platform key: "windows-x64" | "linux-x64" |
     * "macos-x64" | "macos-arm64". Matches the directory convention
     * used by resolveNativeLibraryPath and resource packs.
     */
    public static String getPlatformKeyExact() {
        final String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (getPlatformKey().equals("macos")) {
            return arch.contains("aarch64") || arch.contains("arm64") ? "macos-arm64" : "macos-x64";
        }
        return getPlatformKey() + "-x64";
    }

    /** Human-readable OS description for log/overlay messages. */
    public static String getOsDescription() {
        return System.getProperty("os.name", "?") + " (" + System.getProperty("os.arch", "?") + ")";
    }

    /* ------------------------------------------------------------------ */
    /* Loading                                                             */
    /* ------------------------------------------------------------------ */

    public static void reload() {
        MODULES.values().forEach(NativeScriptModule::dispose);
        MODULES.clear();
        SKIPPED.clear();
        INSTANCE_STATES.clear();
        /* Actual discovery walks the same mtr_custom_resources.json
           entries as MTRContentResourceManager, filtering
           language == "cpp", and calls load(...) below. */
    }

    /**
     * Extract the native library from the resource pack (resource
     * packs can't be dlopen'd from inside a jar/zip) and load it.
     */
    public static NativeScriptModule load(String scriptId, String libraryPath) {
        try {
            final Identifier libId = new Identifier(libraryPath);
            final byte[] lib = readResourceBytes(libId);
            if (lib == null || lib.length == 0) {
                JCMLogger.warn("Native script library {} not found!", libraryPath);
                return null;
            }

            /* Extract to a temp file — dlopen needs a real file. */
            final String realName = libId.getPath().substring(libId.getPath().lastIndexOf('/') + 1);
            final Path extracted = Files.createTempFile("jcm_native_" + sanitize(scriptId) + "_", "_" + realName);
            Files.write(extracted, lib);
            extracted.toFile().deleteOnExit();

            final NativeScriptModule module = new NativeScriptModule(scriptId, extracted.toAbsolutePath().toString());
            if (module.abiVersion != ABI_VERSION) {
                JCMLogger.error("Native script {} has ABI version {} (host {}), refusing to load.",
                        scriptId, module.abiVersion, ABI_VERSION);
                module.dispose();
                return null;
            }
            MODULES.put(scriptId, module);
            return module;
        } catch (IOException e) {
            JCMLogger.error("Failed to load native script {}: {}", scriptId, e.getMessage());
            return null;
        }
    }

    private static byte[] readResourceBytes(Identifier id) {
        /* Binary-safe read: readResource() decodes as text (String),
           which corrupts .so/.dll/.dylib bytes through a UTF-8
           round-trip. readAllResources hands us the raw stream. */
        final byte[][] sink = new byte[1][];
        ResourceManagerHelper.readAllResources(id, inputStream -> {
            try (java.io.InputStream in = inputStream) {
                sink[0] = in.readAllBytes();
            } catch (IOException e) {
                JCMLogger.error("Failed to read native library {}: {}", id, e.getMessage());
            }
        });
        return sink[0];
    }

    /**
     * Entry point used by the resource providers: take the raw
     * nativeLibrary JSON declaration, auto-detect the running
     * OS/arch, resolve the concrete library and load it.
     *
     * When this platform is not declared (per-platform map without a
     * matching key, or an extension-form path for another OS), the
     * script is skipped: NOT executed on this system, the reason is
     * recorded in SKIPPED, a hint goes to latest.log, and — when JCM
     * scripting debug mode is on — to the in-game debug interface
     * through the DebugNotifier hook.
     *
     * @return the loaded module, or null when skipped/failed.
     */
    public static NativeScriptModule loadFromDeclaration(String scriptId, JsonElement declared) {
        final String resolved = resolvePlatformLibrary(declared);
        if (resolved == null) {
            skipScript(scriptId, declared);
            return null;
        }
        final NativeScriptModule module = load(scriptId, resolved);
        if (module == null) {
            /* load() already logged the concrete failure (missing
               file / ABI mismatch); keep it visible in the debug
               overlay too. */
            SKIPPED.put(scriptId, "failed to load " + resolved + " (see latest.log)");
        }
        return module;
    }

    private static void skipScript(String scriptId, JsonElement declared) {
        final String declaredText = declared == null || declared.isJsonNull()
                ? "(no nativeLibrary declared)"
                : declared.toString();
        final String reason = String.format(
                "no nativeLibrary entry for %s (%s) — the script will not run on this system. " +
                "Add a \"%s\" (or \"%s\") entry to its mtr_custom_resources.json declaration, " +
                "or use a platform-agnostic library path.",
                getPlatformKeyExact(), getOsDescription(), getPlatformKey(), getPlatformKeyExact());
        SKIPPED.put(scriptId, reason);

        /* latest.log — always. */
        JCMLogger.warn("Native script {} skipped on {}: {}", scriptId, getPlatformKeyExact(), reason);
        JCMLogger.warn("  nativeLibrary declaration was: {}", declaredText);

        /* JCM debug mode interface (chat + overlay) — only when the
           debug notifier is wired and debug mode is enabled. */
        notifyDebug(String.format("Native script %s skipped: %s", scriptId, reason));
    }

    private static void notifyDebug(String message) {
        final DebugNotifier notifier = debugNotifier;
        if (notifier != null) {
            notifier.accept(message);
        }
    }

    /** Wire the debug-mode sink; called by MtrScriptingResourceManager. */
    public static void setDebugNotifier(DebugNotifier notifier) {
        debugNotifier = notifier;
    }

    /* ------------------------------------------------------------------ */
    /* Platform-aware declaration resolution                               */
    /* ------------------------------------------------------------------ */

    /**
     * Resolve a nativeLibrary declaration for the RUNNING platform.
     * Accepted forms:
     *
     *  - "ns:path/libfoo.so|.dll|.dylib"  full path: only runs on the
     *    OS implied by the extension; other systems skip the script.
     *  - "ns:path/foo"                    platform-agnostic stem:
     *    expanded via the natives/{platform}/ convention for every OS.
     *  - {"windows": ..., "linux": ..., "macos": ...}  per-platform
     *    map; arch-specific keys ("macos-arm64", ...) win over the
     *    generic key; values may be full paths or stems.
     *
     * @return the concrete resource path for this OS/arch, or null
     *         when this platform is not declared (caller must skip
     *         the script and surface the hint).
     */
    public static String resolvePlatformLibrary(JsonElement declared) {
        if (declared == null || declared.isJsonNull()) {
            return null;
        }
        if (declared.isJsonPrimitive()) {
            final String value = declared.getAsString();
            final String extPlatform = platformOfExtension(value);
            if (extPlatform != null) {
                /* Extension-form: bound to the OS the extension
                   implies — auto-detected, other platforms skip. */
                return getPlatformKey().equals(extPlatform) ? value : null;
            }
            return resolveNativeLibraryPath(value);
        }
        if (declared.isJsonObject()) {
            final JsonObject map = declared.getAsJsonObject();
            /* Arch-specific key first, then the generic key, then
               common aliases. */
            JsonElement hit = map.get(getPlatformKeyExact());
            if (hit == null || !hit.isJsonPrimitive()) {
                hit = map.get(getPlatformKey());
            }
            if ((hit == null || !hit.isJsonPrimitive()) && getPlatformKey().equals("windows")) {
                hit = map.get("win");
            }
            if ((hit == null || !hit.isJsonPrimitive()) && getPlatformKey().equals("macos")) {
                hit = map.has("osx") ? map.get("osx") : map.get("darwin");
            }
            if (hit == null || !hit.isJsonPrimitive() || hit.isJsonNull()) {
                return null;
            }
            final String value = hit.getAsString();
            if (platformOfExtension(value) != null) {
                return value;
            }
            return resolveNativeLibraryPath(value);
        }
        return null;
    }

    /** Map a library file extension to its platform key, or null. */
    private static String platformOfExtension(String path) {
        final String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".dll")) return "windows";
        if (lower.endsWith(".dylib")) return "macos";
        if (lower.endsWith(".so")) return "linux";
        return null;
    }

    /**
     * Resolve a platform-agnostic nativeLibrary declaration to the
     * concrete resource path for the running OS/arch. Convention:
     *
     *   "yanyang:natives/jslcd_vehicle"
     *     -> yanyang:natives/linux-x64/libjslcd_vehicle.so
     *     -> yanyang:natives/windows-x64/jslcd_vehicle.dll
     *     -> yanyang:natives/macos-arm64/libjslcd_vehicle.dylib
     *
     * A value that already carries a library extension is used as-is
     * (single-platform packs stay valid).
     */
    public static String resolveNativeLibraryPath(String declared) {
        final String lower = declared.toLowerCase();
        if (lower.endsWith(".so") || lower.endsWith(".dll") || lower.endsWith(".dylib")) {
            return declared;
        }
        final String os = System.getProperty("os.name", "").toLowerCase();
        final String arch = System.getProperty("os.arch", "");
        final String osDir;
        final String ext;
        final String prefix;
        if (os.contains("win")) { osDir = "windows-x64"; ext = ".dll"; prefix = ""; }
        else if (os.contains("mac") || os.contains("darwin")) {
            osDir = "aarch64".equals(arch) ? "macos-arm64" : "macos-x64";
            ext = ".dylib"; prefix = "lib";
        }
        else { osDir = "linux-x64"; ext = ".so"; prefix = "lib"; }
        final int colon = declared.indexOf(':');
        final String ns = colon >= 0 ? declared.substring(0, colon) : "minecraft";
        final String base = colon >= 0 ? declared.substring(colon + 1) : declared;
        final String file = base.substring(base.lastIndexOf('/') + 1);
        final String dir = base.contains("/") ? base.substring(0, base.lastIndexOf('/') + 1) : "";
        return ns + ':' + dir + osDir + '/' + prefix + file + ext;
    }

    private static String sanitize(String id) {
        return id.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /* ------------------------------------------------------------------ */
    /* Registries (validate() + debug overlay)                             */
    /* ------------------------------------------------------------------ */

    public static boolean isLoaded(String scriptId) {
        return MODULES.containsKey(scriptId);
    }

    public static boolean isSkipped(String scriptId) {
        return SKIPPED.containsKey(scriptId);
    }

    /** Unmodifiable view of loaded modules, for the debug overlay. */
    public static Map<String, NativeScriptModule> getModules() {
        return Collections.unmodifiableMap(MODULES);
    }

    /** Unmodifiable view of skipped declarations (id -> reason). */
    public static Map<String, String> getSkippedScripts() {
        return Collections.unmodifiableMap(SKIPPED);
    }

    /* ------------------------------------------------------------------ */
    /* Per-frame driving (called by the same mixins as JS scripts)        */
    /* ------------------------------------------------------------------ */

    /**
     * One frame of a vehicle script. Mirrors RenderVehiclesMixin:
     * build the snapshot, run mtrRender, translate records.
     *
     * @return frame records for replay, or null on cooldown/error.
     */
    public static NativeFrame renderVehicle(String instanceKey, String scriptId,
                                             VehicleSnapshotBuilder snapshot) {
        final NativeScriptModule module = MODULES.get(scriptId);
        if (module == null || !"vehicle".equals(module.scriptType)) return null;

        final ByteBuffer snapshotBuf = snapshot.build(
                ByteBuffer.allocateDirect(SNAPSHOT_CAPACITY).order(ByteOrder.nativeOrder()));

        return module.render(instanceKey, snapshotBuf, MTR_RESOURCE_VEHICLE);
    }

    /* Resource kind ids, must match MtrResourceKind in mtr_native.h. */
    public static final int MTR_RESOURCE_VEHICLE = 0;
    public static final int MTR_RESOURCE_PIDS = 1;
    public static final int MTR_RESOURCE_EYECANDY = 2;

    /**
     * NativeFrame — draw-call records + arenas read back from the
     * module. The replay step (render thread) walks the records and
     * creates the same MainRenderer.scheduleRender calls the JS
     * pipeline issues; see native/jni_bridge.cpp for the struct
     * layouts (they are plain C structs over this ByteBuffer).
     */
    public static final class NativeFrame {
        public final ByteBuffer records;   /* JcmDraw* array */
        public final int recordCount;
        public final ByteBuffer stringArena;
        public final ByteBuffer pixelArena;
        public final ByteBuffer matrixArena;

        NativeFrame(ByteBuffer records, int recordCount, ByteBuffer stringArena,
                    ByteBuffer pixelArena, ByteBuffer matrixArena) {
            this.records = records;
            this.recordCount = recordCount;
            this.stringArena = stringArena;
            this.pixelArena = pixelArena;
            this.matrixArena = matrixArena;
        }
    }

    /**
     * VehicleSnapshotBuilder — flattens VehicleWrapper's getters into
     * the JcmVehicleSnapshot POD. This replaces the hundreds of
     * reflective NativeJavaObject getter calls the JS path makes per
     * frame with a single sequential write.
     *
     * v2 (ABI 2) fields the builder must write before string_pool:
     *   route_name_offset/len  — thisRouteStops.get(0).route.name
     *   route_color            — thisRouteStops.get(0).route.color (ARGB)
     *   circular_state          — same route's CircularState
     *                             (CIRCULAR_* constants above), which
     *                             native scripts use for the 环线/直线
     *                             LCD branch selection (see
     *                             examples/jslcd_vehicle.cpp, the port
     *                             of the community JS LCD script).
     *
     * v3 (ABI 3) per-stop fields (JcmStop, in this order):
     *   exit_count / exit_offset — station.getExits() flattened as a
     *                             JcmExit[] pool; each JcmExit carries
     *                             name + a JcmStrRef[] destination list
     *                             (station.getExits().get(k)
     *                             .getName() / .getDestinations()).
     *                             Write 0/0 when the station exposes no
     *                             exits — the native draw_exit_info port
     *                             bails out identically to the JS guard
     *                             `if (!exits || exits.length === 0)`.
     */
    public interface VehicleSnapshotBuilder {
        ByteBuffer build(ByteBuffer out);
    }

    /* ------------------------------------------------------------------ */
    /* Module wrapper                                                      */
    /* ------------------------------------------------------------------ */

    public static final class NativeScriptModule {
        static {
            /* The C-side dispatcher lives in the host bridge library,
               not in each script module (keeps exports per module to
               the 5 mtr* functions declared in mtr_native.h). */
            System.loadLibrary("jcm_native_bridge");
        }

        private long nativeHandle;       /* dlopen handle */

        final int abiVersion;
        final String scriptType;
        final String scriptId;
        final long stateSize;
        /** Platform the module was loaded on (for the debug overlay). */
        final String loadedPlatform;
        /** Resolved library resource path (for the debug overlay). */
        final String sourcePath;

        private native long nOpen(String path);
        private native void nClose(long handle);
        private native int nAbiVersion(long handle);
        private native String nScriptType(long handle);
        private native String nScriptId(long handle);
        private native long nStateSize(long handle);
        private native NativeFrame nRender(long handle, String instanceKey,
                                           ByteBuffer snapshot, int resourceKind);

        NativeScriptModule(String scriptId, String path) {
            this.nativeHandle = nOpen(path);
            this.abiVersion = nAbiVersion(nativeHandle);
            this.scriptType = nScriptType(nativeHandle);
            this.scriptId = nScriptId(nativeHandle);
            this.stateSize = nStateSize(nativeHandle);
            this.loadedPlatform = getPlatformKeyExact();
            this.sourcePath = path;
            if (!scriptId.equals(this.scriptId)) {
                JCMLogger.warn("Native library declares id {} but was loaded as {}",
                        this.scriptId, scriptId);
            }
        }

        public String getScriptId() {
            return scriptId;
        }

        public String getScriptType() {
            return scriptType;
        }

        public int getAbiVersion() {
            return abiVersion;
        }

        public long getStateSize() {
            return stateSize;
        }

        public String getLoadedPlatform() {
            return loadedPlatform;
        }

        public String getSourcePath() {
            return sourcePath;
        }

        NativeFrame render(String instanceKey, ByteBuffer snapshot, int resourceKind) {
            /* Per-instance state block, zeroed once (mirrors the JS
               `state` object created per ScriptInstance). */
            INSTANCE_STATES.computeIfAbsent(instanceKey,
                    k -> ByteBuffer.allocateDirect((int) Math.max(1, stateSize))
                                   .order(ByteOrder.nativeOrder()));

            return nRender(nativeHandle, instanceKey, snapshot, resourceKind);
        }

        void dispose() {
            if (nativeHandle != 0) {
                nClose(nativeHandle);
                nativeHandle = 0;
            }
        }
    }
}
