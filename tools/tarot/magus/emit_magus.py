"""Gera o Java (geometria) e as texturas do Mago para o mod.
Uso: python emit_magus.py <raiz do projeto>"""
import sys, os
import magus_models, magus_fx

ROOT = sys.argv[1] if len(sys.argv) > 1 else '.'
JAVA = os.path.join(ROOT, 'src/main/java/com/mistech/mistechmod/tarot/magus/client')
TEX = os.path.join(ROOT, 'src/main/resources/assets/mistech/textures/entity')
os.makedirs(JAVA, exist_ok=True)

def emit(M, cls, layer, doc):
    M.pack()
    M.paint()
    return f'''    // ----- {doc} -----
    public static final ModelLayerLocation {layer}_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mistech.MODID, "{cls}"), "main");

    public static LayerDefinition {layer.lower()}Layer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{M.java_layer()}
        return LayerDefinition.create(mesh, {M.tex_w}, {M.tex_h});
    }}

    public static Map<String, ModelPart> {layer.lower()}Parts(ModelPart root) {{
        Map<String, ModelPart> map = new HashMap<>();
{M.java_bind()}
        return map;
    }}
'''

body = emit(magus_models.build(), 'the_magician', 'BODY', 'as cinco formas (atlas 512x256, textura em 1024x512)')
Mb = magus_models.build(); Mb.pack(); Mb.paint()
Mb.img.save(os.path.join(TEX, 'the_magician.png')); Mb.glow.save(os.path.join(TEX, 'the_magician_glow.png'))
Mf = magus_fx.build(); Mf.pack(); Mf.paint()
Mf.img.save(os.path.join(TEX, 'magician_fx.png')); Mf.glow.save(os.path.join(TEX, 'magician_fx_glow.png'))
fx = emit(magus_fx.build(), 'magician_fx', 'FX', 'pedra rúnica, bolha e magias (atlas 128x128)')

src = f'''package com.mistech.mistechmod.tarot.magus.client;

import java.util.HashMap;
import java.util.Map;

import com.mistech.mistechmod.Mistech;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * GERADO por {{@code tools/tarot/magus/emit_magus.py}} — não editar à mão (as animações ficam em {{@link MagusModel}}).
 * Coordenadas de cada forma: chão em y = 0 local (raiz em y = 24), frente = -Z.
 */
@SuppressWarnings("unused")
public final class MagusGeometry {{
    private MagusGeometry() {{}}

{body}
{fx}}}
'''
open(os.path.join(JAVA, 'MagusGeometry.java'), 'w').write(src)
print('ok')
