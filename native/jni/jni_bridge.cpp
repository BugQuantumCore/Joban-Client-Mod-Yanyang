/**
 * jni_bridge.cpp — C-side dispatcher behind NativeScriptManager.java.
 *
 * Java side (fabric/src/main/java/com/lx862/jcm/nativeapi/
 * NativeScriptManager.java) does:
 *
 *     static { System.loadLibrary("jcm_native_bridge"); }
 *     private native long   nOpen(String path);
 *     private native void   nClose(long handle);
 *     private native int    nAbiVersion(long handle);
 *     private native String nScriptType(long handle);
 *     private native String nScriptId(long handle);
 *     private native long   nStateSize(long handle);
 *     private native NativeFrame nRender(long handle, String instanceKey,
 *                                        ByteBuffer snapshot, int resourceKind);
 *     private native void   nDisposeInstance(long handle, String instanceKey);
 *
 * This file implements exactly those entry points on top of the C ABI in
 * include/mtr/mtr_native.h:
 *
 *   nOpen      — dlopen the script module, resolve the 5 mtr* exports,
 *                keep a per-instance state map (one zeroed block of
 *                mtrStateSize() bytes per instanceKey, mirroring JCM's
 *                per-ScriptInstance `state` JS object).
 *   nRender    — ONE boundary crossing per frame:
 *                  direct ByteBuffer snapshot → JcmFrameInput (the last
 *                  snapshot is kept per instance so dispose can rebuild
 *                  the full input), lazy mtrCreate on first use,
 *                  mtrRender, then the output arenas are wrapped as NIO
 *                  ByteBuffers into a NativeFrame — zero copies.
 *   nDisposeInstance / nClose — mtrDispose for one / all instances,
 *                then dlclose.
 *
 * Host services (reference implementation): model/texture handles are
 * opaque ids handed out by this bridge (a production host replaces these
 * with real ModelJS / GraphicsTexture registries through its own JNI
 * callbacks — the function-table shape is identical); rasterize_text
 * returns -1 so scripts fall back to their built-in bitmap font; log
 * goes to stderr.
 *
 * Threading: JCM serializes script execution per module on one
 * background executor (same model as the JS pipeline); the bridge adds
 * a per-module mutex so reload paths stay safe.
 *
 * Build (per platform):
 *   Linux   : g++ -std=c++17 -O2 -fPIC -shared -I<java_include> \
 *             -I<java_include>/linux -I../include jni_bridge.cpp \
 *             -o libjcm_native_bridge.so -ldl
 *   Windows : cl /LD /I<java_include> /I<java_include>\win32 ... → jcm_native_bridge.dll
 *   macOS   : clang++ ... → libjcm_native_bridge.dylib
 * (CI builds all three — see .github/workflows/native-build.yml)
 *
 * NOTE: the output arenas are owned by the module and reused next frame;
 * the Java replay step must finish before the next nRender call (this is
 * the same capture-then-replay discipline JCM's ScriptRenderManager
 * already enforces on the render thread).
 */

#include <jni.h>
#include <mtr/mtr_native.h>

#include <cstdio>
#include <cstring>
#include <mutex>
#include <new>
#include <string>
#include <unordered_map>
#include <vector>

#if defined(_WIN32)
#include <windows.h>
#else
#include <dlfcn.h>
#endif

/* ------------------------------------------------------------------ */
/* module + instance bookkeeping                                       */
/* ------------------------------------------------------------------ */

namespace {

struct Instance {
    std::vector<uint8_t> state;         /* mtrStateSize() zeroed block  */
    std::vector<uint8_t> last_snapshot; /* for safe dispose() rebuild   */
    bool created = false;               /* mtrCreate called?            */
};

struct BridgeModule {
    void* dl = nullptr;

    uint32_t (*abi)(void) = nullptr;
    const char* (*type)(void) = nullptr;
    const char* (*id)(void) = nullptr;
    size_t (*state_size)(void) = nullptr;
    int32_t (*create)(const JcmFrameInput*) = nullptr;
    int32_t (*render)(const JcmFrameInput*, JcmFrameOutput*) = nullptr;
    int32_t (*dispose)(const JcmFrameInput*) = nullptr;

    std::mutex mutex;
    std::unordered_map<std::string, Instance> instances;

    /* host-side opaque handle registries (reference implementation) */
    int32_t next_texture = 1;
    int32_t next_model = 100;

    ~BridgeModule() { close_library(); }

