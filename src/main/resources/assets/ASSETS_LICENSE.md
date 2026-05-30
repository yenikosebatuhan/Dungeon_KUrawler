# Dungeon Crawl Stone Soup (DCSS) asset license

Sprites prefixed with `dc_` in this folder are derived from **Dungeon Crawl Stone Soup**
tile art (48×48 pixel style), used for Kurawler’s dungeon theme.

- **Source:** [Dungeon Crawl Stone Soup](https://github.com/crawl/crawl) (tile database / OpenGameArt-style dungeon tiles bundled for this project).
- **License:** GNU General Public License v2 or later (GPL-2.0+), consistent with DCSS.
- **Use in this project:** Visual assets only; game logic is original.

If you redistribute this repository, include this notice and comply with GPL-2.0+
for the `dc_*.png` files (source offer / license text as required by the GPL).

Issue #112: searchable bookshelf uses `dc_bookshelf.png` (48×48 custom pixel art,
project-authored; brown carcass, three shelves, coloured book spines). The statics
sheet cells at (12,4) are vertical plank doors and were rejected. Loaded in
`TileRenderer.loadDcssOverrides()`; `loadStatics()` fallback uses sheet (10,4)
only if the override file is missing.
