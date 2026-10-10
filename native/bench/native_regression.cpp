#include <mtr/gfx.hpp>
#include <cstdio>
#include <cstring>

int main() {
    JcmFrameInput in{};
    mtr::GraphicsTexture texture;
    texture.create(in, 5, 4);
    mtr::FrameRecorder frame;
    alignas(8) unsigned char records[128]{};
    float matrices[16]{}, floats[16]{};
    char strings[16]{};
    uint8_t pixels[256]{};
    frame.attach(records, matrices, strings, pixels, sizeof(pixels), floats);
    frame.begin_frame();
    texture.upload(frame); // consume initial full-image dirtiness
    frame.begin_frame();
    for (int y = 0; y < 4; y++)
        for (int x = 0; x < 5; x++)
            texture.pixels32()[y * 5 + x] = 0xff000000u | (y << 8) | x;
    texture.mark_dirty(1, 1);
    texture.mark_dirty(2, 2);
    texture.upload(frame);
    JcmFrameOutput out{};
    frame.write_output(out);
    auto* upload = static_cast<const JcmDrawTextureUpload*>(out.records);
    const uint32_t expected[] = {0xff000101u, 0xff000102u, 0xff000201u, 0xff000202u};
    if (out.record_count != 1 || upload->dirty_w != 2 || upload->dirty_h != 2
            || upload->pixel_data_len != sizeof(expected)
            || std::memcmp(out.pixel_arena + upload->pixel_data_offset,
                           expected, sizeof(expected)) != 0) {
        std::fprintf(stderr, "FAIL: cropped texture upload must preserve row stride\n");
        return 1;
    }
    std::puts("NATIVE REGRESSION OK: cropped multi-row BGRA texture upload");
    return 0;
}
