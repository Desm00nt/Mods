package org.sprautestudio.spraute_engine.core.client;

public final class LockIcon {
    public static final int SHADE = -1340598248;
    private static final int METAL = -2565928;
    private static final int BODY = -5207492;

    private LockIcon() {
    }

    public static void draw(LockIcon.Painter g, int x, int y, int size) {
        g.rect(x, y, size, size, -1340598248);
        int u = Math.max(1, size / 16);
        int w = 8 * u;
        int h = 6 * u;
        int shackle = 3 * u;
        int bx = x + (size - w) / 2;
        int by = y + (size - h - shackle) / 2 + shackle;
        g.rect(bx + u, by - shackle, u, shackle, -2565928);
        g.rect(bx + w - 2 * u, by - shackle, u, shackle, -2565928);
        g.rect(bx + u, by - shackle - u, w - 2 * u, u, -2565928);
        g.rect(bx, by, w, h, -5207492);
        g.rect(bx + w / 2 - u / 2, by + 2 * u, Math.max(1, u), 2 * u, -12965360);
    }

    public interface Painter {
        void rect(int var1, int var2, int var3, int var4, int var5);
    }
}
