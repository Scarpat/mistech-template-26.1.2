package com.mistech.mistechmod.fey;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.accessory.Accessories;
import com.mistech.mistechmod.accessory.Accessory;
import com.mistech.mistechmod.entity.goblin.CorruptedGoblinEntity;
import com.mistech.mistechmod.entity.goblin.GoblinKin;
import com.mistech.mistechmod.entity.goblin.GoblinQueenEntity;
import com.mistech.mistechmod.purity.Taint;
import com.mistech.mistechmod.registry.MistechEffects;
import com.mistech.mistechmod.registry.MistechTags;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Véu Feérico (Incenso de Fada) e coroas da Rainha Duende.
 * <ul>
 * <li>Véu Feérico: criaturas da tag {@code mistech:fey_ignores} não miram quem tem o efeito.</li>
 * <li>Coroa (Corrompida ou Purificada): criaturas da vila (menos a Rainha) ficam <b>neutras</b> — só atacam quem bateu nelas.</li>
 * <li>Coroa Corrompida: +{@link #CROWN_TAINT_PER_SECOND} de Mácula por segundo enquanto equipada.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class FeyEvents {
    public static final float CROWN_TAINT_PER_SECOND = 0.02F;
    /** Raio em que duendes que miravam o portador da coroa desistem. */
    public static final double CROWN_CALM_RADIUS = 24.0;

    private FeyEvents() {}

    public static boolean wearsCrown(Player player) {
        return Accessories.has(player, Accessory.CORRUPTED_CROWN) || Accessories.has(player, Accessory.PURIFIED_CROWN);
    }

    /** Criatura da vila que respeita a coroa (a Rainha não; os duendes invocados já não miram jogadores). */
    private static boolean obeysCrown(LivingEntity mob) {
        return mob instanceof GoblinKin && !(mob instanceof GoblinQueenEntity);
    }

    @SubscribeEvent
    static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        LivingEntity mob = event.getEntity();
        // quem apanhou do jogador pode revidar
        if (mob.getLastHurtByMob() == player) {
            return;
        }
        if (player.hasEffect(MistechEffects.FEY_VEIL) && mob.is(MistechTags.FEY_IGNORES)) {
            event.setCanceled(true);
        } else if (obeysCrown(mob) && wearsCrown(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) {
            return;
        }
        boolean corrupted = Accessories.has(player, Accessory.CORRUPTED_CROWN);
        boolean pure = Accessories.has(player, Accessory.PURIFIED_CROWN);
        if (!corrupted && !pure) {
            return;
        }
        if (corrupted && !pure) {
            Taint.add(player, CROWN_TAINT_PER_SECOND);
        }
        // quem já perseguia o portador desiste (menos quem apanhou dele)
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(CROWN_CALM_RADIUS),
                m -> m.getTarget() == player && obeysCrown(m) && !(m instanceof CorruptedGoblinEntity g && g.isSummoned())
                        && m.getLastHurtByMob() != player)) {
            mob.setTarget(null);
        }
    }
}
