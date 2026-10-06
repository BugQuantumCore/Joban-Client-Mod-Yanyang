/**
 * script.hpp — native script registration.
 *
 * Usage (vehicle example):
 *
 *   struct MyLcdState { mtr::CycleTracker dest{"KOWLOON|TSUEN WAN"}; };
 *
 *   struct MyLcdScript : mtr::VehicleScript<MyLcdState> {
 *       static constexpr auto ID = "demo:kcx_lcd";
 *
 *       void create(VehicleContext& ctx, MyLcdState& state,
 *                   const Train& train) override { ... }
 *       void render(VehicleContext& ctx, MyLcdState& state,
 *                   const Train& train) override { ... }
 *   };
 *
 *   MTR_REGISTER_VEHICLE_SCRIPT(MyLcdScript)
 *
 * This mirrors the JS lifecycle functions create/render/dispose.
 * Per-instance `state` blocks are allocated by the HOST (one per
 * vehicle / PIDS / block-entity, exactly like JCM creates one
 * `state` JS object per ScriptInstance) and passed through
 * JcmFrameInput.state / state_size.
 */
#pragma once

#include "mtr_native.h"
#include "frame.hpp"
#include "vehicle.hpp"
#include "pids.hpp"
#include "eyecandy.hpp"
#include <cstring>
#include <new>
#include <type_traits>

namespace mtr {

/* Host-side frame buffers: attached once, reused for the whole module
   lifetime. The host serializes render calls per script module on one
   background thread (mirrors JCM's per-script single-thread executor),
   so plain storage is safe. Double buffering between mtrRender calls
   happens host-side: it reads the previous frame while we fill the
   next one. */
struct HostBuffers {
    static constexpr int32_t MAX_RECORDS  = 256;
    static constexpr int32_t MAX_MATRICES = 128;
    static constexpr int32_t MAX_STRING   = 32 * 1024;
    static constexpr int64_t MAX_PIXEL    = 2048LL * 2048LL * 4;
    static constexpr int32_t MAX_FLOAT    = 1024;

    /* Record slots must fit the LARGEST record struct (JcmDrawText). */
    alignas(64) uint8_t records[sizeof(JcmDrawText) * MAX_RECORDS];
    alignas(64) float  matrix_arena[MAX_MATRICES * 16];
    alignas(64) char   string_arena[MAX_STRING];
    alignas(64) uint8_t pixel_arena[2048 * 2048 * 4];
    alignas(64) float  float_arena[MAX_FLOAT];
};

namespace detail {

inline FrameRecorder& recorder() {
    static FrameRecorder rec;
    return rec;
}

inline void install_frame() {
    /* Heap-backed on purpose: a 16 MB static aggregate is emitted by
       Apple ld64 as *initialized* __DATA (comdat zero aggregates become
       file-backed data), which bloated every macOS dylib to ~17 MB.
       `new` gives demand-zero pages on all three platforms and keeps
       the one-time attach semantics. */
    static HostBuffers* buffers = new HostBuffers();
    static bool attached = false;
    if (!attached) {
        recorder().attach(buffers->records, buffers->matrix_arena,
                          buffers->string_arena, buffers->pixel_arena,
                          sizeof(buffers->pixel_arena), buffers->float_arena);
        attached = true;
    }
    recorder().begin_frame();
}

} /* namespace detail */

/* ------------------------------------------------------------------ */
/* Kind adapters: Context / Wrapper / snapshot construction            */
/* ------------------------------------------------------------------ */

namespace detail {

struct VehicleAdapter {
    static constexpr int RESOURCE_ID = MTR_RESOURCE_VEHICLE;
    static constexpr const char* TYPE = "vehicle";
    using Context = VehicleContext;
    using Wrapper = Train;
    using Snapshot = JcmVehicleSnapshot;
    static VehicleContext make_context(FrameRecorder& f, const JcmFrameInput& in) {
        return VehicleContext(f, in);
    }
    static Train make_wrapper(const void* snap) {
        return Train(static_cast<const JcmVehicleSnapshot*>(static_cast<const void*>(snap)));
    }
};

struct PidsAdapter {
    static constexpr int RESOURCE_ID = MTR_RESOURCE_PIDS;
    static constexpr const char* TYPE = "pids";
    using Context = PidsContext;
    using Wrapper = Pids;
    using Snapshot = JcmPidsSnapshot;
    static PidsContext make_context(FrameRecorder& f, const JcmFrameInput& in) {
        return PidsContext(f, in);
    }
    static Pids make_wrapper(const void* snap) {
        return Pids(static_cast<const JcmPidsSnapshot*>(static_cast<const void*>(snap)));
    }
};

struct EyecandyAdapter {
    static constexpr int RESOURCE_ID = MTR_RESOURCE_EYECANDY;
    static constexpr const char* TYPE = "eyecandy";
    using Context = EyeCandyContext;
    using Wrapper = EyeCandy;
    using Snapshot = JcmEyecandySnapshot;
    static EyeCandyContext make_context(FrameRecorder& f, const JcmFrameInput& in) {
        return EyeCandyContext(f, in);
    }
    static EyeCandy make_wrapper(const void* snap) {
        return EyeCandy(static_cast<const JcmEyecandySnapshot*>(static_cast<const void*>(snap)));
    }
};

} /* namespace detail */

} /* namespace mtr */

/* ------------------------------------------------------------------ */
/* Script base classes                                                 */
/* ------------------------------------------------------------------ */

namespace mtr {

template <typename State, typename Adapter>
class ScriptBase {
public:
    using StateType = State;
    using Kind = Adapter;

    virtual ~ScriptBase() = default;

