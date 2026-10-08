def repivot(part, new_pivot):
    """Muda o pivô de uma parte sem rotação sem mudar a pose de descanso."""
    assert part.rot == (0, 0, 0)
    dx, dy, dz = [n - o for n, o in zip(new_pivot, part.pivot)]
    part.pivot = tuple(new_pivot)
    for b in part.boxes:
        x, y, z = b.origin
        b.origin = (x - dx, y - dy, z - dz)
    for c in part.model.parts:
        if c.parent is part:
            x, y, z = c.pivot
            c.pivot = (x - dx, y - dy, z - dz)
