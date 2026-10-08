# Mago (arcano I) — ferramentas

- `magus_models.py`: as cinco formas (Terra, Ar, Água, Fogo e Quintessência) por cubos; `magus_fx.py`: pedra rúnica, bolha e magias.
- `magus_mats.py`: pintura procedural das texturas (em 2x, como a referência do usuário).
- `emit_magus.py [raiz]`: gera `MagusGeometry.java` + `the_magician(_glow).png` + `magician_fx(_glow).png`.
- `run_preview.py [formas...]`: prévias isométricas (`prev_<forma>.png`) na pasta atual.
- `gen_magus_items.py [pasta_previa]`: Cajado do Mago 3D + ícones (rodar na raiz do projeto).
- `lang_magus.py <lang>` e `guide_magus.py <assets/mistech>`: traduções e Grimório.

Precisa de Python 3 com Pillow e numpy.