    void close_library() {
        if (dl) {
#if defined(_WIN32)
            FreeLibrary(static_cast<HMODULE>(dl));
#else
            dlclose(dl);
#endif
            dl = nullptr;
        }
    }
};

/* ---- host services handed to the script module (see mtr_native.h) ---- */

int32_t host_acquire_model(void* user, const char* path) {
    (void)path;
    return static_cast<BridgeModule*>(user)->next_model++;
}
void host_release_model(void*, int32_t) {}
int32_t host_create_texture(void* user, int32_t w, int32_t h) {
    (void)w; (void)h;
    return static_cast<BridgeModule*>(user)->next_texture++;
}
void host_release_texture(void*, int32_t) {}

/* No host TTF rasterization in the reference bridge: scripts fall back
   to their built-in 5x7 bitmap font (returns -1 = "not covered"). */
int32_t host_rasterize_text(void*, const char*, int32_t, int32_t, int32_t,
                            int32_t, uint8_t, uint8_t, uint8_t, uint8_t*,
                            int32_t, int32_t) {
    return -1;
}

void host_log(void*, int32_t level, const char* utf8, int32_t len) {
    std::fprintf(stderr, "[native-script/%d] %.*s\n", level, len, utf8);
}

void fill_host(JcmHostServices& host, BridgeModule* mod) {
    std::memset(&host, 0, sizeof(host));
    host.user = mod;
    host.acquire_model = host_acquire_model;
    host.release_model = host_release_model;
    host.create_texture = host_create_texture;
    host.release_texture = host_release_texture;
    host.rasterize_text = host_rasterize_text;
    host.log = host_log;
}

#if !defined(_WIN32)
void* load_library(const char* path) { return dlopen(path, RTLD_NOW | RTLD_LOCAL); }
void* load_symbol(void* lib, const char* name) { return dlsym(lib, name); }
const char* last_error() { const char* e = dlerror(); return e ? e : "unknown"; }
#else
void* load_library(const char* path) {
    return static_cast<void*>(LoadLibraryA(path));
}
void* load_symbol(void* lib, const char* name) {
    return reinterpret_cast<void*>(GetProcAddress(static_cast<HMODULE>(lib), name));
}
const char* last_error() { return "LoadLibrary/GetProcAddress failed"; }
#endif

#define JNI_CLASS_PATH com_lx862_jcm_nativeapi_NativeScriptManager_00024NativeScriptModule
#define JNI_NAME(name) Java_##JNI_CLASS_PATH##_##name

} /* anonymous namespace */

/* ------------------------------------------------------------------ */
/* lifecycle natives                                                   */
/* ------------------------------------------------------------------ */

extern "C" JNIEXPORT jlong JNICALL
JNI_NAME(nOpen)(JNIEnv* env, jobject, jstring path) {
    const char* utf = env->GetStringUTFChars(path, nullptr);
    if (!utf) return 0;
    void* lib = load_library(utf);
    if (!lib) {
        std::fprintf(stderr, "[jcm_native_bridge] dlopen failed: %s (%s)\n",
                     utf, last_error());
        env->ReleaseStringUTFChars(path, utf);
        return 0;
    }
    env->ReleaseStringUTFChars(path, utf);

    auto* mod = new (std::nothrow) BridgeModule();
    if (!mod) return 0;
    mod->dl = lib;

    mod->abi        = reinterpret_cast<uint32_t (*)(void)>(load_symbol(lib, "mtrNativeAbiVersion"));
    mod->type       = reinterpret_cast<const char* (*)(void)>(load_symbol(lib, "mtrScriptType"));
    mod->id         = reinterpret_cast<const char* (*)(void)>(load_symbol(lib, "mtrScriptId"));
    mod->state_size = reinterpret_cast<size_t (*)(void)>(load_symbol(lib, "mtrStateSize"));
    mod->create     = reinterpret_cast<int32_t (*)(const JcmFrameInput*)>(load_symbol(lib, "mtrCreate"));
    mod->render     = reinterpret_cast<int32_t (*)(const JcmFrameInput*, JcmFrameOutput*)>(load_symbol(lib, "mtrRender"));
    mod->dispose    = reinterpret_cast<int32_t (*)(const JcmFrameInput*)>(load_symbol(lib, "mtrDispose"));

    if (!mod->abi || !mod->type || !mod->id || !mod->state_size ||
        !mod->create || !mod->render || !mod->dispose) {
        std::fprintf(stderr, "[jcm_native_bridge] module misses mtr* exports\n");
        mod->close_library();
        delete mod;
        return 0;
    }
    return reinterpret_cast<jlong>(mod);
}

extern "C" JNIEXPORT jint JNICALL
JNI_NAME(nAbiVersion)(JNIEnv*, jobject, jlong handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    return mod ? static_cast<jint>(mod->abi()) : 0;
}

extern "C" JNIEXPORT jstring JNICALL
JNI_NAME(nScriptType)(JNIEnv* env, jobject, jlong handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    return (mod && mod->type) ? env->NewStringUTF(mod->type()) : nullptr;
}

extern "C" JNIEXPORT jstring JNICALL
JNI_NAME(nScriptId)(JNIEnv* env, jobject, jlong handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    return (mod && mod->id) ? env->NewStringUTF(mod->id()) : nullptr;
}

extern "C" JNIEXPORT jlong JNICALL
JNI_NAME(nStateSize)(JNIEnv*, jobject, jlong handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    return mod ? static_cast<jlong>(mod->state_size()) : 0;
}

/* ------------------------------------------------------------------ */
/* per-frame render                                                    */
/* ------------------------------------------------------------------ */

