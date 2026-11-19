package com.mistech.mistechmod.spellcraft.conjurer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Config;
import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlock;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.machine.MachineUpgrade;
import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.block.PurityFluids;
import com.mistech.mistechmod.spellcraft.Spell;
import com.mistech.mistechmod.spellcraft.SpellFx;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Conjurador Rúnico: lança sozinho a magia do Cristal de Feitiço que está no espaço 0, pagando FE e (tier 3 ou mais)
 * Essência — Purificada primeiro, Impura se faltar. A recarga é a da magia × {@code Config.conjurerCooldown()} e anda
 * mais rápido com Melhorias de Velocidade; Eficiência corta o FE; Alcance soma 2 blocos por melhoria.
 * <p>
 * Modos: automático (lança sempre que a recarga acaba e há o que fazer) ou pulso de redstone (1 lançamento por pulso).
 * Espaços: 0 = cristal, 1 = frascos/baldes de essência (enchem os tanques), 2..3 = recipientes vazios.
 * info0 = Essência Impura (mB), info1 = Essência Purificada (mB), info2 = modo | estado << 2, info3 = alarme (Olhar Vigilante).
 */
public class RunicConjurerBlockEntity extends MachineBlockEntity implements ArcaneMachineBlock.EssenceTankHolder {
    public static final int TANK = 4000;
    public static final int CAPACITY = 100_000;
    public static final int SLOT_CRYSTAL = 0, SLOT_CONTAINER = 1;
    public static final int IMPURE = 0, PURE = 1;

    /** Botões da tela: trocar o modo. */
    public static final int BUTTON_MODE = 1;
    public static final int MODE_AUTO = 0, MODE_PULSE = 1;

    /** Estados mostrados na tela e no Jade. */
    public static final int ST_NO_CRYSTAL = 0, ST_INCOMPATIBLE = 1, ST_RECHARGING = 2, ST_READY = 3, ST_NO_TARGET = 4,
            ST_NO_ENERGY = 5, ST_NO_ESSENCE = 6, ST_WAIT_PULSE = 7, ST_CAST = 8;
    public static final String[] STATUS_KEYS = {"no_crystal", "incompatible", "recharging", "ready", "no_target", "no_energy",
            "no_essence", "wait_pulse", "cast"};

    private final PurityFluids.Tanks tanks = new PurityFluids.Tanks(TANK, new FluidResource[] {PurityFluids.impure(), PurityFluids.pure()},
            new boolean[] {true, true}, new boolean[] {false, false}, this::setChanged);

    private int mode = MODE_AUTO;
    private int status = ST_NO_CRYSTAL;
    private boolean lastPowered;
    private boolean pulsePending;
    private int scanTimer;
    private int castFlash;
    private int alarm;
    private int alarmTicks;
    private long boostUntil;
    private boolean boostActive;
    private ItemStack syncedCrystal = ItemStack.EMPTY;
    /** Só no cliente: o cristal desenhado em cima do bloco. */
    private ItemStack clientCrystal = ItemStack.EMPTY;

    public RunicConjurerBlockEntity(BlockPos pos, BlockState state) {
        this(ConjurerContent.RUNIC_CONJURER_BE.get(), pos, state, ConjurerContent.LAYOUT);
    }

