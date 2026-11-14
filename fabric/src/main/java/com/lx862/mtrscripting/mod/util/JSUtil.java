package com.lx862.mtrscripting.mod.util;

import com.lx862.mtrscripting.core.util.ScriptResourceUtil;
import net.minecraft.resources.ResourceLocation;

public class JSUtil {
    public static ResourceLocation parseId(String str) {
        if(str.startsWith("./")) {
            return ScriptResourceUtil.idr(str.substring(2, str.length()-1));
        }
        return ScriptResourceUtil.id(str);
    }
}
