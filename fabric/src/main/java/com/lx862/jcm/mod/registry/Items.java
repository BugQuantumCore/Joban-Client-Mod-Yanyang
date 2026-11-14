package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.item.ItemDRLAPG;
import com.lx862.jcm.mod.item.ItemPSDScriptEditor;
import com.lx862.jcm.mod.util.JCMLogger;
import net.minecraft.world.item.Item;
import org.mtr.registry.ObjectHolder;

public class Items {
    public static final ObjectHolder<Item> APG_DOOR_DRL = JCMRegistry.registerItem("apg_door_drl", itemSettings -> new ItemDRLAPG(ItemDRLAPG.EnumPSDAPGItem.DOOR, ItemDRLAPG.EnumPSDAPGType.APG, itemSettings), ItemGroups.MAIN);
    public static final ObjectHolder<Item> APG_GLASS_DRL = JCMRegistry.registerItem("apg_glass_drl", itemSettings -> new ItemDRLAPG(ItemDRLAPG.EnumPSDAPGItem.GLASS, ItemDRLAPG.EnumPSDAPGType.APG, itemSettings), ItemGroups.MAIN);
    public static final ObjectHolder<Item> APG_GLASS_END_DRL = JCMRegistry.registerItem("apg_glass_end_drl", itemSettings -> new ItemDRLAPG(ItemDRLAPG.EnumPSDAPGItem.GLASS_END, ItemDRLAPG.EnumPSDAPGType.APG, itemSettings), ItemGroups.MAIN);

    /* Yanyang/PSD: the PSD Script Editor sits in MTR's CORE tab next to the
       brush (刷) — the same category MTR puts its own editing tools in. */
    public static final ObjectHolder<Item> PSD_SCRIPT_EDITOR = JCMRegistry.registerItem("psd_script_editor", itemSettings -> new ItemPSDScriptEditor(itemSettings), org.mtr.registry.ItemGroups.CORE);

    public static void register() {
        // We just load the class and it will be registered, nothing else
        JCMLogger.debug("Registering items...");
    }
}
