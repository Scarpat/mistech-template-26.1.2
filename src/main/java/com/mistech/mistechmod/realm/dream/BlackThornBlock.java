package com.mistech.mistechmod.realm.dream;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Espinheiro-Negro (Pesadelo): prende e machuca como o arbusto de bagas (duendes passam ilesos). */
public class BlackThornBlock extends DreamPlantBlock {
    public BlackThornBlock(BlockBehaviour.Properties properties) {
        super(properties, 13.0, null, 1);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
        if (!(entity instanceof LivingEntity living) || living instanceof com.mistech.mistechmod.entity.goblin.GoblinKin) {
            return;
        }
        entity.makeStuckInBlock(state, new Vec3(0.8, 0.75, 0.8));
        if (level instanceof ServerLevel server) {
            double dx = Math.abs(entity.getX() - entity.xOld), dz = Math.abs(entity.getZ() - entity.zOld);
            if (dx >= 0.003 || dz >= 0.003) {
                living.hurtServer(server, server.damageSources().sweetBerryBush(), 1.5F);
            }
        }
    }
}
