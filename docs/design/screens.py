"""Draws every Adzan TV screen as a 1920x1080 SVG in docs/design/screens/.
Run: python3 docs/design/screens.py   (then import the SVGs into FigCraft, see screens.figcraft)"""
import math, os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "screens")
SANS, SERIF, ARABIC = "Roboto", "Source Serif 4", "Amiri"

PALETTES = {  # ground, surface, line, accent, text, dim, ornament  (ui/Theme.kt)
    "Kiswah": ("#0B0A08", "#17140F", "#B8964F", "#D9B76A", "#D8CDB4", "#8C8370", "#B8964F"),
    "Hijau Nabawi": ("#071A15", "#0E2A22", "#B8964F", "#D9B76A", "#DCD3BC", "#8FA89C", "#B8964F"),
    "Teal Iznik": ("#061A1C", "#0D2A2D", "#B8964F", "#4FB3A9", "#DAD6C8", "#86A3A0", "#4FB3A9"),
    "Lapis Malam": ("#060E1F", "#0D1C38", "#B8964F", "#D9B76A", "#D8CFBA", "#8593AD", "#B8964F"),
}
TIMES = [("Subuh", "04:03"), ("Terbit", "05:20"), ("Dzuhur", "11:38"), ("Ashar", "15:00"), ("Maghrib", "17:50"), ("Isya", "19:03")]
NEXT = "Ashar"


class P:
    def __init__(self, name):
        self.name = name
        self.ground, self.surface, self.line, self.gold, self.ivory, self.mist, self.ornament = PALETTES[name]


def T(x, y, s, size, fill, anchor="start", weight=400, family=SANS, ls=0, op=1):
    a = f' text-anchor="{anchor}"' if anchor != "start" else ""
    l = f' letter-spacing="{ls * size:.1f}"' if ls else ""
    o = f' fill-opacity="{op}"' if op != 1 else ""
    return f'<text x="{x}" y="{y}" font-family="{family}" font-size="{size}" font-weight="{weight}" fill="{fill}"{a}{l}{o}>{s}</text>'


def R(x, y, w, h, fill="none", r=0, stroke=None, sw=0, op=1):
    s = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
    o = f' fill-opacity="{op}"' if op != 1 else ""
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}"{s}{o}/>'


def mihrab(p, x, y, w, h, stroke=3, line=None, floor=900.0):
    """ui/Mihrab.kt on its 600 x floor grid."""
    sx, sy = w / 600, h / floor

    def arch(i, top):
        pts = [(i, floor), (i, 330 + i / 4.5), (i, 170 + i * .8), (140 + i * .6, 70 + i), (300, top),
               (460 - i * .6, 70 + i), (600 - i, 170 + i * .8), (600 - i, 330 + i / 4.5), (600 - i, floor)]
        q = [f"{x + a * sx:.1f},{y + b * sy:.1f}" for a, b in pts]
        return f"M{q[0]} L{q[1]} C{q[2]} {q[3]} {q[4]} C{q[5]} {q[6]} {q[7]} L{q[8]}"

    line = line or p.line
    return (f'<path d="{arch(0, 0)} Z" fill="{p.surface}"/>'
            f'<path d="{arch(0, 0)}" fill="none" stroke="{line}" stroke-width="{stroke}"/>'
            f'<path d="{arch(18, 22)}" fill="none" stroke="{p.line}" stroke-opacity="0.55" stroke-width="1.5"/>')


def pattern(p, w=1920, h=1080, period=220.0):
    """ui/IslamicPattern.kt: khatam strapwork as two 2px lines at 9%."""
    a = period / (1 + math.sqrt(2))
    r = a / (2 * math.sin(math.pi / 8))
    th = math.radians(67.5)
    d = []

    def poly(cx, cy, n, rad, rot):
        return [(cx + rad * math.cos(rot + i * 2 * math.pi / n), cy + rad * math.sin(rot + i * 2 * math.pi / n)) for i in range(n)]

    def hankin(v):
        n = len(v)
        mids = [((v[i][0] + v[(i + 1) % n][0]) / 2, (v[i][1] + v[(i + 1) % n][1]) / 2) for i in range(n)]
        cx, cy = sum(q[0] for q in v) / n, sum(q[1] for q in v) / n
        for i in range(n):
            vx, vy = v[(i + 1) % n]
            m = mids[i]
            half = math.hypot(vx - m[0], vy - m[1])
            gamma = math.pi - th - (math.pi / 2 - math.pi / n)
            if gamma <= 0:
                continue
            reach = half * math.sin(th) / math.sin(gamma)
            ln = math.hypot(cx - vx, cy - vy)
            px, py = vx + (cx - vx) / ln * reach, vy + (cy - vy) / ln * reach
            nx = mids[(i + 1) % n]
            d.append(f"M{m[0]:.1f},{m[1]:.1f}L{px:.1f},{py:.1f}L{nx[0]:.1f},{nx[1]:.1f}")

    x = -period
    while x <= w + period:
        y = -period
        while y <= h + period:
            hankin(poly(x, y, 8, r, math.pi / 8))
            hankin(poly(x + period / 2, y + period / 2, 4, a / math.sqrt(2), 0))
            y += period
        x += period
    path = "".join(d)
    return (f'<path d="{path}" fill="none" stroke="{p.ornament}" stroke-opacity="0.09" stroke-width="8" stroke-linejoin="round"/>'
            f'<path d="{path}" fill="none" stroke="{p.ground}" stroke-width="4" stroke-linejoin="round"/>')