    /** Para o Conjurador Celeste (Fase 9). */
    protected RunicConjurerBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state,
            com.mistech.mistechmod.machine.MachineLayout layout) {
        super(type, pos, state, layout, 1000, 0);
    }

    /** Espaço do cristal que lança agora (o Conjurador Celeste alterna entre dois). */
    protected int crystalSlot() {
        return SLOT_CRYSTAL;
    }

    /** Chamado depois de cada lançamento (o Celeste troca de cristal). */
    protected void afterCast() {}

    /** Poder extra das magias lançadas (Celeste ×1,25). */
    protected double powerFactor() {
        return 1.0;
    }

    /** Recarga (Celeste ×0,75). */
    protected double cooldownFactor() {
        return 1.0;
    }

    public PurityFluids.Tanks getTanks() {
        return this.tanks;
    }

    public ItemStack crystal() {
        return this.level != null && this.level.isClientSide() ? this.clientCrystal : this.items.get(this.crystalSlot());
    }

    public static boolean isCrystal(ItemStack stack) {
        return stack.is(ConjurerContent.SPELL_CRYSTAL.get()) && SpellCrystalItem.data(stack) != null;
    }

    public static boolean isEssenceContainer(ItemStack stack) {
        return stack.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) || stack.is(Mistech.IMPURE_ESSENCE_BUCKET.get())
                || stack.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) || stack.is(PurityContent.PURIFIED_ESSENCE_BUCKET.get());
    }

    @Override
    protected boolean usesRange() {
        return true;
    }

    @Override
    protected boolean usesWorkStep() {
        return true;
    }

    // ------------------------------------------------------------------ custos

    /** FE de um lançamento (Eficiência e o config entram). */
    public int feCost(Spell spell) {
        double eff = Math.max(0.1, 1.0 - this.upgrade(MachineUpgrade.EFFICIENCY) * Config.efficiencyPerUpgrade());
        return (int) Math.ceil(spell.mana() * Config.conjurerFePerMana() * eff * Config.machineEnergy()
                * com.mistech.mistechmod.machine.socket.MachineSockets.energyFactor(this.sockets));
    }

    public static int essenceCost(Spell spell) {
        return spell.tier() >= 3 ? spell.tier() * Config.conjurerEssencePerTier() : 0;
    }

    private int essence() {
        return this.tanks.amount(IMPURE) + this.tanks.amount(PURE);
    }

    private void takeEssence(int amount) {
        int fromPure = Math.min(amount, this.tanks.amount(PURE));
        this.tanks.take(PURE, fromPure);
        this.tanks.take(IMPURE, amount - fromPure);
    }

    /** Energia que as magias podem usar além do custo do lançamento (Recarregar). */
    int energyForSpells() {
        return this.storedEnergy();
    }

    void useEnergyForSpells(int amount) {
        this.drainEnergy(amount);
    }

    // ------------------------------------------------------------------ Impulso Arcano e alarme

    void startMachineBoost(int ticks) {
        if (this.level == null) {
            return;
        }
        this.boostUntil = Math.max(this.boostUntil, this.level.getGameTime() + ticks);
        if (!this.boostActive) {
            com.mistech.mistechmod.machine.GnomeBoost.setActive(this.level, this.worldPosition, true);
            this.boostActive = true;
        }
    }

    private void stopMachineBoost() {
        if (this.boostActive && this.level != null) {
            com.mistech.mistechmod.machine.GnomeBoost.setActive(this.level, this.worldPosition, false);
        }
        this.boostActive = false;
    }

    void setAlarm(int signal) {
        this.alarmTicks = signal > 0 ? 200 : 0;
        if (signal != this.alarm) {
            this.alarm = signal;
            if (this.level != null) {
                this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
            }
        }
    }

    public int alarm() {
        return this.alarm;
    }

    @Override
    public void setRemoved() {
        this.stopMachineBoost();
        super.setRemoved();
    }

    // ------------------------------------------------------------------ tick

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        this.fillTanks();
        if (this.boostActive && level.getGameTime() >= this.boostUntil) {
            this.stopMachineBoost();
        }
        if (this.alarmTicks > 0 && --this.alarmTicks == 0) {
            this.setAlarm(0);
        }
        if (this.castFlash > 0) {
            this.castFlash--;
        }
        this.syncCrystalIfChanged();
        this.info0 = this.tanks.amount(IMPURE);
        this.info1 = this.tanks.amount(PURE);
        this.info3 = this.alarm;

        boolean powered = level.hasNeighborSignal(pos);
        if (powered && !this.lastPowered) {
            this.pulsePending = true;
        }
        this.lastPowered = powered;

        ItemStack crystalStack = this.items.get(this.crystalSlot());
        SpellCrystal data = SpellCrystalItem.data(crystalStack);
        Spell spell = data == null ? null : data.resolve();
        BlockCasts.Entry entry = spell == null ? null : BlockCasts.get(spell.id());
        if (spell == null || entry == null) {
            this.setStatus(spell == null ? ST_NO_CRYSTAL : ST_INCOMPATIBLE);
            this.progress = 0;
            this.maxProgress = 0;
            this.pulsePending = false;
            this.setLit(level, pos, state, false);
            return;
        }
        this.maxProgress = Math.max(2, (int) Math.round(spell.cooldown() * Config.conjurerCooldown() * this.cooldownFactor()) * 2);
        int fe = this.feCost(spell);
        this.setLit(level, pos, state, this.hasEnergy(fe) || this.castFlash > 0);

        if (this.progress < this.maxProgress) {
            this.progress = Math.min(this.maxProgress, this.progress + this.workStep());
            if (this.castFlash <= 0) {
                this.setStatus(ST_RECHARGING);
            }
            return;
        }
        if (this.mode == MODE_PULSE && !this.pulsePending) {
            this.setStatus(ST_WAIT_PULSE);
            return;
        }
        if (!this.hasEnergy(fe)) {
            this.setStatus(ST_NO_ENERGY);
            return;
        }
        int ess = essenceCost(spell);
        if (this.essence() < ess) {
            this.setStatus(ST_NO_ESSENCE);
            return;
        }
        if (this.status != ST_NO_TARGET) {
            this.setStatus(ST_READY);
        }
        // procurar alvo custa: só a cada 10 ticks (e logo depois de um pulso)
        if (--this.scanTimer > 0 && !this.pulsePending) {
            return;
        }
        this.scanTimer = 10;
        double power = data.power() * this.powerFactor() * (entry.mode() == BlockCasts.Mode.TURRET
                ? com.mistech.mistechmod.celestial.MachineSky.turret(level, this.ownerMask) : 1.0);
        BlockCasts.Ctx ctx = new BlockCasts.Ctx(level, pos, state.getValue(MachineBlock.FACING), power, this.caster(level, data),
                data.owner(), this.upgrade(MachineUpgrade.RANGE), this);
        boolean ok = entry.cast().cast(ctx);
        this.pulsePending = false;
        if (!ok) {
            this.setStatus(ST_NO_TARGET);
            return;
        }
        this.drainEnergy(Math.min(fe, this.storedEnergy()));
        this.takeEssence(ess);
        if (com.mistech.mistechmod.purity.area.AreaTaint.isImpure(spell.element())) {
            com.mistech.mistechmod.purity.area.AreaTaint.add(level, pos, (float) (com.mistech.mistechmod.Config.areaTaintConjurer() * this.taintFactor()));
        }
        this.progress = 0;
        this.castFlash = 20;
        this.afterCast();
        this.setStatus(ST_CAST);
        Vec3 o = ctx.origin();
        SpellFx.burst(level, o, spell.element(), 0.15, 14, SpellFx.glyph(spell.element()));
        level.sendParticles(ParticleTypes.ENCHANT, o.x, o.y, o.z, 12, 0.3, 0.3, 0.3, 0.6);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.6F, 1.4F);
        this.setChanged();
    }

    private void setStatus(int status) {
        this.status = status;
        this.info2 = this.mode | (status << 2);
    }

    /** Jogador falso com o nome do dono: o dano, o loot e a XP saem em nome dele. */
    private FakePlayer caster(ServerLevel level, SpellCrystal data) {
        UUID id = UUID.nameUUIDFromBytes(("mistech_conjurer:" + data.owner()).getBytes(StandardCharsets.UTF_8));
        String name = data.ownerName().isEmpty() ? "[Conjurador]" : data.ownerName();
        FakePlayer fake = FakePlayerFactory.get(level, new GameProfile(id, name.length() > 16 ? name.substring(0, 16) : name));
        Direction facing = this.getBlockState().getValue(MachineBlock.FACING);
        fake.snapTo(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, facing.toYRot(), 0.0F);
        return fake;
    }

    private void fillTanks() {
        ItemStack in = this.items.get(SLOT_CONTAINER);
        if (in.isEmpty()) {
            return;
        }
        boolean pure = in.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) || in.is(PurityContent.PURIFIED_ESSENCE_BUCKET.get());
        boolean bottle = in.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) || in.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get());
        int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        int tank = pure ? PURE : IMPURE;
        ItemStack empty = new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET);
        if (isEssenceContainer(in) && this.tanks.space(tank) >= amount && this.canOutput(empty)) {
            this.tanks.add(tank, amount);
            in.shrink(1);
            this.insertOutput(empty);
        }
    }

    // ------------------------------------------------------------------ cristal no cliente

    private void syncCrystalIfChanged() {
        ItemStack now = this.items.get(this.crystalSlot());
        if (!ItemStack.matches(now, this.syncedCrystal) && this.level != null) {
            this.syncedCrystal = now.copy();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.store("Crystal", ItemStack.OPTIONAL_CODEC, registries.createSerializationContext(NbtOps.INSTANCE), this.items.get(this.crystalSlot()).copy());
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        super.handleUpdateTag(input);
        this.clientCrystal = input.read("Crystal", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ tela, frascos na mão, Jade

    @Override
    public boolean onMenuButton(Player player, int id) {
        if (id == BUTTON_MODE) {
            this.mode = this.mode == MODE_AUTO ? MODE_PULSE : MODE_AUTO;
            this.pulsePending = false;
            this.setStatus(this.status);
            this.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean handles(ItemStack stack) {
        return isEssenceContainer(stack);
    }

    @Override
    public void interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean pure = held.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) || held.is(PurityContent.PURIFIED_ESSENCE_BUCKET.get());
        boolean bottle = held.is(Mistech.IMPURE_ESSENCE_BOTTLE.get()) || held.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get());
        int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        int tank = pure ? PURE : IMPURE;
        if (this.tanks.space(tank) < amount) {
            player.sendOverlayMessage(Component.translatable("info.mistech.tank",
                    Component.translatable(pure ? "block.mistech.purified_essence" : "block.mistech.impure_essence"), this.tanks.amount(tank), TANK));
            return;
        }
        this.tanks.add(tank, amount);
        PurityFluids.swap(player, hand, new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET));
        player.level().playSound(null, this.worldPosition, bottle ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void addInfo(List<Component> lines) {
        Spell spell = SpellCrystalItem.spell(this.items.get(this.crystalSlot()));
        if (spell != null) {
            lines.add(Component.translatable("info.mistech.runic_conjurer.spell", Component.translatable(spell.nameKey()).withColor(spell.element().color())));
        }
        lines.add(Component.translatable("gui.mistech.runic_conjurer.status." + STATUS_KEYS[Math.min(this.status, STATUS_KEYS.length - 1)]));
        lines.add(Component.translatable("info.mistech.tank", Component.translatable("block.mistech.impure_essence"), this.tanks.amount(IMPURE), TANK));
        lines.add(Component.translatable("info.mistech.tank", Component.translatable("block.mistech.purified_essence"), this.tanks.amount(PURE), TANK));
        super.addInfo(lines);
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.tanks.setAmount(IMPURE, input.getIntOr("Impure", 0));
        this.tanks.setAmount(PURE, input.getIntOr("Pure", 0));
        this.mode = Math.floorMod(input.getIntOr("Mode", MODE_AUTO), 2);
        this.lastPowered = input.getBooleanOr("LastPowered", false);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Impure", this.tanks.amount(IMPURE));
        output.putInt("Pure", this.tanks.amount(PURE));
        output.putInt("Mode", this.mode);
        output.putBoolean("LastPowered", this.lastPowered);
    }

    public @Nullable Spell clientSpell() {
        return SpellCrystalItem.spell(this.clientCrystal);
    }
}
