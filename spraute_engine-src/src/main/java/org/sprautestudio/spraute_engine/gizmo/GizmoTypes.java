package org.sprautestudio.spraute_engine.gizmo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.CullStateShard;
import net.minecraft.client.renderer.RenderStateShard.DepthTestStateShard;
import net.minecraft.client.renderer.RenderStateShard.LayeringStateShard;
import net.minecraft.client.renderer.RenderStateShard.LineStateShard;
import net.minecraft.client.renderer.RenderStateShard.WriteMaskStateShard;
import org.lwjgl.opengl.GL11;

final class GizmoTypes extends RenderType {
    private static final LayeringStateShard FRONT_CULL = new LayeringStateShard("spraute_front_cull", () -> {
        RenderSystem.enableCull();
        GL11.glCullFace(1028);
    }, () -> GL11.glCullFace(1029));
    static final RenderType FILL = quads("spraute_fill", LEQUAL_DEPTH_TEST, COLOR_WRITE, CULL, VIEW_OFFSET_Z_LAYERING);
    static final RenderType FILL_DEPTH = quads("spraute_fill_depth", LEQUAL_DEPTH_TEST, COLOR_DEPTH_WRITE, CULL, VIEW_OFFSET_Z_LAYERING);
    static final RenderType HULL = quads("spraute_hull", LEQUAL_DEPTH_TEST, COLOR_WRITE, CULL, FRONT_CULL);
    static final RenderType SHAPE = quads("spraute_shape", LEQUAL_DEPTH_TEST, COLOR_WRITE, NO_CULL, VIEW_OFFSET_Z_LAYERING);
    static final RenderType SHAPE_THROUGH = quads("spraute_shape_through", NO_DEPTH_TEST, COLOR_WRITE, NO_CULL, NO_LAYERING);
    private static final Map<String, RenderType> LINES = new HashMap<>();

    private GizmoTypes(String name, VertexFormat format, Mode mode, int size, boolean sort, RenderStateShard... parts) {
        super(name, format, mode, size, false, sort, () -> {
            for (RenderStateShard p : parts) {
                p.setupRenderState();
            }
        }, () -> {
            for (RenderStateShard p : parts) {
                p.clearRenderState();
            }
        });
    }

    private static RenderType quads(String name, DepthTestStateShard depth, WriteMaskStateShard write, CullStateShard cull, LayeringStateShard layer) {
        return new GizmoTypes(
            name, DefaultVertexFormat.POSITION_COLOR, Mode.QUADS, 262144, true, POSITION_COLOR_SHADER, TRANSLUCENT_TRANSPARENCY, depth, cull, layer, write
        );
    }

    static RenderType lines(double width, boolean through) {
        double w = Math.max(1.0, Math.min(16.0, Math.round(width * 2.0) / 2.0));
        return LINES.computeIfAbsent(
            w + ":" + through,
            k -> new GizmoTypes(
                "spraute_lines_" + w + (through ? "_through" : ""),
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                Mode.LINES,
                65536,
                false,
                RENDERTYPE_LINES_SHADER,
                new LineStateShard(OptionalDouble.of(w)),
                VIEW_OFFSET_Z_LAYERING,
                TRANSLUCENT_TRANSPARENCY,
                through ? NO_DEPTH_TEST : LEQUAL_DEPTH_TEST,
                NO_CULL,
                COLOR_WRITE
            )
        );
    }
}
