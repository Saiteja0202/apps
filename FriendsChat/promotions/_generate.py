# GenZ promo kit — advanced edition.
# Techniques: aurora MESH gradients, film GRAIN, GLASSMORPHISM cards, neon GLOW,
# supersampled vector logo, a phone-mockup product shot, die-cut stickers, and
# real HD MP4 promo videos (drifting mesh + floating hearts + heartbeat logo).
import math, os, glob
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops
import imageio.v2 as imageio

ROOT = os.path.dirname(os.path.abspath(__file__))
APP = os.path.dirname(ROOT)  # FriendsChat/  (has the CA*.jpg photos)
for sub in ("stickers", "banners", "videos"):
    os.makedirs(os.path.join(ROOT, sub), exist_ok=True)
    for f in glob.glob(os.path.join(ROOT, sub, "*.*")):
        os.remove(f)

# ---------------- Brand ----------------
C1 = (124, 108, 240); C2 = (176, 124, 232); C3 = (255, 143, 177)
HOT = (255, 96, 158); CYAN = (124, 226, 234); MINT = (111, 224, 206)
ZP = (91, 52, 200); INK = (38, 20, 70); WHITE = (255, 255, 255)

FB = "C:/Windows/Fonts/seguibl.ttf"    # Segoe UI Black
FBD = "C:/Windows/Fonts/segoeuib.ttf"  # Bold
FRG = "C:/Windows/Fonts/segoeui.ttf"
def font(p, s):
    try: return ImageFont.truetype(p, s)
    except Exception: return ImageFont.load_default()

# ---------------- numpy mesh + grain ----------------
def mesh(w, h, base, blobs):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    img = np.zeros((h, w, 3), np.float32); img[:] = base
    R = float(max(w, h))
    for cx, cy, r, col, st in blobs:
        d = np.sqrt((xx - cx * w) ** 2 + (yy - cy * h) ** 2)
        m = (np.clip(1.0 - d / (r * R), 0, 1) ** 2) * st
        for c in range(3):
            img[..., c] = img[..., c] * (1 - m) + col[c] * m
    return img

def grain(arr, sigma=6.0):
    n = np.random.normal(0, sigma, arr.shape[:2])[..., None]
    return np.clip(arr + n, 0, 255)

def to_pil(arr):
    return Image.fromarray(arr.astype(np.uint8), "RGB").convert("RGBA")

AURORA = [
    (0.16, 0.12, 0.55, (155, 145, 255), 0.95),
    (0.86, 0.10, 0.50, (255, 150, 190), 0.85),
    (0.82, 0.90, 0.62, HOT, 0.85),
    (0.10, 0.88, 0.48, (132, 222, 226), 0.60),
    (0.50, 0.46, 0.42, (205, 175, 255), 0.45),
]
def aurora(w, h, g=6.0):
    return to_pil(grain(mesh(w, h, (104, 84, 206), AURORA), g))

# ---------------- vector logo ----------------
def rounded_mask(size, rad):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=rad, fill=255)
    return m

def heart_pts(cx, cy, s):
    p = []
    for i in range(0, 629, 3):
        t = i / 100.0
        x = 16 * math.sin(t) ** 3
        y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        p.append((cx + x * s, cy - y * s))
    return p

def z_polys(x0, y0, s):
    def p(c): return [(x0 + a * s, y0 + b * s) for a, b in c]
    return [p([(22, 22), (86, 22), (86, 36), (22, 36)]),
            p([(66, 36), (86, 36), (42, 76), (22, 76)]),
            p([(22, 76), (86, 76), (86, 90), (22, 90)])]

def star(d, cx, cy, r, col):
    d.polygon([(cx, cy - r), (cx + .28 * r, cy - .28 * r), (cx + r, cy), (cx + .28 * r, cy + .28 * r),
               (cx, cy + r), (cx - .28 * r, cy + .28 * r), (cx - r, cy), (cx - .28 * r, cy - .28 * r)], fill=col)

