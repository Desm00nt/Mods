package org.sprautestudio.spraute_engine.editor.assets;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.anim.Animation;
import org.sprautestudio.spraute_engine.engine.client.ProjectSounds;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.runtime.Anims;
import org.sprautestudio.spraute_engine.runtime.models.EntityLibrary;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;
import org.sprautestudio.spraute_engine.runtime.orb.Orbs;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Dialog;
import org.sprautestudio.spraute_engine.ui.widgets.ListView;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public final class AssetPicker {
    private static final String PREVIEW = "__picker_preview";
    public static boolean gameSoundsOnly;
    public static String previewEntity;

    private AssetPicker() {
    }

    public static void open(ParamType kind, String current, Consumer<String> onPick, String entity) {
        previewEntity = entity;
        open(kind, current, onPick);
    }

    public static void open(ParamType kind, String current, Consumer<String> onPick) {
        String forEntity = previewEntity;
        previewEntity = null;
        UI ui = UI.get();
        if (ui != null) {
            if (kind == ParamType.ITEM) {
                ItemPicker.open(current, onPick);
            } else {
                String title = switch (kind) {
                    case ANIM -> "Выбор анимации";
                    case ENTITY -> "Выбор модели";
                    case ITEM -> "Выбор предмета";
                    default -> "Выбор звука";
                };
                Icon icon = kind == ParamType.ANIM ? Icon.FILM : (kind != ParamType.ENTITY && kind != ParamType.ITEM ? Icon.NOTE : Icon.CUBE);
                Dialog d = new Dialog(title, icon).width(Math.min(680, ui.vw - 30));
                boolean own = !gameSoundsOnly;
                gameSoundsOnly = false;
                AssetPicker.Body body = new AssetPicker.Body(kind, current, forEntity, own, id -> {
                    ProjectSounds.stop("__picker_preview");
                    d.close();
                    onPick.accept(id);
                });
                body.prefH = Math.min(340, ui.vh - 120);
                d.body.add(body);
                d.addButton("Отмена", Button.Style.DEFAULT, () -> {
                    ProjectSounds.stop("__picker_preview");
                    d.close();
                });
                d.addButton("Выбрать", Button.Style.PRIMARY, () -> {
                    AssetPicker.Item it = body.list.selectedItem();
                    if (it != null) {
                        ProjectSounds.stop("__picker_preview");
                        d.close();
                        onPick.accept(it.id);
                    }
                });
                d.onEnter = () -> {
                    AssetPicker.Item it = body.list.selectedItem();
                    if (it != null) {
                        d.close();
                        onPick.accept(it.id);
                    }
                };
                d.open();
                body.search.requestFocus();
            }
        }
    }

    private static final class Body extends Widget {
        final ParamType kind;
        final TextField search = new TextField().placeholder("Поиск по названию или ID").icon(Icon.SEARCH);
        final ListView<AssetPicker.Item> list;
        final ModelView view;
        final List<AssetPicker.Item> all = new ArrayList<>();
        String shown = "";
        String modelKey = "";

        Body(ParamType kind, String current, String entity, boolean ownSounds, Consumer<String> choose) {
            this.kind = kind;
            this.add(this.search);
            this.list = this.add(new ListView<>((c, it, x, y, w, h, sel, hov) -> {
                c.textEllipsized(it.title, x + 8, y + 3, w - 14, sel ? -1841682 : -6577995);
                c.textEllipsized(it.detail, x + 8, y + 13, w - 14, -9933435);
            }));
            this.list.rowHeight = 24;
            this.list.background = -15592678;
            this.list.onActivate = it -> choose.accept(it.id);
            this.view = kind != ParamType.SOUND && kind != ParamType.ITEM ? this.add(new ModelView()) : null;
            switch (kind) {
                case ANIM:
                    if (entity != null && !entity.isBlank()) {
                        this.showEntity(entity.replace("\"", "").trim());
                    }

                    if (!this.modelKey.isEmpty()) {
                        EntityLibrary.Look look = EntityLibrary.look(this.modelKey);
                        if (look != null && look.model() != null) {
                            for (String n : ModelLibrary.animations(look.model())) {
                                this.all.add(new AssetPicker.Item(n, n + "  · анимация этой модели", this.shown, new AssetPicker.OwnAnim(this.modelKey, n)));
                            }
                        }
                    }

                    for (ModelLibrary.ProjectAnim pa : ModelLibrary.projectAnimations(null)) {
                        this.all
                            .add(
                                new AssetPicker.Item(
                                    pa.shortId(),
                                    pa.shortId() + "  · моя анимация",
                                    pa.project() + String.format(Locale.ROOT, " · %.1f с", pa.seconds()) + (pa.loops() ? " ↻" : ""),
                                    new AssetPicker.OwnAnim(this.modelKey, pa.id())
                                )
                            );
                    }

                    for (Anims.Entry a : AssetIndex.animations()) {
                        this.all
                            .add(
                                new AssetPicker.Item(
                                    a.id(),
                                    a.fileName() + (a.id().contains("!") ? " · " + a.key() : ""),
                                    a.folder() + String.format(Locale.ROOT, " · %.1f с", a.seconds()) + (a.loops() ? " ↻" : ""),
                                    a
                                )
                            );
                    }

                    if (this.shown.isEmpty()) {
                        AssetIndex.Model m = AssetIndex.defaultPreviewModel();
                        if (m != null) {
                            this.view.setType(m.type());
                        }
                    }

                    this.view.emptyText = "Нет модели для превью";
                    break;
                case ENTITY:
                    for (EntityLibrary.Type t : EntityLibrary.all()) {
                        this.all.add(new AssetPicker.Item("spraute:" + t.name(), t.name() + "  · сущность проекта", "spraute:" + t.name(), t));
                    }

                    for (AssetIndex.Model m : AssetIndex.models()) {
                        this.all.add(new AssetPicker.Item(m.idString(), m.title(), m.idString(), m));
                    }
                    break;
                case ITEM:
                    for (net.minecraft.world.item.Item it : Regs.ITEM) {
                        String id = Regs.ITEM.getKey(it).toString();

                        String title;
                        try {
                            title = it.getDescription().getString();
                        } catch (RuntimeException var11) {
                            title = id;
                        }

                        this.all.add(new AssetPicker.Item(id, title, id, it));
                    }
                    break;
                default:
                    if (ownSounds) {
                        for (String name : AssetIndex.projectSounds.get()) {
                            this.all.add(new AssetPicker.Item(name, name, "свой звук", new AssetPicker.OwnSound(name)));
                        }
                    }

                    for (AssetIndex.Sound s : AssetIndex.sounds()) {
                        this.all.add(new AssetPicker.Item(s.id().toString(), s.id().getPath(), s.id().getNamespace(), s));
                    }
            }

            this.list.onSelect = this::preview;
            this.search.onChange = s -> this.refresh();
            this.refresh();
            String cur = current == null ? "" : current.replace("\"", "").trim();
            if (!cur.isEmpty()) {
                List<AssetPicker.Item> items = this.list.items();

                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).id.equals(cur)) {
                        this.list.select(i);
                        this.list.ensureVisible(i);
                        this.preview(items.get(i));
                        break;
                    }
                }
            }
        }

        void refresh() {
            String q = this.search.text();
            List<AssetPicker.Item> out = new ArrayList<>();
            AssetPicker.Item selected = this.list.selectedItem();

            for (AssetPicker.Item it : this.all) {
                if (AssetIndex.matches(q, it.id, it.title, it.detail)) {
                    out.add(it);
                }
            }

            this.list.setItems(out);
            if (this.list.selectedItem() == null && !out.isEmpty()) {
                this.list.select(selected != null && out.contains(selected) ? out.indexOf(selected) : 0);
            }
        }

        void showEntity(String id) {
            if (this.view != null) {
                if (id.startsWith("spraute:")) {
                    String n = id.substring(8);
                    if (n.equals("npc")) {
                        this.modelKey = "builtin/humanoid";
                        this.shown = id;
                        this.view.setType(Orbs.MODEL.get());
                    } else {
                        for (EntityLibrary.Type t : EntityLibrary.all()) {
                            if (t.name().equalsIgnoreCase(n)) {
                                String key = t.key();
                                this.modelKey = key;
                                this.shown = id;
                                this.view.setType(Orbs.MODEL.get(), e -> {
                                    if (e instanceof SprauteModelEntity me) {
                                        me.setModel(key);
                                    }
                                });
                                return;
                            }
                        }
                    }
                } else {
                    ResourceLocation rl = ResourceLocation.tryParse(id.contains(":") ? id : "spraute:" + id);
                    EntityType<?> tx = rl == null ? null : Regs.ENTITY_TYPE.getOptional(rl).orElse(null);
                    if (tx != null) {
                        this.view.setType(tx);
                        this.shown = rl.toString();
                    }
                }
            }
        }

        void preview(AssetPicker.Item it) {
            if (it != null) {
                if (it.data instanceof AssetPicker.OwnAnim oa && this.view != null) {
                    EntityLibrary.Look look = EntityLibrary.look(oa.key());
                    Optional<Animation> a = look != null ? look.animation(oa.name()) : Optional.empty();
                    a.ifPresent(an -> this.view.play(an.configure(), oa.name()));
                } else if (it.data instanceof Anims.Entry a && this.view != null) {
                    this.view.play(a.id(), "action", true, 1.0F);
                } else if (it.data instanceof AssetIndex.Model m && this.view != null) {
                    this.view.setType(m.type());
                } else if (it.data instanceof EntityLibrary.Type t && this.view != null) {
                    this.view.setType(Orbs.MODEL.get(), e -> {
                        if (e instanceof SprauteModelEntity me) {
                            me.setModel(t.key());
                        }
                    });
                } else if (it.data instanceof AssetPicker.OwnSound s) {
                    ProjectSounds.stop("__picker_preview");
                    ProjectSounds.play(AssetIndex.projectName.get(), s.name(), 0.0, 0.0, 0.0, false, 1.0F, 1.0F, false, "__picker_preview");
                } else if (it.data instanceof AssetIndex.Sound s) {
                    ProjectSounds.stop("__picker_preview");

                    try {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Mc.soundEvent(s.id()), 1.0F));
                    } catch (RuntimeException var10) {
                    }
                }
            }
        }

        @Override
        public void layout() {
            int listW = this.view == null ? this.w : this.w * 52 / 100;
            this.search.bounds(this.x, this.y, listW, 18);
            this.list.bounds(this.x, this.y + 22, listW, this.h - 22);
            if (this.view != null) {
                this.view.bounds(this.x + listW + 8, this.y, this.w - listW - 8, this.h);
            }

            super.layout();
        }

        @Override
        protected void paintOver(Canvas c, int mx, int my, float dt) {
            c.outline(this.list.x, this.list.y, this.list.w, this.list.h, -13749952);
            if (this.view != null) {
                c.outline(this.view.x, this.view.y, this.view.w, this.view.h, -13749952);
            }

            if (this.view != null && this.kind == ParamType.ANIM && !this.shown.isEmpty()) {
                c.textEllipsized("на модели: " + this.shown, this.view.x + 6, this.view.y + 5, this.view.w - 12, -9933435);
            }
        }
    }

    private record Item(String id, String title, String detail, Object data) {
    }

    private record OwnAnim(String key, String name) {
    }

    private record OwnSound(String name) {
    }
}
