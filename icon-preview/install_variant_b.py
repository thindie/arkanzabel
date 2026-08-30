from PIL import Image
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)))
for dpi, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    img = Image.open(f"out/variant_b_neon_shield_{dpi}.png").convert("RGBA")
    base = os.path.join("..", "app", "src", "main", "res", f"mipmap-{dpi}")
    img.save(os.path.join(base, "ic_launcher.webp"), "WEBP", quality=95, method=4)
    img.save(os.path.join(base, "ic_launcher_round.webp"), "WEBP", quality=95, method=4)
print("installed")
