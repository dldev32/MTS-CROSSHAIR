package muwwy.client.loader.obj;

import muwwy.client.loader.IModelCustom;
import muwwy.client.loader.IModelCustomLoader;
import muwwy.client.loader.ModelFormatException;
import net.minecraft.util.ResourceLocation;


public class ObjModelLoader implements IModelCustomLoader {
	private static final String[] types = new String[]{"obj"};

	public String getType() {
		return "OBJ model";
	}

	public String[] getSuffixes() {
		return types;
	}

	public IModelCustom loadInstance(ResourceLocation resource) throws ModelFormatException {
		return new WavefrontObject(resource);
	}
}
