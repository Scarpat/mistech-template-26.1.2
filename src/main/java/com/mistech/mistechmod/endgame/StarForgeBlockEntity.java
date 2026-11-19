package com.mistech.mistechmod.endgame;

import java.util.List;

import com.mistech.mistechmod.celestial.Celestial;
import com.mistech.mistechmod.celestial.CelestialContent;
import com.mistech.mistechmod.celestial.CelestialEvent;
import com.mistech.mistechmod.fluid.MoltenMetals;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.area.AreaTaint;
import com.mistech.mistechmod.purity.block.PurityFluids;
import com.mistech.mistechmod.registry.OreContent;

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
 * Forja Estelar (coração de um multibloco 5×5, ver {@link EndgameStructures#forge}): funde os três metais com Essência
 * Purificada e um Fragmento de Estrela num Lingote Celeste.
 * <ul>
 * <li>Metais: lingote = {@link #INGOT} mB, bloco = 9 lingotes, balde de metal fundido = 1.000 mB (também por canos).</li>
 * <li>Cada lingote: {@link #METAL} mB de cada metal + {@link #ESSENCE} mB de Essência Purificada + 1 Fragmento de Estrela +
 * {@link #ENERGY} FE em {@link #TICKS} ticks.</li>
 * <li>Só à noite (o Eclipse vale), com céu aberto acima do vidro e a Mácula do lugar abaixo de {@link #MAX_TAINT}.
 * A Chuva de Estrelas corta o tempo pela metade.</li>
 * </ul>
 * info0..2 = metais (mB), info3 = Essência Purificada (mB).
 */
public class StarForgeBlockEntity extends MachineBlockEntity implements ArcaneMachineBlock.EssenceTankHolder {
    public static final int CAPACITY = 1_000_000;
    public static final int TANK = 4000;
    public static final int INGOT = 144;
    public static final int METAL = 144;
    public static final int ESSENCE = 250;
    public static final int ENERGY = 500_000;
    public static final int TICKS = 2400;
    public static final float MAX_TAINT = 20.0F;
    public static final int MITHRIL = 0, ADAMANTINE = 1, ORICHALCUM = 2, PURE = 3;
    public static final int SLOT_METAL = 0, SLOT_FRAGMENT = 1;

    public static final String[] STATUS = {"forging", "structure", "day", "sky", "taint", "no_metal", "no_essence", "no_fragment",
            "no_energy", "full"};
    public static final int ST_FORGING = 0, ST_STRUCTURE = 1, ST_DAY = 2, ST_SKY = 3, ST_TAINT = 4, ST_NO_METAL = 5, ST_NO_ESSENCE = 6,
            ST_NO_FRAGMENT = 7, ST_NO_ENERGY = 8, ST_FULL = 9;

    private final PurityFluids.Tanks tanks = new PurityFluids.Tanks(TANK, new FluidResource[] {
            FluidResource.of(MoltenMetals.MITHRIL.source.get()), FluidResource.of(MoltenMetals.ADAMANTINE.source.get()),
            FluidResource.of(MoltenMetals.ORICHALCUM.source.get()), PurityFluids.pure()},
            new boolean[] {true, true, true, true}, new boolean[] {false, false, false, false}, this::setChanged);

    private int status = ST_STRUCTURE;
    private int missing = -1;
    private int structureTimer;

    public StarForgeBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        super(EndgameContent.STAR_FORGE_BE.get(), pos, state, EndgameContent.STAR_FORGE_LAYOUT, 10_000, 0);
        this.maxProgress = TICKS * 2;
    }

    public PurityFluids.Tanks getTanks() {
        return this.tanks;
    }

    /** Tanque do metal (−1 = não é metal da forja). */
    public static int metalTank(ItemStack stack) {
        if (stack.is(OreContent.MITHRIL_INGOT.get()) || stack.is(OreContent.MITHRIL_BLOCK_ITEM.get()) || stack.is(MoltenMetals.MITHRIL.bucket.get())) {
            return MITHRIL;
        }
        if (stack.is(OreContent.ADAMANTINE_INGOT.get()) || stack.is(OreContent.ADAMANTINE_BLOCK_ITEM.get())
                || stack.is(MoltenMetals.ADAMANTINE.bucket.get())) {
            return ADAMANTINE;
        }
        if (stack.is(OreContent.ORICHALCUM_INGOT.get()) || stack.is(OreContent.ORICHALCUM_BLOCK_ITEM.get())
                || stack.is(MoltenMetals.ORICHALCUM.bucket.get())) {
            return ORICHALCUM;
        }
        return -1;
    }

    /** mB de uma unidade do item. */
    private static int metalAmount(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.BucketItem) {
            return PurityFluids.BUCKET;
        }
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
            return INGOT * 9;
        }
        return INGOT;
    }

    public static boolean isMetal(ItemStack stack) {
        return metalTank(stack) >= 0;
    }

    public static boolean isFragment(ItemStack stack) {
        return stack.is(CelestialContent.STAR_FRAGMENT.get());
    }

    @Override
    protected float taintPerMinute() {
        return 0.0F;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        this.melt();
        if (--this.structureTimer <= 0 || this.missing < 0) {
            this.structureTimer = 40;
            this.missing = EndgameStructures.missing(level, EndgameStructures.forge(pos));
        }
        int st = this.check(level, pos);
        if (st == ST_FORGING) {
            int fe = this.energyStep((ENERGY + TICKS - 1) / TICKS);
            if (!this.hasEnergy(fe)) {
                st = ST_NO_ENERGY;
            } else {
                this.drainEnergy(fe);
                int step = this.workStep();
                if (Celestial.isActive(level, CelestialEvent.STARFALL)) {
                    step *= 2;
                }
                this.progress += step;
                if (level.getGameTime() % 8 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 2.2, pos.getZ() + 0.5, 3, 0.15, 0.6, 0.15, 0.01);
                    level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.25, 0.05, 0.25, 0.01);
                }
                if (this.progress >= this.maxProgress) {
                    this.progress = 0;
                    this.forge(level, pos);
                }
            }
        } else {
            this.progress = 0;
        }
        this.status = st;
        this.info0 = this.tanks.amount(MITHRIL);
        this.info1 = this.tanks.amount(ADAMANTINE);
        this.info2 = this.tanks.amount(ORICHALCUM);
        this.info3 = this.tanks.amount(PURE);
        this.setLit(level, pos, state, st == ST_FORGING);
    }

    private int check(ServerLevel level, BlockPos pos) {
        if (this.missing > 0) {
            return ST_STRUCTURE;
        }
        if (!level.isDarkOutside() && !Celestial.isActive(level, CelestialEvent.ECLIPSE)) {
            return ST_DAY;
        }
        if (!level.canSeeSky(pos.above(2))) {
            return ST_SKY;
        }
        if (AreaTaint.get(level, pos) >= MAX_TAINT) {
            return ST_TAINT;
        }
        for (int t = MITHRIL; t <= ORICHALCUM; t++) {
            if (this.tanks.amount(t) < METAL) {
                return ST_NO_METAL;
            }
        }
        if (this.tanks.amount(PURE) < ESSENCE) {
            return ST_NO_ESSENCE;
        }
        if (!isFragment(this.items.get(SLOT_FRAGMENT))) {
            return ST_NO_FRAGMENT;
        }
        if (!this.canOutput(new ItemStack(EndgameContent.CELESTIAL_INGOT.get()))) {
            return ST_FULL;
        }
        return ST_FORGING;
    }

    private void forge(ServerLevel level, BlockPos pos) {
        for (int t = MITHRIL; t <= ORICHALCUM; t++) {
            this.tanks.take(t, METAL);
        }
        this.tanks.take(PURE, ESSENCE);
        this.items.get(SLOT_FRAGMENT).shrink(1);
        this.insertOutput(new ItemStack(EndgameContent.CELESTIAL_INGOT.get()));
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.6F);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 1.4F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.08);
        level.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 3.0, pos.getZ() + 0.5, 20, 0.2, 1.0, 0.2, 0.05);
        this.setChanged();
    }

    /** Lingotes, blocos e baldes no espaço de metal vão para os tanques (os baldes vazios saem na saída). */
    private void melt() {
        ItemStack in = this.items.get(SLOT_METAL);
        int tank = metalTank(in);
        if (tank < 0) {
            return;
        }
        int amount = metalAmount(in);
        if (this.tanks.space(tank) < amount) {
            return;
        }
        boolean bucket = in.getItem() instanceof net.minecraft.world.item.BucketItem;
        if (bucket) {
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!this.canOutput(empty)) {
                return;
            }
            this.insertOutput(empty);
        }
        in.shrink(1);
        this.tanks.add(tank, amount);
        this.setChanged();
    }

    // ----- na mão: essência, metal fundido, lingotes -----

    @Override
    public boolean handles(ItemStack stack) {
        return stack.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) || stack.is(PurityContent.PURIFIED_ESSENCE_BUCKET.get())
                || stack.getItem() instanceof net.minecraft.world.item.BucketItem && isMetal(stack);
    }

    @Override
    public void interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        int tank = isMetal(held) ? metalTank(held) : PURE;
        boolean bottle = held.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get());
        int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        if (this.tanks.space(tank) < amount) {
            player.sendOverlayMessage(Component.translatable("info.mistech.tank", tankName(tank), this.tanks.amount(tank), TANK));
            return;
        }
        this.tanks.add(tank, amount);
        PurityFluids.swap(player, hand, new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET));
        player.level().playSound(null, this.worldPosition, tank == PURE
                ? (bottle ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY) : SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public static Component tankName(int tank) {
        return switch (tank) {
            case MITHRIL -> Component.translatable("block.mistech.molten_mithril");
            case ADAMANTINE -> Component.translatable("block.mistech.molten_adamantine");
            case ORICHALCUM -> Component.translatable("block.mistech.molten_orichalcum");
            default -> Component.translatable("block.mistech.purified_essence");
        };
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("gui.mistech.star_forge.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, this.status))],
                Math.max(0, this.missing)));
        for (int t = MITHRIL; t <= PURE; t++) {
            lines.add(Component.translatable("info.mistech.tank", tankName(t), this.tanks.amount(t), TANK));
        }
        super.addInfo(lines);
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.tanks.setAmount(MITHRIL, input.getIntOr("Mithril", 0));
        this.tanks.setAmount(ADAMANTINE, input.getIntOr("Adamantine", 0));
        this.tanks.setAmount(ORICHALCUM, input.getIntOr("Orichalcum", 0));
        this.tanks.setAmount(PURE, input.getIntOr("Essence", 0));
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Mithril", this.tanks.amount(MITHRIL));
        output.putInt("Adamantine", this.tanks.amount(ADAMANTINE));
        output.putInt("Orichalcum", this.tanks.amount(ORICHALCUM));
        output.putInt("Essence", this.tanks.amount(PURE));
    }
}
