package com.mistech.mistechmod.celestial;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.block.ritual.SingleItemBlockEntity;
import com.mistech.mistechmod.entity.EvilEyeEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Ações dos rituais Tier II/III do Altar Impuro e dos rituais celestes do Santuário (campo "action" da receita).
 * Chamado por {@link com.mistech.mistechmod.ritual.RitualActions} e pela Fonte do Santuário.
 */
public final class CelestialRituals {
    public static final long VIGIL_TICKS = 3L * 24000L;
    public static final long LURE_TICKS = 14000L;
    public static final long BLESSING_TICKS = 24000L;
    public static final int EXORCISM_RADIUS = 16;
    public static final int VILLAGE_RADIUS = 32;

    private CelestialRituals() {}

    /** Confere antes de começar. @return mensagem de erro, ou null se pode. */
    public static @Nullable Component check(String action, ServerLevel level, BlockPos pos, @Nullable Player player, @Nullable ItemStack catalyst) {
        MinecraftServer server = level.getServer();
        boolean overworldOnly = switch (action) {
            case "long_night", "solstice", "blood_moon", "silver_moon", "banish_blood_moon", "eclipse", "eternal_night", "starfall",
                 "aurora", "blessed_rain" -> true;
            default -> false;
        };
        if (overworldOnly && level.dimension() != Level.OVERWORLD) {
            return Component.translatable("message.mistech.celestial.overworld_only");
        }
        return switch (action) {
            case "blood_moon" -> Celestial.isActive(level, CelestialEvent.BLOOD_MOON) ? already(CelestialEvent.BLOOD_MOON) : null;
            case "silver_moon" -> Celestial.isActive(level, CelestialEvent.SILVER_MOON) ? already(CelestialEvent.SILVER_MOON) : null;
            case "eclipse" -> Celestial.isScheduled(server, CelestialEvent.ECLIPSE) ? already(CelestialEvent.ECLIPSE) : null;
            case "aurora" -> Celestial.isActive(level, CelestialEvent.AURORA) ? already(CelestialEvent.AURORA) : null;
            case "starfall" -> Celestial.isActive(level, CelestialEvent.STARFALL) ? already(CelestialEvent.STARFALL) : null;
            case "eternal_night" -> Celestial.isScheduled(server, CelestialEvent.ETERNAL_NIGHT) ? already(CelestialEvent.ETERNAL_NIGHT) : null;
            case "banish_blood_moon" -> !Celestial.isActive(level, CelestialEvent.BLOOD_MOON)
                    ? Component.translatable("message.mistech.celestial.no_blood_moon") : null;
            case "impure_bond" -> catalyst == null || upgraded(catalyst) == null
                    ? Component.translatable("message.mistech.celestial.bond_nothing") : null;
            case "village_blessing", "exorcism" -> player == null ? Component.translatable("message.mistech.ritual.no_player") : null;
            default -> com.mistech.mistechmod.folklore.FolkloreRituals.check(action, level, pos, player);
        };
    }

    private static Component already(CelestialEvent event) {
        return Component.translatable("message.mistech.celestial.already", Component.translatable(event.translationKey()));
    }

