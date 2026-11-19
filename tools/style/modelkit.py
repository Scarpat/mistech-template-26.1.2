"""Construtor de modelos de bloco JSON (elementos) para o guia de estilo v3: o volume vem da forma."""
import json

FACES = ('north', 'south', 'east', 'west', 'up', 'down')


class Model:
    def __init__(self, textures, parent='minecraft:block/block', render_type=None):
        self.textures = dict(textures); self.elements = []; self.parent = parent; self.render_type = render_type

    def box(self, frm, to, tex, faces=FACES, rot=None, uv=None, skip=()):
        """tex: chave de textura (str) ou dict face->chave. uv: dict face->[u0,v0,u1,v1] opcional."""
        e = {'from': list(frm), 'to': list(to), 'faces': {}}
        for f in faces:
            if f in skip: continue
            k = tex[f] if isinstance(tex, dict) else tex
            if k is None: continue
            fd = {'texture': '#' + k}
            if uv and f in uv: fd['uv'] = uv[f]
            e['faces'][f] = fd
        if rot: e['rotation'] = {'origin': rot[0], 'axis': rot[1], 'angle': rot[2]}
        self.elements.append(e)
        return self

    def json(self):
        d = {'parent': self.parent, 'textures': self.textures, 'elements': self.elements}
        if self.render_type: d['render_type'] = self.render_type
        return d

    def save(self, path):
        json.dump(self.json(), open(path, 'w'), indent=1)
