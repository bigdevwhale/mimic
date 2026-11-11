"""The bestiary portraits: drawn by Codex's image generation, pixelated to 48×48, written out as Kotlin.

python tools/critters.py gen [name ...]   asks Codex for each creature (needs the codex CLI), saves to design/critters/
python tools/critters.py pixel            design/critters/*.png -> tools/critters/*.png (48×48, at most 20 colours)
python tools/critters.py                  tools/critters/*.png -> app/src/main/java/app/mimic/ui/art/CritterArt.kt

The 48×48 pictures in tools/critters/ are the source of truth; touch them up by hand and rerun the last step.
"""
import glob
import json
import os
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "design/critters"
PX = ROOT / "tools/critters"
KT = ROOT / "app/src/main/java/app/mimic/ui/art/CritterArt.kt"
REF = ROOT / "docs/screenshots/en/5-bestiary.jpg"
N = 48

# Same order as Critters.Kind.
CREATURES = {
    "dust": "A dust bunny: a round fluffy ball of grey house dust with a soft ragged fuzzy edge of stray dust tufts, two tiny stubby feet, big shy eyes. Warm grey (#B8B2A6) with lighter tufts (#E0DBD0). A couple of dust motes float beside it.",
    "wisp": "A wisp: a small living flame spirit, a teardrop of warm orange-gold fire (#F0A030 outside, #FFD860 and cream white in the core) with a flickering tip, a sweet sleepy face in the bright core, holding a tiny folded paper note (a save note) under it.",
    "meeple": "A meeple: the classic wooden board-game pawn figure (round head, arms out, legs apart) made of red painted wood (#D84040) with visible wood grain, a cute face, wearing a small golden crown.",
    "ghost": "A ghost of a game box: a translucent pale blue (#DDE8FF) floating game box shaped like a little sheet ghost, with a faded label stripe on top, a wavy tattered bottom edge, gentle hollow eyes and a small 'o' mouth, slightly see-through, wistful.",
    "dice": "A living die: a red-pink twenty-sided die (D20, #E84860) with facets shaded, standing on two tiny feet, a cute blushing face on the front facet, slightly embarrassed.",
    "crow": "A courier crow: a chubby dark blue-black crow (#2E2E40) with a small crimson courier cap (#C42C58), a grey beak, yellow feet, and a small cream paper parcel with a string hanging from its beak. Standing in side three-quarter view.",
    "merchant": "A travelling merchant: a short hooded figure in a deep purple cloak (#5A3A6A), face hidden in shadow with two glowing golden eyes, carrying a bulging brown leather sack of goods with a gold coin peeking out.",
    "goblin": "A lost-and-found goblin: a tiny green goblin (#78B848) with long pointy ears and big eyes, wearing a brown burlap tunic, holding a long paper list (a checklist of missing pieces) and a stray meeple-shaped game piece.",
    "elder": "A quest giver: a kind old wizard-like elder in a long cream-white robe, a long white beard, a pointed hat, holding a tall wooden staff, with a small scroll and a glowing yellow exclamation mark floating above his head.",
    "slime": "A golden slime: a shiny wobbly blob of liquid gold (#F0C030 with bright highlights) with sparkles around it, a happy face, a drip on its side, glittering like treasure.",
    "moth": "A night moth: a fluffy plump moth with big soft dusky lavender-blue wings (#6C77B6) with pale moon-like eye spots, feathery antennae, a fuzzy cream collar, drawn to light, faint glow on its face.",
    "double": "A shadow double: a game box creature made of dark shadow, deep indigo-black (#2A2440) with a faint purple rim light, the silhouette of a box with stubby arms and a wavy shadowy bottom, two glowing pale eyes, mysterious but friendly.",
}

PROMPT = """Use your image generation tool to create ONE image. The attached screenshot is from this game (a cosy pixel-art game shelf app); it shows how its creatures look now in the bestiary. Keep the cosy spirit, warm palette and chunky pixel-art feel, but draw the creature described below anew, much more charming, detailed and with a clear, readable silhouette.

A single pixel-art sprite of one creature for a cozy game bestiary, drawn on a 48x48 pixel grid and upscaled with hard nearest-neighbour edges: every art pixel is a crisp square block, no anti-aliasing, no blur, no gradients.
The creature is centred, facing the viewer (three-quarter view at most), fills most of the canvas, the whole body visible, standing on an implied baseline near the bottom.
Crisp 1-pixel thick dark outline, cel shading with 3-4 tones per colour, light from the top left, limited palette (at most 16 colours).
Cute and friendly: big glossy eyes with a white highlight where it has eyes.
Transparent background. No ground shadow, no text, no frame, no grid lines, nothing else in the picture.

{desc}

Just generate the image; do not save or copy it anywhere and do not touch any files. Reply with one word: done."""


