package com.mistech.mistechmod.client.projection;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Config;
import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.multiblock.GuideStoneEvents;
import com.mistech.mistechmod.multiblock.MultiblockPlans;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Holograma dos multiblocos e alcance dos blocos, desenhados com os "gizmos" do jogo (contornos e caixas translúcidas).
 * <ul>
 * <li>Agachado + botão direito com a Pedra Guia numa âncora: mostra a planta (clicar de novo troca de planta, e depois
 * da última desliga). Agachado + botão direito no ar desliga.</li>
 * <li>Com a Pedra Guia na mão (agachado, pelo config): mostra o alcance dos blocos por perto ({@link RangeDisplay}).</li>
 * </ul>
 * Cores: azul = falta, vermelho = bloco errado, verde = certo, dourado tracejado = opcional.
 */
@EventBusSubscriber(modid = Mistech.MODID, value = Dist.CLIENT)
public final class GuideStoneClient {
    private static @Nullable BlockPos anchor;
    private static @Nullable ResourceKey<Level> anchorLevel;
    private static int planIndex;
    private static boolean announcedComplete;

    private GuideStoneClient() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || !GuideStoneEvents.isProjectionClick(event)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos().immutable();
        List<MultiblockPlans.Plan> plans = MultiblockPlans.plans(level, pos);
        if (plans.isEmpty()) {
            return;
        }
        if (pos.equals(anchor) && level.dimension() == anchorLevel) {
            planIndex++;
        } else {
            anchor = pos;
            anchorLevel = level.dimension();
            planIndex = 0;
        }
        announcedComplete = false;
        if (planIndex >= plans.size()) {
            clear(true);
            return;
        }
        MultiblockPlans.Plan plan = plans.get(planIndex);
        int missing = plan.missing(level);
        Component msg = Component.translatable("message.mistech.projection.show", plan.title(),
                missing == 0 ? Component.translatable("message.mistech.projection.complete").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("message.mistech.projection.missing", missing).withStyle(ChatFormatting.AQUA));
        if (plans.size() > 1) {
            msg = msg.copy().append(Component.translatable("message.mistech.projection.cycle", planIndex + 1, plans.size())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        event.getEntity().sendOverlayMessage(msg);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F));
    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() && anchor != null && event.getEntity().isShiftKeyDown()
                && event.getItemStack().is(Mistech.GUIDE_STONE.get())) {
            clear(true);
        }
    }

    private static void clear(boolean message) {
        anchor = null;
        anchorLevel = null;
        planIndex = 0;
        LocalPlayer player = Minecraft.getInstance().player;
        if (message && player != null) {
            player.sendOverlayMessage(Component.translatable("message.mistech.projection.off"));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F));
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        try {
            drawProjection(mc, player);
            RangeDisplay.draw(mc, player);
        } catch (IllegalStateException ignored) {
            // sem coletor de gizmos neste momento (ex.: carregando o mundo)
        }
    }

    private static void drawProjection(Minecraft mc, LocalPlayer player) {
        if (anchor == null) {
            return;
        }
        Level level = mc.level;
        if (level.dimension() != anchorLevel || anchor.distSqr(player.blockPosition()) > 96 * 96) {
            clear(false);
            return;
        }
        List<MultiblockPlans.Plan> plans = MultiblockPlans.plans(level, anchor);
        if (planIndex >= plans.size()) {
            clear(false);
            return;
        }
        MultiblockPlans.Plan plan = plans.get(planIndex);
        int alpha = Config.hologramAlpha();
        int missing = 0;
        Vec3 eye = player.getEyePosition();
        for (MultiblockPlans.Piece piece : plan.pieces()) {
            boolean ok = piece.isOk(level);
            BlockPos pos = piece.pos();
            AABB box = new AABB(pos);
            if (ok) {
                Gizmos.cuboid(box.deflate(0.02), GizmoStyle.stroke(0x8040E070, 1.5F));
                continue;
            }
            boolean empty = level.getBlockState(pos).canBeReplaced();
            if (!piece.optional()) {
                missing++;
            }
            int rgb = piece.optional() ? 0xE8C060 : empty ? 0x50A8FF : 0xFF4040;
            Gizmos.cuboid(box.deflate(empty ? 0.18 : -0.01), GizmoStyle.strokeAndFill(0xE0000000 | rgb, 2.0F, (alpha << 24) | rgb));
            if (Config.hologramLabels() && pos.getCenter().distanceToSqr(eye) < 7 * 7) {
                String text = piece.name().getString() + (piece.optional()
                        ? " " + Component.translatable("multiblock.mistech.optional").getString() : "");
                Gizmos.billboardText(text, pos.getCenter().add(0, 0.75, 0), TextGizmo.Style.forColorAndCentered(0xFF000000 | rgb).withScale(0.25F))
                        .setAlwaysOnTop();
            }
        }
        // título em cima da âncora
        String title = plan.title().getString() + "  " + (missing == 0
                ? Component.translatable("message.mistech.projection.complete").getString()
                : Component.translatable("message.mistech.projection.missing", missing).getString());
        Gizmos.billboardText(title, anchor.getCenter().add(0, 1.4, 0),
                TextGizmo.Style.forColorAndCentered(missing == 0 ? 0xFF60F080 : 0xFFE3C98A).withScale(0.4F)).setAlwaysOnTop();
        if (missing == 0 && !announcedComplete) {
            announcedComplete = true;
            player.sendOverlayMessage(Component.translatable("message.mistech.projection.show", plan.title(),
                    Component.translatable("message.mistech.projection.complete").withStyle(ChatFormatting.GREEN)));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.6F));
        } else if (missing > 0) {
            announcedComplete = false;
        }
    }
}
