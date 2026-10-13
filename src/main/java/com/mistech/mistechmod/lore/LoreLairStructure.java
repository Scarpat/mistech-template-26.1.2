package com.mistech.mistechmod.lore;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Arenas dos chefes do folclore na Mata Ancestral. O campo {@code lair} do JSON escolhe a planta:
 * {@code "cuca"} = Toca da Cuca ({@link CucaLairPlan}), {@code "mapinguari"} = Caverna do Mapinguari ({@link MapinguariCavePlan}).
 * Só nasce em terreno seco e sem barranco grande.
 */
public class LoreLairStructure extends Structure {
    public static final MapCodec<LoreLairStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("lair").forGetter(s -> s.lair)).apply(i, LoreLairStructure::new));

    private final String lair;

    public LoreLairStructure(StructureSettings settings, String lair) {
        super(settings);
        this.lair = lair;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        ChunkGenerator gen = context.chunkGenerator();
        LevelHeightAccessor h = context.heightAccessor();
        RandomState rs = context.randomState();
        int surface = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, h, rs);
        int floor = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, h, rs);
        if (surface != floor || surface <= gen.getSeaLevel()) {
            return Optional.empty(); // água ou abaixo do nível do mar
        }
        boolean cave = "mapinguari".equals(this.lair);
        int spread = cave ? 8 : 12;
        int min = surface, max = surface;
        for (int dx = -spread; dx <= spread; dx += spread) {
            for (int dz = -spread; dz <= spread; dz += spread) {
                int y = gen.getFirstOccupiedHeight(x + dx, z + dz, Heightmap.Types.OCEAN_FLOOR_WG, h, rs);
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        if (max - min > (cave ? 14 : 10)) {
            return Optional.empty();
        }
        int y = cave ? surface : Structure.getMeanFirstOccupiedHeight(context, x - 8, 17, z - 8, 17);
        BlockPos center = new BlockPos(x, y, z);
        long seed = context.random().nextLong();
        String lair = this.lair;
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new LoreLairPiece(lair, seed, center))));
    }

    @Override
    public StructureType<?> type() {
        return LoreContent.LAIR_TYPE.get();
    }
}
