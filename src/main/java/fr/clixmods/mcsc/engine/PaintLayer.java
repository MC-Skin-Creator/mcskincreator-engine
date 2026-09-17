/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

/**
 * A layer drawn pixel by pixel, carrying its own RGBA buffer of
 * {@link Composition#BYTES} values.
 *
 * <p>The buffer arrives decoded. The editor stores such a layer base64-encoded,
 * but that is the serialisation format, and the serialisation format stops at
 * the caller: see the README.
 */
public record PaintLayer(int[] pixels, boolean visible, double opacity,
                         Composition.Adjustments adjustments) implements ProjectLayer { }
