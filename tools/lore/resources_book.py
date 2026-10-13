import json

BOOK = 'src/main/resources/assets/mistech/guide/book.json'
bk = json.load(open(BOOK, encoding='utf-8'))
cats = {c['id']: c for c in bk['categories']}


def free_spot(entries, x, y):
    while any(abs(e.get('x', -99) - x) < 1.0 and abs(e.get('y', -99) - y) < 0.9 for e in entries):
        y += 1.0
    return round(x, 2), round(y, 2)


def put(cat, near, entry):
    es = cats[cat]['entries']
    es[:] = [e for e in es if e['id'] != entry['id']]
    ref = next(e for c in bk['categories'] for e in c['entries'] if e['id'] == near)
    same = any(e['id'] == near for e in es)
    x, y = (ref['x'] + 1.6, ref['y']) if same else (max(e['x'] for e in es) + 1.6, 0.0)
    entry['x'], entry['y'] = free_spot(es, x, y)
    es.append(entry)




def place_views(pairs):
    for eid, near in pairs:
        for view in ('progress', 'dimension'):
            chs = bk['views'][view]
            for c in chs:
                c['entries'] = [e for e in c['entries'] if e['id'] != eid]
            chap = next((c for c in chs if any(e['id'] == near for e in c['entries'])), chs[-1])
            ref = next((e for e in chap['entries'] if e['id'] == near), chap['entries'][0])
            x, y = free_spot(chap['entries'], ref['x'] + 1.6, ref['y'])
            chap['entries'].append({'id': eid, 'x': x, 'y': y})


def save():
    with open(BOOK, 'w', encoding='utf-8') as f:
        json.dump(bk, f, ensure_ascii=False, indent=2)
        f.write('\n')
