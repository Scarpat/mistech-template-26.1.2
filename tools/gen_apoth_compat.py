"""Gera os dados de integração do Mistech com Apotheosis / Apothic Enchanting.
Uso: python3 gen_apoth.py <raiz do projeto>"""
import json, os, sys

ROOT = sys.argv[1]
RES = os.path.join(ROOT, "src/main/resources")
P6 = ["cracked", "chipped", "flawed", "normal", "flawless", "perfect"]


def vals(purities, numbers):
    assert len(purities) == len(numbers)
    return dict(zip(purities, numbers))


def from_(p):
    return P6[P6.index(p):]


def write(path, data):
    full = os.path.join(RES, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("ok", path)


LIGHT_WEAPON = {"key": "light_weapon", "types": ["apotheosis:melee_weapon", "apotheosis:trident"]}
CORE_ARMOR = {"key": "core_armor", "types": ["apotheosis:chestplate", "apotheosis:leggings"]}
ALL_ARMOR = {"key": "armor", "types": ["apotheosis:helmet", "apotheosis:chestplate", "apotheosis:leggings", "apotheosis:boots"]}


def attr(gem_class, attribute, op, values):
    return {"type": "apotheosis:attribute", "attribute": attribute, "gem_class": gem_class, "operation": op, "values": values}


def effect(gem_class, eff, target, values):
    return {"type": "apotheosis:mob_effect", "gem_class": gem_class, "mob_effect": eff, "target": target, "values": values}


def eff_vals(purities, duration, amps, cooldowns):
    return {p: {"duration": duration, "amplifier": a, "cooldown": c} for p, a, c in zip(purities, amps, cooldowns)}


# ---------------------------------------------------------------- Gemas
gems = {}

# Gema Impura — poder a um preço (Magia Impura). Cai em qualquer lugar.
p = from_("cracked")
gems["impure"] = {
    "min_purity": "cracked",
    "weights": {"quality": 0.5, "weight": 40},
    "bonuses": [
        {
            "type": "apotheosis:multi_attribute",
            "desc": "bonus.mistech:multi_attr.desc.but",
            "gem_class": LIGHT_WEAPON,
            "modifiers": [
                {"attribute": "minecraft:attack_damage", "operation": "add_value", "values": vals(p, [1.0, 1.5, 2.5, 3.5, 4.5, 6.0])},
                {"attribute": "minecraft:max_health", "operation": "add_value", "values": vals(p, [-1.0, -1.0, -2.0, -2.0, -3.0, -4.0])},
            ],
        },
        attr("apotheosis:bow", "apothic_attributes:armor_pierce", "add_value", vals(p, [1.0, 1.5, 2.0, 3.0, 4.5, 6.0])),
        {"type": "apotheosis:damage_reduction", "damage_type": "magic", "gem_class": CORE_ARMOR,
         "values": vals(p, [0.03, 0.05, 0.08, 0.11, 0.15, 0.2])},
        attr("apotheosis:breaker", "minecraft:block_break_speed", "add_multiplied_base", vals(p, [0.05, 0.08, 0.12, 0.16, 0.22, 0.3])),
    ],
}

# Gema Purificada — cura e proteção (Magia Purificada). Rara, única.
p = from_("flawed")
gems["purified"] = {
    "min_purity": "flawed",
    "unique": True,
    "weights": {"quality": 1.0, "weight": 20},
    "bonuses": [
        attr({"key": "upper_lower", "types": ["apotheosis:helmet", "apotheosis:boots"]},
             "apothic_attributes:healing_received", "add_multiplied_base", vals(p, [0.1, 0.15, 0.2, 0.3])),
        effect(CORE_ARMOR, "mistech:arcane_ward", "hurt_self", eff_vals(p, 100, [0, 0, 0, 1], [900, 700, 500, 400])),
        effect(LIGHT_WEAPON, "minecraft:weakness", "attack_target", eff_vals(p, 80, [0, 0, 1, 1], [200, 160, 160, 120])),
        attr("apotheosis:bow", "apothic_attributes:arrow_damage", "add_multiplied_base", vals(p, [0.05, 0.1, 0.15, 0.2])),
        attr("apotheosis:breaker", "apothic_attributes:experience_gained", "add_multiplied_base", vals(p, [0.25, 0.5, 0.75, 1.0])),
    ],
}
# chave de classe nova precisa de tradução
CUSTOM_CLASSES = {"upper_lower": ("Helmets and Boots", "Capacetes e Botas")}

# Gema Astral — do Plano Arcano e do End; críticos e sorte.
p = from_("normal")
gems["astral"] = {
    "constraints": {"dimensions": ["mistech:arcane_plane", "minecraft:the_end"]},
    "min_purity": "normal",
    "unique": True,
    "weights": {
        "summit": {"quality": 2.0, "weight": 40},
        "pinnacle": {"quality": 2.0, "weight": 50},
    },
    "bonuses": [
        {
            "type": "apotheosis:multi_attribute",
            "desc": "bonus.apotheosis:multi_attr.desc.and",
            "gem_class": LIGHT_WEAPON,
            "modifiers": [
                {"attribute": "apothic_attributes:crit_chance", "operation": "add_value", "values": vals(p, [0.1, 0.15, 0.2])},
                {"attribute": "apothic_attributes:crit_damage", "operation": "add_value", "values": vals(p, [0.2, 0.35, 0.5])},
            ],
        },
        attr("apotheosis:bow", "apothic_attributes:arrow_velocity", "add_multiplied_base", vals(p, [0.2, 0.3, 0.4])),
        attr(ALL_ARMOR, "minecraft:luck", "add_value", vals(p, [1.0, 1.5, 2.5])),
        effect("apotheosis:breaker", "mistech:fortune", "break_self", eff_vals(p, 200, [0, 0, 1], [1200, 900, 600])),
    ],
}

# Gema Gnômica — só no Reino dos Gnomos; mineração e agilidade.
p = from_("chipped")
gems["gnomish"] = {
    "constraints": {"dimensions": ["mistech:gnome_realm"]},
    "min_purity": "chipped",
    "weights": {"quality": 1.0, "weight": 60},
    "bonuses": [
        attr("apotheosis:breaker", "minecraft:block_break_speed", "add_multiplied_base", vals(p, [0.1, 0.15, 0.2, 0.3, 0.4])),
        attr("apotheosis:boots", "minecraft:movement_speed", "add_multiplied_base", vals(p, [0.03, 0.05, 0.07, 0.09, 0.12])),
        attr(CORE_ARMOR, "minecraft:armor_toughness", "add_value", vals(p, [0.5, 1.0, 1.5, 2.0, 3.0])),
        attr(LIGHT_WEAPON, "apothic_attributes:experience_gained", "add_multiplied_base", vals(p, [0.1, 0.2, 0.3, 0.4, 0.5])),
        attr("apotheosis:bow", "apothic_attributes:draw_speed", "add_multiplied_base", vals(p, [0.05, 0.1, 0.15, 0.2, 0.25])),
    ],
}

GEM_TEXTURES = {
    "impure": "mistech:item/impure_magic_shard",
    "purified": "mistech:item/pure_magic_shard",
    "astral": "mistech:item/star_fragment",
    "gnomish": "mistech:item/guide_stone_gem",
}
GEM_NAMES = {
    "impure": ("Impure Gem", "Gema Impura"),
    "purified": ("Purified Gem", "Gema Purificada"),
    "astral": ("Astral Gem", "Gema Astral"),
    "gnomish": ("Gnomish Gem", "Gema Gnômica"),
}

for name, gem in gems.items():
    # sanity: classes não podem se sobrepor
    seen = set()
    for b in gem["bonuses"]:
        gc = b["gem_class"]
        types = [gc] if isinstance(gc, str) else gc["types"]
        for t in types:
            assert t not in seen, (name, t)
            seen.add(t)
    write(f"data/mistech/apotheosis/gems/{name}.json", gem)
    write(f"assets/mistech/models/item/gems/{name}.json", {"parent": "item/generated", "textures": {"layer0": GEM_TEXTURES[name]}})

# ---------------------------------------------------------------- Apothic Enchanting: estatísticas da mesa
# maxEterna / eterna / quanta / arcana / clues (padrão maxEterna = 30)
stats = {
    "impure_magic_block": {"blocks": "mistech:impure_magic_block", "stats": {"maxEterna": 45.0, "eterna": 3.0, "quanta": 7.5}},
    "pure_magic_block": {"blocks": "mistech:pure_magic_block", "stats": {"maxEterna": 60.0, "eterna": 4.0, "quanta": -5.0, "arcana": 7.5}},
    "mithril_block": {"blocks": "mistech:mithril_block", "stats": {"maxEterna": 50.0, "eterna": 2.5, "arcana": 2.5}},
    "adamantine_block": {"blocks": "mistech:adamantine_block", "stats": {"maxEterna": 65.0, "eterna": 3.5, "quanta": 2.5, "arcana": 2.5}},
    "orichalcum_block": {"blocks": "mistech:orichalcum_block", "stats": {"maxEterna": 80.0, "eterna": 5.0, "arcana": 5.0}},
    "mithril_obelisk": {"blocks": "mistech:mithril_obelisk", "stats": {"clues": 1}},
    "astral_pillar": {"blocks": "mistech:astral_pillar", "stats": {"arcana": 5.0, "clues": 1}},
}
for name, s in stats.items():
    write(f"data/mistech/apothic_enchanting/enchanting_stats/{name}.json", s)

# ---------------------------------------------------------------- traduções
def add_lang(file, entries):
    full = os.path.join(RES, "assets/mistech/lang", file)
    with open(full, encoding="utf-8") as f:
        text = f.read()
    json.loads(text)  # valida antes
    entries = {k: v for k, v in entries.items() if json.dumps(k) + ":" not in text.replace('": ', '":')}
    if not entries:
        print("lang", file, "nada novo"); return
    body = text.rstrip()
    assert body.endswith("}")
    body = body[:-1].rstrip()
    lines = ",\n".join(f"  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}" for k, v in entries.items())
    text = body + ",\n" + lines + "\n}\n"
    json.loads(text)
    with open(full, "w", encoding="utf-8", newline="") as f:
        f.write(text)
    print("lang", file, len(entries))

en = {f"item.apotheosis.gem.mistech:{k}": v[0] for k, v in GEM_NAMES.items()}
pt = {f"item.apotheosis.gem.mistech:{k}": v[1] for k, v in GEM_NAMES.items()}
for k, (e, pb) in CUSTOM_CLASSES.items():
    en[f"gem_class.{k}"] = e
    pt[f"gem_class.{k}"] = pb
en["bonus.mistech:multi_attr.desc.but"] = "%s, but %s"
pt["bonus.mistech:multi_attr.desc.but"] = "%s, mas %s"
add_lang("en_us.json", en)
add_lang("pt_br.json", pt)
