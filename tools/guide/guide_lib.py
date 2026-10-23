# Funções compartilhadas por journey.py e views.py: layout automático em camadas e as "ligações"
# (nós-fantasma que mostram, dentro de um capítulo, de qual entrada de outro capítulo uma raiz depende).


def external_links(ids, deps):
    """Dependências de fora do capítulo das entradas que não têm nenhuma dependência dentro dele (as raízes).
    Sem isso essas entradas ficariam soltas no mapa."""
    inside = set(ids)
    out = []
    for n in ids:
        ds = deps.get(n, [])
        if ds and not any(d in inside for d in ds):
            for d in ds:
                if d not in out:
                    out.append(d)
    return out


def layout(ids, deps, links=(), big=()):
    """Camadas pelas dependências: cada coluna (1.6 célula) é um nível; as ligações ficam na primeira coluna.
    Retorna {id: (x, y)} para as entradas e as ligações, já começando em (0, 0)."""
    nodes = list(links) + [n for n in ids if n not in links]
    inside = set(nodes)
    link_set = set(links)

    def ds(n):
        return [] if n in link_set else [p for p in deps.get(n, []) if p in inside]

    depth = {}

    def dep(n):
        if n not in depth:
            ps = ds(n)
            depth[n] = 0 if not ps else 1 + max(dep(p) for p in ps)
        return depth[n]

    for n in nodes:
        dep(n)
    pos = {}
    for d in range(max(depth.values()) + 1):
        col = [n for n in nodes if depth[n] == d]

        def want(n):
            ps = [pos[p][1] for p in ds(n) if p in pos]
            return sum(ps) / len(ps) if ps else None

        col.sort(key=lambda n: (want(n) if want(n) is not None else -1000, nodes.index(n)))
        wants = [want(n) for n in col]
        placed = []
        for i, n in enumerate(col):
            w = wants[i] if wants[i] is not None else (placed[-1] + 1 if placed else 0)
            if placed:
                gap = 1.25 if (n in big or col[i - 1] in big) else 1.0
                if w < placed[-1] + gap:
                    w = placed[-1] + gap
            placed.append(w)
        known = [w for w in wants if w is not None]
        target = sum(known) / len(known) if known else 0
        shift = target - sum(placed) / len(placed)
        # só sobe o grupo (nunca empurra para baixo do que já foi resolvido)
        if shift < 0:
            placed = [y + shift for y in placed]
        for n, y in zip(col, placed):
            pos[n] = (d * 1.6, y)
    if links:
        # segunda passada: cada ligação desce para perto das entradas que ela alimenta, sem encostar nas outras
        col = sorted(links, key=lambda n: pos[n][1])
        placed = []
        for n in col:
            kids = [pos[k][1] for k in ids if n in deps.get(k, []) and k in pos]
            w = sum(kids) / len(kids) if kids else pos[n][1]
            if placed and w < placed[-1] + 1:
                w = placed[-1] + 1
            placed.append(w)
            pos[n] = (pos[n][0], w)
    return normalize(pos)


def normalize(pos):
    mx = min(x for x, _ in pos.values())
    my = min(y for _, y in pos.values())
    return {k: (round(x - mx, 2), round(y - my, 2)) for k, (x, y) in pos.items()}


def place_links(pos, links, ids, deps, big=()):
    """Para capítulos com posições feitas à mão: põe cada ligação num lugar livre ao lado de quem depende dela."""
    pos = dict(pos)
    for link in links:
        kids = [k for k in ids if link in deps.get(k, [])]
        kids.sort(key=lambda k: pos[k][0])
        ax, ay = pos[kids[0]]
        cands = [(ax - 1.6, ay), (ax - 1.6, ay - 1), (ax - 1.6, ay + 1), (ax, ay - 1.2), (ax, ay + 1.2),
                 (ax - 1.6, ay - 2), (ax - 1.6, ay + 2), (ax - 3.2, ay), (ax + 1.6, ay - 1.2), (ax + 1.6, ay + 1.2)]
        for cx, cy in cands:
            if all(max(abs(cx - x), abs(cy - y)) >= (1.25 if k in big else 1.0) for k, (x, y) in pos.items()):
                pos[link] = (cx, cy)
                break
        else:
            pos[link] = (min(x for x, _ in pos.values()) - 1.6, ay)
    return normalize(pos)


def overlaps(pos, big=()):
    """Pares de nós perto demais (para conferência)."""
    out = []
    items = list(pos.items())
    for i in range(len(items)):
        for j in range(i + 1, len(items)):
            (a, (ax, ay)), (b, (bx, by)) = items[i], items[j]
            lim = 1.2 if (a in big or b in big) else 0.95
            if abs(ax - bx) < lim and abs(ay - by) < lim:
                out.append((a, b))
    return out
