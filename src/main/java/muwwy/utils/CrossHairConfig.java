package muwwy.utils;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CrossHairConfig {

    private static final String MAIN = "MAIN";
    public static boolean isEnableCrosshair;
    public static boolean isEnableShiftCamera;
    public static float shiftCameraValue;
    public static float zoomValue;
    public static float limitDistance;

    public static void load(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(new File(event.getModConfigurationDirectory(), "crosshair.cfg"), "1.0.0", true);
        config.load();
        config.addCustomCategoryComment(MAIN, "crosshair, created by muwwy.");
        isEnableCrosshair = config.getBoolean("isEnableCrosshair", MAIN, true, "");
        isEnableShiftCamera = config.getBoolean("isEnableShiftCamera", MAIN, true, "");

        shiftCameraValue = -config.getFloat("shiftCameraValue", MAIN, 2.5F, -100, 100, "");
        zoomValue = -config.getFloat("zoomValue", MAIN, 2, -100, 100, "");
        limitDistance = config.getFloat("limitDistance", MAIN, 6, -100, 100, "");

        config.save();
    }

}
