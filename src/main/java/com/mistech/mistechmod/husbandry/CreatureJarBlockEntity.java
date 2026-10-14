package com.mistech.mistechmod.husbandry;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.machine.GnomeBoost;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Pote com Criatura: guarda o bicho (como {@link TypedEntityData}) e vai juntando o que ele produz.
 * Container de 2 espaços: 0 = produtos (sai por baixo, até 1 pack), 1 = baldes vazios (vaca; entram por cima/lados).
 * A vaca enche leite (até {@link JarCreatures#MAX_MILK} baldes): com balde no espaço 1, vira balde de leite no 0.
 * Gnomo Engarrafado alimentado por perto = +50%.
 */
public class CreatureJarBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int OUTPUT = 0;
    public static final int BUCKETS = 1;
    private static final int[] SLOTS_DOWN = {OUTPUT};
    private static final int[] SLOTS_OTHER = {BUCKETS};

    private @Nullable TypedEntityData<EntityType<?>> creature;
    private ItemStack output = ItemStack.EMPTY;
    private ItemStack buckets = ItemStack.EMPTY;
    private int progress;
    private int cycle;
    private int milk;

    /** Cópia do bicho para gerar produtos (servidor) ou desenhar (cliente). Não fica no mundo. */
    private @Nullable Entity cached;

    public CreatureJarBlockEntity(BlockPos pos, BlockState state) {
        super(HusbandryContent.CREATURE_JAR_BE.get(), pos, state);
    }

    public @Nullable TypedEntityData<EntityType<?>> getCreature() {
        return this.creature;
    }

    public void setCreature(@Nullable TypedEntityData<EntityType<?>> creature) {
        this.creature = creature;
        this.cached = null;
        this.progress = 0;
        this.cycle = 0;
        this.sync();
    }

    public JarCreatures.@Nullable Kind kind() {
        return this.creature == null ? null : JarCreatures.kindOf(this.creature.type());
    }

    /** O bicho (recriado do NBT na primeira vez). */
    public @Nullable Entity creatureEntity() {
        if (this.cached == null && this.creature != null && this.level != null) {
            this.cached = JarCreatures.recreate(this.creature, this.level, EntitySpawnReason.LOAD);
            if (this.cached != null) {
                BlockPos pos = this.worldPosition;
                this.cached.snapTo(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 0.0F, 0.0F);
            }
        }
        return this.cached;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CreatureJarBlockEntity jar) {
        JarCreatures.Kind kind = jar.kind();
        if (kind == null || !(level instanceof ServerLevel server)) {
            return;
        }
        if (kind == JarCreatures.Kind.COW) {
            jar.fillBuckets();
        }
        if (!jar.hasRoom(kind)) {
            return;
        }
        if (jar.cycle <= 0) {
            jar.cycle = kind.nextCycle(level.getRandom());
        }
        jar.progress += GnomeBoost.extraStep(level, pos) ? 2 : 1;
        if (jar.progress < jar.cycle) {
            return;
        }
        jar.progress = 0;
        jar.cycle = kind.nextCycle(level.getRandom());
        if (kind == JarCreatures.Kind.COW) {
            jar.milk++;
            server.playSound(null, pos, SoundEvents.COW_MILK, SoundSource.BLOCKS, 0.4F, 1.3F);
            jar.fillBuckets();
            jar.sync();
            return;
        }
        ItemStack made = JarCreatures.produce(kind, jar.creatureEntity(), server);
        if (made.isEmpty()) {
            return;
        }
        if (jar.output.isEmpty()) {
            jar.output = made;
        } else if (ItemStack.isSameItemSameComponents(jar.output, made)) {
            jar.output.grow(Math.min(made.getCount(), jar.output.getMaxStackSize() - jar.output.getCount()));
        }
        server.playSound(null, pos, kind == JarCreatures.Kind.CHICKEN ? SoundEvents.CHICKEN_EGG : SoundEvents.ITEM_PICKUP,
                SoundSource.BLOCKS, 0.5F, 1.2F);
        jar.sync();
    }

    private boolean hasRoom(JarCreatures.Kind kind) {
        if (kind == JarCreatures.Kind.COW) {
            return this.milk < JarCreatures.MAX_MILK;
        }
        return this.output.isEmpty() || this.output.getCount() < this.output.getMaxStackSize();
    }

    /** Vaca: baldes vazios no espaço 1 viram leite no espaço 0 (1 por vez, balde de leite não empilha). */
    private void fillBuckets() {
        if (this.milk > 0 && !this.buckets.isEmpty() && this.output.isEmpty()) {
            this.buckets.shrink(1);
            this.output = new ItemStack(Items.MILK_BUCKET);
            this.milk--;
            this.sync();
        }
    }

    /** Balde vazio na mão: tira 1 balde de leite. */
    public boolean milkInto() {
        if (this.kind() != JarCreatures.Kind.COW || this.milk <= 0) {
            return false;
        }
        this.milk--;
        this.sync();
        return true;
    }

    public ItemStack takeOutput() {
        ItemStack out = this.output;
        this.output = ItemStack.EMPTY;
        this.sync();
        return out;
    }

    public Component statusMessage() {
        JarCreatures.Kind kind = this.kind();
        if (kind == null || this.creature == null) {
            return Component.empty();
        }
        Component name = this.creature.type().getDescription();
        if (kind == JarCreatures.Kind.COW) {
            return Component.translatable("message.mistech.creature_jar.milk", name, this.milk, JarCreatures.MAX_MILK);
        }
        if (!this.hasRoom(kind)) {
            return Component.translatable("message.mistech.creature_jar.full", name);
        }
        int seconds = Math.max(1, (Math.max(this.cycle, kind.baseTicks) - this.progress) / 20);
        return Component.translatable("message.mistech.creature_jar.status", name, (seconds + 59) / 60);
    }

    public int comparatorSignal() {
        if (this.kind() == JarCreatures.Kind.COW) {
            return this.milk <= 0 ? 0 : Math.round(15.0F * this.milk / JarCreatures.MAX_MILK);
        }
        return this.output.isEmpty() ? 0 : 1 + Math.round(14.0F * this.output.getCount() / this.output.getMaxStackSize());
    }

    private void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ----- o bicho acompanha o item (loot table: copy_components mistech:jar_creature) -----

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.creature = components.get(MistechComponents.JAR_CREATURE.get());
        this.cached = null;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.creature != null) {
            components.set(MistechComponents.JAR_CREATURE.get(), this.creature);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        TypedEntityData<EntityType<?>> loaded = input.read("Creature", MistechComponents.JAR_CREATURE_CODEC).orElse(null);
        if (loaded == null || !loaded.equals(this.creature)) {
            this.cached = null;
        }
        this.creature = loaded;
        this.output = input.read("Output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.buckets = input.read("Buckets", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.progress = input.getIntOr("Progress", 0);
        this.cycle = input.getIntOr("Cycle", 0);
        this.milk = input.getIntOr("Milk", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.creature != null) {
            output.store("Creature", MistechComponents.JAR_CREATURE_CODEC, this.creature);
        }
        if (!this.output.isEmpty()) {
            output.store("Output", ItemStack.OPTIONAL_CODEC, this.output);
        }
        if (!this.buckets.isEmpty()) {
            output.store("Buckets", ItemStack.OPTIONAL_CODEC, this.buckets);
        }
        output.putInt("Progress", this.progress);
        output.putInt("Cycle", this.cycle);
        output.putInt("Milk", this.milk);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("Creature");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ----- Container: 0 = produtos, 1 = baldes vazios -----

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_DOWN : SLOTS_OTHER;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == BUCKETS && this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return this.output.isEmpty() && this.buckets.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == OUTPUT ? this.output : this.buckets;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack source = this.getItem(slot);
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = source.split(count);
        this.sync();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = this.getItem(slot);
        if (slot == OUTPUT) {
            this.output = ItemStack.EMPTY;
        } else {
            this.buckets = ItemStack.EMPTY;
        }
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == OUTPUT) {
            this.output = stack;
        } else {
            this.buckets = stack;
        }
        this.sync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == BUCKETS && stack.is(Items.BUCKET) && this.kind() == JarCreatures.Kind.COW;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.output = ItemStack.EMPTY;
        this.buckets = ItemStack.EMPTY;
        this.sync();
    }
}