def svg(p, body):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" viewBox="0 0 1920 1080">'
            f'<rect width="1920" height="1080" fill="{p.ground}"/>{body}</svg>')


ICONS = {  # 28x28, stroke only
    "pin": '<path d="M14 3a8 8 0 0 0-8 8c0 6 8 14 8 14s8-8 8-14a8 8 0 0 0-8-8z"/><circle cx="14" cy="11" r="3"/>',
    "target": '<circle cx="14" cy="14" r="8"/><path d="M14 2v6M14 20v6M2 14h6M20 14h6"/>',
    "bell": '<path d="M5 21h18l-2.5-3.5v-6.5a6.5 6.5 0 0 0-13 0v6.5z"/><path d="M11.5 24.5a2.5 2.5 0 0 0 5 0"/>',
    "play": '<path d="M9 5l14 9-14 9z"/>',
    "moon": '<path d="M23 17.5A10.5 10.5 0 1 1 10.5 5a8.5 8.5 0 0 0 12.5 12.5z"/>',
    "palette": '<circle cx="14" cy="14" r="11"/><path d="M14 3v22"/><path d="M14 8h6M14 14h10M14 20h6"/>',
    "search": '<circle cx="12" cy="12" r="8"/><path d="M18 18l7 7"/>',
    "check": '<path d="M5 14.5l6 6 12-13"/>',
    "alarm": '<circle cx="14" cy="15" r="9"/><path d="M14 10v5l3.5 2M4 7l3.5-3.5M24 7l-3.5-3.5"/>',
}


def icon(name, x, y, color, s=1.0, sw=2.2):
    return (f'<g transform="translate({x} {y}) scale({s})" fill="none" stroke="{color}" stroke-width="{sw}" '
            f'stroke-linecap="round" stroke-linejoin="round">{ICONS[name]}</g>')


def chevron(x, y, color):
    return f'<path d="M{x},{y} l12,12 l-12,12" fill="none" stroke="{color}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>'


def focus_box(p, x, y, w, h, r=16):
    """Gold fill with an ivory ring 6 outside (the app's existing focus treatment)."""
    return R(x - 8, y - 8, w + 16, h + 16, r=r + 8, stroke=p.ivory, sw=4) + R(x, y, w, h, p.gold, r)


def hints(p, x, y, items):
    """Remote hints as key caps: (cap, label)."""
    out = []
    for cap, label in items:
        w = 44 if cap in ("ud", "back") else 22 + 15 * len(cap)
        out.append(R(x, y - 26, w, 36, r=8, stroke=p.mist, sw=2))
        if cap == "ud":
            out.append(f'<path d="M{x + 10},{y - 4} l6,-9 l6,9 z M{x + 24},{y - 12} l6,9 l6,-9 z" fill="{p.mist}"/>')
        elif cap == "back":
            out.append(f'<path d="M{x + 30},{y - 2} h-14 M{x + 22},{y - 9} l-7,7 l7,7 M{x + 30},{y - 2} v-12" fill="none" stroke="{p.mist}" '
                       f'stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" transform="translate(0 -5)"/>')
        else:
            out.append(T(x + w / 2, y, cap, 20, p.mist, "middle", 500))
        out.append(T(x + w + 12, y, label, 22, p.mist))
        x += w + 12 + 12.5 * len(label) + 40
    return "".join(out)


# ---------------------------------------------------------------- 1. Setelan (redesign)

def switch(p, x, y, on, focused):
    fg = p.ground if focused else (p.gold if on else p.mist)
    word = T(x - 16, y + 27, "Aktif" if on else "Mati", 26, p.ground if focused else (p.gold if on else p.mist), "end", 500)
    if on:
        return word + R(x, y, 64, 36, fg, 18) + f'<circle cx="{x + 46}" cy="{y + 18}" r="13" fill="{p.gold if focused else p.ground}"/>'
    return word + R(x + 1, y + 1, 62, 34, r=17, stroke=fg, sw=2) + f'<circle cx="{x + 18}" cy="{y + 18}" r="11" fill="{fg}"/>'


