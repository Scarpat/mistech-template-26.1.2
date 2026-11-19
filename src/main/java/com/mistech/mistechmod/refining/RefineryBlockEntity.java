package com.mistech.mistechmod.refining;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.machine.MachineLayout;
import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;
import com.mistech.mistechmod.purity.block.PurityFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Base da Lavadora de Essência e do Cristalizador Rúnico: 1 minério por vez, gasta essência do tanque e energia,
 * solta o produto da receita {@link OreRefiningRecipe} (passa pelos engastes) e os subprodutos.
 * Espaços: 0 = minério, 1 = frascos/baldes de essência (enchem o tanque), 2..3 = saída.
 * info0 = luz (%), info1 = tanque (mB).
 */
public abstract class RefineryBlockEntity extends MachineBlockEntity implements ArcaneMachineBlock.EssenceTankHolder {
    public static final int TANK = 4000;
    public static final int SLOT_ORE = 0, SLOT_CONTAINER = 1;

    protected final PurityFluids.Tanks tank;
    private @Nullable RecipeHolder<OreRefiningRecipe> lastRecipe;
    private double progressCarry;

    protected RefineryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineLayout layout) {
        super(type, pos, state, layout, 500, 0);
        this.tank = new PurityFluids.Tanks(TANK, new FluidResource[] {this.pure() ? PurityFluids.pure() : PurityFluids.impure()},
                new boolean[] {true}, new boolean[] {false}, this::setChanged);
        this.maxProgress = this.ticksPerOre() * 2;
    }

    protected abstract OreRefiningRecipe.Stage stage();

    /** Essência Purificada (true) ou Impura (false). */
    protected abstract boolean pure();

    protected abstract int essencePerOre();

    protected abstract int ticksPerOre();

    protected abstract int fePerTick();

    /** Multiplicador de velocidade pela luz (0 = parado). Só o Cristalizador usa. */
    protected double lightFactor(ServerLevel level, BlockPos pos) {
        return 1.0;
    }

    public PurityFluids.Tanks getTank() {
        return this.tank;
    }

    public Optional<RecipeHolder<OreRefiningRecipe>> find(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        var found = level.recipeAccess().getRecipeFor(RefiningContent.TYPE.get(), new OreRefiningRecipe.Input(this.stage(), stack), level,
                this.lastRecipe);
        found.ifPresent(r -> this.lastRecipe = r);
        return found;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        this.fillTank();
        this.info1 = this.tank.amount(0);
        double light = this.lightFactor(level, pos);
        this.info0 = (int) Math.round(light * 100);
        ItemStack ore = this.items.get(SLOT_ORE);
        var holder = this.find(level, ore);
        if (holder.isEmpty()) {
            this.progress = 0;
            this.progressCarry = 0;
            this.setLit(level, pos, state, false);
            return;
        }
        OreRefiningRecipe recipe = holder.get().value();
        ItemStack result = recipe.result().create();
        int cost = this.energyStep(this.fePerTick());
        boolean working = light > 0 && this.tank.amount(0) >= this.essencePerOre() && this.hasEnergy(cost) && this.canOutputProduct(result);
        if (working) {
            this.drainEnergy(cost);
            double step = this.workStep() * light + this.progressCarry;
            int whole = (int) Math.floor(step);
            this.progressCarry = step - whole;
            this.progress += whole;
            this.maxProgress = this.ticksPerOre() * 2;
            if (this.progress >= this.maxProgress) {
                this.progress = 0;
                ore.shrink(1);
                this.tank.take(0, this.essencePerOre());
                this.insertProduct(level, result);
                for (ItemStack extra : recipe.rollByproducts(level.getRandom())) {
                    ItemStack rest = this.insertOutput(extra);
                    if (!rest.isEmpty()) {
                        net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, rest);
                    }
                }
                this.finished(level, pos);
            }
            this.setChanged();
        }
        this.setLit(level, pos, state, working);
    }

    /** Som e partículas de cada minério pronto. */
    protected abstract void finished(ServerLevel level, BlockPos pos);

    private void fillTank() {
        ItemStack in = this.items.get(SLOT_CONTAINER);
        if (in.isEmpty() || !this.handles(in)) {
            return;
        }
        boolean bottle = isBottle(in);
        int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        ItemStack empty = new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET);
        if (this.tank.space(0) >= amount && this.canOutput(empty)) {
            this.tank.add(0, amount);
            in.shrink(1);
            this.insertOutput(empty);
        }
    }

    private static boolean isBottle(ItemStack stack) {
        return stack.is(com.mistech.mistechmod.Mistech.IMPURE_ESSENCE_BOTTLE.get())
                || stack.is(com.mistech.mistechmod.purity.PurityContent.PURIFIED_ESSENCE_BOTTLE.get());
    }

    /** Frasco/balde cheio da essência certa. */
    public static boolean isEssence(ItemStack stack, boolean pure) {
        return pure ? stack.is(com.mistech.mistechmod.purity.PurityContent.PURIFIED_ESSENCE_BOTTLE.get())
                || stack.is(com.mistech.mistechmod.purity.PurityContent.PURIFIED_ESSENCE_BUCKET.get())
                : stack.is(com.mistech.mistechmod.Mistech.IMPURE_ESSENCE_BOTTLE.get())
                || stack.is(com.mistech.mistechmod.Mistech.IMPURE_ESSENCE_BUCKET.get());
    }

    @Override
    public boolean handles(ItemStack stack) {
        return isEssence(stack, this.pure());
    }

    @Override
    public void interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean bottle = isBottle(held);
        int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        if (this.tank.space(0) < amount) {
            player.sendOverlayMessage(Component.translatable("info.mistech.tank", this.fluidName(), this.tank.amount(0), TANK));
            return;
        }
        this.tank.add(0, amount);
        PurityFluids.swap(player, hand, new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET));
        player.level().playSound(null, this.worldPosition, bottle ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private Component fluidName() {
        return Component.translatable(this.pure() ? "block.mistech.purified_essence" : "block.mistech.impure_essence");
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.mistech.tank", this.fluidName(), this.tank.amount(0), TANK));
        super.addInfo(lines);
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.tank.setAmount(0, input.getIntOr("Essence", 0));
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Essence", this.tank.amount(0));
    }

    /** Filtro do espaço de minério (vale no cliente também): qualquer minério de bloco. */
    public static boolean isOre(ItemStack stack) {
        return stack.is(net.neoforged.neoforge.common.Tags.Items.ORES);
    }
}
