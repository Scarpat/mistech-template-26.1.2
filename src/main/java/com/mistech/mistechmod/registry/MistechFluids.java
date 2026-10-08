package com.mistech.mistechmod.registry;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Essência Mágica Impura: líquido roxo-escuro que escorre como água e faz levitar quem encosta. */
public final class MistechFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Mistech.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Mistech.MODID);

    public static final DeferredHolder<FluidType, FluidType> IMPURE_ESSENCE_TYPE = FLUID_TYPES.register("impure_essence",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.mistech.impure_essence")
                    // IMPORTANTE: sem isto o NeoForge 26.1 não aplica física de água nem nenhuma outra
                    // (a entidade fica travada dentro do fluido e a velocidade acumula até sair).
                    .isWaterLike(true)
                    .canSwim(true)
                    .canDrown(true)
                    .canExtinguish(true)
                    .supportsBoating(true)
                    .density(1100)
                    .viscosity(1400)
                    .lightLevel(2)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    /** Fonte parada de Essência Impura: de vez em quando macula a terra/grama encostada (Solo Maculado). */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> IMPURE_ESSENCE =
            FLUIDS.register("impure_essence", () -> new BaseFlowingFluid.Source(properties()) {
                @Override
                protected boolean isRandomlyTicking() {
                    return true;
                }

                @Override
                protected void randomTick(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
                                          net.minecraft.world.level.material.FluidState state, net.minecraft.util.RandomSource random) {
                    if (random.nextInt(8) == 0) {
                        com.mistech.mistechmod.purity.block.TaintedSoilBlock.contaminateNear(level, pos, 1, 1, 1);
                    }
                }
            });
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_IMPURE_ESSENCE =
            FLUIDS.register("flowing_impure_essence", () -> new BaseFlowingFluid.Flowing(properties()));

    // ----- Essência Purificada: branco-dourada, luminosa; tira Mácula de quem encosta -----
    public static final DeferredHolder<FluidType, FluidType> PURIFIED_ESSENCE_TYPE = FLUID_TYPES.register("purified_essence",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.mistech.purified_essence")
                    .isWaterLike(true)
                    .canSwim(true)
                    .canDrown(true)
                    .canExtinguish(true)
                    .supportsBoating(true)
                    .density(900)
                    .viscosity(900)
                    .lightLevel(10)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> PURIFIED_ESSENCE =
            FLUIDS.register("purified_essence", () -> new BaseFlowingFluid.Source(purifiedProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_PURIFIED_ESSENCE =
            FLUIDS.register("flowing_purified_essence", () -> new BaseFlowingFluid.Flowing(purifiedProperties()));

    private static BaseFlowingFluid.Properties purifiedProperties() {
        return new BaseFlowingFluid.Properties(PURIFIED_ESSENCE_TYPE, PURIFIED_ESSENCE, FLOWING_PURIFIED_ESSENCE)
                .bucket(com.mistech.mistechmod.purity.PurityContent.PURIFIED_ESSENCE_BUCKET)
                .block(com.mistech.mistechmod.purity.PurityContent.PURIFIED_ESSENCE_BLOCK)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(6);
    }

    private MistechFluids() {}

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(IMPURE_ESSENCE_TYPE, IMPURE_ESSENCE, FLOWING_IMPURE_ESSENCE)
                .bucket(Mistech.IMPURE_ESSENCE_BUCKET)
                .block(Mistech.IMPURE_ESSENCE_BLOCK)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(8);
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }
}
