package com.mistech.mistechmod.tarot.magus;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.tarot.ArcanaBosses;
import com.mistech.mistechmod.tarot.TarotArcana;

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import com.mistech.mistechmod.accessory.Accessory;
import com.mistech.mistechmod.accessory.AccessoryItem;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.registry.AccessoryContent;
import com.mistech.mistechmod.registry.MistechComponents;
import com.mistech.mistechmod.registry.OreContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * O Mago (arcano I): chefe {@link MagusEntity}, os Aspectos, as Pedras Rúnicas, as magias que voam, o Salão Arcano
 * ({@link MagusArena}) e os equipamentos da Forja (Cajado do Mago e Pingente do Arcano).
 */
public final class MagusContent {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Mistech.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MagusEntity>> MAGUS = ENTITIES.registerEntityType("the_magician",
            MagusEntity::new, MobCategory.MONSTER, b -> b.sized(2.4F, 4.0F).eyeHeight(3.1F).clientTrackingRange(12).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<MagusAspectEntity>> ASPECT = ENTITIES.registerEntityType("magician_aspect",
            MagusAspectEntity::new, MobCategory.MONSTER, b -> b.sized(1.4F, 2.4F).clientTrackingRange(10).noSave());
    public static final DeferredHolder<EntityType<?>, EntityType<MagusRunestoneEntity>> RUNESTONE = ENTITIES.registerEntityType("magician_runestone",
            MagusRunestoneEntity::new, MobCategory.MISC, b -> b.sized(0.8F, 0.8F).clientTrackingRange(8).updateInterval(1).noSave());
    public static final DeferredHolder<EntityType<?>, EntityType<MagusSpellEntity>> SPELL = ENTITIES.registerEntityType("magician_spell",
            MagusSpellEntity::new, MobCategory.MISC, b -> b.sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(1).noSave());

    // ----- Cajado do Mago -----
    private static ItemAttributeModifiers staffAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 9.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.6, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static final DeferredItem<MagusStaffItem> MAGUS_STAFF = Mistech.ITEMS.registerItem("magician_staff", MagusStaffItem::new,
            p -> p.stacksTo(1).attributes(staffAttributes()).rarity(Rarity.EPIC).fireResistant());

    // ----- Pingente do Arcano (amuleto) -----
    public static final DeferredItem<AccessoryItem> ARCANE_PENDANT = Mistech.ITEMS.registerItem("arcane_pendant",
            p -> new AccessoryItem(p, Accessory.ARCANE_PENDANT), p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    public static final DeferredItem<SpawnEggItem> MAGUS_SPAWN_EGG = Mistech.ITEMS.registerItem("the_magician_spawn_egg",
            SpawnEggItem::new, p -> p.spawnEgg(MAGUS.get()));

    private MagusContent() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        List<DeferredItem<? extends Item>> list = new ArrayList<>();
        list.add(MAGUS_STAFF);
        list.add(ARCANE_PENDANT);
        list.add(MAGUS_SPAWN_EGG);
        return list;
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        modEventBus.addListener(MagusContent::onAttributes);
        modEventBus.addListener(MagusContent::onCapabilities);

        ArcanaBosses.registerArena(TarotArcana.THE_MAGICIAN, MagusArena.INSTANCE);
        ArcanaBosses.register(TarotArcana.THE_MAGICIAN, (level, fight, spawn, challenger) -> {
            MagusEntity magus = MAGUS.get().create(level, EntitySpawnReason.EVENT);
            if (magus != null) {
                magus.snapTo(spawn.x, spawn.y, spawn.z, 0.0F, 0.0F);
                magus.setArena(fight.center());
            }
            return magus;
        });
        // Forja: Carta do Mago + materiais nos pedestais (provisórios)
        TarotArcana.THE_MAGICIAN.setOptions(
                new TarotArcana.Option("staff", () -> new ItemStack(MAGUS_STAFF.get()),
                        List.of(() -> Mistech.ARCANE_RUNE.get(), () -> Mistech.EARTH_RUNE.get(), () -> Mistech.AIR_RUNE.get(),
                                () -> Mistech.WATER_RUNE.get(), () -> Mistech.FIRE_RUNE.get(), () -> OreContent.ORICHALCUM_BLOCK_ITEM.get())),
                new TarotArcana.Option("pendant", () -> new ItemStack(ARCANE_PENDANT.get()),
                        List.of(() -> AccessoryContent.AMULET_BLANK.get(), () -> Mistech.ARCANE_RUNE.get(),
                                () -> PurityContent.PURE_MAGIC_SHARD.get(), () -> Items.LAPIS_BLOCK, () -> Items.GOLD_INGOT)));
    }

    /** O Cajado do Mago carrega em qualquer carregador de energia (não deixa tirar). */
    private static void onCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.Energy.ITEM,
                (stack, access) -> new net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler(access,
                        MistechComponents.STAFF_ENERGY.get(), MagusStaffItem.CAPACITY, MagusStaffItem.MAX_CHARGE_PER_TICK, 0),
                MAGUS_STAFF.get());
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(MAGUS.get(), MagusEntity.createAttributes().build());
        event.put(ASPECT.get(), MagusAspectEntity.createAttributes().build());
    }
}
