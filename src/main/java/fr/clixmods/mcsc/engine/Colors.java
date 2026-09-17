/* MC Skin Creator - Copyright (C) 2026 clixmods - Tous droits réservés (voir LICENSE) */
package fr.clixmods.mcsc.engine;

/**
 * Colors — portage de CLR (js/core.js), calcul pour calcul.
 *
 * <p>Le moteur existe en trois exemplaires : js/core.js (l'atelier), le
 * TypeScript du front (l'éditeur, en temps réel) et celui-ci (le serveur, qui
 * recompose les textures enregistrées). Ils ne tiennent ensemble que par les
 * tests de parité : TextureEngineRepoTest ici, parite.spec.ts côté front.
 * Deux détails comptent pour rendre les mêmes octets :
 * <ul>
 *   <li>{@code Math.round} de Java et de JavaScript arrondissent tous deux la
 *       demie vers +∞ ;</li>
 *   <li>un {@code Uint8ClampedArray} arrondit, lui, la demie <b>au pair</b> :
 *       c'est {@link #octet(double)}.</li>
 * </ul>
 */
public final class Colors {

    private Colors() { }

    /** l'écriture dans un Uint8ClampedArray : bornée à 0..255, demie arrondie au pair */
    public static int byteOf(double x) {
        if (Double.isNaN(x) || x <= 0) {
            return 0;
        }
        if (x >= 255) {
            return 255;
        }
        double f = Math.floor(x);
        double reste = x - f;
        if (reste < 0.5) {
            return (int) f;
        }
        if (reste > 0.5) {
            return (int) f + 1;
        }
        return ((int) f) % 2 == 0 ? (int) f : (int) f + 1;
    }

    private static int toHex(String s) {
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** « #rgb », « #rgba », « #rrggbb », « #rrggbbaa » ou « rgb(…) » -> [r, g, b, a] */
    public static int[] parse(String c) {
        if (c == null) {
            return new int[] {0, 0, 0, 0};
        }
        String s = c.trim();
        int r = 0, g = 0, b = 0, a = 255;
        if (s.startsWith("#")) {
            s = s.substring(1);
            if (s.length() == 3 || s.length() == 4) {
                r = toHex("" + s.charAt(0) + s.charAt(0));
                g = toHex("" + s.charAt(1) + s.charAt(1));
                b = toHex("" + s.charAt(2) + s.charAt(2));
                if (s.length() == 4) {
                    a = toHex("" + s.charAt(3) + s.charAt(3));
                }
            } else if (s.length() >= 6) {
                r = toHex(s.substring(0, 2));
                g = toHex(s.substring(2, 4));
                b = toHex(s.substring(4, 6));
                if (s.length() == 8) {
                    a = toHex(s.substring(6, 8));
                }
            }
        } else if (s.startsWith("rgb")) {
            String[] m = s.replaceAll("[^0-9.,-]", "").split(",");
            r = intOf(m, 0);
            g = intOf(m, 1);
            b = intOf(m, 2);
            a = m.length > 3 ? (int) Math.round(number(m, 3) * 255) : 255;
        }
        return new int[] {r, g, b, a};
    }

    private static double number(String[] m, int i) {
        try {
            return i < m.length ? Double.parseDouble(m[i]) : Double.NaN;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static int intOf(String[] m, int i) {
        double v = number(m, i);
        return Double.isNaN(v) ? 0 : (int) v;
    }

    /** « #rrggbb » en minuscules */
    public static String hex(String c) {
        int[] p = parse(c);
        return "#" + Integer.toHexString((1 << 24) + (p[0] << 16) + (p[1] << 8) + p[2]).substring(1);
    }

    public static double[] rgbToHsl(double r, double g, double b) {
        r /= 255;
        g /= 255;
        b /= 255;
        double mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b)), d = mx - mn;
        double h = 0, s = 0, l = (mx + mn) / 2;
        if (d != 0) {
            s = l > 0.5 ? d / (2 - mx - mn) : d / (mx + mn);
            if (mx == r) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if (mx == g) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            h /= 6;
        }
        return new double[] {h, s, l};
    }

    private static double f(double p, double q, double t) {
        if (t < 0) {
            t += 1;
        }
        if (t > 1) {
            t -= 1;
        }
        if (t < 1.0 / 6) {
            return p + (q - p) * 6 * t;
        }
        if (t < 1.0 / 2) {
            return q;
        }
        if (t < 2.0 / 3) {
            return p + (q - p) * (2.0 / 3 - t) * 6;
        }
        return p;
    }

    public static int[] hslToRgb(double h, double s, double l) {
        if (s == 0) {
            int v = (int) Math.round(l * 255);
            return new int[] {v, v, v};
        }
        double q = l < 0.5 ? l * (1 + s) : l + s - l * s, p = 2 * l - q;
        return new int[] {
            (int) Math.round(f(p, q, h + 1.0 / 3) * 255),
            (int) Math.round(f(p, q, h) * 255),
            (int) Math.round(f(p, q, h - 1.0 / 3) * 255)
        };
    }

    /** décale la teinte (degrés), la saturation (facteur) et la luminosité (ajout) */
    public static int[] hsl(int r, int g, int b, int a, double dh, double ds, double dl) {
        double[] t = rgbToHsl(r, g, b);
        double h = (t[0] + dh / 360 + 1) % 1;
        double s = Math.max(0, Math.min(1, t[1] * ds));
        double l = Math.max(0, Math.min(1, t[2] + dl));
        int[] o = hslToRgb(h, s, l);
        return new int[] {o[0], o[1], o[2], a};
    }
}
