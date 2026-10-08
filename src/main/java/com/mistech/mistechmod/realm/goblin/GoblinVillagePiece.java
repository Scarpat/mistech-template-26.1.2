package com.mistech.mistechmod.realm.goblin;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.GoblinContent;

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

/** A Vila dos Duendes inteira numa peça: guarda só a semente e o centro; a planta é refeita quando precisa. */
public class GoblinVillagePiece extends StructurePiece {
    private final long seed;
    private final BlockPos center;
    private @Nullable GoblinVillagePlan plan;

    public GoblinVillagePiece(long seed, BlockPos center) {
        this(seed, center, new GoblinVillagePlan(seed, center.getX(), center.getY(), center.getZ()));
    }

    private GoblinVillagePiece(long seed, BlockPos center, GoblinVillagePlan plan) {
        super(GoblinContent.VILLAGE_PIECE.get(), 0, plan.bounds());
        this.seed = seed;
        this.center = center;
        this.plan = plan;
    }

    public GoblinVillagePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(GoblinContent.VILLAGE_PIECE.get(), tag);
        this.seed = tag.getLongOr("Seed", 0L);
        this.center = new BlockPos(tag.getIntOr("CX", 0), tag.getIntOr("CY", 64), tag.getIntOr("CZ", 0));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("Seed", this.seed);
        tag.putInt("CX", this.center.getX());
        tag.putInt("CY", this.center.getY());
        tag.putInt("CZ", this.center.getZ());
    }

    private GoblinVillagePlan plan() {
        if (this.plan == null) {
            this.plan = new GoblinVillagePlan(this.seed, this.center.getX(), this.center.getY(), this.center.getZ());
        }
        return this.plan;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        this.plan().blueprint().apply(level, chunkBox, random);
    }
}
