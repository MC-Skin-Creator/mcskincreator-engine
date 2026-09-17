/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link PresetSource} held in memory, which is what the supply interface is
 * worth: the engine composes a whole project here without a file, a catalogue
 * or a network in sight.
 *
 * <p>It also plays the two cases a real source runs into - an item the
 * catalogue does not carry, and an item whose pixels are missing - because
 * buildPreset promises never to throw on either.
 */
final class FakePresetSource implements PresetSource {

    private final Map<String, Preset> presets = new HashMap<>();
    private final Map<String, int[]> pixels = new HashMap<>();

    /** an item with its pixels */
    FakePresetSource with(Preset preset, int[] classic) {
        presets.put(preset.cat() + "/" + preset.id(), preset);
        pixels.put(key(preset, false), classic);
        return this;
    }

    /** the slim variant of an item already declared */
    FakePresetSource withSlim(Preset preset, int[] slim) {
        pixels.put(key(preset, true), slim);
        return this;
    }

    /** an item the catalogue declares but whose pixels never arrived */
    FakePresetSource withoutPixels(Preset preset) {
        presets.put(preset.cat() + "/" + preset.id(), preset);
        return this;
    }

    private static String key(Preset p, boolean slim) {
        return p.cat() + "/" + p.id() + (slim && p.slim() ? "@slim" : "");
    }

    @Override
    public Optional<Preset> preset(String cat, String id) {
        return Optional.ofNullable(presets.get(cat + "/" + id));
    }

    @Override
    public Optional<int[]> pixels(Preset preset, boolean slim) {
        return Optional.ofNullable(pixels.get(key(preset, slim)));
    }
}
