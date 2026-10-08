import sys; sys.path.insert(0, 'tools/pendencias2')
from res import lang
C = {
 'p_start': ('First Steps', 'Incense, ash and the first crops.', 'Primeiros Passos', 'Incenso, cinzas e as primeiras plantações.'),
 'p_runes': ('Runes and Machines', 'The Writing Table, the runes and the first runic machines.', 'Runas e Máquinas', 'A Mesa de Escrita, as runas e as primeiras máquinas rúnicas.'),
 'p_essence': ('Impure Essence', 'Compressing magic: astrology, storage, batteries and the Gnome Dimension.', 'Essência Impura', 'Comprimir magia: astrologia, armazenamento, baterias e a Dimensão dos Gnomos.'),
 'p_tier1': ('Rituals', 'The Impure Altar and everything it opens: taint, folklore and the lands beyond.', 'Rituais', 'O Altar Impuro e tudo o que ele abre: mácula, folclore e as terras do além.'),
 'p_tier2': ('Altar Tier II', 'Adamantine, the Sanctuary and the stronger rituals.', 'Altar Tier II', 'Adamantina, o Santuário e os rituais mais fortes.'),
 'p_tier3': ('Altar Tier III', 'Orichalcum, the great spirits and the Primordial Heart.', 'Altar Tier III', 'Oricalco, os grandes espíritos e o Coração Primordial.'),
 'd_overworld_start': ('Overworld · First Steps', 'What you find right around your home.', 'Mundo Superior · Primeiros Passos', 'O que dá para fazer perto de casa.'),
 'd_overworld_runes': ('Overworld · Runes', 'Runes, runic machines and the gnomes of the forest.', 'Mundo Superior · Runas', 'Runas, máquinas rúnicas e os gnomos da floresta.'),
 'd_overworld_essence': ('Overworld · Essence', 'Impure essence, astrology and storage.', 'Mundo Superior · Essência', 'Essência impura, astrologia e armazenamento.'),
 'd_overworld_tier1': ('Overworld · Rituals', 'The altar, taint and folklore.', 'Mundo Superior · Rituais', 'O altar, a mácula e o folclore.'),
 'd_overworld_tier2': ('Overworld · Tier II', 'The Sanctuary and the second altar.', 'Mundo Superior · Tier II', 'O Santuário e o segundo altar.'),
 'd_overworld_tier3': ('Overworld · Tier III', 'The last altar and the great spirits.', 'Mundo Superior · Tier III', 'O último altar e os grandes espíritos.'),
 'd_between': ('Between Worlds', 'Travelling through space: anchors and orbs.', 'Entre Mundos', 'Viajar pelo espaço: âncoras e orbes.'),
 'd_realm': ('Gnome Dimension', 'The realm under the dreams and its metals.', 'Dimensão dos Gnomos', 'O reino debaixo dos sonhos e os seus metais.'),
 'd_deep_dark': ('Deep Dark', 'Echoes, silence and the Warden\'s halls.', 'Escuro Profundo', 'Ecos, silêncio e os salões do Warden.'),
 'd_pale': ('Pale Garden', 'The watchful trees and the Creaking.', 'Jardim Pálido', 'As árvores vigilantes e o Rangedor.'),
 'd_nether': ('Nether', 'Fire, souls and piglins.', 'Nether', 'Fogo, almas e piglins.'),
}
en, pt = {}, {}
for k, (en_n, en_d, pt_n, pt_d) in C.items():
    en['guide.mistech.cat.' + k] = en_n; en['guide.mistech.cat.' + k + '.desc'] = en_d
    pt['guide.mistech.cat.' + k] = pt_n; pt['guide.mistech.cat.' + k + '.desc'] = pt_d
en.update({'guide.mistech.map.view': 'Organize: %s', 'guide.mistech.map.view.type': 'by subject', 'guide.mistech.map.view.progress': 'by progression',
           'guide.mistech.map.view.dimension': 'by dimension', 'guide.mistech.map.view_hint': 'Click to change'})
pt.update({'guide.mistech.map.view': 'Organizar: %s', 'guide.mistech.map.view.type': 'por assunto', 'guide.mistech.map.view.progress': 'por progressão',
           'guide.mistech.map.view.dimension': 'por dimensão', 'guide.mistech.map.view_hint': 'Clique para trocar'})
lang(en, pt)
print('ok')
