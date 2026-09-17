/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

/**
 * One recolourable key of a catalogue item, and the colour it is drawn in.
 *
 * <p>The key's <b>position</b> in {@link Preset#colors()} is what the zone map
 * refers to, one-based: the first key is 1, and 0 means "no key". That is why
 * a preset carries an ordered list and not a map.
 */
public record ColorKey(String name, String defaultHex) { }
