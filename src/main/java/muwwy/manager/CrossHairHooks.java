package muwwy.manager;

import gloomyfolken.hooklib.asm.Hook;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

public class CrossHairHooks {

    public static PartSeat seat;
    public static PartGun gun;

    @Hook
    public static void update(PartSeat clazz) {
        IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();
        if (clientPlayer == null || clazz.rider == null) return;
        if (clientPlayer.equals(clazz.rider)) {
            CrossHairHooks.seat = clazz;
        }
    }

    @Hook
    public static void update(PartGun clazz) {
        IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();
        if (clientPlayer == null || clazz.getGunController() == null) return;
        if (clientPlayer.equals(clazz.getGunController())) {
            CrossHairHooks.gun = clazz;
        }
    }

    public static boolean isActive() {
        return InterfaceManager.clientInterface.getClientPlayer().getEntityRiding() != null && gun != null && seat != null;
    }

}
