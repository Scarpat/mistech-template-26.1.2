package com.mistech.mistechmod.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Tema de cada tela (cores de texto, contorno pulsando e luzinhas), combinando com a textura do bloco que a abre.
 * GERADO por tools/gui/restyle.py a partir de tools/gui/themes.py - edite lá e rode o script.
 */
public final class GuiThemes {
    /**
     * @param label  títulos
     * @param text   textos secundários
     * @param glow   cor (RGB) do contorno que pulsa em volta do painel
     * @param motes  cores (RGB) das luzinhas subindo
     * @param well   miolo dos espaços (ARGB), para o que a tela desenha por código
     * @param accent cor de destaque (ARGB)
     * @param light  tela clara (textos escuros)
     */
    public record Theme(int label, int text, int glow, int[] motes, int well, int accent, boolean light) {}

    /** O visual místico de antes (telas sem tema próprio). */
    public static final Theme MYSTIC = new Theme(0xFFE3C98A, 0xFFC9B8E8, 0xB78CF0, new int[] {0xF0C870, 0xC49CF5, 0x9B6FE0},
            0xFF180F22, 0xFFB98CF0, false);

    private static final Map<String, Theme> THEMES = new HashMap<>();

    private static void put(String name, Theme theme) {
        THEMES.put(name, theme);
    }

