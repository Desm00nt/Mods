package org.sprautestudio.spraute_engine.editor.assets;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.imageio.ImageIO;

public final class FaceSkin {
    private static final int HOLE_Y = 12;
    private static final int HOLE_H = 2;
    private static final int[] LEFT_HOLE = new int[]{9, 10};
    private static final int[] RIGHT_HOLE = new int[]{13, 14};
    private static final int HAT_DX = 32;

    private FaceSkin() {
    }

    public static boolean isSkin(BufferedImage img) {
        return img.getWidth() == 64 && (img.getHeight() == 64 || img.getHeight() == 32) || img.getWidth() == 128 && img.getHeight() == 128;
    }

    public static BufferedImage convert(BufferedImage src) {
        return convert(src, new FaceSkin.Options());
    }

    public static int[] findEyes(BufferedImage src, FaceSkin.Options o) {
        int skin = skinColor(src);
        int h = o.eyeH;
        int off = o.offset;
        if (h <= 0 || off < 0) {
            int bottom = -1;
            int top = -1;

            for (int y = 15; y >= 9; y--) {
                boolean white = whitish(src.getRGB(9, y), skin) || whitish(src.getRGB(14, y), skin);
                if (white && bottom < 0) {
                    bottom = y;
                }

                if (white) {
                    top = y;
                } else if (bottom >= 0) {
                    break;
                }
            }

            if (bottom < 0) {
                for (int y = 15; y >= 10; y--) {
                    boolean eye = false;

                    for (int x : new int[]{9, 10, 13, 14}) {
                        int c = src.getRGB(x, y);
                        if (c >>> 24 > 0 && far(c, skin)) {
                            eye = true;
                        }
                    }

                    if (eye && bottom < 0) {
                        bottom = y;
                    }

                    if (eye) {
                        top = y;
                    } else if (bottom >= 0) {
                        break;
                    }
                }
            }

            if (bottom < 0) {
                bottom = 13;
                top = 12;
            }

            if (off < 0) {
                off = 15 - bottom;
            }

            if (h <= 0) {
                h = Math.min(2, bottom - top + 1);
            }
        }

        h = Math.max(1, Math.min(2, h));
        int bottomRow = 15 - Math.max(0, Math.min(5, off));
        return new int[]{bottomRow - h + 1, h};
    }

