#include <mtr/gfx2d.hpp>
#include <chrono>
#include <cstdio>
using namespace mtr;
using namespace std::chrono;

static double bench(const char* name, int iters, void (*fn)(Gfx2D&), Gfx2D& g) {
    auto t0 = steady_clock::now();
    for (int i = 0; i < iters; i++) fn(g);
    auto t1 = steady_clock::now();
    double us = duration<double, std::micro>(t1 - t0).count() / iters;
    std::printf("  %-28s %10.2f us\n", name, us);
    return us;
}

int main() {
    JcmFrameInput in{};
    in.abi_version = MTR_NATIVE_ABI_VERSION;
    in.resource_kind = MTR_RESOURCE_VEHICLE;
    GraphicsTexture tex;
    tex.create(in, 1652, 472);
    Gfx2D g(tex);
    g.set_scale(0.59, 0.59);

    auto white = [](Gfx2D& g2) { g2.set_color(0xFFFFFFFF); g2.fill_rect(0, 0, 2800, 800); };
    auto header = [](Gfx2D& g2) { g2.set_color(0xFF009BC0); g2.fill_rect(0, 0, 2800, 217); };
    auto thickline = [](Gfx2D& g2) { g2.set_color(0xFF767F89); g2.draw_line(300, 525, 2500, 525, 40); };
    auto ring = [](Gfx2D& g2) {
        g2.set_color(0xFF767F89);
        g2.draw_round_rect(200, 400, 2400, 250, 250, 250, 40);
    };
    auto shadowbox = [](Gfx2D& g2) {
        for (int s = 6; s >= 1; s--) {
            g2.set_color(clra(0, 0, 0, 28));
            g2.fill_round_rect(2300 - s, 50 - s + 3, 700 + s * 2, 130 + s * 2, 14 + s, 14 + s);
        }
        g2.set_color(0xFF009BC0);
        g2.fill_round_rect(2300, 50, 700, 130, 14, 14);
    };
    auto asciitext = [](Gfx2D& g2) {
        g2.set_color(0xFF000000);
        g2.draw_text(100, 400, 40, "Changfeng Street Xuefu Street");
    };
    auto oval = [](Gfx2D& g2) {
        g2.set_color(0xFF00C850);
        for (int i = 0; i < 10; i++) {
            g2.fill_oval(300 + i * 220 - 23, 525 - 23, 46, 46);
            g2.draw_oval(300 + i * 220 - 23, 525 - 23, 46, 46, 3);
        }
    };

    std::printf("gfx2d primitive bench (1652x472 target, scale 0.59):\n");
    bench("fill white screen", 100, white, g);
    bench("fill header band", 100, header, g);
    bench("thick track line", 100, thickline, g);
    bench("ring round-rect stroke", 100, ring, g);
    bench("num-box shadow+card", 100, shadowbox, g);
    bench("10 station dots", 100, oval, g);
    bench("ascii text line", 100, asciitext, g);
    return 0;
}
