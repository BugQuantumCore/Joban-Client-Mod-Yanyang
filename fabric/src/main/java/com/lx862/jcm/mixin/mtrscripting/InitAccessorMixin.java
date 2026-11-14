package com.lx862.jcm.mixin.mtrscripting;

import org.mtr.MTR;
import org.mtr.core.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access to the running TSC {@link Main} instance. MTR 4.1 keeps it in the
 * private static {@code main} field of {@link MTR} (the old
 * {@code org.mtr.mod.Init#main} holder was removed).
 */
@Mixin(value = MTR.class, remap = false)
public interface InitAccessorMixin {
    @Accessor("main")
    static Main getMain() {
        throw new AssertionError();
    }
}
