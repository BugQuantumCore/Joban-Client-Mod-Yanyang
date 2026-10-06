/**
 * matrices.hpp — RAII matrix-stack wrapper.
 *
 * 1:1 port of com.lx862.mtrscripting.core.util.Matrices (the JS
 * `Matrices` global). Poses compile into the frame matrix arena,
 * so the host replays them as StoredMatrixTransformations.
 */
#pragma once

#include "mtr_native.h"
#include "frame.hpp"
#include <cmath>
#include <array>

namespace mtr {

class Matrices {
public:
    explicit Matrices(FrameRecorder& frame) : frame_(&frame) {
        stack_.fill(identity());
    }

    void push_pose() {
        if (depth_ + 1 < static_cast<int32_t>(stack_.size())) {
            stack_[static_cast<size_t>(depth_ + 1)] = stack_[static_cast<size_t>(depth_)];
            depth_++;
        }
    }

    void pop_pose() {
        if (depth_ > 0) depth_--;
    }

    void pop_push_pose() {
        pop_pose();
        push_pose();
    }

    void translate(float x, float y, float z) {
        top() = mul(top(), translation(x, y, z));
    }

    void rotate_x(float radians) { rotate_axis(0, radians); }
    void rotate_y(float radians) { rotate_axis(1, radians); }
    void rotate_z(float radians) { rotate_axis(2, radians); }

    void rotate_x_degrees(float deg) { rotate_x(deg * float(M_PI) / 180.0f); }
    void rotate_y_degrees(float deg) { rotate_y(deg * float(M_PI) / 180.0f); }
    void rotate_z_degrees(float deg) { rotate_z(deg * float(M_PI) / 180.0f); }

    void scale(float x, float y, float z) {
        top() = mul(top(), scaling(x, y, z));
    }

    /* Compile the current stack into the frame arena; returns the
       matrix index for use in JcmDrawModel.pose_offset. */
    int32_t compile() const {
        JcmMat4 flat;
        std::memcpy(flat.m, stack_[static_cast<size_t>(depth_)].data(), sizeof(flat.m));
        return frame_->push_matrix(flat);
    }

private:
    using M = std::array<float, 16>;

    M& top() { return stack_[static_cast<size_t>(depth_)]; }

    void rotate_axis(int axis, float radians) {
        const float c = std::cos(radians), s = std::sin(radians);
        M r = identity();
        if (axis == 0) {           /* X */
            r[5] = c;  r[6] = s;
            r[9] = -s; r[10] = c;
        } else if (axis == 1) {    /* Y */
            r[0] = c;  r[2] = -s;
            r[8] = s;  r[10] = c;
        } else {                   /* Z */
            r[0] = c;  r[1] = s;
            r[4] = -s; r[5] = c;
        }
        top() = mul(top(), r);
    }

    static M identity() {
        M m{};
        m[0] = m[5] = m[10] = m[15] = 1.0f;
        return m;
    }

    static M translation(float x, float y, float z) {
        M m = identity();
        m[12] = x; m[13] = y; m[14] = z;
        return m;
    }

    static M scaling(float x, float y, float z) {
        M m{};
        m[0] = x; m[5] = y; m[10] = z; m[15] = 1.0f;
        return m;
    }

    /* Column-major multiply: out = a * b. */
    static M mul(const M& a, const M& b) {
        M out{};
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float acc = 0;
                for (int k = 0; k < 4; k++) {
                    acc += a[static_cast<size_t>(k * 4 + row)] * b[static_cast<size_t>(col * 4 + k)];
                }
                out[static_cast<size_t>(col * 4 + row)] = acc;
            }
        }
        return out;
    }

    FrameRecorder* frame_;
    std::array<M, 16> stack_{};
    int32_t depth_ = 0;
};

} /* namespace mtr */
