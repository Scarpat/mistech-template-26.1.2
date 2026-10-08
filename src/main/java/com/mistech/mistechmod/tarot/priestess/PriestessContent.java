package com.mistech.mistechmod.tarot.priestess;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.accessory.Accessory;
import com.mistech.mistechmod.accessory.AccessoryItem;
import com.mistech.mistechmod.registry.AccessoryContent;
import com.mistech.mistechmod.tarot.ArcanaBosses;
import com.mistech.mistechmod.tarot.TarotArcana;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * A Sacerdotisa (arcano II): chefe {@link PriestessEntity} que alterna entre <b>Boaz</b> (escuridão, só desviar) e
 * <b>Jachin</b> (luz, achar a verdadeira entre os reflexos), o Templo do Véu ({@link PriestessArena}) e os itens da
 * Forja: o Portal (dimensão de bolso, {@link PriestessPortalItem}) e o Véu (pingente, {@link VeilEvents}).
 */
public final class PriestessContent {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Mistech.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Mistech.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<PriestessEntity>> PRIESTESS = ENTITIES.registerEntityType(
            "the_high_priestess", PriestessEntity::new, MobCategory.MONSTER,
            b -> b.sized(1.0F, 3.0F).eyeHeight(2.6F).clientTrackingRange(12).fireImmune());
    /** Reflexo: igual à Sacerdotisa por fora (só a verdadeira tem sombra). Some com um golpe. */
    public static final DeferredHolder<EntityType<?>, EntityType<PriestessReflectionEntity>> REFLECTION = ENTITIES.registerEntityType(
            "priestess_reflection", PriestessReflectionEntity::new, MobCategory.MONSTER,
            b -> b.sized(1.0F, 3.0F).eyeHeight(2.6F).clientTrackingRange(12).fireImmune().noSave());

    /** Dimensão de bolso do Portal (data/mistech/dimension/pocket_sanctum.json). */
    public static final ResourceKey<Level> POCKET_SANCTUM =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Mistech.MODID, "pocket_sanctum"));

    /** Véu ativo? Espelho sincronizado com todos que veem o jogador (o cliente usa para não desenhar nada dele). */
    public static final Supplier<AttachmentType<Boolean>> VEILED = ATTACHMENTS.register("priestess_veiled",
            () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .sync((holder, to) -> true, ByteBufCodecs.BOOL)
                    .build());

    // ----- itens -----
    public static final DeferredItem<PriestessPortalItem> PORTAL = Mistech.ITEMS.registerItem("priestess_portal", PriestessPortalItem::new,
            p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<AccessoryItem> VEIL = Mistech.ITEMS.registerItem("priestess_veil",
            p -> new AccessoryItem(p, Accessory.PRIESTESS_VEIL), p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<SpawnEggItem> SPAWN_EGG = Mistech.ITEMS.registerItem("the_high_priestess_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(PRIESTESS.get()));

    private PriestessContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(PORTAL, VEIL, SPAWN_EGG);
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(PriestessContent::onAttributes);

        ArcanaBosses.registerArena(TarotArcana.THE_HIGH_PRIESTESS, PriestessArena.INSTANCE);
        ArcanaBosses.register(TarotArcana.THE_HIGH_PRIESTESS, (level, fight, spawn, challenger) -> {
            PriestessEntity priestess = PRIESTESS.get().create(level, EntitySpawnReason.EVENT);
            if (priestess != null) {
                priestess.snapTo(spawn.x, spawn.y, spawn.z, 0.0F, 0.0F);
                priestess.setArena(fight.center());
            }
            return priestess;
        });
        // Forja: Carta da Sacerdotisa + materiais nos pedestais (provisórios)
        TarotArcana.THE_HIGH_PRIESTESS.setOptions(
                new TarotArcana.Option("portal", () -> new ItemStack(PORTAL.get()),
                        List.of(() -> Mistech.SPACE_RUNE.get(), () -> Mistech.DARKNESS_RUNE.get(), () -> Items.ENDER_EYE,
                                () -> Items.CRYING_OBSIDIAN, () -> Items.LODESTONE)),
                new TarotArcana.Option("veil", () -> new ItemStack(VEIL.get()),
                        List.of(() -> AccessoryContent.AMULET_BLANK.get(), () -> Mistech.DARKNESS_RUNE.get(), () -> Mistech.WATER_RUNE.get(),
                                () -> Items.PHANTOM_MEMBRANE, () -> Items.FERMENTED_SPIDER_EYE)));
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(PRIESTESS.get(), PriestessEntity.createAttributes().build());
        event.put(REFLECTION.get(), PriestessReflectionEntity.createAttributes().build());
    }
}
