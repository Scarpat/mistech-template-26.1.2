package com.mistech.mistechmod.spellcraft;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;
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

/** Torre do Arquimago ({@code "tower"}) ou Santuário de Elemento ({@code "shrine:<elemento>"}) numa peça só. */
public class ArcaneRuinPiece extends StructurePiece {
    private final String kind;
    private final long seed;
    private final BlockPos center;
    private @Nullable Blueprint plan;

    public ArcaneRuinPiece(String kind, long seed, BlockPos center) {
        this(kind, seed, center, build(kind, seed, center));
    }

    private ArcaneRuinPiece(String kind, long seed, BlockPos center, Blueprint plan) {
        super(SpellcraftContent.RUIN_PIECE.get(), 0, plan.bounds());
        this.kind = kind;
        this.seed = seed;
        this.center = center;
        this.plan = plan;
    }

    public ArcaneRuinPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(SpellcraftContent.RUIN_PIECE.get(), tag);
        this.kind = tag.getStringOr("Kind", "tower");
        this.seed = tag.getLongOr("Seed", 0L);
        this.center = new BlockPos(tag.getIntOr("CX", 0), tag.getIntOr("CY", 64), tag.getIntOr("CZ", 0));
    }

    private static Blueprint build(String kind, long seed, BlockPos c) {
        if (kind.startsWith("shrine:")) {
            RuneElement element = RuneElement.byId(kind.substring(7));
            return new ElementShrinePlan(element == null ? RuneElement.FIRE : element, seed, c.getX(), c.getY(), c.getZ()).blueprint();
        }
        if ("circle".equals(kind)) {
            return new RuneCirclePlan(seed, c.getX(), c.getY(), c.getZ()).blueprint();
        }
        if ("library".equals(kind)) {
            return new ForbiddenLibraryPlan(seed, c.getX(), c.getY(), c.getZ()).blueprint();
        }
        if ("mothertree".equals(kind)) {
            return new MotherTreePlan(seed, c.getX(), c.getY(), c.getZ()).blueprint();
        }
        return new ArchmageTowerPlan(seed, c.getX(), c.getY(), c.getZ()).blueprint();
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Kind", this.kind);
        tag.putLong("Seed", this.seed);
        tag.putInt("CX", this.center.getX());
        tag.putInt("CY", this.center.getY());
        tag.putInt("CZ", this.center.getZ());
    }

    private Blueprint plan() {
        if (this.plan == null) {
            this.plan = build(this.kind, this.seed, this.center);
        }
        return this.plan;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        this.plan().apply(level, chunkBox, random);
    }
}
