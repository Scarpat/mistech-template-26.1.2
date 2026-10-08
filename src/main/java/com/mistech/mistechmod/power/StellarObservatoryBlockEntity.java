package com.mistech.mistechmod.power;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.astrology.AstrologyField;
import com.mistech.mistechmod.machine.MachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Observatório Estelar (Gerador Astral avançado): precisa de um <b>Fragmento Estelar</b> como lente (não é gasto) e de céu aberto.
 * Noite: lua nova 450 FE/t, crescente/minguante 380, quartos 320, gibosa 260, cheia 200. Dia: 60 FE/t.
 * Chuva ×0,5, tempestade ×0,25. +25% com Mesa de Astrologia completa a até 16 blocos; +25% acima de Y {@link #HIGH_ALTITUDE}.
 * Buffer {@link #CAPACITY} FE; envia até {@link #MAX_OUTPUT} FE/t para cada bloco encostado.
 * info0 = FE/t; info1 = fase (bits 0-2), céu (bits 3-4: 0 noite aberta, 1 dia, 2 coberto, 3 chuva), Mesa (bit 5), altitude (bit 6), sem lente (bit 7).
 */
public class StellarObservatoryBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 400_000;
    public static final int MAX_OUTPUT = 1_000;
    public static final int DAY_RATE = 60;
    public static final int HIGH_ALTITUDE = 128;
    public static final int TABLE_RANGE = 16;
    public static final int SLOT_LENS = 0;

    private static final int[] RATE_BY_PHASE = {200, 260, 320, 380, 450, 380, 320, 260};

    private int rate;
    private int checkTimer;
    private int tableTimer;
    private boolean tableBonus;

    public StellarObservatoryBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.STELLAR_OBSERVATORY_BE.get(), pos, state, PowerLayouts.STELLAR_OBSERVATORY, 0, MAX_OUTPUT);
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        if (--this.tableTimer <= 0) {
            this.tableTimer = 100;
            this.tableBonus = findTable(level, pos);
        }
        if (--this.checkTimer <= 0) {
            this.checkTimer = 20;
            this.recalculate(level, pos);
        }
        int space = this.layout.capacity - this.storedEnergy();
        int generated = Math.min(this.rate, space);
        if (generated > 0) {
            this.addEnergy(generated);
        }
        this.info0 = generated;
        this.pushEnergy(level, pos, MAX_OUTPUT);
        this.setLit(level, pos, state, generated > 0);
    }

    private void recalculate(ServerLevel level, BlockPos pos) {
        MoonPhase phase = level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos);
        int phaseIndex = phase.index() & 7;
        boolean lens = this.items.get(SLOT_LENS).is(com.mistech.mistechmod.celestial.CelestialContent.STAR_FRAGMENT.get());
        boolean openSky = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 1;
        boolean night = level.isDarkOutside();
        boolean high = pos.getY() >= HIGH_ALTITUDE;

        int sky;
        float value;
        if (!openSky) {
            sky = 2;
            value = 0;
        } else {
            sky = night ? 0 : 1;
            value = night ? RATE_BY_PHASE[phaseIndex] : DAY_RATE;
            if (level.isThundering()) {
                sky = 3;
                value *= 0.25F;
            } else if (level.isRaining()) {
                sky = 3;
                value *= 0.5F;
            }
            if (this.tableBonus) value *= 1.25F;
            if (high) value *= 1.25F;
        }
        if (!lens) {
            value = 0;
        }
        this.rate = Math.round(value);
        this.info1 = phaseIndex | (sky << 3) | (this.tableBonus ? 32 : 0) | (high ? 64 : 0) | (lens ? 0 : 128);
    }

    private static boolean findTable(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -TABLE_RANGE; dx <= TABLE_RANGE; dx++) {
                for (int dz = -TABLE_RANGE; dz <= TABLE_RANGE; dz++) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.isLoaded(cursor) && level.getBlockState(cursor).is(Mistech.ASTROLOGY_TABLE.get())
                            && AstrologyField.isValid(level, cursor.immutable())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