def badge(px, heart_scale=1.0):
    """Supersampled icon badge as a transparent RGBA tile."""
    SS = 4; w = px * SS
    g = to_pil(mesh(w, w, C1, [(0.85, 0.85, 0.9, C3, 1.0), (0.5, 0.5, 0.7, C2, 0.6)]))
    tile = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    tile.paste(g, (0, 0), rounded_mask((w, w), int(w * 0.24)))
    d = ImageDraw.Draw(tile); s = w / 108.0
    for poly in z_polys(0, 0, s): d.polygon(poly, fill=ZP + (235,))
    d.polygon(heart_pts(54 * s, 57 * s, 1.35 * s * heart_scale), fill=WHITE)
    star(d, 80 * s, 27 * s, 7 * s, WHITE)
    star(d, 30 * s, 70 * s, 5 * s, MINT + (255,))
    return tile.resize((px, px), Image.LANCZOS)

def glow(silhouette, color, radius, gain=1.6):
    base = Image.new("RGBA", silhouette.size, (0, 0, 0, 0))
    solid = Image.new("RGBA", silhouette.size, color + (255,))
    base.paste(solid, (0, 0), silhouette.split()[-1])
    base = base.filter(ImageFilter.GaussianBlur(radius))
    a = base.split()[-1].point(lambda v: min(255, int(v * gain)))
    base.putalpha(a)
    return base

def paste_logo(img, cx, cy, size, with_glow=True):
    b = badge(size)
    x, y = int(cx - size / 2), int(cy - size / 2)
    if with_glow:
        sh = Image.new("RGBA", img.size, (0, 0, 0, 0))
        sh.alpha_composite(b, (x, y + int(size * 0.05)))
        dark = Image.new("RGBA", img.size, (18, 8, 40, 0)); dark.putalpha(sh.split()[-1])
        dark = dark.filter(ImageFilter.GaussianBlur(size * 0.06))
        dark.putalpha(dark.split()[-1].point(lambda v: int(v * 0.55)))
        img.alpha_composite(dark)
    img.alpha_composite(b, (x, y))

# ---------------- text ----------------
def measure(d, t, f):
    bb = d.textbbox((0, 0), t, font=f); return bb[2] - bb[0], bb[3] - bb[1], bb

def fit_font(d, text, path, max_w, size):
    """Shrink a font until the widest line of `text` fits within max_w."""
    f = font(path, size)
    while size > 14 and max((measure(d, ln, f)[0] for ln in text.split("\n")), default=0) > max_w:
        size -= 4; f = font(path, size)
    return f

