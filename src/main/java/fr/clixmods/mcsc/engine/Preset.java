/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import java.util.List;

/**
 * A catalogue item, as the engine needs it: what to recolour and how, never
 * where the pixels come from.
 *
 * <p>Addressing is the caller's business - the site resolves a PNG on disk, the
 * mod an atlas it has downloaded - so there is no path, no URL and no prefix
 * here. Display is the caller's business too, so no label and no thumbnail crop.
 *
 * @param cat           the category id, "hair", "eyes"...
 * @param id            the item id, unique within the catalogue
 * @param colors        the recolourable keys <b>in order</b>; the zone map
 *                      refers to them by their one-based position
 * @param slim          whether a distinct slim-model variant exists
 * @param colorMap      the zone map, run-length encoded, or null when trivial
 *                      (every opaque pixel belongs to key 1)
 * @param slimColorMap  the zone map of the slim variant, same convention
 */
public record Preset(String cat, String id, List<ColorKey> colors, boolean slim,
                     String colorMap, String slimColorMap) {

    public Preset {
        colors = colors == null ? List.of() : List.copyOf(colors);
    }
}
