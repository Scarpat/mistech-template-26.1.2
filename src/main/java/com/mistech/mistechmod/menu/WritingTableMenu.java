package com.mistech.mistechmod.menu;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.recipe.WritingRecipe;
import com.mistech.mistechmod.recipe.WritingRecipeInput;
import com.mistech.mistechmod.registry.MistechMenus;
import com.mistech.mistechmod.registry.MistechRecipes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/**
 * Mesa de Escrita.
 * Esquerda: giz (1, ferramenta com durabilidade) e papel.
 * Meio: 4 espaços em cruz para ingredientes (em qualquer posição).
 * Direita: saída.
 * Cada escrita gasta 1 papel, 1 de cada ingrediente e {@code chalk_damage} de durabilidade do giz.
 * Os itens de entrada ficam guardados na mesa (block entity).
 */
public class WritingTableMenu extends AbstractContainerMenu {
    // índices no container da mesa (e nos slots do menu)
    public static final int CHALK = 0, PAPER = 1, TOP = 2, LEFT = 3, RIGHT = 4, BOTTOM = 5;
    public static final int INPUT_COUNT = 6;
    public static final int RESULT = 6;
    private static final int INV_START = 7, INV_END = INV_START + 27, HOTBAR_END = INV_END + 9;

    // posições na tela (176x166)
    public static final int CHALK_X = 20, CHALK_Y = 22;
    public static final int PAPER_X = 20, PAPER_Y = 48;
    public static final int CROSS_X = 80, CROSS_Y = 35;   // centro da cruz (fica vazio)
    public static final int RESULT_X = 144, RESULT_Y = 35;

    private final Container inputs;
    private final ContainerLevelAccess access;
    private final Level level;
    private final ResultContainer resultContainer = new ResultContainer();
    private final ItemStack[] lastSeen = new ItemStack[INPUT_COUNT];
    private @Nullable WritingRecipe currentRecipe;
    /** A saída atual é um Selamento em Resina (receita especial, sem WritingRecipe). */
    private boolean amberSeal;
    private long lastSoundTime;

