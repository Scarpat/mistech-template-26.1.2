package com.mistech.mistechmod.item.mithril;

import java.util.Map;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.registry.MistechEffects;
import com.mistech.mistechmod.registry.MistechTags;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * O que cada runa engastada faz. Valem as runas da armadura vestida + da peça na mão principal (só com energia).
 * "Por runa" = soma (Runa Selada vale 1,5).
 *
 * <pre>
 * Atributos (aparecem no tooltip da peça):
 *   Terra      +1 resistência de armadura por runa
 *   Ar         +4% velocidade e +1,5 de queda segura por runa
 *   Energia    +12% velocidade de mineração por runa
 *   Luz        +2 de vida máxima por runa
 *   Espaço     +0,5 de alcance (blocos e criaturas) por runa
 *   Sorte      +1 de sorte por runa (3+ runas: Fortuna)
 *   Água       +25% mineração debaixo d'água e +1 de fôlego por runa
 * Efeitos:
 *   Fogo       golpes incendeiam o alvo por 2 s por runa
 *   Gelo       golpes dão Lentidão (nível = runas, até IV) por 3 s; não congela em neve fina
 *   Escuridão  +1 de dano por runa no escuro (luz 7 ou menos)
 *   Tempo      regenera 0,1 de vida por segundo por runa
 *   Magma      Resistência ao Fogo (não soma); quem te bate pega fogo por 2 s por runa
 *   Vapor      8% por runa de desviar de projéteis (até 50%)
 *   Vazio      +0,75 de dano por runa que ignora armadura
 *   Energia Mágica  -8% de dano mágico por runa (até -60%)
 *   Eco        8% por runa (até 40%) de o golpe soltar um eco sônico: +4 de dano que ignora armadura e empurra;
 *              com qualquer Runa do Eco engastada, você não gera vibrações (sculk/Warden não percebem — {@link com.mistech.mistechmod.beyond.EchoEvents})
 *   Vigília    criaturas hostis que você olha (16 blocos) ficam lentas — nível = runas, até III ({@link com.mistech.mistechmod.beyond.PaleNetherEvents})
 *   Almas      golpes dão Murchar por 1 s por runa; 2 ou mais: Véu das Almas (mobs do Nether não te miram)
 *   Nuvens     -15% de dano de queda por runa; 3 ou mais: sem dano de queda + Queda Lenta agachado no ar ({@link com.mistech.mistechmod.sky.CloudEvents})
 * </pre>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class RuneSocketEffects {
    public static final double EARTH_TOUGHNESS = 1.0;
    public static final double AIR_SPEED = 0.04;
    public static final double AIR_FALL = 1.5;
    public static final double ENERGY_MINING = 0.12;
    public static final double LIGHT_HEALTH = 2.0;
    public static final double SPACE_REACH = 0.5;
    public static final double LUCK_LUCK = 1.0;
    public static final double LUCK_FORTUNE_AT = 3.0;
    public static final double WATER_MINING = 0.25;
    public static final double WATER_OXYGEN = 1.0;
    public static final double FIRE_SECONDS = 2.0;
    public static final int ICE_MAX_LEVEL = 4;
    public static final double DARK_DAMAGE = 1.0;
    public static final double TIME_HEAL = 0.1;
    public static final double MAGMA_SECONDS = 2.0;
    public static final double VAPOR_DODGE = 0.08;
    public static final double VAPOR_MAX = 0.5;
    public static final double VOID_DAMAGE = 0.75;
    public static final double ARCANE_REDUCTION = 0.08;
    public static final double ARCANE_MIN = 0.4;
    public static final double ECHO_CHANCE = 0.08;
    public static final double ECHO_MAX = 0.4;
    public static final float ECHO_DAMAGE = 4.0F;
    public static final double SOUL_SECONDS = 1.0;
    public static final double SOUL_VEIL_AT = 2.0;

    private RuneSocketEffects() {}

    // ------------------------------------------------------------------ atributos (por peça)

    /** Grupo de encaixe da peça: o da armadura ou a mão principal. */
    public static EquipmentSlotGroup groupOf(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null ? EquipmentSlotGroup.bySlot(equippable.slot()) : EquipmentSlotGroup.MAINHAND;
    }

    /** Runas + constelação viram modificadores da própria peça (chamado por {@link MithrilEvents}). */
    static void addModifiers(ItemAttributeModifierEvent event, ItemStack gear) {
        EquipmentSlotGroup group = groupOf(gear);
        Map<RuneElement, Double> tally = GearRunes.tally(gear);
        if (!tally.isEmpty()) {
            add(event, group, tally, RuneElement.EARTH, Attributes.ARMOR_TOUGHNESS, "earth", EARTH_TOUGHNESS, AttributeModifier.Operation.ADD_VALUE);
            add(event, group, tally, RuneElement.AIR, Attributes.MOVEMENT_SPEED, "air_speed", AIR_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            add(event, group, tally, RuneElement.AIR, Attributes.SAFE_FALL_DISTANCE, "air_fall", AIR_FALL, AttributeModifier.Operation.ADD_VALUE);
            add(event, group, tally, RuneElement.ENERGY, Attributes.BLOCK_BREAK_SPEED, "energy", ENERGY_MINING, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            add(event, group, tally, RuneElement.LIGHT, Attributes.MAX_HEALTH, "light", LIGHT_HEALTH, AttributeModifier.Operation.ADD_VALUE);
            add(event, group, tally, RuneElement.SPACE, Attributes.BLOCK_INTERACTION_RANGE, "space_block", SPACE_REACH, AttributeModifier.Operation.ADD_VALUE);
            add(event, group, tally, RuneElement.SPACE, Attributes.ENTITY_INTERACTION_RANGE, "space_entity", SPACE_REACH, AttributeModifier.Operation.ADD_VALUE);
            add(event, group, tally, RuneElement.LUCK, Attributes.LUCK, "luck", LUCK_LUCK, AttributeModifier.Operation.ADD_VALUE);
            add(event, group, tally, RuneElement.WATER, Attributes.SUBMERGED_MINING_SPEED, "water_mining", WATER_MINING, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            add(event, group, tally, RuneElement.WATER, Attributes.OXYGEN_BONUS, "water_oxygen", WATER_OXYGEN, AttributeModifier.Operation.ADD_VALUE);
        }
        GearConstellation.addModifiers(event, gear, group);
    }

    private static void add(ItemAttributeModifierEvent event, EquipmentSlotGroup group, Map<RuneElement, Double> tally, RuneElement element,
            Holder<Attribute> attribute, String name, double perRune, AttributeModifier.Operation operation) {
        double count = GearRunes.count(tally, element);
        if (count <= 0.0) {
            return;
        }
        Identifier id = Identifier.fromNamespaceAndPath(Mistech.MODID, "rune_socket." + name + "." + group.getSerializedName());
        event.addModifier(attribute, new AttributeModifier(id, perRune * count, operation), group);
    }

    // ------------------------------------------------------------------ efeitos contínuos

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel) || player.tickCount % 20 != 7) {
            return;
        }
        Map<RuneElement, Double> tally = GearRunes.tallyEquipped(player);
        if (tally.isEmpty()) {
            return;
        }
        double time = GearRunes.count(tally, RuneElement.TIME);
        if (time > 0.0 && player.getHealth() < player.getMaxHealth() && player.isAlive()) {
            player.heal((float) (TIME_HEAL * time));
        }
        if (GearRunes.count(tally, RuneElement.MAGMA) > 0.0) {
            refresh(player, MobEffects.FIRE_RESISTANCE, 0);
        }
        if (GearRunes.count(tally, RuneElement.LUCK) >= LUCK_FORTUNE_AT) {
            refresh(player, MistechEffects.FORTUNE, 0);
        }
        if (GearRunes.count(tally, RuneElement.SOUL) >= SOUL_VEIL_AT) {
            refresh(player, MistechEffects.SOUL_VEIL, 0);
        }
        if (GearRunes.count(tally, RuneElement.ICE) > 0.0 && player.getTicksFrozen() > 0) {
            player.setTicksFrozen(0);
        }
    }

    private static void refresh(Player player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() < 30 && current.getAmplifier() <= amplifier) {
            player.addEffect(new MobEffectInstance(effect, 60, amplifier, true, false, true));
        }
    }

    // ------------------------------------------------------------------ golpes e defesa

    /** Quem bateu corpo a corpo (atacante = entidade direta). */
    private static LivingEntity meleeAttacker(DamageSource source) {
        Entity direct = source.getDirectEntity();
        return direct instanceof LivingEntity living && direct == source.getEntity() ? living : null;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }

        // ----- defesa (runas de quem apanha)
        if (target instanceof Player) {
            Map<RuneElement, Double> defense = GearRunes.tallyEquipped(target);
            if (!defense.isEmpty()) {
                double vapor = GearRunes.count(defense, RuneElement.VAPOR);
                if (vapor > 0.0 && source.is(DamageTypeTags.IS_PROJECTILE)
                        && target.getRandom().nextDouble() < Math.min(VAPOR_MAX, VAPOR_DODGE * vapor)) {
                    event.setCanceled(true);
                    level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.4, 0.5, 0.4, 0.02);
                    level.playSound(null, target.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.6F);
                    return;
                }
                double arcane = GearRunes.count(defense, RuneElement.ARCANE);
                if (arcane > 0.0 && source.is(MistechTags.MAGIC_DAMAGE)) {
                    event.setAmount(event.getAmount() * (float) Math.max(ARCANE_MIN, 1.0 - ARCANE_REDUCTION * arcane));
                }
                double magma = GearRunes.count(defense, RuneElement.MAGMA);
                LivingEntity attacker = meleeAttacker(source);
                if (magma > 0.0 && attacker != null && attacker != target) {
                    attacker.igniteForSeconds((float) (MAGMA_SECONDS * magma));
                }
            }
        }

        // ----- ataque (runas de quem bate)
        LivingEntity attacker = meleeAttacker(source);
        if (!(attacker instanceof Player) || attacker == target) {
            return;
        }
        Map<RuneElement, Double> offense = GearRunes.tallyEquipped(attacker);
        if (offense.isEmpty()) {
            return;
        }
        double fire = GearRunes.count(offense, RuneElement.FIRE);
        if (fire > 0.0) {
            target.igniteForSeconds((float) (FIRE_SECONDS * fire));
        }
        double ice = GearRunes.count(offense, RuneElement.ICE);
        if (ice > 0.0) {
            int amplifier = Math.min(ICE_MAX_LEVEL, (int) Math.ceil(ice)) - 1;
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, amplifier), attacker);
            level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0, target.getZ(), 6, 0.3, 0.4, 0.3, 0.01);
        }
        double soul = GearRunes.count(offense, RuneElement.SOUL);
        if (soul > 0.0) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) (20 * SOUL_SECONDS * soul) + 10, 0), attacker);
        }
        double dark = GearRunes.count(offense, RuneElement.DARKNESS);
        if (dark > 0.0 && level.getMaxLocalRawBrightness(attacker.blockPosition()) <= 7) {
            event.setAmount(event.getAmount() + (float) (DARK_DAMAGE * dark));
        }
    }

    /** Vazio e Eco: dano extra depois da armadura (= ignora armadura). */
    @SubscribeEvent
    static void onDamage(LivingDamageEvent.Pre event) {
        LivingEntity attacker = meleeAttacker(event.getSource());
        if (!(attacker instanceof Player) || attacker == event.getEntity() || event.getNewDamage() <= 0.0F) {
            return;
        }
        Map<RuneElement, Double> offense = GearRunes.tallyEquipped(attacker);
        double echo = GearRunes.count(offense, RuneElement.ECHO);
        if (echo > 0.0 && attacker.getRandom().nextDouble() < Math.min(ECHO_MAX, ECHO_CHANCE * echo)) {
            LivingEntity target = event.getEntity();
            event.setNewDamage(event.getNewDamage() + ECHO_DAMAGE);
            net.minecraft.world.phys.Vec3 push = target.position().subtract(attacker.position()).normalize();
            double resist = 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            target.push(push.x * 1.2 * resist, 0.25 * resist, push.z * 1.2 * resist);
            target.hurtMarked = true;
            if (target.level() instanceof ServerLevel level) {
                net.minecraft.world.phys.Vec3 c = target.getBoundingBox().getCenter();
                level.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y, c.z, 1, 0.0, 0.0, 0.0, 0.0);
                level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5F, 1.6F);
            }
        }
        double voidRunes = GearRunes.count(offense, RuneElement.VOID);
        if (voidRunes > 0.0) {
            event.setNewDamage(event.getNewDamage() + (float) (VOID_DAMAGE * voidRunes));
            if (event.getEntity().level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, event.getEntity().getX(), event.getEntity().getY() + 1.0,
                        event.getEntity().getZ(), 6, 0.3, 0.4, 0.3, 0.02);
            }
        }
    }
}
