package org.sprautestudio.spraute_engine.skills;

import java.util.ArrayList;
import java.util.List;

public final class BlockArea {
    private BlockArea() {
    }

    public static List<int[]> of(int x, int y, int z, double lookX, double lookY, double lookZ, int width, int height, int depth) {
        boolean alongX = Math.abs(lookX) >= Math.abs(lookZ);
        int[] ground = alongX ? new int[]{lookX >= 0.0 ? 1 : -1, 0, 0} : new int[]{0, 0, lookZ >= 0.0 ? 1 : -1};
        int[] across = alongX ? new int[]{0, 0, 1} : new int[]{1, 0, 0};
        boolean flat = Math.abs(lookY) > 0.7;
        int[] deep = flat ? new int[]{0, lookY >= 0.0 ? 1 : -1, 0} : ground;
        int[] up = flat ? ground : new int[]{0, 1, 0};
        List<int[]> out = new ArrayList<>();

        for (int d = 0; d < Math.max(1, depth); d++) {
            for (int w = -(Math.max(1, width) - 1) / 2; w <= Math.max(1, width) / 2; w++) {
                for (int h = -(Math.max(1, height) - 1) / 2; h <= Math.max(1, height) / 2; h++) {
                    if (d != 0 || w != 0 || h != 0) {
                        out.add(
                            new int[]{
                                x + deep[0] * d + across[0] * w + up[0] * h,
                                y + deep[1] * d + across[1] * w + up[1] * h,
                                z + deep[2] * d + across[2] * w + up[2] * h
                            }
                        );
                    }
                }
            }
        }

        return out;
    }
}
