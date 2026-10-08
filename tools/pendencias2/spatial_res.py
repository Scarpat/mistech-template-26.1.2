import sys; sys.path.insert(0, 'tools/pendencias2')
from res import *
def el(fr, to, tex, faces='nsewud', tint=False):
    names = {'n': 'north', 's': 'south', 'e': 'east', 'w': 'west', 'u': 'up', 'd': 'down'}
    f = {}
    for c in faces:
        n = names[c]
        if c in 'ud':
            uv = [fr[0], fr[2], to[0], to[2]]
            t = tex[1] if isinstance(tex, tuple) else tex
        else:
            w0, w1 = (fr[0], to[0]) if c in 'ns' else (fr[2], to[2])
            uv = [w0, 16 - to[1], w1, 16 - fr[1]]
            t = tex[0] if isinstance(tex, tuple) else tex
        f[n] = {'uv': uv, 'texture': t}
        if tint:
            f[n]['tintindex'] = 0
    return {'from': fr, 'to': to, 'faces': f}
T = {'particle': 'mistech:block/spatial_anchor_base', 'base': 'mistech:block/spatial_anchor_base',
     'top': 'mistech:block/spatial_anchor_top', 'column': 'mistech:block/spatial_anchor_column',
     'crystal': 'mistech:block/spatial_anchor_crystal', 'rune': 'mistech:block/spatial_anchor_rune'}
def rune(y0, y1):
    # faixa da runa nas 4 faces da coluna (um pouco para fora), tingida pela cor da âncora
    return [el([7, y0, 2.95], [9, y1, 13.05], '#rune', 'ns', True), el([2.95, y0, 7], [13.05, y1, 9], '#rune', 'ew', True)]
wj(A + 'models/block/spatial_anchor_lower.json', {'textures': T, 'elements': [
    el([1, 0, 1], [15, 3, 15], ('#base', '#top')),
    el([3, 3, 3], [13, 16, 13], ('#column', '#top'), 'nsewd')] + rune(4, 8) + rune(9, 13) + rune(14, 16)})
upper_crystal = el([5, 11, 5], [11, 15, 11], '#crystal', tint=True)
upper_crystal['rotation'] = {'angle': 45, 'axis': 'y', 'origin': [8, 13, 8]}
wj(A + 'models/block/spatial_anchor_upper.json', {'textures': T, 'elements': [
    el([3, 0, 3], [13, 8, 13], ('#column', '#top'), 'nsewu'),
    el([2, 8, 2], [14, 11, 14], ('#base', '#top')),
    upper_crystal] + rune(0, 3) + rune(4, 8)})
wj(A + 'blockstates/spatial_anchor.json', {'variants': {
    'half=lower': {'model': 'mistech:block/spatial_anchor_lower'},
    'half=upper': {'model': 'mistech:block/spatial_anchor_upper'}}})
flat_item('spatial_anchor')
flat_item('translocation_orb')
wj(D + 'mistech/loot_table/blocks/spatial_anchor.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0,
    'entries': [{'type': 'minecraft:item', 'name': 'mistech:spatial_anchor'}],
    'conditions': [{'condition': 'minecraft:survives_explosion'},
                   {'condition': 'minecraft:block_state_property', 'block': 'mistech:spatial_anchor', 'properties': {'half': 'lower'}}]}],
    'random_sequence': 'mistech:blocks/spatial_anchor'})
tag('minecraft/tags/block/mineable/pickaxe', 'mistech:spatial_anchor')
tag('minecraft/tags/block/needs_iron_tool', 'mistech:spatial_anchor')
recipe('ritual/spatial_anchor', {'type': 'mistech:ritual', 'catalyst': 'minecraft:ender_eye',
    'ingredients': ['mistech:space_rune', 'mistech:impure_magic_block', 'mistech:impure_magic_block', 'minecraft:emerald'],
    'essence': 3, 'duration': 300, 'result': {'id': 'mistech:spatial_anchor'}, 'effect': 'astral'})
recipe('ritual/translocation_orb', {'type': 'mistech:ritual', 'catalyst': 'minecraft:ender_pearl',
    'ingredients': ['mistech:space_rune', 'mistech:impure_magic_shard', 'mistech:impure_magic_shard', 'mistech:mithril_ingot'],
    'essence': 5, 'duration': 400, 'min_tier': 2, 'result': {'id': 'mistech:translocation_orb'}, 'effect': 'astral'})
lang({
 'block.mistech.spatial_anchor': 'Spatial Anchor',
 'block.mistech.spatial_anchor.default_name': 'Anchor %s, %s',
 'item.mistech.translocation_orb': 'Translocation Orb',
 'message.mistech.spatial_anchor.learned': 'Anchor attuned: %s',
 'message.mistech.spatial_anchor.gone': 'That anchor no longer exists.',
 'message.mistech.spatial_anchor.no_xp': 'You need %s levels for this journey.',
 'message.mistech.spatial_anchor.blocked': 'There is no room to arrive at that anchor.',
 'screen.mistech.anchors': 'Spatial Anchor',
 'screen.mistech.anchors.orb': 'Translocation Orb',
 'screen.mistech.anchors.go': 'Go',
 'screen.mistech.anchors.empty': 'You know no other anchor yet.',
 'screen.mistech.anchors.distance': '%s blocks away',
 'screen.mistech.anchors.free': 'free',
 'screen.mistech.anchors.cost': '%s lv',
 'screen.mistech.anchors.hint': 'Touch an anchor to attune to it',
 'screen.mistech.anchors.dim.minecraft.overworld': 'Overworld',
 'screen.mistech.anchors.dim.minecraft.the_nether': 'The Nether',
 'screen.mistech.anchors.dim.minecraft.the_end': 'The End',
 'screen.mistech.anchors.dim.mistech.gnome_realm': 'Gnome Dimension',
 'guide.mistech.ritual.spatial_anchor': 'Spatial Anchor',
 'guide.mistech.ritual.translocation_orb': 'Translocation Orb',
}, {
 'block.mistech.spatial_anchor': 'Âncora Espacial',
 'block.mistech.spatial_anchor.default_name': 'Âncora %s, %s',
 'item.mistech.translocation_orb': 'Orbe de Translocação',
 'message.mistech.spatial_anchor.learned': 'Âncora sintonizada: %s',
 'message.mistech.spatial_anchor.gone': 'Essa âncora não existe mais.',
 'message.mistech.spatial_anchor.no_xp': 'Precisa de %s níveis para essa viagem.',
 'message.mistech.spatial_anchor.blocked': 'Não há espaço para chegar nessa âncora.',
 'screen.mistech.anchors': 'Âncora Espacial',
 'screen.mistech.anchors.orb': 'Orbe de Translocação',
 'screen.mistech.anchors.go': 'Ir',
 'screen.mistech.anchors.empty': 'Você ainda não conhece outra âncora.',
 'screen.mistech.anchors.distance': 'a %s blocos',
 'screen.mistech.anchors.free': 'grátis',
 'screen.mistech.anchors.cost': '%s nv',
 'screen.mistech.anchors.hint': 'Toque numa âncora para sintonizá-la',
 'screen.mistech.anchors.dim.minecraft.overworld': 'Mundo Superior',
 'screen.mistech.anchors.dim.minecraft.the_nether': 'Nether',
 'screen.mistech.anchors.dim.minecraft.the_end': 'O Fim',
 'screen.mistech.anchors.dim.mistech.gnome_realm': 'Dimensão dos Gnomos',
 'guide.mistech.ritual.spatial_anchor': 'Âncora Espacial',
 'guide.mistech.ritual.translocation_orb': 'Orbe de Translocação',
})
print('ok')
