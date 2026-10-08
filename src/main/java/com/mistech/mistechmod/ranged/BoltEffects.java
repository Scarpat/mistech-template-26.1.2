package com.mistech.mistechmod.ranged;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.life.LifeRuneItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * O que cada runa faz quando o virote da Besta Mágica acerta. (Ar, Vazio, Eco e Nuvens agem no disparo, em
 * {@link ArcaneCrossbowItem}.) Aliados do atirador ({@link LifeRuneItem#isAlly}) não são atingidos pelas áreas.
 */
public final class BoltEffects {
    private BoltEffects() {}

    private static List<LivingEntity> foes(ServerLevel level, Vec3 at, double radius, @Nullable Player shooter) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(at)).inflate(radius),
                e -> e.isAlive() && e.position().distanceToSqr(at) <= radius * radius)) {
            if (shooter == null || !LifeRuneItem.isAlly(shooter, e)) {
                out.add(e);
            }
        }
        return out;
    }

    public static void apply(ServerLevel level, Entity bolt, @Nullable Player shooter, Vec3 at, @Nullable LivingEntity target,
            @Nullable BlockHitResult block) {
        RuneElement element = bolt instanceof ArcaneBoltEntity b ? b.element() : RuneElement.FIRE;
        BlockPos pos = BlockPos.containing(at);
        Direction face = block != null ? block.getDirection() : Direction.UP;
        switch (element) {
            case FIRE -> level.explode(shooter, at.x, at.y, at.z, 2.0F, false, Level.ExplosionInteraction.NONE);
            case WATER -> {
                for (LivingEntity e : foes(level, at, 3.0, shooter)) {
                    e.clearFire();
                    Vec3 push = e.position().subtract(at).multiply(1, 0, 1).normalize().scale(1.2);
                    e.push(push.x, 0.35, push.z);
                    e.hurtMarked = true;
                }
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof BaseFireBlock) {
                        level.removeBlock(p, false);
                    } else if (s.getFluidState().is(Fluids.LAVA) && s.getFluidState().isSource()) {
                        level.setBlockAndUpdate(p, Blocks.OBSIDIAN.defaultBlockState());
                    }
                }
                level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.5, at.z, 60, 1.5, 0.5, 1.5, 0.1);
                level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            case EARTH -> earthWall(level, bolt, block != null ? block.getBlockPos().relative(face) : pos);
            case ICE -> {
                for (LivingEntity e : foes(level, at, 3.0, shooter)) {
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 3), shooter);
                    e.setTicksFrozen(Math.max(e.getTicksFrozen(), 200));
                }
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 1, 3))) {
                    if (p.distSqr(pos) <= 9 && level.getBlockState(p).is(Blocks.WATER) && level.getFluidState(p).isSource()
                            && level.getBlockState(p.above()).isAir()) {
                        level.setBlockAndUpdate(p, Blocks.FROSTED_ICE.defaultBlockState());
                    }
                }
                level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.5, at.z, 50, 1.5, 0.6, 1.5, 0.02);
                level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.5F);
            }
            case ENERGY -> chainLightning(level, at, shooter, target);
            case LIGHT -> {
                for (LivingEntity e : foes(level, at, 12.0, shooter)) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0), shooter);
                    if (e.is(EntityTypeTags.UNDEAD)) {
                        e.hurtServer(level, level.damageSources().indirectMagic(bolt, shooter), 6.0F);
                        e.invulnerableTime = 0;
                    }
                }
                level.sendParticles(ParticleTypes.GLOW, at.x, at.y + 0.5, at.z, 25, 1.0, 1.0, 1.0, 0.1);
                level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.5, at.z, 40, 1.0, 1.0, 1.0, 0.2);
            }
            case DARKNESS -> cloud(level, at, shooter, 3.0F, 120, ParticleTypes.SQUID_INK,
                    new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            case VAPOR -> cloud(level, at, shooter, 3.5F, 160, ParticleTypes.CLOUD, new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            case SPACE -> {
                if (shooter != null && shooter.level() == level) {
                    Vec3 to = block != null ? Vec3.atBottomCenterOf(block.getBlockPos().relative(face)) : at;
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, shooter.getX(), shooter.getY() + 1, shooter.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
                    shooter.teleportTo(to.x, to.y, to.z);
                    shooter.resetFallDistance();
                    level.playSound(null, shooter.blockPosition(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                    level.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 1, to.z, 40, 0.4, 0.8, 0.4, 0.3);
                }
            }
            case TIME -> {
                for (LivingEntity e : foes(level, at, 4.0, shooter)) {
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 2), shooter);
                    e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120, 1), shooter);
                }
                if (shooter != null && shooter.distanceToSqr(at) < 16 * 16) {
                    shooter.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0));
                }
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 0.5, at.z, 50, 2.0, 0.6, 2.0, 0.02);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 0.6F);
            }
            case MAGMA -> {
                if (target != null) {
                    target.igniteForSeconds(5.0F);
                }
                BlockPos ground = block != null && face == Direction.UP ? block.getBlockPos() : pos.below();
                for (BlockPos p : BlockPos.betweenClosed(ground.offset(-1, 0, -1), ground.offset(1, 0, 1))) {
                    BlockState s = level.getBlockState(p);
                    if (s.isSolidRender() && level.getBlockState(p.above()).isAir()) {
                        TempBlocks.place(level, p.immutable(), Blocks.MAGMA_BLOCK.defaultBlockState(), 200);
                    }
                }
                level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.3, at.z, 15, 1.0, 0.2, 1.0, 0.0);
                level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            case VIGIL -> {
                for (LivingEntity e : foes(level, at, 3.0, shooter)) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), shooter);
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1), shooter);
                }
            }
            case SOUL -> {
                for (LivingEntity e : foes(level, at, 2.5, shooter)) {
                    e.igniteForSeconds(4.0F);
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1), shooter);
                }
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.3, at.z, 30, 1.0, 0.3, 1.0, 0.02);
            }
            case LIFE -> {
                if (shooter != null) {
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0),
                            e -> e.position().distanceToSqr(at) <= 9.0 && LifeRuneItem.isAlly(shooter, e))) {
                        LifeRuneItem.mend(level, e, 6.0F, 100);
                    }
                }
                LifeRuneItem.lifeFx(level, at.x, at.y + 0.3, at.z, 1.5);
            }
            case LUCK -> level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 0.5, at.z, 15, 0.5, 0.5, 0.5, 0.0);
            case ARCANE -> {
                if (target != null) {
                    target.hurtServer(level, level.damageSources().indirectMagic(bolt, shooter), 4.0F);
                    target.invulnerableTime = 0;
                }
                level.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 0.3, at.z, 30, 0.5, 0.5, 0.5, 0.5);
            }
            default -> level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.2);
        }
    }

    private static void cloud(ServerLevel level, Vec3 at, @Nullable Player shooter, float radius, int duration,
            net.minecraft.core.particles.ParticleOptions particle, MobEffectInstance... effects) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
        if (shooter != null) {
            cloud.setOwner(shooter);
        }
        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(0.0F);
        cloud.setCustomParticle(particle);
        for (MobEffectInstance e : effects) {
            cloud.addEffect(e);
        }
        level.addFreshEntity(cloud);
    }

    /** Muralha de pedra 5 × 3, de frente para o atirador, que some em 10 s. */
    private static void earthWall(ServerLevel level, Entity bolt, BlockPos base) {
        Vec3 dir = bolt.getDeltaMovement();
        Direction across = Math.abs(dir.x) > Math.abs(dir.z) ? Direction.SOUTH : Direction.EAST;
        // desce até o chão
        BlockPos start = base;
        for (int i = 0; i < 4 && level.getBlockState(start.below()).canBeReplaced(); i++) {
            start = start.below();
        }
        int placed = 0;
        for (int w = -2; w <= 2; w++) {
            for (int h = 0; h < 3; h++) {
                BlockPos p = start.relative(across, w).above(h);
                if (level.getBlockState(p).canBeReplaced() && level.getFluidState(p).isEmpty()) {
                    if (TempBlocks.place(level, p, Blocks.PACKED_MUD.defaultBlockState(), 200)) {
                        placed++;
                    }
                }
            }
        }
        if (placed > 0) {
            level.playSound(null, start, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.PLAYERS, 1.2F, 0.7F);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, start.getX() + 0.5, start.getY() + 1, start.getZ() + 0.5, 6, 1.5, 0.5, 1.5, 0.01);
        }
    }

    /** Raio no alvo e em mais 2 inimigos a até 6 blocos (5 de dano cada, sem fogo). */
    private static void chainLightning(ServerLevel level, Vec3 at, @Nullable Player shooter, @Nullable LivingEntity first) {
        List<LivingEntity> hit = new ArrayList<>();
        if (first != null) {
            hit.add(first);
        }
        for (LivingEntity e : foes(level, at, 6.0, shooter)) {
            if (hit.size() >= 3) {
                break;
            }
            if (!hit.contains(e) && e instanceof net.minecraft.world.entity.monster.Enemy) {
                hit.add(e);
            }
        }
        if (hit.isEmpty()) {
            strike(level, at);
            return;
        }
        for (LivingEntity e : hit) {
            strike(level, e.position());
            e.hurtServer(level, level.damageSources().lightningBolt(), 5.0F);
            e.invulnerableTime = 0;
        }
    }

    private static void strike(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.snapTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }
}
