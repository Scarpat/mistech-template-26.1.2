package com.mistech.mistechmod.ranged;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.mithril.GearRunes;
import com.mistech.mistechmod.item.mithril.GearTier;
import com.mistech.mistechmod.item.mithril.MithrilGear;
import com.mistech.mistechmod.purity.TaintEvents;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Besta Mágica (ritual Tier II sobre uma besta; nível Adamantina: energia e 3 engastes). Não usa flechas: dispara
 * <b>munição rúnica</b> do tambor (até {@link #DRUM_SIZE} runas).
 * <ul>
 * <li>Agachar + botão direito com uma runa na outra mão: põe a runa no tambor (cheio: troca pela escolhida).
 *     Agachar + botão direito sem runa: tira a escolhida. <b>Shift + roda</b>: escolhe a runa.</li>
 * <li>Segurar o botão direito carrega ({@link #BASE_CHARGE} ticks; −{@link #TIME_CHARGE_PER_RUNE} por Runa do Tempo engastada);
 *     botão direito de novo dispara.</li>
 * <li>Cada disparo gasta 1 uso da runa (a Selada não gasta) e {@link #SHOT_COST} FE.</li>
 * <li>Runa de Ar engastada: 3 virotes em leque.</li>
 * </ul>
 * Efeitos por runa em {@link BoltEffects} (Ar, Vazio, Eco e Nuvens agem aqui, no disparo).
 */
public class ArcaneCrossbowItem extends CrossbowItem implements MithrilGear.Gear {
    public static final int DRUM_SIZE = 3;
    public static final int SHOT_COST = 600;
    public static final float BOLT_DAMAGE = 7.0F;
    public static final int BASE_CHARGE = 25;
    public static final int TIME_CHARGE_PER_RUNE = 4;
    public static final int MIN_CHARGE = 10;
    public static final float BOLT_SPEED = 3.2F;

    public ArcaneCrossbowItem(Properties properties) {
        super(properties);
    }

    @Override
    public GearTier tier() {
        return GearTier.ADAMANTINE;
    }

    // ------------------------------------------------------------------ tambor

    public static NonNullList<ItemStack> drum(ItemStack crossbow) {
        NonNullList<ItemStack> list = NonNullList.withSize(DRUM_SIZE, ItemStack.EMPTY);
        ItemContainerContents contents = crossbow.get(RangedContent.CROSSBOW_DRUM.get());
        if (contents != null) {
            contents.copyInto(list);
        }
        return list;
    }

    public static void storeDrum(ItemStack crossbow, List<ItemStack> runes) {
        crossbow.set(RangedContent.CROSSBOW_DRUM.get(), ItemContainerContents.fromItems(runes));
    }

    public static int selected(ItemStack crossbow) {
        return Mth.clamp(crossbow.getOrDefault(MistechComponents.STAFF_SELECTED.get(), 0), 0, DRUM_SIZE - 1);
    }

    public static ItemStack selectedRune(ItemStack crossbow) {
        return drum(crossbow).get(selected(crossbow));
    }

    /** Shift + roda (pacote do cliente): próxima runa do tambor que não está vazia. */
    public static void scroll(ServerPlayer player, int direction) {
        ItemStack stack = held(player);
        if (stack.isEmpty() || player.isUsingItem()) {
            return;
        }
        NonNullList<ItemStack> drum = drum(stack);
        int sel = selected(stack);
        for (int i = 1; i <= DRUM_SIZE; i++) {
            int next = Math.floorMod(sel + (direction > 0 ? i : -i), DRUM_SIZE);
            if (!drum.get(next).isEmpty()) {
                sel = next;
                break;
            }
        }
        stack.set(MistechComponents.STAFF_SELECTED.get(), sel);
        announce(player, stack);
        player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.PLAYERS, 0.6F, 1.4F + sel * 0.15F);
    }

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof ArcaneCrossbowItem;
    }

    public static ItemStack held(Player player) {
        return is(player.getMainHandItem()) ? player.getMainHandItem() : is(player.getOffhandItem()) ? player.getOffhandItem() : ItemStack.EMPTY;
    }

    private static void announce(Player player, ItemStack crossbow) {
        ItemStack rune = selectedRune(crossbow);
        RuneElement element = RuneElement.ofRune(rune);
        if (element == null) {
            player.sendOverlayMessage(Component.translatable("item.mistech.arcane_crossbow.empty_drum").withStyle(ChatFormatting.GRAY));
        } else {
            player.sendOverlayMessage(Component.translatable("item.mistech.arcane_crossbow.selected", rune.getHoverName(), uses(rune))
                    .withColor(element.color()));
        }
    }

    private static Component uses(ItemStack rune) {
        return TaintEvents.isSealed(rune) || !rune.isDamageableItem() ? Component.literal("∞")
                : Component.literal((rune.getMaxDamage() - rune.getDamageValue()) + "/" + rune.getMaxDamage());
    }

    /** Agachar + botão direito: põe a runa da outra mão no tambor ou tira a escolhida. */
    private static void manageDrum(Player player, InteractionHand hand, ItemStack crossbow) {
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        NonNullList<ItemStack> drum = drum(crossbow);
        int sel = selected(crossbow);
        if (RuneElement.ofRune(other) != null) {
            int slot = -1;
            for (int i = 0; i < DRUM_SIZE; i++) {
                if (drum.get(i).isEmpty()) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) {
                slot = sel;
                player.getInventory().placeItemBackInInventory(drum.get(slot));
            }
            drum.set(slot, other.split(1));
            sel = slot;
            player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.8F, 1.3F);
        } else if (!drum.get(sel).isEmpty()) {
            player.getInventory().placeItemBackInInventory(drum.get(sel));
            drum.set(sel, ItemStack.EMPTY);
            for (int i = 0; i < DRUM_SIZE; i++) {
                if (!drum.get(i).isEmpty()) {
                    sel = i;
                    break;
                }
            }
            crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
            player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.PLAYERS, 0.8F, 0.8F);
        } else {
            player.sendOverlayMessage(Component.translatable("item.mistech.arcane_crossbow.how").withStyle(ChatFormatting.GRAY));
            return;
        }
        storeDrum(crossbow, drum);
        crossbow.set(MistechComponents.STAFF_SELECTED.get(), sel);
        announce(player, crossbow);
    }

    // ------------------------------------------------------------------ carregar e atirar

    public static int chargeTicks(ItemStack crossbow) {
        double time = GearRunes.count(GearRunes.tally(crossbow), RuneElement.TIME);
        return Math.max(MIN_CHARGE, (int) Math.round(BASE_CHARGE - TIME_CHARGE_PER_RUNE * time));
    }

    private static boolean canPay(Player player, ItemStack crossbow) {
        return player.hasInfiniteMaterials() || MithrilGear.getEnergy(crossbow) >= com.mistech.mistechmod.mage.MageRobes.cost(player, SHOT_COST);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return stack -> false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                manageDrum(player, hand, stack);
            }
            return InteractionResult.SUCCESS;
        }
        if (isCharged(stack)) {
            if (level instanceof ServerLevel server) {
                this.fire(server, player, hand, stack);
            }
            return InteractionResult.CONSUME;
        }
        if (selectedRune(stack).isEmpty()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("item.mistech.arcane_crossbow.how").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.FAIL;
        }
        if (!canPay(player, stack)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("item.mistech.mithril.empty").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (level.isClientSide()) {
            return;
        }
        int held = this.getUseDuration(stack, entity) - ticksRemaining;
        int need = chargeTicks(stack);
        if (held == need / 3) {
            level.playSound(null, entity.blockPosition(), SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.PLAYERS, 0.5F, 1.2F);
        } else if (held == need * 2 / 3) {
            level.playSound(null, entity.blockPosition(), SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.PLAYERS, 0.5F, 1.2F);
        }
        if (held >= need && !isCharged(stack)) {
            stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(RangedContent.ARCANE_BOLT.get()))));
            level.playSound(null, entity.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1.0F, 1.3F);
            if (level instanceof ServerLevel server) {
                RuneElement element = RuneElement.ofRune(selectedRune(stack));
                if (element != null) {
                    server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(element.color(), 1.0F),
                            entity.getX(), entity.getY() + 1.2, entity.getZ(), 10, 0.3, 0.2, 0.3, 0.0);
                }
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        return isCharged(stack);
    }

    private void fire(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
        stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        NonNullList<ItemStack> drum = drum(stack);
        int sel = selected(stack);
        ItemStack rune = drum.get(sel);
        RuneElement element = RuneElement.ofRune(rune);
        if (element == null || !canPay(player, stack)) {
            player.sendOverlayMessage(Component.translatable("item.mistech.arcane_crossbow.fizzle").withStyle(ChatFormatting.GRAY));
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.6F);
            return;
        }
        if (TaintEvents.isSealed(rune) && com.mistech.mistechmod.purity.Taint.stage(player).pureEfficiency() <= 0) {
            // Runa Selada falha com a Mácula alta (como no resto do mod)
            player.sendOverlayMessage(Component.translatable("item.mistech.arcane_crossbow.fizzle").withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }
        if (!player.hasInfiniteMaterials()) {
            MithrilGear.setEnergy(stack, MithrilGear.getEnergy(stack) - com.mistech.mistechmod.mage.MageRobes.cost(player, SHOT_COST));
            if (rune.isDamageableItem()) {
                rune.setDamageValue(rune.getDamageValue() + 1);
                if (rune.getDamageValue() >= rune.getMaxDamage()) {
                    drum.set(sel, ItemStack.EMPTY);
                    level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
                }
            }
            storeDrum(stack, drum);
        }
        Map<RuneElement, Double> sockets = GearRunes.tally(stack);
        boolean multishot = GearRunes.count(sockets, RuneElement.AIR) > 0.0;
        switch (element) {
            case AIR -> windBurst(level, player, multishot);
            case VOID -> voidBeam(level, player);
            case ECHO -> sonicBoom(level, player);
            case CLOUD -> {
                BlockPos feet = BlockPos.containing(player.getX(), player.getY() - 0.5, player.getZ());
                com.mistech.mistechmod.sky.Clouds.bridge(level, feet, player.getDirection(), 16, player);
                level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
            default -> {
                float[] angles = multishot ? new float[] {0.0F, -10.0F, 10.0F} : new float[] {0.0F};
                for (float angle : angles) {
                    ArcaneBoltEntity bolt = new ArcaneBoltEntity(level, player, element, BOLT_DAMAGE);
                    bolt.shootFromRotation(player, player.getXRot(), player.getYRot() + angle, 0.0F, BOLT_SPEED, 0.5F);
                    level.addFreshEntity(bolt);
                }
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.9F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.2F);
        player.getCooldowns().addCooldown(stack, 10);
        Affinity.grant(player, element, 1);
        if (rune.isEmpty() || drum.get(sel).isEmpty()) {
            announce(player, stack);
        }
    }

    /** Ar: Carga de Vento (3 em leque com Ar engastado) e o atirador é jogado para trás. */
    private static void windBurst(ServerLevel level, Player player, boolean multishot) {
        float[] angles = multishot ? new float[] {0.0F, -12.0F, 12.0F} : new float[] {0.0F};
        Vec3 eye = player.getEyePosition();
        for (float angle : angles) {
            WindCharge charge = new WindCharge(player, level, eye.x, eye.y - 0.1, eye.z);
            charge.shootFromRotation(player, player.getXRot(), player.getYRot() + angle, 0.0F, 1.8F, 0.5F);
            level.addFreshEntity(charge);
        }
        Vec3 back = player.getLookAngle().scale(-1.1);
        player.push(back.x, Math.max(0.25, back.y * 0.5 + 0.25), back.z);
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    /** Vazio: raio que atravessa blocos e criaturas (32 blocos, 9 de dano mágico). */
    private static void voidBeam(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(32.0));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.0),
                e -> e != player && e.isAlive() && e.getBoundingBox().inflate(0.4).clip(eye, end).isPresent())) {
            e.hurtServer(level, level.damageSources().indirectMagic(player, player), 9.0F);
            e.invulnerableTime = 0;
        }
        for (double d = 1.0; d < 32.0; d += 0.7) {
            Vec3 p = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y - 0.1, p.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    /** Eco: estrondo sônico do Warden (16 blocos, para no primeiro bloco sólido; 10 de dano que ignora armadura). */
    private static void sonicBoom(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult wall = level.clip(new ClipContext(eye, eye.add(look.scale(16.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = wall.getType() == HitResult.Type.MISS ? eye.add(look.scale(16.0)) : wall.getLocation();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.0),
                e -> e != player && e.isAlive() && e.getBoundingBox().inflate(0.5).clip(eye, end).isPresent())) {
            e.hurtServer(level, level.damageSources().sonicBoom(player), 10.0F);
            e.invulnerableTime = 0;
            Vec3 push = look.scale(1.5);
            e.push(push.x, 0.3, push.z);
            e.hurtMarked = true;
        }
        double len = end.distanceTo(eye);
        for (double d = 1.0; d < len; d += 1.0) {
            Vec3 p = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y - 0.1, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5F, 1.2F);
    }

    // ------------------------------------------------------------------ energia como os outros equipamentos

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return MithrilGear.damageItem(stack, amount, entity);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return MithrilGear.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return this.tier().barColor();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.arcane_crossbow.desc").withStyle(ChatFormatting.GRAY));
        NonNullList<ItemStack> drum = drum(stack);
        int sel = selected(stack);
        List<Component> lines = new ArrayList<>();
        for (int i = 0; i < DRUM_SIZE; i++) {
            ItemStack rune = drum.get(i);
            RuneElement element = RuneElement.ofRune(rune);
            Component line = element == null ? Component.translatable("item.mistech.arcane_crossbow.slot_empty").withStyle(ChatFormatting.DARK_GRAY)
                    : Component.empty().append(rune.getHoverName()).append(" (").append(uses(rune)).append(")").withColor(element.color());
            lines.add(Component.literal(i == sel ? "▶ " : "   ").withStyle(ChatFormatting.WHITE).append(line));
        }
        builder.accept(Component.translatable("item.mistech.arcane_crossbow.drum").withStyle(ChatFormatting.GOLD));
        lines.forEach(builder);
        builder.accept(Component.translatable("item.mistech.arcane_crossbow.help").withStyle(ChatFormatting.DARK_GRAY));
        MithrilGear.tooltip(stack, builder);
    }
}
