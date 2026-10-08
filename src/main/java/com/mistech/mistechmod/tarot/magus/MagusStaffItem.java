package com.mistech.mistechmod.tarot.magus;

import java.util.function.Consumer;

import com.mistech.mistechmod.accessory.Accessories;
import com.mistech.mistechmod.accessory.Accessory;
import com.mistech.mistechmod.registry.MistechComponents;
import com.mistech.mistechmod.tarot.magus.MagusSpellEntity.Kind;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cajado do Mago: uma arma absurda com o poder dos quatro elementos. Gasta energia (FE) — carrega em qualquer
 * carregador; sem energia só bate.
 * <ul>
 * <li><b>Shift + roda do mouse</b>: troca o elemento (Terra, Ar, Água, Fogo).</li>
 * <li><b>Botão direito</b>: magia do elemento — Linha de Espinhos, leque de lâminas de vento, Orbe de Gelo, Bola de Fogo.</li>
 * <li><b>Agachado + botão direito</b>: Pele de Pedra, Pisca, Bolha (cura), Meteoro no ponto mirado.</li>
 * <li><b>Segurar o botão direito 2 s</b>: Raio do ∞, que atravessa tudo em linha reta.</li>
 * </ul>
 * O Pingente do Arcano diminui as recargas em 20%.
 */
public class MagusStaffItem extends Item {
    public static final int CAPACITY = 200_000;
    public static final int MAX_CHARGE_PER_TICK = 4_000;
    public static final int COST_PRIMARY = 1_500;
    public static final int COST_SECONDARY = 4_000;
    public static final int COST_QUINT = 25_000;
    public static final int CHARGE_TICKS = 40;
    private static final MagusElement[] MODES = MagusElement.CLASSIC;

    public MagusStaffItem(Item.Properties properties) {
        super(properties);
    }