def setting_row(p, y, ic, label, hint, value=None, chev=False, sw=None, focused=False):
    x, w, h = 656, 1168, 88
    fg, sub, val = (p.ground, p.ground, p.ground) if focused else (p.ivory, p.mist, p.gold)
    out = focus_box(p, x, y, w, h) if focused else R(x, y, w, h, p.surface, 16)
    out += icon(ic, x + 32, y + 30, fg if focused else p.gold)
    out += T(x + 88, y + 39, label, 30, fg, weight=500) + T(x + 88, y + 69, hint, 22, sub, op=0.8 if focused else 1)
    right = x + w - 32
    if chev:
        out += chevron(right - 12, y + 32, fg)
        right -= 36
    if value:
        out += T(right, y + 54, value, 26, val, "end", 500)
    if sw is not None:
        out += switch(p, right - 64, y + 26, sw, focused)
    return out


def sidebar_times(p):
    x, y, w, rh = 96, 424, 424, 72
    out = T(x, y - 32, "JADWAL HARI INI", 20, p.mist, ls=0.12) + T(x + w, y - 32, "Kota Bandung", 20, p.mist, "end")
    out += R(x, y, w, rh * 6, p.surface, 18)
    for i, (name, at) in enumerate(TIMES):
        ry = y + i * rh
        nxt = name == NEXT
        past = i < [n for n, _ in TIMES].index(NEXT)
        c = p.gold if nxt else (p.mist if past else p.ivory)
        if nxt:
            out += R(x + 1.5, ry + 1.5, w - 3, rh - 3, r=16, stroke=p.gold, sw=3)
        elif i and TIMES[i - 1][0] != NEXT:
            out += R(x + 24, ry, w - 48, 1, p.ivory, op=0.12)
        out += T(x + 24, ry + 46, name, 26, c, weight=600 if nxt else 400)
        if nxt:
            out += T(x + w - 120, ry + 44, "BERIKUT", 18, p.gold, "end", ls=0.12)
        out += T(x + w - 24, ry + 47, at, 30, c, "end", 600 if nxt else 400)
    out += T(x, y + rh * 6 + 40, "Sumber: Kemenag RI", 20, p.mist)
    return out


def settings():
    p = P("Kiswah")
    b = mihrab(p, 96, 56, 40, 60, 2) + T(156, 106, "Setelan", 52, p.ivory, weight=700, family=SERIF)
    # the clock and both dates, so the TV's time can be checked against the schedule below
    b += T(92, 250, "14:21", 100, p.ivory, family=SERIF)
    b += T(96, 296, "4 Jumadilakhir 1448 H", 26, p.ivory) + T(96, 330, "Sabtu, 14 November 2026", 22, p.mist)
    b += sidebar_times(p)
    b += R(592, 56, 1, 968, p.ivory, op=0.12)

    def section(y, title):
        return T(656, y, title, 20, p.mist, ls=0.12)

    b += section(100, "LOKASI")
    b += setting_row(p, 112, "pin", "Kota", "Jadwal shalat Kemenag untuk kota ini", "Kota Bandung", chev=True)
    b += setting_row(p, 212, "target", "Deteksi ulang", "Cari kota dari sambungan internet")
    b += section(348, "ADZAN")
    b += setting_row(p, 360, "bell", "Adzan otomatis", "Berbunyi di setiap waktu shalat wajib", sw=True)
    b += setting_row(p, 460, "alarm", "Pengingat", "Muncul di layar sebelum adzan · Semua shalat", "5 menit sebelum", chev=True, focused=True)
    b += setting_row(p, 560, "play", "Tes adzan", "Berbunyi 5 detik setelah ditekan")
    b += section(696, "TAMPILAN")
    b += setting_row(p, 708, "moon", "Screensaver Adzan TV", "Tampil saat TV diam", sw=False)
    b += setting_row(p, 808, "palette", "Tema warna", "Empat tema gelap untuk ruangan redup", "Kiswah", chev=True)
    # one status line, in a fixed place, instead of text appended under the rows
    b += R(656, 918, 470, 52, p.surface, 26) + icon("check", 676, 930, p.gold) + T(716, 953, "Pengingat 5 menit sebelum adzan", 22, p.ivory)
    b += hints(p, 656, 1010, [("ud", "Pindah"), ("OK", "Pilih"), ("back", "Keluar")])
    return svg(p, b)


# ---------------------------------------------------------------- 2. Pilih kota

