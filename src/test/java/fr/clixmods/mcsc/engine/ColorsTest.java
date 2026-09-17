/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Colour maths, against the bytes the reference engine really produced. */
class ColorsTest {

    @Test
    void a_uint8clampedarray_rounds_halves_to_even() {
        for (String[] v : Vectors.of("byteof")) {
            double x = Double.parseDouble(v[0]);
            assertThat(Colors.byteOf(x)).as("byteOf(%s)", v[0]).isEqualTo(Integer.parseInt(v[1]));
        }
    }

    @Test
    void the_rounding_differs_from_math_round_where_it_matters() {
        // the whole reason byteOf exists: Math.round would say 1, 3 and 255
        assertThat(Colors.byteOf(0.5)).isZero();
        assertThat(Colors.byteOf(2.5)).isEqualTo(2);
        assertThat(Colors.byteOf(254.5)).isEqualTo(254);
        assertThat(Math.round(0.5)).isEqualTo(1);
    }

    @Test
    void every_accepted_colour_notation_parses_to_the_same_rgba() {
        for (String[] v : Vectors.of("parse")) {
            String in = Vectors.field(v[0]);
            assertThat(Colors.parse(in)).as("parse(%s)", v[0])
                    .containsExactly(Integer.parseInt(v[1]), Integer.parseInt(v[2]),
                            Integer.parseInt(v[3]), Integer.parseInt(v[4]));
        }
    }

    @Test
    void a_null_colour_parses_to_a_transparent_black() {
        assertThat(Colors.parse(null)).containsExactly(0, 0, 0, 0);
    }

    @Test
    void hex_normalises_to_six_lower_case_digits() {
        for (String[] v : Vectors.of("hex")) {
            assertThat(Colors.hex(Vectors.field(v[0]))).as("hex(%s)", v[0]).isEqualTo(v[1]);
        }
    }

    @Test
    void the_conversions_between_rgb_and_hsl_match_to_the_last_bit() {
        for (String[] v : Vectors.of("rgbtohsl")) {
            double[] hsl = Colors.rgbToHsl(Integer.parseInt(v[0]), Integer.parseInt(v[1]),
                    Integer.parseInt(v[2]));
            assertThat(hsl).as("rgbToHsl(%s, %s, %s)", v[0], v[1], v[2])
                    .containsExactly(Double.parseDouble(v[3]), Double.parseDouble(v[4]),
                            Double.parseDouble(v[5]));
        }
        for (String[] v : Vectors.of("hsltorgb")) {
            int[] rgb = Colors.hslToRgb(Double.parseDouble(v[0]), Double.parseDouble(v[1]),
                    Double.parseDouble(v[2]));
            assertThat(rgb).as("hslToRgb(%s, %s, %s)", v[0], v[1], v[2])
                    .containsExactly(Integer.parseInt(v[3]), Integer.parseInt(v[4]),
                            Integer.parseInt(v[5]));
        }
    }

    @Test
    void shifting_hue_saturation_and_lightness_keeps_the_alpha() {
        // the reference takes a colour and parses it; this port takes the
        // components, because Composition.adjustBuffer already has them apart
        for (String[] v : Vectors.of("hsl")) {
            int[] c = Colors.parse(v[0]);
            int[] out = Colors.hsl(c[0], c[1], c[2], c[3],
                    Double.parseDouble(v[1]), Double.parseDouble(v[2]), Double.parseDouble(v[3]));
            assertThat(out).as("hsl(%s, %s, %s, %s)", v[0], v[1], v[2], v[3])
                    .containsExactly(Integer.parseInt(v[4]), Integer.parseInt(v[5]),
                            Integer.parseInt(v[6]), Integer.parseInt(v[7]));
            assertThat(out[3]).as("alpha is carried through untouched").isEqualTo(c[3]);
        }
    }

    @Test
    void a_malformed_hex_colour_is_read_exactly_as_the_reference_reads_it() {
        // the reference slices, which clamps; a length guard would answer black
        assertThat(Colors.parse("#12345")).containsExactly(0x12, 0x34, 0x5, 255);
        assertThat(Colors.parse("#1")).containsExactly(1, 0, 0, 255);
        assertThat(Colors.parse("#")).containsExactly(0, 0, 0, 255);
    }

    @Test
    void the_vectors_cover_every_colour_function() {
        assertThat(List.of(Vectors.of("byteof").size(), Vectors.of("parse").size(),
                Vectors.of("hex").size(), Vectors.of("rgbtohsl").size(),
                Vectors.of("hsltorgb").size(), Vectors.of("hsl").size()))
                .allSatisfy(n -> assertThat(n).isGreaterThan(3));
    }
}