    /** Construtor do lado do cliente. */
    public WritingTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(INPUT_COUNT), ContainerLevelAccess.NULL);
    }

    public WritingTableMenu(int containerId, Inventory inventory, Container inputs, ContainerLevelAccess access) {
        super(MistechMenus.WRITING_TABLE.get(), containerId);
        checkContainerSize(inputs, INPUT_COUNT);
        this.inputs = inputs;
        this.access = access;
        this.level = inventory.player.level();
        java.util.Arrays.fill(this.lastSeen, ItemStack.EMPTY);

        this.addSlot(new InputSlot(CHALK, CHALK_X, CHALK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isChalk(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new InputSlot(PAPER, PAPER_X, PAPER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isPaper(stack);
            }
        });
        this.addSlot(new InputSlot(TOP, CROSS_X, CROSS_Y - 18));
        this.addSlot(new InputSlot(LEFT, CROSS_X - 18, CROSS_Y));
        this.addSlot(new InputSlot(RIGHT, CROSS_X + 18, CROSS_Y));
        this.addSlot(new InputSlot(BOTTOM, CROSS_X, CROSS_Y + 18));

        this.addSlot(new Slot(this.resultContainer, 0, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack carried) {
                carried.onCraftedBy(taker, carried.getCount());
                WritingTableMenu.this.consumeInputs(taker);
                super.onTake(taker, carried);
            }
        });

        this.addStandardInventorySlots(inventory, 8, 84);
        this.updateResult();
    }

    /** Slot da mesa que avisa o menu quando muda (para recalcular a saída na hora). */
    private class InputSlot extends Slot {
        InputSlot(int index, int x, int y) {
            super(WritingTableMenu.this.inputs, index, x, y);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            WritingTableMenu.this.slotsChanged(this.container);
        }
    }

    public static boolean isChalk(ItemStack stack) {
        return stack.is(com.mistech.mistechmod.registry.MistechTags.CHALKS);
    }

    public static boolean isPaper(ItemStack stack) {
        return stack.is(Items.PAPER);
    }

    private WritingRecipeInput crossInput() {
        return new WritingRecipeInput(
                this.inputs.getItem(TOP), this.inputs.getItem(LEFT), this.inputs.getItem(RIGHT), this.inputs.getItem(BOTTOM));
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == this.inputs && this.updateResult()) {
            this.broadcastChanges();
        }
    }

    /** Recalcula a saída (só no servidor, onde as receitas existem). */
    private boolean updateResult() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return false;
        }
        for (int i = 0; i < INPUT_COUNT; i++) {
            this.lastSeen[i] = this.inputs.getItem(i).copy();
        }
        WritingRecipeInput input = this.crossInput();
        ItemStack result = ItemStack.EMPTY;
        this.currentRecipe = null;

        this.amberSeal = false;
        if (!input.isEmpty()) {
            // Selamento em Resina: runa + Grumo de Resina (receita especial, copia a runa)
            ItemStack amber = com.mistech.mistechmod.beyond.AmberSeal.sealResult(java.util.List.of(
                    this.inputs.getItem(TOP), this.inputs.getItem(LEFT), this.inputs.getItem(RIGHT), this.inputs.getItem(BOTTOM)));
            if (!amber.isEmpty()) {
                boolean hasPaper = isPaper(this.inputs.getItem(PAPER)) && this.inputs.getItem(PAPER).getCount() >= WritingRecipe.PAPER_COST;
                if (hasPaper && isChalk(this.inputs.getItem(CHALK))) {
                    this.amberSeal = true;
                    this.resultContainer.setItem(0, amber);
                    this.showPreview(amber);
                    return true;
                }
            }
            Optional<RecipeHolder<WritingRecipe>> found =
                    serverLevel.recipeAccess().getRecipeFor(MistechRecipes.WRITING_TYPE.get(), input, serverLevel);
            if (found.isPresent() && this.canAfford(found.get().value())) {
                this.currentRecipe = found.get().value();
                result = this.currentRecipe.assemble(input);
            }
        }
        this.resultContainer.setItem(0, result);
        this.showPreview(result);
        return true;
    }

    /** O item que vai sair flutua em cima da mesa enquanto ela está aberta. */
    private void showPreview(ItemStack result) {
        if (this.inputs instanceof com.mistech.mistechmod.block.WritingTableBlockEntity table) {
            table.setPreview(result);
        }
    }

    /** Pega mudanças feitas por fora do menu (funil, outro jogador com a mesa aberta...). */
    @Override
    public void broadcastChanges() {
        if (this.level instanceof ServerLevel) {
            for (int i = 0; i < INPUT_COUNT; i++) {
                if (!ItemStack.matches(this.lastSeen[i], this.inputs.getItem(i))) {
                    this.updateResult();
                    break;
                }
            }
        }
        super.broadcastChanges();
    }

    /** Precisa de 1 papel sempre, e de giz se a receita gastar giz. */
    private boolean canAfford(WritingRecipe recipe) {
        boolean hasPaper = isPaper(this.inputs.getItem(PAPER)) && this.inputs.getItem(PAPER).getCount() >= WritingRecipe.PAPER_COST;
        boolean hasChalk = recipe.chalkDamage() <= 0 || isChalk(this.inputs.getItem(CHALK));
        return hasPaper && hasChalk;
    }

    private void consumeInputs(Player taker) {
        WritingRecipe recipe = this.currentRecipe;
        if (recipe == null && !this.amberSeal) {
            return;
        }
        int chalkDamage = recipe != null ? recipe.chalkDamage() : com.mistech.mistechmod.beyond.AmberSeal.CHALK_COST;
        for (int slot = TOP; slot <= BOTTOM; slot++) {
            ItemStack stack = this.inputs.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            boolean waterBottle = stack.is(Items.POTION);
            net.minecraft.world.item.ItemStackTemplate remainder = stack.getItem().getCraftingRemainder(stack);
            this.inputs.removeItem(slot, 1);
            if (!this.level.isClientSide()) {
                // devolve o vidro da garrafa de água / o balde vazio dos baldes de água e lava
                ItemStack back = waterBottle ? new ItemStack(Items.GLASS_BOTTLE)
                        : remainder != null ? remainder.create() : ItemStack.EMPTY;
                if (!back.isEmpty() && !taker.getInventory().add(back)) {
                    taker.drop(back, false);
                }
            }
        }
        this.inputs.removeItem(PAPER, WritingRecipe.PAPER_COST);

        ItemStack chalk = this.inputs.getItem(CHALK);
        if (chalkDamage > 0 && !chalk.isEmpty() && this.level instanceof ServerLevel serverLevel) {
            // Sem passar o jogador: assim o giz gasta mesmo no modo criativo
            // (o Minecraft ignora dano de durabilidade de jogadores com "materiais infinitos").
            chalk.hurtAndBreak(chalkDamage, serverLevel, (net.minecraft.world.entity.LivingEntity) null, item ->
                    serverLevel.playSound(null, taker.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.6F, 1.4F));
            this.inputs.setChanged();
        }

        this.access.execute((lvl, pos) -> {
            long gameTime = lvl.getGameTime();
            if (this.lastSoundTime != gameTime) {
                lvl.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.lastSoundTime = gameTime;
            }
        });
        this.updateResult();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inputs.stillValid(player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultContainer && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return original;
        }
        ItemStack stack = slot.getItem();
        original = stack.copy();

        if (slotIndex == RESULT) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (slotIndex < INPUT_COUNT) {
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isChalk(stack) && !this.slots.get(CHALK).hasItem()) {
            if (!this.moveItemStackTo(stack, CHALK, CHALK + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isPaper(stack)) {
            if (!this.moveItemStackTo(stack, PAPER, PAPER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.moveItemStackTo(stack, TOP, BOTTOM + 1, false)) {
            // foi para a cruz
        } else if (slotIndex < INV_END) {
            if (!this.moveItemStackTo(stack, INV_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, INV_START, INV_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        }
        slot.setChanged();
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        if (slotIndex == RESULT) {
            player.drop(stack, false);
        }
        this.broadcastChanges();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.resultContainer.removeItemNoUpdate(0); // a saída é só uma prévia; os itens ficam na mesa
        if (!player.level().isClientSide()) {
            this.showPreview(ItemStack.EMPTY);
        }
    }
}
