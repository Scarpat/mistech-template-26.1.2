package com.mistech.mistechmod.machine.arcane;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.purity.block.PurityFluids;
import com.mistech.mistechmod.registry.MachineContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Liquefator Rúnico: derrete a magia impura sólida de volta em Essência Impura.
 * Fragmento = 250 mB, Pó de Magia Impura = 125 mB, Bloco de Magia Impura = 750 mB; {@link #SECONDS} s por item,
 * {@link #FE_PER_TICK} FE/t. Tanque de {@link #TANK} mB (canos e golens tiram). Frascos de vidro / baldes no espaço
 * de recipientes saem cheios.
 * Espaços: 0 = magia sólida, 1 = recipientes vazios, 2..3 = saída. info1 = tanque (mB).
 */
public class LiquefierBlockEntity extends MachineBlockEntity implements ArcaneMachineBlock.EssenceTankHolder {
    public static final int TANK = 8000;
    public static final int SECONDS = 5;
    public static final int FE_PER_TICK = 25;
    public static final int SLOT_SOLID = 0, SLOT_EMPTY = 1;

    private final PurityFluids.Tanks tank = new PurityFluids.Tanks(TANK, new FluidResource[] {PurityFluids.impure()},
            new boolean[] {false}, new boolean[] {true}, this::setChanged);

    public LiquefierBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneMachines.LIQUEFIER_BE.get(), pos, state, ArcaneMachines.LIQUEFIER_LAYOUT, 300, 0);
        this.maxProgress = SECONDS * 20 * 2;
    }

    public PurityFluids.Tanks getTank() {
        return this.tank;
    }

    /** mB de essência que o item dá (0 = não derrete). */
    public static int essenceOf(ItemStack stack) {
        if (stack.is(Mistech.IMPURE_MAGIC_SHARD.get())) {
            return 250;
        }
        if (stack.is(MachineContent.IMPURE_MAGIC_DUST.get())) {
            return 125;
        }
        if (stack.is(Mistech.IMPURE_MAGIC_BLOCK_ITEM.get())) {
            return 750;
        }
        return 0;
    }

    public static boolean isContainer(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE) || stack.is(Items.BUCKET);
    }

    @Override
    protected float taintPerMinute() {
        return com.mistech.mistechmod.Config.areaTaintEssenceMachines();
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        this.fillContainers();
        ItemStack solid = this.items.get(SLOT_SOLID);
        int amount = essenceOf(solid);
        int cost = this.energyStep(FE_PER_TICK);
        boolean working = amount > 0 && this.tank.space(0) >= amount && this.hasEnergy(cost);
        if (working) {
            this.drainEnergy(cost);
            this.progress += this.workStep();
            if (this.progress >= this.maxProgress) {
                this.progress = 0;
                solid.shrink(1);
                this.tank.add(0, amount);
                level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.6F, 0.7F);
                level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 6, 0.2, 0.05, 0.2, 0.0);
            }
            this.setChanged();
        } else if (amount == 0 && this.progress > 0) {
            this.progress = 0;
        }
        this.info1 = this.tank.amount(0);
        this.setLit(level, pos, state, working);
    }

    private void fillContainers() {
        ItemStack in = this.items.get(SLOT_EMPTY);
        if (in.isEmpty()) {
            return;
        }
        boolean bottle = in.is(Items.GLASS_BOTTLE);
        int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        ItemStack full = new ItemStack(bottle ? Mistech.IMPURE_ESSENCE_BOTTLE.get() : Mistech.IMPURE_ESSENCE_BUCKET.get());
        if (this.tank.amount(0) >= amount && this.canOutput(full)) {
            this.tank.take(0, amount);
            in.shrink(1);
            this.insertOutput(full);
        }
    }

    @Override
    public boolean handles(ItemStack stack) {
        return isContainer(stack);
    }

    @Override
    public void interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        int out = held.is(Items.GLASS_BOTTLE) ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        if (!this.tank.take(0, out)) {
            player.sendOverlayMessage(Component.translatable("info.mistech.tank", Component.translatable("block.mistech.impure_essence"),
                    this.tank.amount(0), TANK));
            return;
        }
        PurityFluids.swap(player, hand, new ItemStack(out == PurityFluids.BOTTLE ? Mistech.IMPURE_ESSENCE_BOTTLE.get() : Mistech.IMPURE_ESSENCE_BUCKET.get()));
        player.level().playSound(null, this.worldPosition, out == PurityFluids.BOTTLE ? SoundEvents.BOTTLE_FILL : SoundEvents.BUCKET_FILL,
                SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.mistech.tank", Component.translatable("block.mistech.impure_essence"), this.tank.amount(0), TANK));
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
}
