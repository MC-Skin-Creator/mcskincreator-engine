/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * The order of the slots in a category's atlas.
 *
 * <p>An atlas is the RGBA buffers of a category's items end to end, and their order
 * is a contract between whoever writes it and whoever reads it: <b>each item, in
 * catalogue order, then its slim variant when it has one</b>. It is three lines, and
 * it is written here because getting it wrong is silent - an item's rank is not its
 * rank in the list, so everything after the first slim item is drawn with somebody
 * else's pixels, and nothing throws.
 *
 * <p>What an atlas is <i>made of</i> stays with the caller: the site reads PNG files
 * off its disk, the mod downloads a gzipped one. Only the order lives here.
 */
public final class Atlas {

    /** the bytes of one slot: a 64x64 RGBA buffer */
    public static final int SLOT_BYTES = Composition.BYTES;

    /** an item of a category, as the order needs it: its id, and whether it has a slim variant */
    public record Item(String id, boolean slim) { }

    private Atlas() { }

    /** the key of one slot, "cat/id" or "cat/id@slim" */
    public static String slot(String cat, String id, boolean slim) {
        return cat + "/" + id + (slim ? "@slim" : "");
    }

    /**
     * The keys of a category's slots, in the order they are laid out.
     *
     * <p>A slot's rank in this list is its rank in the atlas, which is what a reader
     * needs to find an item's pixels.
     */
    public static List<String> slots(String cat, List<Item> items) {
        List<String> keys = new ArrayList<>(items.size());
        for (Item item : items) {
            keys.add(slot(cat, item.id(), false));
            if (item.slim()) {
                keys.add(slot(cat, item.id(), true));
            }
        }
        return List.copyOf(keys);
    }
}
