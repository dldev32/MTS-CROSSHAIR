package muwwy.manager;

import mcinterface1122.WrapperPlayer;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.jsondefs.JSONMuzzle;
import minecrafttransportsimulator.jsondefs.JSONPart;
import muwwy.CrossHairMod;
import muwwy.proxy.ClientProxy;
import muwwy.utils.ClientUtils;
import muwwy.utils.CrossHairConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
import org.lwjgl.util.vector.Vector3f;

import java.lang.reflect.Field;

import static muwwy.utils.CrossHairConfig.*;

public class RenderManager {

    public static final Point3D color = new Point3D();
    private PartSeat seat;
    private PartGun gun;

    public final static Color FAR = new Color(255, 0, 0);
    public final static Color CLOSE = new Color(0, 255, 0);
    private boolean isColorSwap = false;

    private float partialTicks = 1F;

    private double prevTurretX = 0.0;
    private double prevTurretY = 0.0;
    private double prevTurretZ = 0.0;

    private double prevPlayerX = 0.0;
    private double prevPlayerY = 0.0;
    private double prevPlayerZ = 0.0;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void changeCameraPosition(EntityViewRenderEvent.CameraSetup event) {
        if (CrossHairHooks.isActive() && Minecraft.getMinecraft().gameSettings.thirdPersonView == 1 && isEnableShiftCamera)
            GL11.glTranslatef(0, CrossHairConfig.shiftCameraValue, 0);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void renderCrossHair(RenderWorldLastEvent event) throws NoSuchFieldException, IllegalAccessException {
        partialTicks = event.getPartialTicks();

        if (CrossHairHooks.isActive() && isEnableCrosshair) {
            update();
            if (seat.canControlGuns) {
                renderFirstCrossHair();
                renderSecondCrossHair();
            }
        }
    }

    public void renderFirstCrossHair() throws NoSuchFieldException, IllegalAccessException {
        PartGun gun = (seat.gunGroups.isEmpty() || seat.activeGunItem == null || seat.gunGroups.get(seat.activeGunItem).isEmpty()) ? this.gun : seat.gunGroups.get(seat.activeGunItem).get(seat.gunIndex);

        JSONPart.JSONPartGun definitionGun = gun.definition.gun;

        Point3D bulletPosition = new Point3D();
        Point3D bulletVelocity = new Point3D();

        Field internalOrientationField = gun.getClass().getDeclaredField("internalOrientation");
        internalOrientationField.setAccessible(true);
        RotationMatrix internalOrientation = (RotationMatrix) internalOrientationField.get(gun);

        Field currentMuzzleGroupIndexField = gun.getClass().getDeclaredField("currentMuzzleGroupIndex");
        currentMuzzleGroupIndexField.setAccessible(true);
        int currentMuzzleGroupIndex = currentMuzzleGroupIndexField.getInt(gun);

        Field firingSpreadRotationField = gun.getClass().getDeclaredField("firingSpreadRotation");
        firingSpreadRotationField.setAccessible(true);
        RotationMatrix firingSpreadRotation = (RotationMatrix) firingSpreadRotationField.get(gun);

        Field pitchMuzzleRotationField = gun.getClass().getDeclaredField("pitchMuzzleRotation");
        pitchMuzzleRotationField.setAccessible(true);
        RotationMatrix pitchMuzzleRotationMatrix = (RotationMatrix) pitchMuzzleRotationField.get(gun);

        Field yawMuzzleRotationField = gun.getClass().getDeclaredField("yawMuzzleRotation");
        yawMuzzleRotationField.setAccessible(true);
        RotationMatrix yawMuzzleRotationMatrix = (RotationMatrix) yawMuzzleRotationField.get(gun);

        WrapperPlayer player = (WrapperPlayer) gun.getGunController();

        if (player == null) return;

        RotationMatrix playerRotation = new RotationMatrix().set(player.getOrientation());

        float pitch = (float) playerRotation.angles.x;
        float yaw = (float) playerRotation.angles.y;

        for (JSONMuzzle muzzle : definitionGun.muzzleGroups.get(currentMuzzleGroupIndex).muzzles) {
            if (definitionGun.muzzleVelocity != 0) {
                bulletVelocity.set(0, 0, definitionGun.muzzleVelocity / 20D / 10D);
                bulletVelocity.rotate(firingSpreadRotation);

                if (muzzle.rot != null) {
                    bulletVelocity.rotate(muzzle.rot);
                }
                bulletVelocity.rotate(internalOrientation).rotate(gun.zeroReferenceOrientation);
            } else {
                bulletVelocity.set(0, 0, 0);
            }

            if (gun.vehicleOn != null) {
                bulletVelocity.addScaled(gun.motion, gun.vehicleOn.speedFactor);
            } else {
                bulletVelocity.add(gun.motion);
            }

            bulletPosition.set(muzzle.pos);
            if (muzzle.center != null) {
                RotationMatrix pitchMuzzleRotation = pitchMuzzleRotationMatrix.setToZero().rotateX(internalOrientation.angles.x);
                RotationMatrix yawMuzzleRotation = yawMuzzleRotationMatrix.setToZero().rotateY(internalOrientation.angles.y);
                bulletPosition.subtract(muzzle.center).rotate(pitchMuzzleRotation).add(muzzle.center).rotate(yawMuzzleRotation);
            } else {
                bulletPosition.rotate(internalOrientation);
            }
            bulletPosition.rotate(gun.zeroReferenceOrientation).add(gun.position);
        }

        Vector3f origin = new Vector3f((float) bulletPosition.x, (float) bulletPosition.y, (float) bulletPosition.z);

        Vector3f hitPos = new Vector3f((float) (bulletPosition.x + (bulletVelocity.x * limitDistance)), (float) (bulletPosition.y + (bulletVelocity.y * limitDistance)), (float) (bulletPosition.z + (bulletVelocity.z * limitDistance)));

        RayTraceResult result = ClientUtils.rayTraceBlocks(Minecraft.getMinecraft().world, new Vec3d(origin.x, origin.y, origin.z), new Vec3d(hitPos.x, hitPos.y, hitPos.z), true, true, false);
        if (result != null && result.hitVec != null)
            hitPos = new Vector3f((float) result.hitVec.x, (float) result.hitVec.y, (float) result.hitVec.z);

        prevTurretX = (prevTurretX + (0.01 * limitDistance) < hitPos.x) ? hitPos.x : prevTurretX;
        prevTurretY = (prevTurretY + (0.01 * limitDistance) < hitPos.y) ? hitPos.y : prevTurretY;
        prevTurretZ = (prevTurretZ + (0.01 * limitDistance) < hitPos.z) ? hitPos.z : prevTurretZ;

        prevTurretX = (prevTurretX - (0.01 * limitDistance) > hitPos.x) ? hitPos.x : prevTurretX;
        prevTurretY = (prevTurretY - (0.01 * limitDistance) > hitPos.y) ? hitPos.y : prevTurretY;
        prevTurretZ = (prevTurretZ - (0.01 * limitDistance) > hitPos.z) ? hitPos.z : prevTurretZ;

        float range = Vector3f.sub(hitPos, origin, null).length();

        float zoom = (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2) ? zoomValue : 1;

        render(prevTurretX, prevTurretY, prevTurretZ, yaw, pitch, range / 110 * zoom, "first-crosshair");
    }

    public void renderSecondCrossHair() throws NoSuchFieldException, IllegalAccessException {
        PartGun gun = (seat.gunGroups.isEmpty() || seat.activeGunItem == null || seat.gunGroups.get(seat.activeGunItem).isEmpty()) ? this.gun : seat.gunGroups.get(seat.activeGunItem).get(seat.gunIndex);

        JSONPart.JSONPartGun definitionGun = gun.definition.gun;

        Point3D bulletPosition = new Point3D();
        Point3D bulletVelocity = new Point3D();

        Field internalOrientationField = gun.getClass().getDeclaredField("internalOrientation");
        internalOrientationField.setAccessible(true);
        RotationMatrix internalOrientation = (RotationMatrix) internalOrientationField.get(gun);

        Field currentMuzzleGroupIndexField = gun.getClass().getDeclaredField("currentMuzzleGroupIndex");
        currentMuzzleGroupIndexField.setAccessible(true);
        int currentMuzzleGroupIndex = currentMuzzleGroupIndexField.getInt(gun);

        Field firingSpreadRotationField = gun.getClass().getDeclaredField("firingSpreadRotation");
        firingSpreadRotationField.setAccessible(true);
        RotationMatrix firingSpreadRotation = (RotationMatrix) firingSpreadRotationField.get(gun);

        Field pitchMuzzleRotationField = gun.getClass().getDeclaredField("pitchMuzzleRotation");
        pitchMuzzleRotationField.setAccessible(true);
        RotationMatrix pitchMuzzleRotationMatrix = (RotationMatrix) pitchMuzzleRotationField.get(gun);

        Field yawMuzzleRotationField = gun.getClass().getDeclaredField("yawMuzzleRotation");
        yawMuzzleRotationField.setAccessible(true);
        RotationMatrix yawMuzzleRotationMatrix = (RotationMatrix) yawMuzzleRotationField.get(gun);

        WrapperPlayer player = (WrapperPlayer) gun.getGunController();

        if (player == null) return;

        RotationMatrix playerRotation = new RotationMatrix().set(player.getOrientation());

        float pitch = (float) playerRotation.angles.x;
        float yaw = (float) playerRotation.angles.y;

        for (JSONMuzzle muzzle : definitionGun.muzzleGroups.get(currentMuzzleGroupIndex).muzzles) {
            if (definitionGun.muzzleVelocity != 0) {
                bulletVelocity.set(0, 0, definitionGun.muzzleVelocity / 20D / 10D);
                bulletVelocity.rotate(firingSpreadRotation);

                if (muzzle.rot != null) {
                    bulletVelocity.rotate(muzzle.rot);
                }
                bulletVelocity.rotate(playerRotation);
            }

            if (gun.vehicleOn != null) {
                bulletVelocity.addScaled(gun.motion, gun.vehicleOn.speedFactor);
            } else {
                bulletVelocity.add(gun.motion);
            }

            bulletPosition.set(muzzle.pos);
            if (muzzle.center != null) {
                RotationMatrix pitchMuzzleRotation = pitchMuzzleRotationMatrix.setToZero().rotateX(internalOrientation.angles.x);
                RotationMatrix yawMuzzleRotation = yawMuzzleRotationMatrix.setToZero().rotateY(internalOrientation.angles.y);
                bulletPosition.subtract(muzzle.center).rotate(pitchMuzzleRotation).add(muzzle.center).rotate(yawMuzzleRotation);
            } else {
                bulletPosition.rotate(internalOrientation);
            }
            bulletPosition.rotate(gun.zeroReferenceOrientation).add(gun.position);
        }

        Vector3f origin = new Vector3f((float) bulletPosition.x, (float) bulletPosition.y, (float) bulletPosition.z);

        Vector3f hitPos = new Vector3f((float) (bulletPosition.x + (bulletVelocity.x * limitDistance)), (float) (bulletPosition.y + (bulletVelocity.y * limitDistance)), (float) (bulletPosition.z + (bulletVelocity.z * limitDistance)));

        RayTraceResult result = ClientUtils.rayTraceBlocks(Minecraft.getMinecraft().world, new Vec3d(origin.x, origin.y, origin.z), new Vec3d(hitPos.x, hitPos.y, hitPos.z), true, true, false);
        if (result != null && result.hitVec != null)
            hitPos = new Vector3f((float) result.hitVec.x, (float) result.hitVec.y, (float) result.hitVec.z);

        prevPlayerX = (prevPlayerX + (0.01 * limitDistance) < hitPos.x) ? hitPos.x : prevPlayerX;
        prevPlayerY = (prevPlayerY + (0.01 * limitDistance) < hitPos.y) ? hitPos.y : prevPlayerY;
        prevPlayerZ = (prevPlayerZ + (0.01 * limitDistance) < hitPos.z) ? hitPos.z : prevPlayerZ;

        prevPlayerX = (prevPlayerX - (0.01 * limitDistance) > hitPos.x) ? hitPos.x : prevPlayerX;
        prevPlayerY = (prevPlayerY - (0.01 * limitDistance) > hitPos.y) ? hitPos.y : prevPlayerY;
        prevPlayerZ = (prevPlayerZ - (0.01 * limitDistance) > hitPos.z) ? hitPos.z : prevPlayerZ;

        float range = Vector3f.sub(hitPos, origin, null).length();

        float zoom = (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2) ? zoomValue : 1;

        render(prevPlayerX, prevPlayerY, prevPlayerZ, yaw, pitch, range / 60 * zoom, "second-crosshair");
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void changeFirstPersonCrossHair(RenderGameOverlayEvent event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            event.setCanceled(true);
        }
    }

    public void update() throws NoSuchFieldException, IllegalAccessException {
        this.gun = CrossHairHooks.gun;
        this.seat = CrossHairHooks.seat;

        Field reloadTimeRemainingField = gun.getClass().getDeclaredField("reloadTimeRemaining");
        reloadTimeRemainingField.setAccessible(true);
        int reloadTimeRemaining = reloadTimeRemainingField.getInt(gun);

        Field reloadingBulletField = gun.getClass().getDeclaredField("reloadingBullet");
        reloadingBulletField.setAccessible(true);
        ItemBullet reloadingBullet = (ItemBullet) reloadingBulletField.get(gun);

        Field bulletsLeftField = gun.getClass().getDeclaredField("bulletsLeft");
        bulletsLeftField.setAccessible(true);
        int bulletsLeft = bulletsLeftField.getInt(gun);

        if (reloadTimeRemaining > 1 && reloadingBullet != null) {
            double amplifier = ClientUtils.normalize(reloadTimeRemaining, 1, gun.definition.gun.reloadTime);

            int red = (int) Math.abs((amplifier * FAR.getRed()) + ((1 - amplifier) * CLOSE.getRed()));
            int green = (int) Math.abs((amplifier * FAR.getGreen()) + ((1 - amplifier) * CLOSE.getGreen()));
            int blue = (int) Math.abs((amplifier * FAR.getBlue()) + ((1 - amplifier) * CLOSE.getBlue()));

            color.set(red / 255F, green / 255F, blue / 255F);

            isColorSwap = true;
        } else if (reloadTimeRemaining < 1 && bulletsLeft > 0) {
            color.set(0, 1, 0);

            isColorSwap = false;
        } else if (!isColorSwap) {
            color.set(1, 1, 1);
        }

    }

    private void render(double prevX, double prevY, double prevZ, float yaw, float pitch, float scale, String name) {
        Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(CrossHairMod.MODID, "textures/" + name + ".png"));

        GlStateManager.color((float) color.x, (float) color.y, (float) color.z, 1.0F);

        GlStateManager.pushMatrix();
        {
            Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
            assert camera != null;
            double x = (camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partialTicks);
            double y = (camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partialTicks);
            double z = (camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partialTicks);

            GlStateManager.translate(-(float) x, -(float) y, -(float) z);
            GlStateManager.translate(prevX, prevY, prevZ);
            GlStateManager.rotate(yaw, 0, 1, 0);
            GlStateManager.rotate(pitch, 1, 0, 0);
            GlStateManager.scale(scale, scale, scale);

            GlStateManager.enableRescaleNormal();
            GL11.glEnable(3042);
            GL11.glEnable(2832);
            GL11.glHint(3153, 4353);

            GlStateManager.disableDepth();

            GL11.glCallList(ClientProxy.getRender(name));
            GlStateManager.popMatrix();

            GlStateManager.enableDepth();

            GL11.glDisable(3042);
            GL11.glDisable(2832);
            GlStateManager.disableRescaleNormal();

        }
        GlStateManager.popMatrix();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

}
