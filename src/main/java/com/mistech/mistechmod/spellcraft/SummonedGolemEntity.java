package com.mistech.mistechmod.spellcraft;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Golem invocado (Golem de Pedra e Guardião de Madeira): usa a IA e a animação do Golem de Ferro criado por jogador,
 * mas com modelo e textura próprios, sons de pedra ou madeira, sem soltar ferro e sem ser consertado com lingotes.
 */
public class SummonedGolemEntity extends IronGolem {
    public SummonedGolemEntity(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        this.setPlayerCreated(true);
    }

    private boolean wooden() {
        return this.getType() == SpellcraftContent.WOOD_GUARDIAN.get();
    }

    /** Não ataca as outras invocações (o Espírito e a Fênix voam como Vex, que o golem veria como inimigos). */
    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return !target.entityTags().contains(Summons.TAG) && super.canAttack(target);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS; // não aceita lingotes de ferro
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.wooden() ? SoundEvents.WOOD_HIT : SoundEvents.STONE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.wooden() ? SoundEvents.WOOD_BREAK : SoundEvents.STONE_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(this.wooden() ? SoundEvents.WOOD_STEP : SoundEvents.STONE_STEP, 1.0F, 0.6F);
    }
}
