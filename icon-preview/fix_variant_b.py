"""Redraw variant B centered in adaptive-icon safe zone and install to mipmaps.

Adaptive icon canvas is 108x108dp; only central 72dp (2/3) is guaranteed visible.
Foreground must therefore be scaled to ~0.69 and centered (same trick as the
original ic_launcher_foreground.xml).
"""
from PIL import Image, ImageDraw
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)))

# ---- palette ----
NAVY_TOP = (30, 41, 59)
NAVY_BOT = (10, 15, 30)
NEON = (56, 255, 219)
WHITE = (255, 255, 255)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def draw_shield(fg: Image.Image, cx, cy, w, s, lw, glow_layers=3):
    """Draw the neon shield centered at (cx, cy) with half-width w, scale s (for strokes)."""
    top_y = cy - w * 1.05
    bot_y = cy + w * 1.30
    # shield polygon (classic 6-point)
    shield = [
        (cx, top_y),
        (cx + w, top_y + 0.20 * w),
        (cx + w, top_y + 1.15 * w),
        (cx, bot_y),
        (cx - w, top_y + 1.15 * w),
        (cx - w, top_y + 0.20 * w),
    ]
    # soft outer glow (cheap multi-pass)
    for i in range(glow_layers, 0, -1):
        r = 10 * i
        d = ImageDraw.Draw(fg)
        d.polygon(shield, outline=(*NEON, max(0, 60 - i * 14)), width=lw + r)
    d = ImageDraw.Draw(fg)
    d.polygon(shield, outline=(*NEON, 255), width=lw)
    d.polygon(shield, fill=(*NEON, 24))
    # signal arcs inside upper shield
    arc_cy = top_y + 0.60 * w
    for r, alpha in [(0.30 * w, 255), (0.48 * w, 150), (0.66 * w, 80)]:
        d.arc([cx - r, arc_cy - r, cx + r, arc_cy + r], start=180, end=360,
              fill=(*NEON, alpha), width=max(2, int(0.06 * w)))
    # keyhole
    kr = 0.11 * w
    d.ellipse([cx - kr, arc_cy - 1.1 * kr, cx + kr, arc_cy + 0.9 * kr], fill=(*WHITE, 255))
    d.polygon([(cx - 0.07 * w, arc_cy + 0.7 * kr),
               (cx + 0.07 * w, arc_cy + 0.7 * kr),
               (cx + 0.10 * w, arc_cy + 1.8 * kr),
               (cx - 0.10 * w, arc_cy + 1.8 * kr)], fill=(*WHITE, 255))
    # base dot
    dr = 0.06 * w
    d.ellipse([cx - dr, bot_y - 0.55 * w, cx + dr, bot_y - 0.35 * w], fill=(*NEON, 255))


def render(size: int) -> Image.Image:
    """Render the full square launcher icon at `size` px, adaptive-icon aware."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # full-bleed navy background (adaptive mask will crop corners)
    px = img.load()
    for y in range(size):
        t = y / max(1, size - 1)
        c = lerp(NAVY_TOP, NAVY_BOT, t)
        for x in range(size):
            px[x, y] = (c[0], c[1], c[2], 255)
    # foreground, scaled to safe zone (72/108 = 2/3) and centered
    fg_size = int(size * 2 / 3)
    fg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
    w = fg_size * 0.30  # shield half-width
    s = fg_size / 512.0
    lw = max(2, int(26 * s))
    draw_shield(fg, fg_size / 2, fg_size / 2, w, s, lw)
    off = (size - fg_size) // 2
    img.alpha_composite(fg, (off, off))
    return img


def rounded_mask(size, radius):
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size - 1, size - 1], radius, fill=255)
    return m


for dpi, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    img = render(px)
    base = os.path.join("..", "app", "src", "main", "res", f"mipmap-{dpi}")
    img.save(os.path.join(base, "ic_launcher.webp"), "WEBP", quality=95, method=4)
    img.save(os.path.join(base, "ic_launcher_round.webp"), "WEBP", quality=95, method=4)
print("installed scaled variant B")

# 512 preview
render(512).save("out/variant_b_fixed_preview_512.png")
# round preview (55% crop circle mask, like most launchers)
prev = render(512)
prev.putalpha(rounded_mask(512, 115))
prev.save("out/variant_b_fixed_round_preview_512.png")
print("previews written")