    // ----- dados -----

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(MistechComponents.STAFF_ENERGY.get(), 0);
    }

    public static void setEnergy(ItemStack stack, int energy) {
        stack.set(MistechComponents.STAFF_ENERGY.get(), Mth.clamp(energy, 0, CAPACITY));
    }

    public static MagusElement mode(ItemStack stack) {
        return MODES[Math.floorMod(stack.getOrDefault(MistechComponents.STAFF_SELECTED.get(), 0), MODES.length)];
    }

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof MagusStaffItem;
    }

    private static boolean spend(Player player, ItemStack stack, int amount) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        amount = com.mistech.mistechmod.mage.MageRobes.cost(player, amount); // Vestes de Mago
        int energy = getEnergy(stack);
        if (energy < amount) {
            player.sendOverlayMessage(Component.translatable("item.mistech.magician_staff.no_energy").withStyle(ChatFormatting.RED));
            return false;
        }
        setEnergy(stack, energy - amount);
        return true;
    }

    /** Recarga própria de cada magia (guardada no jogador). */
    private static boolean ready(Player player, String key) {
        long until = player.getPersistentData().getLongOr("mistech_magus_staff_" + key, 0L);
        if (player.level().getGameTime() < until) {
            long secs = (until - player.level().getGameTime() + 19) / 20;
            player.sendOverlayMessage(Component.translatable("item.mistech.magician_staff.cooldown", secs).withStyle(ChatFormatting.GRAY));
            return false;
        }
        return true;
    }

    private static void cooldown(Player player, String key, int ticks) {
        if (Accessories.has(player, Accessory.ARCANE_PENDANT)) {
            ticks = (int) (ticks * 0.8);
        }
        player.getPersistentData().putLong("mistech_magus_staff_" + key, player.level().getGameTime() + ticks);
    }

    /** Shift + roda do mouse (pacote do cliente): troca o elemento. */
    public static void scroll(ServerPlayer player, int direction) {
        ItemStack stack = is(player.getMainHandItem()) ? player.getMainHandItem() : is(player.getOffhandItem()) ? player.getOffhandItem() : ItemStack.EMPTY;
        if (stack.isEmpty() || player.isUsingItem()) {
            return;
        }
        int selected = Math.floorMod(stack.getOrDefault(MistechComponents.STAFF_SELECTED.get(), 0) + (direction > 0 ? 1 : -1), MODES.length);
        stack.set(MistechComponents.STAFF_SELECTED.get(), selected);
        player.sendOverlayMessage(Component.translatable("item.mistech.magician_staff.mode", MODES[selected].displayName()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 0.8F + selected * 0.2F);
    }

    // ----- uso -----

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level instanceof ServerLevel server) {
                this.secondary(server, player, stack);
            }
            return InteractionResult.SUCCESS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return CHARGE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        int used = CHARGE_TICKS - remaining;
        if (used > 10 && level instanceof ServerLevel server) {
            double a = used * 0.5;
            MagusElement e = MODES[(used / 4) % 4];
            server.sendParticles(e.dust(1.0F), entity.getX() + Math.cos(a) * 0.8, entity.getY() + 1.0 + (used - 10) * 0.03,
                    entity.getZ() + Math.sin(a) * 0.8, 1, 0, 0, 0, 0);
            if (used == 30) {
                server.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.6F);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        int used = CHARGE_TICKS - remaining;
        if (entity instanceof Player player && level instanceof ServerLevel server && used < CHARGE_TICKS - 4) {
            this.primary(server, player, stack);
        }
        return true;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && level instanceof ServerLevel server) {
            this.quintessence(server, player, stack);
        }
        return stack;
    }

    private static Vec3 eye(Player player) {
        return player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.15, 0);
    }

    private void primary(ServerLevel level, Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack) || !spend(player, stack, COST_PRIMARY)) {
            return;
        }
        MagusElement mode = mode(stack);
        Vec3 look = player.getLookAngle();
        switch (mode) {
            case EARTH -> this.spikes(level, player);
            case AIR -> {
                for (int i = 0; i < 5; i++) {
                    Vec3 v = look.yRot((float) ((i - 2) * 0.16)).scale(1.0);
                    level.addFreshEntity(MagusSpellEntity.create(level, Kind.WIND_BOLT, player, eye(player), v, 6.0F));
                }
                level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.3F);
            }
            case WATER -> {
                level.addFreshEntity(MagusSpellEntity.create(level, Kind.ICE_ORB, player, eye(player), look.scale(0.9), 9.0F));
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
            case FIRE -> {
                level.addFreshEntity(MagusSpellEntity.create(level, Kind.FIREBALL, player, eye(player), look.scale(0.9), 14.0F));
                level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.9F);
            }
            default -> {}
        }
        player.getCooldowns().addCooldown(stack, Accessories.has(player, Accessory.ARCANE_PENDANT) ? 10 : 12);
        player.swing(player.getUsedItemHand());
    }

    /** Linha de espinhos de pedra até onde o jogador olha (até 16 blocos). */
    private void spikes(ServerLevel level, Player player) {
        Vec3 dir = MagusSpells.flat(player.getLookAngle());
        Vec3 start = player.position();
        level.playSound(null, player.blockPosition(), SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.PLAYERS, 1.2F, 0.7F);
        for (int i = 2; i <= 16; i++) {
            Vec3 p = start.add(dir.scale(i));
            double y = MagusSpells.groundY(level, p.add(0, 2, 0));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.POINTED_DRIPSTONE.defaultBlockState()),
                    p.x, y + 0.6, p.z, 8, 0.2, 0.5, 0.2, 0.1);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p.x - 1.3, y - 0.5, p.z - 1.3, p.x + 1.3, y + 2.5, p.z + 1.3),
                    e -> canHurt(player, e))) {
                if (e.hurtServer(level, level.damageSources().playerAttack(player), 12.0F)) {
                    MagusSpellEntity.push(e, Vec3.ZERO, 0.8);
                }
            }
        }
    }

    private static boolean canHurt(Player player, LivingEntity e) {
        return e != player && e.isAlive() && !(e instanceof Player) && !(e instanceof TamableAnimal t && t.isOwnedBy(player));
    }

    private void secondary(ServerLevel level, Player player, ItemStack stack) {
        MagusElement mode = mode(stack);
        String key = "secondary_" + mode.id;
        if (!ready(player, key) || !spend(player, stack, COST_SECONDARY)) {
            return;
        }
        switch (mode) {
            case EARTH -> {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 160, 1));
                level.playSound(null, player.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.5F, 0.6F);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                        player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
                cooldown(player, key, 600);
            }
            case AIR -> {
                Vec3 from = player.getEyePosition();
                Vec3 to = from.add(player.getLookAngle().scale(14));
                BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                Vec3 dest = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().subtract(player.getLookAngle().scale(0.8)) : to;
                level.sendParticles(ParticleTypes.GUST, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
                player.teleportTo(dest.x, dest.y - 1.2, dest.z);
                player.resetFallDistance();
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false));
                level.sendParticles(ParticleTypes.GUST, dest.x, dest.y, dest.z, 1, 0, 0, 0, 0);
                level.playSound(null, BlockPos.containing(dest), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1.0F, 1.3F);
                cooldown(player, key, 80);
            }
            case WATER -> {
                player.heal(4.0F);
                player.clearFire();
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
                level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 0.8, 0.6, 0.1);
                level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 1.2F, 1.0F);
                cooldown(player, key, 400);
            }
            case FIRE -> {
                Vec3 from = player.getEyePosition();
                BlockHitResult hit = level.clip(new ClipContext(from, from.add(player.getLookAngle().scale(40)), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player));
                Vec3 at = hit.getLocation();
                Vec3 sky = at.add(2, 18, 1);
                level.addFreshEntity(MagusSpellEntity.create(level, Kind.METEOR, player, sky, at.subtract(sky).normalize().scale(1.1), 22.0F).life(60));
                level.sendParticles(new DustParticleOptions(0xFF4A1A, 1.5F), at.x, at.y + 0.2, at.z, 30, 1.5, 0.0, 1.5, 0.0);
                level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.6F);
                cooldown(player, key, 240);
            }
            default -> {}
        }
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    /** Raio do ∞: feixe em linha reta que atravessa tudo (32 blocos). */
    private void quintessence(ServerLevel level, Player player, ItemStack stack) {
        if (!ready(player, "quintessence") || !spend(player, stack, COST_QUINT)) {
            return;
        }
        Vec3 from = player.getEyePosition().add(0, -0.2, 0);
        Vec3 dir = player.getLookAngle();
        for (int i = 1; i <= 64; i++) {
            Vec3 p = from.add(dir.scale(i * 0.5));
            level.sendParticles(new DustParticleOptions(0xFFD87A, 1.6F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.01);
            }
        }
        AABB box = new AABB(from, from.add(dir.scale(32))).inflate(1.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> canHurt(player, e))) {
            Vec3 c = e.position().add(0, e.getBbHeight() / 2, 0);
            double t = c.subtract(from).dot(dir);
            if (t < 0 || t > 32 || c.distanceTo(from.add(dir.scale(t))) > 1.2 + e.getBbWidth() / 2) {
                continue;
            }
            e.hurtServer(level, level.damageSources().playerAttack(player), 30.0F);
            e.igniteForSeconds(3.0F);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.5F, 1.8F);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6F, 1.6F);
        cooldown(player, "quintessence", 600);
        player.getCooldowns().addCooldown(stack, 20);
    }

    // ----- barra e texto -----

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return mode(stack).color;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.mistech.magician_staff.mode", mode(stack).displayName()));
        tooltip.accept(Component.translatable("item.mistech.magician_staff.energy", getEnergy(stack), CAPACITY).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.mistech.magician_staff.help1").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.mistech.magician_staff.help2").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.mistech.magician_staff.help3").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.mistech.magician_staff.help4").withStyle(ChatFormatting.DARK_GRAY));
    }
}
