package com.mistech.mistechmod.spellcraft;

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

/** Estruturas únicas das magias. {@code kind}: {@code "tower"} (Torre do Arquimago) ou {@code "shrine"} (Santuário de um elemento sorteado). */
public class ArcaneRuinStructure extends Structure {
    public static final MapCodec<ArcaneRuinStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("kind").forGetter(s -> s.kind)).apply(i, ArcaneRuinStructure::new));

    private final String kind;

    public ArcaneRuinStructure(StructureSettings settings, String kind) {
        super(settings);
        this.kind = kind;
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
        int min = surface, max = surface;
        for (int dx = -10; dx <= 10; dx += 10) {
            for (int dz = -10; dz <= 10; dz += 10) {
                int y = gen.getFirstOccupiedHeight(x + dx, z + dz, Heightmap.Types.OCEAN_FLOOR_WG, h, rs);
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        if (max - min > 12) {
            return Optional.empty();
        }
        BlockPos center = new BlockPos(x, surface, z);
        long seed = context.random().nextLong();
        String piece = this.kind;
        if ("shrine".equals(this.kind)) {
            piece = "shrine:" + ElementShrinePlan.ELEMENTS[context.random().nextInt(ElementShrinePlan.ELEMENTS.length)].id();
        }
        String finalPiece = piece;
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new ArcaneRuinPiece(finalPiece, seed, center))));
    }

    @Override
    public StructureType<?> type() {
        return SpellcraftContent.RUIN_TYPE.get();
    }
}
