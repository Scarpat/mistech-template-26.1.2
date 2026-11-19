package com.mistech.mistechmod.spellcraft.fx;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.spellcraft.Spell;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Efeitos oníricos das magias: cada magia daqui tem um efeito só dela, desenhado e animado no cliente
 * ({@code spellcraft.client.fx.DreamFxClient}), no lugar dos blocos e partículas genéricos. As magias listadas em
 * {@link #OWN} não recebem o círculo/runas padrão do lançamento nem a assinatura antiga do {@code SpellVisuals}.
 */
public final class DreamFx {
    /** Fagulha: mariposas de brasa voando até o alvo. b = velocidade por tick, power = alcance. */
    public static final int MOTH_LAUNCH = 1;
    /** Fagulha acertou: as mariposas se espalham e viram cinza. */
    public static final int MOTH_HIT = 2;
    /** Penumbra: um olho abre ao lado do mago e outro voa até o alvo. b = velocidade por tick, power = alcance. */
    public static final int EYE_LAUNCH = 3;
    /** Penumbra acertou: olhos abrem em volta da vítima e a encaram. */
    public static final int EYE_HIT = 4;
    /** Piscar: uma pálpebra rasga o ar em a (origem) e em b (destino). */
    public static final int BLINK = 5;
    /** Lentidão Temporal (a cada pulso): mostrador de algarismos que anda aos trancos e areia parada no ar. */
    public static final int TIME_PULSE = 6;
    /** Floração: jardim em espiral que abre, respira e se recolhe. */
    public static final int BLOOM = 7;
    /** Luz Sagrada: rodas de olhos girando acima do mago e penas caindo. power = raio. */
    public static final int HALO = 8;

    /** Explosão de Fogo: um casulo de mariposas voa de a até b em {@code power} ticks. */
    public static final int COCOON = 9;
    /** Explosão de Fogo chegou: o casulo abre numa mariposa enorme que queima. power = raio. */
    public static final int COCOON_HIT = 10;
    /** Anel de Brasas: ciranda de gente de fogo em volta do mago. power = raio. */
    public static final int CIRANDA = 11;
    /** Meteoro: um sol de olho fechado cai em a depois de {@code power} ticks; b.x = tamanho, b.y = raio. */
    public static final int SUN_FALL = 12;
    /** Fênix: um ovo de brasa racha onde a ave nasce. */
    public static final int EGG = 13;
    /** Inferno: mariposa gigante paira sobre a e três cirandas se abrem. power = raio. */
    public static final int INFERNO = 14;
    /** Véu de Sombras: mãos saem do chão e se fecham sobre o mago. */
    public static final int VEIL = 15;
    /** Dreno: uma boca voa até o alvo. b = velocidade por tick, power = alcance. */
    public static final int MOUTH_LAUNCH = 16;
    /** Dreno acertou: a boca morde. */
    public static final int MOUTH_HIT = 17;
    /** Dreno: o que foi mordido volta em gotas de a até a entidade (o mago). */
    public static final int FEED = 18;
    /** Lobo das Sombras: uma poça escura se abre no chão, com olhos dentro. */
    public static final int PUDDLE = 19;
    /** Eclipse Total: um olho enorme abre no céu e uma roda de olhos cerca a área. power = raio. */
    public static final int ECLIPSE = 20;

    /** Carga (a cada 2 ticks enquanto segura): id = elemento, power = carga de 0 a 1, entidade = mago. */
    public static final int CHARGE = 21;
    /** Carga completa. id = elemento. */
    public static final int CHARGE_FULL = 22;
    /** Ritual começou: id = elemento, power = duração em ticks, entidade = mago. A cena acaba sozinha se ele parar. */
    public static final int RITUAL = 23;

    // ---------- Gelo: cacos de espelho, máscaras de porcelana, geada
    /** Estilhaço de Gelo: caco de espelho voando. b = velocidade por tick, power = alcance. */
    public static final int SHARD_LAUNCH = 24;
    public static final int SHARD_HIT = 25;
    /** Nova de Gelo: geada se abre no chão e máscaras sobem dela. power = raio. */
    public static final int FROST_NOVA = 26;
    /** Nevasca (a cada pulso): cacos e máscaras girando no vento. power = raio. */
    public static final int BLIZZARD = 27;
    /** Era Glacial: uma máscara enorme sobe, acorda e sopra; cacos brotam por toda a área. power = raio. */
    public static final int ICE_AGE = 28;
    // ---------- Água: peixes no ar, bolhas, águas-vivas, baleia
    /** Jato d'Água: um peixe nada pelo ar até o alvo. */
    public static final int FISH_LAUNCH = 29;
    public static final int FISH_HIT = 30;
    /** Cura d'Água: carpas sobem em volta da entidade. */
    public static final int MEND = 31;
    /** Purificar: águas-vivas sobem levando o que estava sujo. power = raio. */
    public static final int PURIFY = 32;
    /** Maremoto: uma baleia e um cardume atravessam o ar a partir de a, na direção b. */
    public static final int WHALE = 33;
    // ---------- Terra: cabeças e dentes de pedra, seixos, rachaduras
    /** Pedregulho: uma cabeça de pedra voa girando. */
    public static final int HEAD_LAUNCH = 34;
    public static final int HEAD_HIT = 35;
    /** Pele de Pedra: dentes de pedra sobem em volta e se fecham. */
    public static final int STONE_SKIN = 36;
    /** Golem de Pedra: uma cabeça sai do chão e abre os olhos. */
    public static final int GOLEM_RISE = 37;
    /** Terremoto (a cada pulso): rachaduras, dentes e seixos pulando. power = raio. */
    public static final int QUAKE = 38;
    /** Cataclismo: fileiras de dentes abrem como uma boca; três cabeças sobem. power = raio. */
    public static final int CATACLYSM = 39;

    // ---------- Ar: pássaros de papel, redemoinhos, folhas
    /** Rajada: revoada de pássaros de papel a partir de a, na direção b. */
    public static final int GUST = 40;
    /** Corrente Ascendente: os pássaros sobem em espiral levando a entidade. */
    public static final int UPDRAFT = 41;
    /** Passo de Vento: os pássaros acompanham a entidade no arranque. b = direção. */
    public static final int WIND_STEP = 42;
    /** Furacão (a cada pulso): funil de pássaros e papel em a. power = raio. */
    public static final int HURRICANE = 43;
    // ---------- Energia: agulha e linha
    /** Uma agulha costura um fio de luz de a até b. id = espera em ticks, power = grossura (1 = normal). */
    public static final int STITCH = 44;
    /** Uma agulha cai do céu em a, puxando o fio. b.x = espera em ticks, power = tamanho. */
    public static final int NEEDLE_FALL = 45;
    /** Tempestade: teia de fios no céu sobre a. */
    public static final int STORM_WEB = 46;
    // ---------- Luz (resto)
    /** Vislumbre: roda pequena sobre o mago e penas voando para fora. power = raio. */
    public static final int GLIMMER = 47;
    /** Vislumbre: roda pequena marcando uma criatura revelada (entidade). */
    public static final int GLIMMER_MARK = 48;
    /** Orbe de Luz: uma rodinha se recolhe no orbe em a. */
    public static final int LIGHT_ORB = 49;
    /** Alvorada: as rodas sobem no céu, fachos de luz e chuva de penas. power = raio. */
    public static final int DAWN = 50;

    // ---------- Tempo (resto): chave de corda, relógios de bolso, ampulheta
    /** Acelerar: uma chave de corda gira nas costas da entidade e os algarismos disparam em volta. */
    public static final int QUICKEN = 51;
    /** Presságio: três relógios de bolso orbitam a cabeça, cada um numa hora. */
    public static final int FORESIGHT = 52;
    /** Rebobinar: de a (agora) até b (5 s atrás): ampulhetas viram e os algarismos contam para trás no caminho. */
    public static final int REWIND = 53;
    /** Parar o Tempo: o mostrador dá três trancos e trava; a areia fica parada no ar. power = raio. */
    public static final int TIME_STOP = 54;
    /** Parar o Tempo: relógio parado sobre uma criatura presa (entidade). */
    public static final int TIME_MARK = 55;
    /** Instante Eterno: ampulheta girando sem parar sobre o mago, dois mostradores em sentidos opostos. */
    public static final int ETERNAL = 56;
    // ---------- Espaço (resto): pálpebras, estrelas, constelações
    /** Puxão: uma linha de estrelas de a (alvo) até b (mão do mago), com uma pálpebra em cada ponta. */
    public static final int PULL = 57;
    /** Telecinese: constelações se fecham sobre a entidade. power = raio. */
    public static final int TELEKINESIS = 58;
    /** Âncora marcada: o chão pisca em a e uma estrela fica presa ali. */
    public static final int ANCHOR_SET = 59;
    /** Fenda: uma pálpebra pequena abre em a, virada para b (direção do olhar), e fica aberta um tempo. */
    public static final int RIFT = 60;
    /** Buraco Negro: a pupila. Em a, por b.x ticks; power = raio. */
    public static final int BLACK_HOLE = 61;
    // ---------- Vida (resto): cogumelos, cipó, espinheiro, vagem, a árvore
    /** Broto: cogumelos nascem em volta. power = raio. */
    public static final int SPROUT = 62;
    /** Cipó: desenha o cipó de a até b por {@code power} ticks (mandado de novo enquanto a magia dura). */
    public static final int VINE = 63;
    /** Cipó prendeu em a: folhas e uma flor. */
    public static final int VINE_LATCH = 64;
    /** Espinhos: espinheiros sobem do chão na área. power = raio. */
    public static final int THORNS = 65;
    /** Espinhos: o espinheiro fecha em volta de uma criatura (entidade). */
    public static final int THORN_BIND = 66;
    /** Guardião de madeira: uma vagem sai do chão e abre. */
    public static final int POD = 67;
    /** Árvore da Vida: a árvore cresce em a e fica por b.x ticks. power = raio. */
    public static final int TREE = 68;

    // ---------- Magma, Vapor, Nuvem, Alma, Arcano, Vigília, Sorte, Eco, Vazio
    /** Rio de Lava: rachadura acesa de a na direção b, com gente de fogo em procissão. power = duração em ticks. */
    public static final int LAVA_RIVER = 69;
    /** Erupção: uma cabeça de pedra sobe em a e vomita brasa. power = raio. */
    public static final int ERUPTION = 70;
    /** Armadura de Magma: rachaduras sob os pés, seixos em brasa e mariposas fecham na entidade. */
    public static final int MAGMA_ARMOR = 71;
    /** Névoa: fantasmas de vapor rodam em volta da entidade. power = duração em ticks. */
    public static final int MIST = 72;
    /** Jato de Vapor: fantasmas de vapor disparam de a na direção b. */
    public static final int STEAM_JET = 73;
    /** Plataforma de Nuvem: ovelhas de nuvem pulam para fora de a. */
    public static final int SHEEP_PLATFORM = 74;
    /** Ponte de Nuvem: ovelhas pulam em fila de a, na direção b, por {@code power} blocos. */
    public static final int SHEEP_BRIDGE = 75;
    /** Espírito Guardião: uma vela acende no ar em a. */
    public static final int CANDLE = 76;
    /** Colheita de Almas: uma alma sai de a e vai até a entidade (o mago). */
    public static final int SOUL_TAKE = 77;
    /** Colheita de Almas: roda de velas em volta da entidade. power = raio. */
    public static final int SOUL_HARVEST = 78;
    /** Recarregar: engrenagens de luz giram em volta da entidade (ou em a) ligadas a ela por fios. */
    public static final int RECHARGE = 79;
    /** Impulso Arcano: engrenagens em volta do bloco em a. power = 1 ao lançar, 0 a cada segundo enquanto dura. */
    public static final int MACHINE_BOOST = 80;
    /** Foco Arcano: uma engrenagem gira atrás da cabeça e encaixa. */
    public static final int ARCANE_FOCUS = 81;
    /** Olhar Vigilante: uma coruja pousa sobre a entidade (ou em a) e abre os olhos. */
    public static final int VIGIL = 82;
    /** Olhar Vigilante: uma corujinha marca uma criatura vista (entidade). */
    public static final int VIGIL_MARK = 83;
    /** Moeda da Sorte: a moeda sobe girando sobre a entidade. power = 1 deu bom, 0 deu ruim. */
    public static final int COIN = 84;
    /** Eco: ondas e bocas repetindo, saindo da entidade. */
    public static final int ECHO = 85;
    /** Toque do Vazio: um buraco voa. b = velocidade por tick, power = alcance. */
    public static final int VOID_LAUNCH = 86;
    /** Toque do Vazio acertou: o chão abre debaixo do alvo. */
    public static final int VOID_HIT = 87;

    /** Magias que já têm efeito onírico próprio. */
    public static final Set<String> OWN = Set.of("fire_spark", "gloom", "blink", "time_slow", "bloom", "holy_light",
            "fire_burst", "ember_ring", "meteor", "phoenix", "inferno", "shadow_veil", "dark_drain", "shadow_wolf", "total_eclipse",
            "ice_shard", "frost_nova", "blizzard", "ice_age", "water_bolt", "water_mend", "purify", "tidal_wave",
            "boulder", "stone_skin", "stone_golem", "earthquake", "cataclysm",
            "gust", "updraft", "wind_step", "hurricane", "spark_arc", "energy_discharge", "thunderstorm", "judgement",
            "glimmer", "light_orb", "dawn",
            "quicken", "foresight", "rewind", "time_stop", "eternal_instant", "pull", "telekinesis", "anchor", "rift", "black_hole",
            "sprout", "vine", "quick_vine", "living_web", "thorns", "living_forest", "tree_of_life",
            "lava_river", "eruption", "magma_armor", "mist", "steam_jet", "cloud_platform", "cloud_bridge", "spirit_guardian", "soul_harvest",
            "recharge", "machine_boost", "arcane_focus", "vigilant_gaze", "lucky_coin", "echo", "void_touch");

    private static int nextId = 1;

    private DreamFx() {}

    public static boolean owns(Spell spell) {
        return OWN.contains(spell.id());
    }

    /** A preparação (carga ou ritual) dessa magia também já é onírica? Só nos elementos que já têm a cena no cliente. */
    public static boolean ownsPrep(Spell spell) {
        return owns(spell) && switch (spell.element()) {
            case FIRE, DARKNESS, ICE, WATER, EARTH, ENERGY, LIGHT, SPACE, LIFE, MAGMA -> true;
            default -> false;
        };
    }

    public static int newId() {
        return nextId++ & 0xFFFFFF;
    }

    /** Manda o efeito para quem está a até 64 blocos de {@code a}. */
    public static void send(ServerLevel level, int kind, int id, @Nullable Entity entity, float power, Vec3 a, Vec3 b) {
        PacketDistributor.sendToPlayersNear(level, null, a.x, a.y, a.z, 64.0,
                new DreamFxPayload(kind, id, entity == null ? -1 : entity.getId(), power, a.x, a.y, a.z, b.x, b.y, b.z));
    }

    public static void send(ServerLevel level, int kind, @Nullable Entity entity, float power, Vec3 a) {
        send(level, kind, 0, entity, power, a, Vec3.ZERO);
    }
}
