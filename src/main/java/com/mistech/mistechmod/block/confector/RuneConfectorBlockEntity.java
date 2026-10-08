package com.mistech.mistechmod.block.confector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.FluidInteraction;
import com.mistech.mistechmod.block.FluidTank;
import com.mistech.mistechmod.menu.RuneConfectorMenu;
import com.mistech.mistechmod.menu.WritingTableMenu;
import com.mistech.mistechmod.network.ConfectorRecipesPayload;
import com.mistech.mistechmod.recipe.WritingRecipe;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechFluids;
import com.mistech.mistechmod.registry.MistechRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Parte principal do Confector de Runas (fica onde estava a Mesa de Escrita).
 *
 * Espaços: 0..26 ingredientes, 27 giz, 28..36 saída, 37..38 entrada de balde/garrafa para os tanques 0 e 1.
 * Dois tanques de 100 baldes: garrafa de água, baldes de água/lava e frasco/balde de essência nas receitas
 * podem ser tirados direto dos tanques.
 * Modo automático (botão de energia): com uma runa escolhida, cada pulso de redstone faz 1 runa.
 */
public class RuneConfectorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int STORAGE_SLOTS = 27;
    public static final int CHALK_SLOT = 27;
    public static final int OUTPUT_START = 28;
    public static final int OUTPUT_SLOTS = 9;
    public static final int FLUID_SLOT_0 = OUTPUT_START + OUTPUT_SLOTS; // 37
    public static final int FLUID_SLOT_1 = FLUID_SLOT_0 + 1;             // 38
    public static final int SIZE = FLUID_SLOT_1 + 1;                     // 39
    public static final int TANK_CAPACITY = 100_000;
    /** Quantas receitas cabem na máscara de "dá para fazer" sincronizada com a tela (4 x 16 bits). */
    public static final int MAX_RECIPES = 64;
    public static final int DATA_COUNT = 12;

    private static final Component DEFAULT_NAME = Component.translatable("container.mistech.rune_confector");
    private static final int[] SLOTS_UP_SIDES = buildInputSlots();
    private static final int[] SLOTS_DOWN = buildOutputSlots();

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final FluidTank[] tanks = {
            new FluidTank(TANK_CAPACITY, this::onTankChanged),
            new FluidTank(TANK_CAPACITY, this::onTankChanged)};
    private @Nullable ResourceKey<Recipe<?>> selected;
    private boolean autoMode;
    private boolean wasPowered;
    private DyeColor tableColor = DyeColor.PURPLE;
    /** Evita derrubar as peças duas vezes. */
    private boolean dropped;

    private long maskTime = -1;
    private final int[] mask = new int[4];

    /** Dados sincronizados com a tela (valores de até 16 bits cada). */
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int id) {
            RuneConfectorBlockEntity self = RuneConfectorBlockEntity.this;
            return switch (id) {
                case 0 -> self.selectedIndex() + 1;
                case 1 -> self.autoMode ? 1 : 0;
                case 2 -> self.tanks[0].getAmount() & 0xFFFF;
                case 3 -> self.tanks[0].getAmount() >>> 16;
                case 4 -> fluidId(self.tanks[0]);
                case 5 -> self.tanks[1].getAmount() & 0xFFFF;
                case 6 -> self.tanks[1].getAmount() >>> 16;
                case 7 -> fluidId(self.tanks[1]);
                case 8, 9, 10, 11 -> self.craftableMask()[id - 8];
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

    public RuneConfectorBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.RUNE_CONFECTOR.get(), pos, state);
    }

    private static int[] buildInputSlots() {
        int[] slots = new int[STORAGE_SLOTS + 1];
        for (int i = 0; i <= STORAGE_SLOTS; i++) {
            slots[i] = i; // ingredientes + giz
        }
        return slots;
    }

    private static int[] buildOutputSlots() {
        int[] slots = new int[OUTPUT_SLOTS];
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            slots[i] = OUTPUT_START + i;
        }
        return slots;
    }

    /** Registro numérico do fluido (+1; 0 = vazio), para a tela saber qual textura desenhar. */
    private static int fluidId(FluidTank tank) {
        return tank.isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid()) + 1;
    }

    public static Fluid fluidFromId(int id) {
        return id <= 0 ? Fluids.EMPTY : BuiltInRegistries.FLUID.byId(id - 1);
    }

    // ----- montagem -----

    /** Chamado ao montar: guarda os itens (o que passar de 27 cai no chão), o giz, os fluidos e a cor da mesa. */
    public void setup(List<ItemStack> stored, ItemStack chalk, FluidStack tank0, FluidStack tank1, DyeColor color, BlockPos dropPos) {
        int slot = 0;
        for (ItemStack stack : stored) {
            if (slot < STORAGE_SLOTS) {
                this.items.set(slot++, stack);
            } else if (this.level != null) {
                Block.popResource(this.level, dropPos.above(), stack);
            }
        }
        if (!chalk.isEmpty()) {
            if (WritingTableMenu.isChalk(chalk)) {
                this.items.set(CHALK_SLOT, chalk);
            } else if (this.level != null) {
                Block.popResource(this.level, dropPos.above(), chalk);
            }
        }
        this.tanks[0].setFluid(tank0);
        this.tanks[1].setFluid(tank1);
        this.tableColor = color;
        this.setChanged();
    }

    public FluidTank getTank(int index) {
        return this.tanks[index];
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return new CombinedResourceHandler<>(this.tanks[0], this.tanks[1]);
    }

    private void onTankChanged() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ----- tick -----

    public static void serverTick(Level level, BlockPos pos, BlockState state, RuneConfectorBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        be.pourFluidSlot(FLUID_SLOT_0, 0);
        be.pourFluidSlot(FLUID_SLOT_1, 1);

        // redstone em qualquer parte: cada pulso (borda de subida) faz 1 runa no modo automático
        boolean powered = false;
        Direction facing = state.getValue(RuneConfectorBlock.FACING);
        boolean mirrored = state.getValue(RuneConfectorBlock.MIRRORED);
        for (ConfectorPart part : ConfectorPart.values()) {
            if (level.hasNeighborSignal(RuneConfectorBlock.partPos(pos, facing, mirrored, part))) {
                powered = true;
                break;
            }
        }
        if (powered && !be.wasPowered && be.autoMode) {
            RecipeHolder<WritingRecipe> recipe = be.selectedRecipe(serverLevel);
            if (recipe != null) {
                be.craft(serverLevel, recipe.value(), 1);
            }
        }
        if (powered != be.wasPowered) {
            be.wasPowered = powered;
            be.setChanged();
        }
    }

    /** Garrafa/frasco/balde cheio no espaço de fluido: despeja no tanque e deixa o recipiente vazio no lugar. */
    private void pourFluidSlot(int slot, int tankIndex) {
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty() || stack.getCount() != 1) {
            return;
        }
        for (FluidEquivalent eq : FluidEquivalent.all()) {
            if (eq.matches(stack)) {
                FluidTank tank = this.tanks[tankIndex];
                if (tank.fill(eq.fluid(), eq.amount(), true) == eq.amount()) {
                    tank.fill(eq.fluid(), eq.amount(), false);
                    this.items.set(slot, eq.empty());
                    this.setChanged();
                }
                return;
            }
        }
    }

    // ----- receitas -----

    /** Todas as receitas da Mesa de Escrita, em ordem fixa (pelo id). */
    public static List<RecipeHolder<WritingRecipe>> recipes(ServerLevel level) {
        List<RecipeHolder<WritingRecipe>> list = new ArrayList<>(
                level.getServer().getRecipeManager().recipeMap().byType(MistechRecipes.WRITING_TYPE.get()));
        list.sort(Comparator.comparing(holder -> holder.id().identifier().toString()));
        if (list.size() > MAX_RECIPES) {
            return list.subList(0, MAX_RECIPES);
        }
        return list;
    }

    private int selectedIndex() {
        if (this.selected == null || !(this.level instanceof ServerLevel serverLevel)) {
            return -1;
        }
        List<RecipeHolder<WritingRecipe>> list = recipes(serverLevel);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(this.selected)) {
                return i;
            }
        }
        return -1;
    }

    private @Nullable RecipeHolder<WritingRecipe> selectedRecipe(ServerLevel level) {
        int index = this.selectedIndex();
        return index < 0 ? null : recipes(level).get(index);
    }

    private int[] craftableMask() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return this.mask;
        }
        long time = serverLevel.getGameTime();
        if (time != this.maskTime) {
            this.maskTime = time;
            java.util.Arrays.fill(this.mask, 0);
            List<RecipeHolder<WritingRecipe>> list = recipes(serverLevel);
            for (int i = 0; i < list.size(); i++) {
                if (this.plan(list.get(i).value()) != null) {
                    this.mask[i / 16] |= 1 << (i % 16);
                }
            }
        }
        return this.mask;
    }

    /** Botões da tela: escolher runa (clicar de novo faz 1; com shift, faz até acabar) e ligar/desligar. */
    public void handleButton(ServerPlayer player, int id) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (id == RuneConfectorMenu.BUTTON_POWER) {
            this.autoMode = !this.autoMode;
            serverLevel.playSound(null, this.worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, this.autoMode ? 0.8F : 0.6F);
            this.setChanged();
            return;
        }
        boolean many = id >= RuneConfectorMenu.BUTTON_MANY;
        int index = many ? id - RuneConfectorMenu.BUTTON_MANY : id;
        List<RecipeHolder<WritingRecipe>> list = recipes(serverLevel);
        if (index < 0 || index >= list.size()) {
            return;
        }
        RecipeHolder<WritingRecipe> recipe = list.get(index);
        if (!recipe.id().equals(this.selected)) {
            this.selected = recipe.id();
            this.setChanged();
            if (!many) {
                return; // primeiro clique só escolhe
            }
        }
        this.craft(serverLevel, recipe.value(), many ? 64 : 1);
    }

    /** O que tirar de onde para fazer uma runa (null = não dá). */
    private record Plan(int[] take, int[] drain, FluidResource[] drainFluid) {}

    private @Nullable Plan plan(WritingRecipe recipe) {
        int[] take = new int[STORAGE_SLOTS];
        int[] drain = new int[2];
        FluidResource[] drainFluid = new FluidResource[2];

        // 1 papel
        if (!this.reserve(take, stack -> stack.is(Items.PAPER))) {
            return null;
        }
        for (Ingredient ingredient : recipe.ingredients()) {
            if (this.reserve(take, ingredient)) {
                continue;
            }
            boolean found = false;
            for (FluidEquivalent eq : FluidEquivalent.all()) {
                if (!ingredient.test(eq.representative())) {
                    continue;
                }
                for (int t = 0; t < 2 && !found; t++) {
                    FluidTank tank = this.tanks[t];
                    if (tank.has(eq.fluid(), drain[t] + eq.amount())) {
                        drain[t] += eq.amount();
                        drainFluid[t] = eq.fluid();
                        found = true;
                    }
                }
                if (found) {
                    break;
                }
            }
            if (!found) {
                return null;
            }
        }
        if (recipe.chalkDamage() > 0 && !WritingTableMenu.isChalk(this.items.get(CHALK_SLOT))) {
            return null;
        }
        if (!this.fitsOutput(recipe.result().create())) {
            return null;
        }
        return new Plan(take, drain, drainFluid);
    }

    private boolean reserve(int[] take, java.util.function.Predicate<ItemStack> test) {
        for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty() && stack.getCount() - take[slot] >= 1 && test.test(stack)) {
                take[slot]++;
                return true;
            }
        }
        return false;
    }

    private boolean fitsOutput(ItemStack result) {
        int remaining = result.getCount();
        for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUT_SLOTS && remaining > 0; slot++) {
            ItemStack stack = this.items.get(slot);
            if (stack.isEmpty()) {
                remaining -= result.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(stack, result)) {
                remaining -= stack.getMaxStackSize() - stack.getCount();
            }
        }
        return remaining <= 0;
    }

    /** Guarda na saída (e depois nos ingredientes); o que não couber cai no chão. */
    private void insertOrDrop(ItemStack stack, int from, int to) {
        for (int slot = from; slot < to && !stack.isEmpty(); slot++) {
            ItemStack there = this.items.get(slot);
            if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, stack)) {
                int move = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
                there.grow(move);
                stack.shrink(move);
            }
        }
        for (int slot = from; slot < to && !stack.isEmpty(); slot++) {
            if (this.items.get(slot).isEmpty()) {
                this.items.set(slot, stack.copy());
                stack.setCount(0);
            }
        }
        if (!stack.isEmpty() && this.level != null) {
            Block.popResource(this.level, this.worldPosition.above(2), stack);
        }
    }

    /** Faz até {@code times} runas; devolve quantas fez. */
    public int craft(ServerLevel level, WritingRecipe recipe, int times) {
        int made = 0;
        for (int i = 0; i < times; i++) {
            Plan plan = this.plan(recipe);
            if (plan == null) {
                break;
            }
            for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
                for (int n = 0; n < plan.take()[slot]; n++) {
                    ItemStack used = this.items.get(slot).split(1);
                    ItemStack leftover = remainderOf(used);
                    if (!leftover.isEmpty()) {
                        this.insertOrDrop(leftover, 0, STORAGE_SLOTS); // garrafa/balde vazio volta para os ingredientes
                    }
                }
            }
            for (int t = 0; t < 2; t++) {
                if (plan.drain()[t] > 0) {
                    this.tanks[t].drain(plan.drainFluid()[t], plan.drain()[t], false);
                }
            }
            ItemStack chalk = this.items.get(CHALK_SLOT);
            if (recipe.chalkDamage() > 0 && !chalk.isEmpty()) {
                // sem jogador: gasta até no criativo, igual à Mesa de Escrita
                chalk.hurtAndBreak(recipe.chalkDamage(), level, (net.minecraft.world.entity.LivingEntity) null, item ->
                        level.playSound(null, this.worldPosition, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 0.6F, 1.4F));
            }
            this.insertOrDrop(recipe.result().create(), OUTPUT_START, OUTPUT_START + OUTPUT_SLOTS);
            made++;
        }
        if (made > 0) {
            level.playSound(null, this.worldPosition, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.1F);
            this.setChanged();
        }
        return made;
    }

    private static ItemStack remainderOf(ItemStack used) {
        if (FluidInteraction.isWaterBottle(used)) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        ItemStackTemplate remainder = used.getItem().getCraftingRemainder(used);
        return remainder != null ? remainder.create() : ItemStack.EMPTY;
    }

    /** Recipientes cheios que valem como fluido nas receitas (e que o espaço de fluido aceita). */
    public record FluidEquivalent(ItemStack representative, FluidResource fluid, int amount, ItemStack emptyTemplate) {
        public boolean matches(ItemStack stack) {
            return ItemStack.isSameItemSameComponents(stack, this.representative);
        }

        public ItemStack empty() {
            return this.emptyTemplate.copy();
        }

        public static List<FluidEquivalent> all() {
            FluidResource water = FluidResource.of(Fluids.WATER);
            FluidResource lava = FluidResource.of(Fluids.LAVA);
            FluidResource essence = FluidResource.of(MistechFluids.IMPURE_ESSENCE.get());
            return List.of(
                    new FluidEquivalent(PotionContents.createItemStack(Items.POTION, Potions.WATER), water, 250, new ItemStack(Items.GLASS_BOTTLE)),
                    new FluidEquivalent(new ItemStack(Items.WATER_BUCKET), water, 1000, new ItemStack(Items.BUCKET)),
                    new FluidEquivalent(new ItemStack(Items.LAVA_BUCKET), lava, 1000, new ItemStack(Items.BUCKET)),
                    new FluidEquivalent(new ItemStack(Mistech.IMPURE_ESSENCE_BOTTLE.get()), essence, 250, new ItemStack(Items.GLASS_BOTTLE)),
                    new FluidEquivalent(new ItemStack(Mistech.IMPURE_ESSENCE_BUCKET.get()), essence, 1000, new ItemStack(Items.BUCKET)));
        }

        public static boolean isFilled(ItemStack stack) {
            for (FluidEquivalent eq : all()) {
                if (eq.matches(stack)) {
                    return true;
                }
            }
            return false;
        }
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

    /** Regra de cada espaço (usada também pela tela no cliente). */
    public static boolean isValidFor(int slot, ItemStack stack) {
        if (slot == CHALK_SLOT) {
            return WritingTableMenu.isChalk(stack);
        }
        if (slot == FLUID_SLOT_0 || slot == FLUID_SLOT_1) {
            return FluidEquivalent.isFilled(stack);
        }
        return slot < STORAGE_SLOTS;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_DOWN : SLOTS_UP_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_START && slot < OUTPUT_START + OUTPUT_SLOTS;
    }

    /** 0..15 pela ocupação da saída. */
    public int getComparatorLevel() {
        float fullness = 0;
        for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUT_SLOTS; slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty()) {
                fullness += stack.getCount() / (float) stack.getMaxStackSize();
            }
        }
        fullness /= OUTPUT_SLOTS;
        return fullness <= 0 ? 0 : 1 + (int) (fullness * 14);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        if (inventory.player instanceof ServerPlayer player && this.level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayer(player, ConfectorRecipesPayload.of(recipes(serverLevel)));
        }
        return new RuneConfectorMenu(containerId, inventory, this, this.dataAccess);
    }

    /** Ao sumir (desmontado): derruba os itens guardados e devolve as peças (barril, mesa e tanques com fluido). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level == null || this.dropped) {
            return;
        }
        this.dropped = true;
        Containers.dropContents(this.level, pos, this);
        Block.popResource(this.level, pos, new ItemStack(Mistech.SPATIAL_BARREL_ITEM.get()));
        Block.popResource(this.level, pos, Mistech.writingTableStack(this.tableColor));
        for (FluidTank tank : this.tanks) {
            ItemStack tankItem = new ItemStack(Mistech.RUNIC_TANK_ITEM.get());
            if (!tank.isEmpty()) {
                tankItem.set(com.mistech.mistechmod.registry.MistechComponents.TANK_FLUID.get(),
                        net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(tank.getFluid()));
            }
            Block.popResource(this.level, pos, tankItem);
        }
    }

    // ----- salvar -----

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        input.child("Tank0").ifPresent(this.tanks[0]::deserialize);
        input.child("Tank1").ifPresent(this.tanks[1]::deserialize);
        this.selected = input.getString("Selected")
                .map(s -> ResourceKey.create(Registries.RECIPE, Identifier.parse(s))).orElse(null);
        this.autoMode = input.getBooleanOr("Auto", false);
        this.wasPowered = input.getBooleanOr("Powered", false);
        this.tableColor = DyeColor.byName(input.getStringOr("TableColor", "purple"), DyeColor.PURPLE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        this.tanks[0].serialize(output.child("Tank0"));
        this.tanks[1].serialize(output.child("Tank1"));
        if (this.selected != null) {
            output.putString("Selected", this.selected.identifier().toString());
        }
        output.putBoolean("Auto", this.autoMode);
        output.putBoolean("Powered", this.wasPowered);
        output.putString("TableColor", this.tableColor.getSerializedName());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries); // o cliente precisa dos tanques para desenhar o fluido
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
