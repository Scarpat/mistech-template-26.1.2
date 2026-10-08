package com.mistech.mistechmod.machine;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.astrology.AstrologyField;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Gerador Astral: gera energia com a luz das estrelas — só à noite e com céu aberto acima.
 * Sem a lua atrapalhando aparecem mais estrelas: lua nova 40 FE/t, crescente/minguante 30, quartos 25,
 * gibosa 20, cheia 15. Chuva = 25%. +25% se houver uma Mesa de Astrologia com campo completo a até 16 blocos.
 * Buffer 50.000 FE; envia até 200 FE/t para cada bloco encostado. Sem Impulso de Gnomo.
 */
public class AstralGeneratorBlockEntity extends MachineBlockEntity {
    public static final int MAX_OUTPUT_PER_TICK = 200;
    public static final int TABLE_RANGE = 16;
    public static final float TABLE_BONUS = 1.25F;

    /** FE/t por fase (índice de {@link MoonPhase}: 0 = cheia ... 4 = nova ... 7 = crescente gibosa). */
    private static final int[] RATE_BY_PHASE = {15, 20, 25, 30, 40, 30, 25, 20};

    private int rate;
    private int checkTimer;
    private int tableTimer;
    private boolean tableBonus;

    public AstralGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.ASTRAL_GENERATOR.get(), pos, state, MachineLayouts.ASTRAL_GENERATOR, 0, MAX_OUTPUT_PER_TICK);
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
        this.pushEnergy(level, pos, MAX_OUTPUT_PER_TICK);
        this.setLit(level, pos, state, generated > 0);
    }

    private void recalculate(ServerLevel level, BlockPos pos) {
        MoonPhase phase = level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos);
        int phaseIndex = phase.index() & 7;
        boolean openSky = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 1;
        boolean night = level.isDarkOutside();
        boolean rain = level.isRaining();

        int sky; // 0 aberto, 1 dia, 2 coberto, 3 chuva
        float value;
        if (!openSky) {
            sky = 2;
            value = 0;
        } else if (!night) {
            sky = 1;
            value = 0;
        } else {
            value = RATE_BY_PHASE[phaseIndex];
            sky = 0;
            if (rain) {
                sky = 3;
                value *= 0.25F;
            }
            if (this.tableBonus) {
                value *= TABLE_BONUS;
            }
        }
        this.rate = Math.round(value);
        this.info1 = phaseIndex | (sky << 3) | (this.tableBonus ? 32 : 0);
    }

    /** Há uma Mesa de Astrologia com o campo completo a até 16 blocos (altura ±4)? */
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
