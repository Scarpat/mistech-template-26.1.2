package com.mistech.mistechmod.artifact.client;

import org.lwjgl.glfw.GLFW;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.MistechClient;
import com.mistech.mistechmod.artifact.ArtifactContent;
import com.mistech.mistechmod.artifact.ArtifactEvents;
import com.mistech.mistechmod.artifact.ArtifactNetwork;
import com.mistech.mistechmod.item.mithril.MithrilGear;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Cliente dos artefatos: desenho da vassoura, pulo duplo das Botas do Vento e tecla do Anel Espacial. */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class ArtifactClient {
    /** Tecla do Anel Espacial (V). Registrada em {@link MistechClient}. */
    public static final KeyMapping SPATIAL_RING = new KeyMapping("key.mistech.spatial_ring",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, MistechClient.KEY_CATEGORY);

    private static boolean jumpWasDown;
    private static int extraJumps;
    /** Ticks seguidos no ar: o pulo normal já acontece antes deste evento, então o pulo extra só vale depois de alguns ticks. */
    private static int airTicks;

    private ArtifactClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ArtifactContent.BROOM_ENTITY.get(), BroomRenderer::new);
    }

    /** Linha de descrição dos artefatos que usam a classe genérica do mitril. */
    @SubscribeEvent
    static void onTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String id = null;
        if (stack.is(ArtifactContent.WIND_BOOTS.get())) id = "wind_boots";
        else if (stack.is(ArtifactContent.FROST_SKATES.get())) id = "frost_skates";
        else if (stack.is(ArtifactContent.STORM_GAUNTLET.get())) id = "storm_gauntlet";
        else if (stack.is(ArtifactContent.TIDE_MASK.get())) id = "tide_mask";
        else if (stack.is(ArtifactContent.BEACON_CROWN.get())) id = "beacon_crown";
        else if (stack.is(ArtifactContent.SHADOW_CLOAK.get())) id = "shadow_cloak";
        if (id == null) {
            return;
        }
        java.util.List<net.minecraft.network.chat.Component> lines = event.getToolTip();
        int at = Math.min(1, lines.size());
        lines.add(at, net.minecraft.network.chat.Component.translatable("item.mistech." + id + ".desc")
                .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        if (Boolean.TRUE.equals(stack.get(ArtifactContent.CLOUD_STEP.get()))) {
            lines.add(at + 1, net.minecraft.network.chat.Component.translatable("item.mistech.wind_boots.cloud_step")
                    .withStyle(net.minecraft.ChatFormatting.WHITE));
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        while (SPATIAL_RING.consumeClick()) {
            if (player != null && minecraft.screen == null) {
                ClientPacketDistributor.sendToServer(ArtifactNetwork.SpatialRingPayload.INSTANCE);
            }
        }
        if (player == null) {
            return;
        }
        boolean jumpDown = player.input.keyPresses.jump();
        if (player.onGround() || player.onClimbable() || player.isInWater() || player.isPassenger() || player.getAbilities().flying) {
            extraJumps = 0;
            airTicks = 0;
        } else if (++airTicks > 2 && jumpDown && !jumpWasDown && minecraft.screen == null) {
            tryDoubleJump(player);
        }
        jumpWasDown = jumpDown;
    }

    private static void tryDoubleJump(LocalPlayer player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ArtifactContent.WIND_BOOTS.get()) || extraJumps >= ArtifactEvents.maxExtraJumps(boots)
                || (!player.hasInfiniteMaterials() && MithrilGear.getEnergy(boots) < ArtifactEvents.JUMP_COST)) {
            return;
        }
        extraJumps++;
        Vec3 motion = player.getDeltaMovement();
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(motion.x + look.x * 0.15, ArtifactEvents.JUMP_VELOCITY, motion.z + look.z * 0.15);
        player.resetFallDistance();
        ClientPacketDistributor.sendToServer(new ArtifactNetwork.DoubleJumpPayload(extraJumps));
    }
}
