"""Troca o createBodyLayer() dos *Model.java do folclore pelos gerados em build/lore/<nome>_layer.txt.
Uso (raiz do projeto): python tools/lore/apply_layers.py"""
import re
J = 'src/main/java/com/mistech/mistechmod/lore/client/'
MAP = {'MatintaBirdModel': 'matinta_bird', 'MatintaWitchModel': 'matinta_witch', 'CaiporaModel': 'caipora',
       'IaraModel': 'iara', 'CucaModel': 'cuca', 'MapinguariModel': 'mapinguari'}
for cls, name in MAP.items():
    path = J + cls + '.java'
    src = open(path, encoding='utf-8').read()
    layer = open('build/lore/%s_layer.txt' % name, encoding='utf-8').read()
    a = src.index('    public static LayerDefinition createBodyLayer()')
    b = src.index('\n    }\n', a) + len('\n    }\n')
    src = src[:a] + layer + src[b:]
    open(path, 'w', encoding='utf-8', newline='\n').write(src)
    print('ok', cls)
