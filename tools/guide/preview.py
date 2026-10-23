# Desenha uma prévia dos mapas de todos os capítulos (build/tmp/grimorio_preview.png). Só para conferir o layout.
# Uso: python3 tools/guide/preview.py [progress|dimension]   (sem argumento: por assunto)
import json, sys
from PIL import Image, ImageDraw
b = json.load(open('src/main/resources/assets/mistech/guide/book.json', encoding='utf-8'))
tex = Image.open('src/main/resources/assets/mistech/textures/gui/guide_map.png').convert('RGBA')
C = 34
done = {'welcome', 'path', 'incense', 'incense_holder', 'ash', 'ash_chalk', 'writing_table', 'fire_rune', 'water_rune', 'crops', 'cigar'}
allE = {e['id']: e for c in b['categories'] for e in c['entries']}
def st(e):
    if e['id'] in done: return 'done'
    return 'avail' if all(d in done for d in e['deps']) else 'locked'
tiles = []
view = sys.argv[1] if len(sys.argv) > 1 else None
chapters = b['views'][view] if view else b['categories']
for c in chapters:
    c = dict(c, entries=[dict(allE[e['id']], **e) for e in c['entries']])
    es = {e['id']: e for e in c['entries']}
    links = {l['id']: l for l in c.get('links', [])}
    xs = [e['x'] for e in list(es.values()) + list(links.values())]; ys = [e['y'] for e in list(es.values()) + list(links.values())]
    W = int((max(xs) - min(xs)) * C + 2 * C); H = int((max(ys) - min(ys)) * C + 2 * C) + 14
    im = Image.new('RGBA', (W, H), (17, 13, 23, 255)); d = ImageDraw.Draw(im)
    d.text((4, 2), c['id'], fill=(215, 168, 255, 255))
    P = lambda e: ((e['x'] - min(xs)) * C + C, (e['y'] - min(ys)) * C + C + 14)
    def dashed(a, bb, col):
        (x0, y0), (x1, y1) = a, bb
        n = max(1, int(((x1 - x0) ** 2 + (y1 - y0) ** 2) ** 0.5 / 7))
        for i in range(n):
            t0, t1 = i / n, min(1, (i + 0.55) / n)
            d.line([(x0 + (x1 - x0) * t0, y0 + (y1 - y0) * t0), (x0 + (x1 - x0) * t1, y0 + (y1 - y0) * t1)], fill=col, width=2)
    for e in es.values():
        for dp in e['deps']:
            if dp in links and dp not in es:
                dashed(P(links[dp]), P(e), (150, 120, 70))
    for l in links.values():
        x, y = P(l)
        d.rectangle([x - 10, y - 10, x + 10, y + 10], fill=(23, 18, 31), outline=(150, 120, 70))
        d.text((x - 8, y - 5), l['id'][:3], fill=(170, 160, 180, 255))
    for e in es.values():
        for dp in e['deps']:
            if dp in es:
                col = (79, 174, 90) if st(e) == 'done' else (200, 154, 64) if dp in done else (58, 53, 66)
                d.line([P(es[dp]), P(e)], fill=(11, 8, 16), width=4); d.line([P(es[dp]), P(e)], fill=col, width=2)
    for e in es.values():
        s = 34 if e.get('size') else 26; v = 26 if e.get('size') else 0
        u = {'avail': 0, 'done': s, 'locked': 2 * s}[st(e)]
        x, y = P(e)
        im.alpha_composite(tex.crop((u, v, u + s, v + s)), (int(x - s / 2), int(y - s / 2)))
        d.text((x - 8, y - 5), e['id'][:3], fill=(255, 255, 255, 255))
    tiles.append(im)
cols = 3; pad = 8
rows = [tiles[i:i + cols] for i in range(0, len(tiles), cols)]
W = max(sum(t.width for t in r) + pad * (len(r) + 1) for r in rows)
H = sum(max(t.height for t in r) for r in rows) + pad * (len(rows) + 1)
sheet = Image.new('RGBA', (W, H), (40, 34, 48, 255)); y = pad
for r in rows:
    x = pad
    for t in r:
        sheet.alpha_composite(t, (x, y)); x += t.width + pad
    y += max(t.height for t in r) + pad
sheet.save('build/tmp/grimorio_preview' + ('_' + view if view else '') + '.png'); print(sheet.size)
