"""Lê um modelo exportado do Blockbench (Java, mapeamentos Mojang) para o Model do modelgen (UV fixa)."""
import re, sys
sys.path.insert(0,'/home/claude/gen')
from modelgen import Model, Box

def parse(path, tex_w=64, tex_h=64, name='bb'):
    src = open(path, encoding='utf-8').read()
    M = Model(name, tex_w)
    M.tex_h = tex_h
    var = {'partdefinition': None}
    parts = {}
    pat = re.compile(r'PartDefinition\s+(\w+)\s*=\s*(\w+)\.addOrReplaceChild\("([^"]+)",\s*CubeListBuilder\.create\(\)(.*?),\s*PartPose\.(offset|offsetAndRotation)\(([^)]*)\)\);', re.S)
    for m in pat.finditer(src):
        v, parentv, pname, cubes, kind, args = m.groups()
        nums = [float(x.strip().rstrip('F')) for x in args.split(',')]
        pivot = tuple(nums[:3]); rot = tuple(nums[3:6]) if kind == 'offsetAndRotation' else (0, 0, 0)
        parent = parts.get(parentv)
        p = M.part(pname, parent, pivot, rot)
        parts[v] = p
        for cm in re.finditer(r'\.texOffs\((\d+),\s*(\d+)\)(\.mirror\(\))?\.addBox\(([^)]*?)(?:,\s*new CubeDeformation\(([^)]*)\))?\)', cubes):
            u, vv, mir, box, infl = cm.groups()
            b = [float(x.strip().rstrip('F')) for x in box.split(',')[:6]]
            bx = Box(tuple(b[:3]), tuple(b[3:6]), 0x888888, mirror=bool(mir))
            bx.uv = (int(u), int(vv))
            p.boxes.append(bx)
    return M
