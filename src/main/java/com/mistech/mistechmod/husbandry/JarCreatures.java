package com.mistech.mistechmod.husbandry;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** Quais bichos cabem no Pote com Criatura e o que cada um produz. */
public final class JarCreatures {
    public enum Kind {
        /** Ovo (respeita a variante: galinha fria = ovo azul, quente = ovo marrom). A cada 2,5–5 min. */
        CHICKEN(3000, 3000),
        /** Leite: enche aos poucos (1 balde a cada 2 min, guarda até 4). Tira com balde vazio. */
        COW(2400, 0),
        /** Lã da cor da ovelha, a cada 2,5 min. */
        SHEEP(3000, 0),
        /** Bolsa de tinta, a cada 2,5 min. */
        SQUID(3000, 0),
        /** Bolsa de tinta brilhante, a cada 2,5 min. */
        GLOW_SQUID(3000, 0);

        public final int baseTicks;
        public final int randomTicks;

        Kind(int baseTicks, int randomTicks) {
            this.baseTicks = baseTicks;
            this.randomTicks = randomTicks;
        }

        public int nextCycle(RandomSource random) {
            return this.baseTicks + (this.randomTicks > 0 ? random.nextInt(this.randomTicks) : 0);
        }
    }

    public static final int MAX_MILK = 4;

    /** Partes do NBT do bicho que não fazem sentido dentro do pote. */
    private static final List<String> STRIP = List.of("Pos", "Motion", "Rotation", "UUID", "FallDistance", "fall_distance", "Fire",
            "Air", "OnGround", "PortalCooldown", "leash", "Leash", "Brain", "InLove", "LoveCause", "Passengers", "EggLayTime");

    private JarCreatures() {}

    public static @Nullable Kind kindOf(EntityType<?> type) {
        if (type == EntityType.CHICKEN) {
            return Kind.CHICKEN;
        }
        if (type == EntityType.COW || type == EntityType.MOOSHROOM) {
            return Kind.COW;
        }
        if (type == EntityType.SHEEP) {
            return Kind.SHEEP;
        }
        if (type == EntityType.SQUID) {
            return Kind.SQUID;
        }
        if (type == EntityType.GLOW_SQUID) {
            return Kind.GLOW_SQUID;
        }
        return null;
    }

    /** Guarda o bicho (variante, cor, nome...) para o item/bloco. */
    public static TypedEntityData<EntityType<?>> capture(Entity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        CompoundTag tag = output.buildResult();
        STRIP.forEach(tag::remove);
        return TypedEntityData.of(entity.getType(), tag);
    }

    /** Recria o bicho guardado (não é adicionado ao mundo). */
    public static @Nullable Entity recreate(TypedEntityData<EntityType<?>> data, Level level, EntitySpawnReason reason) {
        Entity entity = data.type().create(level, reason);
        if (entity != null) {
            data.loadInto(entity);
        }
        return entity;
    }

    /** Um produto do bicho (não usado para a vaca, que enche leite). */
    public static ItemStack produce(Kind kind, @Nullable Entity creature, ServerLevel level) {
        switch (kind) {
            case CHICKEN -> {
                if (creature instanceof LivingEntity living) {
                    ItemStack[] laid = {ItemStack.EMPTY};
                    living.dropFromGiftLootTable(level, BuiltInLootTables.CHICKEN_LAY, (lvl, stack) -> {
                        if (laid[0].isEmpty()) {
                            laid[0] = stack;
                        }
                    });
                    if (!laid[0].isEmpty()) {
                        return laid[0];
                    }
                }
                return new ItemStack(Items.EGG);
            }
            case SHEEP -> {
                if (creature instanceof Sheep sheep) {
                    Item wool = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(sheep.getColor().getSerializedName() + "_wool"));
                    if (wool != null && wool != Items.AIR) {
                        return new ItemStack(wool);
                    }
                }
                return new ItemStack(Items.WHITE_WOOL);
            }
            case SQUID -> {
                return new ItemStack(Items.INK_SAC);
            }
            case GLOW_SQUID -> {
                return new ItemStack(Items.GLOW_INK_SAC);
            }
            default -> {
                return ItemStack.EMPTY;
            }
        }
    }
}
