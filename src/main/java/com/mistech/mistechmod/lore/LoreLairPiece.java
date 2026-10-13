package com.mistech.mistechmod.lore;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.realm.goblin.Blueprint;

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

/** Toca da Cuca ou Caverna do Mapinguari numa peça só: guarda tipo, semente e centro; a planta é refeita quando precisa. */
public class LoreLairPiece extends StructurePiece {
    private final String lair;
    private final long seed;
    private final BlockPos center;
    private @Nullable Blueprint plan;

    public LoreLairPiece(String lair, long seed, BlockPos center) {
        this(lair, seed, center, build(lair, seed, center));
    }

    private LoreLairPiece(String lair, long seed, BlockPos center, Blueprint plan) {
        super(LoreContent.LAIR_PIECE.get(), 0, plan.bounds());
        this.lair = lair;
        this.seed = seed;
        this.center = center;
        this.plan = plan;
    }

    public LoreLairPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(LoreContent.LAIR_PIECE.get(), tag);
        this.lair = tag.getStringOr("Lair", "cuca");
        this.seed = tag.getLongOr("Seed", 0L);
        this.center = new BlockPos(tag.getIntOr("CX", 0), tag.getIntOr("CY", 64), tag.getIntOr("CZ", 0));
    }

    private static Blueprint build(String lair, long seed, BlockPos c) {
        return "mapinguari".equals(lair) ? new MapinguariCavePlan(seed, c.getX(), c.getY(), c.getZ()).blueprint()
                : new CucaLairPlan(seed, c.getX(), c.getY(), c.getZ()).blueprint();
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Lair", this.lair);
        tag.putLong("Seed", this.seed);
        tag.putInt("CX", this.center.getX());
        tag.putInt("CY", this.center.getY());
        tag.putInt("CZ", this.center.getZ());
    }

    private Blueprint plan() {
        if (this.plan == null) {
            this.plan = build(this.lair, this.seed, this.center);
        }
        return this.plan;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        this.plan().apply(level, chunkBox, random);
    }
}
