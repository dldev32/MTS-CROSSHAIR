package muwwy.proxy;

import muwwy.CrossHairMod;
import muwwy.client.loader.AdvancedModelLoader;
import muwwy.manager.RenderManager;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;

public class ClientProxy extends CommonProxy {


    private static final HashMap<String, Integer> modelHash = new HashMap<>();

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void preInit() {
        super.preInit();
        OBJLoader.INSTANCE.addDomain(CrossHairMod.MODID);
        MinecraftForge.EVENT_BUS.register(new RenderManager());
    }

    public static int getRender(String model) {
        if (modelHash.containsKey(model))
            return modelHash.get(model);

        int displayList = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(displayList, GL11.GL_COMPILE);
        AdvancedModelLoader.loadModel(new ResourceLocation("crosshair", "models/" + model + ".obj")).renderAll();
        GL11.glEndList();
        modelHash.put(model, displayList);
        return displayList;
    }

}
