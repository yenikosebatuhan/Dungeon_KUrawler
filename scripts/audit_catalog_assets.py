#!/usr/bin/env python3
"""Audit sprite catalog crops: bleed, structures, wall variants."""
from PIL import Image
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets")
FAIL = 0


def opaque_bbox(im, thresh=46):
    w, h = im.size
    minx, miny, maxx, maxy = w, h, -1, -1
    px = im.load()
    for y in range(h):
        for x in range(w):
            if px[x, y][3] > thresh:
                minx = min(minx, x)
                miny = min(miny, y)
                maxx = max(maxx, x)
                maxy = max(maxy, y)
    if maxx < minx:
        return None
    return (minx, miny, maxx - minx + 1, maxy - miny + 1)


def bleed_score(im):
    w, h = im.size
    if w < 2 or h < 2:
        return 0.0
    px = im.load()
    edge = inner = 0
    for y in range(h):
        for x in range(w):
            if px[x, y][3] < 46:
                continue
            if x == 0 or y == 0 or x == w - 1 or y == h - 1:
                edge += 1
            else:
                inner += 1
    total = edge + inner
    return edge / total if total else 0.0


def bottom_pad_for_row(row):
    if row == 0:
        return 22
    if row == 1:
        return 18
    if row <= 4:
        return 20
    return 24


def weapon_crop_region(sheet, col, row, tile=48):
    """Match TileRenderer.cropWeaponCell edge-touch expansion."""
    cx, cy = col * tile, row * tile
    cell = sheet.crop((cx, cy, cx + tile, cy + tile))
    bb = opaque_bbox(cell)
    if not bb:
        return None
    minx, miny, w, h = bb
    maxx, maxy = minx + w - 1, miny + h - 1
    touch_left = minx <= 1
    touch_top = miny <= 1
    touch_right = maxx >= tile - 2
    touch_bottom = maxy >= tile - 2
    pl = 14 if touch_left and col > 0 else 4
    pr = 14 if touch_right and col < 9 else 4
    pt = (14 if row > 0 else 6) if touch_top else (12 if row >= 3 else 4)
    pb = bottom_pad_for_row(row) if touch_bottom else (12 if row >= 3 else 4)
    x0 = max(0, cx - pl)
    y0 = max(0, cy - pt)
    x1 = min(sheet.width, cx + tile + pr)
    y1 = min(sheet.height, cy + tile + pb)
    return sheet.crop((x0, y0, x1, y1))


def audit_weapons():
    sheet = Image.open(os.path.join(ASSETS, "weapons_x3.png")).convert("RGBA")
    tile = 48
    bad = []
    for r in range(10):
        for c in range(10):
            reg = weapon_crop_region(sheet, c, r, tile)
            if reg is None:
                continue
            bb = opaque_bbox(reg)
            if not bb or bb[2] * bb[3] < 100:
                continue
            crop = reg.crop((bb[0], bb[1], bb[0] + bb[2], bb[1] + bb[3]))
            b = bleed_score(crop)
            aspect = crop.height / max(1, crop.width)
            limit = 0.20 if aspect > 1.8 else 0.12
            if b > limit:
                bad.append((r, c, round(b, 3)))
    if bad:
        print(f"WARN: {len(bad)} weapon crops border bleed >12% (sample): {bad[:8]}")
    else:
        print("OK: weapon expanded crops — no significant border bleed")


def audit_structures():
    global FAIL
    sim = Image.open(os.path.join(ASSETS, "walls and statics x3.png")).convert("RGBA")
    s3 = 3
    expected = {
        "Great Door": (152, 14, 96, 90),
        "Stone Archway": (40, 11, 96, 109),
        "Barred Window": (88, 8, 96, 112),
        "Red Carpet": (360, 24, 79, 92),
        "Tan Carpet": (392, 24, 79, 92),
        "Barrel Stack": (152, 56, 80, 72),
    }
    for name, (bx, by, bw, bh) in expected.items():
        x, y, w, h = bx * s3, by * s3, bw * s3, bh * s3
        crop = sim.crop((x, y, x + w, y + h))
        bb = opaque_bbox(crop)
        if not bb:
            print(f"FAIL: structure {name} is empty at base {bx},{by},{bw},{bh}")
            FAIL += 1
            continue
        fill = (bb[2] * bb[3]) / (w * h)
        trimmed = crop.crop((bb[0], bb[1], bb[0] + bb[2], bb[1] + bb[3]))
        b = bleed_score(trimmed)
        status = "OK" if b <= 0.15 and fill >= 0.15 else "WARN"
        print(f"{status}: structure {name} ({bw}x{bh} base, fill {fill:.0%}, bleed {b:.0%})")


def audit_wall_variants():
    sim = Image.open(os.path.join(ASSETS, "walls and statics x3.png")).convert("RGBA")
    variants = [
        ("Vine Wall", 3, 1),
        ("Water Grate", 4, 1),
        ("Caged Wall", 5, 1),
        ("Ring Wall", 6, 1),
        ("Red Torch Wall", 11, 1),
        ("Green Torch Wall", 12, 1),
        ("Blue Torch Wall", 13, 1),
    ]
    s3 = 3
    for name, col, row in variants:
        x, y = (col * 16 + 1) * s3, (row * 16 + 1) * s3
        crop = sim.crop((x, y, x + 14 * s3, y + 14 * s3))
        bb = opaque_bbox(crop)
        if not bb:
            print(f"FAIL: wall variant {name} empty")
            continue
        trimmed = crop.crop((bb[0], bb[1], bb[0] + bb[2], bb[1] + bb[3]))
        b = bleed_score(trimmed)
        print(f"{'OK' if b <= 0.2 else 'WARN'}: wall {name} (bleed {b:.0%})")


if __name__ == "__main__":
    print("=== Catalog asset audit ===\n")
    audit_weapons()
    print()
    audit_structures()
    print()
    audit_wall_variants()
    sys.exit(FAIL)
