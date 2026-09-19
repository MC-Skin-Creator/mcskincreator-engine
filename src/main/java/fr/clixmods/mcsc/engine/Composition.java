/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import java.util.List;
import java.util.Map;

/**
 * Stacking layers, hue/saturation/lightness adjustments and the front view, a
 * port of js/core.js. A buffer is an RGBA {@code int[]} of 64x64x4 values
 * between 0 and 255, laid out like a Uint8ClampedArray.
 */
public final class Composition {

    private Composition() { }

    public static final int BYTES = Model.SIZE * Model.SIZE * 4;

    /** what compositing reads from a layer */
    public record ComposedLayer(boolean visible, double opacity, int[] buffer) { }

    /**
     * A layer's adjustments: hue in degrees, saturation as a factor, lightness as an
     * offset.
     *
     * <p>The bounds are the clients' shared contract rather than a limit of the
     * calculation, which clamps what it is given: an interface counts in whole
     * percent for the last two and divides on the way in, and a project outside these
     * is refused by the site's validator.
     */
    public record Adjustments(double hue, double saturation, double lightness) {
        public static final Adjustments NEUTRAL = new Adjustments(0, 1, 0);

        public static final double HUE_MIN = -180;
        public static final double HUE_MAX = 180;
        public static final double SATURATION_MIN = 0;
        public static final double SATURATION_MAX = 2;
        public static final double LIGHTNESS_MIN = -1;
        public static final double LIGHTNESS_MAX = 1;
    }

    public static int[] composite(List<ComposedLayer> layers) {
        int[] out = new int[BYTES];
        for (ComposedLayer c : layers) {
            if (!c.visible() || c.buffer() == null) {
                continue;
            }
            double op = c.opacity();
            if (!(op > 0)) {
                continue;
            }
            int[] src = c.buffer();
            int n = Math.min(src.length, out.length);
            for (int i = 0; i + 3 < n; i += 4) {
                double sa = (src[i + 3] / 255.0) * op;
                if (!(sa > 0)) {
                    continue;
                }
                if (sa >= 1) {
                    out[i] = src[i];
                    out[i + 1] = src[i + 1];
                    out[i + 2] = src[i + 2];
                    out[i + 3] = 255;
                    continue;
                }
                double da = out[i + 3] / 255.0, oa = sa + da * (1 - sa);
                out[i] = Colors.byteOf((src[i] * sa + out[i] * da * (1 - sa)) / oa);
                out[i + 1] = Colors.byteOf((src[i + 1] * sa + out[i + 1] * da * (1 - sa)) / oa);
                out[i + 2] = Colors.byteOf((src[i + 2] * sa + out[i + 2] * da * (1 - sa)) / oa);
                out[i + 3] = Colors.byteOf(oa * 255);
            }
        }
        return out;
    }

    /** hue/saturation/lightness over a buffer; the buffer itself when there is nothing to change */
    public static int[] adjustBuffer(int[] src, Adjustments adj) {
        if (adj == null) {
            return src;
        }
        double dh = adj.hue(), ds = adj.saturation(), dl = adj.lightness();
        if (dh == 0 && ds == 1 && dl == 0) {
            return src;
        }
        int[] out = new int[src.length];
        for (int i = 0; i + 3 < src.length; i += 4) {
            if (src[i + 3] == 0) {
                continue;
            }
            int[] c = Colors.hsl(src[i], src[i + 1], src[i + 2], src[i + 3], dh, ds, dl);
            out[i] = c[0];
            out[i + 1] = c[1];
            out[i + 2] = c[2];
            out[i + 3] = src[i + 3];
        }
        return out;
    }

    /* ------------------------------------------------------------------ */
    /* The 16x32 front view                                                */
    /* ------------------------------------------------------------------ */
    private record Slot(String part, int x, int y) { }

    private static final List<Slot> LAYOUT = List.of(
            new Slot("armR", 0, 8), new Slot("legR", 4, 20), new Slot("legL", 8, 20),
            new Slot("body", 4, 8), new Slot("head", 4, 0), new Slot("armL", 12, 8));

    /** thumbnail crops, in texels of the front view: x, y, width, height */
    public static final Map<String, int[]> SPRITE_CROP = Map.of(
            "base", new int[] {0, 0, 16, 32},
            "head", new int[] {3, 0, 10, 11},
            "torso", new int[] {0, 6, 16, 16},
            "headtorso", new int[] {2, 0, 12, 22},
            "arms", new int[] {0, 6, 16, 16},
            "legs", new int[] {2, 17, 12, 15},
            "all", new int[] {0, 0, 16, 32});

    /** the front view (base layer then outer layer) in a 16x32 buffer */
    public static int[] frontSprite(int[] tex, boolean slim) {
        int[] img = new int[16 * 32 * 4];
        for (String layer : List.of("base", "over")) {
            for (Slot p : LAYOUT) {
                Model.Rect r = Model.faceRect(p.part(), "front", layer, slim);
                int w = Model.width(p.part(), slim);
                int pad = "armR".equals(p.part()) && slim ? 1 : 0;   // pushes the slim arm against the body
                for (int j = 0; j < r.h(); j++) {
                    for (int i = 0; i < w; i++) {
                        int si = ((r.y() + j) * Model.SIZE + (r.x() + i)) * 4;
                        int a = tex[si + 3];
                        if (a == 0) {
                            continue;
                        }
                        int dx = p.x() + i + pad, dy = p.y() + j;
                        if (dx < 0 || dy < 0 || dx >= 16 || dy >= 32) {
                            continue;
                        }
                        int di = (dy * 16 + dx) * 4;
                        double sa = a / 255.0, da = img[di + 3] / 255.0, oa = sa + da * (1 - sa);
                        img[di] = Colors.byteOf((tex[si] * sa + img[di] * da * (1 - sa)) / oa);
                        img[di + 1] = Colors.byteOf((tex[si + 1] * sa + img[di + 1] * da * (1 - sa)) / oa);
                        img[di + 2] = Colors.byteOf((tex[si + 2] * sa + img[di + 2] * da * (1 - sa)) / oa);
                        img[di + 3] = Colors.byteOf(oa * 255);
                    }
                }
            }
        }
        return img;
    }

    /**
     * A region of a buffer, enlarged n times with nearest neighbour.
     *
     * <p>It lives here rather than next to the PNG encoding it was written
     * beside, because it is never called on its own: a thumbnail is always
     * {@link #frontSprite} then a {@link #SPRITE_CROP} then this.
     *
     * @param w    the width in pixels of the source buffer
     * @param crop x, y, width, height, in pixels of the source buffer
     */
    public static int[] scale(int[] rgba, int w, int[] crop, int n) {
        int cw = crop[2] * n, ch = crop[3] * n;
        int[] out = new int[cw * ch * 4];
        for (int y = 0; y < ch; y++) {
            for (int x = 0; x < cw; x++) {
                int si = ((crop[1] + y / n) * w + (crop[0] + x / n)) * 4, di = (y * cw + x) * 4;
                System.arraycopy(rgba, si, out, di, 4);
            }
        }
        return out;
    }
}
