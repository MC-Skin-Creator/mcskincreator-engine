/* MC Skin Creator - Copyright (C) 2026 clixmods - Tous droits réservés (voir LICENSE) */
package fr.clixmods.mcsc.engine;

import java.util.List;
import java.util.Map;

/**
 * Le modèle Minecraft et le dépliage de sa texture 64×64 — portage de MODEL
 * (js/core.js). Repère : +x = gauche du personnage, +y = haut, +z = avant.
 */
public final class Model {

    private Model() { }

    public static final int SIZE = 64;

    public record Part(int w, int h, int d, int x, int[] base, int[] over, Character arm) { }

    public record Rect(int x, int y, int w, int h) { }

    public record Texel(String part, String layer, String face, int lx, int ly, Rect rect) { }

    public static final List<String> PART_IDS = List.of("head", "body", "armR", "armL", "legR", "legL");
    public static final List<String> FACES = List.of("top", "bottom", "right", "front", "left", "back");

    private static final Map<String, Part> P = Map.of(
            "head", new Part(8, 8, 8, -4, new int[] {0, 0}, new int[] {32, 0}, null),
            "body", new Part(8, 12, 4, -4, new int[] {16, 16}, new int[] {16, 32}, null),
            "armR", new Part(4, 12, 4, -8, new int[] {40, 16}, new int[] {40, 32}, 'R'),
            "armL", new Part(4, 12, 4, 4, new int[] {32, 48}, new int[] {48, 48}, 'L'),
            "legR", new Part(4, 12, 4, -4, new int[] {0, 16}, new int[] {0, 32}, null),
            "legL", new Part(4, 12, 4, 0, new int[] {16, 48}, new int[] {0, 48}, null));

    /** largeur d'une partie : le bras du modèle fin fait 3 texels */
    public static int width(String part, boolean slim) {
        Part p = P.get(part);
        return slim && p.arm() != null ? 3 : p.w();
    }

    /** le rectangle d'une face dans la texture */
    public static Rect faceRect(String part, String face, String layer, boolean slim) {
        Part p = P.get(part);
        int[] o = "over".equals(layer) ? p.over() : p.base();
        int u = o[0], v = o[1], w = width(part, slim), h = p.h(), d = p.d();
        return switch (face) {
            case "top" -> new Rect(u + d, v, w, d);
            case "bottom" -> new Rect(u + d + w, v, w, d);
            case "right" -> new Rect(u, v + d, d, h);
            case "front" -> new Rect(u + d, v + d, w, h);
            case "left" -> new Rect(u + d + w, v + d, d, h);
            case "back" -> new Rect(u + d + w + d, v + d, w, h);
            default -> throw new IllegalArgumentException("face inconnue: " + face);
        };
    }

    /** à quelle partie, quel calque et quelle face appartient un texel ; null s'il est hors du dépliage */
    public static Texel texelInfo(int px, int py, boolean slim) {
        for (String id : PART_IDS) {
            for (String layer : List.of("base", "over")) {
                for (String f : FACES) {
                    Rect r = faceRect(id, f, layer, slim);
                    if (px >= r.x() && px < r.x() + r.w() && py >= r.y() && py < r.y() + r.h()) {
                        return new Texel(id, layer, f, px - r.x(), py - r.y(), r);
                    }
                }
            }
        }
        return null;
    }

    /** le texel symétrique gauche/droite (miroir du personnage), ou null */
    public static int[] mirrorTexel(int px, int py, boolean slim) {
        Texel t = texelInfo(px, py, slim);
        if (t == null) {
            return null;
        }
        String part = switch (t.part()) {
            case "armR" -> "armL";
            case "armL" -> "armR";
            case "legR" -> "legL";
            case "legL" -> "legR";
            default -> t.part();
        };
        String face = "left".equals(t.face()) ? "right" : "right".equals(t.face()) ? "left" : t.face();
        Rect r = faceRect(part, face, t.layer(), slim);
        int lx = t.lx();
        // front/back/top/bottom : miroir horizontal ; left/right : conserver l'ordre
        if (!"left".equals(t.face()) && !"right".equals(t.face())) {
            lx = r.w() - 1 - t.lx();
        }
        return new int[] {r.x() + lx, r.y() + t.ly()};
    }
}
