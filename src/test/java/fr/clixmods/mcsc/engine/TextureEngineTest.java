/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Recolouring and composing, against the bytes the reference engine really
 * produced on synthetic items.
 */
class TextureEngineTest {

    /** the colours of a case, written "key=#rrggbb,key=#rrggbb" or "-" */
    private static Map<String, String> colours(String spec) {
        if ("-".equals(spec)) {
            return null;
        }
        Map<String, String> m = new java.util.LinkedHashMap<>();
        for (String pair : spec.split(",")) {
            String[] kv = pair.split("=");
            m.put(kv[0], kv[1]);
        }
        return m;
    }

    @Test
    void recolouring_an_item_gives_the_reference_bytes() {
        for (String[] v : Vectors.of("applycolors")) {
            Preset el = Vectors.preset(v[0]);
            int[] buf = Vectors.synth(Integer.parseInt(v[1]));
            boolean slim = Boolean.parseBoolean(v[2]);
            // a fresh engine per case, so that no cached zone map can hide a bug
            int[] out = new TextureEngine(new FakePresetSource())
                    .applyColors(el, buf, colours(v[3]), slim);
            assertThat(Vectors.sha(out)).as("%s, seed %s, slim=%s, colours %s",
                    v[0], v[1], v[2], v[3]).isEqualTo(v[4]);
        }
    }

    @Test
    void a_zone_map_decodes_to_the_reference_bytes() {
        for (String[] v : Vectors.of("keymap")) {
            Preset el = Vectors.preset(v[0]);
            int[] buf = Vectors.synth(1);
            assertThat(shaOf(TextureEngine.colorMap(el, false, buf))).as("%s classic", v[0]).isEqualTo(v[1]);
            assertThat(shaOf(TextureEngine.colorMap(el, true, buf))).as("%s slim", v[0]).isEqualTo(v[2]);
        }
    }

    private static String shaOf(byte[] map) {
        return Vectors.sha(map);
    }

    @Test
    void a_missing_zone_map_means_every_opaque_pixel_belongs_to_key_one() {
        Preset trivial = Vectors.preset("trivial-map");
        int[] buf = Vectors.synth(0);
        byte[] map = TextureEngine.colorMap(trivial, false, buf);
        assertThat(map).hasSize(Model.SIZE * Model.SIZE);
        for (int p = 0; p < map.length; p++) {
            assertThat(map[p]).as("texel %d", p).isEqualTo((byte) (buf[p * 4 + 3] != 0 ? 1 : 0));
        }
    }

    @Test
    void a_run_length_map_stops_at_the_end_of_the_texture() {
        // a run far longer than the texture must not overflow, and a length that
        // is not a base36 number counts as zero rather than throwing
        byte[] big = TextureEngine.decodeRle("zzzzB");
        assertThat(big).hasSize(Model.SIZE * Model.SIZE);
        assertThat(big[big.length - 1]).isEqualTo((byte) 1);

        assertThat(TextureEngine.decodeRle("")).containsOnly((byte) 0);
        assertThat(TextureEngine.decodeRle("B")).containsOnly((byte) 0);        // no length: zero long
        byte[] two = TextureEngine.decodeRle("2B3C");
        assertThat(two[0]).isEqualTo((byte) 1);
        assertThat(two[1]).isEqualTo((byte) 1);
        assertThat(two[2]).isEqualTo((byte) 2);
        assertThat(two[4]).isEqualTo((byte) 2);
        assertThat(two[5]).isEqualTo((byte) 0);
    }

    @Test
    void recolouring_always_hands_back_a_copy() {
        Preset el = Vectors.preset("two-keys");
        int[] buf = Vectors.synth(0);
        TextureEngine engine = new TextureEngine(new FakePresetSource());

        int[] untouched = engine.applyColors(el, buf, null, false);
        assertThat(untouched).isNotSameAs(buf).isEqualTo(buf);

        int[] recoloured = engine.applyColors(el, buf, Map.of("main", "#ff0000"), false);
        assertThat(recoloured).isNotSameAs(buf).isNotEqualTo(buf);
        assertThat(buf).as("the source is never written into").isEqualTo(Vectors.synth(0));
    }

