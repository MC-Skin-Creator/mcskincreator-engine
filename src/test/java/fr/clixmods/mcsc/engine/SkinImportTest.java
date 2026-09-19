/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** A texture found elsewhere, brought back to the 64x64 the editor works in. */
class SkinImportTest {

    /** A texture of one colour, at {@code f} times a skin's size. */
    private static int[] texture(int width, int height, int red) {
        int[] pixels = new int[width * height * 4];
        for (int i = 0; i < pixels.length; i += 4) {
            pixels[i] = red;
            pixels[i + 3] = 255;
        }
        return pixels;
    }

    private static int[] pixel(int[] texture, int x, int y) {
        int i = (y * Model.SIZE + x) * 4;
        return new int[] {texture[i], texture[i + 1], texture[i + 2], texture[i + 3]};
    }

    private static void paint(int[] pixels, int width, int x, int y, int[] rgba) {
        int i = (y * width + x) * 4;
        System.arraycopy(rgba, 0, pixels, i, 4);
    }

    @Nested
    class TheSizesAccepted {
        @Test
        void a_plain_skin_is_taken_as_it_is() {
            assertThat(SkinImport.factor(64, 64)).isEqualTo(1);
            assertThat(SkinImport.legacy(64, 64)).isFalse();
        }

        @Test
        void an_hd_skin_is_a_whole_multiple() {
            assertThat(SkinImport.factor(128, 128)).isEqualTo(2);
            assertThat(SkinImport.factor(256, 256)).isEqualTo(4);
            assertThat(SkinImport.factor(1024, 512)).isEqualTo(16);
        }

        @Test
        void the_old_shape_is_half_as_tall() {
            assertThat(SkinImport.factor(64, 32)).isEqualTo(1);
            assertThat(SkinImport.legacy(64, 32)).isTrue();
            assertThat(SkinImport.legacy(128, 64)).isTrue();
        }

        @Test
        void anything_else_is_refused_rather_than_squashed() {
            assertThat(SkinImport.factor(63, 64)).isZero();
            assertThat(SkinImport.factor(100, 100)).isZero();
            assertThat(SkinImport.factor(64, 48)).isZero();
            assertThat(SkinImport.factor(32, 32)).isZero();
            assertThat(SkinImport.normalise(texture(100, 100, 9), 100, 100)).isNull();
        }
    }

    @Nested
    class WhatComesBack {
        @Test
        void a_skin_of_the_right_size_comes_back_whole() {
            int[] out = SkinImport.normalise(texture(64, 64, 200), 64, 64);

            assertThat(out).hasSize(Composition.BYTES);
            assertThat(pixel(out, 0, 0)).containsExactly(200, 0, 0, 255);
            assertThat(pixel(out, 63, 63)).containsExactly(200, 0, 0, 255);
        }

        @Test
        void an_hd_skin_keeps_one_texel_in_f() {
            int[] pixels = texture(128, 128, 10);
            // The texel a 64x64 reading takes, and the three it must not take.
            paint(pixels, 128, 20, 8, new int[] {1, 2, 3, 255});
            paint(pixels, 128, 21, 8, new int[] {9, 9, 9, 255});
            paint(pixels, 128, 20, 9, new int[] {8, 8, 8, 255});

            int[] out = SkinImport.normalise(pixels, 128, 128);

            assertThat(pixel(out, 10, 4)).containsExactly(1, 2, 3, 255);
        }

        @Test
        void the_old_shape_gets_its_left_limbs_from_the_right_ones() {
            int[] pixels = texture(64, 32, 0);
            // A texel of the right arm's front face, which the left arm has to mirror.
            Model.Rect right = Model.faceRect("armR", "front", "base", false);
            paint(pixels, 64, right.x(), right.y(), new int[] {7, 7, 7, 255});

            int[] out = SkinImport.normalise(pixels, 64, 32);
            Model.Rect left = Model.faceRect("armL", "front", "base", false);

            // The mirror swaps the limbs and flips the face, so the texel lands at the
            // other end of the left arm's front rather than at its corner.
            int[] mirrored = Model.mirrorTexel(right.x(), right.y(), false);
            assertThat(mirrored).isNotNull();
            assertThat(Model.texelInfo(left.x(), left.y(), false).part()).isEqualTo("armL");

            int[] lit = null;
            for (int y = left.y(); y < left.y() + left.h() && lit == null; y++) {
                for (int x = left.x(); x < left.x() + left.w(); x++) {
                    if (pixel(out, x, y)[0] == 7) {
                        lit = new int[] {x, y};
                        break;
                    }
                }
            }
            assertThat(lit).as("the right arm's texel reached the left arm").isNotNull();
            assertThat(Model.mirrorTexel(lit[0], lit[1], false)).containsExactly(right.x(), right.y());
        }

        @Test
        void the_bottom_half_of_the_old_shape_is_left_empty_where_nothing_mirrors_it() {
            int[] out = SkinImport.normalise(texture(64, 32, 120), 64, 32);

            // Row 32 onwards only carries what the unfolding painted: the left limbs.
            assertThat(pixel(out, 0, 32)).containsExactly(0, 0, 0, 0);
        }
    }
}
