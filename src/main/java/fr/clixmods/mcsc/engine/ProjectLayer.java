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

    /** the bounds a layer's opacity is held between, which the clients share */
    double OPACITY_MIN = 0;
    double OPACITY_MAX = 1;

    boolean visible();

    /**
     * {@value #OPACITY_MIN} to {@value #OPACITY_MAX}.
     *
     * <p>A fraction, not a percentage: the sliders of an interface count in whole
     * percent and divide on the way in. Getting that wrong is refused by the site's
     * validator and composes an invisible or an opaque layer anywhere else.
     */
    double opacity();

    /** null when the layer is left alone */
    Composition.Adjustments adjustments();
}
