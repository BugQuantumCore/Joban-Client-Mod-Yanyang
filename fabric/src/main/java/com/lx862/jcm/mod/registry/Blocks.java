package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.block.*;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.JCMLogger;
import net.minecraft.client.renderer.RenderType;
import org.mtr.block.IBlock;
import static com.lx862.jcm.mod.block.SpotLampBlock.LIT;
import net.minecraft.world.level.block.Block;
import org.mtr.registry.ObjectHolder;

public final class Blocks {
    public static final ObjectHolder<Block> APG_DOOR_DRL = JCMRegistry.registerBlock("apg_door_drl", settings -> new APGDoorDRL(settings));
    public static final ObjectHolder<Block> APG_GLASS_DRL = JCMRegistry.registerBlock("apg_glass_drl", settings -> new APGGlassDRL(settings));
    public static final ObjectHolder<Block> APG_GLASS_END_DRL = JCMRegistry.registerBlock("apg_glass_end_drl", settings -> new APGGlassEndDRL(settings));
    public static final ObjectHolder<Block> AUTO_IRON_DOOR = JCMRegistry.registerBlockItem("auto_iron_door", settings -> new AutoIronDoorBlock((blockSettings) -> blockSettings.strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> BUTTERFLY_LIGHT = JCMRegistry.registerBlockItem("butterfly_light", settings -> new ButterflyLightBlock(JCMRegistry.createDefaultBlockSettings(false).strength(3.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> BUFFER_STOP = JCMRegistry.registerBlockItem("buffer_stop", settings -> new BufferStopBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 8).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> CEILING_SLANTED = JCMRegistry.registerBlockItem("ceiling_slanted", settings -> new CeilingSlantedBlock(JCMRegistry.createDefaultBlockSettings(false).strength(2.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> CIRCLE_WALL_1 = JCMRegistry.registerBlockItem("circle_wall_1", settings -> new CircleWallBlock(JCMRegistry.createDefaultBlockSettings(false).strength(8.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> CIRCLE_WALL_2 = JCMRegistry.registerBlockItem("circle_wall_2", settings -> new CircleWallBlock(JCMRegistry.createDefaultBlockSettings(false).strength(8.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> CIRCLE_WALL_3 = JCMRegistry.registerBlockItem("circle_wall_3", settings -> new CircleWallBlock(JCMRegistry.createDefaultBlockSettings(false).strength(8.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> CIRCLE_WALL_4 = JCMRegistry.registerBlockItem("circle_wall_4", settings -> new CircleWallBlock(JCMRegistry.createDefaultBlockSettings(false).strength(8.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> CIRCLE_WALL_5 = JCMRegistry.registerBlockItem("circle_wall_5", settings -> new CircleWallBlock(JCMRegistry.createDefaultBlockSettings(false).strength(8.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> CIRCLE_WALL_6 = JCMRegistry.registerBlockItem("circle_wall_6", settings -> new CircleWallBlock(JCMRegistry.createDefaultBlockSettings(false).strength(8.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> CIRCLE_WALL_7 = JCMRegistry.registerBlockItem("circle_wall_7", settings -> new CircleWallBlock(JCMRegistry.createDefaultBlockSettings(false).strength(8.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> DEPARTURE_POLE = JCMRegistry.registerBlockItem("departure_pole", settings -> new DeparturePoleBlock(JCMRegistry.createDefaultBlockSettings(false).strength(3.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> DEPARTURE_TIMER = JCMRegistry.registerBlockItem("departure_timer", settings -> new DepartureTimerBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 4).strength(3.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> EXIT_SIGN_ODD = JCMRegistry.registerBlockItem("exit_sign_odd", settings -> new ExitSignOdd(JCMRegistry.createDefaultBlockSettings(false, state -> 15).strength(1.5f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> EXIT_SIGN_EVEN = JCMRegistry.registerBlockItem("exit_sign_even", settings -> new ExitSignEven(JCMRegistry.createDefaultBlockSettings(false, state -> 15).strength(1.5f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> FIRE_ALARM = JCMRegistry.registerBlockItem("fire_alarm", settings -> new FireAlarmWallBlock(JCMRegistry.createDefaultBlockSettings(false).strength(1.5f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> FARE_SAVER = JCMRegistry.registerBlockItem("fare_saver", settings -> new FareSaverBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 15).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> HELPLINE_1 = JCMRegistry.registerBlockItem("helpline_1", settings -> new WallAttachedHelpLineBlock(JCMRegistry.createDefaultBlockSettings(false).strength(1.5f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> HELPLINE_2 = JCMRegistry.registerBlockItem("helpline_2", settings -> new WallAttachedHelpLineBlock(JCMRegistry.createDefaultBlockSettings(false).strength(1.5f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> HELPLINE_HKWK = JCMRegistry.registerBlockItem("helpline_hkwk", settings -> new WallAttachedHelpLineBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> HELPLINE_STANDING = JCMRegistry.registerBlockItem("helpline_standing", settings -> new HelpLineStandingBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 15).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> HELPLINE_STANDING_EAL = JCMRegistry.registerBlockItem("helpline_standing_eal", settings -> new HelpLineStandingEALBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 15).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> HELPLINE_STANDING_TKL = JCMRegistry.registerBlockItem("helpline_standing_tkl", settings -> new HelpLineStandingTKLBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 15).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> KCR_EMG_STOP_SIGN = JCMRegistry.registerBlockItem("kcr_emg_stop_sign", settings -> new KCREmergencyStopSign(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> KCR_ENQUIRY_MACHINE = JCMRegistry.registerBlockItem("kcr_enquiry_machine", settings -> new KCREnquiryMachineWall(JCMRegistry.createDefaultBlockSettings(false, state -> 4).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> KCR_STATION_NAME_SIGN = JCMRegistry.registerBlockItem("kcr_name_sign", settings -> new KCRStationNameSignBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 15).strength(4.0f).noOcclusion(), false), ItemGroups.MAIN);
    public static final ObjectHolder<Block> KCR_STATION_NAME_SIGN_STATION_COLOR = JCMRegistry.registerBlockItem("kcr_name_sign_station_color", settings -> new KCRStationNameSignBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 15).strength(4.0f).noOcclusion(), true), ItemGroups.MAIN);
    public static final ObjectHolder<Block> KCR_TRESPASS_SIGN = JCMRegistry.registerBlockItem("kcr_trespass_sign", settings -> new KCRTrespassSignageBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> LCD_PIDS = JCMRegistry.registerBlockItem("lcd_pids", settings -> new LCDPIDSBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 8).strength(2.0f).noOcclusion()), ItemGroups.PIDS);
    public static final ObjectHolder<Block> LIGHT_BLOCK = JCMRegistry.registerBlockItem("light_block", settings -> new LightBlock(JCMRegistry.createDefaultBlockSettings(false, state -> IBlock.getStatePropertySafe(state, BlockProperties.LIGHT_LEVEL)).strength(1.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> LIGHT_LANTERN = JCMRegistry.registerBlockItem("light_lantern", settings -> new LightLanternBlock(JCMRegistry.createDefaultBlockSettings(false, state -> IBlock.getStatePropertySafe(state, LIT) ? 15 : 0).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> LRT_TRESPASS_SIGN = JCMRegistry.registerBlockItem("lrt_trespass_sign", settings -> new LRTTrespassSignageBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> LRT_INTER_CAR_BARRIER_LEFT = JCMRegistry.registerBlockItem("lrt_inter_car_barrier_left", settings -> new LRTInterCarBarrierBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> LRT_INTER_CAR_BARRIER_MIDDLE = JCMRegistry.registerBlockItem("lrt_inter_car_barrier_middle", settings -> new LRTInterCarBarrierBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> LRT_INTER_CAR_BARRIER_RIGHT = JCMRegistry.registerBlockItem("lrt_inter_car_barrier_right", settings -> new LRTInterCarBarrierBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> MTR_ENQUIRY_MACHINE = JCMRegistry.registerBlockItem("mtr_enquiry_machine", settings -> new MTREnquiryMachine(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> MTR_ENQUIRY_MACHINE_WALL = JCMRegistry.registerBlockItem("mtr_enquiry_machine_wall", settings -> new MTREnquiryMachineWall(JCMRegistry.createDefaultBlockSettings(false, state -> 4).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> MTR_STAIRS = JCMRegistry.registerBlockItem("mtr_stairs", settings -> new MTRStairsBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> MTR_TRESPASS_SIGN = JCMRegistry.registerBlockItem("mtr_trespass_sign", settings -> new MTRTrespassSignageBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> OPERATOR_BUTTON = JCMRegistry.registerBlockItem("operator_button", settings -> new OperatorButtonBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 5).strength(1.0f).noOcclusion(), 40), ItemGroups.MAIN);
    public static final ObjectHolder<Block> PIDS_PROJECTOR = JCMRegistry.registerBlockItem("pids_projector", settings -> new PIDSProjectorBlock(JCMRegistry.createDefaultBlockSettings(false).strength(2.0f).noOcclusion()), ItemGroups.PIDS);
    public static final ObjectHolder<Block> PIDS_1A = JCMRegistry.registerBlockItem("pids_1a", settings -> new PIDS1ABlock(settings), ItemGroups.PIDS);
    public static final ObjectHolder<Block> RV_PIDS = JCMRegistry.registerBlockItem("rv_pids", settings -> new RVPIDSBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 8).strength(2.0f).noOcclusion()), ItemGroups.PIDS);
    public static final ObjectHolder<Block> RV_PIDS_SIL_1 = JCMRegistry.registerBlockItem("rv_pids_sil_1", settings -> new RVPIDSSIL1Block(JCMRegistry.createDefaultBlockSettings(false, state -> 8).strength(2.0f).noOcclusion()), ItemGroups.PIDS);
    public static final ObjectHolder<Block> RV_PIDS_SIL_2 = JCMRegistry.registerBlockItem("rv_pids_sil_2", settings -> new RVPIDSSIL2Block(JCMRegistry.createDefaultBlockSettings(false, state -> 8).strength(2.0f).noOcclusion()), ItemGroups.PIDS);
    public static final ObjectHolder<Block> RV_PIDS_POLE = JCMRegistry.registerBlockItem("rv_pids_pole", settings -> new RVPIDSPole(JCMRegistry.createDefaultBlockSettings(false).strength(2.0f).noOcclusion()), ItemGroups.PIDS);
    public static final ObjectHolder<Block> RV_ENQUIRY_MACHINE = JCMRegistry.registerBlockItem("rv_enquiry_machine", settings -> new RVEnquiryMachine(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> SIL_EMG_STOP_BUTTON = JCMRegistry.registerBlockItem("sil_emg_stop_button", settings -> new SILEmergencyButtonBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 10).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> SIGNAL_LIGHT_INVERTED_RED_ABOVE = JCMRegistry.registerBlockItem("signal_light_inverted_1", settings -> new InvertedSignalBlockRedAbove(JCMRegistry.createDefaultBlockSettings(false).strength(1.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> SIGNAL_LIGHT_INVERTED_RED_BOTTOM = JCMRegistry.registerBlockItem("signal_light_inverted_2", settings -> new InvertedSignalBlockRedBelow(JCMRegistry.createDefaultBlockSettings(false).strength(1.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> STATIC_SIGNAL_LIGHT_RED_BELOW = JCMRegistry.registerBlockItem("signal_light_red_1", settings -> new StaticSignalLightBlockRedBelow(JCMRegistry.createDefaultBlockSettings(false).strength(1.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> STATIC_SIGNAL_LIGHT_RED_TOP = JCMRegistry.registerBlockItem("signal_light_red_2", settings -> new StaticSignalLightBlockRedTop(JCMRegistry.createDefaultBlockSettings(false).strength(1.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> STATIC_SIGNAL_LIGHT_GREEN = JCMRegistry.registerBlockItem("signal_light_green", settings -> new StaticSignalLightBlockGreen(JCMRegistry.createDefaultBlockSettings(false).strength(1.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> STATIC_SIGNAL_LIGHT_BLUE = JCMRegistry.registerBlockItem("signal_light_blue", settings -> new StaticSignalLightBlockBlue(JCMRegistry.createDefaultBlockSettings(false).strength(1.0f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> SPOT_LAMP = JCMRegistry.registerBlockItem("spot_lamp", settings -> new SpotLampBlock(JCMRegistry.createDefaultBlockSettings(false, state -> IBlock.getStatePropertySafe(state, LIT) ? 15 : 0).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> STATION_NAME_STANDING = JCMRegistry.registerBlock("station_name_standing", settings -> new StationNameStandingBlock(settings));
    public static final ObjectHolder<Block> SUBSIDY_MACHINE = JCMRegistry.registerBlockItem("subsidy_machine", settings -> new SubsidyMachineBlock(JCMRegistry.createDefaultBlockSettings(false).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> SOUND_LOOPER = JCMRegistry.registerBlockItem("sound_looper", settings -> new SoundLooperBlock(JCMRegistry.createDefaultBlockSettings(false)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> STATION_CEILING_WRL = JCMRegistry.registerBlockItem("station_ceiling_wrl", settings -> new StationCeilingWRL2Block(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> STATION_CEILING_WRL_SINGLE = JCMRegistry.registerBlockItem("station_ceiling_wrl_single", settings -> new StationCeilingWRLBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> STATION_CEILING_WRL_STATION_COLOR = JCMRegistry.registerBlockItem("station_ceiling_wrl_station_color", settings -> new StationCeilingWRL2Block(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> STATION_CEILING_WRL_SINGLE_STATION_COLOR = JCMRegistry.registerBlockItem("station_ceiling_wrl_single_station_color", settings -> new StationCeilingWRLBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> STATION_CEILING_WRL_POLE = JCMRegistry.registerBlockItem("station_ceiling_wrl_pole", settings -> new StationCeilingWRL2Pole(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> STATION_CEILING_WRL_POLE_SINGLE = JCMRegistry.registerBlockItem("station_ceiling_wrl_single_pole", settings -> new StationCeilingWRLPole(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> TCL_EMG_STOP_BUTTON = JCMRegistry.registerBlockItem("tcl_emg_stop_button", settings -> new TCLEmergencyButtonBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> THALES_TICKET_BARRIER_ENTRANCE = JCMRegistry.registerBlockItem("thales_ticket_barrier_entrance", settings -> new ThalesTicketBarrier(settings, true), ItemGroups.MAIN);
    public static final ObjectHolder<Block> THALES_TICKET_BARRIER_EXIT = JCMRegistry.registerBlockItem("thales_ticket_barrier_exit", settings -> new ThalesTicketBarrier(settings, false), ItemGroups.MAIN);
    public static final ObjectHolder<Block> THALES_TICKET_BARRIER_BARE = JCMRegistry.registerBlockItem("thales_ticket_barrier_bare", settings -> new ThalesTicketBarrierBareBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> TML_EMG_STOP_BUTTON = JCMRegistry.registerBlockItem("tml_emg_stop_button", settings -> new TMLEmergencyButtonBlock(JCMRegistry.createDefaultBlockSettings(false, state -> 15).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> TML_EMG_STOP_BUTTON_WALL = JCMRegistry.registerBlockItem("tml_emg_stop_button_wall", settings -> new TMLEmergencyButtonWallBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> URL_EMG_STOP_BUTTON_WALL = JCMRegistry.registerBlockItem("url_emg_stop_button_wall", settings -> new URLEmergencyButtonWallBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);
    public static final ObjectHolder<Block> TRAIN_MODEL_E44 = JCMRegistry.registerBlockItem("train_model_e44", settings -> new MTRTrainModelBlock(JCMRegistry.createDefaultBlockSettings(false).strength(0.5f)), ItemGroups.MAIN);
    public static final ObjectHolder<Block> WATER_MACHINE = JCMRegistry.registerBlockItem("water_machine", settings -> new WaterMachineBlock(JCMRegistry.createDefaultBlockSettings(false).strength(4.0f).noOcclusion()), ItemGroups.MAIN);

    public static void register() {
        JCMLogger.debug("Registering blocks...");
        // Calling this method cause the static class to be loaded, in turn registering the content.
    }

    public static void registerClient() {
        /* Define Render Layer (Texture transparency etc.) */
        JCMRegistryClient.registerBlockRenderType(RenderType.cutout(),
            APG_DOOR_DRL,
            APG_GLASS_DRL,
            APG_GLASS_END_DRL,
            AUTO_IRON_DOOR,
            BUFFER_STOP,
            BUTTERFLY_LIGHT,
            CIRCLE_WALL_1,
            CIRCLE_WALL_2,
            CIRCLE_WALL_3,
            CIRCLE_WALL_4,
            CIRCLE_WALL_5,
            CIRCLE_WALL_6,
            FIRE_ALARM,
            FARE_SAVER,
            KCR_STATION_NAME_SIGN,
            KCR_STATION_NAME_SIGN_STATION_COLOR,
            KCR_EMG_STOP_SIGN,
            MTR_ENQUIRY_MACHINE,
            RV_ENQUIRY_MACHINE,
            RV_PIDS_SIL_1,
            RV_PIDS_SIL_2,
            SUBSIDY_MACHINE,
            SPOT_LAMP,
            HELPLINE_1,
            HELPLINE_2,
            HELPLINE_HKWK,
            HELPLINE_STANDING,
            HELPLINE_STANDING_TKL,
            HELPLINE_STANDING_EAL,
            SIL_EMG_STOP_BUTTON,
            STATION_NAME_STANDING,
            TML_EMG_STOP_BUTTON,
            TML_EMG_STOP_BUTTON_WALL,
            THALES_TICKET_BARRIER_ENTRANCE,
            THALES_TICKET_BARRIER_EXIT,
            MTR_TRESPASS_SIGN,
            WATER_MACHINE
        );

        JCMRegistryClient.registerBlockRenderType(RenderType.translucent(),
            Blocks.THALES_TICKET_BARRIER_BARE
        );

        /* Station Colored Blocks */
        JCMRegistryClient.registerStationColoredBlock(
            KCR_STATION_NAME_SIGN_STATION_COLOR,
            STATION_NAME_STANDING,
            STATION_CEILING_WRL_STATION_COLOR,
            STATION_CEILING_WRL_SINGLE_STATION_COLOR
        );
    }
}
