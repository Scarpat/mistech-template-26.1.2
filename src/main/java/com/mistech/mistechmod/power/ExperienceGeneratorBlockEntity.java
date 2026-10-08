package com.mistech.mistechmod.power;

import java.lang.reflect.Field;
import java.util.List;

import com.mistech.mistechmod.machine.MachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Gerador de Experiência: transforma pontos de experiência em energia — 1 ponto = {@link #FE_PER_POINT} FE,
 * {@link #POINTS_PER_TICK} ponto por tick ({@link #RATE} FE/t). Reserva de {@link #TANK} pontos.
 * <ul>
 * <li>Suga os orbes de experiência num raio de {@link #ORB_RANGE} blocos.</li>
 * <li>Agachado com a mão vazia: deposita toda a experiência do jogador.</li>
 * <li>Espaço de combustível: Frasco de Experiência (7 pontos) ou itens/livros encantados — tira os encantamentos
 * (menos maldições) e devolve o item limpo na saída, rendendo o custo mínimo de cada encantamento em pontos.</li>
 * </ul>
 * Buffer {@link #CAPACITY} FE; envia até {@link #MAX_OUTPUT} FE/t para cada bloco encostado.
 * info0 = FE/t; info1 = pontos guardados.
 */
public class ExperienceGeneratorBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 400_000;
    public static final int FE_PER_POINT = 600;
    public static final int POINTS_PER_TICK = 1;
    public static final int RATE = FE_PER_POINT * POINTS_PER_TICK;
    public static final int MAX_OUTPUT = 2_000;
    public static final int TANK = 10_000;
    public static final int ORB_RANGE = 5;
    public static final int BOTTLE_POINTS = 7;
    public static final int SLOT_INPUT = 0;

    private static final Field ORB_COUNT = orbCountField();

    private int points;
    private int orbTimer;

    public ExperienceGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.EXPERIENCE_GENERATOR_BE.get(), pos, state, PowerLayouts.EXPERIENCE_GENERATOR, 0, MAX_OUTPUT);
    }

    public int points() {
        return this.points;
    }

    public int space() {
        return TANK - this.points;
    }

    /** Pontos que o item rende no espaço de combustível (0 = não serve). */
    public static int itemPoints(ItemStack stack) {
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            return BOTTLE_POINTS;
        }
        int total = 0;
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            if (!holder.is(EnchantmentTags.CURSE)) {
                total += Math.max(1, holder.value().getMinCost(entry.getIntValue()));
            }
        }
        return total;
    }

    /** O item sem os encantamentos (maldições ficam). */
    private static ItemStack cleaned(ItemStack stack) {
        ItemStack result = stack.copyWithCount(1);
        EnchantmentHelper.updateEnchantments(result, mutable -> mutable.removeIf(holder -> !holder.is(EnchantmentTags.CURSE)));
        if (result.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantmentsForCrafting(result).isEmpty()) {
            result = new ItemStack(Items.BOOK);
        }
        result.remove(DataComponents.REPAIR_COST);
        return result;
    }

    @Override
    protected boolean canBeBoosted() {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        if (--this.orbTimer <= 0) {
            this.orbTimer = 10;
            this.absorbOrbs(level, pos);
        }
        ItemStack input = this.items.get(SLOT_INPUT);
        if (!input.isEmpty()) {
            int value = itemPoints(input);
            if (value > 0 && value <= this.space()) {
                if (input.is(Items.EXPERIENCE_BOTTLE)) {
                    input.shrink(1);
                    this.points += value;
                    this.setChanged();
                } else {
                    ItemStack clean = cleaned(input);
                    if (this.canOutput(clean)) {
                        input.shrink(1);
                        this.insertOutput(clean);
                        this.points += value;
                        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 1.3F);
                        this.setChanged();
                    }
                }
            }
        }

        int space = this.layout.capacity - this.storedEnergy();
        int generated = 0;
        if (this.points >= POINTS_PER_TICK && space >= RATE) {
            this.points -= POINTS_PER_TICK;
            generated = RATE;
            this.addEnergy(generated);
            this.setChanged();
        }
        this.maxProgress = TANK;
        this.progress = this.points;
        this.info0 = generated;
        this.info1 = this.points;
        this.pushEnergy(level, pos, MAX_OUTPUT);
        this.setLit(level, pos, state, generated > 0);
    }

    private void absorbOrbs(ServerLevel level, BlockPos pos) {
        if (this.space() <= 0) {
            return;
        }
        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(ORB_RANGE));
        boolean took = false;
        for (ExperienceOrb orb : orbs) {
            if (!orb.isAlive()) {
                continue;
            }
            int count = orbCount(orb);
            int value = orb.getValue();
            if (ORB_COUNT == null && (long) value * count > this.space()) {
                break; // sem como diminuir o orbe: só pega inteiro
            }
            while (count > 0 && this.space() >= value) {
                this.points += value;
                count--;
                took = true;
            }
            if (count <= 0) {
                orb.discard();
            } else {
                setOrbCount(orb, count);
                break;
            }
        }
        if (took) {
            this.setChanged();
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.3F, 0.6F + level.getRandom().nextFloat() * 0.4F);
        }
    }

    /** Deposita experiência do jogador: 1 nível, ou tudo se {@code all}. @return pontos guardados. */
    public int deposit(Player player, boolean all) {
        int have = totalPoints(player);
        if (have <= 0 || this.space() <= 0) {
            return 0;
        }
        int want;
        if (all) {
            want = have;
        } else {
            // o que falta para descer 1 nível (ou o que sobra no nível 0)
            int progress = Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
            want = progress > 0 ? progress : player.experienceLevel > 0 ? xpForLevel(player.experienceLevel - 1) : have;
        }
        int moved = Math.min(Math.min(want, have), this.space());
        if (moved > 0) {
            player.giveExperiencePoints(-moved);
            this.points += moved;
            this.setChanged();
        }
        return moved;
    }

    /** Pontos para ir do nível {@code level} ao seguinte (fórmula da vanilla). */
    private static int xpForLevel(int level) {
        return level >= 30 ? 112 + (level - 30) * 9 : level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }

    public static int totalPoints(Player player) {
        int total = 0;
        for (int i = 0; i < player.experienceLevel; i++) {
            total += xpForLevel(i);
        }
        return total + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    private static Field orbCountField() {
        try {
            Field field = ExperienceOrb.class.getDeclaredField("count");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static int orbCount(ExperienceOrb orb) {
        if (ORB_COUNT != null) {
            try {
                return Math.max(1, ORB_COUNT.getInt(orb));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // cai no padrão
            }
        }
        return 1;
    }

    private static void setOrbCount(ExperienceOrb orb, int count) {
        if (ORB_COUNT != null) {
            try {
                ORB_COUNT.setInt(orb, count);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // sem como mudar: deixa o orbe como está
            }
        }
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.points = Math.max(0, Math.min(TANK, input.getIntOr("Points", 0)));
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Points", this.points);
    }
}
