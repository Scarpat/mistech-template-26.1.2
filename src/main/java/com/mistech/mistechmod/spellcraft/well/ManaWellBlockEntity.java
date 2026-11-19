package com.mistech.mistechmod.spellcraft.well;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mistech.mistechmod.Config;
import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.block.PurityFluids;
import com.mistech.mistechmod.spellcraft.ManaService;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Poço de Mana: FE + Essência Purificada → mana do jogador ({@link Config#wellFePerMana()} FE e
 * {@link Config#wellEssencePerMana()} mB por ponto).
 * <ul>
 * <li>Aura: a cada segundo, cada jogador num raio de {@link Config#wellRadius()} ganha até {@link Config#wellAura()} de mana
 * (por cima da regeneração normal) e a reserva do cajado na mão ganha o mesmo.</li>
 * <li>Clique (sem agachar): enche a mana de uma vez, pagando o custo cheio; uma vez a cada {@link Config#wellCooldown()} ticks.</li>
 * <li>Runa de um elemento no espaço de runa: +{@link Config#wellAffinityBonus()} de rendimento para quem tem nível 5+ nele.</li>
 * </ul>
 * <li><b>Mana líquida</b> (26/10): o poço converte a essência em mana líquida aos poucos ({@link #CONVERT_PER_TICK} mB/t, mesmo
 * custo por ponto), num tanque de {@link #MANA_TANK} mB (1 mB = 1 ponto). A aura e o clique gastam primeiro a mana líquida.
 * Pote de Vidro na mão tira {@link ManaPotionItem#MANA} mB numa Poção de Mana.</li>
 * info0 = mana dada no último segundo ×10, info1 = tanque, info2 = situação ({@link #STATUS}), info3 = mana líquida (mB).
 */
public class ManaWellBlockEntity extends MachineBlockEntity implements ArcaneMachineBlock.EssenceTankHolder {
    public static final int TANK = 4000;
    public static final int CAPACITY = 50_000;
    public static final int SLOT_FUEL = 0, SLOT_EMPTY = 1, SLOT_RUNE = 2;
    public static final int AFFINITY_LEVEL = 5;
    public static final int MANA_TANK = 4000;
    public static final int CONVERT_PER_TICK = 1;
    public static final String[] STATUS = {"idle", "working", "no_energy", "no_essence"};
    public static final int ST_IDLE = 0, ST_WORKING = 1, ST_NO_ENERGY = 2, ST_NO_ESSENCE = 3;

    private final PurityFluids.Tanks tank = new PurityFluids.Tanks(TANK, new FluidResource[] {PurityFluids.pure()},
            new boolean[] {true}, new boolean[] {false}, this::setChanged);
    /** Último clique de encher de cada jogador (tempo do mundo). Não é salvo. */
    private final Map<UUID, Long> lastFill = new HashMap<>();
    private int auraTimer;
    /** Mana líquida (mB = pontos). */
    private int mana;

    public ManaWellBlockEntity(BlockPos pos, BlockState state) {
        super(WellContent.MANA_WELL_BE.get(), pos, state, WellContent.WELL_LAYOUT, 1000, 0);
    }

    public PurityFluids.Tanks getTank() {
        return this.tank;
    }

    public static boolean isEssence(ItemStack stack) {
        return stack.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) || stack.is(PurityContent.PURIFIED_ESSENCE_BUCKET.get());
    }

    public static boolean isRune(ItemStack stack) {
        return RuneElement.ofRune(stack) != null;
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    // ----- custo -----

    /** Rendimento para este jogador: 1 + bônus se a runa do espaço for de um elemento em que ele tem nível 5+. */
    public float yield(Player player) {
        RuneElement element = RuneElement.ofRune(this.items.get(SLOT_RUNE));
        if (element != null && Affinity.effectiveLevel(player, element) >= AFFINITY_LEVEL) {
            return 1.0F + Config.wellAffinityBonus();
        }
        return 1.0F;
    }

    /** Quantos pontos de mana o poço consegue pagar agora (mana líquida + o que a energia e a essência pagam). */
    private int affordable() {
        return this.mana + this.convertible();
    }

    /** Pontos que a energia e a essência ainda pagam. */
    private int convertible() {
        int fe = Config.wellFePerMana(), ess = Config.wellEssencePerMana();
        long byEnergy = fe <= 0 ? Integer.MAX_VALUE : this.storedEnergy() / fe;
        long byEssence = ess <= 0 ? Integer.MAX_VALUE : this.tank.amount(0) / ess;
        return (int) Math.min(Integer.MAX_VALUE, Math.min(byEnergy, byEssence));
    }

    private void pay(int points) {
        int fromMana = Math.min(points, this.mana);
        this.mana -= fromMana;
        int rest = points - fromMana;
        this.drainEnergy(rest * Config.wellFePerMana());
        this.tank.take(0, rest * Config.wellEssencePerMana());
        this.setChanged();
    }

    public int mana() {
        return this.mana;
    }

    /** Converte essência + FE em mana líquida, aos poucos. */
    private void convert() {
        int room = MANA_TANK - this.mana;
        int n = Math.min(Math.min(room, CONVERT_PER_TICK), this.convertible());
        if (n > 0) {
            this.drainEnergy(n * Config.wellFePerMana());
            this.tank.take(0, n * Config.wellEssencePerMana());
            this.mana += n;
            this.setChanged();
        }
    }

    /** Dá até {@code wanted} de mana ao jogador (e mais {@code reserveWanted} à reserva do cajado). @return mana dada (jogador + reserva) */
    private float give(Player player, float wanted, float reserveWanted) {
        float yield = this.yield(player);
        float given = 0;
        float space = ManaService.max(player) - ManaService.get(player);
        float toPlayer = Math.min(wanted, space);
        if (toPlayer > 0.01F) {
            int points = Math.min(this.affordable(), (int) Math.ceil(toPlayer / yield));
            if (points > 0) {
                float amount = Math.min(toPlayer, points * yield);
                this.pay(points);
                ManaService.set(player, ManaService.get(player) + amount);
                given += amount;
            }
        }
        ItemStack staff = ManaService.reserveStack(player);
        if (reserveWanted > 0 && !staff.isEmpty()) {
            int room = ManaService.reserveMax(staff) - ManaService.reserveOf(staff);
            int want = (int) Math.min(room, Math.floor(reserveWanted));
            int points = Math.min(this.affordable(), (int) Math.ceil(want / yield));
            if (want > 0 && points > 0) {
                int added = ManaService.addReserve(staff, (int) Math.min(want, Math.floor(points * yield)));
                if (added > 0) {
                    this.pay(points);
                    given += added;
                }
            }
        }
        return given;
    }

    // ----- tick -----

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        this.fillTank();
        this.convert();
        this.info1 = this.tank.amount(0);
        this.info3 = this.mana;
        if (++this.auraTimer < 20) {
            return;
        }
        this.auraTimer = 0;
        float total = 0;
        boolean wanted = false;
        double r = Config.wellRadius();
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(r), p -> p.isAlive() && !p.isSpectator()
                && p.distanceToSqr(Vec3.atCenterOf(pos)) <= r * r)) {
            float per = Config.wellAura();
            boolean needs = ManaService.get(player) < ManaService.max(player) - 0.01F;
            ItemStack staff = ManaService.reserveStack(player);
            needs |= !staff.isEmpty() && ManaService.reserveOf(staff) < ManaService.reserveMax(staff);
            if (!needs) {
                continue;
            }
            wanted = true;
            float given = this.give(player, per, per);
            if (given > 0) {
                total += given;
                Vec3 from = Vec3.atCenterOf(pos).add(0, 0.6, 0);
                Vec3 to = player.position().add(0, 1.0, 0);
                Vec3 dir = to.subtract(from);
                for (int i = 1; i <= 4; i++) {
                    Vec3 p = from.add(dir.scale(i / 5.0));
                    level.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }
        }
        int status;
        if (total > 0) {
            status = ST_WORKING;
        } else if (!wanted) {
            status = ST_IDLE;
        } else if (this.mana <= 0 && this.tank.amount(0) < Config.wellEssencePerMana()) {
            status = ST_NO_ESSENCE;
        } else {
            status = ST_NO_ENERGY;
        }
        this.info0 = Math.round(total * 10);
        this.info2 = status;
        this.setLit(level, pos, state, status == ST_WORKING || this.mana > 0);
    }

    private void fillTank() {
        ItemStack in = this.items.get(SLOT_FUEL);
        if (in.isEmpty() || !isEssence(in)) {
            return;
        }
        boolean bottle = in.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get());
        int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        ItemStack empty = new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET);
        if (this.tank.space(0) >= amount && this.canOutput(empty)) {
            this.tank.add(0, amount);
            in.shrink(1);
            this.insertOutput(empty);
        }
    }

    // ----- clique -----

    /** Clique sem agachar: enche a mana de uma vez (o quanto der para pagar). */
    public void fill(ServerLevel level, Player player) {
        long now = level.getGameTime();
        Long last = this.lastFill.get(player.getUUID());
        if (last != null && now - last < Config.wellCooldown()) {
            long seconds = (Config.wellCooldown() - (now - last) + 19) / 20;
            player.sendOverlayMessage(Component.translatable("message.mistech.mana_well.cooldown", seconds).withStyle(ChatFormatting.GRAY));
            return;
        }
        float space = ManaService.max(player) - ManaService.get(player);
        if (space < 1.0F) {
            player.sendOverlayMessage(Component.translatable("message.mistech.mana_well.full").withStyle(ChatFormatting.GRAY));
            return;
        }
        float given = this.give(player, space, 0);
        if (given <= 0) {
            player.sendOverlayMessage(Component.translatable(this.tank.amount(0) < Config.wellEssencePerMana()
                    ? "message.mistech.mana_well.no_essence" : "message.mistech.mana_well.no_energy").withStyle(ChatFormatting.RED));
            return;
        }
        this.lastFill.put(player.getUUID(), now);
        player.sendOverlayMessage(Component.translatable("message.mistech.mana_well.filled", Math.round(given)).withStyle(ChatFormatting.AQUA));
        BlockPos pos = this.worldPosition;
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 30, 0.3, 0.2, 0.3, 0.5);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.6F);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.4F);
        this.setChanged();
    }

    // ----- essência na mão -----

    @Override
    public boolean handles(ItemStack stack) {
        return isEssence(stack) || isManaContainer(stack);
    }

    /** Garrafa de vidro (ou Pote de Vidro) tira mana líquida numa Poção de Mana. */
    public static boolean isManaContainer(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE) || stack.is(com.mistech.mistechmod.Mistech.GLASS_JAR_ITEM.get());
    }

    @Override
    public void interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (isManaContainer(held)) {
            if (this.mana < ManaPotionItem.MANA) {
                player.sendOverlayMessage(Component.translatable("message.mistech.mana_well.no_liquid", this.mana, ManaPotionItem.MANA)
                        .withStyle(ChatFormatting.GRAY));
                return;
            }
            this.mana -= ManaPotionItem.MANA;
            this.setChanged();
            PurityFluids.swap(player, hand, new ItemStack(WellContent.MANA_POTION.get()));
            player.level().playSound(null, this.worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.4F);
            return;
        }
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
        lines.add(Component.translatable("info.mistech.tank", Component.translatable("gui.mistech.mana_well.liquid"), this.mana, MANA_TANK));
        if (this.info0 > 0) {
            lines.add(Component.translatable("gui.mistech.mana_well.giving", String.format(java.util.Locale.ROOT, "%.1f", this.info0 / 10.0F)));
        }
        super.addInfo(lines);
    }

    /** Linhas da tela (cliente). */
    public static List<Component> infoLines(int given, int status) {
        String id = STATUS[Math.max(0, Math.min(STATUS.length - 1, status))];
        return given > 0
                ? List.of(Component.translatable("gui.mistech.mana_well.status." + id),
                        Component.translatable("gui.mistech.mana_well.giving", String.format(java.util.Locale.ROOT, "%.1f", given / 10.0F)))
                : List.of(Component.translatable("gui.mistech.mana_well.status." + id));
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.tank.setAmount(0, input.getIntOr("Essence", 0));
        this.mana = Math.max(0, Math.min(MANA_TANK, input.getIntOr("Mana", 0)));
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Essence", this.tank.amount(0));
        output.putInt("Mana", this.mana);
    }
}
