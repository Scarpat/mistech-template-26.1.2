package com.mistech.mistechmod.folklore;

import java.util.List;
import java.util.function.Supplier;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.GnomeContent;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Folclore brasileiro: Saci, Corvo, Curupira, Lobisomem (e a Licantropia), Boitatá e Mula sem Cabeça. */
public final class FolkloreContent {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Mistech.MODID);

    /** Armas de prata (mitril): ferem mais os lobisomens. */
    public static final TagKey<Item> SILVER_WEAPONS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mistech.MODID, "silver_weapons"));

    // ----- criaturas -----
    public static final DeferredHolder<EntityType<?>, EntityType<SaciEntity>> SACI = GnomeContent.ENTITIES.registerEntityType("saci",
            SaciEntity::new, MobCategory.CREATURE, b -> b.sized(0.55F, 1.35F).eyeHeight(1.0F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<RavenEntity>> RAVEN = GnomeContent.ENTITIES.registerEntityType("raven",
            RavenEntity::new, MobCategory.CREATURE, b -> b.sized(0.5F, 0.9F).eyeHeight(0.6F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<CurupiraEntity>> CURUPIRA = GnomeContent.ENTITIES.registerEntityType("curupira",
            CurupiraEntity::new, MobCategory.CREATURE, b -> b.sized(0.6F, 1.35F).eyeHeight(1.0F).fireImmune().clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<WerewolfEntity>> WEREWOLF = GnomeContent.ENTITIES.registerEntityType("werewolf",
            WerewolfEntity::new, MobCategory.MONSTER, b -> b.sized(0.8F, 2.3F).eyeHeight(1.9F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<BoitataEntity>> BOITATA = GnomeContent.ENTITIES.registerEntityType("boitata",
            BoitataEntity::new, MobCategory.MONSTER, b -> b.sized(2.4F, 1.6F).eyeHeight(1.0F).fireImmune().clientTrackingRange(16));
    public static final DeferredHolder<EntityType<?>, EntityType<HeadlessMuleEntity>> HEADLESS_MULE = GnomeContent.ENTITIES.registerEntityType("headless_mule",
            HeadlessMuleEntity::new, MobCategory.CREATURE, b -> b.sized(1.3964844F, 1.6F).eyeHeight(1.52F).fireImmune().clientTrackingRange(10));

    // ----- itens -----
    public static final DeferredItem<SaciJarItem> SACI_JAR = Mistech.ITEMS.registerItem("saci_jar", SaciJarItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> WEREWOLF_FANG = Mistech.ITEMS.registerSimpleItem("werewolf_fang", p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> BOITATA_SCALE = Mistech.ITEMS.registerSimpleItem("boitata_scale", p -> p.rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<Item> BOITATA_EYE = Mistech.ITEMS.registerSimpleItem("boitata_eye", p -> p.rarity(Rarity.EPIC).fireResistant());

    // ----- Licantropia (salva, mantida ao morrer) -----
    public static final Supplier<AttachmentType<Boolean>> LYCANTHROPY = ATTACHMENTS.register("lycanthropy",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL.fieldOf("cursed"))
                    .copyOnDeath()
                    .build());

    private FolkloreContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(SACI_JAR, WEREWOLF_FANG, BOITATA_SCALE, BOITATA_EYE);
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(FolkloreContent::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(SACI.get(), SaciEntity.createAttributes().build());
        event.put(RAVEN.get(), Parrot.createAttributes().build());
        event.put(CURUPIRA.get(), CurupiraEntity.createAttributes().build());
        event.put(WEREWOLF.get(), WerewolfEntity.createAttributes().build());
        event.put(BOITATA.get(), BoitataEntity.createAttributes().build());
        event.put(HEADLESS_MULE.get(), HeadlessMuleEntity.createAttributes().build());
    }
}
