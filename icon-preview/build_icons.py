"""Generate 3 candidate launcher icons (PNG previews + legacy fallbacks) for Arkanzabel."""
from PIL import Image, ImageDraw
import math, os, struct

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "out")
os.makedirs(OUT, exist_ok=True)

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def rounded_mask(size, radius):
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size - 1, size - 1], radius, fill=255)
    return m

def draw_variant_a(size):
    """Blue gradient + white circuit house (evolves current icon)."""
    top, bottom = (74, 158, 255), (21, 101, 192)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y in range(size):
        t = y / (size - 1)
        c = lerp(top, bottom, t)
        for x in range(size):
            px[x, y] = (c[0], c[1], c[2], 255)
    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(fg)
    s = size / 512.0
    lw = int(24 * s)
    # shield
    d.line([int(256*0.53*s), int(80*0.53*s), int(256*0.53*s), int(432*0.53*s)], fill=(255,255,255,60), width=int(4*s))
    # house outline
    pts = [(256,160),(352,240),(352,340),(220,340),(220,280),(292,280),(292,340),(352,340)]
    # house: roof from 160,240->256,160->352,240; walls 160,240->160,340 ; 352,240->352,340; bottom 160,340->352,340
    d.line([(160,240),(256,160),(352,240)], fill=(255,255,255,255), width=lw, joint="curve")
    d.line([(160,240),(160,340)], fill=(255,255,255,255), width=lw)
    d.line([(352,240),(352,340)], fill=(255,255,255,255), width=lw)
    d.line([(160,340),(352,340)], fill=(255,255,255,255), width=lw)
    # door
    d.line([(220,340),(220,280),(292,280),(292,340)], fill=(255,255,255,255), width=lw, joint="curve")
    # circuit dots
    for (cx, cy, r) in [(256,120,15),(160,340,10),(352,340,10)]:
        d.ellipse([cx-r, cy-r, cx+r, cy+r], fill=(138,180,248,255))
    # node connectors (subtle)
    d.line([(256,135),(256,160)], fill=(138,180,248,160), width=int(6*s))
    d.line([(256,135),(220,180)], fill=(138,180,248,0), width=1)
    img = Image.alpha_composite(img, fg)
    m = rounded_mask(size, int(size * 0.18))
    out = Image.new("RGBA", (size, size), (0,0,0,0))
    out.paste(img, (0,0), m)
    return out

def draw_variant_b(size):
    """Dark navy gradient + neon shield with signal arc + keyhole (VPN/security)."""
    top, bottom = (30, 41, 59), (10, 15, 30)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y in range(size):
        t = y / (size - 1)
        c = lerp(top, bottom, t)
        for x in range(size):
            px[x, y] = (c[0], c[1], c[2], 255)
    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(fg)
    s = size / 512.0
    lw = int(26 * s)
    # shield outline (neon cyan)
    cx = 256
    top_y, bot_y, w = 130, 390, 200
    shield = [
        (cx, top_y), (cx + w, top_y + 40), (cx + w, top_y + 150),
        (cx, bot_y), (cx - w, top_y + 150), (cx - w, top_y + 40),
    ]
    d.polygon(shield, outline=(56, 255, 219, 255), width=lw)
    # smooth shield as arcs: overdraw with quadratic feel -> just fill inner glow
    d.polygon(shield, fill=(56, 255, 219, 28))
    # signal arcs (3) inside upper shield
    for r, alpha in [(60, 255), (95, 160), (130, 90)]:
        bbox = [cx - r, top_y + 60 - r, cx + r, top_y + 60 + r]
        d.arc(bbox, start=180, end=360, fill=(56, 255, 219, alpha), width=int(12 * s))
    # keyhole
    d.ellipse([cx-22, top_y+45, cx+22, top_y+89], fill=(255,255,255,255))
    d.polygon([(cx-14, top_y+80),(cx+14, top_y+80),(cx+20, top_y+128),(cx-20, top_y+128)], fill=(255,255,255,255))
    # base dot
    d.ellipse([cx-12, bot_y-58, cx+12, bot_y-34], fill=(56, 255, 219, 255))
    img = Image.alpha_composite(img, fg)
    m = rounded_mask(size, int(size * 0.18))
    out = Image.new("RGBA", (size, size), (0,0,0,0))
    out.paste(img, (0,0), m)
    return out

def draw_variant_c(size):
    """Teal-green gradient + globe with orbit ring (global networking)."""
    top, bottom = (45, 212, 191), (16, 122, 140)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y in range(size):
        t = y / (size - 1)
        c = lerp(top, bottom, t)
        for x in range(size):
            px[x, y] = (c[0], c[1], c[2], 255)
    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(fg)
    s = size / 512.0
    cx, cy, r = 256, 256, 150
    lw = int(20 * s)
    # globe
    d.ellipse([cx-r, cy-r, cx+r, cy+r], outline=(255,255,255,255), width=lw)
    # meridians
    for rx in (r*0.55,):
        d.ellipse([cx-rx, cy-r, cx+rx, cy+r], outline=(255,255,255,220), width=int(10*s))
    d.line([(cx-r, cy), (cx+r, cy)], fill=(255,255,255,220), width=int(10*s))
    d.ellipse([cx-r, cy-r*0.62, cx+r, cy+r*0.62], outline=(255,255,255,140), width=int(8*s))
    # orbit ellipse (tilted)
    orx, ory = r + 52, r * 0.42
    ang = -24 * math.pi / 180
    for i in range(0, 360, 2):
        a = math.radians(i)
        x = orx * math.cos(a)
        y = ory * math.sin(a)
        xr = x * math.cos(ang) - y * math.sin(ang) + cx
        yr = x * math.sin(ang) + y * math.cos(ang) + cy
        # mask: hide the part that goes "behind" the globe (upper half of orbit)
        if yr > cy - r*0.15 or (xr < cx - r and yr < cy):
            d.point((int(xr), int(yr)), fill=(255,255,255,255))
    # dot on orbit
    a = math.radians(40)
    x = orx * math.cos(a); y = ory * math.sin(a)
    xr = x * math.cos(ang) - y * math.sin(ang) + cx
    yr = x * math.sin(ang) + y * math.cos(ang) + cy
    d.ellipse([xr-16, yr-16, xr+16, yr+16], fill=(255,255,255,255))
    img = Image.alpha_composite(img, fg)
    m = rounded_mask(size, int(size * 0.18))
    out = Image.new("RGBA", (size, size), (0,0,0,0))
    out.paste(img, (0,0), m)
    return out

VARIANTS = {
    "variant_a_circuit_house": draw_variant_a,
    "variant_b_neon_shield": draw_variant_b,
    "variant_c_orbit_globe": draw_variant_c,
}

MIPMAP = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

for name, fn in VARIANTS.items():
    for dpi, px in MIPMAP.items():
        img = fn(px)
        path = os.path.join(OUT, f"{name}_{dpi}.png")
        img.save(path)
    # 512 preview
    fn(512).save(os.path.join(OUT, f"{name}_preview_512.png"))
    print("done", name)
print("ALL DONE")
