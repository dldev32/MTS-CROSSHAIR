package muwwy;

import muwwy.proxy.CommonProxy;
import muwwy.utils.CrossHairConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = CrossHairMod.MODID, version = CrossHairMod.VERSION, name = CrossHairMod.NAME)
public class CrossHairMod {

    public static final String MODID = "crosshair";
    public static final String NAME = "crosshair";
    public static final String VERSION = "0.0.1";

    @SidedProxy(clientSide = "muwwy.proxy.ClientProxy", serverSide = "muwwy.proxy.CommonProxy")
    public static CommonProxy proxy;


    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        CrossHairConfig.load(event);
        proxy.preInit();
    }
}