    /* JS: function create(ctx, state, wrapper) {} */
    virtual void create(typename Adapter::Context& ctx, State& state,
                        const typename Adapter::Wrapper& wrapper) {
        (void)ctx; (void)state; (void)wrapper;
    }

    /* JS: function render(ctx, state, wrapper) {} */
    virtual void render(typename Adapter::Context& ctx, State& state,
                        const typename Adapter::Wrapper& wrapper) {
        (void)ctx; (void)state; (void)wrapper;
    }

    /* JS: function dispose(ctx, state, wrapper) {} */
    virtual void dispose(typename Adapter::Context& ctx, State& state,
                         const typename Adapter::Wrapper& wrapper) {
        (void)ctx; (void)state; (void)wrapper;
    }
};

template <typename State>
class VehicleScript : public ScriptBase<State, detail::VehicleAdapter> {};

template <typename State>
class PidsScript : public ScriptBase<State, detail::PidsAdapter> {};

template <typename State>
class EyecandyScript : public ScriptBase<State, detail::EyecandyAdapter> {};

} /* namespace mtr */

/* ------------------------------------------------------------------ */
/* Registration: expands the module exports                            */
/* ------------------------------------------------------------------ */

#define MTR_INTERNAL_EXPORTS(ScriptType, TypeString, IdString)                  \
    extern "C" {                                                                \
    MTR_NATIVE_EXPORT uint32_t mtrNativeAbiVersion(void) {                      \
        return MTR_NATIVE_ABI_VERSION;                                          \
    }                                                                           \
    MTR_NATIVE_EXPORT const char* mtrScriptType(void) { return TypeString; }    \
    MTR_NATIVE_EXPORT const char* mtrScriptId(void) { return IdString; }        \
    MTR_NATIVE_EXPORT size_t mtrStateSize(void) {                               \
        return sizeof(typename ScriptType::StateType);                          \
    }                                                                           \
    static ::mtr::detail::ScriptBox<ScriptType> mtr_g_script;                   \
    MTR_NATIVE_EXPORT int32_t mtrCreate(const JcmFrameInput* in) {              \
        return mtr_g_script.lifecycle<0>(in, nullptr);                          \
    }                                                                           \
    MTR_NATIVE_EXPORT int32_t mtrRender(const JcmFrameInput* in,                \
                                        JcmFrameOutput* out) {                  \
        return mtr_g_script.lifecycle<1>(in, out);                              \
    }                                                                           \
    MTR_NATIVE_EXPORT int32_t mtrDispose(const JcmFrameInput* in) {             \
        return mtr_g_script.lifecycle<2>(in, nullptr);                          \
    }                                                                           \
    } /* extern "C" */

#define MTR_REGISTER_VEHICLE_SCRIPT(S) \
    MTR_INTERNAL_EXPORTS(S, "vehicle", S::ID)
#define MTR_REGISTER_PIDS_SCRIPT(S) \
    MTR_INTERNAL_EXPORTS(S, "pids", S::ID)
#define MTR_REGISTER_EYECANDY_SCRIPT(S) \
    MTR_INTERNAL_EXPORTS(S, "eyecandy", S::ID)

namespace mtr { namespace detail {

/**
 * ScriptBox — adapts a typed ScriptType onto the C exports.
 *
 * Per-instance state: the host owns the state block (JcmFrameInput.
 * state / state_size, zero-initialized, one per instance). Non-trivial
 * State types are placement-new'ed on mtrCreate and destroyed on
 * mtrDispose. If the host passes no state (e.g. the benchmark driver),
 * a module-local block is used.
 */
template <typename ScriptType>
struct ScriptBox {
    ScriptType script;
    typename ScriptType::StateType* fallback_state = nullptr;
    typename ScriptType::StateType* active_state = nullptr;
    bool constructed_fallback = false;

    using State = typename ScriptType::StateType;
    using Adapter = typename ScriptType::Kind;

    template <int Phase> /* 0=create 1=render 2=dispose */
    int32_t lifecycle(const JcmFrameInput* in, JcmFrameOutput* out) {
        if (!in) return -1;
        if (in->abi_version != MTR_NATIVE_ABI_VERSION) return -2;
        if (in->resource_kind != Adapter::RESOURCE_ID) return -3;

        install_frame();

        /* Resolve the state block for this instance. */
        State* state = resolve_state(*in);

        if constexpr (Phase == 0) {
            if (!std::is_trivially_default_constructible_v<State> && state) {
                /* host-provided block was zeroed; construct in place once.
                   Fallback block gets constructed on first use. */
                new (static_cast<void*>(state)) State();
            }
        }

        auto ctx = Adapter::make_context(recorder(), *in);
        auto wrapper = Adapter::make_wrapper(in->snapshot);

        if constexpr (Phase == 0) {
            script.create(ctx, *state, wrapper);
        } else if constexpr (Phase == 1) {
            script.render(ctx, *state, wrapper);
            recorder().write_output(*out);
        } else {
            script.dispose(ctx, *state, wrapper);
            if (!std::is_trivially_default_constructible_v<State> && state) {
                state->~State();
            }
        }
        return 0;
    }

private:
    State* resolve_state(const JcmFrameInput& in) {
        if (in.state && in.state_size >= sizeof(State)) {
            return static_cast<State*>(in.state);
        }
        if (!fallback_state) {
            static Storage storage;
            fallback_state = storage.addr();
        }
        return fallback_state;
    }

    /* Module-local fallback storage (also used by the benchmark driver). */
    struct Storage {
        alignas(alignof(State)) uint8_t bytes[sizeof(State)];
        State* addr() { return reinterpret_cast<State*>(bytes); }
    };
};

}} /* namespace mtr::detail */
