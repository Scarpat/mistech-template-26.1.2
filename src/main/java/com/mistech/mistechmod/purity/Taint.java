package com.mistech.mistechmod.purity;

import com.mistech.mistechmod.Mistech;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Mácula: corrupção do jogador (0 a 100), salva e mantida ao morrer. Sobe com magia impura
 * e desce com sol, arruda, sal e magia pura. O valor de verdade fica em {@link PurityContent#TAINT} (só no servidor);
 * o cliente recebe uma cópia em décimos ({@link PurityContent#TAINT_CLIENT}) uma vez por segundo.
 */
public final class Taint {
    public static final float MAX = 100.0F;
    /** Limite das formas de limpeza do early game (sol, arruda, sal): só a magia pura passa daqui. */
    public static final float EARLY_FLOOR = 20.0F;

    public static final Identifier HEALTH_MODIFIER = Identifier.fromNamespaceAndPath(Mistech.MODID, "taint_health");

    /** Faixas da Mácula. */
    public enum Stage {
        PURE(0, 0xFFF5E6A8, ChatFormatting.YELLOW, 0),
        STAINED(20, 0xFFD8C4F0, ChatFormatting.LIGHT_PURPLE, 0),
        TAINTED(40, 0xFFA066D8, ChatFormatting.DARK_PURPLE, -4),
        CORRUPTED(60, 0xFF6A2A9A, ChatFormatting.DARK_PURPLE, -8),
        PROFANE(80, 0xFF3A0F2A, ChatFormatting.DARK_RED, -12);

        public final int from;
        public final int color;
        public final ChatFormatting format;
        /** Vida máxima tirada nesta faixa. */
        public final int health;

        Stage(int from, int color, ChatFormatting format, int health) {
            this.from = from;
            this.color = color;
            this.format = format;
            this.health = health;
        }

        public static Stage of(float taint) {
            Stage result = PURE;
            for (Stage stage : values()) {
                if (taint >= stage.from) {
                    result = stage;
                }
            }
            return result;
        }

        public String id() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }

        public Component displayName() {
            return Component.translatable("taint.mistech.stage." + this.id()).withStyle(this.format);
        }

        /** Eficiência dos artefatos puros nesta faixa (1 = total, 0 = falham). */
        public float pureEfficiency() {
            return switch (this) {
                case PURE -> 1.0F;
                case STAINED -> 0.75F;
                case TAINTED -> 0.5F;
                default -> 0.0F;
            };
        }
    }

    private Taint() {}

    public static float get(Player player) {
        return player.getData(PurityContent.TAINT);
    }

    public static Stage stage(Player player) {
        return Stage.of(get(player));
    }

    /** Valor visto pelo cliente (o próprio jogador). */
    public static float clientValue(Player player) {
        return player.getData(PurityContent.TAINT_CLIENT) / 10.0F;
    }

    public static void set(Player player, float value) {
        if (player.level().isClientSide()) {
            return;
        }
        float clamped = Math.max(0.0F, Math.min(MAX, value));
        if (clamped != get(player)) {
            player.setData(PurityContent.TAINT, clamped);
        }
    }

    /** Soma Mácula (servidor). Jogadores no criativo/espectador não se maculam. */
    public static void add(Player player, float amount) {
        if (amount <= 0 || player.isCreative() || player.isSpectator()) {
            return;
        }
        amount = com.mistech.mistechmod.mage.MageRobes.taint(player, amount); // Vestes do Arquimago
        set(player, get(player) + amount);
    }

    /** Tira Mácula sem descer abaixo de {@code floor}. Se já estiver abaixo, não mexe. */
    public static void reduce(Player player, float amount, float floor) {
        float current = get(player);
        if (amount <= 0 || current <= floor) {
            return;
        }
        set(player, Math.max(floor, current - amount));
    }

    /** Limpeza do early game (sol, arruda, sal): para em {@link #EARLY_FLOOR}. */
    public static void cleanse(Player player, float amount) {
        reduce(player, amount, EARLY_FLOOR);
    }

    /** Limpeza da magia pura: pode chegar a zero. */
    public static void purify(Player player, float amount) {
        reduce(player, amount, 0.0F);
    }

    /** Artefato puro funciona para este jogador? (falha a partir de Corrompida). Avisa e devolve false se falhar. */
    public static boolean pureWorks(Player player) {
        if (stage(player).pureEfficiency() > 0) {
            return true;
        }
        player.sendOverlayMessage(Component.translatable("message.mistech.taint.pure_fails").withStyle(ChatFormatting.DARK_PURPLE));
        return false;
    }

    /** Céu aberto acima da posição (nada que bloqueie movimento por cima). */
    public static boolean openSky(Level level, BlockPos pos) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY();
    }

    /** Sol direto: de dia, sem chuva e com céu aberto. */
    public static boolean inSunlight(Level level, BlockPos pos) {
        return level.isBrightOutside() && !level.isRainingAt(pos) && openSky(level, pos);
    }

    /** Aplica a vida máxima da faixa (só troca o modificador quando o valor muda). */
    static void applyHealth(Player player) {
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) {
            return;
        }
        int wanted = stage(player).health;
        AttributeModifier existing = instance.getModifier(HEALTH_MODIFIER);
        double current = existing == null ? 0 : existing.amount();
        if (current == wanted) {
            return;
        }
        instance.removeModifier(HEALTH_MODIFIER);
        if (wanted != 0) {
            instance.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER, wanted, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Texto "Mácula 37 — Manchada". */
    public static Component describe(float taint) {
        Stage stage = Stage.of(taint);
        return Component.translatable("message.mistech.taint.value", String.format(java.util.Locale.ROOT, "%.1f", taint), stage.displayName());
    }
}
