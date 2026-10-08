package com.mistech.mistechmod.block;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.IncenseItem;
import com.mistech.mistechmod.item.IncenseVeil;
import com.mistech.mistechmod.menu.IncenseHolderMenu;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Slots:
 *  0 = isqueiro (flint and steel)
 *  1 = estoque de incensos (até 64)
 *  2 = incenso ativo (queimando)
 *  3 = cinzas (25% de chance quando um incenso acaba)
 */
public class IncenseHolderBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_LIGHTER = 0;
    public static final int SLOT_STOCK = 1;
    public static final int SLOT_ACTIVE = 2;
    public static final int SLOT_ASH = 3;
    public static final int SLOT_COUNT = 4;

    public static final double EFFECT_RADIUS = 10.0;
    /** Maior raio possível (Incenso Mágico: 4×). */
    public static final double MAX_EFFECT_RADIUS = EFFECT_RADIUS * 4;
    public static final float ASH_CHANCE = 0.25F;

    private static final int[] SLOTS_FOR_UP = {SLOT_STOCK};
    private static final int[] SLOTS_FOR_SIDES = {SLOT_LIGHTER, SLOT_STOCK};
    private static final int[] SLOTS_FOR_DOWN = {SLOT_ASH};
    private static final Component DEFAULT_NAME = Component.translatable("container.mistech.incense_holder");

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    /** Dados sincronizados com a tela: 0 = segundos restantes do incenso ativo. */
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int id) {
            ItemStack active = IncenseHolderBlockEntity.this.items.get(SLOT_ACTIVE);
            if (active.isEmpty()) {
                return 0;
            }
            return id == 0 ? IncenseItem.getRemainingSeconds(active) : IncenseItem.maxBurnSeconds(active);
        }

        @Override
        public void set(int id, int value) {
            // somente leitura: o valor vem do item ativo
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public IncenseHolderBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.INCENSE_HOLDER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IncenseHolderBlockEntity holder) {
        ItemStack active = holder.items.get(SLOT_ACTIVE);

        // 1) Sem incenso ativo: o isqueiro acende o próximo do estoque
        if (active.isEmpty()) {
            ItemStack stock = holder.items.get(SLOT_STOCK);
            ItemStack lighter = holder.items.get(SLOT_LIGHTER);
            if (!stock.isEmpty() && isLighter(lighter) && level instanceof ServerLevel serverLevel) {
                ItemStack lit = stock.split(1);
                IncenseItem.setLit(lit);
                holder.items.set(SLOT_ACTIVE, lit);
                lighter.hurtAndBreak(1, serverLevel, (LivingEntity) null, item -> {});
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                        level.getRandom().nextFloat() * 0.4F + 0.8F);
                active = lit;
                setChanged(level, pos, state);
            }
        }

        // 2) Incenso queimando: 1x por segundo aplica o efeito e gasta 1 segundo
        if (!active.isEmpty() && level.getGameTime() % 20 == 0 && level instanceof ServerLevel serverLevel) {
            double radius = EFFECT_RADIUS * IncenseItem.rangeMultiplier(active);
            AABB area = new AABB(pos).inflate(radius);
            double radiusSqr = radius * radius;
            for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e.isAlive() && e.distanceToSqr(pos.getCenter()) <= radiusSqr)) {
                IncenseVeil.apply(serverLevel, entity);
            }

            // Incenso de Arruda: limpa a Mácula de quem está na fumaça e, devagar, o Solo Maculado
            if (com.mistech.mistechmod.purity.item.RueIncenseItem.isRue(active)) {
                for (net.minecraft.world.entity.player.Player player : serverLevel.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, area,
                        p -> p.distanceToSqr(pos.getCenter()) <= radiusSqr)) {
                    com.mistech.mistechmod.purity.Taint.cleanse(player, com.mistech.mistechmod.purity.item.RueIncenseItem.CLEANSE_PER_SECOND);
                }
                if (level.getRandom().nextInt(4) == 0) {
                    com.mistech.mistechmod.purity.block.TaintedSoilBlock.cleanseNear(level, pos, (int) radius, 1);
                }
            }

            // Incenso de Olho-florido: os hostis da área brilham
            if (com.mistech.mistechmod.beyond.EyeblossomIncenseItem.isEyeblossom(active)) {
                com.mistech.mistechmod.beyond.EyeblossomIncenseItem.reveal(serverLevel, pos.getCenter(), radius);
            }
            // Incensos de flor (Rosa do Wither, Tocha-flor, Lírio-do-vale)
            com.mistech.mistechmod.beyond.FlowerIncenseItem.applyAny(active, serverLevel, pos.getCenter(), radius);
            // Incenso de Fada: Véu Feérico (duendes ignoram)
            com.mistech.mistechmod.fey.FairyIncenseItem.applyAny(active, serverLevel, pos.getCenter(), radius);

            boolean magic = IncenseItem.makesResidue(active);
            if (IncenseItem.burnOneSecond(active, () -> holder.addAsh(level, pos, Mistech.MAGIC_RESIDUE.get()))) {
                holder.items.set(SLOT_ACTIVE, ItemStack.EMPTY);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.8F);
                // o incenso mágico já deixa Resíduo Mágico; cinzas comuns só do incenso comum
                if (!magic && level.getRandom().nextFloat() < ASH_CHANCE) {
                    holder.addAsh(level, pos, Mistech.INCENSE_ASH.get());
                }
            }
            setChanged(level, pos, state);
        }

        // 3) Mantém o blockstate LIT em dia (modelo com o incenso + fumaça)
        ItemStack burning = holder.items.get(SLOT_ACTIVE);
        boolean lit = !burning.isEmpty();
        boolean magic = lit && IncenseItem.makesResidue(burning);
        if (state.getValue(IncenseHolderBlock.LIT) != lit || state.getValue(IncenseHolderBlock.MAGIC) != magic) {
            level.setBlock(pos, state.setValue(IncenseHolderBlock.LIT, lit).setValue(IncenseHolderBlock.MAGIC, magic), Block.UPDATE_ALL);
        }
    }

    /** Põe cinzas ou Resíduo Mágico no espaço de cinzas (se estiver cheio ou com outro item, solta em cima do bloco). */
    private void addAsh(Level level, BlockPos pos, net.minecraft.world.item.Item item) {
        ItemStack ash = this.items.get(SLOT_ASH);
        if (ash.isEmpty()) {
            this.items.set(SLOT_ASH, new ItemStack(item));
        } else if (ash.is(item) && ash.getCount() < ash.getMaxStackSize()) {
            ash.grow(1);
        } else {
            Block.popResource(level, pos.above(), new ItemStack(item));
        }
    }

    /** Raio do efeito do incenso que está queimando agora (0 se nenhum). */
    public double getEffectRadius() {
        ItemStack active = this.items.get(SLOT_ACTIVE);
        return active.isEmpty() ? 0 : EFFECT_RADIUS * IncenseItem.rangeMultiplier(active);
    }

    public static boolean isLighter(ItemStack stack) {
        return IncenseItem.isLighter(stack);
    }

    public static boolean isStockIncense(ItemStack stack) {
        return IncenseItem.isIncense(stack) && !IncenseItem.isLit(stack);
    }

    // ----- Container -----

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
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
        return switch (slot) {
            case SLOT_LIGHTER -> isLighter(stack);
            case SLOT_STOCK -> isStockIncense(stack);
            default -> false; // ativo e cinzas só o próprio bloco preenche
        };
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return switch (direction) {
            case UP -> SLOTS_FOR_UP;
            case DOWN -> SLOTS_FOR_DOWN;
            default -> SLOTS_FOR_SIDES;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == SLOT_ASH;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new IncenseHolderMenu(containerId, inventory, this, this.dataAccess);
    }
}
