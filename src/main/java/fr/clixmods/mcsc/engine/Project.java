/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import java.util.List;

/**
 * A skin as the editor saves it: a model and a stack of layers.
 *
 * @param slim   the slim model (3-texel arms) rather than the classic one
 * @param layers bottom of the stack first
 */
public record Project(boolean slim, List<ProjectLayer> layers) {

    /**
     * The most layers a project carries.
     *
     * <p>Not a limit of the calculation - it stacks whatever it is handed - but of
     * the contract the clients share: the site refuses a longer project by path, the
     * mod stops stacking at it, and a number written down in two places is a number
     * that ends up saying two things.
     */
    public static final int MAX_LAYERS = 300;

    public Project {
        layers = layers == null ? List.of() : List.copyOf(layers);
    }
}
