package com.mistech.mistechmod.event;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Sorte das runas:
 * - efeito Fortuna: 25% de chance de dobrar os drops de blocos que o jogador quebra e de criaturas que ele mata;
 * - criatura marcada pela sorte (Runa da Sorte / magia do cajado): drops e XP dobrados se morrer dentro do prazo.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class FortuneEvents {
    public static final String MARK_TAG = "mistech_luck_mark";
    public static final float FORTUNE_CHANCE = 0.25F;

    private FortuneEvents() {}

    public static void mark(LivingEntity target, int ticks) {
        target.getPersistentData().putLong(MARK_TAG, target.level().getGameTime() + ticks);
    }

    public static boolean isMarked(LivingEntity target) {
        long until = target.getPersistentData().getLongOr(MARK_TAG, 0L);
        return until > 0 && target.level().getGameTime() <= until;
    }

    private static boolean rollFortune(Entity entity) {
        return entity instanceof LivingEntity living && living.hasEffect(MistechEffects.FORTUNE)
                && living.getRandom().nextFloat() < FORTUNE_CHANCE;
    }

    private static List<ItemEntity> copies(ServerLevel level, java.util.Collection<ItemEntity> drops) {
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity drop : drops) {
            ItemEntity copy = new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
            copy.setDefaultPickUpDelay();
            extra.add(copy);
        }
        return extra;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onBlockDrops(BlockDropsEvent event) {
        if (event.getBreaker() != null && !event.getDrops().isEmpty() && rollFortune(event.getBreaker())) {
            event.getDrops().addAll(copies(event.getLevel(), event.getDrops()));
            event.getLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5,
                    event.getPos().getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || event.getDrops().isEmpty()) {
            return;
        }
        if (isMarked(dead) || rollFortune(event.getSource().getEntity())) {
            event.getDrops().addAll(copies(level, event.getDrops()));
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, dead.getX(), dead.getY() + 0.5, dead.getZ(), 12, 0.4, 0.4, 0.4, 0.0);
        }
    }

    @SubscribeEvent
    static void onExperience(LivingExperienceDropEvent event) {
        if (isMarked(event.getEntity())) {
            event.setDroppedExperience(event.getDroppedExperience() * 2);
        }
    }
}