extern "C" JNIEXPORT jobject JNICALL
JNI_NAME(nRender)(JNIEnv* env, jobject, jlong handle, jstring instanceKey,
                  jobject snapshotBuf, jint resourceKind) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    if (!mod || !instanceKey || !snapshotBuf) return nullptr;

    const char* key = env->GetStringUTFChars(instanceKey, nullptr);
    if (!key) return nullptr;

    /* one JNI round-trip per frame: everything below runs under the
       module lock, mirroring JCM's per-script single-thread executor */
    std::lock_guard<std::mutex> lock(mod->mutex);

    Instance& inst = mod->instances[key];
    env->ReleaseStringUTFChars(instanceKey, key);

    if (inst.state.empty() && mod->state_size() > 0) {
        inst.state.assign(mod->state_size(), 0);
    }

    /* snapshot: direct ByteBuffer written sequentially by the Java side */
    const void* snap = env->GetDirectBufferAddress(snapshotBuf);
    const jlong snap_len = env->GetDirectBufferCapacity(snapshotBuf);
    if (!snap || snap_len <= 0) return nullptr;
    inst.last_snapshot.assign(static_cast<const uint8_t*>(snap),
                              static_cast<const uint8_t*>(snap) + snap_len);

    JcmHostServices host;
    fill_host(host, mod);

    JcmFrameInput in{};
    in.abi_version = MTR_NATIVE_ABI_VERSION;
    in.resource_kind = resourceKind;
    in.snapshot = inst.last_snapshot.data();
    in.state = inst.state.data();
    in.state_size = inst.state.size();
    in.host = &host;

    if (!inst.created) {
        if (mod->create(&in) != 0) {
            /* same failure semantics as a JS script that throws in
               create(): drop the instance, let the host cool it down */
            return nullptr;
        }
        inst.created = true;
    }

    JcmFrameOutput out{};
    if (mod->render(&in, &out) != 0) return nullptr;

    /* wrap the module-owned arenas as NIO ByteBuffers (zero copy).
       Valid until the next nRender on this module — the replay step
       must complete first (same discipline as the JS pipeline). */
    jclass frameCls = env->FindClass(
        "com/lx862/jcm/nativeapi/NativeScriptManager$NativeFrame");
    if (!frameCls) return nullptr;
    jmethodID ctor = env->GetMethodID(
        frameCls, "<init>",
        "(Ljava/nio/ByteBuffer;ILjava/nio/ByteBuffer;"
        "Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)V");
    if (!ctor) return nullptr;

    jobject records = env->NewDirectByteBuffer(
        const_cast<void*>(out.records), out.records_len);
    jobject strings = env->NewDirectByteBuffer(
        out.string_arena ? const_cast<char*>(out.string_arena) : nullptr,
        out.string_arena_len);
    jobject pixels = env->NewDirectByteBuffer(
        const_cast<uint8_t*>(out.pixel_arena), out.pixel_arena_len);
    jobject matrices = env->NewDirectByteBuffer(
        out.matrix_arena ? const_cast<float*>(out.matrix_arena) : nullptr,
        out.matrix_arena_len);

    return env->NewObject(frameCls, ctor, records,
                          static_cast<jint>(out.record_count),
                          strings, pixels, matrices);
}

/* ------------------------------------------------------------------ */
/* dispose                                                             */
/* ------------------------------------------------------------------ */

extern "C" JNIEXPORT void JNICALL
JNI_NAME(nDisposeInstance)(JNIEnv* env, jobject, jlong handle, jstring instanceKey) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    if (!mod || !instanceKey) return;
    const char* key = env->GetStringUTFChars(instanceKey, nullptr);
    if (!key) return;
    std::lock_guard<std::mutex> lock(mod->mutex);
    auto it = mod->instances.find(key);
    env->ReleaseStringUTFChars(instanceKey, key);
    if (it == mod->instances.end()) return;

    if (it->second.created) {
        JcmHostServices host;
        fill_host(host, mod);
        JcmFrameInput in{};
        in.abi_version = MTR_NATIVE_ABI_VERSION;
        in.resource_kind = MTR_RESOURCE_VEHICLE; /* informational for dispose */
        in.snapshot = it->second.last_snapshot.data();
        in.state = it->second.state.data();
        in.state_size = it->second.state.size();
        in.host = &host;
        mod->dispose(&in);
    }
    mod->instances.erase(it);
}

extern "C" JNIEXPORT void JNICALL
JNI_NAME(nClose)(JNIEnv*, jobject, jlong handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    if (!mod) return;
    {
        std::lock_guard<std::mutex> lock(mod->mutex);
        JcmHostServices host;
        fill_host(host, mod);
        for (auto& kv : mod->instances) {
            if (!kv.second.created) continue;
            JcmFrameInput in{};
            in.abi_version = MTR_NATIVE_ABI_VERSION;
            in.resource_kind = MTR_RESOURCE_VEHICLE;
            in.snapshot = kv.second.last_snapshot.data();
            in.state = kv.second.state.data();
            in.state_size = kv.second.state.size();
            in.host = &host;
            mod->dispose(&in);
        }
    }
    /* dlclose happens in ~BridgeModule */
    delete mod;
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM*, void*) {
    return JNI_VERSION_1_8;
}
