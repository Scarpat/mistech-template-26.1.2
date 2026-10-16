"""Spawners das criaturas do Sonho (fase 2) — idempotente; roda depois do gen/gen_world.py."""
import json
D = 'src/main/resources/data/mistech/worldgen/biome/'
SHEEP = ("mistech:counting_sheep", 6, 2, 3)
SPAWNS = {
    'gnome_meadow': ([SHEEP], []),
    'gnome_depths': ([("mistech:gnome_miner", 4, 1, 2)], []),
    'pillow_hills': ([("mistech:counting_sheep", 10, 2, 4)], []),
    'murky_wastes': ([], [("mistech:corrupted_gnome", 40, 1, 3), ("mistech:shade", 15, 1, 1), ("mistech:bogeyman", 8, 1, 1)]),
    'hand_forest': ([], [("mistech:shade", 30, 1, 1), ("mistech:corrupted_gnome", 20, 1, 3), ("mistech:bogeyman", 14, 1, 1)]),
    'falling_chasms': ([], [("mistech:shade", 25, 1, 2), ("mistech:corrupted_gnome", 15, 1, 2)]),
    'wrong_neighborhood': ([], [("mistech:bogeyman", 25, 1, 1), ("mistech:shade", 20, 1, 1)]),
}
for name, (cr, mo) in SPAWNS.items():
    p = D + name + '.json'
    b = json.load(open(p))
    for cat, lst in (('creature', cr), ('monster', mo)):
        cur = [e for e in b['spawners'][cat] if e['type'] not in {t for t, *_ in lst}]
        b['spawners'][cat] = cur + [{"type": t, "weight": wt, "minCount": a, "maxCount": c} for t, wt, a, c in lst]
    json.dump(b, open(p, 'w'), indent=2); open(p, 'a').write('\n')
print('ok')
