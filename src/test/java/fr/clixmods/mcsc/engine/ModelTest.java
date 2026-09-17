/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The unwrapping of the 64x64 texture. These are the invariants the twin
 * repository watches, and they hold without a catalogue, so they are tested
 * here rather than only there.
 */
class ModelTest {

    private static final List<String> LAYERS = List.of("base", "over");

    @Test
    void no_uv_area_overlaps_another_or_leaves_the_texture() {
        for (boolean slim : List.of(false, true)) {
            String[] owner = new String[Model.SIZE * Model.SIZE];
            for (String part : Model.PART_IDS) {
                for (String layer : LAYERS) {
                    for (String face : Model.FACES) {
                        Model.Rect r = Model.faceRect(part, face, layer, slim);
                        String who = part + "." + layer + "." + face;
                        assertThat(r.x()).as("%s starts inside", who).isNotNegative();
                        assertThat(r.y()).as("%s starts inside", who).isNotNegative();
                        assertThat(r.x() + r.w()).as("%s ends inside", who).isLessThanOrEqualTo(Model.SIZE);
                        assertThat(r.y() + r.h()).as("%s ends inside", who).isLessThanOrEqualTo(Model.SIZE);
                        for (int y = r.y(); y < r.y() + r.h(); y++) {
                            for (int x = r.x(); x < r.x() + r.w(); x++) {
                                int p = y * Model.SIZE + x;
                                assertThat(owner[p]).as("(%d, %d) claimed by %s and %s, slim=%s",
                                        x, y, owner[p], who, slim).isNull();
                                owner[p] = who;
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void the_slim_model_narrows_the_arms_and_nothing_else() {
        for (String part : Model.PART_IDS) {
            int classic = Model.width(part, false), slim = Model.width(part, true);
            if (part.startsWith("arm")) {
                assertThat(classic).isEqualTo(4);
                assertThat(slim).isEqualTo(3);
            } else {
                assertThat(slim).as("%s keeps its width", part).isEqualTo(classic);
            }
        }
    }

    @Test
    void the_mirror_is_involutive_and_swaps_limbs() {
        for (boolean slim : List.of(false, true)) {
            for (int y = 0; y < Model.SIZE; y++) {
                for (int x = 0; x < Model.SIZE; x++) {
                    int[] m = Model.mirrorTexel(x, y, slim);
                    if (m == null) {
                        continue;
                    }
                    int[] back = Model.mirrorTexel(m[0], m[1], slim);
                    assertThat(back).as("mirror of (%d, %d), slim=%s", x, y, slim)
                            .containsExactly(x, y);
                }
            }
        }
        Model.Texel t = Model.texelInfo(44, 20, false);
        assertThat(t).isNotNull();
        assertThat(t.part()).isEqualTo("armR");
        int[] m = Model.mirrorTexel(44, 20, false);
        assertThat(Model.texelInfo(m[0], m[1], false).part()).isEqualTo("armL");
    }

    @Test
    void a_texel_outside_the_unwrapping_belongs_to_nothing() {
        // the dead column, u56-63 / v16-47: no part claims it
        assertThat(Model.texelInfo(60, 30, false)).isNull();
        assertThat(Model.mirrorTexel(60, 30, false)).isNull();
    }

    @Test
    void an_unknown_face_is_refused_rather_than_guessed() {
        assertThatThrownBy(() -> Model.faceRect("head", "sideways", "base", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sideways");
    }
}
