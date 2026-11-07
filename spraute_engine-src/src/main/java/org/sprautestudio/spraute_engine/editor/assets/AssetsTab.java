package org.sprautestudio.spraute_engine.editor.assets;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.Specs;
import org.sprautestudio.spraute_engine.runtime.Anims;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Checkbox;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.ListView;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class AssetsTab extends EditorTab {
    private static final int TOP = 30;
    private AssetsTab.Mode mode = AssetsTab.Mode.MODELS;
    private final Consumer<String> insertCode;
    private final Supplier<String> characterName;
    private final Button modelsBtn;
    private final Button animsBtn;
    private final Button soundsBtn;
    private final TextField modelSearch = new TextField().placeholder("Модель: название или ID").icon(Icon.SEARCH);
    private boolean onlySpraute = true;
    private final Checkbox onlySt;
    private final ListView<AssetIndex.Model> modelList;
    private final ModelView view = new ModelView();
    private final Button resetView;
    private final Button spin;
    private final Button stopAnims;
    private final Button copyModel;
    private final Button insertCharacter;
    private final TextField animSearch = new TextField().placeholder("Анимация: имя, файл или ID").icon(Icon.SEARCH);
    private final ListView<Anims.Entry> animList;
    private String layer = "action";
    private boolean loop;
    private float speed = 1.0F;
    private final Dropdown<String> layerPick;
    private final Dropdown<Float> speedPick;
    private final Checkbox loopBox;
    private final Button insertAnim;
    private final Button copyAnim;
    private final TextField soundSearch = new TextField().placeholder("Поиск звука").icon(Icon.SEARCH);
    private final ListView<AssetIndex.Sound> soundList;
    private final Button playSound;
    private final Button insertSound;
    private final Button copySound;
    private String status = "";
    private int statusColor = -6577995;

    public AssetsTab(Consumer<String> insertCode, Supplier<String> characterName) {
        super(null, null);
        this.insertCode = insertCode;
        this.characterName = characterName;
        this.modelsBtn = this.add(
            new Button("Модели", () -> this.setMode(AssetsTab.Mode.MODELS))
                .icon(Icon.CUBE)
                .toggle(() -> this.mode == AssetsTab.Mode.MODELS)
                .style(Button.Style.GHOST)
        );
        this.animsBtn = this.add(
            new Button("Анимации", () -> this.setMode(AssetsTab.Mode.ANIMATIONS))
                .icon(Icon.FILM)
                .toggle(() -> this.mode == AssetsTab.Mode.ANIMATIONS)
                .style(Button.Style.GHOST)
        );
        this.soundsBtn = this.add(
            new Button("Звуки", () -> this.setMode(AssetsTab.Mode.SOUNDS))
                .icon(Icon.NOTE)
                .toggle(() -> this.mode == AssetsTab.Mode.SOUNDS)
                .style(Button.Style.GHOST)
        );

        for (Button b : List.of(this.modelsBtn, this.animsBtn, this.soundsBtn)) {
            b.autoWidth();
        }

        this.add(this.modelSearch);
        this.modelSearch.onChange = s -> this.refreshModels();
        this.onlySt = this.add(new Checkbox("Только Spraute", () -> this.onlySpraute, v -> {
            this.onlySpraute = v;
            this.refreshModels();
        }));
        this.modelList = this.add(new ListView<>((c, m, x, y, w, h, sel, hov) -> {
            c.icon(Icon.CUBE, x + 6, y + (h - 9) / 2, m.builtin() ? -12988235 : -9933435);
            c.textEllipsized(m.title(), x + 20, y + 3, w - 26, sel ? -1841682 : -6577995);
            c.textEllipsized(m.idString(), x + 20, y + 13, w - 26, -9933435);
        }));
        this.modelList.rowHeight = 24;
        this.modelList.background = -14934490;
        this.modelList.emptyText = "Ничего не найдено";
        this.modelList.onSelect = m -> {
            this.view.setType(m.type());
            this.setStatus(m.idString(), -6577995);
        };
        this.modelList.onActivate = m -> this.insertCharacter(m);
        this.modelList.onContext = (m, mx, my) -> {
            PopupMenu pm = new PopupMenu();
            pm.item("Вставить персонажа в скрипт", Icon.USER, null, () -> this.insertCharacter(m)).color(-11544898);
            pm.item("Копировать id", Icon.COPY, null, () -> this.copy(m.idString()));
            pm.show(mx, my);
        };
        this.add(this.view);
        this.resetView = this.add(Button.icon(Icon.REFRESH, "Сбросить вид (двойной клик по модели)", this.view::resetView));
        this.spin = this.add(Button.icon(Icon.PLAY, "Вращать модель", () -> this.view.setAutoSpin(!this.view.autoSpin())).toggle(this.view::autoSpin));
        this.stopAnims = this.add(Button.icon(Icon.STOP, "Остановить анимации на превью", () -> {
            this.view.stopAll();
            this.setStatus("Анимации остановлены", -6577995);
        }).iconColor(-1023878));
        this.copyModel = this.add(Button.icon(Icon.COPY, "Копировать id модели", () -> {
            AssetIndex.Model m = this.modelList.selectedItem();
            if (m != null) {
                this.copy(m.idString());
            }
        }));
        this.insertCharacter = this.add(new Button("Персонаж в скрипт", () -> {
            AssetIndex.Model m = this.modelList.selectedItem();
            if (m != null) {
                this.insertCharacter(m);
            }
        }).icon(Icon.USER).style(Button.Style.PRIMARY).enabledWhen(() -> this.modelList.selectedItem() != null));
        this.insertCharacter.autoWidth();
        this.add(this.animSearch);
        this.animSearch.onChange = s -> this.refreshAnims();
        this.animList = this.add(new ListView<>((c, a, x, y, w, h, sel, hov) -> {
            boolean now = a.id().equals(this.view.playing());
            c.icon(now ? Icon.PLAY : Icon.FILM, x + 6, y + (h - 9) / 2, now ? -11746422 : (a.loops() ? -12988235 : -28838));
            String len = String.format(Locale.ROOT, "%.1fс", a.seconds()) + (a.loops() ? " ↻" : "");
            int lw = c.width(len);
            c.textEllipsized(a.fileName() + (a.id().contains("!") ? " · " + a.key() : ""), x + 20, y + 3, w - 30 - lw, sel ? -1841682 : -6577995);
            c.text(len, x + w - lw - 6, y + 3, -9933435);
            c.textEllipsized(a.folder(), x + 20, y + 13, w - 26, -9933435);
        }));
        this.animList.rowHeight = 24;
        this.animList.background = -14934490;
        this.animList.emptyText = "Анимации не найдены (войдите в мир)";
        this.animList.activateOnSingleClick = true;
        this.animList.onActivate = this::preview;
        this.animList.onContext = (a, mx, my) -> {
            PopupMenu pm = new PopupMenu();
            pm.item("Проиграть на модели", Icon.PLAY, null, () -> this.preview(a)).color(-11746422);
            pm.item("Вставить anim в скрипт", Icon.CODE, null, () -> this.insertAnim(a, false)).color(-8213761);
            pm.item("Вставить как мимику (face)", Icon.USER, null, () -> this.insertAnim(a, true));
            pm.item("Копировать id", Icon.COPY, null, () -> this.copy(a.id()));
            pm.show(mx, my);
        };
        this.layerPick = this.add(new Dropdown<>(() -> Specs.LAYERS, () -> this.layer, l -> "слой: " + l, l -> {
            this.layer = l;
            Anims.Entry a = this.animList.selectedItem();
            if (a != null) {
                this.preview(a);
            }
        }));
        this.layerPick.tooltip("Слой анимации. action — всё тело; emotion — лицо; 0–20 — накладываются поверх");
        this.speedPick = this.add(new Dropdown<>(() -> List.of(0.25F, 0.5F, 1.0F, 1.5F, 2.0F), () -> this.speed, s -> "×" + trim(s), s -> {
            this.speed = s;
            Anims.Entry a = this.animList.selectedItem();
            if (a != null) {
                this.preview(a);
            }
        }));
        this.speedPick.tooltip("Скорость проигрывания");
        this.loopBox = this.add(new Checkbox("Цикл", () -> this.loop, v -> {
            this.loop = v;
            Anims.Entry a = this.animList.selectedItem();
            if (a != null) {
                this.preview(a);
            }
        }));
        this.insertAnim = this.add(new Button("anim в скрипт", () -> {
            Anims.Entry a = this.animList.selectedItem();
            if (a != null) {
                this.insertAnim(a, false);
            }
        }).icon(Icon.CODE).style(Button.Style.PRIMARY).enabledWhen(() -> this.animList.selectedItem() != null));
        this.insertAnim.autoWidth();
        this.copyAnim = this.add(Button.icon(Icon.COPY, "Копировать id анимации", () -> {
            Anims.Entry a = this.animList.selectedItem();
            if (a != null) {
                this.copy(a.id());
            }
        }));
        this.add(this.soundSearch);
        this.soundSearch.onChange = s -> this.refreshSounds();
        this.soundList = this.add(new ListView<>((c, s, x, y, w, h, sel, hov) -> {
            boolean st = s.id().getNamespace().equals("spraute_engine");
            c.icon(Icon.NOTE, x + 6, y + (h - 9) / 2, st ? -12988235 : -9933435);
            c.textEllipsized(s.id().toString(), x + 20, y + (h - 8) / 2, w - 26, sel ? -1841682 : -6577995);
        }));
        this.soundList.rowHeight = 16;
        this.soundList.background = -14934490;
        this.soundList.activateOnSingleClick = true;
        this.soundList.onActivate = this::playSound;
        this.soundList.onContext = (s, mx, my) -> {
            PopupMenu pm = new PopupMenu();
            pm.item("Прослушать", Icon.PLAY, null, () -> this.playSound(s)).color(-11746422);
            pm.item("Вставить sound в скрипт", Icon.CODE, null, () -> insertCode.accept("sound \"" + s.id() + "\"")).color(-8213761);
            pm.item("Копировать id", Icon.COPY, null, () -> this.copy(s.id().toString()));
            pm.show(mx, my);
        };
        this.playSound = this.add(new Button("Прослушать", () -> {
            AssetIndex.Sound s = this.soundList.selectedItem();
            if (s != null) {
                this.playSound(s);
            }
        }).icon(Icon.PLAY).style(Button.Style.SUCCESS).enabledWhen(() -> this.soundList.selectedItem() != null));
        this.playSound.autoWidth();
        this.insertSound = this.add(new Button("sound в скрипт", () -> {
            AssetIndex.Sound s = this.soundList.selectedItem();
            if (s != null) {
                insertCode.accept("sound \"" + s.id() + "\"");
            }
        }).icon(Icon.CODE).style(Button.Style.PRIMARY).enabledWhen(() -> this.soundList.selectedItem() != null));
        this.insertSound.autoWidth();
        this.copySound = this.add(Button.icon(Icon.COPY, "Копировать id звука", () -> {
            AssetIndex.Sound s = this.soundList.selectedItem();
            if (s != null) {
                this.copy(s.id().toString());
            }
        }));
        this.refreshModels();
        this.refreshAnims();
        this.refreshSounds();
        AssetIndex.Model def = AssetIndex.defaultPreviewModel();
        if (def != null) {
            int i = this.modelList.items().indexOf(def);
            if (i >= 0) {
                this.modelList.select(i);
            }

            this.view.setType(def.type());
        }
    }

    private static String trim(float f) {
        return f == (int)f ? String.valueOf((int)f) : String.valueOf(f);
    }

    public void setMode(AssetsTab.Mode m) {
        this.mode = m;
        this.layout();
        this.focusContent();
    }

    public AssetsTab.Mode mode() {
        return this.mode;
    }

    private void refreshModels() {
        String q = this.modelSearch.text();
        List<AssetIndex.Model> list = new ArrayList<>();

        for (AssetIndex.Model m : AssetIndex.models()) {
            if ((!this.onlySpraute || m.builtin() || !q.isBlank()) && AssetIndex.matches(q, m.idString(), m.title())) {
                list.add(m);
            }
        }

        AssetIndex.Model sel = this.modelList.selectedItem();
        this.modelList.setItems(list);
        if (sel != null && list.contains(sel)) {
            this.modelList.select(list.indexOf(sel));
        }
    }

    private void refreshAnims() {
        String q = this.animSearch.text();
        List<Anims.Entry> list = new ArrayList<>();

        for (Anims.Entry a : AssetIndex.animations()) {
            if (AssetIndex.matches(q, a.id(), a.key(), a.fileName(), a.folder())) {
                list.add(a);
            }
        }

        this.animList.setItems(list);
    }

    private void refreshSounds() {
        String q = this.soundSearch.text();
        List<AssetIndex.Sound> list = new ArrayList<>();

        for (AssetIndex.Sound s : AssetIndex.sounds()) {
            if (AssetIndex.matches(q, s.id().toString())) {
                list.add(s);
            }
        }

        this.soundList.setItems(list);
    }

    private void preview(Anims.Entry a) {
        String err = this.view.play(a.id(), this.layer, this.loop || a.loops(), this.speed);
        if (err != null) {
            this.setStatus(err, -870325);
        } else {
            this.setStatus("▶ " + a.id() + "   (" + String.format(Locale.ROOT, "%.1f", a.seconds()) + " с, слой " + this.layer + ")", -11746422);
        }
    }

    private void playSound(AssetIndex.Sound s) {
        try {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Mc.soundEvent(s.id()), 1.0F));
            this.setStatus("♪ " + s.id(), -11746422);
        } catch (RuntimeException var3) {
            this.setStatus("Не удалось проиграть: " + var3.getMessage(), -870325);
        }
    }

    private void insertCharacter(AssetIndex.Model m) {
        String var = m.id().getPath().replaceAll("[^a-zA-Z0-9_]", "_");
        if (var.isEmpty() || Character.isDigit(var.charAt(0))) {
            var = "npc_" + var;
        }

        this.insertCode.accept("character " + var + " = \"" + m.idString() + "\" name \"" + m.title().replace("\"", "'") + "\"");
    }

    private void insertAnim(Anims.Entry a, boolean face) {
        String who = this.characterName.get();
        if (face) {
            this.insertCode.accept("face " + who + " \"" + a.id() + "\"");
        } else {
            StringBuilder sb = new StringBuilder("anim ").append(who).append(" \"").append(a.id()).append('"');
            if (!this.layer.equals("action")) {
                sb.append(" layer ").append(this.layer);
            }

            if (this.speed != 1.0F) {
                sb.append(" speed ").append(trim(this.speed));
            }

            if (this.loop) {
                sb.append(" loop");
            } else if (!a.loops()) {
                sb.append(" wait");
            }

            this.insertCode.accept(sb.toString());
        }
    }

    private void copy(String s) {
        Minecraft.getInstance().keyboardHandler.setClipboard(s);
        UI ui = UI.get();
        if (ui != null) {
            ui.toastInfo("Скопировано: " + s);
        }
    }

    private void setStatus(String s, int color) {
        this.status = s;
        this.statusColor = color;
    }

    @Override
    public void layout() {
        int bx = this.x + 10;

        for (Button b : List.of(this.modelsBtn, this.animsBtn, this.soundsBtn)) {
            b.bounds(bx, this.y + 6, b.prefW, 18);
            bx += b.prefW + 4;
        }

        boolean models = this.mode != AssetsTab.Mode.SOUNDS;

        for (Widget wdg : List.of(
            this.modelSearch,
            this.onlySt,
            this.modelList,
            this.view,
            this.resetView,
            this.spin,
            this.stopAnims,
            this.copyModel,
            this.insertCharacter,
            this.animSearch,
            this.animList,
            this.layerPick,
            this.speedPick,
            this.loopBox,
            this.insertAnim,
            this.copyAnim
        )) {
            wdg.visible = models;
        }

        for (Widget wdg : List.of(this.soundSearch, this.soundList, this.playSound, this.insertSound, this.copySound)) {
            wdg.visible = !models;
        }

        int top = this.y + 30;
        int bottom = this.y + this.h - 18;
        if (models) {
            int leftW = this.mode == AssetsTab.Mode.ANIMATIONS ? Math.max(150, this.w / 5) : Math.max(170, this.w * 27 / 100);
            int rightW = this.mode == AssetsTab.Mode.ANIMATIONS ? Math.max(230, this.w * 36 / 100) : Math.max(200, this.w * 28 / 100);
            if (leftW + rightW > this.w - 160) {
                leftW = (this.w - 160) * 45 / 100;
                rightW = (this.w - 160) * 55 / 100;
            }

            int lx = this.x;
            int rx = this.x + this.w - rightW;
            int cx = this.x + leftW + 1;
            int cw = this.w - leftW - rightW - 2;
            this.modelSearch.bounds(lx + 6, top + 4, leftW - 12, 18);
            this.onlySt.bounds(lx + 6, top + 26, leftW - 12, 14);
            this.modelList.bounds(lx, top + 44, leftW, bottom - top - 44);
            this.view.bounds(cx, top, cw, bottom - top - 26);
            int ty = bottom - 23;
            this.resetView.bounds(cx + 6, ty, 20, 20);
            this.spin.bounds(cx + 28, ty, 20, 20);
            this.stopAnims.bounds(cx + 50, ty, 20, 20);
            this.copyModel.bounds(cx + 72, ty, 20, 20);
            this.insertCharacter.bounds(cx + cw - this.insertCharacter.prefW - 6, ty, this.insertCharacter.prefW, 20);
            this.animSearch.bounds(rx + 6, top + 4, rightW - 12, 18);
            int ry = top + 26;
            int third = (rightW - 18) / 3;
            this.layerPick.bounds(rx + 6, ry, third + 14, 16);
            this.speedPick.bounds(rx + 6 + third + 17, ry, third - 10, 16);
            this.loopBox.bounds(rx + 6 + 2 * third + 12, ry + 1, third, 14);
            this.animList.bounds(rx, ry + 20, rightW, bottom - ry - 20 - 26);
            this.copyAnim.bounds(rx + 6, ty, 20, 20);
            this.insertAnim.bounds(rx + rightW - this.insertAnim.prefW - 6, ty, this.insertAnim.prefW, 20);
        } else {
            this.soundSearch.bounds(this.x + 10, top + 4, Math.min(360, this.w - 20), 18);
            this.soundList.bounds(this.x, top + 26, this.w, bottom - top - 26 - 26);
            int ty = bottom - 23;
            this.playSound.bounds(this.x + 10, ty, this.playSound.prefW, 20);
            this.insertSound.bounds(this.x + 14 + this.playSound.prefW, ty, this.insertSound.prefW, 20);
            this.copySound.bounds(this.x + 18 + this.playSound.prefW + this.insertSound.prefW, ty, 20, 20);
        }

        super.layout();
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.fill(this.x, this.y, this.w, this.h, -15461093);
        c.fill(this.x, this.y + 30 - 1, this.w, 1, -13749952);
        String counts = this.mode == AssetsTab.Mode.SOUNDS
            ? AssetIndex.sounds().size() + " звуков"
            : AssetIndex.models().size() + " моделей · " + AssetIndex.animations().size() + " анимаций";
        c.text(counts, this.x + this.w - c.width(counts) - 10, this.y + 11, -9933435);
        if (this.mode != AssetsTab.Mode.SOUNDS) {
            int leftR = this.modelList.x + this.modelList.w;
            int rightL = this.animList.x;
            c.fill(leftR, this.y + 30, 1, this.h - 30 - 18, -13749952);
            c.fill(rightL - 1, this.y + 30, 1, this.h - 30 - 18, -13749952);
            c.fill(this.modelList.x, this.view.y + this.view.h, this.modelList.w, 1, 0);
            c.fill(this.view.x, this.view.y + this.view.h, this.view.w, 26, -14934490);
            c.fill(this.animList.x, this.animList.y + this.animList.h, this.animList.w, 26, -14934490);
            c.fill(this.modelList.x, this.y + 30, this.modelList.w, 44, -14934490);
            c.fill(this.animList.x, this.y + 30, this.animList.w, this.animList.y - this.y - 30, -14934490);
        } else {
            c.fill(this.x, this.soundList.y + this.soundList.h, this.w, 26, -14934490);
        }
    }

    @Override
    protected void paintOver(Canvas c, int mx, int my, float dt) {
        if (this.mode != AssetsTab.Mode.SOUNDS) {
            c.pushClip(this.view.x, this.view.y, this.view.w, this.view.h);
            AssetIndex.Model m = this.modelList.selectedItem();
            if (m != null) {
                c.text(m.title(), this.view.x + 10, this.view.y + 8, -1841682);
                c.text(m.idString(), this.view.x + 10, this.view.y + 20, -9933435);
                if (this.created(m)) {
                    c.text(
                        this.view.isAnimated() ? "анимации поддерживаются" : "без анимаций анимаций",
                        this.view.x + 10,
                        this.view.y + 32,
                        this.view.isAnimated() ? -12988235 : -9933435
                    );
                }
            }

            String hint = "ЛКМ — вращать, колесо — масштаб";
            if (this.view.w > c.width(hint) + c.width(m != null ? m.title() : "") + 40) {
                c.text(hint, this.view.x + this.view.w - c.width(hint) - 8, this.view.y + 8, 1728053247);
            }

            c.popClip();
        }

        c.fill(this.x, this.y + this.h - 18, this.w, 18, -15724266);
        c.fill(this.x, this.y + this.h - 18, this.w, 1, -13749952);
        c.textEllipsized(this.status, this.x + 8, this.y + this.h - 13, this.w - 16, this.statusColor);
    }

    private boolean created(AssetIndex.Model m) {
        return this.view.type() == m.type() && Minecraft.getInstance().level != null;
    }

    @Override
    public String title() {
        return "Ассеты";
    }

    @Override
    public Icon icon() {
        return Icon.CUBE;
    }

    @Override
    public int iconColor() {
        return -28838;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void save() {
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
        if (this.mode == AssetsTab.Mode.SOUNDS) {
            this.soundSearch.requestFocus();
        } else if (this.mode == AssetsTab.Mode.ANIMATIONS) {
            this.animSearch.requestFocus();
        } else {
            this.modelSearch.requestFocus();
        }
    }

    public static enum Mode {
        MODELS,
        ANIMATIONS,
        SOUNDS;
    }
}
