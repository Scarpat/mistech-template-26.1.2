package com.mistech.mistechmod.accessory;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.astrology.AstrologyData;
import com.mistech.mistechmod.astrology.AstrologyPerks;
import com.mistech.mistechmod.astrology.Constellations;
import com.mistech.mistechmod.registry.AccessoryContent;
import com.mistech.mistechmod.registry.MistechAttachments;
import com.mistech.mistechmod.registry.MistechComponents;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Acessórios: 1 amuleto + 2 anéis por jogador (attachment {@code mistech:accessories}, mantido ao morrer).
 * Abre com a tecla J. Efeitos iguais não somam.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class Accessories {
    public static final int SLOT_AMULET = 0;
    public static final int SIZE = 3;

    private static final Identifier BREEZE_FALL = id("breeze_fall");
    private static final Identifier BREEZE_JUMP = id("breeze_jump");
    private static final Identifier TIDE_SWIM = id("tide_swim");
    private static final Identifier ARCANE_ARMOR = id("arcane_armor");
    private static final Identifier ARCANE_TOUGHNESS = id("arcane_toughness");
    /** Pingente do Arcano: +20% de dano mágico causado, -20% recebido, multiplicador de afinidade dos 4 elementos. */
    public static final float ARCANE_DAMAGE_BONUS = 1.2F;
    public static final float ARCANE_DAMAGE_TAKEN = 0.8F;
    public static final double ARCANE_AFFINITY = 1.5;

    private Accessories() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Mistech.MODID, "accessory_" + path);
    }

    public static List<ItemStack> get(Player player) {
        return player.getData(MistechAttachments.ACCESSORIES);
    }

    /**
     * Tudo o que está equipado: os 3 espaços do Mistech + os espaços do Curios (se ele estiver instalado).
     * Guardado por tick para não perguntar ao Curios várias vezes no mesmo tick.
     */
    public static List<ItemStack> all(Player player) {
        if (!CuriosCompat.active()) {
            return get(player);
        }
        long now = player.level().getGameTime();
        Cached cached = CACHE.get(player.getUUID());
        if (cached != null && cached.tick == now && cached.client == player.level().isClientSide()) {
            return cached.stacks;
        }
        List<ItemStack> curios = CuriosCompat.equipped(player);
        List<ItemStack> list;
        if (curios.isEmpty()) {
            list = get(player);
        } else {
            list = new java.util.ArrayList<>(get(player));
            list.addAll(curios);
        }
        CACHE.put(player.getUUID(), new Cached(now, player.level().isClientSide(), list));
        return list;
    }

    private record Cached(long tick, boolean client, List<ItemStack> stacks) {}
    private static final java.util.Map<java.util.UUID, Cached> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /** Primeiro acessório desse tipo equipado (espaços do Mistech ou do Curios), ou vazio. */
    public static ItemStack find(Player player, Accessory accessory) {
        for (ItemStack stack : all(player)) {
            if (stack.getItem() instanceof AccessoryItem item && item.accessory() == accessory) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean has(Player player, Accessory accessory) {
        for (ItemStack stack : all(player)) {
            if (stack.getItem() instanceof AccessoryItem item && item.accessory() == accessory) {
                return true;
            }
        }
        return false;
    }

    /** Constelação gravada no Amuleto de Constelação equipado (-1 = nenhuma). */
    public static int boundConstellation(Player player) {
        ItemStack amulet = find(player, Accessory.CONSTELLATION_AMULET);
        if (!amulet.isEmpty()) {
            return amulet.getOrDefault(MistechComponents.AMULET_CONSTELLATION.get(), -1);
        }
        return -1;
    }

    /** Custo das magias do Cajado do Gnomo com o Amuleto da Essência (-25%). */
    public static int staffCost(Player player, int amount) {
        amount = com.mistech.mistechmod.mage.MageRobes.cost(player, amount); // Vestes de Mago
        if (has(player, Accessory.ARCANE_PENDANT)) {
            amount = (int) Math.ceil(amount * 0.8);
        }
        return has(player, Accessory.ESSENCE_AMULET) ? (int) Math.ceil(amount * 0.75) : amount;
    }

    /** Pingente do Arcano: pontos de afinidade dos quatro elementos rendem 50% a mais. */
    public static int affinityGain(Player player, com.mistech.mistechmod.item.RuneElement element, int amount) {
        boolean classic = element == com.mistech.mistechmod.item.RuneElement.FIRE || element == com.mistech.mistechmod.item.RuneElement.WATER
                || element == com.mistech.mistechmod.item.RuneElement.EARTH || element == com.mistech.mistechmod.item.RuneElement.AIR;
        return classic && has(player, Accessory.ARCANE_PENDANT) ? (int) Math.ceil(amount * ARCANE_AFFINITY) : amount;
    }

    /** Dano "mágico": magia/magia indireta e o que vem das magias do Tarô. */
    public static boolean isMagicDamage(net.minecraft.world.damagesource.DamageSource source) {
        return source.is(DamageTypeTags.WITCH_RESISTANT_TO)
                || source.getDirectEntity() instanceof com.mistech.mistechmod.tarot.magus.MagusSpellEntity
                || source.getDirectEntity() instanceof com.mistech.mistechmod.tarot.fool.FoolTornadoEntity;
    }

    /** Chamado quando os acessórios mudam: atualiza atributos e bônus da astrologia. */
    public static void refresh(Player player) {
        setModifier(player, Attributes.SAFE_FALL_DISTANCE, BREEZE_FALL, has(player, Accessory.BREEZE_RING) ? 9.0 : 0.0);
        setModifier(player, Attributes.JUMP_STRENGTH, BREEZE_JUMP, has(player, Accessory.BREEZE_RING) ? 0.08 : 0.0);
        setModifier(player, Attributes.WATER_MOVEMENT_EFFICIENCY, TIDE_SWIM, has(player, Accessory.TIDE_RING) ? 0.5 : 0.0);
        boolean arcane = has(player, Accessory.ARCANE_PENDANT);
        setModifier(player, Attributes.ARMOR, ARCANE_ARMOR, arcane ? 4.0 : 0.0);
        setModifier(player, Attributes.ARMOR_TOUGHNESS, ARCANE_TOUGHNESS, arcane ? 2.0 : 0.0);
        AstrologyPerks.apply(player);
    }

    private static void setModifier(Player player, Holder<Attribute> attribute, Identifier id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void keep(Player player, Holder<MobEffect> effect) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() < 60) {
            player.addEffect(new MobEffectInstance(effect, 220, 0, true, false, true));
        }
    }

    @SubscribeEvent
    static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        CACHE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (has(player, Accessory.FROST_RING)) {
            freezeWater(serverPlayer);
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        if (player.tickCount % 200 == 0 || CuriosCompat.active()) {
            refresh(player); // garante os atributos depois de morrer / trocar de dimensão (e ao trocar algo no Curios)
        }
        if (has(player, Accessory.VEIL_RING)) {
            keep(player, MistechEffects.INCENSE_VEIL);
        }
        if (has(player, Accessory.STONE_RING)) {
            keep(player, MobEffects.HASTE);
        }
        if (has(player, Accessory.TIDE_RING)) {
            keep(player, MobEffects.WATER_BREATHING);
        }
        if (has(player, Accessory.EMBER_RING) && player.isOnFire()) {
            player.clearFire();
        }
    }

    /** Anel da Geada: como Passos Gelados, congela a água parada em volta dos pés (raio 2). */
    private static void freezeWater(ServerPlayer player) {
        if (!player.onGround() || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.level();
        BlockPos feet = player.blockPosition();
        BlockState frosted = Blocks.FROSTED_ICE.defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, -1, 2))) {
            if (!pos.closerToCenterThan(player.position(), 2.5)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.WATER) && level.getFluidState(pos).isSourceOfType(Fluids.WATER)
                    && level.getBlockState(pos.above()).isAir()
                    && frosted.canSurvive(level, pos) && level.isUnobstructed(frosted, pos, CollisionContext.empty())) {
                level.setBlockAndUpdate(pos, frosted);
                level.scheduleTick(pos, Blocks.FROSTED_ICE, 60 + player.getRandom().nextInt(60));
            }
        }
    }

    /** Anel da Brasa: -50% de dano de fogo. */
    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getSource().is(DamageTypeTags.IS_FIRE)
                && has(player, Accessory.EMBER_RING)) {
            event.setAmount(event.getAmount() * 0.5F);
        }
        // Pingente do Arcano
        if (isMagicDamage(event.getSource())) {
            if (event.getEntity() instanceof Player player && has(player, Accessory.ARCANE_PENDANT)) {
                event.setAmount(event.getAmount() * ARCANE_DAMAGE_TAKEN);
            }
            if (event.getSource().getEntity() instanceof Player attacker && has(attacker, Accessory.ARCANE_PENDANT)) {
                event.setAmount(event.getAmount() * ARCANE_DAMAGE_BONUS);
            }
        }
    }

    /**
     * Amuleto de Constelação: clicar na Mesa de Astrologia grava (ou troca para a próxima) constelação COMPLETA.
     */
    @SubscribeEvent
    static void onUseTable(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(AccessoryContent.CONSTELLATION_AMULET.get())
                || !event.getLevel().getBlockState(event.getPos()).is(Mistech.ASTROLOGY_TABLE.get())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        Player player = event.getEntity();
        if (event.getLevel().isClientSide()) {
            return;
        }
        AstrologyData data = player.getData(MistechAttachments.ASTROLOGY);
        int size = Constellations.ALL.size();
        int current = stack.getOrDefault(MistechComponents.AMULET_CONSTELLATION.get(), -1);
        for (int step = 1; step <= size; step++) {
            int next = Math.floorMod(current + step, size);
            if (data.countIn(next) >= Constellations.get(next).stars().size()) {
                stack.set(MistechComponents.AMULET_CONSTELLATION.get(), next);
                player.sendOverlayMessage(Component.translatable("message.mistech.constellation_amulet.bound", Constellations.get(next).name()));
                event.getLevel().playSound(null, event.getPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
                return;
            }
        }
        player.sendOverlayMessage(Component.translatable("message.mistech.constellation_amulet.none"));
    }
}
