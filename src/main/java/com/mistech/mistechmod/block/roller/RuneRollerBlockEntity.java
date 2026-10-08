package com.mistech.mistechmod.block.roller;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.MagicSmokableItem;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.menu.RuneRollerMenu;
import com.mistech.mistechmod.recipe.MagicSmokeRecipe;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Enroladora Rúnica: faz Charutos e Baseados Mágicos sozinha.
 * Espaços: 0..3 runas, 4 tabaco seco, 5 cânhamo seco, 6 Resíduo Mágico, 7..9 saída.
 * Cada item gasta 1 uso da runa (uma runa nova rende 16), 1 folha e 1 resíduo.
 * Opções (0..19): elemento * 2 + (0 = charuto, 1 = baseado).
 * Com o botão de energia ligado e uma opção escolhida, enrola 1 a cada 2 s enquanto houver material.
 */
public class RuneRollerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int RUNE_START = 0, RUNE_SLOTS = 4;
    public static final int TOBACCO_SLOT = 4, HEMP_SLOT = 5, RESIDUE_SLOT = 6;
    public static final int OUTPUT_START = 7, OUTPUT_SLOTS = 3;
    public static final int SIZE = OUTPUT_START + OUTPUT_SLOTS;
    public static final int OPTIONS = RuneElement.values().length * 2;
    public static final int AUTO_TICKS = 40;
    public static final int DATA_COUNT = 5;

    private static final Component DEFAULT_NAME = Component.translatable("container.mistech.rune_roller");
    private static final int[] SLOTS_IN = {0, 1, 2, 3, TOBACCO_SLOT, HEMP_SLOT, RESIDUE_SLOT};
    private static final int[] SLOTS_OUT = {7, 8, 9};

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int selected = -1;
    private boolean autoMode;
    private int progress;

    private long maskTime = -1;
    private int mask;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int id) {
            RuneRollerBlockEntity self = RuneRollerBlockEntity.this;
            return switch (id) {
                case 0 -> self.selected + 1;
                case 1 -> self.autoMode ? 1 : 0;
                case 2 -> self.craftableMask() & 0xFFFF;
                case 3 -> self.craftableMask() >>> 16;
                case 4 -> self.progress;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public RuneRollerBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.RUNE_ROLLER.get(), pos, state);
    }

    public static RuneElement elementOf(int option) {
        return RuneElement.values()[option / 2];
    }

    public static boolean isTobacco(int option) {
        return option % 2 == 0;
    }

    public static ItemStack preview(int option) {
        return MagicSmokableItem.create(MagicSmokeRecipe.resultItem(isTobacco(option)), elementOf(option), 1);
    }

    /** Regra de cada espaço (usada também pela tela no cliente). */
    public static boolean isValidFor(int slot, ItemStack stack) {
        if (slot < RUNE_SLOTS) {
            return RuneElement.ofRune(stack) != null;
        }
        return switch (slot) {
            case TOBACCO_SLOT -> stack.is(Mistech.DRIED_TOBACCO_LEAF.get());
            case HEMP_SLOT -> stack.is(Mistech.DRIED_HEMP_LEAF.get());
            case RESIDUE_SLOT -> stack.is(Mistech.MAGIC_RESIDUE.get());
            default -> false;
        };
    }

    // ----- tick -----

    public static void serverTick(Level level, BlockPos pos, BlockState state, RuneRollerBlockEntity be) {
        if (!be.autoMode || be.selected < 0) {
            if (be.progress != 0) {
                be.progress = 0;
            }
            return;
        }
        if (be.findRune(elementOf(be.selected)) < 0 || !be.canMake(be.selected)) {
            be.progress = 0;
            return;
        }
        if (++be.progress >= AUTO_TICKS) {
            be.progress = 0;
            be.make(be.selected, 1);
        }
    }

    // ----- fazer -----

    private int findRune(RuneElement element) {
        for (int slot = RUNE_START; slot < RUNE_START + RUNE_SLOTS; slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty() && RuneElement.ofRune(stack) == element && MagicSmokeRecipe.hasUseLeft(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private boolean canMake(int option) {
        if (option < 0 || option >= OPTIONS || this.findRune(elementOf(option)) < 0) {
            return false;
        }
        int leafSlot = isTobacco(option) ? TOBACCO_SLOT : HEMP_SLOT;
        return !this.items.get(leafSlot).isEmpty() && !this.items.get(RESIDUE_SLOT).isEmpty()
                && this.fitsOutput(preview(option));
    }

    private boolean fitsOutput(ItemStack result) {
        for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUT_SLOTS; slot++) {
            ItemStack stack = this.items.get(slot);
            if (stack.isEmpty() || (ItemStack.isSameItemSameComponents(stack, result) && stack.getCount() < stack.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    /** Faz até {@code times} itens; devolve quantos fez. */
    public int make(int option, int times) {
        int made = 0;
        while (made < times && this.canMake(option)) {
            int runeSlot = this.findRune(elementOf(option));
            this.items.set(runeSlot, MagicSmokeRecipe.usedRune(this.items.get(runeSlot)));
            this.items.get(isTobacco(option) ? TOBACCO_SLOT : HEMP_SLOT).shrink(1);
            this.items.get(RESIDUE_SLOT).shrink(1);
            ItemStack result = preview(option);
            for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUT_SLOTS && !result.isEmpty(); slot++) {
                ItemStack stack = this.items.get(slot);
                if (stack.isEmpty()) {
                    this.items.set(slot, result);
                    result = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(stack, result) && stack.getCount() < stack.getMaxStackSize()) {
                    stack.grow(1);
                    result = ItemStack.EMPTY;
                }
            }
            made++;
        }
        if (made > 0 && this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.3F);
            this.setChanged();
        }
        return made;
    }

    private int craftableMask() {
        if (this.level == null) {
            return 0;
        }
        long time = this.level.getGameTime();
        if (time != this.maskTime) {
            this.maskTime = time;
            this.mask = 0;
            for (int option = 0; option < OPTIONS; option++) {
                if (this.canMake(option)) {
                    this.mask |= 1 << option;
                }
            }
        }
        return this.mask;
    }

    /** Botões: opção (clicar de novo faz 1), 100 + opção (shift: faz até acabar), 1000 (energia). */
    public void handleButton(int id) {
        if (!(this.level instanceof ServerLevel level)) {
            return;
        }
        if (id == RuneRollerMenu.BUTTON_POWER) {
            this.autoMode = !this.autoMode;
            this.progress = 0;
            level.playSound(null, this.worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, this.autoMode ? 0.8F : 0.6F);
            this.setChanged();
            return;
        }
        boolean many = id >= RuneRollerMenu.BUTTON_MANY;
        int option = many ? id - RuneRollerMenu.BUTTON_MANY : id;
        if (option < 0 || option >= OPTIONS) {
            return;
        }
        if (option != this.selected) {
            this.selected = option;
            this.progress = 0;
            this.setChanged();
            if (!many) {
                return;
            }
        }
        this.make(option, many ? 64 : 1);
    }

    // ----- Container -----

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isValidFor(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_OUT : SLOTS_IN;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isValidFor(slot, stack) && (slot >= RUNE_SLOTS || this.items.get(slot).isEmpty());
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // saída pronta, ou runa já gasta (não sobra nada dela, então só as saídas)
        return slot >= OUTPUT_START;
    }

    public int getComparatorLevel() {
        int count = 0;
        for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUT_SLOTS; slot++) {
            count += this.items.get(slot).getCount();
        }
        return count == 0 ? 0 : 1 + Math.min(14, count * 14 / (OUTPUT_SLOTS * 64));
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new RuneRollerMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.selected = input.getIntOr("Selected", -1);
        this.autoMode = input.getBooleanOr("Auto", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("Selected", this.selected);
        output.putBoolean("Auto", this.autoMode);
    }
}
