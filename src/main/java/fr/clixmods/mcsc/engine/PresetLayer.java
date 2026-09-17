/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import java.util.Map;

/**
 * A layer that names a catalogue item, with the colours chosen for it.
 *
 * <p>{@code colors} maps a key name to "#rrggbb". A key left out, empty, or set
 * to the item's own default colour changes nothing - the recolouring is a HSL
 * delta, so an unchanged colour is the identity.
 */
public record PresetLayer(String cat, String id, Map<String, String> colors, boolean visible,
                          double opacity, Composition.Adjustments adjustments) implements ProjectLayer {

    public PresetLayer {
        colors = colors == null ? Map.of() : Map.copyOf(colors);
    }
}
