package com.mistech.mistechmod.item;

import javax.annotation.Nullable;

import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Incenso: empilha até 64 enquanto novo. Acende com um isqueiro (flint and steel) na outra mão;
 * aceso, queima por 2 minutos e mantém o efeito Véu de Incenso no jogador.
 * O tempo restante fica no componente mistech:incense_burn (em segundos).
 */
public class IncenseItem extends Item {
    /** Segundos de queima de um incenso comum novo. */
    public static final int BURN_SECONDS = 120;

    /** Segundos de queima deste tipo de incenso. */
    private final int burnSeconds;
    /** Multiplicador do alcance do efeito (Porta-Incenso e perda de alvo). */
    private final int rangeMultiplier;
    /** A cada quantos segundos queimando gera Resíduo Mágico (0 = nunca). */
    private final int residueInterval;

    /** Incenso comum: 2 minutos, alcance normal, sem resíduo. */
    public IncenseItem(Item.Properties properties) {
        this(properties, BURN_SECONDS, 1, 0);
    }

    public IncenseItem(Item.Properties properties, int burnSeconds, int rangeMultiplier, int residueInterval) {
        super(properties);
        this.burnSeconds = burnSeconds;
        this.rangeMultiplier = rangeMultiplier;
        this.residueInterval = residueInterval;
    }

    public int getBurnSeconds() {
        return this.burnSeconds;
    }

    public int getRangeMultiplier() {
        return this.rangeMultiplier;
    }

    /** Incenso comum ou mágico. */
    public static boolean isIncense(ItemStack stack) {
        return stack.getItem() instanceof IncenseItem;
    }

    /** Tempo total de queima do incenso deste stack. */
    public static int maxBurnSeconds(ItemStack stack) {
        return stack.getItem() instanceof IncenseItem incense ? incense.burnSeconds : BURN_SECONDS;
    }

    /** Multiplicador de alcance do incenso deste stack (1 para o comum). */
    public static int rangeMultiplier(ItemStack stack) {
        return stack.getItem() instanceof IncenseItem incense ? incense.rangeMultiplier : 1;
    }

    /** true se este incenso gera Resíduo Mágico (e por isso não deixa cinzas comuns). */
    public static boolean makesResidue(ItemStack stack) {
        return stack.getItem() instanceof IncenseItem incense && incense.residueInterval > 0;
    }

    /**
     * Quantas Cinzas de Incenso um incenso não mágico deixa ao terminar: sempre 1, e
     * {@link com.mistech.mistechmod.block.IncenseHolderBlockEntity#EXTRA_ASH_CHANCE} de chance de 2.
     */
    public static int rollAsh(net.minecraft.util.RandomSource random) {
        return random.nextFloat() < com.mistech.mistechmod.block.IncenseHolderBlockEntity.EXTRA_ASH_CHANCE ? 2 : 1;
    }

    /** Isqueiro ou Runa de Fogo. */
    public static boolean isLighter(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.FlintAndSteelItem || stack.is(com.mistech.mistechmod.Mistech.FIRE_RUNE.get());
    }

