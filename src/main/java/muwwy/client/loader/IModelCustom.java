package muwwy.client.loader;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public interface IModelCustom {
	String getType();

	@SideOnly(Side.CLIENT)
	void renderAll();
}
