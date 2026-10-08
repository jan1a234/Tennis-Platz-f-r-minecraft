#!/usr/bin/env python3
"""Erzeugt alle Texturen der Mod (Skins, Items, Icon).

Die Skins sind eigene Pixel-Figuren, die vom Stil berühmter Tennis-Legenden
inspiriert sind (Frisur, Stirnband, Outfit). Es sind keine Abbilder echter Personen.

Aufruf:  python3 tools/generate_textures.py
"""
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "tennis"
CLIENT_ASSETS = ROOT / "src" / "client" / "resources" / "assets" / "tennis"


def hexc(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def shade(c, amount):
    return tuple(max(0, min(255, int(v + amount))) for v in c[:3]) + (c[3],)


# ---------------------------------------------------------------------------
# Skin-Layout (klassisches 64x64-Format)
# ---------------------------------------------------------------------------
def faces(ox, oy, w, h, d):
    """UV-Rechtecke eines Minecraft-Quaders: name -> (x, y, breite, hoehe)."""
    return {
        "top": (ox + d, oy, w, d),
        "bottom": (ox + d + w, oy, w, d),
        "right": (ox, oy + d, d, h),
        "front": (ox + d, oy + d, w, h),
        "left": (ox + d + w, oy + d, d, h),
        "back": (ox + 2 * d + w, oy + d, w, h),
    }


PARTS = {
    "head": ((0, 0), (32, 0), (8, 8, 8)),
    "body": ((16, 16), (16, 32), (8, 12, 4)),
    "right_arm": ((40, 16), (40, 32), (4, 12, 4)),
    "left_arm": ((32, 48), (48, 48), (4, 12, 4)),
    "right_leg": ((0, 16), (0, 32), (4, 12, 4)),
    "left_leg": ((16, 48), (0, 48), (4, 12, 4)),
}


class Skin:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.rng = random.Random(seed)

    def paint(self, part, painter, overlay=False):
        base, over, (w, h, d) = PARTS[part]
        ox, oy = over if overlay else base
        for face, (fx, fy, fw, fh) in faces(ox, oy, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    c = painter(face, x, y, fw, fh)
                    if c is None:
                        continue
                    if c[3] == 255:
                        c = shade(c, self.rng.randint(-7, 7))
                    self.img.putpixel((fx + x, fy + y), c)


def make_skin(spec, seed):
    s = Skin(seed)
    skin = hexc(spec["skin"])
    hair = hexc(spec["hair"])
    shirt = hexc(spec["shirt"])
    trim = hexc(spec.get("trim", spec["shirt"]))
    shorts = hexc(spec["shorts"])
    sock = hexc(spec.get("socks", "#f4f4f4"))
    shoe = hexc(spec.get("shoes", "#ececec"))
    sole = hexc(spec.get("sole", "#9a9a9a"))
    band = hexc(spec["headband"]) if spec.get("headband") else None
    eyes = hexc(spec.get("eyes", "#3a5a8a"))
    stripes = hexc(spec["stripes"]) if spec.get("stripes") else None
    long_hair = spec.get("long_hair", False)
    ponytail = spec.get("ponytail", False)
    curly = spec.get("curly", False)
    sleeveless = spec.get("sleeveless", False)
    dress = spec.get("dress", False)
    long_shorts = spec.get("long_shorts", False)
    freckles = spec.get("freckles", False)
    hair_dark = shade(hair, -30)

    # ---- Kopf ------------------------------------------------------------
    def head(face, x, y, w, h):
        if face == "top":
            return hair
        if face == "bottom":
            return skin
        if face == "back":
            if long_hair or ponytail:
                return hair if y < 8 else skin
            return hair if y < 6 else (hair_dark if y == 6 else skin)
        if face in ("left", "right"):
            if y < 2:
                return hair
            if y < 4 and (face == "right" and x < 5 or face == "left" and x > 2):
                return hair
            if long_hair and (face == "right" and x < 3 or face == "left" and x > 4):
                return hair
            return skin
        # front
        if y == 0:
            return hair
        if y == 1:
            return hair_dark if curly and x % 2 else hair
        if y == 2:
            if spec.get("fringe", True) and x in (0, 1, 6, 7):
                return hair
            return skin
        if y == 3:
            return hair_dark if x in (1, 2, 5, 6) else skin
        if y == 4:
            if x in (1, 6):
                return (250, 250, 250, 255)
            if x in (2, 5):
                return eyes
            if long_hair and x in (0, 7):
                return hair
            return skin
        if y == 5:
            if freckles and x in (1, 6):
                return shade(skin, -35)
            if x in (3, 4):
                return shade(skin, -22)
            if long_hair and x in (0, 7):
                return hair
            return skin
        if y == 6:
            if x in (3, 4):
                return hexc(spec.get("mouth", "#a0524a"))
            if long_hair and x in (0, 7):
                return hair
            return skin
        return shade(skin, -10)

    s.paint("head", head)

    # Hut-Ebene: Stirnband, Locken, lange Haare, Pferdeschwanz
    def hat(face, x, y, w, h):
        if face in ("top", "bottom"):
            if curly and face == "top" and (x + y) % 3 == 0:
                return hair_dark
            return None
        if band and y == 2:
            if spec.get("band_stripe") and x % 4 == 1:
                return hexc(spec["band_stripe"])
            return band
        if band and spec.get("bandana_tail") and face == "back" and y in (3, 4) and x in (3, 4):
            return band
        if curly and y < 2:
            return hair if (x + y) % 2 else hair_dark
        if curly and face in ("left", "right", "back") and y < 5:
            return hair if (x * 3 + y) % 4 else hair_dark
        if long_hair and face == "back" and y >= 6:
            return hair
        if ponytail and face == "back" and 2 <= y <= 7 and x in (3, 4):
            return hair_dark if y % 2 else hair
        return None

    s.paint("head", hat, overlay=True)

    # ---- Körper ----------------------------------------------------------
    def body(face, x, y, w, h):
        if face == "top":
            return shirt
        if face == "bottom":
            return shorts if not dress else shirt
        if y >= 10:
            return shorts if not dress else shirt
        if face == "front":
            if y == 0 and x in (3, 4):
                return skin
            if y == 0 and not sleeveless:
                return trim
            if y == 1 and x in (3, 4) and spec.get("collar_open", True):
                return trim
            if spec.get("logo") and y == 3 and x == 1:
                return hexc(spec["logo"])
        if stripes and x % 2 == 0 and y > 0:
            return stripes
        if y == 9 and spec.get("waistband"):
            return hexc(spec["waistband"])
        return shirt

    s.paint("body", body)

    if dress:
        def skirt(face, x, y, w, h):
            if face in ("top", "bottom"):
                return None
            if y >= 9:
                return trim if y == 11 else shirt
            return None
        s.paint("body", skirt, overlay=True)

    # ---- Arme ------------------------------------------------------------
    def arm(side):
        def painter(face, x, y, w, h):
            sleeve_len = 0 if sleeveless else 4
            if face == "top":
                return skin if sleeveless else shirt
            if face == "bottom":
                return skin
            if y < sleeve_len:
                return trim if y == sleeve_len - 1 else shirt
            if spec.get("wristband") and y in (8, 9):
                return hexc(spec["wristband"])
            if y == 11:
                return shade(skin, -12)
            return skin
        return painter

    s.paint("right_arm", arm("right"))
    s.paint("left_arm", arm("left"))

    # ---- Beine -----------------------------------------------------------
    def leg(face, x, y, w, h):
        shorts_len = 8 if long_shorts else 5
        if dress:
            shorts_len = 0
        if face == "top":
            return shorts if not dress else skin
        if face == "bottom":
            return sole
        if y < shorts_len:
            return shorts
        if y >= 11:
            return sole if face != "front" or x % 3 else shade(sole, -20)
        if y >= 9:
            if face == "front" and y == 9 and spec.get("shoe_stripe"):
                return hexc(spec["shoe_stripe"])
            return shoe
        if y == 8:
            return sock
        return skin

    s.paint("right_leg", leg)
    s.paint("left_leg", leg)

    if dress:
        def skirt_leg(face, x, y, w, h):
            if face in ("top", "bottom"):
                return None
            if y < 3:
                return trim if y == 2 else shirt
            return None
        s.paint("right_leg", skirt_leg, overlay=True)
        s.paint("left_leg", skirt_leg, overlay=True)

    return s.img


# Eigene Figuren, inspiriert vom Stil bekannter Tennis-Legenden.
OUTFITS = {
    # Der Rotfuchs: rotblonder Wuschelkopf, Sommersprossen, klassisches Weiß
    "rotfuchs": dict(skin="#f2c6a6", hair="#d9793b", eyes="#4a7bb5", freckles=True,
                     shirt="#f7f7f7", trim="#c8323c", shorts="#f2f2f2", logo="#1f4fa0",
                     wristband="#f7f7f7", shoe_stripe="#1f4fa0"),
    # Der Eismann: lange blonde Haare, Stirnband, Nadelstreifen-Polo
    "eismann": dict(skin="#f0c8a8", hair="#e8cf86", eyes="#5d87b8", long_hair=True,
                    headband="#1d2f6b", band_stripe="#c22b33",
                    shirt="#fafafa", trim="#1d2f6b", stripes="#d4d9ea", shorts="#1d2f6b",
                    wristband="#1d2f6b", shoe_stripe="#1d2f6b"),
    # Der Hitzkopf: dunkle Locken, rotes Stirnband
    "hitzkopf": dict(skin="#efc19f", hair="#3a2618", eyes="#4c6f4a", curly=True,
                     headband="#d0202a", shirt="#ffffff", trim="#d0202a",
                     shorts="#ffffff", logo="#d0202a", shoe_stripe="#d0202a"),
    # Die Gräfin: blonder Pferdeschwanz, weißes Tenniskleid
    "graefin": dict(skin="#f3caa9", hair="#e9c46a", eyes="#5180b0", ponytail=True, fringe=False,
                    shirt="#ffffff", trim="#2d6cdf", shorts="#ffffff", dress=True,
                    mouth="#c0605a", shoe_stripe="#2d6cdf"),
    # Der Maestro: weißes Bandana, edles Creme-Weiß mit Goldakzenten
    "maestro": dict(skin="#eebf9c", hair="#4b3020", eyes="#5a4030", headband="#ffffff",
                    bandana_tail=True, shirt="#fbf8ef", trim="#c9a227", shorts="#fbf8ef",
                    logo="#c9a227", wristband="#ffffff", shoe_stripe="#c9a227"),
    # Der Matador: Bandana, ärmelloses Shirt, lange Hose bis übers Knie
    "matador": dict(skin="#d9a27c", hair="#2b1d14", eyes="#3a2a1a", headband="#ff6b1a",
                    bandana_tail=True, shirt="#7ed321", trim="#ff6b1a", shorts="#ffffff",
                    sleeveless=True, long_shorts=True, wristband="#ff6b1a", shoe_stripe="#ff6b1a"),
    # Die Königin: dunkle Zöpfe, kraftvolles Lila
    "koenigin": dict(skin="#7a4a2e", hair="#140d0a", eyes="#2a1a10", long_hair=True,
                     fringe=False, shirt="#7b2fbf", trim="#ff4fa3", shorts="#7b2fbf", dress=True,
                     mouth="#5a2a20", wristband="#ff4fa3", shoe_stripe="#ff4fa3"),
}


def make_racket():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    frame = hexc("#1b1b1b")
    accent = hexc("#d22f2f")
    strings = hexc("#e8e8e8")
    grip = hexc("#f0f0f0")
    grip_dark = hexc("#b0b0b0")
    cx, cy = 10.0, 5.5
    ang = math.radians(-45)
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            a = dx * math.cos(ang) - dy * math.sin(ang)
            b = dx * math.sin(ang) + dy * math.cos(ang)
            r = (a / 5.0) ** 2 + (b / 3.6) ** 2
            if 0.62 <= r <= 1.0:
                img.putpixel((x, y), accent if (x + y) % 5 == 0 else frame)
            elif r < 0.62:
                if (x + y) % 2 == 0:
                    img.putpixel((x, y), strings)
    # Herz und Griff
    for i in range(7):
        x, y = 6 - i, 9 + i
        if not (0 <= x < 16 and 0 <= y < 16):
            continue
        if i < 2:
            img.putpixel((x, y), frame)
            img.putpixel((x + 1, y), frame)
        else:
            img.putpixel((x, y), grip if i % 2 else grip_dark)
            if x + 1 < 16:
                img.putpixel((x + 1, y), grip_dark if i % 2 else grip)
    img.putpixel((7, 8), frame)
    img.putpixel((8, 9), frame)
    return img


def make_ball(size=16, radius=4.2):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    base = hexc("#d7ec3a")
    dark = hexc("#a9c22a")
    seam = hexc("#fbfbf0")
    c = size / 2
    for y in range(size):
        for x in range(size):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            d = math.hypot(dx, dy)
            if d <= radius:
                col = base if dx + dy < radius * 0.6 else dark
                # Naht: zwei gegenläufige Bögen
                if abs(math.hypot(dx + radius * 1.05, dy) - radius * 0.95) < 0.55 or \
                        abs(math.hypot(dx - radius * 1.05, dy) - radius * 0.95) < 0.55:
                    col = seam
                img.putpixel((x, y), col)
    return img


def make_icon():
    img = Image.new("RGBA", (128, 128), hexc("#2e7d32"))
    for y in range(128):
        for x in range(128):
            if x in range(14, 18) or x in range(110, 114) or y in range(14, 18) or y in range(110, 114) \
                    or (y in range(62, 66) and 14 <= x <= 114):
                img.putpixel((x, y), hexc("#ffffff"))
    ball = make_ball(64, 26).resize((64, 64), Image.NEAREST)
    img.alpha_composite(ball, (32, 30))
    return img


def main():
    skins_dir = CLIENT_ASSETS / "textures" / "skin"
    skins_dir.mkdir(parents=True, exist_ok=True)
    for i, (name, spec) in enumerate(OUTFITS.items()):
        make_skin(spec, seed=1000 + i).save(skins_dir / f"{name}.png")
    items = ASSETS / "textures" / "item"
    items.mkdir(parents=True, exist_ok=True)
    make_racket().save(items / "tennis_racket.png")
    make_ball().save(items / "tennis_ball.png")
    make_icon().save(ASSETS / "icon.png")
    print("Texturen erzeugt:", ", ".join(OUTFITS))


if __name__ == "__main__":
    main()
