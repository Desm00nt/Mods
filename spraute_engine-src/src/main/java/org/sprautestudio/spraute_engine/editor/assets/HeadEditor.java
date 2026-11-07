package org.sprautestudio.spraute_engine.editor.assets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.engine.client.ChatHeads;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Dialog;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public final class HeadEditor {
    private HeadEditor() {
    }

    public static void open(String entityType, ChatHeads.Params start, Consumer<ChatHeads.Params> onApply) {
        UI ui = UI.get();
        if (ui != null) {
            String shown = entityType == null ? "" : entityType.replace("\"", "").trim();
            Dialog d = new Dialog("Голова в чате" + (shown.isEmpty() ? "" : " — " + shown), Icon.USER).width(Math.min(560, ui.vw - 30));
            HeadEditor.Pane pane = new HeadEditor.Pane(entityType, start);
            pane.prefH = Math.min(300, ui.vh - 130);
            d.body.add(pane);
            d.addButton("Сбросить", Button.Style.GHOST, pane::reset);
            d.addButton("Отмена", Button.Style.DEFAULT, d::close);
            d.addButton("Применить", Button.Style.PRIMARY, () -> {
                d.close();
                onApply.accept(pane.params());
            });
            d.open();
        }
    }

    private static final class Pane extends Widget {
        final LivingEntity entity;
        final String typeId;
        float scale;
        int dx;
        int dy;
        int yaw;
        String bone;
        final Dropdown<String> boneBox;
        final TextField scaleF;
        final TextField dxF;
        final TextField dyF;
        final TextField yawF;
        boolean dragging;
        double dragX;
        double dragY;
        int startDx;
        int startDy;
        int fx;
        int fy;
        int fs;

        Pane(String typeId, ChatHeads.Params p) {
            this.typeId = typeId == null ? "" : typeId.replace("\"", "").trim();
            ResourceLocation rl = ResourceLocation.tryParse(this.typeId.contains(":") ? this.typeId : "minecraft:" + this.typeId);
            this.entity = this.typeId.startsWith("spraute:")
                ? ChatHeads.ofSprauteType(this.typeId)
                : (rl == null ? null : ChatHeads.ofType(Regs.ENTITY_TYPE.getOptional(rl).orElse(null)));
            this.set(p);
            List<String> bones = new ArrayList<>();
            bones.add("auto");
            bones.add("all");
            if (this.entity != null) {
                bones.addAll(ChatHeads.bones(this.entity));
            }

            this.boneBox = this.add(
                new Dropdown<>(() -> bones, () -> this.bone, b -> b.equals("auto") ? "авто (голова)" : (b.equals("all") ? "вся модель" : b), b -> this.bone = b)
            );
            this.scaleF = this.add(this.field(() -> fmt(this.scale), v -> this.scale = (float)Math.max(0.2, Math.min(8.0, v))));
            this.dxF = this.add(this.field(() -> String.valueOf(this.dx), v -> this.dx = (int)Math.round(v)));
            this.dyF = this.add(this.field(() -> String.valueOf(this.dy), v -> this.dy = (int)Math.round(v)));
            this.yawF = this.add(this.field(() -> String.valueOf(this.yaw), v -> this.yaw = (int)Math.round(v)));
        }

        void set(ChatHeads.Params p) {
            this.scale = p.scale();
            this.dx = p.dx();
            this.dy = p.dy();
            this.yaw = p.yaw();
            this.bone = p.bone() != null && !p.bone().isEmpty() ? p.bone() : "auto";
        }

        void reset() {
            this.set(ChatHeads.Params.DEFAULT);
        }

        ChatHeads.Params params() {
            return new ChatHeads.Params(Math.round(this.scale * 100.0F) / 100.0F, this.dx, this.dy, this.yaw, this.bone);
        }

        static String fmt(float v) {
            return v == (int)v ? String.valueOf((int)v) : String.valueOf(Math.round(v * 100.0F) / 100.0);
        }

        TextField field(final Supplier<String> get, DoubleConsumer set) {
            TextField f = new TextField() {
                @Override
                public void tick() {
                    super.tick();
                    if (!this.isFocused() && !get.get().equals(this.text())) {
                        this.setText(get.get());
                    }
                }
            };
            f.setText(get.get());
            f.filter = ch -> Character.isDigit(ch) || ch == '-' || ch == '.';
            f.onChange = s -> {
                try {
                    set.accept(Double.parseDouble(s.trim()));
                } catch (NumberFormatException var3x) {
                }
            };
            return f;
        }

        @Override
        public void layout() {
            int cx = this.x + this.w - 200;
            int[] ys = new int[]{this.y + 22, this.y + 66, this.y + 104, this.y + 142, this.y + 180};
            this.boneBox.bounds(cx + 70, ys[0], 128, 18);
            this.scaleF.bounds(cx + 70, ys[1], 128, 18);
            this.dxF.bounds(cx + 70, ys[2], 128, 18);
            this.dyF.bounds(cx + 70, ys[3], 128, 18);
            this.yawF.bounds(cx + 70, ys[4], 128, 18);
            super.layout();
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            int cx = this.x + this.w - 200;
            c.text("Кость", cx, this.y + 27, -6577995);
            c.text("Что видно в рамке", cx, this.y + 8, -9933435);
            c.text("Размер", cx, this.y + 71, -6577995);
            c.text("Сдвиг X, %", cx, this.y + 109, -6577995);
            c.text("Сдвиг Y, %", cx, this.y + 147, -6577995);
            c.text("Поворот, °", cx, this.y + 185, -6577995);
            c.textEllipsized("Тяните голову мышью, колёсико — размер", cx, this.y + 214, 198, -9933435);
            c.textEllipsized(this.typeId, cx, this.y + 228, 198, -9933435);
            int pw = this.w - 214;
            int ph = Math.min(this.h - 10, 150);
            int bx = this.x;
            int by = this.y + 4;
            c.fill(bx, by, pw, ph, -2013265920);
            this.fs = Math.min(ph - 24, 104);
            this.fx = bx + 12;
            this.fy = by + (ph - this.fs) / 2;
            c.fill(this.fx, this.fy, this.fs, this.fs, 872415231);
            c.text("Алиса:", this.fx + this.fs + 12, this.fy + 6, -13461);
            c.text("Ты наконец проснулся?", this.fx + this.fs + 12, this.fy + 20, -1);
            c.text("Пойдём, покажу дорогу.", this.fx + this.fs + 12, this.fy + 32, -1);
            this.drawHead(c, this.fx, this.fy, this.fs);
            c.outline(this.fx - 1, this.fy - 1, this.fs + 2, this.fs + 2, this.dragging ? -7639809 : 1728053247);
            int sy = by + ph + 10;
            c.text("В игре:", bx, sy + 6, -9933435);
            c.fill(bx + 44, sy, 80, 22, -2013265920);
            this.drawHead(c, bx + 46, sy + 1, 20);
            c.text("Алиса: …", bx + 70, sy + 7, -1);
            if (this.entity == null) {
                c.textCentered(
                    Minecraft.getInstance().level == null ? "войдите в мир" : "нет модели " + this.typeId,
                    this.fx + this.fs / 2,
                    this.fy + this.fs / 2 - 4,
                    -1023878
                );
            }
        }

        private void drawHead(Canvas c, int x0, int y0, int size) {
            if (this.entity != null) {
                c.flush();
                Gfx g = c.g;
                int[] r = ChatHeads.screenRect(g, x0, y0, size, size);
                g.enableScissor(r[0], r[1], r[2], r[3]);

                try {
                    ChatHeads.render(g, this.entity, x0, y0, size, size, this.params());
                } finally {
                    g.disableScissor();
                }
            }
        }

        boolean inFrame(double mx, double my) {
            return mx >= this.fx && my >= this.fy && mx < this.fx + this.fs && my < this.fy + this.fs;
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            if (super.mousePressed(mx, my, button)) {
                return true;
            } else if (button == 0 && this.inFrame(mx, my)) {
                this.dragging = true;
                this.dragX = mx;
                this.dragY = my;
                this.startDx = this.dx;
                this.startDy = this.dy;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
            if (!this.dragging) {
                return super.mouseDragged(mx, my, button, ddx, ddy);
            } else {
                this.dx = this.startDx + (int)Math.round((mx - this.dragX) * 100.0 / Math.max(1, this.fs));
                this.dy = this.startDy + (int)Math.round((my - this.dragY) * 100.0 / Math.max(1, this.fs));
                return true;
            }
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            this.dragging = false;
            return super.mouseReleased(mx, my, button);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            if (!this.inFrame(mx, my)) {
                return super.mouseScrolled(mx, my, delta);
            } else {
                this.scale = (float)Math.max(0.2, Math.min(8.0, this.scale * (delta > 0.0 ? 1.08 : 0.9259259259259258)));
                return true;
            }
        }
    }
}