def gen(name):
    p = subprocess.run(["codex", "exec", "--json", "--skip-git-repo-check", "-s", "read-only", "-i", str(REF), "-"],
                       input=PROMPT.format(desc=CREATURES[name]), capture_output=True, text=True, encoding="utf8", shell=os.name == "nt")
    tid = next((o.get("thread_id") for o in map(_json, p.stdout.splitlines()) if o.get("type") == "thread.started"), None)
    imgs = sorted(glob.glob(os.path.expanduser(f"~/.codex/generated_images/{tid}/*.png")), key=os.path.getmtime) if tid else []
    if not imgs:
        return f"{name}: failed\n{p.stderr[-400:]}"
    RAW.mkdir(parents=True, exist_ok=True)
    shutil.copy(imgs[-1], RAW / f"{name}.png")
    return f"{name}: ok"


def _json(line):
    try:
        return json.loads(line)
    except ValueError:
        return {}


def pixel(src, dst):
    im = Image.open(src).convert("RGBA")
    a = im.getchannel("A").point(lambda v: 255 if v > 128 else 0)
    im.putalpha(a)
    im = im.crop(a.getbbox())
    w, h = im.size
    s = (N - 2) / max(w, h)
    nw, nh = max(1, round(w * s)), max(1, round(h * s))
    small = im.resize((nw, nh), Image.BOX)
    pal = im.convert("RGB").quantize(colors=20, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    q = small.convert("RGB").quantize(palette=pal, dither=Image.Dither.NONE).convert("RGB")
    q.putalpha(small.getchannel("A").point(lambda v: 255 if v >= 110 else 0))
    out = Image.new("RGBA", (N, N))
    out.paste(q, ((N - nw) // 2, N - 1 - nh), q)  # one row free at the top for the bob
    out.save(dst)


KEYS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"


def kotlin():
    parts = []
    for name in CREATURES:
        im = Image.open(PX / f"{name}.png").convert("RGBA")
        assert im.size == (N, N), name
        pal, rows = [], []
        for y in range(N):
            row = ""
            for x in range(N):
                r, g, b, a = im.getpixel((x, y))
                if a < 128:
                    row += "."
                    continue
                c = (r << 16) | (g << 8) | b
                if c not in pal:
                    pal.append(c)
                row += KEYS[pal.index(c)]
            rows.append(row)
        body = "\n".join(f'            "{r}" +' for r in rows)[:-2]
        cols = ", ".join(f"0x{c:06X}" for c in pal)
        parts.append(f"        // {name}\n        Sprite(\n            intArrayOf({cols}),\n{body},\n        ),")
    KT.write_text(f"""package app.mimic.ui.art

// Generated by tools/critters.py from tools/critters/*.png. Do not edit by hand.

/** The bestiary portraits, {N}×{N}, in [Critters.Kind] order: a palette and one letter per pixel ('.' is empty). */
internal object CritterArt {{
    const val SIZE = {N}
    private const val KEYS = "{KEYS}"

    class Sprite(private val pal: IntArray, private val px: String) {{
        fun paint(x: Buf, dx: Int, dy: Int) {{
            for (i in px.indices) {{
                val ch = px[i]
                if (ch != '.') x.p(i % SIZE + dx, i / SIZE + dy, rgb(pal[KEYS.indexOf(ch)].toLong()))
            }}
        }}
    }}

    val all = listOf(
{chr(10).join(parts)}
    )
}}
""", encoding="utf8")
    print(f"wrote {KT.relative_to(ROOT)}")


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "kotlin"
    if cmd == "gen":
        names = sys.argv[2:] or list(CREATURES)
        with ThreadPoolExecutor(len(names)) as ex:
            for r in ex.map(gen, names):
                print(r, flush=True)
    elif cmd == "pixel":
        PX.mkdir(parents=True, exist_ok=True)
        for name in CREATURES:
            pixel(RAW / f"{name}.png", PX / f"{name}.png")
        print(f"wrote {len(CREATURES)} pictures to {PX.relative_to(ROOT)}")
    else:
        kotlin()
