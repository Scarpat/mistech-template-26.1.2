package com.mistech.mistechmod.purity.block;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.PurityLayouts;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Destilador Alquímico: 1.000 mB de Essência Impura → 250 mB de Essência Purificada + 1 Borra Impura,
 * em 20 s gastando 20 FE/t e 1 uso do Filtro de Mitril. Se não houver espaço para a Borra, o Destilador
 * ventila: a Borra cai em cima e macula a terra em volta.
 * Tanques: 0 = impura (só entra), 1 = purificada (só sai). 4.000 mB cada.
 */
public class DistillerBlockEntity extends MachineBlockEntity {
    public static final int TANK = 4000;
    public static final int BATCH_IN = 1000;
    public static final int BATCH_OUT = 250;
    public static final int DURATION = 20 * 20;
    public static final int FE_PER_TICK = 20;
    public static final int SLOT_INPUT = 0, SLOT_FILTER = 1;

    private final PurityFluids.Tanks tanks = new PurityFluids.Tanks(TANK,
            new FluidResource[] {PurityFluids.impure(), PurityFluids.pure()},
            new boolean[] {true, false}, new boolean[] {false, true}, this::setChanged);

    public DistillerBlockEntity(BlockPos pos, BlockState state) {
        super(PurityContent.DISTILLER_BE.get(), pos, state, PurityLayouts.DISTILLER, 200, 0);
    }

    public PurityFluids.Tanks getTanks() {
        return this.tanks;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        this.pullInput();
        boolean filter = !this.items.get(SLOT_FILTER).isEmpty();
        int cost = this.energyStep(FE_PER_TICK);
        boolean canRun = filter && this.tanks.amount(0) >= BATCH_IN && this.tanks.space(1) >= BATCH_OUT && this.hasEnergy(cost);
        this.maxProgress = DURATION * 2;
        if (canRun) {
            this.drainEnergy(cost);
            this.progress += this.workStep();
            if (this.progress >= this.maxProgress) {
                this.progress = 0;
                this.finishBatch(level, pos);
            }
            this.setChanged();
        }
        this.info0 = this.tanks.amount(0);
        this.info1 = this.tanks.amount(1);
        this.setLit(level, pos, state, canRun);
    }

    /** Frasco/balde de Essência Impura no espaço de entrada vai para o tanque (o recipiente vazio vai para a saída). */
    private void pullInput() {
        ItemStack in = this.items.get(SLOT_INPUT);
        if (in.isEmpty()) {
            return;
        }
        boolean bottle = in.is(Mistech.IMPURE_ESSENCE_BOTTLE.get());
        int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        ItemStack empty = new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET);
        if (this.tanks.space(0) >= amount && this.canOutput(empty)) {
            in.shrink(1);
            this.tanks.add(0, amount);
            this.insertOutput(empty);
        }
    }

    private void finishBatch(ServerLevel level, BlockPos pos) {
        this.tanks.take(0, BATCH_IN);
        this.tanks.add(1, BATCH_OUT);
        ItemStack filter = this.items.get(SLOT_FILTER);
        int damage = filter.getDamageValue() + 1;
        if (damage >= filter.getMaxDamage()) {
            this.items.set(SLOT_FILTER, ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 0.8F, 1.0F);
        } else {
            filter.setDamageValue(damage);
        }
        ItemStack rest = this.insertOutput(new ItemStack(PurityContent.TAINTED_RESIDUE.get()));
        if (!rest.isEmpty()) {
            // sem espaço: ventila — a borra macula a terra em volta (ou cai em cima se não houver terra)
            if (TaintedSoilBlock.contaminateNear(level, pos, 3, 1, 1) == 0) {
                Block.popResource(level, pos.above(), rest);
            }
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.6F);
        }
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.3F);
    }

    /** Frasco/balde na mão do jogador: põe Essência Impura ou tira Essência Purificada. Só no servidor. */
    public boolean interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        int in = held.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) ? PurityFluids.BOTTLE : held.is(Mistech.IMPURE_ESSENCE_BUCKET.get()) ? PurityFluids.BUCKET : 0;
        if (in > 0) {
            if (this.tanks.space(0) < in) {
                return false;
            }
            this.tanks.add(0, in);
            PurityFluids.swap(player, hand, new ItemStack(in == PurityFluids.BOTTLE ? Items.GLASS_BOTTLE : Items.BUCKET));
            player.level().playSound(null, this.worldPosition, in == PurityFluids.BOTTLE ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        int out = held.is(Items.GLASS_BOTTLE) ? PurityFluids.BOTTLE : held.is(Items.BUCKET) ? PurityFluids.BUCKET : 0;
        if (out > 0) {
            if (!this.tanks.take(1, out)) {
                player.sendOverlayMessage(Component.translatable("message.mistech.distiller.status", this.tanks.amount(0), this.tanks.amount(1)));
                return true;
            }
            PurityFluids.swap(player, hand, new ItemStack(out == PurityFluids.BOTTLE
                    ? PurityContent.PURIFIED_ESSENCE_BOTTLE.get() : PurityContent.PURIFIED_ESSENCE_BUCKET.get()));
            player.level().playSound(null, this.worldPosition, out == PurityFluids.BOTTLE ? SoundEvents.BOTTLE_FILL : SoundEvents.BUCKET_FILL,
                    SoundSource.BLOCKS, 1.0F, 1.2F);
            return true;
        }
        return false;
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.tanks.setAmount(0, input.getIntOr("Impure", 0));
        this.tanks.setAmount(1, input.getIntOr("Pure", 0));
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Impure", this.tanks.amount(0));
        output.putInt("Pure", this.tanks.amount(1));
    }
}
