package com.mistech.mistechmod.ritual;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

/**
 * Visual de cada ritual (campo "effect" da receita). Cada estilo tem:
 * fios dos pedestais → altar, fios dos incensos, um anel que gira em volta do altar e fecha conforme o ritual avança,
 * uma coluna no centro, um som que se repete e a explosão final. Os estilos "storm" e "ascension" soltam raios só visuais.
 */
public enum RitualEffect implements StringRepresentable {
    IMPURE("impure", ParticleTypes.WITCH, ParticleTypes.REVERSE_PORTAL, ParticleTypes.PORTAL, ParticleTypes.SQUID_INK,
            SoundEvents.AMETHYST_BLOCK_RESONATE, 40, ParticleTypes.WITCH),
    SHADOW("shadow", ParticleTypes.SQUID_INK, ParticleTypes.SMOKE, ParticleTypes.SCULK_SOUL, ParticleTypes.SOUL,
            SoundEvents.SOUL_ESCAPE.value(), 30, ParticleTypes.SCULK_SOUL),
    LIGHT("light", ParticleTypes.END_ROD, ParticleTypes.GLOW, ParticleTypes.END_ROD, ParticleTypes.GLOW,
            SoundEvents.BEACON_AMBIENT, 50, ParticleTypes.END_ROD),
    NATURE("nature", ParticleTypes.HAPPY_VILLAGER, ParticleTypes.COMPOSTER, ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.HAPPY_VILLAGER,
            SoundEvents.AZALEA_LEAVES_PLACE, 25, ParticleTypes.HAPPY_VILLAGER),
    STORM("storm", ParticleTypes.ELECTRIC_SPARK, ParticleTypes.CLOUD, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.CLOUD,
            SoundEvents.BEACON_POWER_SELECT, 35, ParticleTypes.ELECTRIC_SPARK),
    FIRE("fire", ParticleTypes.FLAME, ParticleTypes.SMALL_FLAME, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.LAVA,
            SoundEvents.FIRE_AMBIENT, 30, ParticleTypes.FLAME),
    MIST("mist", ParticleTypes.CLOUD, ParticleTypes.CLOUD, ParticleTypes.SNOWFLAKE, ParticleTypes.CLOUD,
            SoundEvents.FIRE_EXTINGUISH, 45, ParticleTypes.CLOUD),
    LUCK("luck", ParticleTypes.HAPPY_VILLAGER, ParticleTypes.WAX_ON, ParticleTypes.WAX_ON, ParticleTypes.HAPPY_VILLAGER,
            SoundEvents.AMETHYST_BLOCK_CHIME, 20, ParticleTypes.TOTEM_OF_UNDYING),
    ARCANE("arcane", ParticleTypes.ELECTRIC_SPARK, ParticleTypes.ENCHANT, ParticleTypes.ENCHANT, ParticleTypes.WITCH,
            SoundEvents.ENCHANTMENT_TABLE_USE, 30, ParticleTypes.ENCHANT),
    FORGE("forge", ParticleTypes.WAX_OFF, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.CRIT, ParticleTypes.ELECTRIC_SPARK,
            SoundEvents.SMITHING_TABLE_USE, 25, ParticleTypes.ELECTRIC_SPARK),
    ASTRAL("astral", ParticleTypes.END_ROD, ParticleTypes.REVERSE_PORTAL, ParticleTypes.ENCHANT, ParticleTypes.END_ROD,
            SoundEvents.AMETHYST_BLOCK_CHIME, 30, ParticleTypes.END_ROD),
    ASCENSION("ascension", ParticleTypes.END_ROD, ParticleTypes.REVERSE_PORTAL, ParticleTypes.TRIAL_OMEN, ParticleTypes.WITCH,
            SoundEvents.BEACON_AMBIENT, 40, ParticleTypes.TOTEM_OF_UNDYING),
    BLOOD("blood", ParticleTypes.CRIMSON_SPORE, ParticleTypes.DRIPPING_LAVA, ParticleTypes.CRIMSON_SPORE, ParticleTypes.FALLING_LAVA,
            SoundEvents.WARDEN_HEARTBEAT, 20, ParticleTypes.DAMAGE_INDICATOR),
    MOON("moon", ParticleTypes.END_ROD, ParticleTypes.WHITE_ASH, ParticleTypes.SNOWFLAKE, ParticleTypes.GLOW,
            SoundEvents.AMETHYST_BLOCK_CHIME, 25, ParticleTypes.END_ROD),
    ECLIPSE("eclipse", ParticleTypes.SQUID_INK, ParticleTypes.ASH, ParticleTypes.SMOKE, ParticleTypes.END_ROD,
            SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 40, ParticleTypes.SQUID_INK),
    STARS("stars", ParticleTypes.END_ROD, ParticleTypes.FIREWORK, ParticleTypes.FIREWORK, ParticleTypes.END_ROD,
            SoundEvents.AMETHYST_BLOCK_RESONATE, 30, ParticleTypes.FIREWORK);

