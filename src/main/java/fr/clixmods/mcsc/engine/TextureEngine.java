/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turning a project into a 64x64 texture: recolouring an item through its zone
 * map, then stacking the layers.
 *
 * <p>This is js/assets.js in Java. Byte-for-byte parity with it is what the
 * whole library is for, and it is proven in the MCSkinCreator repository by
 * TextureEngineRepoTest, on the real catalogue. The tests here run on frozen
 * cases and catch a regression at compile time; they do not replace it.
 *
 * <p>Pixels come from a {@link PresetSource}, which is the only thing this
 * class does not compute itself.
 */
public final class TextureEngine {

    private final PresetSource source;

    /** decoded zone maps, keyed by item and model; pure computation, so it is cached here */
    private final Map<String, byte[]> colorMaps = new ConcurrentHashMap<>();

    public TextureEngine(PresetSource source) {
        this.source = source;
    }

    /**
     * Forgets the decoded zone maps.
     *
     * <p>To be called when the catalogue has been re-read and an item may now
     * carry a different map under the same id - which is exactly what happens
     * on the site when the workshop rewrites a manifest while the server runs.
     */
    public void clearCache() {
        colorMaps.clear();
    }

    /* ------------------------------------------------------------------ */
    /* Zone map and recolouring                                            */
    /* ------------------------------------------------------------------ */

    /** "&lt;length base36&gt;&lt;VALUE A-Z&gt;", A = no key, B = key 1, ... */
    public static byte[] decodeRle(String s) {
        byte[] map = new byte[Model.SIZE * Model.SIZE];
        int p = 0;
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                int n;
                try {
                    n = num.length() == 0 ? 0 : Integer.parseInt(num.toString(), 36);
                } catch (NumberFormatException e) {
                    n = 0;
                }
                for (int j = 0; j < n && p < map.length; j++) {
                    map[p++] = (byte) (ch - 'A');
                }
                num.setLength(0);
            } else {
                num.append(ch);
            }
        }
        return map;
    }

    /** the key map: the manifest's one, or else "every opaque pixel is key 1" */
    public static byte[] colorMap(Preset el, boolean slim, int[] buf) {
        String rle = slim && el.slim() ? el.slimColorMap() : el.colorMap();
        if (rle != null) {
            return decodeRle(rle);
        }
        byte[] map = new byte[Model.SIZE * Model.SIZE];
        for (int p = 0; p < map.length; p++) {
            map[p] = (byte) (buf[p * 4 + 3] != 0 ? 1 : 0);
        }
        return map;
    }

    private byte[] cachedColorMap(Preset el, boolean slim, int[] buf) {
        return colorMaps.computeIfAbsent(key(el, slim), k -> colorMap(el, slim, buf));
    }

    private static String key(Preset el, boolean slim) {
        return el.cat() + "/" + el.id() + (slim && el.slim() ? "@slim" : "");
    }

    private record Delta(double h, double s, double sAbs, double l) { }

    /** the HSL gap between the original colour and the chosen one */
    private static Delta delta(String de, String to) {
        int[] a = Colors.parse(de), b = Colors.parse(to);
        double[] ha = Colors.rgbToHsl(a[0], a[1], a[2]), hb = Colors.rgbToHsl(b[0], b[1], b[2]);
        // -1: the source is grey, so the target saturation is imposed instead
        return new Delta(hb[0] - ha[0], ha[1] > 0.02 ? hb[1] / ha[1] : -1, hb[1], hb[2] - ha[2]);
    }

    /** an item's pixels, recoloured as asked (a copy, always) */
    public int[] applyColors(Preset el, int[] buf, Map<String, String> colors, boolean slim) {
        Map<Integer, Delta> violations = null;
        int ki = 0;
        for (ColorKey e : el.colors()) {
            ki++;
            String wanted = colors == null ? null : colors.get(e.name());
            if (wanted == null || wanted.isEmpty() || Colors.hex(wanted).equals(Colors.hex(e.defaultHex()))) {
                continue;
            }
            if (violations == null) {
                violations = new LinkedHashMap<>();
            }
            violations.put(ki, delta(e.defaultHex(), wanted));
        }
        int[] out = buf.clone();
        if (violations == null) {
            return out;
        }
        byte[] map = cachedColorMap(el, slim, buf);
        Map<Long, Integer> memo = new HashMap<>();   // source colour -> final colour, per key
        for (int p = 0; p < map.length; p++) {
            int k = map[p];
            if (k == 0) {
                continue;
            }
            Delta d = violations.get(k);
            if (d == null) {
                continue;
            }
            int i = p * 4;
            if (buf[i + 3] == 0) {
                continue;
            }
            long mk = k * 16777216L + (buf[i] << 16) + (buf[i + 1] << 8) + buf[i + 2];
            Integer hit = memo.get(mk);
            if (hit == null) {
                double[] t = Colors.rgbToHsl(buf[i], buf[i + 1], buf[i + 2]);
                double h = (t[0] + d.h() + 1) % 1;
                double s = d.s() < 0 ? d.sAbs() : t[1] * d.s();
                double l = t[2] + d.l();
                s = s < 0 ? 0 : s > 1 ? 1 : s;
                l = l < 0 ? 0 : l > 1 ? 1 : l;
                int[] c = Colors.hslToRgb(h, s, l);
                hit = (c[0] << 16) + (c[1] << 8) + c[2];
                memo.put(mk, hit);
            }
            out[i] = (hit >> 16) & 255;
            out[i + 1] = (hit >> 8) & 255;
            out[i + 2] = hit & 255;
        }
        return out;
    }

    /** an item's recoloured buffer; transparent when the item or its pixels are missing (never throws) */
    public int[] buildPreset(String cat, String id, Map<String, String> colors, boolean slim) {
        Optional<Preset> el = source.preset(cat, id);
        if (el.isEmpty()) {
            return new int[Composition.BYTES];
        }
        return source.pixels(el.get(), slim).map(buf -> applyColors(el.get(), buf, colors, slim))
                .orElseGet(() -> new int[Composition.BYTES]);
    }

    /* ------------------------------------------------------------------ */
    /* A saved project                                                     */
    /* ------------------------------------------------------------------ */

    /** a project's layers, ready to composite - what dataLayers does in the front end */
    public List<Composition.ComposedLayer> layers(Project project) {
        List<Composition.ComposedLayer> out = new ArrayList<>();
        for (ProjectLayer o : project.layers()) {
            int[] raw = switch (o) {
                case PaintLayer p -> p.pixels();
                case PresetLayer p -> buildPreset(p.cat(), p.id(), p.colors(), project.slim());
            };
            if (raw == null) {
                continue;
            }
            out.add(new Composition.ComposedLayer(o.visible(), o.opacity(),
                    Composition.adjustBuffer(raw, o.adjustments())));
        }
        return out;
    }

    /** a project's 64x64 texture */
    public int[] texture(Project project) {
        return Composition.composite(layers(project));
    }

    /** a project's thumbnail: the full-length front view, enlarged n times */
    public int[] thumbnail(Project project, int n) {
        int[] face = Composition.frontSprite(texture(project), project.slim());
        return Composition.scale(face, 16, Composition.SPRITE_CROP.get("all"), n);
    }
}
