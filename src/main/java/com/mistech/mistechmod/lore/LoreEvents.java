package com.mistech.mistechmod.lore;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Mistech;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Caipora: cada animal morto por um jogador soma 1 de "caça" (desce 1 a cada 2 min). Com {@link #SIN_LIMIT} ou mais,
 * numa mata (floresta, selva, taiga ou Mata Ancestral), a Caipora aparece brava atrás dele.
 * Bênção da Caça = animais soltam o dobro (e não contam caça); Maldição da Caipora = animais não soltam nada.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class LoreEvents {
    public static final int SIN_LIMIT = 12;
    public static final int SIN_DECAY_TICKS = 2400;

    private LoreEvents() {}

    /** Chefe que nasceu de uma toca morreu: a toca fica "derrotada" e só acorda outro depois de uns dias. */
    @SubscribeEvent
    static void onBossDeath(LivingDeathEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !entity.getPersistentData().contains(LairCoreBlockEntity.LAIR_TAG)) {
            return;
        }
        BlockPos core = BlockPos.of(entity.getPersistentData().getLongOr(LairCoreBlockEntity.LAIR_TAG, 0L));
        if (level.isLoaded(core) && level.getBlockEntity(core) instanceof LairCoreBlockEntity be) {
            be.onBossDefeated();
        }
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Animal animal) || animal instanceof TamableAnimal t && t.isTame()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || player.hasEffect(LoreContent.HUNT_BLESSING)) {
            return;
        }
        int sin = player.getData(LoreContent.HUNT_SIN) + 1;
        player.setData(LoreContent.HUNT_SIN, sin);
        if (sin == SIN_LIMIT - 3) {
            player.sendSystemMessage(Component.translatable("message.mistech.caipora.warning").withStyle(ChatFormatting.DARK_GREEN));
        }
        if (sin >= SIN_LIMIT) {
            summonAngry(player);
        }
    }

    private static boolean inWilds(ServerPlayer player) {
        var biome = player.level().getBiome(player.blockPosition());
        return biome.is(LoreContent.ANCESTRAL_FOREST) || biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_JUNGLE) || biome.is(BiomeTags.IS_TAIGA);
    }

    private static void summonAngry(ServerPlayer player) {
        ServerLevel level = player.level();
        if (!inWilds(player) || !level.getEntitiesOfClass(CaiporaEntity.class, player.getBoundingBox().inflate(64), CaiporaEntity::isAngry).isEmpty()) {
            return;
        }
        for (int i = 0; i < 12; i++) {
            double a = player.getRandom().nextDouble() * Math.PI * 2;
            double d = 14 + player.getRandom().nextDouble() * 8;
            int x = (int) Math.floor(player.getX() + Math.cos(a) * d), z = (int) Math.floor(player.getZ() + Math.sin(a) * d);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (Math.abs(top.getY() - player.getY()) > 8) {
                continue;
            }
            CaiporaEntity caipora = LoreContent.CAIPORA.get().create(level, EntitySpawnReason.EVENT);
            if (caipora == null) {
                return;
            }
            caipora.snapTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, player.getRandom().nextFloat() * 360, 0);
            caipora.hunt(player);
            level.addFreshEntity(caipora);
            player.setData(LoreContent.HUNT_SIN, SIN_LIMIT / 2);
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.HOSTILE, 2.0F, 1.8F);
            player.sendSystemMessage(Component.translatable("message.mistech.caipora.coming").withStyle(ChatFormatting.DARK_RED));
            return;
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % 200 == 0 && player instanceof ServerPlayer sp) {
            LoreRituals.sleeplessTick(sp);
        }
        if (player.tickCount % SIN_DECAY_TICKS != 0) {
            return;
        }
        int sin = player.getData(LoreContent.HUNT_SIN);
        if (sin > 0) {
            player.setData(LoreContent.HUNT_SIN, sin - 1);
        }
    }

    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Animal) || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (player.hasEffect(LoreContent.CAIPORA_CURSE)) {
            event.getDrops().clear();
            return;
        }
        if (player.hasEffect(LoreContent.HUNT_BLESSING)) {
            List<ItemEntity> extra = new ArrayList<>();
            for (ItemEntity drop : event.getDrops()) {
                extra.add(new ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
            }
            event.getDrops().addAll(extra);
        }
    }
}
