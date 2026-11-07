package org.sprautestudio.spraute_engine.editor.camera;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.sprautestudio.spraute_engine.camera.CamProfile;
import org.sprautestudio.spraute_engine.camera.client.PlayerCam;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.ui.Box;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Checkbox;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.Label;
import org.sprautestudio.spraute_engine.ui.widgets.ScrollView;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public final class CameraTab extends EditorTab {
    private CamProfile doc;
    private boolean dirty;
    private boolean shown;
    private boolean hadHideGui;
    private String base = "";
    private final Set<String> closed = new HashSet<>(CamProfile.groups());
    private final Box props = Box.column().gap(2).pad(8, 6);
    private final ScrollView scroll = new ScrollView(this.props);
    private final CameraTab.Inspector inspector = new CameraTab.Inspector();
    private final Box toolbar = Box.row().gap(4).pad(6, 3).background(-14934490);

    public CameraTab(Project project, Project.FileRef file, String content) {
        super(project, file);
        this.doc = CamProfile.fromJson(content);
        this.focusable = true;
        this.add(this.toolbar);
        Label from = new Label("Начать с:").color(-6577995);
        from.prefW = 58;
        this.toolbar.add(from);
        Dropdown<String> preset = new Dropdown<>(CamProfile::presetNames, () -> this.base, CameraTab::presetName, v -> {
            this.base = v;
            this.doc = CamProfile.preset(v).profile().copy();
            this.changed();
            this.build();
        });
        preset.prefW = 190;
        this.toolbar.add(preset);
        Widget gap = new Widget();
        gap.flex = 1.0F;
        this.toolbar.add(gap);
        Label hint = new Label(() -> "В сюжете: camera rig \"" + this.title() + "\"").color(-9933435);
        hint.prefW = 260;
        this.toolbar.add(hint);
        this.inspector.add(this.scroll);
        this.build();
    }

    private static String presetName(String id) {
        if (id != null && !id.isEmpty()) {
            CamProfile.Preset p = CamProfile.preset(id);
            return p != null ? p.title() : id;
        } else {
            return "Готовая камера";
        }
    }

    @Override
    public void setShown(boolean want) {
        Minecraft mc = Minecraft.getInstance();
        if (want && mc.player == null) {
            want = false;
        }

        if (want != this.shown) {
            this.shown = want;
            if (want) {
                this.hadHideGui = mc.options.hideGui;
                mc.options.hideGui = true;
                PlayerCam.preview(this.doc);
            } else {
                mc.options.hideGui = this.hadHideGui;
                PlayerCam.preview(null);
            }
        }
    }

    @Override
    public boolean showsWorld() {
        return this.shown;
    }

    private void changed() {
        if (!this.dirty) {
            this.dirty = true;
            if (this.onDirtyChanged != null) {
                this.onDirtyChanged.run();
            }
        }

        if (this.shown) {
            PlayerCam.preview(this.doc);
        }
    }

    private int height() {
        int sum = 12;

        for (Widget w : this.props.children()) {
            sum += Math.max(0, w.prefH) + 2;
        }

        return sum;
    }

    @Override
    public Widget inspector() {
        return this.inspector;
    }

    private void build() {
        this.props.clear();

        for (String group : CamProfile.groups()) {
            int set = 0;
            List<CamProfile.Param> list = new ArrayList<>();

            for (CamProfile.Param p : CamProfile.params()) {
                if (p.group().equals(group)) {
                    list.add(p);
                    if (this.doc.has(p.id())) {
                        set++;
                    }
                }
            }

            Button head = new Button(group.toUpperCase() + (set > 0 ? "  ·  " + set : ""), () -> {
                if (!this.closed.remove(group)) {
                    this.closed.add(group);
                }

                this.build();
            }).icon(this.closed.contains(group) ? Icon.CHEVRON_RIGHT : Icon.CHEVRON_DOWN).style(Button.Style.GHOST);
            head.prefH = 20;
            this.props.add(head);
            if (!this.closed.contains(group)) {
                for (CamProfile.Param p : list) {
                    if (p.toggle()) {
                        Checkbox box = new Checkbox(p.title(), () -> this.doc.on(p.id()), v -> {
                            if (v) {
                                this.doc.values.put(p.id(), 1.0);
                            } else {
                                this.doc.values.remove(p.id());
                            }

                            this.changed();
                        });
                        box.prefH = 18;
                        if (!p.doc().isEmpty()) {
                            box.tooltip(p.doc());
                        }

                        this.props.add(box);
                    } else if (p.free()) {
                        this.props.add(this.freeField(p));
                    } else {
                        this.props.add(new CameraTab.Slider(p));
                    }
                }
            }
        }

        this.props.prefH = this.height();
        UI ui = UI.get();
        if (ui != null) {
            ui.relayout();
        }
    }

    private Widget freeField(CamProfile.Param p) {
        Box row = Box.row().gap(6);
        row.prefH = 20;
        Label l = new Label(p.title()).color(-6577995);
        l.prefW = 110;
        row.add(l);
        TextField f = new TextField().placeholder("там, где была");
        f.setText(this.doc.has(p.id()) ? fmt(this.doc.num(p.id())) : "");
        f.flex = 1.0F;
        f.onChange = text -> {
            try {
                if (text.isBlank()) {
                    this.doc.values.remove(p.id());
                } else {
                    this.doc.values.put(p.id(), Double.parseDouble(text.trim().replace(',', '.')));
                }

                this.changed();
            } catch (NumberFormatException var4x) {
            }
        };
        row.add(f);
        if (!p.doc().isEmpty()) {
            row.tooltip(p.doc());
        }

        return row;
    }

    private static String fmt(double v) {
        double r = Math.round(v * 100.0) / 100.0;
        return r == Math.rint(r) ? String.valueOf((long)r) : String.valueOf(r);
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        return this.shown && button == 0;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (this.shown && p != null) {
            float yaw = p.getYRot() + (float)dx * 0.35F;
            float pitch = Mth.clamp(p.getXRot() + (float)dy * 0.35F, -90.0F, 90.0F);
            p.setYRot(yaw);
            p.setXRot(pitch);
            p.yRotO = yaw;
            p.xRotO = pitch;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return this.shown;
    }

    @Override
    public void layout() {
        this.toolbar.bounds(this.x, this.y, this.w, 26);
        this.toolbar.layout();
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        if (!this.shown) {
            c.fill(this.x, this.y, this.w, this.h, -15461093);
            c.textCentered("Войдите в мир, чтобы видеть камеру вживую", this.x + this.w / 2, this.y + this.h / 2, -6577995);
        }
    }

    @Override
    public Icon icon() {
        return Icon.FILM;
    }

    @Override
    public int iconColor() {
        return -9706552;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void save() throws IOException {
        this.project.write(this.file, this.doc.toJson());
        this.dirty = false;
        if (this.onDirtyChanged != null) {
            this.onDirtyChanged.run();
        }
    }

    @Override
    public String source() {
        return null;
    }

    @Override
    public int scriptId() {
        return -1;
    }

    @Override
    public List<EditorTab.Problem> problems() {
        return List.of();
    }

    @Override
    public Analyzer.Result analysis() {
        return null;
    }

    @Override
    public void reveal(int offset) {
    }

    @Override
    public void focusContent() {
        this.requestFocus();
    }

    private final class Inspector extends Widget {
        @Override
        public void layout() {
            CameraTab.this.scroll.bounds(this.x, this.y, this.w, this.h);
            CameraTab.this.props.prefH = CameraTab.this.height();
            CameraTab.this.scroll.layout();
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, this.h, -14934490);
        }
    }

    private final class Slider extends Widget {
        final CamProfile.Param p;
        boolean dragging;

        Slider(CamProfile.Param p) {
            this.p = p;
            this.prefH = 28;
            if (!p.doc().isEmpty()) {
                this.tooltip(p.doc());
            }
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            double v = CameraTab.this.doc.num(this.p.id());
            boolean set = CameraTab.this.doc.has(this.p.id());
            c.textEllipsized(this.p.title(), this.x, this.y + 2, this.w - 46, set ? -1841682 : -6577995);
            String text = CameraTab.fmt(v);
            c.text(text, this.x + this.w - c.width(text), this.y + 2, set ? -7639809 : -9933435);
            int ty = this.y + 16;
            c.round(this.x, ty, this.w, 4, 2, -15592678);
            double k = (v - this.p.min()) / (this.p.max() - this.p.min());
            double k0 = (this.p.def() - this.p.min()) / (this.p.max() - this.p.min());
            int from = this.x + (int)(Math.min(k, k0) * this.w);
            int to = this.x + (int)(Math.max(k, k0) * this.w);
            if (to > from) {
                c.fill(from, ty, to - from, 4, set ? -7639809 : -12762793);
            }

            c.round(this.x + (int)(k * this.w) - 4, ty - 3, 8, 10, 3, !this.dragging && !this.isHovered() ? -6577995 : -1);
        }

        private void put(double mx) {
            double k = Mth.clamp((mx - this.x) / Math.max(1, this.w), 0.0, 1.0);
            double v = this.p.min() + (this.p.max() - this.p.min()) * k;
            double step = this.p.max() - this.p.min() > 20.0 ? 0.5 : 0.01;
            v = Math.round(v / step) * step;
            if (Math.abs(v - this.p.def()) < (this.p.max() - this.p.min()) * 0.01) {
                CameraTab.this.doc.values.remove(this.p.id());
            } else {
                CameraTab.this.doc.values.put(this.p.id(), v);
            }

            CameraTab.this.changed();
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            if (button != 0) {
                return false;
            } else {
                this.dragging = true;
                this.put(mx);
                return true;
            }
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (this.dragging) {
                this.put(mx);
            }

            return this.dragging;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            boolean was = this.dragging;
            this.dragging = false;
            if (was) {
                CameraTab.this.build();
            }

            return was;
        }

        @Override
        public boolean mouseDoubleClicked(double mx, double my, int button) {
            CameraTab.this.doc.values.remove(this.p.id());
            CameraTab.this.changed();
            return true;
        }
    }
}
