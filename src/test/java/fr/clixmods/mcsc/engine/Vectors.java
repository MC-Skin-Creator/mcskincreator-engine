/* MC Skin Creator - Copyright (C) 2026 clixmods - All rights reserved (see LICENSE) */
package fr.clixmods.mcsc.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * The frozen parity vectors, and the synthetic inputs they were produced from.
 *
 * <p>golden-vectors.txt comes out of js/core.js and js/assets.js - the
 * reference engine - through site/scripts/engine-vectors.js in the private
 * MCSkinCreator repository. It cannot be regenerated here: this repository has
 * no catalogue, and deliberately so.
 *
 * <p>The inputs are synthetic, never catalogue items. {@link #synth(int)} is
 * integer arithmetic on purpose, so that JavaScript and Java build the same
 * buffer without either rounding anything.
 */
final class Vectors {

    private Vectors() { }

    private static final List<String[]> ROWS = read();

    private static List<String[]> read() {
        List<String[]> rows = new ArrayList<>();
        try (InputStream in = Vectors.class.getResourceAsStream("/golden-vectors.txt")) {
            if (in == null) {
                throw new IllegalStateException("golden-vectors.txt is missing from the test resources");
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) {
                String s = line.strip();
                if (!s.isEmpty() && !s.startsWith("#")) {
                    rows.add(s.split(" "));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return List.copyOf(rows);
    }

    /** every vector of that kind, its leading type word dropped */
    static List<String[]> of(String type) {
        List<String[]> out = new ArrayList<>();
        for (String[] row : ROWS) {
            if (row[0].equals(type)) {
                String[] tail = new String[row.length - 1];
                System.arraycopy(row, 1, tail, 0, tail.length);
                out.add(tail);
            }
        }
        if (out.isEmpty()) {
            throw new IllegalStateException("no vector of kind " + type);
        }
        return out;
    }

    /** an empty field is written "-", because a blank would break the split */
    static String field(String s) {
        return "-".equals(s) ? "" : s;
    }

    /** a deterministic 64x64 RGBA buffer; the generator computes the very same one */
    static int[] synth(int seed) {
        int[] b = new int[Composition.BYTES];
        for (int p = 0; p < Model.SIZE * Model.SIZE; p++) {
            int i = p * 4;
            b[i] = (p * 37 + seed * 11) % 256;
            b[i + 1] = (p * 91 + 13 + seed * 29) % 256;
            b[i + 2] = (p * 17 + 200 + seed * 7) % 256;
            b[i + 3] = (p % 11 == 0) ? 0 : ((p % 5 == 0) ? 128 : 255);
        }
        return b;
    }

    static String sha(int[] buffer) {
        byte[] bytes = new byte[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            bytes[i] = (byte) buffer[i];
        }
        return sha(bytes);
    }

    static String sha(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** the synthetic items, rebuilt from the "preset" vectors */
    static Preset preset(String id) {
        for (String[] v : of("preset")) {
            if (v[0].equals(id)) {
                String[] names = v[2].split(",");
                String[] defaults = v[3].split(",");
                List<ColorKey> keys = new ArrayList<>();
                for (int i = 0; i < names.length; i++) {
                    keys.add(new ColorKey(names[i], defaults[i]));
                }
                return new Preset("syn", id, keys, Boolean.parseBoolean(v[1]),
                        field(v[4]).isEmpty() ? null : v[4],
                        field(v[5]).isEmpty() ? null : v[5]);
            }
        }
        throw new IllegalStateException("unknown synthetic preset: " + id);
    }
}