    @Test
    void a_colour_equal_to_the_default_changes_nothing() {
        Preset el = Vectors.preset("two-keys");
        int[] buf = Vectors.synth(0);
        TextureEngine engine = new TextureEngine(new FakePresetSource());
        assertThat(engine.applyColors(el, buf, Map.of("main", "#3B6EA5"), false))
                .as("the comparison folds case").isEqualTo(buf);
        assertThat(engine.applyColors(el, buf, Map.of("main", ""), false)).isEqualTo(buf);
        assertThat(engine.applyColors(el, buf, Map.of("nosuchkey", "#ff00ff"), false)).isEqualTo(buf);
    }

    @Test
    void recolouring_never_touches_the_alpha() {
        Preset el = Vectors.preset("two-keys");
        int[] buf = Vectors.synth(2);
        int[] out = new TextureEngine(new FakePresetSource())
                .applyColors(el, buf, Map.of("main", "#ff0000", "accent", "#00ff88"), false);
        for (int p = 0; p < Model.SIZE * Model.SIZE; p++) {
            assertThat(out[p * 4 + 3]).as("alpha of texel %d", p).isEqualTo(buf[p * 4 + 3]);
        }
    }

    @Test
    void building_a_missing_item_gives_an_empty_buffer_rather_than_throwing() {
        Preset known = Vectors.preset("trivial-map");
        TextureEngine engine = new TextureEngine(new FakePresetSource()
                .with(known, Vectors.synth(0))
                .withoutPixels(new Preset("syn", "no-pixels", List.of(), false, null, null)));

        assertThat(engine.buildPreset("syn", "never-heard-of-it", null, false))
                .as("unknown item").containsOnly(0).hasSize(Composition.BYTES);
        assertThat(engine.buildPreset("syn", "no-pixels", null, false))
                .as("known item, missing pixels").containsOnly(0).hasSize(Composition.BYTES);
        assertThat(engine.buildPreset("syn", "trivial-map", null, false))
                .as("an item that is there").isEqualTo(Vectors.synth(0));
    }

    @Test
    void a_project_composes_through_the_supply_interface() {
        Preset el = Vectors.preset("two-keys");
        TextureEngine engine = new TextureEngine(new FakePresetSource().with(el, Vectors.synth(0)));

        Project project = new Project(false, List.of(
                new PresetLayer("syn", "two-keys", Map.of(), true, 1, null),
                new PaintLayer(Vectors.synth(1), true, 0.5, null)));

        int[] expected = Composition.composite(List.of(
                new Composition.ComposedLayer(true, 1, Vectors.synth(0)),
                new Composition.ComposedLayer(true, 0.5, Vectors.synth(1))));
        assertThat(engine.texture(project)).isEqualTo(expected);
        assertThat(engine.layers(project)).hasSize(2);
    }

    @Test
    void the_slim_model_asks_the_source_for_the_slim_pixels() {
        Preset el = Vectors.preset("slim-variant");
        TextureEngine engine = new TextureEngine(new FakePresetSource()
                .with(el, Vectors.synth(0)).withSlim(el, Vectors.synth(2)));

        assertThat(engine.buildPreset("syn", "slim-variant", null, false)).isEqualTo(Vectors.synth(0));
        assertThat(engine.buildPreset("syn", "slim-variant", null, true)).isEqualTo(Vectors.synth(2));
    }

    @Test
    void a_thumbnail_is_the_front_view_enlarged() {
        Preset el = Vectors.preset("two-keys");
        TextureEngine engine = new TextureEngine(new FakePresetSource().with(el, Vectors.synth(0)));
        Project project = new Project(false,
                List.of(new PresetLayer("syn", "two-keys", Map.of(), true, 1, null)));

        int[] crop = Composition.SPRITE_CROP.get("all");
        assertThat(engine.thumbnail(project, 3)).hasSize(crop[2] * 3 * crop[3] * 3 * 4);
    }

    @Test
    void forgetting_the_cached_maps_does_not_change_what_comes_out() {
        Preset el = Vectors.preset("two-keys");
        int[] buf = Vectors.synth(1);
        Map<String, String> colours = Map.of("accent", "#101010");
        TextureEngine engine = new TextureEngine(new FakePresetSource());

        int[] first = engine.applyColors(el, buf, colours, false);
        int[] cached = engine.applyColors(el, buf, colours, false);
        engine.clearCache();
        int[] afterClear = engine.applyColors(el, buf, colours, false);

        assertThat(cached).isEqualTo(first);
        assertThat(afterClear).isEqualTo(first);
    }
}
