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

    public Project {
        layers = layers == null ? List.of() : List.copyOf(layers);
    }
}
