package com.mistech.mistechmod.block;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.menu.RunicFurnaceMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Slots: 0 = entrada, 1 = saída. Sem slot de combustível.
 * Usa as receitas de fornalha comum (tipo minecraft:smelting) com 75% do tempo.
 */
public class RunicFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    /** Multiplicador do tempo de cozimento (0.75 = 25% mais rápida que a fornalha comum). */
    public static final float SPEED_FACTOR = 0.75F;

    private static final int[] SLOTS_UP_AND_SIDES = {SLOT_INPUT};
    private static final int[] SLOTS_DOWN = {SLOT_OUTPUT};
    private static final Component DEFAULT_NAME = Component.translatable("container.mistech.runic_furnace");

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int cookProgress;
    private int cookTotal;
    private float storedExperience;
    private @Nullable RecipeHolder<SmeltingRecipe> lastRecipe;

    /** 0 = progresso, 1 = tempo total (sincronizados com a tela). */
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int id) {
            return id == 0 ? RunicFurnaceBlockEntity.this.cookProgress : RunicFurnaceBlockEntity.this.cookTotal;
        }

        @Override
        public void set(int id, int value) {
            if (id == 0) {
                RunicFurnaceBlockEntity.this.cookProgress = value;
            } else {
                RunicFurnaceBlockEntity.this.cookTotal = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public RunicFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.RUNIC_FURNACE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RunicFurnaceBlockEntity furnace) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ItemStack input = furnace.items.get(SLOT_INPUT);
        boolean cooking = false;

        if (!input.isEmpty()) {
            SingleRecipeInput recipeInput = new SingleRecipeInput(input);
            Optional<RecipeHolder<SmeltingRecipe>> found =
                    serverLevel.recipeAccess().getRecipeFor(RecipeType.SMELTING, recipeInput, serverLevel, furnace.lastRecipe);
            if (found.isPresent()) {
                furnace.lastRecipe = found.get();
                SmeltingRecipe recipe = found.get().value();
                ItemStack result = recipe.assemble(recipeInput);
                if (furnace.canOutput(result)) {
                    cooking = true;
                    furnace.cookTotal = Math.max(1, Math.round(recipe.cookingTime() * SPEED_FACTOR));
                    furnace.cookProgress += com.mistech.mistechmod.machine.GnomeBoost.extraStep(level, pos) ? 2 : 1;
                    if (furnace.cookProgress >= furnace.cookTotal) {
                        furnace.cookProgress = 0;
                        furnace.addOutput(result);
                        input.shrink(1);
                        furnace.storedExperience += recipe.experience();
                    }
                    setChanged(level, pos, state);
                }
            }
        }

        if (!cooking && furnace.cookProgress > 0) {
            furnace.cookProgress = 0;
            setChanged(level, pos, state);
        }
        if (state.getValue(RunicFurnaceBlock.LIT) != cooking) {
            level.setBlock(pos, state.setValue(RunicFurnaceBlock.LIT, cooking), Block.UPDATE_ALL);
        }
    }

    private boolean canOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = this.items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void addOutput(ItemStack result) {
        ItemStack output = this.items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            this.items.set(SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }
    }

    /** Solta a experiência acumulada (chamado quando o jogador tira itens da saída). */
    public void awardExperience(Player player) {
        if (!(this.level instanceof ServerLevel serverLevel) || this.storedExperience <= 0.0F) {
            return;
        }
        int whole = Mth.floor(this.storedExperience);
        float fraction = this.storedExperience - whole;
        if (fraction > 0.0F && serverLevel.getRandom().nextFloat() < fraction) {
            whole++;
        }
        this.storedExperience = 0.0F;
        if (whole > 0) {
            ExperienceOrb.award(serverLevel, player.position(), whole);
        }
        this.setChanged();
    }

    // ----- Container -----

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    public int getContainerSize() {
        return 2;
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
        return slot == SLOT_INPUT;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return direction == Direction.DOWN ? SLOTS_DOWN : SLOTS_UP_AND_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.cookProgress = input.getIntOr("CookProgress", 0);
        this.cookTotal = input.getIntOr("CookTotal", 0);
        this.storedExperience = input.getFloatOr("StoredXp", 0.0F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("CookProgress", this.cookProgress);
        output.putInt("CookTotal", this.cookTotal);
        output.putFloat("StoredXp", this.storedExperience);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new RunicFurnaceMenu(containerId, inventory, this, this.dataAccess);
    }
}
