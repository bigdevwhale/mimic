"""Downloads the demo games' real covers from Wikipedia for the screenshot renders.

python tools/demo_covers.py
./gradlew testDebugUnitTest -Prender --tests '*RenderScreens*'

The pictures go to app/build/demo-covers/<title>.jpg (not committed: they belong to their publishers).
RenderScreens puts them on the demo games when the folder is there.
"""
import json
import re
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app/build/demo-covers"
UA = {"User-Agent": "Mimic/1.0 (https://github.com/bigdevwhale/mimic) demo covers"}

# English demo title -> its English Wikipedia article.
ARTICLES = {
    "Super Mario Bros. 3": "Super Mario Bros. 3",
    "Zelda: A Link to the Past": "The Legend of Zelda: A Link to the Past",
    "Silent Hill 2": "Silent Hill 2",
    "Resident Evil 4": "Resident Evil 4",
    "Carcassonne": "Carcassonne (board game)",
    "Terraforming Mars": "Terraforming Mars (board game)",
    "Dixit": "Dixit (board game)",
    "Hollow Knight": "Hollow Knight",
    "Stardew Valley": "Stardew Valley",
    "Slay the Spire": "Slay the Spire",
    "Codenames": "Codenames (board game)",
    "Celeste": "Celeste (video game)",
    "Arkham Horror": "Arkham Horror",
    "The Witcher 3": "The Witcher 3: Wild Hunt",
    "Hades": "Hades (video game)",
    "Disco Elysium": "Disco Elysium",
    "Divinity: Original Sin 2": "Divinity: Original Sin II",
    "Exploding Kittens": "Exploding Kittens",
    "Into the Breach": "Into the Breach",
    "Inscryption": "Inscryption",
    "Story of Seasons": "Story of Seasons: Pioneers of Olive Town",
    "Alan Wake 2": "Alan Wake 2",
    "Wingspan": "Wingspan (board game)",
    "Cascadia": "Cascadia (board game)",
    "A Short Hike": "A Short Hike",
    "Outer Wilds": "Outer Wilds",
    "Balatro": "Balatro (video game)",
    "Isle of Cats": "The Isle of Cats",
}


def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=20) as r:
        return r.read()


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for title, article in ARTICLES.items():
        name = re.sub(r"[^A-Za-z0-9]+", "_", title).strip("_") + ".jpg"
        if (OUT / name).exists():
            continue
        try:
            s = json.loads(get("https://en.wikipedia.org/api/rest_v1/page/summary/" + urllib.parse.quote(article.replace(" ", "_"))))
            src = (s.get("originalimage") or s.get("thumbnail") or {}).get("source")
            if not src:
                print("no picture:", title)
                continue
            (OUT / name).write_bytes(get(src))
            print("ok", title)
        except Exception as e:
            print("failed:", title, e)


if __name__ == "__main__":
    main()
