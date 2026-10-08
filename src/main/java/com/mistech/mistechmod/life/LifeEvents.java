package com.mistech.mistechmod.life;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.mithril.GearRunes;
import com.mistech.mistechmod.item.staff.GnomeStaffItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Runa da Vida fora da mão:
 * <ul>
 * <li><b>Engastada</b>: matar uma criatura cura {@link #HEAL_PER_RUNE} por runa (no máximo {@link #MAX_HEAL_PER_SECOND} por segundo).</li>
 * <li><b>Cajado do Gnomo — Fonte da Vida</b>: área de {@link #FOUNTAIN_RADIUS} blocos por {@link #FOUNTAIN_TICKS} que cura os aliados e faz as plantas crescerem.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class LifeEvents {
    public static final float HEAL_PER_RUNE = 1.0F;
    public static final float MAX_HEAL_PER_SECOND = 3.0F;
    public static final int FOUNTAIN_COST = 2_500;
    public static final int FOUNTAIN_TICKS = 6 * 20;
    public static final double FOUNTAIN_RADIUS = 4.0;
    public static final float FOUNTAIN_HEAL = 2.0F;

    /** Por jogador: {segundo do jogo, cura já dada nesse segundo}. */
    private static final Map<UUID, double[]> HEAL_BUDGET = new HashMap<>();

    private record Fountain(ResourceKey<Level> dimension, Vec3 center, UUID caster, int[] ticksLeft) {}

    private static final List<Fountain> FOUNTAINS = new ArrayList<>();

    private LifeEvents() {}

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide() || event.getEntity() == player) {
            return;
        }
        double runes = GearRunes.count(GearRunes.tallyEquipped(player), RuneElement.LIFE);
        if (runes <= 0.0 || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        double second = player.level().getGameTime() / 20L;
        double[] budget = HEAL_BUDGET.computeIfAbsent(player.getUUID(), k -> new double[] {second, 0.0});
        if (budget[0] != second) {
            budget[0] = second;
            budget[1] = 0.0;
        }
        float amount = (float) Math.min(runes * HEAL_PER_RUNE, MAX_HEAL_PER_SECOND - budget[1]);
        if (amount <= 0.0F) {
            return;
        }
        budget[1] += amount;
        player.heal(amount);
        if (player.level() instanceof ServerLevel level) {
            LivingEntity dead = event.getEntity();
            level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 2.1, player.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, dead.getX(), dead.getY() + 0.5, dead.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        }
    }

    /** Magia do Cajado do Gnomo com a Runa da Vida. */
    public static boolean fountain(ServerLevel level, ServerPlayer player, ItemStack staff) {
        if (!GnomeStaffItem.tryUse(player, staff, FOUNTAIN_COST)) {
            return false;
        }
        Vec3 center = player.position();
        FOUNTAINS.add(new Fountain(level.dimension(), center, player.getUUID(), new int[] {FOUNTAIN_TICKS}));
        LifeRuneItem.lifeFx(level, center.x, center.y + 0.5, center.z, FOUNTAIN_RADIUS * 0.5);
        return true;
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (FOUNTAINS.isEmpty()) {
            return;
        }
        Iterator<Fountain> it = FOUNTAINS.iterator();
        while (it.hasNext()) {
            Fountain fountain = it.next();
            ServerLevel level = event.getServer().getLevel(fountain.dimension());
            if (level == null || --fountain.ticksLeft()[0] < 0) {
                it.remove();
                continue;
            }
            Vec3 c = fountain.center();
            // anel de partículas
            if (level.getGameTime() % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = (level.getGameTime() * 0.15) + i * Math.PI / 4.0;
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x + Math.cos(a) * FOUNTAIN_RADIUS, c.y + 0.2, c.z + Math.sin(a) * FOUNTAIN_RADIUS,
                            1, 0.0, 0.0, 0.0, 0.0);
                }
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.x, c.y + 1.5, c.z, 3, 1.5, 0.5, 1.5, 0.0);
            }
            if (fountain.ticksLeft()[0] % 20 != 0) {
                continue;
            }
            Player caster = level.getPlayerByUUID(fountain.caster());
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(BlockPos.containing(c)).inflate(FOUNTAIN_RADIUS),
                    e -> e.position().distanceToSqr(c) <= FOUNTAIN_RADIUS * FOUNTAIN_RADIUS
                            && (caster != null ? LifeRuneItem.isAlly(caster, e) : e instanceof Player))) {
                LifeRuneItem.mend(level, entity, FOUNTAIN_HEAL, 40);
            }
            // 3 plantas ao acaso crescem um pouco
            for (int i = 0; i < 6; i++) {
                BlockPos pos = BlockPos.containing(c.x + (level.getRandom().nextDouble() * 2 - 1) * FOUNTAIN_RADIUS,
                        c.y - 1 + level.getRandom().nextInt(3), c.z + (level.getRandom().nextDouble() * 2 - 1) * FOUNTAIN_RADIUS);
                if (LifeRuneItem.growsFully(level.getBlockState(pos).getBlock())) {
                    net.minecraft.world.item.BoneMealItem.growCrop(new ItemStack(net.minecraft.world.item.Items.BONE_MEAL), level, pos);
                    level.levelEvent(net.minecraft.world.level.block.LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 5);
                }
            }
        }
    }
}