    private static void put(BufferedImage out, int x, int y, int w, int h, int color) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                out.setRGB(x + dx, y + dy, color);
            }
        }
    }

    private static boolean transparent(BufferedImage img, int x, int y) {
        return img.getRGB(x, y) >>> 24 == 0;
    }

    private static boolean alreadyConverted(BufferedImage src) {
        if (src.getHeight() != 64) {
            return false;
        } else {
            for (int y = 12; y < 14; y++) {
                for (int x : LEFT_HOLE) {
                    if (!transparent(src, x, y)) {
                        return false;
                    }
                }

                for (int xx : RIGHT_HOLE) {
                    if (!transparent(src, xx, y)) {
                        return false;
                    }
                }
            }

            return !transparent(src, 6, 4) && !transparent(src, 2, 4) && !transparent(src, 5, 4);
        }
    }

    private static void cutHole(BufferedImage out, int[] columns) {
        for (int y = 12; y < 14; y++) {
            for (int x : columns) {
                out.setRGB(x, y, 0);
                out.setRGB(x + 32, y, 0);
            }
        }
    }

    public static BufferedImage convert(BufferedImage src, FaceSkin.Options o) {
        if (o.target == FaceSkin.Target.SUPER_RIG) {
            return SuperRigSkin.convert(src, o);
        } else {
            BufferedImage out = new BufferedImage(64, 64, 2);
            int h = Math.min(64, src.getHeight());

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < 64; x++) {
                    out.setRGB(x, y, src.getRGB(x, y));
                }
            }

            if (o.cutEyes && alreadyConverted(src)) {
                return out;
            } else {
                if (src.getHeight() == 32) {
                    copy(out, 0, 16, 16, 16, 16, 48);
                    copy(out, 40, 16, 16, 16, 32, 48);
                }

                int[] eyes = findEyes(src, o);
                int eyeY = eyes[0];
                int eyeH = eyes[1];
                int lOut = 9;
                int lIn = 10;
                int rIn = 13;
                int rOut = 14;
                boolean leftMoves = o.still != 1;
                boolean rightMoves = o.still != 2;
                int skin = skinColor(src);
                int bottom = eyeY + eyeH - 1;
                put(out, 0, 0, 8, 8, 0);
                int white = 0;

                for (int y = bottom; y >= eyeY && white == 0; y--) {
                    int left = src.getRGB(lOut, y);
                    int right = src.getRGB(rOut, y);
                    if (leftMoves && whitish(left, skin)) {
                        white = opaque(left);
                    } else if (rightMoves && whitish(right, skin)) {
                        white = opaque(right);
                    }
                }

                if (white == 0) {
                    int left = src.getRGB(lOut, bottom);
                    int right = src.getRGB(rOut, bottom);
                    int chosen = (!leftMoves || left >>> 24 == 0) && rightMoves ? right : left;
                    white = chosen >>> 24 == 0 ? -1 : opaque(chosen);
                }

                put(out, 6, 4, 2, 2, white);

                for (int dy = 0; dy < 2; dy++) {
                    int sy = eyeY + Math.min(dy, eyeH - 1);
                    int r = src.getRGB(rightMoves ? rIn : lIn, sy);
                    int l = src.getRGB(leftMoves ? lIn : rIn, sy);
                    out.setRGB(2, 4 + dy, r >>> 24 == 0 ? darker(skin) : opaque(r));
                    out.setRGB(5, 4 + dy, l >>> 24 == 0 ? darker(skin) : opaque(l));
                }

                put(out, 0, 3, 2, 1, skin);
                put(out, 6, 3, 2, 1, skin);
                put(out, 0, 6, 2, 1, skin);
                put(out, 6, 6, 2, 1, skin);
                int browY = eyeY - 1;
                int browColor = 0;

                for (int i = 0; i < 6; i++) {
                    int c = src.getRGB(lOut + i, browY);
                    if (c >>> 24 > 0 && far(c, skin) && browColor == 0) {
                        browColor = opaque(c);
                    }
                }

                if (browColor == 0) {
                    browColor = darker(skin);
                }

                if (o.brows != FaceSkin.Brows.NONE) {
                    put(out, 6, 2, 2, 1, browColor);
                    put(out, 0, 2, 2, 1, browColor);
                }

                if (o.eraseFace) {
                    for (int x = lOut; x <= rOut; x++) {
                        if (o.brows != FaceSkin.Brows.NONE && browY >= 8 && far(src.getRGB(x, browY), skin)) {
                            boolean overStill = o.still == 1 && x <= lIn || o.still == 2 && x >= rIn;
                            if (!overStill) {
                                out.setRGB(x, browY, skin);
                            }
                        }

                        boolean eyeCol = leftMoves && (x == lOut || x == lIn) || rightMoves && (x == rIn || x == rOut);
                        if (eyeCol) {
                            for (int dy = 0; dy < eyeH; dy++) {
                                out.setRGB(x, eyeY + dy, skin);
                            }
                        }
                    }

                    if (o.cutEyes && leftMoves) {
                        cutHole(out, LEFT_HOLE);
                    }

                    if (o.cutEyes && rightMoves) {
                        cutHole(out, RIGHT_HOLE);
                    }
                }

                return out;
            }
        }
    }

    public static Path convertFile(Path source, Path targetDir, String name) throws IOException {
        return convertFile(source, targetDir, name, new FaceSkin.Options());
    }

    public static BufferedImage read(Path source) throws IOException {
        BufferedImage img = ImageIO.read(source.toFile());
        if (img == null) {
            throw new IOException("Не картинка");
        } else if (!isSkin(img)) {
            throw new IOException("Нужен скин Minecraft 64×64, 64×32 или Super Rig 128×128, а тут " + img.getWidth() + "×" + img.getHeight());
        } else {
            return img;
        }
    }

    public static Path convertFile(Path source, Path targetDir, String name, FaceSkin.Options o) throws IOException {
        BufferedImage img = ImageIO.read(source.toFile());
        if (img == null) {
            throw new IOException("Не картинка");
        } else if (!isSkin(img)) {
            throw new IOException("Нужен скин Minecraft 64×64, 64×32 или Super Rig 128×128, а тут " + img.getWidth() + "×" + img.getHeight());
        } else {
            Files.createDirectories(targetDir);
            Path out = targetDir.resolve(name + ".png");
            int n = 2;

            while (Files.exists(out)) {
                out = targetDir.resolve(name + " " + n++ + ".png");
            }

            ImageIO.write(convert(img, o), "png", out.toFile());
            return out;
        }
    }

    static int skinColor(BufferedImage src) {
        Map<Integer, Integer> count = new HashMap<>();

        for (int y = 14; y < 16; y++) {
            for (int x = 8; x < 16; x++) {
                int c = src.getRGB(x, y);
                if (c >>> 24 > 0) {
                    count.merge(c | 0xFF000000, 1, Integer::sum);
                }
            }
        }

        for (int xx = 8; xx < 16; xx++) {
            int c = src.getRGB(xx, 11);
            if (c >>> 24 > 0) {
                count.merge(c | 0xFF000000, 1, Integer::sum);
            }
        }

        return count.entrySet().stream().max(Entry.comparingByValue()).map(Entry::getKey).orElse(-4421777);
    }

    private static boolean whitish(int c, int skin) {
        if (c >>> 24 != 0 && far(c, skin)) {
            int r = c >> 16 & 0xFF;
            int g = c >> 8 & 0xFF;
            int b = c & 0xFF;
            return Math.min(r, Math.min(g, b)) > 140 && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) < 45;
        } else {
            return false;
        }
    }

    private static boolean far(int a, int b) {
        int dr = (a >> 16 & 0xFF) - (b >> 16 & 0xFF);
        int dg = (a >> 8 & 0xFF) - (b >> 8 & 0xFF);
        int db = (a & 0xFF) - (b & 0xFF);
        return dr * dr + dg * dg + db * db > 1600;
    }

    private static int darker(int c) {
        int r = (int)((c >> 16 & 0xFF) * 0.35);
        int g = (int)((c >> 8 & 0xFF) * 0.3);
        int b = (int)((c & 0xFF) * 0.3);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int opaque(int c) {
        return c | 0xFF000000;
    }

    private static void copy(BufferedImage img, int sx, int sy, int w, int h, int dx, int dy) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(dx + x, dy + y, img.getRGB(sx + x, sy + y));
            }
        }
    }

    public static void main(String[] a) throws IOException {
        FaceSkin.Options o = new FaceSkin.Options();
        if (a.length > 2) {
            o.eyeH = Integer.parseInt(a[2]);
        }

        if (a.length > 3) {
            o.offset = Integer.parseInt(a[3]);
        }

        if (a.length > 4) {
            o.still = Integer.parseInt(a[4]);
        }

        if (a.length > 5) {
            o.brows = FaceSkin.Brows.valueOf(a[5]);
        }

        ImageIO.write(convert(ImageIO.read(Path.of(a[0]).toFile()), o), "png", Path.of(a[1]).toFile());
    }

    public static enum Brows {
        THICK("толстые 3×1"),
        THIN("тонкие 2×1"),
        ARROW("стрелками"),
        NONE("без бровей");

        public final String title;

        private Brows(String title) {
            this.title = title;
        }
    }

    public static final class Options {
        public FaceSkin.Target target = FaceSkin.Target.SPRAUTE;
        public int eyeH = 0;
        public int offset = -1;
        public int still = 0;
        public FaceSkin.Brows brows = FaceSkin.Brows.THICK;
        public boolean eraseFace = true;
        public boolean cutEyes = true;

        public FaceSkin.Options copy() {
            FaceSkin.Options o = new FaceSkin.Options();
            o.target = this.target;
            o.eyeH = this.eyeH;
            o.offset = this.offset;
            o.still = this.still;
            o.brows = this.brows;
            o.eraseFace = this.eraseFace;
            o.cutEyes = this.cutEyes;
            return o;
        }
    }

    public static enum Target {
        SPRAUTE("Spraute 64×64"),
        SUPER_RIG("Super Rig 128×128");

        public final String title;

        private Target(String title) {
            this.title = title;
        }
    }
}