    /** @return true se a ação era daqui. */
    public static boolean run(String action, ServerLevel level, BlockPos pos, @Nullable SingleItemBlockEntity holder, @Nullable Player player) {
        MinecraftServer server = level.getServer();
        long t = Celestial.now(server);
        switch (action) {
            case "long_night" -> {
                Celestial.start(server, CelestialEvent.LONG_NIGHT, Celestial.nights(t, 3));
                announce(server, CelestialEvent.LONG_NIGHT, SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.5F);
            }
            case "solstice" -> {
                Celestial.start(server, CelestialEvent.SOLSTICE, Celestial.days(t, 3));
                announce(server, CelestialEvent.SOLSTICE, SoundEvents.BEACON_POWER_SELECT, 1.2F);
            }
            case "blood_moon" -> {
                Celestial.start(server, CelestialEvent.BLOOD_MOON, Celestial.tonight(t));
                announce(server, CelestialEvent.BLOOD_MOON, SoundEvents.WITHER_SPAWN, 0.6F);
            }
            case "eternal_night" -> {
                CelestialData.Window blood = Celestial.window(server, CelestialEvent.BLOOD_MOON);
                Celestial.start(server, CelestialEvent.ETERNAL_NIGHT, blood != null ? blood : Celestial.tonight(t));
                announce(server, CelestialEvent.ETERNAL_NIGHT, SoundEvents.WARDEN_EMERGE, 0.6F);
            }
            case "silver_moon", "banish_blood_moon" -> {
                CelestialData.Window night = Celestial.tonight(t);
                // vai até o começo da manhã seguinte (a Pisadeira não vem depois de uma Lua de Prata)
                Celestial.start(server, CelestialEvent.SILVER_MOON, new CelestialData.Window(night.start(), night.end() + 2000));
                announce(server, CelestialEvent.SILVER_MOON, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F);
            }
            case "eclipse" -> {
                Celestial.start(server, CelestialEvent.ECLIPSE, Celestial.today(t));
                announce(server, CelestialEvent.ECLIPSE, SoundEvents.END_PORTAL_SPAWN, 0.5F);
            }
            case "starfall" -> {
                Celestial.start(server, CelestialEvent.STARFALL, Celestial.tonight(t));
                announce(server, CelestialEvent.STARFALL, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 0.8F);
                if (player != null) {
                    CelestialEffects.launchMeteor(level, player);
                }
            }
            case "aurora" -> {
                Celestial.start(server, CelestialEvent.AURORA, Celestial.tonight(t));
                announce(server, CelestialEvent.AURORA, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F);
            }
            case "blessed_rain" -> {
                server.setWeatherParameters(0, 24000, true, false);
                Celestial.start(server, CelestialEvent.BLESSED_RAIN, new CelestialData.Window(t, t + 24000));
                announce(server, CelestialEvent.BLESSED_RAIN, SoundEvents.BEACON_ACTIVATE, 1.4F);
            }
            case "lure" -> {
                CelestialEffects.addLure(level, pos, LURE_TICKS);
                tell(player, Component.translatable("message.mistech.celestial.lure").withStyle(ChatFormatting.DARK_RED));
            }
            case "vigil" -> {
                CelestialEffects.addVigil(level, pos, VIGIL_TICKS);
                tell(player, Component.translatable("message.mistech.celestial.vigil").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            case "impure_bond" -> {
                if (holder != null) {
                    ItemStack book = holder.getStored();
                    ItemEnchantments better = upgraded(book);
                    if (better != null) {
                        ItemStack copy = book.copy();
                        copy.set(DataComponents.STORED_ENCHANTMENTS, better);
                        holder.setStored(copy);
                    }
                }
            }
            case "exorcism" -> exorcism(level, pos, player);
            case "village_blessing" -> villageBlessing(level, pos, player);
            default -> {
                return com.mistech.mistechmod.folklore.FolkloreRituals.run(action, level, pos, player);
            }
        }
        return true;
    }

    /** Cada encantamento do livro sobe 1 nível, até um acima do máximo. null = nada para subir. */
    public static @Nullable ItemEnchantments upgraded(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return null;
        }
        ItemEnchantments current = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable out = new ItemEnchantments.Mutable(current);
        boolean any = false;
        for (var entry : current.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int lvl = entry.getIntValue();
            int max = enchantment.value().getMaxLevel();
            if (lvl <= max) {
                out.set(enchantment, lvl + 1);
                any = true;
            }
        }
        return any ? out.toImmutable() : null;
    }

    private static void exorcism(ServerLevel level, BlockPos pos, @Nullable Player starter) {
        List<Player> players = new java.util.ArrayList<>(level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(EXORCISM_RADIUS)));
        if (starter != null && !players.contains(starter)) {
            players.add(starter);
        }
        for (Player p : players) {
            p.removeEffect(MobEffects.UNLUCK);
            p.removeEffect(MobEffects.WEAKNESS);
            p.removeEffect(MobEffects.HUNGER);
            p.removeEffect(MobEffects.WITHER);
            p.removeEffect(MobEffects.POISON);
            p.removeEffect(MobEffects.NAUSEA);
            p.removeEffect(MobEffects.BLINDNESS);
            p.removeEffect(MobEffects.DARKNESS);
            Celestial.bless(p, BLESSING_TICKS);
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.3, 0.8, 0.3, 0.05);
            p.sendSystemMessage(Component.translatable("message.mistech.celestial.exorcism").withStyle(ChatFormatting.GOLD));
        }
        for (EvilEyeEntity eye : level.getEntitiesOfClass(EvilEyeEntity.class, new AABB(pos).inflate(48))) {
            level.sendParticles(ParticleTypes.END_ROD, eye.getX(), eye.getY() + 0.5, eye.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
            eye.discard();
        }
    }

    private static void villageBlessing(ServerLevel level, BlockPos pos, @Nullable Player starter) {
        AABB area = new AABB(pos).inflate(VILLAGE_RADIUS, 12, VILLAGE_RADIUS);
        if (starter != null) {
            // mesma rotina da maçã dourada + Fraqueza (como no frasco de Essência Purificada), só que em todos
            for (ZombieVillager zombie : level.getEntitiesOfClass(ZombieVillager.class, area, z -> !z.isConverting())) {
                zombie.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
                ItemStack keep = starter.getItemInHand(InteractionHand.MAIN_HAND);
                starter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE));
                zombie.mobInteract(starter, InteractionHand.MAIN_HAND);
                starter.setItemInHand(InteractionHand.MAIN_HAND, keep);
                if (zombie.isConverting()) {
                    zombie.setVillagerConversionTime(20 + level.getRandom().nextInt(40));
                    level.sendParticles(ParticleTypes.END_ROD, zombie.getX(), zombie.getY() + 1.0, zombie.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
                }
            }
        }
        for (Villager villager : level.getEntitiesOfClass(Villager.class, area)) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.8, villager.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        }
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(EXORCISM_RADIUS))) {
            p.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 20 * 60 * 20, 0));
            p.sendSystemMessage(Component.translatable("message.mistech.celestial.village").withStyle(ChatFormatting.GOLD));
        }
    }

    /** Avisa todo mundo no overworld (o céu é de todos). */
    private static void announce(MinecraftServer server, CelestialEvent event, SoundEvent sound, float pitch) {
        Component msg = Component.translatable("message.mistech.celestial.start." + event.getSerializedName()).withStyle(color(event));
        for (ServerPlayer p : server.overworld().players()) {
            p.sendSystemMessage(msg);
            server.overworld().playSound(null, p.blockPosition(), sound, SoundSource.AMBIENT, 0.8F, pitch);
        }
    }

    private static ChatFormatting color(CelestialEvent event) {
        return switch (event) {
            case BLOOD_MOON, ETERNAL_NIGHT -> ChatFormatting.DARK_RED;
            case SILVER_MOON -> ChatFormatting.WHITE;
            case SOLSTICE, BLESSED_RAIN -> ChatFormatting.GOLD;
            case ECLIPSE, LONG_NIGHT -> ChatFormatting.DARK_PURPLE;
            case AURORA -> ChatFormatting.GREEN;
            case STARFALL -> ChatFormatting.AQUA;
        };
    }

    private static void tell(@Nullable Player player, Component msg) {
        if (player != null) {
            player.sendSystemMessage(msg);
        }
    }
}
