package com.mistech.mistechmod.block;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.menu.CoffeeMakerMenu;
import com.mistech.mistechmod.registry.MistechBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * Cafeteira Rúnica: a Chaleira avançada. Enche o tanque de água sozinha, guarda até 4 líquidos diferentes
 * (tanque 0 = água; 1 a 3 = outros) e tem 3 LINHAS que trabalham ao mesmo tempo, cada uma com a sua entrada e a sua saída:
 * <ul>
 * <li>linha 0, Café: Semente de Café (torra, mói e passa), Grão Torrado (mói e passa) ou Pó de Café (só passa);</li>
 * <li>linha 1, Beladona: Bagas de Beladona (Visão Mística);</li>
 * <li>linha 2, Cogumelo: Cogumelo Amarelo (Chá de Cogumelo).</li>
 * </ul>
 * Cada linha vai fazendo a bebida sozinha enquanto tem ingrediente e água, e guarda num reservatório (até
 * {@link #MAX_SERVINGS} doses; cada ingrediente rende até {@link #MAX_BATCH} doses de 125 mB de água). As Xícaras não
 * enchem sozinhas: você escolhe a bebida, clicando numa jarra com a Xícara na mão ou no botão "Encher" da tela.
 * Espaços: 0-2 ingredientes (um por linha), 3 Xícaras vazias, 4-6 xícaras cheias (uma saída por linha).
 * Funis e canos: ingredientes e Xícaras entram por qualquer lado; só as xícaras cheias saem.
 */
public class RunicCoffeeMakerBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int LINES = 3;
    public static final int SLOT_CUPS = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int SLOTS = 7;
    public static final int MAX_BATCH = 6;
    /** Doses que cada reservatório guarda (uma pilha de xícaras). */
    public static final int MAX_SERVINGS = 16;
    public static final int WATER_PER_CUP = 125;
    /** Água própria: 125 mB a cada 5 ticks (um balde em 2 s). */
    private static final int REFILL_INTERVAL = 5;

    /** Etapas de uma receita, em ticks. */
    private static final int ROAST_TICKS = 60;
    private static final int GRIND_TICKS = 40;
    private static final int BREW_TICKS = 100;

    /** Bebida de cada linha. */
    private static final KettleBlockEntity.Drink[] LINE_DRINK = {
            KettleBlockEntity.Drink.COFFEE, KettleBlockEntity.Drink.MYSTIC_SIGHT, KettleBlockEntity.Drink.MUSHROOM};

    /** O que um ingrediente vira e quanto cada etapa demora (0 = pula a etapa). */
    private record Brew(int line, KettleBlockEntity.Drink drink, int roast, int grind, int brew) {
        static @Nullable Brew of(ItemStack stack) {
            if (stack.isEmpty()) {
                return null;
            }
            if (stack.is(Mistech.COFFEE_SEEDS.get())) {
                return new Brew(0, KettleBlockEntity.Drink.COFFEE, ROAST_TICKS, GRIND_TICKS, BREW_TICKS);
            }
            if (stack.is(Mistech.ROASTED_COFFEE_BEANS.get())) {
                return new Brew(0, KettleBlockEntity.Drink.COFFEE, 0, GRIND_TICKS, BREW_TICKS);
            }
            KettleBlockEntity.Drink drink = KettleBlockEntity.Drink.byIngredient(stack);
            for (int line = 0; line < LINES; line++) {
                if (LINE_DRINK[line] == drink) {
                    return new Brew(line, drink, 0, 0, BREW_TICKS);
                }
            }
            return null;
        }
    }

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final MakerTanks tanks = new MakerTanks(this::setChanged);

    /** Receita em andamento em cada linha. */
    private final int[] pendingCups = new int[LINES];
    /** Doses prontas guardadas em cada reservatório. */
    private final int[] servings = new int[LINES];
    private final int[] roast = new int[LINES];
    private final int[] grind = new int[LINES];
    private final int[] brew = new int[LINES];
    private final int[] progress = new int[LINES];

    /** Sincroniza com a tela: 3 progressos (%), 4 fluidos (id), 4 quantidades (÷10) e 3 reservatórios (doses). */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            RunicCoffeeMakerBlockEntity maker = RunicCoffeeMakerBlockEntity.this;
            if (index < LINES) {
                int total = maker.total(index);
                return maker.pendingCups[index] > 0 && total > 0 ? Math.max(1, maker.progress[index] * 100 / total) : 0;
            }
            if (index < LINES + MakerTanks.TANKS) {
                return BuiltInRegistries.FLUID.getId(maker.tanks.getFluid(index - LINES).getFluid());
            }
            if (index < LINES + MakerTanks.TANKS * 2) {
                return maker.tanks.getAmountAsInt(index - LINES - MakerTanks.TANKS) / 10;
            }
            return maker.servings[index - LINES - MakerTanks.TANKS * 2];
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return LINES + MakerTanks.TANKS * 2 + LINES;
        }
    };

    public RunicCoffeeMakerBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.RUNIC_COFFEE_MAKER.get(), pos, state);
    }

    public MakerTanks getTanks() {
        return this.tanks;
    }

    public static boolean isIngredient(ItemStack stack) {
        return Brew.of(stack) != null;
    }

    /** Esse ingrediente vai na linha {@code line}? */
    public static boolean acceptsIngredient(int line, ItemStack stack) {
        Brew recipe = Brew.of(stack);
        return recipe != null && recipe.line() == line;
    }

    /** Linha do ingrediente (-1 se não é ingrediente). */
    public static int lineOf(ItemStack stack) {
        Brew recipe = Brew.of(stack);
        return recipe == null ? -1 : recipe.line();
    }

    private int total(int line) {
        return this.roast[line] + this.grind[line] + this.brew[line];
    }

    private boolean anyWorking() {
        for (int line = 0; line < LINES; line++) {
            if (this.pendingCups[line] > 0) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- tick

    public static void serverTick(Level level, BlockPos pos, BlockState state, RunicCoffeeMakerBlockEntity maker) {
        if (level.getGameTime() % REFILL_INTERVAL == 0) {
            maker.tanks.fill(FluidResource.of(Fluids.WATER), WATER_PER_CUP, false);
        }
        boolean sound = false;
        for (int line = 0; line < LINES; line++) {
            if (maker.pendingCups[line] > 0) {
                maker.progress[line]++;
                if (maker.progress[line] >= maker.total(line)) {
                    maker.finish(level, pos, line);
                } else if (!sound && maker.progress[line] % 20 == 0) {
                    sound = true;
                    int p = maker.progress[line];
                    boolean roasting = p < maker.roast[line];
                    boolean grinding = !roasting && p < maker.roast[line] + maker.grind[line];
                    level.playSound(null, pos, roasting ? SoundEvents.FIRE_AMBIENT
                            : grinding ? SoundEvents.GRINDSTONE_USE : SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                            SoundSource.BLOCKS, grinding ? 0.25F : 0.6F, grinding ? 1.6F : 1.2F);
                }
            } else if (level.getGameTime() % 10 == line * 3L) {
                maker.tryStart(level, pos, line);
            }
        }
    }

    private void tryStart(Level level, BlockPos pos, int line) {
        Brew recipe = Brew.of(this.items.get(line));
        if (recipe == null || recipe.line() != line) {
            return;
        }
        int room = MAX_SERVINGS - this.servings[line];
        int byWater = this.tanks.amountOf(FluidResource.of(Fluids.WATER)) / WATER_PER_CUP;
        int batch = Math.min(Math.min(MAX_BATCH, byWater), room);
        if (batch <= 0) {
            return;
        }
        this.tanks.drain(FluidResource.of(Fluids.WATER), batch * WATER_PER_CUP, false);
        this.items.get(line).shrink(1);
        this.pendingCups[line] = batch;
        this.roast[line] = recipe.roast();
        this.grind[line] = recipe.grind();
        this.brew[line] = recipe.brew();
        this.progress[line] = 0;
        this.updateWorking(level, pos);
        this.setChanged();
    }

    private void finish(Level level, BlockPos pos, int line) {
        this.servings[line] = Math.min(MAX_SERVINGS, this.servings[line] + this.pendingCups[line]);
        this.pendingCups[line] = 0;
        this.progress[line] = 0;
        level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.5F);
        this.updateWorking(level, pos);
        this.setChanged();
    }

    /**
     * Enche Xícaras do espaço 3 com a bebida da linha (cada uma gasta uma dose) e põe na saída da linha.
     * @param all enche quantas der; senão só uma
     * @return quantas encheu
     */
    public int fillCups(int line, boolean all) {
        ItemStack cups = this.items.get(SLOT_CUPS);
        if (line < 0 || line >= LINES || cups.isEmpty() || !cups.is(Mistech.CUP.get()) || this.servings[line] <= 0) {
            return 0;
        }
        ItemStack made = LINE_DRINK[line].cup();
        ItemStack out = this.items.get(SLOT_OUTPUT + line);
        int room;
        if (out.isEmpty()) {
            room = made.getMaxStackSize();
        } else if (ItemStack.isSameItemSameComponents(out, made)) {
            room = out.getMaxStackSize() - out.getCount();
        } else {
            return 0;
        }
        int count = Math.min(Math.min(room, cups.getCount()), this.servings[line]);
        if (!all) {
            count = Math.min(count, 1);
        }
        if (count <= 0) {
            return 0;
        }
        cups.shrink(count);
        this.servings[line] -= count;
        made.setCount(count);
        if (out.isEmpty()) {
            this.items.set(SLOT_OUTPUT + line, made);
        } else {
            out.grow(count);
        }
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7F, 1.2F);
            this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
        }
        this.setChanged();
        return count;
    }

    /** Qual jarra o clique pegou (0 a 2, da esquerda para a direita do modelo); -1 se não foi numa jarra. */
    private static int jarAt(BlockState state, BlockHitResult hit) {
        double wx = hit.getLocation().x - hit.getBlockPos().getX();
        double wz = hit.getLocation().z - hit.getBlockPos().getZ();
        double mx;
        switch (state.getValue(RunicCoffeeMakerBlock.FACING)) {
            case EAST -> mx = wz;
            case SOUTH -> mx = 1.0 - wx;
            case WEST -> mx = 1.0 - wz;
            default -> mx = wx;
        }
        double u = mx * 16.0;
        return u < 5.4 ? 0 : u < 10.1 ? 1 : 2;
    }

    private void updateWorking(Level level, BlockPos pos) {
        this.syncState();
    }

    /** Nível (0 a 4) do líquido da jarra: doses guardadas + em preparo, em relação ao máximo do reservatório. */
    private int jarLevel(int line) {
        int cups = this.servings[line] + this.pendingCups[line];
        return cups <= 0 ? 0 : Math.min(4, 1 + (cups * 4 - 1) / MAX_SERVINGS);
    }

    /** Põe no bloco se está trabalhando e quanto líquido há em cada jarra (o modelo muda sozinho). */
    private void syncState() {
        Level level = this.level;
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = this.getBlockState();
        if (!state.hasProperty(RunicCoffeeMakerBlock.WORKING)) {
            return;
        }
        BlockState next = state.setValue(RunicCoffeeMakerBlock.WORKING, this.anyWorking());
        for (int line = 0; line < LINES; line++) {
            next = next.setValue(RunicCoffeeMakerBlock.JARS[line], this.jarLevel(line));
        }
        if (next != state) {
            level.setBlock(this.worldPosition, next, 3);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.syncState();
    }

    // ---------------------------------------------------------------- interação

    /**
     * Só no servidor. Item na mão: ingrediente (vai para a linha dele), algo de fluido, ou Xícara: clicando numa jarra que
     * tem bebida enche a Xícara dessa bebida; senão guarda as Xícaras na cafeteira.
     */
    public InteractionResult useItem(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        int line = lineOf(held);
        if (line >= 0) {
            return this.insertInto(line, player, held) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (held.is(Mistech.CUP.get())) {
            int jar = jarAt(this.getBlockState(), hit);
            if (this.servings[jar] > 0) {
                ItemStack filled = LINE_DRINK[jar].cup();
                this.servings[jar]--;
                if (this.level != null) {
                    this.level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7F, 1.2F);
                }
                this.setChanged();
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled));
                return InteractionResult.SUCCESS;
            }
            return this.insertInto(SLOT_CUPS, player, held) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (FluidInteraction.isFluidItem(held)) {
            return this.fluidInteract(player, hand, held) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Só no servidor. Mão vazia abre a tela; agachado mostra o estado. */
    public void useEmpty(Player player) {
        if (player.isSecondaryUseActive()) {
            player.sendOverlayMessage(this.statusMessage());
        } else {
            player.openMenu(this);
        }
    }

    private boolean insertInto(int slot, Player player, ItemStack held) {
        ItemStack present = this.items.get(slot);
        if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, held)) {
            return false;
        }
        int room = held.getMaxStackSize() - present.getCount();
        int moved = Math.min(room, held.getCount());
        if (moved <= 0) {
            return false;
        }
        if (present.isEmpty()) {
            this.items.set(slot, held.copyWithCount(moved));
        } else {
            present.grow(moved);
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(moved);
        }
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.2F);
        }
        this.setChanged();
        return true;
    }

    private boolean fluidInteract(Player player, InteractionHand hand, ItemStack held) {
        Level level = this.level;
        if (level == null) {
            return false;
        }
        FluidResource water = FluidResource.of(Fluids.WATER);
        FluidResource essence = FluidResource.of(com.mistech.mistechmod.registry.MistechFluids.IMPURE_ESSENCE.get());
        FluidResource pure = FluidResource.of(com.mistech.mistechmod.registry.MistechFluids.PURIFIED_ESSENCE.get());
        int bottle = FluidInteraction.BOTTLE_AMOUNT;

        // garrafa de água / frasco de essência -> cafeteira
        FluidResource bottleFluid = FluidInteraction.isWaterBottle(held) ? water
                : held.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) ? essence
                : held.is(com.mistech.mistechmod.purity.PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) ? pure : null;
        if (bottleFluid != null) {
            if (this.tanks.fill(bottleFluid, bottle, true) < bottle) {
                return false;
            }
            this.tanks.fill(bottleFluid, bottle, false);
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.GLASS_BOTTLE)));
            level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }

        // garrafa vazia <- cafeteira
        if (held.is(Items.GLASS_BOTTLE)) {
            ItemStack filled;
            FluidResource from;
            if (this.tanks.has(water, bottle)) {
                from = water;
                filled = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            } else if (this.tanks.has(essence, bottle)) {
                from = essence;
                filled = new ItemStack(Mistech.IMPURE_ESSENCE_BOTTLE.get());
            } else if (this.tanks.has(pure, bottle)) {
                from = pure;
                filled = new ItemStack(com.mistech.mistechmod.purity.PurityContent.PURIFIED_ESSENCE_BOTTLE.get());
            } else {
                return false;
            }
            this.tanks.drain(from, bottle, false);
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled));
            level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }

        // baldes (e qualquer item com capability de fluido)
        return FluidUtil.interactWithFluidHandler(player, hand, this.worldPosition, this.tanks, null);
    }

    public Component statusMessage() {
        MutableComponent text = Component.empty();
        boolean first = true;
        for (int i = 0; i < MakerTanks.TANKS; i++) {
            if (this.tanks.getFluid(i).isEmpty()) {
                continue;
            }
            if (!first) {
                text.append(Component.literal(" | "));
            }
            first = false;
            text.append(Component.translatable("message.mistech.coffee_maker.tank", this.tanks.getFluid(i).getHoverName(),
                    String.format(java.util.Locale.ROOT, "%.1f", this.tanks.getAmountAsInt(i) / 1000.0)));
        }
        if (first) {
            text.append(Component.translatable("message.mistech.coffee_maker.no_fluid"));
        }
        text.append(Component.literal("\n"));
        for (int line = 0; line < LINES; line++) {
            if (line > 0) {
                text.append(Component.literal(" | "));
            }
            text.append(Component.translatable("message.mistech.coffee_maker.line",
                    Component.translatable("gui.mistech.coffee_maker.line." + line), this.servings[line], MAX_SERVINGS));
        }
        return text;
    }

    // ---------------------------------------------------------------- tela

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mistech.runic_coffee_maker");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CoffeeMakerMenu(containerId, inventory, this, this.dataAccess);
    }

    // ---------------------------------------------------------------- dados

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(input, this.items);
        this.tanks.deserialize(input.childOrEmpty("Tanks"));
        for (int line = 0; line < LINES; line++) {
            this.pendingCups[line] = input.getIntOr("Pending" + line, 0);
            this.roast[line] = input.getIntOr("Roast" + line, 0);
            this.grind[line] = input.getIntOr("Grind" + line, 0);
            this.brew[line] = input.getIntOr("Brew" + line, 0);
            this.progress[line] = input.getIntOr("Progress" + line, 0);
            this.servings[line] = input.getIntOr("Servings" + line, 0);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        this.tanks.serialize(output.child("Tanks"));
        for (int line = 0; line < LINES; line++) {
            output.putInt("Pending" + line, this.pendingCups[line]);
            output.putInt("Roast" + line, this.roast[line]);
            output.putInt("Grind" + line, this.grind[line]);
            output.putInt("Brew" + line, this.brew[line]);
            output.putInt("Progress" + line, this.progress[line]);
            output.putInt("Servings" + line, this.servings[line]);
        }
    }

    // ---------------------------------------------------------------- Container (funis, canos e a tela)

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[] {0, 1, 2, SLOT_CUPS, 4, 5, 6};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < LINES) {
            return acceptsIngredient(slot, stack);
        }
        return slot == SLOT_CUPS && stack.is(Mistech.CUP.get());
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, count);
        if (!removed.isEmpty()) {
            this.setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    /** Sinal do comparador: bebidas prontas somadas (máx. 15). */
    public int comparatorSignal() {
        int total = 0;
        for (int line = 0; line < LINES; line++) {
            total += this.items.get(SLOT_OUTPUT + line).getCount();
        }
        return Math.min(15, total);
    }
}
