package com.mistech.mistechmod.purity.item;

import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.Taint;
import com.mistech.mistechmod.purity.block.TaintedSoilBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pêndulo de Ametista (radiestesia): mede a Mácula.
 * No ar = a sua; num bloco = a contaminação do solo; num jogador = a dele. Segurando, o HUD mostra a sua.
 */
public class DowsingPendulumItem extends Item {
    public DowsingPendulumItem(Item.Properties properties) {
        super(properties);
    }

    public static boolean isHeld(Player player) {
        return player.getMainHandItem().getItem() instanceof DowsingPendulumItem || player.getOffhandItem().getItem() instanceof DowsingPendulumItem;
    }

    private static void swing(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            if (player.isShiftKeyDown()) {
                seek(server, player);
            } else {
                player.sendOverlayMessage(Component.translatable("message.mistech.pendulum.self", Taint.describe(Taint.get(player)))
                        .append(" · ").append(Component.translatable("message.mistech.pendulum.place",
                                Taint.describe(com.mistech.mistechmod.purity.area.AreaTaint.get(server, player.blockPosition())))));
                swing(level, player);
            }
        }
        player.getCooldowns().addCooldown(player.getItemInHand(hand), player.isShiftKeyDown() ? 30 : 10);
        return InteractionResult.SUCCESS;
    }

    /** Raio da busca da Mácula do lugar, em chunks. */
    public static final int SEEK_RADIUS = 4;

    /**
     * Agachado: aponta o chunk mais maculado num raio de {@link #SEEK_RADIUS} chunks.
     * O pêndulo balança (som e partículas) mais forte quanto pior o lugar.
     */
    private static void seek(net.minecraft.server.level.ServerLevel level, Player player) {
        int[] worst = com.mistech.mistechmod.purity.area.AreaTaint.worstAround(level, player.blockPosition(), SEEK_RADIUS);
        float value = worst[2] / 100.0F;
        if (value < 1.0F) {
            player.sendOverlayMessage(Component.translatable("message.mistech.pendulum.seek_clean").withStyle(ChatFormatting.YELLOW));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 1.6F);
            return;
        }
        double tx = (worst[0] << 4) + 8.5, tz = (worst[1] << 4) + 8.5;
        double dx = tx - player.getX(), dz = tz - player.getZ();
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        Component where;
        if (worst[0] == (player.getBlockX() >> 4) && worst[1] == (player.getBlockZ() >> 4)) {
            where = Component.translatable("message.mistech.pendulum.seek_here");
        } else {
            String[] dirs = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};
            double angle = Math.toDegrees(Math.atan2(-dx, dz));
            int index = (int) Math.floorMod(Math.round(angle / 45.0), 8);
            where = Component.translatable("message.mistech.pendulum.seek_dir", Component.translatable("direction.mistech." + dirs[index]), distance);
        }
        player.sendOverlayMessage(Component.translatable("message.mistech.pendulum.seek", Taint.describe(value), where)
                .withStyle(ChatFormatting.DARK_PURPLE));
        // balanço: mais forte quanto pior
        float strength = Math.min(1.0F, value / Taint.MAX);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                0.4F + strength, 1.4F - strength * 0.6F);
        net.minecraft.world.phys.Vec3 look = new net.minecraft.world.phys.Vec3(dx, 0, dz).normalize();
        for (int i = 1; i <= 6; i++) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, player.getX() + look.x * i * 0.5, player.getEyeY() - 0.4,
                    player.getZ() + look.z * i * 0.5, 1 + Math.round(strength * 3), 0.05, 0.05, 0.05, 0.0);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                seek(server, player);
                player.getCooldowns().addCooldown(context.getItemInHand(), 30);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            BlockPos pos = context.getClickedPos();
            BlockState state = level.getBlockState(pos);
            Component message;
            if (state.is(PurityContent.TAINTED_SOIL.get())) {
                int strength = state.getValue(TaintedSoilBlock.STRENGTH);
                message = Component.translatable("message.mistech.pendulum.soil", strength, TaintedSoilBlock.MAX_STRENGTH)
                        .withStyle(ChatFormatting.DARK_PURPLE);
            } else if (state.is(PurityContent.IMPURE_BLOCKS)) {
                message = Component.translatable("message.mistech.pendulum.impure_block", state.getBlock().getName()).withStyle(ChatFormatting.DARK_PURPLE);
            } else {
                message = Component.translatable("message.mistech.pendulum.clean").withStyle(ChatFormatting.YELLOW);
            }
            float area = com.mistech.mistechmod.purity.area.AreaTaint.get(level, pos);
            if (area >= 1.0F) {
                message = message.copy().append(" · ").append(Component.translatable("message.mistech.pendulum.place", Taint.describe(area)));
            }
            player.sendOverlayMessage(message);
            swing(level, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Player other) {
            if (!player.level().isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.mistech.pendulum.other", other.getDisplayName(), Taint.describe(Taint.get(other))));
                swing(player.level(), player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
