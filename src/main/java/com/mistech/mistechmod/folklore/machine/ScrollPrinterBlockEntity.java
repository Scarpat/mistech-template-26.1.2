package com.mistech.mistechmod.folklore.machine;

import java.util.List;

import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.block.PurityFluids;
import com.mistech.mistechmod.spellcraft.Spell;
import com.mistech.mistechmod.spellcraft.SpellScrollItem;
import com.mistech.mistechmod.spellcraft.SpellcraftContent;

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
 * Impressora de Pergaminhos: copia o Pergaminho Matriz (um pergaminho de magia no espaço de cima, que não é gasto).
 * Cada cópia: 1 papel + {@link #LAPIS} lápis-lazúli + {@link #ESSENCE} mB de Essência Purificada + {@link #ENERGY} FE, em
 * {@link #TICKS} ticks. Magias "só no mundo" não podem ser copiadas.
 * info0 = tanque (mB), info1 = situação ({@link #STATUS}).
 */
public class ScrollPrinterBlockEntity extends MachineBlockEntity implements ArcaneMachineBlock.EssenceTankHolder {
    public static final int TANK = 4000;
    public static final int LAPIS = 2;
    public static final int ESSENCE = 100;
    public static final int ENERGY = 5000;
    public static final int TICKS = 1200;
    public static final int SLOT_TEMPLATE = 0, SLOT_PAPER = 1, SLOT_LAPIS = 2;
    public static final String[] STATUS = {"idle", "printing", "world_only", "no_paper", "no_lapis", "no_essence", "no_energy", "full"};
    public static final int ST_IDLE = 0, ST_PRINTING = 1, ST_WORLD_ONLY = 2, ST_NO_PAPER = 3, ST_NO_LAPIS = 4, ST_NO_ESSENCE = 5,
            ST_NO_ENERGY = 6, ST_FULL = 7;

    private final PurityFluids.Tanks tank = new PurityFluids.Tanks(TANK, new FluidResource[] {PurityFluids.pure()},
            new boolean[] {true}, new boolean[] {false}, this::setChanged);

    public ScrollPrinterBlockEntity(BlockPos pos, BlockState state) {
        super(FolkMachines.SCROLL_PRINTER_BE.get(), pos, state, FolkMachines.PRINTER_LAYOUT, 500, 0);
        this.maxProgress = TICKS * 2;
    }

    public PurityFluids.Tanks getTank() {
        return this.tank;
    }

    public static boolean isScroll(ItemStack stack) {
        return stack.is(SpellcraftContent.SPELL_SCROLL.get()) && SpellScrollItem.spell(stack) != null;
    }

    public static boolean isPaper(ItemStack stack) {
        return stack.is(Items.PAPER);
    }

    public static boolean isLapis(ItemStack stack) {
        return stack.is(Items.LAPIS_LAZULI);
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        int status = this.status();
        if (status == ST_PRINTING) {
            int fe = this.energyStep((ENERGY + TICKS - 1) / TICKS);
            if (!this.hasEnergy(fe)) {
                status = ST_NO_ENERGY;
            } else {
                this.drainEnergy(fe);
                this.progress += this.workStep();
                if (level.getGameTime() % 10 == 0) {
                    level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.3);
                }
                if (this.progress >= this.maxProgress) {
                    this.progress = 0;
                    this.print(level, pos);
                }
            }
        } else {
            this.progress = 0;
        }
        this.info0 = this.tank.amount(0);
        this.info1 = status;
        this.setLit(level, pos, state, status == ST_PRINTING);
    }

    private int status() {
        ItemStack template = this.items.get(SLOT_TEMPLATE);
        if (!isScroll(template)) {
            return ST_IDLE;
        }
        Spell spell = SpellScrollItem.spell(template);
        if (spell == null || spell.worldOnly()) {
            return ST_WORLD_ONLY;
        }
        if (!isPaper(this.items.get(SLOT_PAPER))) {
            return ST_NO_PAPER;
        }
        if (!isLapis(this.items.get(SLOT_LAPIS)) || this.items.get(SLOT_LAPIS).getCount() < LAPIS) {
            return ST_NO_LAPIS;
        }
        if (this.tank.amount(0) < ESSENCE) {
            return ST_NO_ESSENCE;
        }
        if (!this.canOutput(template.copyWithCount(1))) {
            return ST_FULL;
        }
        return ST_PRINTING;
    }

    private void print(ServerLevel level, BlockPos pos) {
        ItemStack copy = this.items.get(SLOT_TEMPLATE).copyWithCount(1);
        this.items.get(SLOT_PAPER).shrink(1);
        this.items.get(SLOT_LAPIS).shrink(LAPIS);
        this.tank.take(0, ESSENCE);
        this.insertOutput(copy);
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.03);
        this.setChanged();
    }

    // ----- essência na mão -----

    @Override
    public boolean handles(ItemStack stack) {
        return stack.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) || stack.is(PurityContent.PURIFIED_ESSENCE_BUCKET.get());
    }

    @Override
    public void interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean bottle = held.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get());
        int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        if (this.tank.space(0) < amount) {
            player.sendOverlayMessage(Component.translatable("info.mistech.tank", Component.translatable("block.mistech.purified_essence"),
                    this.tank.amount(0), TANK));
            return;
        }
        this.tank.add(0, amount);
        PurityFluids.swap(player, hand, new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET));
        player.level().playSound(null, this.worldPosition, bottle ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.2F);
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("info.mistech.tank", Component.translatable("block.mistech.purified_essence"), this.tank.amount(0), TANK));
        lines.add(Component.translatable("gui.mistech.scroll_printer.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, this.info1))]));
        super.addInfo(lines);
    }

    public static List<Component> infoLines(int status) {
        return List.of(Component.translatable("gui.mistech.scroll_printer.status." + STATUS[Math.max(0, Math.min(STATUS.length - 1, status))]));
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
