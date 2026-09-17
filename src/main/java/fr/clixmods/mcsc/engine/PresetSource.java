/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import java.util.Optional;

/**
 * Where the engine gets a catalogue item and its pixels.
 *
 * <p>This is the seam the library exists for. The two callers do not fetch
 * their pixels from the same place: the site reads PNG files from the assets
 * folders of its repository, the mod reads the atlases it downloaded from
 * /api/atlas and cached on disk. Same computation, different supply.
 *
 * <p><b>Caching belongs to the implementation.</b> Reading a file and decoding
 * an atlas have neither the same cost nor the same invalidation, and the
 * library has no way to know when either goes stale.
 */
public interface PresetSource {

    /** the item of that category, or empty when the catalogue does not carry it */
    Optional<Preset> preset(String cat, String id);

    /**
     * the item's pixels at its default colours: an RGBA buffer of
     * {@link Composition#BYTES} values, or empty when they are missing.
     *
     * <p>{@code slim} asks for the slim-model variant, which only exists when
     * {@link Preset#slim()} is true; otherwise the classic pixels answer both.
     */
    Optional<int[]> pixels(Preset preset, boolean slim);
}
