/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

/**
 * One layer of a project, bottom of the stack first.
 *
 * <p>Two shapes and no more, which is why the interface is sealed: a layer
 * either names a catalogue item ({@link PresetLayer}) or carries its own pixels
 * ({@link PaintLayer}).
 */
public sealed interface ProjectLayer permits PresetLayer, PaintLayer {

    boolean visible();

    /** 0 to 1 */
    double opacity();

    /** null when the layer is left alone */
    Composition.Adjustments adjustments();
}