    public static boolean isLit(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(MistechComponents.INCENSE_LIT.get()));
    }

    public static int getRemainingSeconds(ItemStack stack) {
        return stack.getOrDefault(MistechComponents.INCENSE_BURN.get(), maxBurnSeconds(stack));
    }

    /** Marca UM incenso (stack de tamanho 1) como aceso. */
    public static void setLit(ItemStack single) {
        single.set(MistechComponents.INCENSE_LIT.get(), true);
        single.set(MistechComponents.INCENSE_BURN.get(), getRemainingSeconds(single));
        single.set(DataComponents.MAX_STACK_SIZE, 1); // aceso/usado não empilha
    }

    /**
     * Queima 1 segundo do incenso aceso.
     * @return true se o incenso acabou (o stack foi esvaziado)
     */
    public static boolean burnOneSecond(ItemStack stack) {
        return burnOneSecond(stack, () -> {});
    }

    /**
     * Queima 1 segundo; chama {@code onResidue} cada vez que o incenso mágico completa um intervalo
     * de resíduo (a cada 30 s, incluindo o último segundo).
     * @return true se o incenso acabou (o stack foi esvaziado)
     */
    public static boolean burnOneSecond(ItemStack stack, Runnable onResidue) {
        int remaining = getRemainingSeconds(stack) - 1;
        if (stack.getItem() instanceof IncenseItem incense && incense.residueInterval > 0) {
            int elapsed = incense.burnSeconds - Math.max(remaining, 0);
            if (elapsed > 0 && elapsed % incense.residueInterval == 0) {
                onResidue.run();
            }
        }
        if (remaining <= 0) {
            stack.shrink(1);
            return true;
        }
        stack.set(MistechComponents.INCENSE_BURN.get(), remaining);
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack incense = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);

        // Shift + botão direito apaga o incenso (guarda o tempo que falta)
        if (isLit(incense) && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                incense.remove(MistechComponents.INCENSE_LIT.get());
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.6F);
            }
            return InteractionResult.SUCCESS;
        }

        if (!isLit(incense) && isLighter(other)) {
            lightInHand(level, player, hand, other, otherHand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Acende um incenso da mão {@code incenseHand} gastando 1 de durabilidade do isqueiro.
     * Se a mão tiver vários incensos, só um é aceso e o resto vai para o inventário.
     */
    public static void lightInHand(Level level, Player player, InteractionHand incenseHand, ItemStack flint, InteractionHand flintHand) {
        if (level.isClientSide()) {
            return;
        }
        // Importante: acende o PRÓPRIO stack da mão (o jogo reaproveita esse objeto depois do use();
        // se trocarmos por um stack novo, o item some). O excedente vai para o inventário.
        lightInPlace(player, player.getItemInHand(incenseHand));

        EquipmentSlot flintSlot = flintHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        flint.hurtAndBreak(1, player, flintSlot);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
    }

    /**
     * Acende o PRÓPRIO stack (o jogo reaproveita esse objeto depois do use(); trocar por um novo faz o item sumir).
     * Se houver vários, só 1 fica aceso e o excedente vai para o inventário do jogador.
     */
    public static void lightInPlace(Player player, ItemStack stack) {
        ItemStack rest = stack.getCount() > 1 ? stack.copyWithCount(stack.getCount() - 1) : ItemStack.EMPTY;
        stack.setCount(1);
        setLit(stack);
        if (!rest.isEmpty() && !player.getInventory().add(rest)) {
            player.drop(rest, false);
        }
    }

    /** Apaga (mantém o tempo restante). Serve para incenso, charuto e baseado. */
    public static void extinguish(ItemStack stack) {
        stack.remove(MistechComponents.INCENSE_LIT.get());
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!isLit(stack) || !(entity instanceof Player player)) {
            return;
        }

        boolean inHand = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (inHand && level.getGameTime() % 8 == 0) {
            level.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getEyeY() - 0.2, player.getZ(),
                    1, 0.25, 0.1, 0.25, 0.005);
        }

        if (level.getGameTime() % 20 != 0) {
            return;
        }

        // Mácula Maculada ou pior: o véu alcança só a metade
        double veil = com.mistech.mistechmod.purity.Taint.stage(player).ordinal() >= com.mistech.mistechmod.purity.Taint.Stage.TAINTED.ordinal() ? 0.5 : 1.0;
        IncenseVeil.apply(level, player, IncenseVeil.CALM_RADIUS * this.rangeMultiplier * veil);

        if (player.getAbilities().instabuild) {
            return; // criativo: não gasta
        }
        boolean magic = makesResidue(stack);
        if (burnOneSecond(stack, () -> giveResidue(player))) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.6F);
            // queimado na mão/inventário também deixa cinzas (o mágico já deixou Resíduo Mágico)
            if (!magic) {
                giveItem(player, new ItemStack(com.mistech.mistechmod.Mistech.INCENSE_ASH.get(), rollAsh(level.getRandom())));
            }
        }
    }

    /** Resíduo Mágico vai para o inventário (cai no chão se estiver cheio). */
    private static void giveResidue(Player player) {
        giveItem(player, new ItemStack(com.mistech.mistechmod.Mistech.MAGIC_RESIDUE.get()));
    }

    /** Vai para o inventário (cai no chão se estiver cheio). */
    private static void giveItem(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // Barra de "queima" no lugar da barra de durabilidade
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(MistechComponents.INCENSE_BURN.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F * getRemainingSeconds(stack) / this.burnSeconds), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE8913A; // laranja-brasa
    }

    @Override
    public Component getName(ItemStack stack) {
        return isLit(stack) ? Component.translatable(this.getDescriptionId() + ".lit") : super.getName(stack);
    }
}
