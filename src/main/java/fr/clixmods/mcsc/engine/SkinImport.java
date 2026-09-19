/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

/**
 * A skin texture found elsewhere, brought back to the 64x64 the editor works in.
 *
 * <p>Two things are wrong with a texture off a skin site, and neither shows before
 * you hit it:
 *
 * <ul>
 *   <li><b>it is often "HD"</b> - 128x128, 256x256 - and almost always a nearest
 *       neighbour enlargement of a 64x64, which taking one texel in {@code f} undoes
 *       exactly. Anything else is refused rather than squashed: half of a 128x128 read
 *       as a 64x64 is mush announced as a success;</li>
 *   <li><b>it may be the pre-2013 64x32</b>, which has no left arm and no left leg -
 *       the game mirrored the right ones. They are painted here from
 *       {@link Model#mirrorTexel}, which is the same symmetry the editor's mirror tool
 *       uses. It is not an image flip: each face of a box has its own place in the
 *       unwrapping, and flipping the picture gives a left arm inside out.</li>
 * </ul>
 *
 * <p>Decoding the PNG is the caller's, as ever - this takes pixels and gives pixels.
 */
public final class SkinImport {

    private SkinImport() { }

    /**
     * The enlargement factor of a skin texture, or <b>0</b> when the size is not a
     * skin's at all: a multiple of 64 wide, and as tall as it is wide or half of it.
     */
    public static int factor(int width, int height) {
        if (width < Model.SIZE || width % Model.SIZE != 0 || (height != width && height * 2 != width)) {
            return 0;
        }
        return width / Model.SIZE;
    }

    /** whether the texture is the pre-2013 64x32, the one that needs unfolding */
    public static boolean legacy(int width, int height) {
        return factor(width, height) > 0 && height * 2 == width;
    }

    /**
     * The texture as the editor holds it: 64x64 RGBA, sampled down and unfolded.
     *
     * @param pixels the source, RGBA, {@code width * height * 4} values of 0 to 255
     * @return a fresh buffer of {@link Composition#BYTES}, or <b>null</b> when the
     *         size is not a skin's
     */
    public static int[] normalise(int[] pixels, int width, int height) {
        int f = factor(width, height);
        if (f == 0 || pixels == null || pixels.length < width * height * 4) {
            return null;
        }
        boolean legacy = legacy(width, height);
        int[] out = sample(pixels, width, f, legacy ? Model.SIZE / 2 : Model.SIZE);
        if (legacy) {
            unfold(out);
        }
        return out;
    }

    /** one texel in f, row by row */
    static int[] sample(int[] pixels, int width, int f, int rows) {
        int[] out = new int[Composition.BYTES];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < Model.SIZE; x++) {
                int source = (y * f * width + x * f) * 4, target = (y * Model.SIZE + x) * 4;
                System.arraycopy(pixels, source, out, target, 4);
            }
        }
        return out;
    }

    /** the old 64x32: the left arm and leg painted from the right ones, mirrored */
    static void unfold(int[] texture) {
        for (String part : java.util.List.of("armL", "legL")) {
            for (String face : Model.FACES) {
                Model.Rect r = Model.faceRect(part, face, "base", false);
                for (int y = 0; y < r.h(); y++) {
                    for (int x = 0; x < r.w(); x++) {
                        int tx = r.x() + x, ty = r.y() + y;
                        int[] source = Model.mirrorTexel(tx, ty, false);
                        if (source == null) {
                            continue;
                        }
                        System.arraycopy(texture, (source[1] * Model.SIZE + source[0]) * 4,
                                texture, (ty * Model.SIZE + tx) * 4, 4);
                    }
                }
            }
        }
    }
}
