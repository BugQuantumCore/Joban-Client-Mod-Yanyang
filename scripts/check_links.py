"""Validate the static site's local links and anchors in GitHub Actions."""

from collections import Counter
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parents[1]
LOCALES = ("zh-CN", "zh-HK", "zh-TW", "lzh", "en-US", "en-GB", "fr-FR")


class Page(HTMLParser):
    def __init__(self, path):
        super().__init__(convert_charrefs=True)
        self.ids = Counter()
        self.links = []
        self.feed(path.read_text(encoding="utf-8"))

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if attrs.get("id"):
            self.ids[attrs["id"]] += 1
        for attribute in ("href", "src"):
            if attribute in attrs:
                self.links.append((self.getpos()[0], attrs[attribute]))


paths = [ROOT / "index.html"]
for locale in LOCALES:
    paths.extend(sorted((ROOT / locale).glob("*.html")))
pages = {path: Page(path) for path in paths}
errors = []
expected_pages = {path.name for path in (ROOT / LOCALES[0]).glob("*.html")}
for locale in LOCALES:
    actual_pages = {path.name for path in (ROOT / locale).glob("*.html")}
    if actual_pages != expected_pages:
        errors.append(f"{locale}: translated page set differs: {actual_pages ^ expected_pages}")
for path, page in pages.items():
    relative = path.relative_to(ROOT)
    for anchor, count in page.ids.items():
        if count > 1:
            errors.append(f"{relative}: duplicate id {anchor!r}")
    for line, link in page.links:
        parts = urlsplit(link)
        if parts.scheme or parts.netloc:
            continue
        target = (path.parent / unquote(parts.path)).resolve() if parts.path else path
        if target.is_dir():
            target /= "index.html"
        if not target.is_relative_to(ROOT) or not target.is_file():
            errors.append(f"{relative}:{line}: missing local target {link!r}")
        elif parts.fragment and target in pages and unquote(parts.fragment) not in pages[target].ids:
            errors.append(f"{relative}:{line}: missing anchor {link!r}")

if errors:
    raise SystemExit("\n".join(errors))
print(f"Checked {len(pages)} pages across {len(LOCALES)} locales; all local links and anchors resolve.")
