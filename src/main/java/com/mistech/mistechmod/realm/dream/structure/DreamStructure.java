package com.mistech.mistechmod.realm.dream.structure;

import java.util.Optional;

import com.mistech.mistechmod.realm.DreamLayout;
import com.mistech.mistechmod.registry.DreamStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Estruturas do Sonho ({@code "kind"}): {@code hamlet} Vilarejo Gnomo (Sonho Comum), {@code ruined_hamlet} Vila Gnoma em Ruínas,
 * {@code wrong_house} Casa Errada e {@code endless_stair} Escadaria Sem Fim (Pesadelo). A planta sai de {@link DreamPlans}.
 */
public class DreamStructure extends Structure {
    public static final MapCodec<DreamStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("kind").forGetter(s -> s.kind)
    ).apply(i, DreamStructure::new));

    private final String kind;

    public DreamStructure(StructureSettings settings, String kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        double d = DreamLayout.distance(x, z);
        int ring = DreamLayout.ringAt(d);
        boolean ok = switch (this.kind) {
            case "hamlet" -> ring == DreamLayout.RING_COMMON && d > 150 && d < DreamLayout.COMMON - 30;
            case "fractal_temple" -> ring == DreamLayout.RING_DEEP && d > DreamLayout.NIGHTMARE + 150 && d < DreamLayout.DEEP - 150;
            case "memory_library" -> ring == DreamLayout.RING_REM && d > DreamLayout.DEEP + 150;
            default -> ring == DreamLayout.RING_NIGHTMARE && d > DreamLayout.COMMON + 40 && d < DreamLayout.NIGHTMARE - 60;
        };
        if (!ok) {
            return Optional.empty();
        }
        // nos anéis 3-4 (ilhas) vale a altura do próprio centro: a média misturaria ilha e chão de baixo
        int y = ring >= DreamLayout.RING_DEEP
                ? context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState())
                : Structure.getMeanFirstOccupiedHeight(context, x - 8, 16, z - 8, 16);
        if (y < 50 || y > 150) {
            return Optional.empty();
        }
        BlockPos center = new BlockPos(x, y, z);
        long seed = context.random().nextLong();
        String kind = this.kind;
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new DreamStructurePiece(kind, seed, center))));
    }

    @Override
    public StructureType<?> type() {
        return DreamStructures.STRUCTURE_TYPE.get();
    }
}
