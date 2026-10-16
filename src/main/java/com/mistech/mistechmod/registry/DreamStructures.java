package com.mistech.mistechmod.registry;

import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.realm.dream.structure.DreamAnchorBlock;
import com.mistech.mistechmod.realm.dream.structure.DreamAnchorBlockEntity;
import com.mistech.mistechmod.realm.dream.structure.DreamStructure;
import com.mistech.mistechmod.realm.dream.structure.DreamStructurePiece;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Estruturas do Sonho (anéis 1 e 2): Vilarejo Gnomo, Vilarejo Arruinado, Casa Errada e Escada sem Fim. */
public final class DreamStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Mistech.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Mistech.MODID);

    /** Âncora Onírica: escondida nas paredes, sem item (indestrutível, como bedrock). */
    public static final DeferredBlock<DreamAnchorBlock> ANCHOR = Mistech.BLOCKS.registerBlock("dream_anchor", DreamAnchorBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.STONE)
                    .pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, pos, t) -> false));
    public static final Supplier<BlockEntityType<DreamAnchorBlockEntity>> ANCHOR_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES.register("dream_anchor",
            () -> new BlockEntityType<>(DreamAnchorBlockEntity::new, ANCHOR.get()));

    public static final DeferredHolder<StructureType<?>, StructureType<DreamStructure>> STRUCTURE_TYPE =
            STRUCTURE_TYPES.register("dream_structure", () -> () -> DreamStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PIECE =
            PIECE_TYPES.register("dream_structure", () -> (StructurePieceType) DreamStructurePiece::new);

    private DreamStructures() {}

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        PIECE_TYPES.register(modEventBus);
    }
}
