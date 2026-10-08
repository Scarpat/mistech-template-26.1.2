package com.mistech.mistechmod.tarot;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Base para os chefes dos arcanos. Não precisa ser usada (qualquer {@link net.minecraft.world.entity.LivingEntity}
 * criada por {@link ArcanaBosses.Factory} serve), mas já resolve o básico: não some de longe, não troca de dimensão,
 * não sofre dano de queda e sabe de qual arcano é. A barra de vida, a arena e a vitória ficam com o {@link ArcanePlane}.
 * <p>
 * Para fases/ataques, sobrescrever {@link #onFightTick(ArcanePlane.Fight)} (chamado 1x por tick pelo Plano Arcano).
 */
public abstract class ArcanaBossEntity extends Monster {
    protected ArcanaBossEntity(EntityType<? extends ArcanaBossEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public abstract TarotArcana arcana();

    /** Chamado pelo Plano Arcano a cada tick enquanto a luta acontece. */
    public void onFightTick(ArcanePlane.Fight fight) {}

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot(net.minecraft.server.level.ServerLevel level) {
        return false; // o prêmio é a Carta do Arcano, dada pelo Plano Arcano
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }
}
