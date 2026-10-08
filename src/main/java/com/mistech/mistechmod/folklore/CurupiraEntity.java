package com.mistech.mistechmod.folklore;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Curupira (familiar puro, do Santuário). Cabelo de fogo, pés virados para trás.
 * Alimentado (Folha de Tabaco Seca):
 * <ul>
 * <li><b>Pés virados</b>: monstros que perseguem o dono de longe perdem o rastro;</li>
 * <li><b>Guardião da mata</b>: planta as mudas caídas no chão por perto e faz as mudas plantadas crescerem.</li>
 * </ul>
 */
public class CurupiraEntity extends FolkFamiliar {
    public static final int RANGE = 12;

    public CurupiraEntity(EntityType<? extends CurupiraEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.34);
    }

    @Override
    protected void workSecond(ServerLevel level, Player owner) {
        if (!this.isFed()) {
            return;
        }
        RandomSource random = this.getRandom();
        // pés virados: quem segue o rastro do dono vai para o lado errado
        for (Mob mob : level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(24), m -> m instanceof Enemy && m.getTarget() == owner)) {
            if (mob.distanceToSqr(owner) > 6 * 6 && random.nextFloat() < 0.3F) {
                mob.setTarget(null);
                mob.getNavigation().stop();
                level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
            }
        }
        if (this.tickCount % 100 == 0) {
            this.plantSaplings(level);
        }
        if (this.tickCount % 200 == 0) {
            this.growSapling(level, random);
        }
    }

    private void plantSaplings(ServerLevel level) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(this.blockPosition()).inflate(RANGE),
                e -> e.getItem().is(ItemTags.SAPLINGS) && e.getItem().getItem() instanceof BlockItem)) {
            ItemStack stack = item.getItem();
            BlockPos pos = item.blockPosition();
            BlockState sapling = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
            if (level.getBlockState(pos).isAir() && sapling.canSurvive(level, pos)) {
                level.setBlockAndUpdate(pos, sapling);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(stack);
                }
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.0);
                this.playSound(SoundEvents.AZALEA_LEAVES_PLACE, 1.0F, 1.2F);
                return; // uma por vez
            }
        }
    }

    private void growSapling(ServerLevel level, RandomSource random) {
        for (int i = 0; i < 20; i++) {
            BlockPos pos = this.blockPosition().offset(random.nextInt(RANGE * 2 + 1) - RANGE, random.nextInt(7) - 3, random.nextInt(RANGE * 2 + 1) - RANGE);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.SAPLINGS) && state.getBlock() instanceof BonemealableBlock grow && grow.isValidBonemealTarget(level, pos, state)) {
                grow.performBonemeal(level, random, pos, state);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.4, 0.4, 0.4, 0.0);
                return;
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // brasas soltando do cabelo
        if (this.level().isClientSide() && this.getRandom().nextInt(6) == 0) {
            this.level().addParticle(ParticleTypes.SMALL_FLAME, this.getX() + (this.getRandom().nextDouble() - 0.5) * 0.3,
                    this.getY() + this.getBbHeight() + 0.1, this.getZ() + (this.getRandom().nextDouble() - 0.5) * 0.3, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.NOTE_BLOCK_FLUTE.value(); // o assobio do Curupira
    }

    @Override
    public float getVoicePitch() {
        return 1.4F + this.getRandom().nextFloat() * 0.4F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }
}