def city_chooser():
    p = P("Kiswah")
    x, w = 460, 1000
    b = T(x, 124, "Pilih kota", 52, p.ivory, weight=700, family=SERIF) + T(x + w, 120, "517 kabupaten/kota", 22, p.mist, "end")
    b += R(x, 160, w, 76, p.surface, 16, p.ivory, 2, 1) .replace('stroke="%s"' % p.ivory, 'stroke="%s" stroke-opacity="0.25"' % p.ivory)
    b += icon("search", x + 28, 184, p.mist) + T(x + 76, 208, "Cari kabupaten/kota", 28, p.mist)
    rows = ["Kab. Bandung", "Kab. Bandung Barat", "Kota Bandung", "Kab. Banggai", "Kota Banjar", "Kab. Banjarnegara", "Kab. Bantul", "Kab. Banyumas"]
    y = 268
    for name in rows:
        sel = name == "Kota Bandung"
        b += focus_box(p, x, y, w, 76) if sel else R(x, y, w, 76, p.surface, 16)
        fg = p.ground if sel else p.ivory
        b += T(x + 32, y + 48, name, 28, fg, weight=500 if sel else 400)
        if sel:
            b += icon("check", x + w - 168, y + 24, fg) + T(x + w - 32, y + 46, "DIPILIH", 20, fg, "end", 500, ls=0.12)
        y += 88
    b += hints(p, x, 1010, [("ud", "Pindah"), ("OK", "Pilih"), ("back", "Batal")])
    return svg(p, b)


# ---------------------------------------------------------------- 4. Screensaver, 5. Adzan

def screensaver_body(p, with_pattern=True):
    b = pattern(p) if with_pattern else ""
    b += mihrab(p, 96, 90, 680, 870, 3, floor=870 * 600 / 680)
    b += R(56, 958, 760, 4, p.line) + R(80, 974, 712, 2, p.line, op=0.55)
    cx = 436
    b += T(cx, 372, "إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ", 38, p.gold, "middle", family=ARABIC)
    b += T(cx, 437, "كِتَابًا مَّوْقُوتًا", 38, p.gold, "middle", family=ARABIC)
    b += T(cx, 482, "QS An-Nisa: 103", 20, p.mist, "middle")
    b += R(cx - 60, 520, 120, 2, p.line)
    b += T(cx, 584, "SHALAT BERIKUTNYA", 22, p.mist, "middle", ls=0.12)
    b += T(cx, 678, NEXT, 88, p.gold, "middle", 700, SERIF)
    b += T(cx, 790, "0:38:12", 110, p.ivory, "middle", family=SERIF)
    b += T(cx, 842, "Adzan pukul 15:00", 26, p.mist, "middle")
    x = 840
    b += T(x, 452, "14:21", 168, p.ivory, family=SERIF)
    b += T(x, 520, "4 Jumadilakhir 1448 H  ·  Kota Bandung", 30, p.ivory)
    b += T(x, 562, "Sabtu, 14 November 2026", 26, p.mist)
    cw = (984 - 5 * 12) / 6
    for i, (name, at) in enumerate(TIMES):
        cx0, cy0 = x + i * (cw + 12), 660
        nxt, past = name == NEXT, i < 3
        c = p.gold if nxt else (p.mist if past else p.ivory)
        if nxt:
            b += R(cx0 + 1.5, cy0 + 1.5, cw - 3, 153, r=14, stroke=p.gold, sw=3) + T(cx0 + 20, cy0 + 36, "BERIKUT", 18, p.gold, ls=0.12)
        else:
            b += R(cx0, cy0, cw, 156, p.surface, 14, op=0.6 if past else 1)
        b += T(cx0 + 20, cy0 + 92, name, 26, c, weight=600 if nxt else 400) + T(cx0 + 20, cy0 + 134, at, 38, c, weight=600 if nxt else 400)
    return b


def screensaver():
    p = P("Kiswah")
    return svg(p, screensaver_body(p))


def adhan():
    p = P("Kiswah")
    edge = "#E6D09E"  # GoldLight
    b = pattern(p) + mihrab(p, 540, 60, 840, 920, 6, edge, floor=920 * 600 / 840)
    b += R(480, 978, 960, 4, edge) + R(510, 994, 900, 2, p.line, op=0.55)
    cx = 960
    b += T(cx, 300, "حَيَّ عَلَى الصَّلَاةِ", 60, p.gold, "middle", family=ARABIC)
    b += T(cx, 372, "WAKTUNYA SHALAT", 22, p.mist, "middle", ls=0.12)
    b += T(cx, 500, NEXT, 128, p.ivory, "middle", 700, SERIF)
    b += T(cx, 592, "صَلَاةُ العَصْر", 44, p.gold, "middle", family=ARABIC)
    b += T(cx, 648, "15:00  ·  Kota Bandung", 28, p.mist, "middle")
    b += R(cx - 220, 690, 440, 4, p.ivory, 2, op=0.25) + R(cx - 220, 690, 150, 4, p.gold, 2)
    b += T(cx, 732, "1:12 / 3:34", 22, p.mist, "middle")
    b += focus_box(p, cx - 210, 790, 420, 70, 35) + T(cx, 835, "Hentikan adzan", 28, p.ground, "middle", 500)
    b += T(cx, 906, "OK atau Kembali", 22, p.mist, "middle")
    return svg(p, b)


