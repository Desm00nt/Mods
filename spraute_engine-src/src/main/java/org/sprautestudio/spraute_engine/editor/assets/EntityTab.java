package org.sprautestudio.spraute_engine.editor.assets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.sprautestudio.spraute_engine.core.anim.Animation;
import org.sprautestudio.spraute_engine.core.anim.ConfiguredAnimation;
import org.sprautestudio.spraute_engine.editor.SprauteScreen;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.runtime.Images;
import org.sprautestudio.spraute_engine.runtime.models.EntityLibrary;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;
import org.sprautestudio.spraute_engine.runtime.models.PartShape;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;
import org.sprautestudio.spraute_engine.runtime.orb.Orbs;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class EntityTab extends EditorTab {
    public final Project.EntityRef ref;
    private EntityLibrary.Def def;
    private final ModelView view = new ModelView();
    private final List<Widget> rows = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private final List<Integer> rowTabs = new ArrayList<>();
    private static final String[] TABS = new String[]{"Модель", "Предмет", "Хитбокс"};
    private final List<Button> tabButtons = new ArrayList<>();
    private int tab;
    private int buildTab;
    private final Button character;
    private final Button playWalk;
    private final Button playIdle;
    private static List<String> textureCache;
    private final Runnable importModels;
    private int selPart = -1;
    private boolean mainSel;
    private final List<Runnable> partRefresh = new ArrayList<>();
    private final List<Runnable> mainRefresh = new ArrayList<>();
    private List<String> boneCache;
    private Object boneCacheFor;
    private static final int FORM_W = 320;
    private static final int TOP = 62;
    private int formScroll;

    public void useModel(String name) {
        this.def.model = name;
        this.def.texture = "model";
        this.changed();
    }

    public EntityTab(Project project, Project.EntityRef ref, Consumer<String> insertCode, final Runnable importModels) {
        super(project, null);
        this.ref = ref;
        this.importModels = importModels;
        String json = "";

        try {
            json = Files.readString(ref.path(), StandardCharsets.UTF_8);
        } catch (IOException var11) {
        }

        this.def = EntityLibrary.Def.fromJson(json);
        this.add(this.view);
        this.view.emptyText = "Войдите в мир, чтобы увидеть сущность";
        final Dropdown<String> modelBox = new Dropdown<>(this::models, () -> this.def.model, EntityTab::modelTitle, v -> {
            String previousModel = this.def.model;
            String previousSkin = firstSkin(previousModel);
            this.def.model = v;
            if (!v.startsWith("st:")) {
                this.def.texture = "model";
            } else if (this.def.texture.equals("model") || this.def.texture.equals(previousSkin)) {
                this.def.texture = firstSkin(v);
            }

            this.changed();
        }).searchable();
        this.row(
            "Модель",
            new Widget() {
                final Dropdown<String> dd = this.add(modelBox);
                final Button add = this.add(
                    Button.icon(
                        Icon.PLUS,
                        "Добавить свою модель: файлы Blockbench (.geo.json, .png, .animation.json). Можно и просто перетащить их в окно",
                        importModels
                    )
                );

                @Override
                public void layout() {
                    this.dd.bounds(this.x, this.y, this.w - 22, this.h);
                    this.add.bounds(this.x + this.w - 20, this.y, 20, this.h);
                    super.layout();
                }
            }
        );
        final Dropdown<String> skinBox = new Dropdown<>(this::skins, () -> this.def.texture, EntityTab::skinTitle, v -> {
            this.def.texture = v;
            this.changed();
        });
        this.row(
            "Скин",
            new Widget() {
                final Dropdown<String> dd = this.add(skinBox);
                final Button fromMc = this.add(
                    Button.icon(
                        Icon.USER,
                        "Из скина Minecraft: сделать текстуру Spraute 64×64 или Super Rig 128×128 с анимируемым лицом",
                        () -> SprauteScreen.pickFaceSkin(name -> {
                            EntityTab.this.def.texture = "image:" + name;
                            EntityTab.this.changed();
                        })
                    )
                );

                @Override
                public void layout() {
                    this.dd.bounds(this.x, this.y, this.w - 22, this.h);
                    this.fromMc.bounds(this.x + this.w - 20, this.y, 20, this.h);
                    super.layout();
                }
            }
        );
        this.animRow("Стоит", () -> this.def.idle, v -> this.def.idle = v);
        this.animRow("Ходит", () -> this.def.walk, v -> this.def.walk = v);
        this.animRow("Бежит", () -> this.def.run, v -> this.def.run = v);
        this.buildTab = 1;
        this.row(
            "Предмет в руке",
            new Dropdown<>(() -> this.bones(this.def.itemBone), () -> this.def.itemBone, v -> v.isEmpty() ? "авто (Right_placeholder, RightHand…)" : v, v -> {
                this.def.itemBone = v;
                this.changed();
            })
        );
        this.row(
            "Во второй руке",
            new Dropdown<>(
                () -> this.bones(this.def.offhandBone), () -> this.def.offhandBone, v -> v.isEmpty() ? "авто (Left_placeholder, LeftHand…)" : v, v -> {
                    this.def.offhandBone = v;
                    this.changed();
                }
            )
        );
        this.signedRow("Предмет: поворот X", () -> this.def.itemTurnX, v -> this.def.itemTurnX = (float)v);
        this.signedRow("Предмет: поворот Y", () -> this.def.itemTurnY, v -> this.def.itemTurnY = (float)v);
        this.signedRow("Предмет: поворот Z", () -> this.def.itemTurnZ, v -> this.def.itemTurnZ = (float)v);
        this.signedRow("Предмет: сдвиг X", () -> this.def.itemX, v -> this.def.itemX = (float)v);
        this.signedRow("Предмет: сдвиг Y", () -> this.def.itemY, v -> this.def.itemY = (float)v);
        this.signedRow("Предмет: сдвиг Z", () -> this.def.itemZ, v -> this.def.itemZ = (float)v);
        this.numberRow("Предмет: размер", () -> this.def.itemScale, v -> this.def.itemScale = (float)Math.max(0.05, Math.min(10.0, v)));
        this.buildTab = 0;
        this.numberRow("Размер", () -> this.def.scale, v -> this.def.scale = (float)Math.max(0.05, Math.min(10.0, v)));
        this.numberRow("Скорость", () -> this.def.speed, v -> this.def.speed = Math.max(0.0, Math.min(10.0, v)));
        this.signedRow("Сдвиг модели вверх", () -> this.def.modelY, v -> this.def.modelY = (float)Math.max(-10.0, Math.min(10.0, v)));
        this.numberRow("Здоровье", () -> this.def.health, v -> this.def.health = (float)Math.max(0.0, Math.min(10000.0, v)));
        this.buildTab = 2;
        this.row(
            "Поведение тела",
            new Dropdown<>(
                () -> List.of("", "solid", "fixed", "ghost", "damage", "mortal", "solid damage", "solid mortal", "nodamage", "nopick"),
                () -> this.def.hitbox,
                v -> v.isEmpty() ? "как обычно" : v,
                v -> {
                    this.def.hitbox = v;
                    this.changed();
                }
            )
        );
        this.hitboxRows();
        this.character = this.add(new Button("Персонаж с этой сущностью", () -> {
            String var = Images.slug(ref.name());
            insertCode.accept("character " + var + " = \"spraute:" + ref.name() + "\" name \"" + ref.name().replace("\"", "'") + "\"");
        }).icon(Icon.USER).style(Button.Style.PRIMARY));
        this.playIdle = this.add(new Button("Стоит", () -> this.play(this.def.idle, "idle")).icon(Icon.PLAY).style(Button.Style.GHOST));
        this.playWalk = this.add(new Button("Ходит", () -> this.play(this.def.walk, "walk")).icon(Icon.PLAY).style(Button.Style.GHOST));

        for (Button b : List.of(this.character, this.playIdle, this.playWalk)) {
            b.autoWidth();
        }

        for (int i = 0; i < TABS.length; i++) {
            int t = i;
            Button tb = this.add(new Button(TABS[i], () -> {
                this.tab = t;
                this.formScroll = 0;
                this.layout();
            }).toggle(() -> this.tab == t).style(Button.Style.GHOST));
            tb.autoWidth();
            this.tabButtons.add(tb);
        }

        this.changed();
    }

    private EntityLibrary.Def.Part part() {
        return this.selPart >= 0 && this.selPart < this.def.parts.size() ? this.def.parts.get(this.selPart) : null;
    }

    private void pickPart(int i) {
        this.selPart = i;

        for (Runnable r : this.partRefresh) {
            r.run();
        }
    }

    private void hitboxRows() {
        this.mainNumber("Основной: ширина", () -> this.def.width, v -> this.def.width = (float)Math.max(0.0, Math.min(20.0, v)));
        this.mainNumber("Основной: высота", () -> this.def.height, v -> this.def.height = (float)Math.max(0.0, Math.min(40.0, v)));
        this.row(
            "Части хитбокса",
            new Widget() {
                final Dropdown<Integer> dd = this.add(
                    new Dropdown<>(
                        () -> {
                            List<Integer> out = new ArrayList<>();
                            out.add(-1);

                            for (int i = 0; i < EntityTab.this.def.parts.size(); i++) {
                                out.add(i);
                            }

                            return out;
                        },
                        () -> EntityTab.this.selPart,
                        i -> i < 0
                            ? (EntityTab.this.def.parts.isEmpty() ? "нет — добавьте +" : "основной хитбокс")
                            : (i < EntityTab.this.def.parts.size() ? EntityTab.this.def.parts.get(i).name : "?"),
                        EntityTab.this::pickPart
                    )
                );
                final Button plus = this.add(
                    Button.icon(Icon.PLUS, "Добавить часть хитбокса (голова, хвост, уязвимое место…). Её можно таскать мышью прямо в превью", () -> {
                        if (EntityTab.this.def.parts.size() >= 8) {
                            UI ui = UI.get();
                            if (ui != null) {
                                ui.toastError("Не больше 8 частей");
                            }
                        } else {
                            EntityLibrary.Def.Part np = new EntityLibrary.Def.Part();
                            np.name = EntityTab.this.def.parts.isEmpty() ? "голова" : "часть" + (EntityTab.this.def.parts.size() + 1);
                            np.y = EntityTab.this.def.parts.isEmpty() ? 1.5F : 1.0F;
                            EntityTab.this.def.parts.add(np);
                            EntityTab.this.saveOnly();
                            EntityTab.this.pickPart(EntityTab.this.def.parts.size() - 1);
                        }
                    })
                );
                final Button minus = this.add(Button.icon(Icon.TRASH, "Убрать выбранную часть", () -> {
                    if (EntityTab.this.part() != null) {
                        EntityTab.this.def.parts.remove(EntityTab.this.selPart);
                        EntityTab.this.saveOnly();
                        EntityTab.this.pickPart(Math.min(EntityTab.this.selPart, EntityTab.this.def.parts.size() - 1));
                    }
                }));

                @Override
                public void layout() {
                    this.dd.bounds(this.x, this.y, this.w - 44, this.h);
                    this.plus.bounds(this.x + this.w - 42, this.y, 20, this.h);
                    this.minus.bounds(this.x + this.w - 20, this.y, 20, this.h);
                    super.layout();
                }
            }
        );
        this.partText("  имя", () -> this.part() == null ? "" : this.part().name, v -> {
            String n = v.trim().replace(",", "");
            if (this.part() != null && !n.isEmpty()) {
                this.part().name = n;
            }
        });
        this.partNumber("  X (влево)", () -> this.part() == null ? 0.0 : this.part().x, v -> this.part().x = (float)v, true);
        this.partNumber("  Y (вверх)", () -> this.part() == null ? 0.0 : this.part().y, v -> this.part().y = (float)v, true);
        this.partNumber("  Z (вперёд)", () -> this.part() == null ? 0.0 : this.part().z, v -> this.part().z = (float)v, true);
        this.partNumber("  ширина", () -> this.part() == null ? 0.0 : this.part().width, v -> this.part().width = (float)Math.max(0.05, v), false);
        this.partNumber("  высота", () -> this.part() == null ? 0.0 : this.part().height, v -> this.part().height = (float)Math.max(0.05, v), false);
        this.partNumber("  глубина", () -> this.part() == null ? 0.0 : this.part().depth, v -> this.part().depth = (float)Math.max(0.0, v), false);
        this.partNumber("  поворот X", () -> this.part() == null ? 0.0 : this.part().rx, v -> this.part().rx = (float)v, true);
        this.partNumber("  поворот Y", () -> this.part() == null ? 0.0 : this.part().ry, v -> this.part().ry = (float)v, true);
        this.partNumber("  поворот Z", () -> this.part() == null ? 0.0 : this.part().rz, v -> this.part().rz = (float)v, true);
        this.row(
            "  кость",
            new Dropdown<>(
                () -> this.bones(this.part() == null ? "" : this.part().bone),
                () -> this.part() == null ? "" : this.part().bone,
                v -> v.isEmpty() ? "нет — стоит от ног" : v,
                v -> {
                    if (this.part() != null) {
                        this.part().bone = v;
                        this.saveOnly();
                    }
                }
            )
        );
        this.partNumber("  урон ×", () -> this.part() == null ? 0.0 : this.part().damage, v -> this.part().damage = (float)Math.max(0.0, v), false);
        this.row(
            "  включена",
            new Dropdown<>(
                () -> List.of(true, false), () -> this.part() == null || this.part().enabled, v -> v ? "да" : "нет (в сюжете: part … enabled true)", v -> {
                    if (this.part() != null) {
                        this.part().enabled = v;
                        this.saveOnly();
                    }
                }
            )
        );
        this.view.boxes = () -> {
            List<ModelView.Box> out = new ArrayList<>();
            if (this.tab != 2) {
                return out;
            } else {
                float sc = this.def.scale;
                float mw = (this.def.width > 0.0F ? this.def.width : 0.6F) * sc;
                float mh = (this.def.height > 0.0F ? this.def.height : 1.8F) * sc;
                out.add(
                    new ModelView.Box(
                        new PartShape(new Vector3f(0.0F, mh / 2.0F, 0.0F), new Quaternionf(), new Vector3f(mw / 2.0F, mh / 2.0F, mw / 2.0F)),
                        -1426063361,
                        "основной " + fmt(mw / sc) + "×" + fmt(mh / sc),
                        this.mainSel
                    )
                );

                for (int i = 0; i < this.def.parts.size(); i++) {
                    EntityLibrary.Def.Part pt = this.def.parts.get(i);
                    int col = !pt.enabled ? -2004318072 : (pt.damage > 1.0F ? -40864 : (pt.damage < 1.0F ? -10444545 : -10420336));
                    out.add(
                        new ModelView.Box(
                            PartShape.of(pt, sc, 0.0F, this.previewPose(pt.bone)),
                            col,
                            pt.name + (pt.damage != 1.0F ? " ×" + fmt(pt.damage) : "") + (pt.bone.isEmpty() ? "" : " → " + pt.bone),
                            i == this.selPart
                        )
                    );
                }

                return out;
            }
        };
        this.view.onPickBox = i -> {
            this.mainSel = i == 0;
            if (i >= 0) {
                this.pickPart(i - 1);
            }
        };
        this.view.onMoveBox = new ModelView.MoveListener() {
            @Override
            public void moved(float dx, float dy, float dz, float sx, float sy) {
                float sc = EntityTab.this.def.scale > 0.0F ? EntityTab.this.def.scale : 1.0F;
                if (EntityTab.this.mainSel) {
                    float w0 = EntityTab.this.def.width > 0.0F ? EntityTab.this.def.width : 0.6F;
                    float h0 = EntityTab.this.def.height > 0.0F ? EntityTab.this.def.height : 1.8F;
                    EntityTab.this.def.width = EntityTab.snap(Math.max(0.1F, Math.min(20.0F, w0 + 2.0F * sx / sc)));
                    EntityTab.this.def.height = EntityTab.snap(Math.max(0.1F, Math.min(40.0F, h0 - sy / sc)));

                    for (Runnable r : EntityTab.this.mainRefresh) {
                        r.run();
                    }
                } else {
                    EntityLibrary.Def.Part pt = EntityTab.this.part();
                    if (pt != null) {
                        PartShape.BonePose bp = EntityTab.this.previewPose(pt.bone);
                        Vector3f d = new Vector3f(dx, dy, dz);
                        if (bp != null) {
                            new Quaternionf(bp.rot()).conjugate().transform(d);
                        }

                        pt.x = EntityTab.snap(pt.x + d.x / sc);
                        pt.y = EntityTab.snap(pt.y + d.y / sc);
                        pt.z = EntityTab.snap(pt.z + d.z / sc);

                        for (Runnable r : EntityTab.this.partRefresh) {
                            r.run();
                        }
                    }
                }
            }

            @Override
            public void released() {
                if (EntityTab.this.mainSel || EntityTab.this.part() != null) {
                    EntityTab.this.saveOnly();
                }
            }
        };
    }

    private PartShape.BonePose previewPose(String bone) {
        if (bone != null && !bone.isEmpty()) {
            return this.view.previewEntity() instanceof SprauteModelEntity me ? me.bonePoses.get(bone) : null;
        } else {
            return null;
        }
    }

    private static float snap(float v) {
        return Math.round(v * 100.0F) / 100.0F;
    }

    private void mainNumber(String label, DoubleSupplier get, DoubleConsumer set) {
        TextField f = new TextField();
        f.setText(fmt(get.getAsDouble()));
        f.filter = ch -> Character.isDigit(ch) || ch == '.';
        f.onChange = s -> {
            try {
                set.accept(Double.parseDouble(s.trim()));
                this.saveOnly();
            } catch (NumberFormatException var4x) {
            }
        };
        this.mainRefresh.add(() -> f.setText(fmt(get.getAsDouble())));
        this.row(label, f);
    }

    private void partText(String label, Supplier<String> get, Consumer<String> set) {
        TextField f = new TextField();
        f.setText(get.get());
        f.onChange = s -> {
            if (this.part() != null) {
                set.accept(s);
                this.saveOnly();
            }
        };
        this.partRefresh.add(() -> f.setText(get.get()));
        this.row(label, f);
    }

    private void partNumber(String label, DoubleSupplier get, DoubleConsumer set, boolean signed) {
        TextField f = new TextField();
        f.setText(this.part() == null ? "" : fmt(get.getAsDouble()));
        f.filter = ch -> Character.isDigit(ch) || ch == '.' || signed && ch == '-';
        f.onChange = s -> {
            if (this.part() != null) {
                try {
                    set.accept(Double.parseDouble(s.trim()));
                    this.saveOnly();
                } catch (NumberFormatException var4x) {
                }
            }
        };
        this.partRefresh.add(() -> f.setText(this.part() == null ? "" : fmt(get.getAsDouble())));
        this.row(label, f);
    }

    private void saveOnly() {
        try {
            Files.writeString(this.ref.path(), this.def.toJson(), StandardCharsets.UTF_8);
        } catch (IOException var3) {
            UI ui = UI.get();
            if (ui != null) {
                ui.toastError("Не удалось сохранить: " + var3.getMessage());
            }
        }

        ModelLibrary.scan();
    }

    private List<String> bones(String cur) {
        List<String> out = new ArrayList<>(List.of(""));

        try {
            EntityLibrary.Type t = EntityLibrary.find(this.project.name(), this.ref.name());
            EntityLibrary.Look l = t != null ? EntityLibrary.look(t.key()) : null;
            if (l != null && l.geometry() != null) {
                if (this.boneCache == null || this.boneCacheFor != l.geometry()) {
                    this.boneCache = new ArrayList<>(new TreeSet<>(l.geometry().boneMap.keySet()));
                    this.boneCacheFor = l.geometry();
                }

                out.addAll(this.boneCache);
            }
        } catch (Throwable var5) {
        }

        if (!cur.isEmpty() && !out.contains(cur)) {
            out.add(cur);
        }

        return out;
    }

    private void row(String label, Widget editor) {
        this.labels.add(label);
        this.rowTabs.add(this.buildTab);
        this.rows.add(this.add(editor));
    }

    private void numberRow(String label, DoubleSupplier get, DoubleConsumer set) {
        TextField f = new TextField();
        f.setText(fmt(get.getAsDouble()));
        f.filter = ch -> Character.isDigit(ch) || ch == '.';
        f.onChange = s -> {
            try {
                set.accept(Double.parseDouble(s.trim()));
                this.changed();
            } catch (NumberFormatException var4x) {
            }
        };
        this.row(label, f);
    }

    private void signedRow(String label, DoubleSupplier get, DoubleConsumer set) {
        TextField f = new TextField();
        f.setText(fmt(get.getAsDouble()));
        f.filter = ch -> Character.isDigit(ch) || ch == '.' || ch == '-';
        f.onChange = s -> {
            try {
                set.accept(Double.parseDouble(s.trim()));
                this.changed();
            } catch (NumberFormatException var4x) {
            }
        };
        this.row(label, f);
    }

    private void animRow(String label, final Supplier<String> get, final Consumer<String> set) {
        Widget box = new Widget() {
            final Dropdown<String> dd = this.add(
                new Dropdown<>(() -> EntityTab.this.anims(get.get()), get, a -> a.isEmpty() ? "авто (по имени idle/walk/run)" : EntityTab.animTitle(a), v -> {
                    set.accept(v);
                    EntityTab.this.changed();
                }).searchable()
            );
            final Button pick = this.add(Button.icon(Icon.SEARCH, "Анимация Spraute…", () -> AssetPicker.open(ParamType.ANIM, get.get(), v -> {
                set.accept(v);
                EntityTab.this.changed();
            }, "spraute:" + EntityTab.this.ref.name())));

            @Override
            public void layout() {
                this.dd.bounds(this.x, this.y, this.w - 22, this.h);
                this.pick.bounds(this.x + this.w - 20, this.y, 20, this.h);
                super.layout();
            }
        };
        this.row(label, box);
    }

    private static String fmt(double v) {
        return v == (long)v ? String.valueOf((long)v) : String.valueOf(Math.round(v * 100.0) / 100.0);
    }

    private List<String> models() {
        List<String> out = new ArrayList<>();

        for (Project.ModelRef m : this.project.models()) {
            out.add(m.name());
        }

        for (String b : EntityLibrary.builtinModels()) {
            out.add("st:" + b);
        }

        if (!out.contains(this.def.model)) {
            out.add(0, this.def.model);
        }

        return out;
    }

    private static String modelTitle(String m) {
        return m.startsWith("st:") ? "Spraute · " + m.substring(m.lastIndexOf(47) + 1) : m + "  (файлы проекта)";
    }

    private List<String> skins() {
        List<String> out = new ArrayList<>();
        if (!this.def.model.startsWith("st:")) {
            out.add("model");
        }

        for (Project.ImageRef img : this.project.images()) {
            out.add("image:" + img.name());
        }

        String folder = this.def.model.startsWith("st:") ? this.def.model.substring(this.def.model.lastIndexOf(47) + 1) : null;
        List<String> all = textures();
        List<String> own = new ArrayList<>();
        List<String> rest = new ArrayList<>();

        for (String t : all) {
            if (folder != null && t.contains("/entity/" + folder + "/")) {
                own.add("st:" + t);
            } else {
                rest.add("st:" + t);
            }
        }

        out.addAll(own);
        out.addAll(rest);
        if (!out.contains(this.def.texture)) {
            out.add(0, this.def.texture);
        }

        return out;
    }

    private static String skinTitle(String s) {
        if (s.equals("model")) {
            return "картинка из файлов модели";
        } else {
            return s.startsWith("image:")
                ? "картинка проекта: " + s.substring(6)
                : s.substring(s.indexOf("textures/entity/") >= 0 ? s.indexOf("textures/entity/") + 16 : 0).replace(".png", "");
        }
    }

    private static List<String> textures() {
        if (textureCache != null) {
            return textureCache;
        } else {
            List<String> out = new ArrayList<>();

            try {
                for (ResourceLocation rl : Minecraft.getInstance()
                    .getResourceManager()
                    .listResources("textures/entity", r -> r.getPath().endsWith(".png"))
                    .keySet()) {
                    if (rl.getNamespace().equals("spraute_engine")) {
                        out.add(rl.toString());
                    }
                }
            } catch (Throwable var3) {
            }

            Collections.sort(out);
            textureCache = out;
            return out;
        }
    }

    private static String firstSkin(String model) {
        String folder = model.substring(model.lastIndexOf(47) + 1);

        for (String t : textures()) {
            if (t.contains("/entity/" + folder + "/")) {
                return "st:" + t;
            }
        }

        return "st:spraute_engine:textures/entity/classic.png";
    }

    private List<String> anims(String current) {
        List<String> out = new ArrayList<>();
        out.add("");
        if (!this.def.model.startsWith("st:")) {
            out.addAll(ModelLibrary.animations(ModelLibrary.find(this.project.name(), this.def.model)));
        }

        for (ModelLibrary.ProjectAnim a : ModelLibrary.projectAnimations(this.project.name())) {
            out.add(a.shortId());
        }

        if (current != null && !current.isEmpty() && !out.contains(current)) {
            out.add(current);
        }

        return out;
    }

    private static String animTitle(String a) {
        int bang = a.indexOf(33);
        String s = bang >= 0 ? a.substring(0, bang) : a;
        return s.substring(s.lastIndexOf(47) + 1);
    }

    private void changed() {
        try {
            Files.writeString(this.ref.path(), this.def.toJson(), StandardCharsets.UTF_8);
        } catch (IOException var3) {
            UI ui = UI.get();
            if (ui != null) {
                ui.toastError("Не удалось сохранить: " + var3.getMessage());
            }
        }

        ModelLibrary.scan();
        EntityLibrary.Type t = EntityLibrary.find(this.project.name(), this.ref.name());
        String key = t != null ? t.key() : "";
        this.view.setType(Orbs.MODEL.get(), e -> {
            if (e instanceof SprauteModelEntity me) {
                me.setModel(key);
                me.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            }
        });
        this.play(this.def.idle, "idle");
    }

    private void play(String anim, String fallback) {
        EntityLibrary.Type t = EntityLibrary.find(this.project.name(), this.ref.name());
        if (t != null) {
            EntityLibrary.Look l = EntityLibrary.look(t.key());
            if (l != null) {
                Optional<Animation> a = l.animation(anim);
                if (a.isEmpty()) {
                    a = l.animation(fallback);
                }

                ConfiguredAnimation ca = a.map(Animation::configure).orElse(null);
                String err = ca != null ? this.view.play(ca, fallback) : "У модели нет анимации «" + fallback + "» — выберите её в поле выше";
                if (err != null && !fallback.equals("idle")) {
                    UI ui = UI.get();
                    if (ui != null) {
                        ui.toastError(err);
                    }
                }
            }
        }
    }

    @Override
    public String title() {
        return this.ref.name();
    }

    private int tabRows() {
        int n = 0;

        for (int t : this.rowTabs) {
            if (t == this.tab) {
                n++;
            }
        }

        return n;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < this.x + Math.min(320, this.w / 2)) {
            this.formScroll = Math.max(0, this.formScroll - (int)Math.round(delta * 26.0));
            this.layout();
            return true;
        } else {
            return super.mouseScrolled(mx, my, delta);
        }
    }

    @Override
    public void layout() {
        int fw = Math.min(320, this.w / 2);
        this.formScroll = Math.max(0, Math.min(this.formScroll, Math.max(0, this.tabRows() * 26 + 150 - (this.h - 100))));
        int tx = this.x + 10;

        for (Button tb : this.tabButtons) {
            tb.bounds(tx, this.y + 34, tb.prefW, 20);
            tx += tb.prefW + 4;
        }

        int ry = this.y + 62 - this.formScroll;

        for (int i = 0; i < this.rows.size(); i++) {
            Widget r = this.rows.get(i);
            if (this.rowTabs.get(i) != this.tab) {
                r.bounds(-10000, 0, fw - 106, 18);
                r.layout();
            } else {
                boolean seen = ry >= this.y + 62 - 4 && ry + 18 <= this.y + this.h - 36;
                r.bounds(seen ? this.x + 116 : -10000, ry, fw - 126, 18);
                r.layout();
                ry += 26;
            }
        }

        this.view.bounds(this.x + fw, this.y + 30, this.w - fw, this.h - 30 - 34);
        int bx = this.x + 10;
        int by = this.y + this.h - 27;

        for (Button b : List.of(this.character, this.playIdle, this.playWalk)) {
            b.bounds(bx, by, b.prefW, 20);
            bx += b.prefW + 6;
        }

        super.layout();
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        int fw = Math.min(320, this.w / 2);
        c.fill(this.x, this.y, this.w, this.h, -15461093);
        c.fill(this.x, this.y, fw, this.h - 34, -14934490);
        c.fill(this.x + fw, this.y + 30, 1, this.h - 64, -13749952);
        c.text(this.ref.name(), this.x + 10, this.y + 10, -1841682);
        c.text("в сюжете: \"spraute:" + this.ref.name() + "\"", this.x + 20 + c.width(this.ref.name()), this.y + 10, -9933435);
        c.fill(this.x, this.y + 29, this.w, 1, -13749952);
        c.fill(this.x, this.y + 58, fw, 1, -13749952);
        int ry = this.y + 62 - this.formScroll;
        c.pushClip(this.x, this.y + 59, fw, this.h - 93);

        for (int i = 0; i < this.labels.size(); i++) {
            if (this.rowTabs.get(i) == this.tab) {
                if (ry >= this.y + 62 - 4 && ry + 18 <= this.y + this.h - 36) {
                    c.text(this.labels.get(i), this.x + 10, ry + 5, -6577995);
                }

                ry += 26;
            }
        }

        if (this.tab == 0) {
            c.wrapped(
                "Сущность — это тип персонажа. Настройте её тут, а в сюжете создайте персонажа: character x = \"spraute:"
                    + this.ref.name()
                    + "\". Анимации «авто» берутся из файлов модели по именам idle, walk и run.",
                this.x + 10,
                ry + 8,
                fw - 20,
                -9933435,
                2
            );
        }

        if (this.tab == 1) {
            c.wrapped(
                "Как сидит предмет в руке: кость, поворот, сдвиг (в пикселях модели) и размер. В превью у сущности меч.",
                this.x + 10,
                ry + 8,
                fw - 20,
                -9933435,
                2
            );
        }

        if (this.tab == 2) {
            c.wrapped(
                "Белая рамка — основной хитбокс: он всегда стоит по центру у ног (так устроена игра), его можно только растянуть — тащите его мышью вбок (ширина) и вверх-вниз (высота). 0 в полях — обычный размер 0.6×1.8.\nЦветные — части: кликните и тащите куда угодно. Красная — урон больше, синяя — меньше, серая — выключена. Часть можно повернуть и посадить на кость модели — тогда она ездит за анимацией (рука, хвост, оружие).\nЕсли ноги модели ниже хитбокса — вкладка «Модель», «Сдвиг модели вверх». В игре F3+B показывает части жёлтым.\nЧтобы удар в голову отличался от удара в тело, сделайте основной пониже, а голову поставьте над ним.\nВ сюжете: on parthit x { event(\"part\") }, lastpart(x), partpos(x, \"голова\"), part x \"голова\" enabled false.",
                this.x + 10,
                ry + 8,
                fw - 20,
                -9933435,
                2
            );
        }

        c.popClip();
        if (this.formScroll > 0 || this.tabRows() * 26 + 150 > this.h - 100) {
            c.text("колесо — прокрутка", this.x + fw - 10 - c.width("колесо — прокрутка"), this.y + 10, -9933435);
        }

        c.fill(this.x, this.y + this.h - 34, this.w, 34, -14934490);
        c.fill(this.x, this.y + this.h - 34, this.w, 1, -13749952);
    }

    @Override
    public Icon icon() {
        return Icon.USER;
    }

    @Override
    public int iconColor() {
        return -11544898;
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
    }
}
