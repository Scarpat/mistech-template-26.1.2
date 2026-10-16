package com.mistech.mistechmod.realm.goblin;

import java.util.Optional;

import com.mistech.mistechmod.registry.GoblinContent;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Vila dos Duendes: aparece no Ermo Turvo da Dimensão dos Gnomos (bioma definido no JSON da estrutura),
 * longe do prado central. Uma peça só ({@link GoblinVillagePiece}) com a planta inteira.
 */
public class GoblinVillageStructure extends Structure {
    public static final MapCodec<GoblinVillageStructure> CODEC = simpleCodec(GoblinVillageStructure::new);
    /** A vila (raio ~45) inteira dentro do Pesadelo: longe da borda do Sonho Comum e da queda para o Sonho Profundo. */
    private static final int MARGIN = 70;

    public GoblinVillageStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        double d = com.mistech.mistechmod.realm.DreamLayout.distance(x, z);
        if (d < com.mistech.mistechmod.realm.DreamLayout.COMMON + MARGIN || d > com.mistech.mistechmod.realm.DreamLayout.NIGHTMARE - MARGIN) {
            return Optional.empty();
        }
        int y = Structure.getMeanFirstOccupiedHeight(context, x - 12, 25, z - 12, 25);
        y = Math.max(60, Math.min(140, y));
        BlockPos center = new BlockPos(x, y, z);
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new GoblinVillagePiece(seed, center))));
    }

    @Override
    public StructureType<?> type() {
        return GoblinContent.VILLAGE_TYPE.get();
    }
}