# ---------------------------------------------------------------- 3. Tema warna

def theme_chooser():
    p = P("Kiswah")
    b = T(340, 108, "Tema warna", 52, p.ivory, weight=700, family=SERIF)
    b += T(340, 150, "Semua tema gelap dan redup untuk ruangan tanpa lampu", 24, p.mist)
    s = 588 / 1920
    for i, name in enumerate(PALETTES):
        q = P(name)
        x, y = 340 + (i % 2) * 640, 190 + (i // 2) * 416
        cur = name == "Kiswah"
        b += focus_box(p, x, y, 600, 388) if cur else R(x, y, 600, 388, p.surface, 16)
        b += (f'<clipPath id="c{i}"><rect x="{x + 6}" y="{y + 6}" width="588" height="330.75" rx="12"/></clipPath>'
              f'<g clip-path="url(#c{i})"><g transform="translate({x + 6} {y + 6}) scale({s:.5f})">'
              f'<rect width="1920" height="1080" fill="{q.ground}"/>{screensaver_body(q, False)}</g></g>')
        fg = p.ground if cur else p.ivory
        b += T(x + 22, y + 371, name, 26, fg, weight=500)
        if cur:
            b += icon("check", x + 600 - 160, y + 348, fg) + T(x + 578, y + 369, "DIPAKAI", 20, fg, "end", 500, ls=0.12)
    b += hints(p, 340, 1040, [("ud", "Pindah"), ("OK", "Pakai"), ("back", "Batal")])
    return svg(p, b)


# ---------------------------------------------------------------- 6. Pengingat, 7. Pengingat tampil

def reminder_card(p, x, y):
    """What appears on screen before the adhan: 620 x 148."""
    b = R(x, y, 620, 148, p.surface, 20, p.gold, 2) + mihrab(p, x + 32, y + 38, 48, 72, 2)
    b += T(x + 108, y + 44, "PENGINGAT SHALAT", 18, p.mist, ls=0.12)
    b += T(x + 108, y + 92, "Ashar 5 menit lagi", 40, p.ivory, weight=700, family=SERIF)
    b += T(x + 108, y + 126, "Adzan pukul 15:00  ·  Kota Bandung", 22, p.mist)
    return b


def chip(p, x, y, w, label, state):
    """state: focus (gold fill, ring), on (gold outline, checked), off (plain)."""
    h = 84
    if state == "focus":
        return focus_box(p, x, y, w, h) + T(x + w / 2, y + 53, label, 28, p.ground, "middle", 600)
    if state == "off":
        return R(x, y, w, h, p.surface, 16) + T(x + w / 2, y + 53, label, 28, p.mist, "middle")
    return R(x + 1.5, y + 1.5, w - 3, h - 3, p.surface, 16, p.gold, 3) + T(x + w / 2 + 14, y + 53, label, 28, p.gold, "middle", 600)


def reminder(all_prayers=True):
    p = P("Kiswah")
    x, w = 340, 1240
    b = T(x, 108, "Pengingat", 52, p.ivory, weight=700, family=SERIF)
    b += T(x, 150, "Muncul di layar beberapa menit sebelum adzan, tanpa suara", 24, p.mist)
    b += R(x, 190, w, 88, p.surface, 16) + icon("alarm", x + 32, 220, p.gold)
    b += T(x + 88, 229, "Pengingat", 30, p.ivory, weight=500) + T(x + 88, 259, "Mati: hanya adzan yang berbunyi", 22, p.mist)
    b += switch(p, x + w - 96, 216, True, False)

    b += T(x, 334, "WAKTU", 20, p.mist, ls=0.12) + T(x + w, 334, "menit sebelum adzan", 22, p.mist, "end")
    cw = (w - 9 * 12) / 10
    for i in range(10):
        b += chip(p, x + i * (cw + 12), 352, cw, str(i + 1), "focus" if i == 4 else "off")

    b += T(x, 496, "SHALAT", 20, p.mist, ls=0.12)
    b += T(x + w, 496, "Semua shalat" if all_prayers else "3 dari 5 shalat", 22, p.mist, "end")
    cw = (w - 5 * 12) / 6
    on = {"Semua"} if all_prayers else {"Subuh", "Maghrib", "Isya"}
    for i, name in enumerate(["Semua", "Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya"]):
        cx = x + i * (cw + 12)
        checked = name in on or (all_prayers and name != "Semua")
        b += chip(p, cx, 514, cw, name, "on" if checked else "off")
        if checked:  # a mark, not only gold
            b += icon("check", cx + 18, 542, p.gold, 0.8)
    b += T(x, 632, "Semua adalah bawaan. Pilih shalat tertentu untuk mengingatkan hanya di waktu itu.", 22, p.mist)

    b += T(x, 716, "PRATINJAU", 20, p.mist, ls=0.12) + reminder_card(p, x, 736)
    b += T(x + 660, 800, "Tampil di pojok kanan atas selama 10 detik,", 22, p.mist)
    b += T(x + 660, 832, "lalu hilang sendiri. Tayangan tidak dihentikan.", 22, p.mist)
    b += hints(p, x, 1010, [("ud", "Pindah"), ("OK", "Pilih"), ("back", "Kembali")])
    return svg(p, b)


def reminder_shown():
    p = P("Kiswah")
    # a stand-in for whatever the TV is showing
    b = ('<defs><linearGradient id="tv" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#2B3440"/>'
         '<stop offset="1" stop-color="#11151B"/></linearGradient></defs><rect width="1920" height="1080" fill="url(#tv)"/>')
    b += T(960, 560, "Tayangan TV", 44, "#5B6673", "middle")
    b += reminder_card(p, 1920 - 96 - 620, 72)
    return svg(p, b)


# ---------------------------------------------------------------- 8-10. Large type, for elderly viewers
# Rules: nothing under 26, hint text at 7:1, every row outlined, accent only for state (on, chosen, next).

def _mix(a, b, t):
    ca, cb = [int(a[i:i + 2], 16) for i in (1, 3, 5)], [int(b[i:i + 2], 16) for i in (1, 3, 5)]
    return "#" + "".join(f"{round(x + (y - x) * t):02X}" for x, y in zip(ca, cb))


def big_palette(name="Kiswah"):
    p = P(name)
    p.dim2 = _mix(p.mist, p.ivory, 0.45)  # 7.5:1 on the row fill
    p.edge = _mix(p.ground, p.ivory, 0.40)  # row outline, about 3:1 on the ground
    p.focus = p.on = p.gold
    p.static, p.ring = False, True
    return p


def static_palette():
    """Settings keep one fixed palette whatever theme is chosen, so they always read the same:
    amber = where you are, green = on / chosen / next. Themes colour only the screensaver, adzan and reminder."""
    p = P("Kiswah")
    p.ground, p.surface, p.edge = "#0F1114", "#1C2026", "#6E7682"
    p.ivory, p.dim2, p.mist = "#F3EFE7", "#C0C5CC", "#C0C5CC"  # 14.3 and 9.4 on the row fill
    # focus is a plain light row with dark text (14.6): unmistakable, yet no colour of its own
    p.focus, p.on, p.ring = "#ECE9E2", "#7BDCA2", False  # green on the row fill 9.8
    p.line = p.gold = p.dim2
    p.static = True
    return p


def big_box(p, x, y, w, h, focused=False, r=18):
    if focused:  # ring 6 thick, 6 clear of the fill, so it reads against the ground and not against gold
        return (R(x - 9, y - 9, w + 18, h + 18, r=r + 9, stroke=p.ivory, sw=6) if p.ring else "") + R(x, y, w, h, p.focus, r)
    return R(x + 1, y + 1, w - 2, h - 2, p.surface, r, p.edge, 2)


def big_switch(p, x, y, on, focused):
    c = p.ground if focused else (p.on if on else p.dim2)
    b = T(x - 20, y + 33, "Nyala" if on else "Mati", 30, c, "end", 600)
    if on:
        return b + R(x, y, 80, 44, c, 22) + f'<circle cx="{x + 58}" cy="{y + 22}" r="16" fill="{p.focus if focused else p.ground}"/>'
    return b + R(x + 1.5, y + 1.5, 77, 41, r=20.5, stroke=c, sw=3) + f'<circle cx="{x + 22}" cy="{y + 22}" r="13" fill="{c}"/>'


def big_row(p, x, y, w, ic, label, hint=None, value=None, chev=False, sw=None, focused=False, h=116):
    fg = p.ground if focused else p.ivory
    b = big_box(p, x, y, w, h, focused) + icon(ic, x + 32, y + h / 2 - 18, fg, 1.3, 2)
    if hint:
        b += T(x + 100, y + h / 2 - 4, label, 34, fg, weight=500) + T(x + 100, y + h / 2 + 32, hint, 26, p.ground if focused else p.dim2)
    else:
        b += T(x + 100, y + h / 2 + 12, label, 34, fg, weight=500)
    right = x + w - 36
    if chev:
        b += f'<path d="M{right - 14},{y + h / 2 - 15} l15,15 l-15,15" fill="none" stroke="{fg}" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>'
        right -= 44
    if value:
        b += T(right, y + h / 2 + 11, value, 30, fg, "end", 500)
    if sw is not None:
        b += big_switch(p, right - 80, y + h / 2 - 22, sw, focused)
    return b


def big_hints(p, x, y, items):
    """Remote hints in words, 26."""
    out = ""
    for cap, label in items:
        w = 30 + 14.5 * len(cap)
        out += R(x, y - 32, w, 46, r=10, stroke=p.dim2, sw=2) + T(x + w / 2, y, cap, 24, p.dim2, "middle", 600)
        out += T(x + w + 14, y, label, 26, p.dim2)
        x += w + 14 + 14.5 * len(label) + 48
    return out


def big_settings(p=None):
    p = p or big_palette()
    b = mihrab(p, 96, 56, 40, 60, 2) + T(156, 106, "Setelan", 52, p.ivory, weight=700, family=SERIF)
    b += T(92, 250, "14:21", 100, p.ivory, family=SERIF)
    b += T(96, 300, "4 Jumadilakhir 1448 H", 30, p.ivory) + T(96, 338, "Sabtu, 14 November 2026", 26, p.dim2)
    x, y, w, rh = 96, 424, 424, 76
    b += T(x, y - 24, "Jadwal hari ini", 26, p.dim2)
    b += R(x + 1, y + 1, w - 2, rh * 6 - 2, p.surface, 18, p.edge, 2)
    for i, (name, at) in enumerate(TIMES):
        ry, nxt, past = y + i * rh, name == NEXT, i < 3
        c = p.on if nxt else (p.dim2 if past else p.ivory)
        if nxt:
            b += R(x + 2, ry + 2, w - 4, rh - 4, r=16, stroke=p.on, sw=4)
        elif i and TIMES[i - 1][0] != NEXT:
            b += R(x + 24, ry, w - 48, 2, p.edge)
        b += T(x + 24, ry + 50, name, 32, c, weight=600 if nxt else 400)
        if nxt:
            b += T(x + w - 134, ry + 48, "berikutnya", 24, p.on, "end")
        b += T(x + w - 24, ry + 51, at, 36, c, "end", 600 if nxt else 400)
    b += T(x, y + rh * 6 + 44, "Sumber: Kemenag RI", 26, p.dim2)
    b += R(592, 56, 2, 968, p.edge)

    rx, rw = 656, 1168
    rows = [("pin", "Kota", None, "Kota Bandung", True, None),
            ("bell", "Adzan otomatis", "Berbunyi di setiap waktu shalat wajib", None, False, True),
            ("alarm", "Pengingat", "Semua shalat", "5 menit sebelum", True, None),
            ("play", "Tes adzan", "Berbunyi 5 detik setelah ditekan", None, False, None),
            ("moon", "Screensaver Adzan TV", "Tampil saat TV diam", None, False, False),
            ("palette", "Tema warna", "Untuk screensaver dan pengingat" if p.static else None, "Kiswah", True, None)]
    for i, (ic, label, hint, value, chev, sw) in enumerate(rows):
        b += big_row(p, rx, 72 + i * 128, rw, ic, label, hint, value, chev, sw, focused=i == 2)
    b += R(rx, 852, 640, 64, p.surface, 32, p.edge, 2) + icon("check", rx + 26, 866, p.on, 1.2, 2.4)
    b += T(rx + 76, 895, "Pengingat 5 menit sebelum adzan", 30, p.ivory)
    b += big_hints(p, rx, 1000, [("Panah", "pindah"), ("OK", "pilih"), ("Kembali", "keluar")])
    return svg(p, b)


def big_city(p=None):
    p = p or big_palette()
    x, w = 420, 1080
    b = T(x, 124, "Pilih kota", 56, p.ivory, weight=700, family=SERIF) + T(x + w, 120, "517 kota", 26, p.dim2, "end")
    b += R(x + 1, 161, w - 2, 94, p.surface, 18, p.edge, 2) + icon("search", x + 30, 190, p.dim2, 1.3, 2) + T(x + 96, 220, "Cari nama kota", 34, p.dim2)
    b += big_row(p, x, 279, w, "target", "Deteksi otomatis", "Cari kota dari sambungan internet", h=104)
    y = 407
    for name in ["Kab. Bandung", "Kab. Bandung Barat", "Kota Bandung", "Kab. Banggai", "Kota Banjar"]:
        sel = name == "Kota Bandung"
        fg = p.ground if sel else p.ivory
        b += big_box(p, x, y, w, 96, sel) + T(x + 36, y + 60, name, 34, fg, weight=600 if sel else 400)
        if sel:
            b += icon("check", x + w - 190, y + 30, fg, 1.3, 2.4) + T(x + w - 36, y + 58, "dipilih", 28, fg, "end", 600)
        y += 108
    b += big_hints(p, x, 1000, [("Panah", "pindah"), ("OK", "pilih"), ("Kembali", "batal")])
    return svg(p, b)


def big_reminder(p=None):
    p = p or big_palette()
    x, w = 96, 820
    b = T(x, 112, "Pengingat", 56, p.ivory, weight=700, family=SERIF)
    b += T(x, 156, "Muncul di layar sebelum adzan, tanpa suara", 28, p.dim2)
    b += big_row(p, x, 196, w, "alarm", "Pengingat", sw=True)
    # one stepper instead of ten small chips, the size of any other row: Left and Right change the number
    sy, sh = 336, 140
    b += big_box(p, x, sy, w, sh, True)
    b += T(x + 36, sy + 64, "Waktu pengingat", 34, p.ground, weight=500) + T(x + 36, sy + 102, "sebelum adzan", 26, p.ground)
    b += T(x + w - 250, sy + 88, "5 menit", 52, p.ground, "middle", 700)
    for cx, d in ((x + w - 434, "l-13,13 l13,13"), (x + w - 66, "l13,13 l-13,13")):
        b += f'<circle cx="{cx}" cy="{sy + 70}" r="30" fill="{p.ground}"/>'
        b += (f'<path d="M{cx + (6 if d[1] == "-" else -6)},{sy + 57} {d}" fill="none" stroke="{p.focus}" stroke-width="5" '
              f'stroke-linecap="round" stroke-linejoin="round"/>')
    b += T(x, 548, "Contoh", 26, p.dim2)
    cy = 568
    q = big_palette()  # the reminder itself wears the chosen theme
    b += R(x + 1, cy + 1, w - 2, 178, q.surface, 22, q.gold, 3) + mihrab(q, x + 36, cy + 44, 60, 90, 3)
    b += T(x + 128, cy + 52, "Pengingat shalat", 26, q.dim2)
    b += T(x + 128, cy + 108, "Ashar 5 menit lagi", 48, q.ivory, weight=700, family=SERIF)
    b += T(x + 128, cy + 150, "Adzan pukul 15:00  ·  Kota Bandung", 26, q.dim2)

    lx, lw = 1004, 820
    b += T(lx, 156, "Ingatkan untuk shalat", 28, p.dim2)
    for i, name in enumerate(["Semua shalat", "Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya"]):
        y = 196 + i * 112
        b += big_box(p, lx, y, lw, 100) + R(lx + 32, y + 28, 44, 44, p.on, 10)
        b += f'<path d="M{lx + 42},{y + 51} l9,9 l16,-18" fill="none" stroke="{p.ground}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>'
        b += T(lx + 104, y + 62, name, 34, p.ivory, weight=600 if i == 0 else 400)
        if i == 0:
            b += T(lx + lw - 36, y + 60, "bawaan", 26, p.dim2, "end")
    b += big_hints(p, x, 1000, [("Panah", "pindah"), ("Kiri Kanan", "ubah menit"), ("OK", "pilih"), ("Kembali", "selesai")])
    return svg(p, b)


def big_theme(p=None):
    p = p or static_palette()
    b = T(340, 104, "Tema warna", 56, p.ivory, weight=700, family=SERIF)
    b += T(340, 148, "Warna untuk screensaver, adzan dan pengingat. Setelan tidak berubah.", 28, p.dim2)
    s = 588 / 1920
    for i, name in enumerate(PALETTES):
        q = P(name)
        x, y = 340 + (i % 2) * 640, 184 + (i // 2) * 400
        cur = name == "Kiswah"
        b += big_box(p, x, y, 600, 380, cur, 16)
        b += (f'<clipPath id="t{i}"><rect x="{x + 6}" y="{y + 6}" width="588" height="318" rx="12"/></clipPath>'
              f'<g clip-path="url(#t{i})"><g transform="translate({x + 6} {y}) scale({s:.5f})">'
              f'<rect width="1920" height="1080" fill="{q.ground}"/>{screensaver_body(q, False)}</g></g>')
        fg = p.ground if cur else p.ivory
        b += T(x + 24, y + 364, name, 32, fg, weight=600 if cur else 400)
        if cur:
            b += icon("check", x + 600 - 176, y + 336, fg, 1.2, 2.4) + T(x + 576, y + 362, "dipakai", 28, fg, "end", 600)
    b += big_hints(p, 340, 1016, [("Panah", "pindah"), ("OK", "pakai"), ("Kembali", "batal")])
    return svg(p, b)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, fn in [("1-setelan", settings), ("2-pilih-kota", city_chooser), ("3-tema-warna", theme_chooser),
                     ("4-screensaver", screensaver), ("5-adzan", adhan), ("6-pengingat", reminder),
                     ("6b-pengingat-shalat-tertentu", lambda: reminder(False)), ("7-pengingat-tampil", reminder_shown),
                     ("8-setelan-besar", big_settings), ("9-pilih-kota-besar", big_city), ("10-pengingat-besar", big_reminder),
                     ("11-setelan-statis", lambda: big_settings(static_palette())), ("12-pilih-kota-statis", lambda: big_city(static_palette())),
                     ("13-pengingat-statis", lambda: big_reminder(static_palette())), ("14-tema-warna-statis", big_theme)]:
        open(os.path.join(OUT, name + ".svg"), "w", encoding="utf-8").write(fn())
        print(name)
