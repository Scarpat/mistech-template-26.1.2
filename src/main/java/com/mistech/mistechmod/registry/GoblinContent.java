package com.mistech.mistechmod.registry;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.goblin.BoneLockBlock;
import com.mistech.mistechmod.block.goblin.GoblinSealBlock;
import com.mistech.mistechmod.block.goblin.GoblinTotemBlock;
import com.mistech.mistechmod.block.goblin.RunePanelBlock;
import com.mistech.mistechmod.entity.goblin.AberrationEntity;
import com.mistech.mistechmod.entity.goblin.CorruptedGoblinEntity;
import com.mistech.mistechmod.entity.goblin.GoblinQueenEntity;
import com.mistech.mistechmod.entity.goblin.TrollEntity;
import com.mistech.mistechmod.realm.goblin.GoblinVillagePiece;
import com.mistech.mistechmod.realm.goblin.GoblinVillageStructure;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Vila dos Duendes (Ermo Turvo da Dimensão dos Gnomos): duendes corrompidos na superfície, um fosso até as
 * catacumbas-labirinto com trolls, aberrações e puzzles, e a arena da Rainha Duende no centro.
 */
public final class GoblinContent {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Mistech.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Mistech.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<GoblinVillageStructure>> VILLAGE_TYPE =
            STRUCTURE_TYPES.register("goblin_village", () -> () -> GoblinVillageStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> VILLAGE_PIECE =
            PIECE_TYPES.register("goblin_village", () -> (StructurePieceType) GoblinVillagePiece::new);

    // ----- mobs -----
    public static final DeferredHolder<EntityType<?>, EntityType<CorruptedGoblinEntity>> CORRUPTED_GOBLIN = GnomeContent.ENTITIES.registerEntityType(
            "corrupted_goblin", CorruptedGoblinEntity::new, MobCategory.MONSTER, b -> b.sized(0.6F, 1.3F).eyeHeight(1.0F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<TrollEntity>> TROLL = GnomeContent.ENTITIES.registerEntityType(
            "troll", TrollEntity::new, MobCategory.MONSTER, b -> b.sized(1.4F, 2.7F).eyeHeight(2.2F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<AberrationEntity>> ABERRATION = GnomeContent.ENTITIES.registerEntityType(
            "aberration", AberrationEntity::new, MobCategory.MONSTER, b -> b.sized(1.0F, 0.8F).eyeHeight(0.5F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<GoblinQueenEntity>> GOBLIN_QUEEN = GnomeContent.ENTITIES.registerEntityType(
            "goblin_queen", GoblinQueenEntity::new, MobCategory.MONSTER, b -> b.sized(1.4F, 2.3F).eyeHeight(1.9F).clientTrackingRange(10));

    // ----- blocos de puzzle (indestrutíveis) -----
    public static final DeferredBlock<GoblinSealBlock> GOBLIN_SEAL = Mistech.BLOCKS.registerBlock("goblin_seal", GoblinSealBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.DEEPSLATE_BRICKS)
                    .lightLevel(s -> 5).pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, pos, t) -> false));
    public static final DeferredBlock<BoneLockBlock> BONE_LOCK = Mistech.BLOCKS.registerBlock("bone_lock", BoneLockBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.BONE_BLOCK)
                    .lightLevel(s -> 2 + s.getValue(BoneLockBlock.KEYS) * 3).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<GoblinTotemBlock> GOBLIN_TOTEM = Mistech.BLOCKS.registerBlock("goblin_totem", GoblinTotemBlock::new,
            p -> p.mapColor(MapColor.COLOR_BROWN).strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.WOOD)
                    .lightLevel(s -> s.getValue(GoblinTotemBlock.SOLVED) ? 9 : 0).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<RunePanelBlock> RUNE_PANEL = Mistech.BLOCKS.registerBlock("rune_panel", RunePanelBlock::new,
            p -> p.mapColor(MapColor.DEEPSLATE).strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.DEEPSLATE_TILES)
                    .lightLevel(s -> s.getValue(RunePanelBlock.LIT) ? 7 : 1).pushReaction(PushReaction.BLOCK));

    public static final DeferredItem<BlockItem> GOBLIN_SEAL_ITEM = Mistech.ITEMS.registerSimpleBlockItem("goblin_seal", GOBLIN_SEAL);
    public static final DeferredItem<BlockItem> BONE_LOCK_ITEM = Mistech.ITEMS.registerSimpleBlockItem("bone_lock", BONE_LOCK);
    public static final DeferredItem<BlockItem> GOBLIN_TOTEM_ITEM = Mistech.ITEMS.registerSimpleBlockItem("goblin_totem", GOBLIN_TOTEM);
    public static final DeferredItem<BlockItem> RUNE_PANEL_ITEM = Mistech.ITEMS.registerSimpleBlockItem("rune_panel", RUNE_PANEL);

    // ----- itens -----
    public static final DeferredItem<Item> BONE_KEY = Mistech.ITEMS.registerSimpleItem("bone_key", p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    /** Coroa Corrompida (drop da Rainha): acessório — duendes neutros + Chamado da Rainha; macula aos poucos. */
    public static final DeferredItem<com.mistech.mistechmod.accessory.AccessoryItem> CORRUPTED_CROWN = Mistech.ITEMS.registerItem("corrupted_crown",
            p -> new com.mistech.mistechmod.accessory.AccessoryItem(p, com.mistech.mistechmod.accessory.Accessory.CORRUPTED_CROWN),
            p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    /** Restos Feéricos: drop dos duendes. Comestível como o olho de aranha (dá veneno); no Triturador vira Pó de Fada. */
    public static final DeferredItem<Item> FEY_REMAINS = Mistech.ITEMS.registerSimpleItem("fey_remains",
            p -> p.food(net.minecraft.world.food.Foods.SPIDER_EYE, net.minecraft.world.item.component.Consumables.SPIDER_EYE));
    public static final DeferredItem<Item> FAIRY_DUST = Mistech.ITEMS.registerSimpleItem("fairy_dust", p -> p.rarity(Rarity.UNCOMMON));

    /** Clava de Troll: drop raro do Troll. Dano 10, bem lenta, joga longe e às vezes atordoa. */
    public static final net.minecraft.world.item.ToolMaterial TROLL_WOOD = new net.minecraft.world.item.ToolMaterial(
            net.minecraft.tags.BlockTags.INCORRECT_FOR_STONE_TOOL, 750, 2.0F, 2.0F, 10, net.minecraft.tags.ItemTags.LOGS);
    public static final DeferredItem<com.mistech.mistechmod.item.TrollClubItem> TROLL_CLUB = Mistech.ITEMS.registerItem("troll_club",
            com.mistech.mistechmod.item.TrollClubItem::new, p -> p.sword(TROLL_WOOD, 7.0F, -3.3F).rarity(Rarity.RARE));

    public static final DeferredItem<SpawnEggItem> CORRUPTED_GOBLIN_SPAWN_EGG = Mistech.ITEMS.registerItem("corrupted_goblin_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(CORRUPTED_GOBLIN.get()));
    public static final DeferredItem<SpawnEggItem> TROLL_SPAWN_EGG = Mistech.ITEMS.registerItem("troll_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(TROLL.get()));
    public static final DeferredItem<SpawnEggItem> ABERRATION_SPAWN_EGG = Mistech.ITEMS.registerItem("aberration_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(ABERRATION.get()));
    public static final DeferredItem<SpawnEggItem> GOBLIN_QUEEN_SPAWN_EGG = Mistech.ITEMS.registerItem("goblin_queen_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(GOBLIN_QUEEN.get()));

    private GoblinContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(FEY_REMAINS, FAIRY_DUST, TROLL_CLUB, BONE_KEY, CORRUPTED_CROWN, GOBLIN_SEAL_ITEM, BONE_LOCK_ITEM, GOBLIN_TOTEM_ITEM, RUNE_PANEL_ITEM,
                CORRUPTED_GOBLIN_SPAWN_EGG, TROLL_SPAWN_EGG, ABERRATION_SPAWN_EGG, GOBLIN_QUEEN_SPAWN_EGG);
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        PIECE_TYPES.register(modEventBus);
        modEventBus.addListener(GoblinContent::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(CORRUPTED_GOBLIN.get(), CorruptedGoblinEntity.createAttributes().build());
        event.put(TROLL.get(), TrollEntity.createAttributes().build());
        event.put(ABERRATION.get(), AberrationEntity.createAttributes().build());
        event.put(GOBLIN_QUEEN.get(), GoblinQueenEntity.createAttributes().build());
    }
}
