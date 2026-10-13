package com.mistech.mistechmod.infuser;

import java.util.List;

import com.mistech.mistechmod.infuser.InfuserRules.Cost;
import com.mistech.mistechmod.infuser.InfuserRules.Option;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/** Infusor Arcano: guarda o item, os materiais e a energia; o botão da tela escolhe encanto e nível (id = encanto × 8 + nível). */
public class ArcaneInfuserBlockEntity extends BaseContainerBlockEntity {
    public static final int CAPACITY = 100_000;
    public static final int DATA_COUNT = 3;

    private NonNullList<ItemStack> items = NonNullList.withSize(InfuserRules.SIZE, ItemStack.EMPTY);
    private final SimpleEnergyHandler energy = new SimpleEnergyHandler(CAPACITY, 2_000, 0) {
        @Override
        protected void onEnergyChanged(int previousAmount) {
            ArcaneInfuserBlockEntity.this.setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            int e = ArcaneInfuserBlockEntity.this.energy.getAmountAsInt();
            return switch (id) {
                case 0 -> e & 0x7FFF;
                case 1 -> (e >> 15) & 0x7FFF;
                case 2 -> ArcaneInfuserBlockEntity.this.tier();
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ArcaneInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(InfuserContent.ARCANE_INFUSER_BE.get(), pos, state);
    }

    public int tier() {
        return this.getBlockState().getBlock() instanceof ArcaneInfuserBlock block ? block.tier() : 1;
    }

    public EnergyHandler getEnergyHandler() {
        return this.energy;
    }

    /** Tenta infundir; manda a mensagem do problema ao jogador quando não dá. */
    public void infuse(Player player, int buttonId) {
        if (!(this.level instanceof ServerLevel level)) {
            return;
        }
        int rawId = buttonId / 8, wanted = buttonId % 8;
        ItemStack target = this.items.get(InfuserRules.SLOT_TARGET);
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<Option> options = InfuserRules.options(registry, this.tier(), target);
        Option option = options.stream().filter(o -> o.rawId() == rawId).findFirst().orElse(null);
        if (option == null || wanted <= option.current() || wanted > option.max()) {
            return;
        }
        Holder<Enchantment> enchantment = option.enchantment();
        Cost cost = InfuserRules.cost(enchantment, wanted);
        String problem = this.problem(player, cost);
        if (problem != null) {
            player.sendOverlayMessage(Component.translatable(problem).withStyle(ChatFormatting.RED));
            return;
        }
        // paga
        if (!player.getAbilities().instabuild) {
            player.giveExperienceLevels(-cost.xp());
        }
        this.energy.set(this.energy.getAmountAsInt() - cost.energy());
        this.items.get(InfuserRules.SLOT_LAPIS).shrink(cost.lapis());
        this.items.get(InfuserRules.SLOT_ESSENCE).shrink(cost.essence());
        this.items.get(InfuserRules.SLOT_STAR).shrink(cost.star());
        this.items.get(InfuserRules.SLOT_PURIFIED).shrink(cost.purified());
        int bottles = cost.essence() + cost.purified();
        if (bottles > 0) {
            player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE, bottles));
        }
        // aplica
        ItemStack result = target;
        if (target.is(Items.BOOK)) {
            result = new ItemStack(Items.ENCHANTED_BOOK);
            result.set(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
        EnchantmentHelper.updateEnchantments(result, mutable -> mutable.set(enchantment, wanted));
        this.items.set(InfuserRules.SLOT_TARGET, result);
        this.setChanged();
        BlockPos pos = this.worldPosition;
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        if (cost.star() > 0) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        }
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.6);
        level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.02);
    }

    /** Chave de tradução do que falta (null = pode). */
    public String problem(Player player, Cost cost) {
        if (!player.getAbilities().instabuild && player.experienceLevel < cost.xp()) {
            return "gui.mistech.arcane_infuser.need_xp";
        }
        if (this.energy.getAmountAsInt() < cost.energy()) {
            return "gui.mistech.arcane_infuser.need_energy";
        }
        if (this.items.get(InfuserRules.SLOT_LAPIS).getCount() < cost.lapis()) {
            return "gui.mistech.arcane_infuser.need_lapis";
        }
        if (this.items.get(InfuserRules.SLOT_ESSENCE).getCount() < cost.essence()) {
            return "gui.mistech.arcane_infuser.need_essence";
        }
        if (this.items.get(InfuserRules.SLOT_STAR).getCount() < cost.star()) {
            return "gui.mistech.arcane_infuser.need_star";
        }
        if (this.items.get(InfuserRules.SLOT_PURIFIED).getCount() < cost.purified()) {
            return "gui.mistech.arcane_infuser.need_purified";
        }
        return null;
    }

    // ----- Container -----

    @Override
    protected Component getDefaultName() {
        return this.getBlockState().getBlock().getName();
    }

    @Override
    public int getContainerSize() {
        return InfuserRules.SIZE;
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
        return InfuserRules.validFor(slot, stack);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(InfuserRules.SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.energy.deserialize(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        this.energy.serialize(output);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ArcaneInfuserMenu(containerId, inventory, this, this.data);
    }
}
