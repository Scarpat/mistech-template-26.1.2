package com.mistech.mistechmod.realm.dream;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Flor-Olho (Pesadelo): vigia quem chega perto e entrega para os monstros ({@link DreamRealmEvents}). */
public class EyeFlowerBlock extends DreamFlowerBlock {
    public static final int RANGE = 6;

    public EyeFlowerBlock(BlockBehaviour.Properties properties) {
        super(MobEffects.GLOWING, 8.0F, properties, new DustParticleOptions(0xC21F2F, 0.8F), 10);
    }
}
