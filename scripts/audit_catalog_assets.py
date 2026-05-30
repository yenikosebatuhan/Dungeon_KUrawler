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


def audit_weapons():
    sheet = Image.open(os.path.join(ASSETS, "weapons_x3.png")).convert("RGBA")
    tile, pad = 48, 12
    bad = []
    for r in range(10):
        for c in range(10):
            x0, y0 = max(0, c * tile - pad), max(0, r * tile - pad)
            x1, y1 = min(sheet.width, (c + 1) * tile + pad), min(sheet.height, (r + 1) * tile + pad)
            reg = sheet.crop((x0, y0, x1, y1))
            bb = opaque_bbox(reg)
            if not bb or bb[2] * bb[3] < 100:
                continue
            crop = reg.crop((bb[0], bb[1], bb[0] + bb[2], bb[1] + bb[3]))
            b = bleed_score(crop)
            if b > 0.12:
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