    public static final Codec<RitualEffect> CODEC = StringRepresentable.fromEnum(RitualEffect::values);

    private final String name;
    private final SimpleParticleType stream;
    private final SimpleParticleType holder;
    private final SimpleParticleType ring;
    private final SimpleParticleType column;
    private final SoundEvent sound;
    private final int soundEvery;
    private final SimpleParticleType burst;

    RitualEffect(String name, SimpleParticleType stream, SimpleParticleType holder, SimpleParticleType ring, SimpleParticleType column,
                 SoundEvent sound, int soundEvery, SimpleParticleType burst) {
        this.name = name;
        this.stream = stream;
        this.holder = holder;
        this.ring = ring;
        this.column = column;
        this.sound = sound;
        this.soundEvery = soundEvery;
        this.burst = burst;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public SimpleParticleType stream() {
        return this.stream;
    }

    public SimpleParticleType holder() {
        return this.holder;
    }

    /** Chamado a cada tick do ritual. {@code fraction} = progresso 0–1. */
    public void tick(ServerLevel level, BlockPos altar, int tier, int progress, float fraction) {
        Vec3 c = Vec3.atCenterOf(altar).add(0, 0.75, 0);
        // anel girando que fecha em direção ao altar
        if (progress % 2 == 0) {
            double radius = (tier >= 2 ? 4.0 : 2.8) * (1.0 - 0.75 * fraction);
            double angle = progress * 0.18;
            for (int i = 0; i < 3; i++) {
                double a = angle + i * (Math.PI * 2 / 3);
                level.sendParticles(this.ring, c.x + Math.cos(a) * radius, c.y - 0.4 + 0.3 * Math.sin(progress * 0.1), c.z + Math.sin(a) * radius,
                        1, 0, 0, 0, 0.0);
            }
        }
        // coluna subindo do altar, mais alta perto do fim
        if (progress % 4 == 0) {
            double h = 0.5 + 3.0 * fraction;
            level.sendParticles(this.column, c.x, c.y + level.getRandom().nextDouble() * h, c.z, 1, 0.08, 0.05, 0.08, 0.0);
        }
        if (progress % this.soundEvery == 0) {
            level.playSound(null, altar, this.sound, SoundSource.BLOCKS, 0.7F, 0.6F + fraction * 0.8F);
        }
        // extras
        if (this == STORM && progress % 100 == 50) {
            visualLightning(level, altar.offset(level.getRandom().nextInt(7) - 3, 0, level.getRandom().nextInt(7) - 3));
        }
        if (this == ASCENSION && progress % 60 == 0) {
            level.playSound(null, altar, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.8F, 0.5F + fraction);
        }
        if (this == FIRE && progress % 20 == 0) {
            level.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 2, 0.3, 0.1, 0.3, 0.0);
        }
    }

    /** Explosão final. */
    public void finish(ServerLevel level, BlockPos altar, int tier) {
        Vec3 c = Vec3.atCenterOf(altar).add(0, 1.3, 0);
        level.sendParticles(this.burst, c.x, c.y, c.z, 40, 0.5, 0.6, 0.5, 0.15);
        level.sendParticles(this.stream, c.x, c.y, c.z, 25, 0.8, 0.5, 0.8, 0.1);
        switch (this) {
            case STORM -> visualLightning(level, altar);
            case ASCENSION -> {
                for (BlockPos p : AltarStructure.obelisks(altar)) {
                    visualLightning(level, p);
                }
                level.playSound(null, altar, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 0.8F, 0.8F);
            }
            case ASTRAL -> level.playSound(null, altar, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
            case LUCK -> level.playSound(null, altar, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7F, 1.4F);
            case FORGE -> level.playSound(null, altar, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.3F);
            case BLOOD -> level.playSound(null, altar, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 0.5F, 0.7F);
            case MOON, STARS -> level.playSound(null, altar, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0F, 1.5F);
            case ECLIPSE -> level.playSound(null, altar, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.5F, 0.5F);
            default -> {
            }
        }
    }

    private static void visualLightning(ServerLevel level, BlockPos pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }
}