def centered(d, cx, y, t, f, fill=WHITE, shadow=72, glowcol=None, img=None):
    w, h, bb = measure(d, t, f); x = cx - w / 2 - bb[0]
    if glowcol is not None and img is not None:
        gl = Image.new("RGBA", img.size, (0, 0, 0, 0))
        ImageDraw.Draw(gl).text((x, y), t, font=f, fill=glowcol + (255,))
        img.alpha_composite(gl.filter(ImageFilter.GaussianBlur(max(6, h // 8))))
    if shadow:
        off = max(2, h // 26)
        d.text((x + off, y + off), t, font=f, fill=(30, 14, 60, shadow))
    d.text((x, y), t, font=f, fill=fill)
    return h

def wordmark(d, cx, y, size, shadow=True):
    f = font(FB, size); gen, z = "Gen", "Z"
    wg = measure(d, gen, f)[0]; wz = measure(d, z, f)[0]; x = cx - (wg + wz) / 2
    if shadow:
        o = max(3, size // 26)
        d.text((x + o, y + o), gen, font=f, fill=(28, 12, 56, 120))
        d.text((x + wg + o, y + o), z, font=f, fill=(28, 12, 56, 120))
    d.text((x, y), gen, font=f, fill=WHITE)
    d.text((x + wg, y), z, font=f, fill=MINT)

def pill(img, cx, y, text, f, bg=MINT, fg=INK, padx=None, pady=None):
    d = ImageDraw.Draw(img); w, h, bb = measure(d, text, f)
    padx = padx or int(h * 0.9); pady = pady or int(h * 0.55)
    x0, x1 = cx - w / 2 - padx, cx + w / 2 + padx; y1 = y + h + 2 * pady
    d.rounded_rectangle([x0, y, x1, y1], radius=int((y1 - y) / 2), fill=bg + (255,))
    d.text((cx - w / 2 - bb[0], y + pady - bb[1]), text, font=f, fill=fg + (255,) if isinstance(fg, tuple) else fg)
    return y1

def chip(d, x, y, text, f, bg, fg=WHITE):
    w, h, bb = measure(d, text, f); padx = int(h * 0.7); pady = int(h * 0.42)
    d.rounded_rectangle([x, y, x + w + 2 * padx, y + h + 2 * pady], radius=int((h + 2 * pady) / 2),
                        fill=bg + (235,))
    d.text((x + padx - bb[0], y + pady - bb[1]), text, font=f, fill=fg + (255,))
    return x + w + 2 * padx

# ---------------- glassmorphism ----------------
def glass(img, box, radius=None, tint=(255, 255, 255, 46)):
    x0, y0, x1, y1 = [int(v) for v in box]; w, h = x1 - x0, y1 - y0
    radius = radius or int(min(w, h) * 0.12)
    region = img.crop((x0, y0, x1, y1)).filter(ImageFilter.GaussianBlur(28))
    ov = Image.new("RGBA", (w, h), tint)
    region.alpha_composite(ov)
    m = rounded_mask((w, h), radius)
    img.paste(region, (x0, y0), m)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([x0, y0, x1 - 1, y1 - 1], radius=radius, outline=(255, 255, 255, 90), width=max(2, w // 300))

# ---------------- phone mockup ----------------
def load_photo(name, size):
    im = Image.open(os.path.join(APP, name)).convert("RGB")
    tw, th = size; sr = tw / th; r = im.width / im.height
    if r > sr:
        nw = int(im.height * sr); im = im.crop(((im.width - nw) // 2, 0, (im.width + nw) // 2, im.height))
    else:
        nh = int(im.width / sr); im = im.crop((0, (im.height - nh) // 2, im.width, (im.height + nh) // 2))
    return im.resize(size, Image.LANCZOS).convert("RGBA")

def phone(height, photo="CA (3).jpg", name="Aanya, 23", dist="2 km away", match="92% match"):
    W = int(height * 0.50); H = height
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    rad = int(W * 0.16)
    d.rounded_rectangle([0, 0, W - 1, H - 1], radius=rad, fill=(18, 12, 30, 255))  # bezel
    inset = int(W * 0.035); sx0, sy0, sx1, sy1 = inset, inset, W - inset, H - inset
    srad = rad - inset
    screen = Image.new("RGBA", (sx1 - sx0, sy1 - sy0), (0, 0, 0, 0))
    ph = load_photo(photo, (sx1 - sx0, sy1 - sy0))
    smask = rounded_mask(ph.size, srad)
    # bottom gradient scrim for legibility
    sc = np.zeros((ph.size[1], ph.size[0], 4), np.uint8)
    grad = (np.linspace(0, 1, ph.size[1]) ** 2.2 * 210).astype(np.uint8)
    sc[..., 3] = grad[:, None]
    ph.alpha_composite(Image.fromarray(sc, "RGBA"))
    screen.paste(ph, (0, 0))
    img.paste(screen, (sx0, sy0), smask)
    sd = ImageDraw.Draw(img)
    # match chip top
    chip(sd, sx0 + int(W * 0.06), sy0 + int(W * 0.06), match, font(FBD, int(W * 0.058)), HOT)
    # name + distance bottom
    nf = font(FB, int(W * 0.11)); df = font(FBD, int(W * 0.05))
    sd.text((sx0 + int(W * 0.07), sy1 - int(W * 0.42)), name, font=nf, fill=WHITE)
    sd.text((sx0 + int(W * 0.07), sy1 - int(W * 0.28)), "● " + dist, font=df, fill=(235, 225, 255, 255))
    # like / pass buttons (drawn as shapes — no font-glyph dependency)
    r = int(W * 0.085); cyb = sy1 - int(W * 0.11)
    cxp = sx0 + (sx1 - sx0) * 0.30
    sd.ellipse([cxp - r, cyb - r, cxp + r, cyb + r], fill=(255, 255, 255, 255))
    lw = max(3, int(r * 0.20)); o = r * 0.40
    sd.line([cxp - o, cyb - o, cxp + o, cyb + o], fill=HOT + (255,), width=lw)
    sd.line([cxp - o, cyb + o, cxp + o, cyb - o], fill=HOT + (255,), width=lw)
    cxl = sx0 + (sx1 - sx0) * 0.70
    sd.ellipse([cxl - r, cyb - r, cxl + r, cyb + r], fill=HOT + (255,))
    sd.polygon(heart_pts(cxl, cyb + r * 0.10, r / 15.5), fill=WHITE)
    return img

# ---------------- floating hearts (for video + accents) ----------------
def mini_heart(px, col=WHITE, a=255):
    t = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    ImageDraw.Draw(t).polygon(heart_pts(px / 2, px * 0.56, px / 38.0), fill=col + (a,))
    return t

# =================================================================== BANNERS
def save(img, sub, name, q=92):
    img.convert("RGB").save(os.path.join(ROOT, sub, name), quality=q)
    print(sub + "/" + name)

def hero(name, w, h, tagline, cta="Download now", vertical=False):
    img = aurora(w, h)
    d = ImageDraw.Draw(img)
    S = min(w, h)
    paste_logo(img, w // 2, int(h * (0.20 if vertical else 0.26)), int(S * 0.30))
    d = ImageDraw.Draw(img)
    wy = int(h * (0.36 if vertical else 0.50))
    wordmark(d, w // 2, wy, int(S * 0.20))
    ty = wy + int(S * 0.24)
    centered(d, w // 2, ty, tagline, fit_font(d, tagline, FBD, int(w * 0.88), int(S * 0.058)),
             img=img, glowcol=(60, 30, 90))
    # social-proof chips (rating star is drawn, not a font glyph)
    cf = font(FBD, int(S * 0.034)); cyc = ty + int(S * 0.13)
    ssz = int(S * 0.026); ipad = int(S * 0.022); spad = int(S * 0.03)
    items = [("4.9", MINT, True), ("Now live", HOT, False), ("Free • 18+", (150, 140, 255), False)]
    widths = [measure(d, t, cf)[0] + (ssz + ipad if ic else 0) + 2 * spad for t, _, ic in items]
    gap = int(S * 0.022); cx = w // 2 - (sum(widths) + gap * 2) / 2
    for (t, co, ic), ww in zip(items, widths):
        h2 = measure(d, t, cf)[1]; pady = int(h2 * 0.55); y1 = cyc + h2 + 2 * pady
        d.rounded_rectangle([cx, cyc, cx + ww, y1], radius=int((y1 - cyc) / 2), fill=co + (235,))
        fg = INK if co == MINT else WHITE; tx = cx + spad
        if ic:
            star(d, tx + ssz / 2, (cyc + y1) / 2, ssz / 2, fg + (255,)); tx += ssz + ipad
        bb = d.textbbox((0, 0), t, font=cf); d.text((tx - bb[0], cyc + pady - bb[1]), t, font=cf, fill=fg + (255,))
        cx += ww + gap
    pill(img, w // 2, cyc + int(S * 0.15), cta, font(FB, int(S * 0.05)))
    save(img, "banners", name)

def mockup_banner(name, w, h, headline, photo="CA (3).jpg", landscape=False):
    img = aurora(w, h); d = ImageDraw.Draw(img); S = min(w, h)
    ph = phone(int(h * (0.80 if not landscape else 0.86)), photo=photo)
    if landscape:
        img.alpha_composite(glow(ph, (40, 20, 70), 40, 0.8), (int(w * 0.60), int(h * 0.07)))
        img.alpha_composite(ph, (int(w * 0.60), int(h * 0.07)))
        lx = int(w * 0.07); paste_logo(img, lx + int(S * 0.09), int(h * 0.18), int(S * 0.18))
        d = ImageDraw.Draw(img)
        wordmark(d, lx + int(S * 0.30), int(h * 0.12), int(S * 0.15))
        d.text((lx, int(h * 0.40)), headline, font=font(FB, int(S * 0.075)), fill=WHITE)
        d.text((lx, int(h * 0.40) + int(S * 0.10)), "Match. Chat. Vibe.", font=font(FBD, int(S * 0.045)),
               fill=(235, 225, 255, 255))
        pill(img, lx + int(S * 0.16), int(h * 0.62), "Download now", font(FB, int(S * 0.044)))
    else:
        px = (w - ph.width) // 2
        img.alpha_composite(glow(ph, (40, 20, 70), 46, 0.8), (px, int(h * 0.20)))
        img.alpha_composite(ph, (px, int(h * 0.20)))
        d = ImageDraw.Draw(img)
        hf = fit_font(d, headline, FB, int(w * 0.92), int(S * 0.072))
        centered(d, w // 2, int(h * 0.045), headline, hf, img=img, glowcol=(60, 30, 90))
        wordmark(d, w // 2, int(h * 0.125), int(S * 0.082))
    save(img, "banners", name)

def carousel(idx, total, w, h, big, small=None, photo=None):
    img = aurora(w, h); d = ImageDraw.Draw(img); S = min(w, h)
    paste_logo(img, int(w * 0.13), int(h * 0.10), int(S * 0.12))
    d = ImageDraw.Draw(img)
    wordmark(d, int(w * 0.30), int(h * 0.072), int(S * 0.07))
    if photo:
        ph = phone(int(h * 0.40), photo=photo)
        img.alpha_composite(glow(ph, (30, 14, 60), 34, 0.7), ((w - ph.width) // 2, int(h * 0.18)))
        img.alpha_composite(ph, ((w - ph.width) // 2, int(h * 0.18)))
        d = ImageDraw.Draw(img)
    if big:
        bf = fit_font(d, big, FB, int(w * 0.84), int(S * 0.085))
        d.multiline_text((int(w * 0.08), int(h * 0.24)), big, font=bf, fill=WHITE, spacing=int(S * 0.02))
    if small:
        cy0 = int(h * 0.62) if photo else int(h * 0.50)
        cy1 = int(h * 0.86)
        glass(img, (int(w * 0.07), cy0, int(w * 0.93), cy1))
        d = ImageDraw.Draw(img)
        bf = font(FBD, int(S * 0.046))
        for i, line in enumerate(small):
            d.text((int(w * 0.12), cy0 + int(S * 0.045) + i * int(S * 0.072)), line, font=bf, fill=WHITE)
    dn = int(S * 0.012)
    for i in range(total):
        cx = w // 2 - (total * 3 * dn) / 2 + i * 3 * dn
        col = MINT if i == idx else (255, 255, 255); a = 255 if i == idx else 110
        d.ellipse([cx, int(h * 0.93), cx + 2 * dn, int(h * 0.93) + 2 * dn], fill=col + (a,))
    save(img, "banners", f"ig_carousel_{idx+1}_2048.png")

# =================================================================== STICKERS
def diecut(subject, pad=70, border=34):
    w, h = subject.size; W, H = w + 2 * pad, h + 2 * pad
    canvas = Image.new("RGBA", (W, H), (0, 0, 0, 0)); canvas.alpha_composite(subject, (pad, pad))
    a = canvas.split()[-1]
    dil = a.filter(ImageFilter.MaxFilter(border * 2 + 1))
    white = Image.new("RGBA", (W, H), (255, 255, 255, 0)); white.putalpha(dil)
    whitefill = Image.new("RGBA", (W, H), (255, 255, 255, 255)); whitefill.putalpha(dil)
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0)); shadow.putalpha(dil)
    shadow = shadow.filter(ImageFilter.GaussianBlur(22))
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    out.alpha_composite(Image.composite(Image.new("RGBA", (W, H), (20, 10, 40, 130)), out, shadow.split()[-1]),
                        (0, 14))
    out.alpha_composite(whitefill); out.alpha_composite(canvas)
    return out

def sticker_badge():
    diecut(badge(760), pad=60, border=30).resize((1024, 1024), Image.LANCZOS).save(
        os.path.join(ROOT, "stickers", "logo_diecut_1024.png")); print("stickers/logo_diecut_1024.png")

def sticker_text(name, line, accent, accent_col=MINT):
    sub = Image.new("RGBA", (760, 560), (0, 0, 0, 0)); d = ImageDraw.Draw(sub)
    b = badge(200); sub.alpha_composite(b, (760 // 2 - 100, 0))
    centered(d, 380, 230, line, font(FB, 96), fill=INK, shadow=0)
    centered(d, 380, 360, accent, font(FBD, 60), fill=accent_col, shadow=0)
    diecut(sub, pad=56, border=30).resize((1024, 754), Image.LANCZOS).save(
        os.path.join(ROOT, "stickers", name)); print("stickers/" + name)

# =================================================================== VIDEOS (MP4)
def mp4(name, w, h, tagline, seconds=5, fps=30):
    frames = seconds * fps
    # static foreground (logo region redrawn per frame for the heartbeat)
    hearts = [(np.random.rand(), np.random.rand(), 0.018 + 0.03 * np.random.rand(),
               0.4 + 0.6 * np.random.rand(), np.random.rand()) for _ in range(18)]
    S = min(w, h)
    path = os.path.join(ROOT, "videos", name)
    wr = imageio.get_writer(path, fps=fps, codec="libx264", macro_block_size=None,
                            ffmpeg_params=["-crf", "20", "-preset", "veryfast", "-pix_fmt", "yuv420p"])
    for fi in range(frames):
        t = fi / frames
        # drifting aurora
        blobs = []
        for i, (cx, cy, r, col, st) in enumerate(AURORA):
            dx = 0.04 * math.sin(2 * math.pi * (t + i * 0.13))
            dy = 0.04 * math.cos(2 * math.pi * (t + i * 0.21))
            blobs.append((cx + dx, cy + dy, r, col, st))
        arr = grain(mesh(w, h, (104, 84, 206), blobs), 5.0)
        img = to_pil(arr)
        # floating hearts rising + fading
        for (hx, hy0, sp, sc, ph0) in hearts:
            prog = (t / 1.0 + ph0) % 1.0
            yy = (1.05 - prog) * h
            size = int(S * (0.03 + 0.05 * sc))
            a = int(180 * math.sin(math.pi * prog))
            mh = mini_heart(size, WHITE, max(0, a))
            img.alpha_composite(mh, (int(hx * w), int(yy)))
        d = ImageDraw.Draw(img)
        # heartbeat logo (double-thump)
        beat = (math.sin(2 * math.pi * t * 2) * 0.5 + math.sin(2 * math.pi * t * 2 + 1.1) * 0.3)
        pulse = 1.0 + 0.06 * max(0, beat)
        intro = min(1.0, t / 0.18)
        paste_logo(img, w // 2, int(h * 0.34), int(S * 0.34 * (0.6 + 0.4 * intro)))
        d = ImageDraw.Draw(img)
        if t > 0.22:
            wordmark(d, w // 2, int(h * 0.60), int(S * 0.17))
        if t > 0.40:
            centered(d, w // 2, int(h * 0.76), tagline, font(FBD, int(S * 0.058)), img=img, glowcol=(60, 30, 90))
        if t > 0.60:
            pill(img, w // 2, int(h * 0.86), "Download now", font(FB, int(S * 0.05)))
        wr.append_data(np.array(img.convert("RGB")))
    wr.close(); print("videos/" + name)

# =================================================================== RUN
if __name__ == "__main__":
    # Stickers (die-cut, 1024)
    sticker_badge()
    sticker_text("sticker_match_1024.png", "It's a match!", "swipe right →", HOT)
    sticker_text("sticker_vibe_1024.png", "Vibe check", "✦ certified ✦", MINT)
    sticker_text("sticker_genz_1024.png", "GenZ", "Match. Chat. Vibe.", (150, 140, 255))

    # Banners (high-res)
    hero("instagram_post_2048.png", 2048, 2048, "Match. Chat. Vibe.")
    hero("instagram_story_1440x2560.png", 1440, 2560, "Find your people, in your city.", vertical=True)
    hero("whatsapp_status_1440x2560.png", 1440, 2560, "Come find me on GenZ 👀", vertical=True)
    mockup_banner("instagram_post_mockup_2048.png", 2048, 2048, "Your match is one swipe away.", "CA (3).jpg")
    mockup_banner("facebook_1920x1008.png", 1920, 1008, "Real connections.\nZero cringe.", "CA (5).jpg",
                  landscape=True)
    mockup_banner("x_1920x1080.png", 1920, 1080, "Match.\nChat.\nVibe.", "CA (1).jpg", landscape=True)
    hero("linkedin_1920x1004.png", 1920, 1004, "The dating & friends app built for Gen Z.")

    # Instagram carousel (3 slides)
    carousel(0, 3, 2048, 2048, "Meeting people\nshould feel easy.", None)
    carousel(1, 3, 2048, 2048, "", ["● Match by city & vibe", "● Real-time chat", "● Verified, 18+ only"],
             photo="CA (6).jpg")
    carousel(2, 3, 2048, 2048, "Your circle is\nwaiting.", ["Download GenZ — it's free."])

    # Videos (real MP4, HD)
    mp4("promo_square_1080.mp4", 1080, 1080, "Match. Chat. Vibe.")
    mp4("promo_story_1080x1920.mp4", 1080, 1920, "Find your circle.")
    print("DONE")
