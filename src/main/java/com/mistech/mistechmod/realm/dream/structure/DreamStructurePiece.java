package com.mistech.mistechmod.realm.dream.structure;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.realm.goblin.Blueprint;
import com.mistech.mistechmod.registry.DreamStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Uma estrutura do Sonho inteira numa peça: guarda tipo, semente e centro; a planta é refeita quando precisa. */
public class DreamStructurePiece extends StructurePiece {
    private final String kind;
    private final long seed;
    private final BlockPos center;
    private @Nullable Blueprint plan;

    public DreamStructurePiece(String kind, long seed, BlockPos center) {
        this(kind, seed, center, DreamPlans.build(kind, seed, center));
    }

    private DreamStructurePiece(String kind, long seed, BlockPos center, Blueprint plan) {
        super(DreamStructures.PIECE.get(), 0, plan.bounds());
        this.kind = kind;
        this.seed = seed;
        this.center = center;
        this.plan = plan;
    }

    public DreamStructurePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(DreamStructures.PIECE.get(), tag);
        this.kind = tag.getStringOr("Kind", "hamlet");
        this.seed = tag.getLongOr("Seed", 0L);
        this.center = new BlockPos(tag.getIntOr("CX", 0), tag.getIntOr("CY", 64), tag.getIntOr("CZ", 0));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Kind", this.kind);
        tag.putLong("Seed", this.seed);
        tag.putInt("CX", this.center.getX());
        tag.putInt("CY", this.center.getY());
        tag.putInt("CZ", this.center.getZ());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        if (this.plan == null) {
            this.plan = DreamPlans.build(this.kind, this.seed, this.center);
        }
        this.plan.apply(level, chunkBox, random);
    }
}
