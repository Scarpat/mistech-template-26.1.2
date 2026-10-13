package com.mistech.mistechmod.fluid;

import java.util.ArrayList;
import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Metais derretidos (Mitril, Adamantina, Oricalco): fluido quente com balde e bloco líquido. Usados pela fundição do
 * Productive Metalworks (receitas em data/mistech/recipe/compat/productivemetalworks, só carregam com ele instalado)
 * e por qualquer outro mod que trabalhe com as tags c:molten_*.
 */
public final class MoltenMetals {
    public static final Molten MITHRIL = new Molten("mithril", MapColor.COLOR_LIGHT_BLUE, Rarity.UNCOMMON);
    public static final Molten ADAMANTINE = new Molten("adamantine", MapColor.COLOR_RED, Rarity.RARE);
    public static final Molten ORICHALCUM = new Molten("orichalcum", MapColor.GOLD, Rarity.RARE);
    public static final List<Molten> ALL = List.of(MITHRIL, ADAMANTINE, ORICHALCUM);

    private MoltenMetals() {}

    /** Só para carregar a classe (os registros já estão nos DeferredRegister do Mistech). */
    public static void init() {}

    public static List<DeferredItem<? extends Item>> creativeItems() {
        List<DeferredItem<? extends Item>> list = new ArrayList<>();
        ALL.forEach(m -> list.add(m.bucket));
        return list;
    }

    public static final class Molten {
        public final String metal;
        public final DeferredHolder<FluidType, FluidType> type;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        public final DeferredBlock<HotMetalBlock> block;
        public final DeferredItem<BucketItem> bucket;

        Molten(String metal, MapColor color, Rarity rarity) {
            this.metal = metal;
            String name = "molten_" + metal;
            this.type = MistechFluids.FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.mistech." + name)
                    // sem isWaterLike o NeoForge 26.1 trava a entidade dentro do fluido (ver MistechFluids)
                    .isWaterLike(true)
                    .canSwim(false)
                    .canDrown(false)
                    .canExtinguish(false)
                    .supportsBoating(false)
                    .canConvertToSource(false)
                    .canHydrate(false)
                    .lightLevel(15)
                    .density(3000)
                    .viscosity(6000)
                    .temperature(1300)
                    .motionScale(0.002335)
                    .rarity(rarity)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.LAVA_EXTINGUISH)));
            this.source = MistechFluids.FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties()));
            this.flowing = MistechFluids.FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties()));
            this.block = Mistech.BLOCKS.registerBlock(name, p -> new HotMetalBlock(this.source.get(), p),
                    p -> p.mapColor(color).replaceable().noCollision().strength(100.0F).speedFactor(0.7F)
                            .pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY).lightLevel(state -> 15));
            this.bucket = Mistech.ITEMS.registerItem(name + "_bucket",
                    p -> new BucketItem(this.source.get(), p.craftRemainder(Items.BUCKET).stacksTo(1).rarity(rarity)));
        }

        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(this.type, this.source, this.flowing)
                    .bucket(this.bucket)
                    .block(this.block)
                    .slopeFindDistance(2)
                    .levelDecreasePerBlock(2)
                    .tickRate(30);
        }

        public Identifier id() {
            return Identifier.fromNamespaceAndPath(Mistech.MODID, "molten_" + this.metal);
        }
    }

    /** Metal derretido no mundo: põe fogo em quem entra, como a lava. */
    public static class HotMetalBlock extends LiquidBlock {
        public HotMetalBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
            super(fluid, properties);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
            super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
            if (!entity.fireImmune()) {
                entity.igniteForSeconds(8.0F);
            }
        }
    }
}