    static {
        put("accessories", new Theme(0xFFF0D080, 0xFFE8C8C8, 0xE0B860, new int[] {0xFFF0B0, 0xE0B860, 0x3CCF80}, 0xFF1E080C, 0xFFE0B860, false));
        put("advanced_runic_battery", new Theme(0xFFC8FFF0, 0xFFB0D8CC, 0xB05CFF, new int[] {0xB05CFF, 0x70C8A8, 0xF0D8FF}, 0xFF08140F, 0xFFB05CFF, false));
        put("ash_generator", new Theme(0xFFFFD8A8, 0xFFD8CCC4, 0xFF7A2A, new int[] {0xA8A4A0, 0xFF7A2A, 0xD8D2CC}, 0xFF141312, 0xFFFF8A3A, false));
        put("astral_generator", new Theme(0xFFFFF0B0, 0xFFC8D4F0, 0x8AB0FF, new int[] {0xFFFFFF, 0xFFF6C0, 0x8AB0FF}, 0xFF08102A, 0xFF8AB0FF, false));
        put("blaze_generator", new Theme(0xFFFFD27A, 0xFFE8C8B8, 0xFF8A2A, new int[] {0xFF8A2A, 0xFFCF4A, 0xFF5A1A}, 0xFF160707, 0xFFFF8A2A, false));
        put("condenser", new Theme(0xFFC8F0FF, 0xFFA8D0E8, 0x7FD0FF, new int[] {0xFFFFFF, 0x9FE3FF, 0x7FD0FF}, 0xFF0A1622, 0xFF7FD0FF, false));
        put("distiller", new Theme(0xFF3A3028, 0xFF4A4036, 0xFFFFFF, new int[] {0xFFFFFF, 0x8AC0E8, 0xF0E0A0}, 0xFF5A564E, 0xFF4A88C0, true));
        put("experience_generator", new Theme(0xFFD8FFA0, 0xFFB0E0C8, 0x8AF040, new int[] {0xD8FF5A, 0x8AF040, 0x3AD8C8}, 0xFF041012, 0xFFA8F040, false));
        put("gnome_bicycle", new Theme(0xFFFFE0B0, 0xFFE8D8C0, 0xE06048, new int[] {0xF0C870, 0xE06048, 0xFFE0B0}, 0xFF18130C, 0xFFE06048, false));
        put("igneous_boiler", new Theme(0xFFFFE0A0, 0xFFF0C8B0, 0xFF7A1A, new int[] {0xFF9A3A, 0xFFE07A, 0xFF5A1A}, 0xFF1A0806, 0xFFFF9A3A, false));
        put("impure_reactor", new Theme(0xFFE8C8FF, 0xFFC8B0E0, 0xB05CFF, new int[] {0xB05CFF, 0x8A3AD8, 0xF0C8FF}, 0xFF120A1C, 0xFFB05CFF, false));
        put("incense_holder", new Theme(0xFFF0D890, 0xFFD8CCC0, 0xC8A050, new int[] {0xD8D2DC, 0xA8A2B0, 0xF0D890}, 0xFF17110C, 0xFFA8A2B0, false));
        put("miner", new Theme(0xFFFFE0A0, 0xFFD0CCD8, 0xC8A050, new int[] {0xA89880, 0xC8A050, 0x8A8A96}, 0xFF100F11, 0xFFC8A050, false));
        put("pure_generator", new Theme(0xFF4A3A10, 0xFF5A4A20, 0xFFF0B0, new int[] {0xFFF0B0, 0xFFFFFF, 0x5AC8FF}, 0xFF5E5644, 0xFF8A6A20, true));
        put("rune_anvil", new Theme(0xFFE8C46A, 0xFFC8C8D8, 0xB78CF0, new int[] {0xB78CF0, 0xE8C46A, 0xFF9A4A}, 0xFF141419, 0xFFB78CF0, false));
        put("rune_confector", new Theme(0xFFF0C890, 0xFFDCC8B0, 0xD88A5A, new int[] {0xB78CF0, 0xD88A5A, 0xF0C870}, 0xFF170F09, 0xFFB78CF0, false));
        put("rune_roller", new Theme(0xFFE0F0B0, 0xFFC8D8B0, 0x7AD04A, new int[] {0xD8D2DC, 0x7AD04A, 0xB8E080}, 0xFF101608, 0xFF7AD04A, false));
        put("runic_battery", new Theme(0xFFFFE8A0, 0xFFE8D0B8, 0xFFE14A, new int[] {0xFFE14A, 0xFFF8C0, 0xE89A62}, 0xFF140C07, 0xFFFFE14A, false));
        put("runic_crusher", new Theme(0xFFE0DCF0, 0xFFC0BCD0, 0x9A9AB0, new int[] {0x9A9AB0, 0xB78CF0, 0xE0E0F0}, 0xFF121214, 0xFF9A9AB0, false));
        put("runic_furnace", new Theme(0xFFFFCF8A, 0xFFE6D2BC, 0xFF6A1A, new int[] {0xFF7A2A, 0xFFB04A, 0xFFE07A}, 0xFF181411, 0xFFFF7A2A, false));
        put("runic_harvester", new Theme(0xFFFFF0B0, 0xFFE8DCB0, 0xC8D04A, new int[] {0xFFF0A0, 0xC8D04A, 0xE8C870}, 0xFF1A1407, 0xFFC8D04A, false));
        put("runic_planter", new Theme(0xFFD8F0B0, 0xFFD0C8B0, 0x7AA850, new int[] {0xB8E080, 0x7AD04A, 0xF08AC0}, 0xFF160F09, 0xFF7AD04A, false));
        put("spatial_storage", new Theme(0xFFF3D9A4, 0xFFE0C9A6, 0x8A5A2A, new int[] {0x9B6FE0, 0xC49CF5, 0xF0C870}, 0xFF1E140B, 0xFFB07A3C, false));
        put("stellar_observatory", new Theme(0xFFFFF6D0, 0xFFC8D0F0, 0xC0D0FF, new int[] {0xFFFFFF, 0xFFF6C0, 0xA8C0FF}, 0xFF060A22, 0xFFA8C0FF, false));
        put("writing_table", new Theme(0xFFF0E2C0, 0xFFD8C8A8, 0xD8D2C4, new int[] {0xFFFFFF, 0xE0DCD2, 0xBDB6A8}, 0xFF1A110A, 0xFFD8D2C4, false));
    }

    private GuiThemes() {}

    public static Theme get(String name) {
        return THEMES.getOrDefault(name, MYSTIC);
    }
}
