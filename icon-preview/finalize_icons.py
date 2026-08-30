"""Regenerate PNG mipmaps to match the new vector (safe-zone scaled, centered)."""
from PIL import Image, ImageDraw
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)))

NAVY_TOP = (30, 41, 59)
NAVY_BOT = (10, 15, 30)
NEON = (56, 255, 219)
WHITE = (255, 255, 255)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def draw_shield(d, cx, cy, w, s):
    """Draw shield centered at (cx, cy), half-width w. s = scale for strokes."""
    lw = max(2, int(22 * s))
    top_y = cy - w * 1.05
    bot_y = cy + w * 1.25
    shield = [
        (cx, top_y),
        (cx + w, top_y + 0.20 * w),
        (cx + w, top_y + 1.10 * w),
        (cx, bot_y),
        (cx - w, top_y + 1.10 * w),
        (cx - w, top_y + 0.20 * w),
    ]
    # outer glow
    for i in range(3, 0, -1):
        d.polygon(shield, outline=(*NEON, max(0, 50 - i * 12)), width=lw + 8 * i)
    d.polygon(shield, outline=(*NEON, 255), width=lw)
    d.polygon(shield, fill=(*NEON, 20))
    # arcs
    arc_cy = cy - 0.05 * w
    for r, alpha in [(0.30 * w, 255), (0.48 * w, 140), (0.66 * w, 75)]:
        d.arc([cx - r, arc_cy - r, cx + r, arc_cy + r], start=180, end=360,
              fill=(*NEON, alpha), width=max(2, int(0.055 * w)))
    # keyhole
    kr = 0.10 * w
    d.ellipse([cx - kr, arc_cy - 1.1 * kr, cx + kr, arc_cy + 0.9 * kr], fill=(*WHITE, 255))
    d.polygon([(cx - 0.065 * w, arc_cy + 0.8 * kr),
               (cx + 0.065 * w, arc_cy + 0.8 * kr),
               (cx + 0.09 * w, arc_cy + 1.9 * kr),
               (cx - 0.09 * w, arc_cy + 1.9 * kr)], fill=(*WHITE, 240))
    # base dot
    dr = 0.055 * w
    d.ellipse([cx - dr, bot_y - 0.55 * w, cx + dr, bot_y - 0.35 * w], fill=(*NEON, 255))


def render(size):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y in range(size):
        t = y / max(1, size - 1)
        c = lerp(NAVY_TOP, NAVY_BOT, t)
        for x in range(size):
            px[x, y] = (c[0], c[1], c[2], 255)
    # safe zone: 72/108 = 2/3
    fg_size = int(size * 2 / 3)
    fg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
    w = fg_size * 0.30
    s = fg_size / 512.0
    draw_shield(ImageDraw.Draw(fg), fg_size / 2, fg_size / 2, w, s)
    off = (size - fg_size) // 2
    img.alpha_composite(fg, (off, off))
    return img


for dpi, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    img = render(px)
    base = os.path.join("..", "app", "src", "main", "res", f"mipmap-{dpi}")
    img.save(os.path.join(base, "ic_launcher.webp"), "WEBP", quality=95, method=4)
    img.save(os.path.join(base, "ic_launcher_round.webp"), "WEBP", quality=95, method=4)
print("mipmaps updated")

render(512).save("out/final_preview_512.png")
print("preview saved")
