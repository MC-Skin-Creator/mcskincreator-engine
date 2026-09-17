/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Stacking, adjustments and the front view, against the reference bytes. */
class CompositionTest {

    @Test
    void stacking_layers_gives_the_reference_bytes() {
        for (String[] v : Vectors.of("composite")) {
            List<Composition.ComposedLayer> layers = new ArrayList<>();
            for (int i = 1; i < v.length; i++) {
                String[] l = v[i].split(",");
                layers.add(new Composition.ComposedLayer(Boolean.parseBoolean(l[1]),
                        Double.parseDouble(l[2]), Vectors.synth(Integer.parseInt(l[0]))));
            }
            assertThat(Vectors.sha(Composition.composite(layers)))
                    .as("composite of %d layer(s)", layers.size()).isEqualTo(v[0]);
        }
    }

    @Test
    void an_invisible_or_fully_transparent_layer_contributes_nothing() {
        int[] a = Vectors.synth(0), b = Vectors.synth(1);
        int[] alone = Composition.composite(List.of(new Composition.ComposedLayer(true, 1, a)));
        int[] withHidden = Composition.composite(List.of(
                new Composition.ComposedLayer(true, 1, a),
                new Composition.ComposedLayer(false, 1, b)));
        int[] withEmpty = Composition.composite(List.of(
                new Composition.ComposedLayer(true, 1, a),
                new Composition.ComposedLayer(true, 0, b)));
        assertThat(withHidden).isEqualTo(alone);
        assertThat(withEmpty).isEqualTo(alone);
    }

    @Test
    void adjusting_a_buffer_gives_the_reference_bytes() {
        for (String[] v : Vectors.of("adjust")) {
            Composition.Adjustments adj = "null".equals(v[1]) ? null
                    : new Composition.Adjustments(Double.parseDouble(v[1]),
                            Double.parseDouble(v[2]), Double.parseDouble(v[3]));
            int[] out = Composition.adjustBuffer(Vectors.synth(Integer.parseInt(v[0])), adj);
            assertThat(Vectors.sha(out)).as("adjust seed %s by %s", v[0], adj).isEqualTo(v[4]);
        }
    }

    @Test
    void a_neutral_adjustment_hands_back_the_very_same_array() {
        // not an optimisation but a contract: the caller may keep the reference
        int[] src = Vectors.synth(0);
        assertThat(Composition.adjustBuffer(src, null)).isSameAs(src);
        assertThat(Composition.adjustBuffer(src, Composition.Adjustments.NEUTRAL)).isSameAs(src);
        assertThat(Composition.adjustBuffer(src, new Composition.Adjustments(0, 1, 0))).isSameAs(src);
        assertThat(Composition.adjustBuffer(src, new Composition.Adjustments(1, 1, 0))).isNotSameAs(src);
    }

    @Test
    void the_front_view_gives_the_reference_bytes_on_both_models() {
        for (String[] v : Vectors.of("frontsprite")) {
            int[] tex = Vectors.synth(Integer.parseInt(v[0]));
            assertThat(Vectors.sha(Composition.frontSprite(tex, false))).as("classic").isEqualTo(v[1]);
            assertThat(Vectors.sha(Composition.frontSprite(tex, true))).as("slim").isEqualTo(v[2]);
        }
    }

    @Test
    void every_thumbnail_crop_stays_inside_the_front_view() {
        for (var e : Composition.SPRITE_CROP.entrySet()) {
            int[] c = e.getValue();
            assertThat(c).as("%s has x, y, width, height", e.getKey()).hasSize(4);
            assertThat(c[0] + c[2]).as("%s ends inside the 16 columns", e.getKey()).isLessThanOrEqualTo(16);
            assertThat(c[1] + c[3]).as("%s ends inside the 32 rows", e.getKey()).isLessThanOrEqualTo(32);
        }
    }

    @Test
    void scaling_repeats_each_pixel_n_times_in_both_directions() {
        int[] src = Vectors.synth(0);
        int[] crop = {2, 3, 4, 5};
        int[] out = Composition.scale(src, Model.SIZE, crop, 3);
        assertThat(out).hasSize(4 * 3 * 5 * 3 * 4);
        for (int y = 0; y < 5 * 3; y++) {
            for (int x = 0; x < 4 * 3; x++) {
                int si = ((3 + y / 3) * Model.SIZE + (2 + x / 3)) * 4, di = (y * 12 + x) * 4;
                assertThat(out[di]).as("(%d, %d) red", x, y).isEqualTo(src[si]);
                assertThat(out[di + 3]).as("(%d, %d) alpha", x, y).isEqualTo(src[si + 3]);
            }
        }
    }

    @Test
    void a_buffer_is_sixty_four_squared_rgba() {
        assertThat(Composition.BYTES).isEqualTo(64 * 64 * 4);
        assertThat(Vectors.synth(0)).hasSize(Composition.BYTES);
    }
}
