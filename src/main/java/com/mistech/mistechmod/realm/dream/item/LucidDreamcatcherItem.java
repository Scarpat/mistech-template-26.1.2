package com.mistech.mistechmod.realm.dream.item;

import java.util.function.Consumer;

import com.mistech.mistechmod.realm.DreamLayout;
import com.mistech.mistechmod.realm.GnomeRealm;
import com.mistech.mistechmod.realm.GnomeRealmHouse;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Apanha-Sonho Lúcido: viagem entre as camadas do Sonho. Só funciona dentro da Dimensão dos Gnomos e só leva a camadas que
 * você já visitou andando. Agachado + uso: escolhe a camada. Uso: vai para ela, na mesma direção em que você está
 * (Sonho Comum = Casa do Rei). Chega com Queda Lenta. Recarga de 5 s, 32 usos.
 */
public class LucidDreamcatcherItem extends Item {
    /** Bits das camadas visitadas (1 &lt;&lt; anel), em {@code getPersistentData()}. */
    public static final String VISITED_TAG = "mistech_dream_rings";
    private static final String[] NAMES = {"", "common", "nightmare", "deep", "rem"};
    private static final int[] DISTANCE = {0, 0, 1200, 3500, 9000};

    public LucidDreamcatcherItem(Item.Properties properties) {
        super(properties);
    }

    public static int target(ItemStack stack) {
        int r = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("Ring", 1);
        return r < 1 || r > 4 ? 1 : r;
    }

    public static boolean visited(Player player, int ring) {
        return ring == 1 || (player.getPersistentData().getIntOr(VISITED_TAG, 0) & (1 << ring)) != 0;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            int ring = target(stack);
            for (int i = 0; i < 4; i++) {
                ring = ring % 4 + 1;
                if (visited(player, ring)) {
                    break;
                }
            }
            int chosen = ring;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("Ring", chosen));
            player.sendOverlayMessage(Component.translatable("message.mistech.lucid_dreamcatcher.selected",
                    Component.translatable("ring.mistech." + NAMES[chosen])));
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.6F + chosen * 0.2F);
            return InteractionResult.SUCCESS;
        }
        if (!GnomeRealm.isRealm(level)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.lucid_dreamcatcher.not_dreaming"));
            return InteractionResult.FAIL;
        }
        int ring = target(stack);
        if (!visited(player, ring)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.lucid_dreamcatcher.unknown"));
            return InteractionResult.FAIL;
        }
        BlockPos to = ring == 1 ? GnomeRealmHouse.ensureBuilt(server) : this.findSpot(server, player, DISTANCE[ring]);
        if (to == null) {
            player.sendOverlayMessage(Component.translatable("message.mistech.lucid_dreamcatcher.lost"));
            return InteractionResult.FAIL;
        }
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        sp.teleportTo(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5);
        sp.resetFallDistance();
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, true));
        sp.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0, false, false));
        server.playSound(null, to, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.7F);
        player.getCooldowns().addCooldown(stack, 100);
        stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        return InteractionResult.SUCCESS;
    }

    /** Um chão firme na direção em que o jogador está, à distância {@code dist} do centro (procura em espiral). */
    private BlockPos findSpot(ServerLevel level, Player player, int dist) {
        double a = Math.atan2(player.getZ(), player.getX());
        if (Math.abs(player.getX()) + Math.abs(player.getZ()) < 1) {
            a = level.getRandom().nextDouble() * Math.PI * 2;
        }
        for (int i = 0; i < 16; i++) {
            double aa = a + (i % 2 == 0 ? 1 : -1) * (i / 2) * 0.004;
            double dd = dist + ((i / 4) % 2 == 0 ? 1 : -1) * (i / 4) * 24;
            int x = (int) Math.round(Math.cos(aa) * dd), z = (int) Math.round(Math.sin(aa) * dd);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y > 30 && y < level.getMaxY() - 2) {
                return new BlockPos(x, y, z);
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.mistech.lucid_dreamcatcher.target",
                Component.translatable("ring.mistech." + NAMES[target(stack)])).withStyle(ChatFormatting.LIGHT_PURPLE));
        builder.accept(Component.translatable("tooltip.mistech.lucid_dreamcatcher.help").withStyle(ChatFormatting.GRAY));
    }

    /** Camada (1-4) do nome curto, para mensagens. */
    public static int ringOf(double x, double z) {
        return Math.min(4, DreamLayout.ring(x, z));
    }
}
