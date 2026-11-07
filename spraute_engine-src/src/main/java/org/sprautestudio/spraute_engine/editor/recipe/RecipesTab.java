package org.sprautestudio.spraute_engine.editor.recipe;

import com.mojang.blaze3d.vertex.PoseStack;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.content.ProjectContent;
import org.sprautestudio.spraute_engine.editor.assets.AssetIndex;
import org.sprautestudio.spraute_engine.editor.assets.AssetPicker;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.recipe.RecipeDef;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Checkbox;
import org.sprautestudio.spraute_engine.ui.widgets.Dialog;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.ListView;
import org.sprautestudio.spraute_engine.ui.widgets.ScrollView;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public final class RecipesTab extends EditorTab {
    private static final int RESULT = 9;
    private static final String OTHER = "other";
    private static final String SORT_NAME = "name";
    private static final String SORT_ID = "id";
    private static final String SORT_MOD = "mod";
    private static final String SORT_TYPE = "type";
    private static final String SORT_STATUS = "status";
    private final Map<String, RecipeDef> files = new TreeMap<>();
    private final Set<String> deleted = new HashSet<>();
    private final Map<String, RecipesTab.Row> returning = new HashMap<>();
    private Map<String, Recipe<?>> game = new HashMap<>();
    private final Map<String, RecipesTab.Row> gameRows = new HashMap<>();
    private long gameStamp;
    private int stampWait;
    private boolean dirty;
    private String notice = "";
    private RecipeDef cur;
    private int slot = -1;
    private String filterMod = "";
    private String filterType = "";
    private String filterStatus = "";
    private String sort = "name";
    private boolean descending;
    private int total;
    public Runnable onSave;
    private final TextField search = new TextField().placeholder("Поиск по названию или id").icon(Icon.SEARCH);
    private final Dropdown<String> modBox;
    private final Dropdown<String> typeBox;
    private final Dropdown<String> statusBox;
    private final Dropdown<String> sortBox;
    private final Button sortDir;
    private final ListView<RecipesTab.Row> list;
    private final RecipesTab.Preview preview = new RecipesTab.Preview();
    private final RecipesTab.Form form = new RecipesTab.Form();
    private final ScrollView inspector;
    private final List<Button> tools = new ArrayList<>();
    private static Map<Set<String>, String> tagsByItems;
    private static List<String> tagNames;
    private final Map<String, ItemStack[]> options = new HashMap<>();
    private static final ItemStack[] NONE = new ItemStack[0];
    private static final ItemStack MISSING = new ItemStack(Items.BARRIER);
    private static final int LABEL_W = 96;
    private static final int TOOLBAR = 28;
    private static final int LEFT_W = 330;
    private static final int RIGHT_W = 300;
    private static final int FILTERS = 70;

    public RecipesTab(Project project) {
        super(project, null);
        this.readFiles();
        this.list = new ListView<>(this::paintRow);
        this.list.rowHeight = 24;
        this.list.background = -14934490;
        this.list.emptyText = "Рецептов нет. Они видны, когда открыт мир: игра хранит их в нём.";
        this.list.onSelect = r -> this.select(r.id);
        this.add(this.list);
        this.add(this.search);
        this.search.onChange = s -> this.refreshList();
        this.modBox = this.add(new Dropdown<>(this::mods, () -> this.filterMod, m -> m.isEmpty() ? "Все моды" : m, m -> {
            this.filterMod = m;
            this.refreshList();
        }));
        this.modBox.tooltip("Из какого мода");
        this.typeBox = this.add(
            new Dropdown<>(
                RecipesTab::typeFilters, () -> this.filterType, t -> t.isEmpty() ? "Все виды" : (t.equals("other") ? "Другие виды" : typeTitle(t)), t -> {
                    this.filterType = t;
                    this.refreshList();
                }
            )
        );
        this.typeBox.tooltip("Вид рецепта");
        this.statusBox = this.add(
            new Dropdown<>(
                () -> List.of(
                    "", "touched", RecipesTab.Status.CHANGED.name(), RecipesTab.Status.MINE.name(), RecipesTab.Status.OFF.name(), RecipesTab.Status.GAME.name()
                ),
                () -> this.filterStatus,
                RecipesTab::statusFilterTitle,
                s -> {
                    this.filterStatus = s;
                    this.refreshList();
                }
            )
        );
        this.statusBox.tooltip("Что с рецептом в этом проекте");
        this.sortBox = this.add(new Dropdown<>(() -> List.of("name", "id", "mod", "type", "status"), () -> this.sort, RecipesTab::sortTitle, s -> {
            this.sort = s;
            this.refreshList();
        }));
        this.sortBox.tooltip("Сортировка");
        this.sortDir = this.add(Button.icon(Icon.CHEVRON_DOWN, "Порядок сортировки", () -> {
            this.descending = !this.descending;
            this.refreshList();
        }));
        this.sortDir.toggle(() -> this.descending);
        this.add(this.preview);
        this.inspector = this.add(new ScrollView(this.form));
        this.tool("Новый", Icon.PLUS, Button.Style.PRIMARY, "Новый рецепт проекта", this::newRecipe, null);
        this.tool("Копия", Icon.COPY, Button.Style.GHOST, "Новый рецепт на основе выбранного", this::copyRecipe, () -> this.cur != null && this.cur.editable());
        Button off = this.tool(null, Icon.STOP, Button.Style.GHOST, "Отключить рецепт в игре или вернуть его", this::toggleOff, () -> this.cur != null);
        off.autoWidth();
        this.tool(
            "Сбросить",
            Icon.UNDO,
            Button.Style.GHOST,
            "Вернуть рецепт игры или удалить свой",
            this::resetRecipe,
            () -> this.cur != null && this.files.containsKey(this.cur.id)
        );
        this.tool("Сохранить", Icon.SAVE, Button.Style.PRIMARY, "Сохранить и сразу применить в мире (Ctrl+S)", () -> {
            if (this.onSave != null) {
                this.onSave.run();
            }
        }, () -> this.dirty);
        this.refreshGame();
        this.buildForm();
    }

    private Button tool(String text, Icon icon, Button.Style style, String tip, Runnable r, BooleanSupplier enabled) {
        Button b = text != null ? new Button(text, r) : new Button(() -> this.cur != null && this.cur.disabled ? "Включить" : "Отключить", r);
        this.add(b.icon(icon).style(style));
        b.tooltip(tip);
        b.autoWidth();
        if (enabled != null) {
            b.enabledWhen(enabled);
        }

        this.tools.add(b);
        return b;
    }

    private Path dir() {
        return this.project.dir.resolve("recipes");
    }

    private Path fileOf(String id) {
        int colon = id.indexOf(58);
        return this.dir().resolve(id.substring(0, colon)).resolve(id.substring(colon + 1) + ".json");
    }

    private void readFiles() {
        Path root = this.dir();
        if (Files.isDirectory(root)) {
            try (Stream<Path> all = Files.walk(root)) {
                for (Path f : (Iterable<Path>) all.filter(p -> p.toString().endsWith(".json")).sorted()::iterator) {
                    String rel = root.relativize(f).toString().replace('\\', '/');
                    int slash = rel.indexOf(47);
                    if (slash > 0) {
                        String id = rel.substring(0, slash) + ":" + rel.substring(slash + 1, rel.length() - 5);

                        try {
                            this.files.put(id, RecipeDef.fromJson(id, Files.readString(f, StandardCharsets.UTF_8)));
                        } catch (IOException | RuntimeException var10) {
                            SprauteEngine.LOGGER.warn("Spraute: cannot read the recipe {}", f, var10);
                        }
                    }
                }
            } catch (IOException var12) {
                SprauteEngine.LOGGER.warn("Spraute: cannot list the recipes of {}", this.project.name(), var12);
            }
        }
    }

    @Override
    public void save() throws IOException {
        for (String id : this.deleted) {
            if (!this.files.containsKey(id)) {
                Files.deleteIfExists(this.fileOf(id));
            }
        }

        this.deleted.clear();
        int unfinished = 0;
        String firstBad = null;

        for (RecipeDef d : this.files.values()) {
            String problem = d.problem();
            if (problem != null) {
                unfinished++;
                if (firstBad == null) {
                    firstBad = d.id + ": " + problem;
                }
            } else {
                Path f = this.fileOf(d.id);
                Files.createDirectories(f.getParent());
                Files.writeString(f, d.toJson(), StandardCharsets.UTF_8);
            }
        }

        ProjectContent.removeEmpty(this.dir());
        this.dirty = unfinished > 0;
        this.notice = unfinished > 0 ? "Не сохранено (" + unfinished + "): " + firstBad : "";
        this.refreshList();
    }

    private static Collection<Recipe<?>> worldRecipes() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return (Collection<Recipe<?>>)(connection == null ? List.of() : connection.getRecipeManager().getRecipes());
    }

    private static long stamp(Collection<Recipe<?>> recipes) {
        long s = recipes.size();

        for (Recipe<?> r : recipes) {
            s = s * 31L + System.identityHashCode(r);
        }

        return s;
    }

    private void refreshGame() {
        Collection<Recipe<?>> recipes = worldRecipes();
        this.gameStamp = stamp(recipes);
        this.game = new HashMap<>();
        this.gameRows.clear();

        for (Recipe<?> r : recipes) {
            String id = r.getId().toString();
            this.game.put(id, r);
            ItemStack icon = resultOf(r);
            String type = typeOf(r);
            this.gameRows.put(id, new RecipesTab.Row(id, nameOf(icon, id), type != null ? type : r.getType().toString(), icon, RecipesTab.Status.GAME));
        }

        this.returning.keySet().removeIf(this.game::containsKey);
        this.refreshList();
        if (this.cur != null && !this.files.containsKey(this.cur.id)) {
            RecipeDef now = this.fromGame(this.cur.id);
            if (now != null) {
                this.cur = now;
                this.buildForm();
            }
        }
    }

    private static ItemStack resultOf(Recipe<?> r) {
        try {
            ItemStack s = Mc.recipeResult(r, Minecraft.getInstance().level);
            return s == null ? ItemStack.EMPTY : s;
        } catch (RuntimeException var2) {
            return ItemStack.EMPTY;
        }
    }

    private static String nameOf(ItemStack icon, String id) {
        return icon.isEmpty() ? id.substring(id.indexOf(58) + 1) : icon.getHoverName().getString();
    }

    private static String itemId(ItemStack s) {
        return s.isEmpty() ? "" : Regs.ITEM.getKey(s.getItem()).toString();
    }

    private static String typeOf(Recipe<?> r) {
        RecipeSerializer<?> s = r.getSerializer();
        if (s == RecipeSerializer.SHAPED_RECIPE) {
            return "minecraft:crafting_shaped";
        } else if (s == RecipeSerializer.SHAPELESS_RECIPE) {
            return "minecraft:crafting_shapeless";
        } else if (s == RecipeSerializer.SMELTING_RECIPE) {
            return "minecraft:smelting";
        } else if (s == RecipeSerializer.BLASTING_RECIPE) {
            return "minecraft:blasting";
        } else if (s == RecipeSerializer.SMOKING_RECIPE) {
            return "minecraft:smoking";
        } else if (s == RecipeSerializer.CAMPFIRE_COOKING_RECIPE) {
            return "minecraft:campfire_cooking";
        } else {
            return s == RecipeSerializer.STONECUTTER ? "minecraft:stonecutting" : null;
        }
    }

    private RecipeDef fromGame(String id) {
        Recipe<?> r = this.game.get(id);
        if (r == null) {
            return null;
        } else {
            ItemStack out = resultOf(r);
            String type = typeOf(r);
            if (type == null) {
                return RecipeDef.other(id, r.getType().toString(), itemId(out));
            } else {
                RecipeDef d = RecipeDef.create(id, type);
                d.group = r.getGroup();
                d.result = itemId(out);
                d.count = Math.max(1, out.getCount());
                if (out.hasTag()) {
                    d.resultNbt = out.getTag().toString();
                }

                NonNullList<Ingredient> ingredients = r.getIngredients();
                if (r instanceof ShapedRecipe shaped) {
                    int w = shaped.getWidth();
                    if (w > 3 || shaped.getHeight() > 3) {
                        return RecipeDef.other(id, type, d.result);
                    }

                    for (int i = 0; i < ingredients.size(); i++) {
                        d.slots[i / w * 3 + i % w] = specOf(ingredients.get(i));
                    }
                } else {
                    if (ingredients.size() > 9) {
                        return RecipeDef.other(id, type, d.result);
                    }

                    for (int i = 0; i < ingredients.size(); i++) {
                        d.slots[i] = specOf(ingredients.get(i));
                    }
                }

                if (r instanceof AbstractCookingRecipe cooking) {
                    d.experience = cooking.getExperience();
                    d.cookingTime = cooking.getCookingTime();
                }

                return d;
            }
        }
    }

    private static String specOf(Ingredient ing) {
        if (ing.isEmpty()) {
            return "";
        } else {
            String spec = RecipeDef.specOf(ing.toJson());
            if (spec.contains("|") && !spec.startsWith("{") && !spec.startsWith("[")) {
                String tag = tagsByItems().get(new HashSet<>(List.of(spec.split("\\|"))));
                return tag != null ? "#" + tag : spec;
            } else {
                return spec;
            }
        }
    }

    private static Map<Set<String>, String> tagsByItems() {
        if (tagsByItems != null) {
            return tagsByItems;
        } else {
            Map<Set<String>, String> out = new HashMap<>();
            TreeSet<String> names = new TreeSet<>();
            Regs.ITEM.getTags().forEach(pair -> {
                String name = pair.getFirst().location().toString();
                names.add(name);
                Set<String> items = new HashSet<>();
                pair.getSecond().forEach(h -> items.add(Regs.ITEM.getKey(h.value()).toString()));
                out.merge(items, name, (a, b) -> a.length() <= b.length() ? a : b);
            });
            tagNames = new ArrayList<>(names);
            tagsByItems = out;
            return out;
        }
    }

    private static List<String> tagNames() {
        tagsByItems();
        return tagNames;
    }

    private static List<String> typeFilters() {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(RecipeDef.TYPES);
        out.add("other");
        return out;
    }

    private static String statusFilterTitle(String s) {
        if (s.isEmpty()) {
            return "Любые";
        } else if (s.equals("touched")) {
            return "Все изменения проекта";
        } else {
            return switch (RecipesTab.Status.valueOf(s)) {
                case GAME -> "Без изменений";
                case CHANGED -> "Изменённые";
                case MINE -> "Свои";
                case OFF -> "Отключённые";
            };
        }
    }

    private static String sortTitle(String s) {
        return switch (s) {
            case "id" -> "По id";
            case "mod" -> "По моду";
            case "type" -> "По виду";
            case "status" -> "По состоянию";
            default -> "По названию";
        };
    }

    public static String typeTitle(String t) {
        return switch (t) {
            case "minecraft:crafting_shaped" -> "Верстак: по форме";
            case "minecraft:crafting_shapeless" -> "Верстак: без формы";
            case "minecraft:smelting" -> "Печь";
            case "minecraft:blasting" -> "Плавильная печь";
            case "minecraft:smoking" -> "Коптильня";
            case "minecraft:campfire_cooking" -> "Костёр";
            case "minecraft:stonecutting" -> "Камнерез";
            default -> t.replace("minecraft:", "");
        };
    }

    private List<String> mods() {
        TreeSet<String> names = new TreeSet<>();

        for (RecipesTab.Row r : this.allRows().values()) {
            names.add(r.mod);
        }

        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(names);
        return out;
    }

    private Map<String, RecipesTab.Row> allRows() {
        Map<String, RecipesTab.Row> out = new LinkedHashMap<>(this.gameRows);
        out.putAll(this.returning);

        for (String id : this.deleted) {
            if (id.startsWith("spraute:")) {
                out.remove(id);
            }
        }

        for (RecipeDef d : this.files.values()) {
            ItemStack icon = stackOf(d.result);
            RecipesTab.Status status = d.disabled ? RecipesTab.Status.OFF : (d.own() ? RecipesTab.Status.MINE : RecipesTab.Status.CHANGED);
            out.put(d.id, new RecipesTab.Row(d.id, nameOf(icon, d.id), d.type, icon, status));
        }

        return out;
    }

    private void refreshList() {
        Map<String, RecipesTab.Row> all = this.allRows();
        this.total = all.size();
        String q = this.search.text();
        List<RecipesTab.Row> shown = new ArrayList<>();

        for (RecipesTab.Row r : all.values()) {
            if ((this.filterMod.isEmpty() || r.mod.equals(this.filterMod))
                && (this.filterType.equals("other") ? !RecipeDef.TYPES.contains(r.type) : this.filterType.isEmpty() || r.type.equals(this.filterType))
                && (
                    this.filterStatus.equals("touched")
                        ? r.status != RecipesTab.Status.GAME
                        : this.filterStatus.isEmpty() || r.status.name().equals(this.filterStatus)
                )
                && AssetIndex.matches(q, r.id, r.name)) {
                shown.add(r);
            }
        }

        Comparator<String> text = String.CASE_INSENSITIVE_ORDER;
        Comparator<RecipesTab.Row> byName = Comparator.<RecipesTab.Row, String>comparing(r -> r.name, text).thenComparing(r -> r.id);
        String var7 = this.sort;

        Comparator<RecipesTab.Row> order = switch (var7) {
            case "id" -> Comparator.comparing(r -> r.id);
            case "mod" -> Comparator.<RecipesTab.Row, String>comparing(r -> r.mod, text).thenComparing(byName);
            case "type" -> Comparator.<RecipesTab.Row, String>comparing(r -> r.type, text).thenComparing(byName);
            case "status" -> Comparator.<RecipesTab.Row, Integer>comparing(r -> -r.status.ordinal()).thenComparing(byName);
            default -> byName;
        };
        shown.sort(this.descending ? order.reversed() : order);
        this.list.setItems(shown);
    }

    private void paintRow(Canvas c, RecipesTab.Row r, int x, int y, int w, int h, boolean selected, boolean hovered) {
        c.flush();
        if (r.icon.isEmpty()) {
            c.icon(Icon.CUBE, x + 8, y + 7, -9933435);
        } else {
            c.g.renderItem(r.icon, x + 4, y + 4);
        }

        int right = w - 6;
        if (r.status != RecipesTab.Status.GAME) {
            int bw = c.width(r.status.title);
            c.text(r.status.title, x + right - bw, y + 4, r.status.color);
            right -= bw + 6;
        }

        c.textEllipsized(r.name, x + 24, y + 4, right - 24, r.status == RecipesTab.Status.OFF ? -9933435 : -1841682);
        c.textEllipsized(r.id, x + 24, y + 14, w - 30, -9933435);
    }

    private void select(String id) {
        RecipeDef d = this.files.get(id);
        this.cur = d != null ? d : this.fromGame(id);
        this.slot = -1;
        this.buildForm();
    }

    private void changed() {
        if (this.cur != null) {
            this.files.putIfAbsent(this.cur.id, this.cur);
            this.deleted.remove(this.cur.id);
            this.dirty = true;
            this.notice = "";
            this.refreshList();
        }
    }

    private String checkNewName(String v) {
        String name = v.trim().toLowerCase(Locale.ROOT);
        if (name.matches("[a-z0-9_./-]+") && !name.contains("..") && !name.startsWith("/") && !name.endsWith("/")) {
            String id = "spraute:" + name;
            return !this.files.containsKey(id) && (!this.game.containsKey(id) || this.deleted.contains(id)) ? null : "Такой рецепт уже есть";
        } else {
            return "Латиница, цифры и _ . / -";
        }
    }

    private String freeName(String base) {
        String name = base;
        int n = 2;

        while (this.checkNewName(name) != null) {
            name = base + "_" + n++;
        }

        return name;
    }

    private void put(RecipeDef d) {
        this.files.put(d.id, d);
        this.deleted.remove(d.id);
        this.dirty = true;
        this.notice = "";
        this.cur = d;
        this.slot = -1;
        this.filterStatus = "";
        this.filterMod = "";
        this.filterType = "";
        this.search.setText("");
        this.refreshList();

        for (int i = 0; i < this.list.items().size(); i++) {
            if (this.list.items().get(i).id.equals(d.id)) {
                this.list.select(i);
                break;
            }
        }

        this.buildForm();
    }

    private void newRecipe() {
        Dialog.prompt(
            "Новый рецепт",
            "Имя (латиницей)",
            this.freeName("recipe"),
            this::checkNewName,
            v -> this.put(RecipeDef.create("spraute:" + v.trim().toLowerCase(Locale.ROOT), "minecraft:crafting_shaped"))
        );
    }

    private void copyRecipe() {
        if (this.cur != null && this.cur.editable()) {
            String base = this.cur.id.substring(this.cur.id.indexOf(58) + 1).replaceAll("[^a-z0-9_./-]", "_");
            RecipeDef from = this.cur;
            Dialog.prompt(
                "Копия рецепта",
                "Имя (латиницей)",
                this.freeName(base),
                this::checkNewName,
                v -> this.put(from.copy("spraute:" + v.trim().toLowerCase(Locale.ROOT)))
            );
        }
    }

    private void renameRecipe() {
        if (this.cur != null && this.cur.own()) {
            RecipeDef from = this.cur;
            Dialog.prompt("Имя рецепта", "Имя (латиницей)", from.id.substring(from.id.indexOf(58) + 1), this::checkNewName, v -> {
                String id = "spraute:" + v.trim().toLowerCase(Locale.ROOT);
                this.files.remove(from.id);
                this.deleted.add(from.id);
                from.id = id;
                this.put(from);
            });
        }
    }

    private void toggleOff() {
        if (this.cur != null) {
            if (this.cur.disabled && this.cur.placeholder()) {
                this.drop(this.cur);
            } else {
                this.cur.disabled = !this.cur.disabled;
                this.changed();
                this.buildForm();
            }
        }
    }

    private void resetRecipe() {
        if (this.cur != null && this.files.containsKey(this.cur.id)) {
            if (this.cur.own()) {
                RecipeDef d = this.cur;
                Dialog.confirm("Удалить рецепт", "Рецепт «" + d.id + "» будет удалён из проекта.", "Удалить", true, () -> this.drop(d));
            } else {
                this.drop(this.cur);
            }
        }
    }

    private void drop(RecipeDef d) {
        this.files.remove(d.id);
        this.deleted.add(d.id);
        this.dirty = true;
        this.notice = "";
        if (!d.own() && !this.game.containsKey(d.id)) {
            ItemStack icon = stackOf(d.result);
            this.returning.put(d.id, new RecipesTab.Row(d.id, nameOf(icon, d.id), d.type, icon, RecipesTab.Status.GAME));
        }

        this.cur = d.own() ? null : this.fromGame(d.id);
        this.slot = -1;
        this.refreshList();
        this.buildForm();
    }

    private void pick(int index) {
        String current = index == 9 ? this.cur.result : this.cur.slots[index];
        AssetPicker.open(ParamType.ITEM, current, v -> {
            if (index == 9) {
                this.cur.result = v;
            } else {
                this.cur.slots[index] = v;
            }

            this.changed();
            this.buildForm();
        });
    }

    private ItemStack[] optionsOf(String spec) {
        return spec != null && !spec.isBlank() ? this.options.computeIfAbsent(spec, s -> {
            try {
                ItemStack[] items = Ingredient.fromJson(RecipeDef.ingredientJson(s)).getItems();
                return items.length == 0 ? new ItemStack[]{MISSING} : items;
            } catch (RuntimeException var2) {
                return new ItemStack[]{MISSING};
            }
        }) : NONE;
    }

    private static ItemStack stackOf(String id) {
        if (id != null && !id.isEmpty()) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            return key != null && Regs.ITEM.containsKey(key) ? new ItemStack(Regs.ITEM.get(key)) : MISSING;
        } else {
            return ItemStack.EMPTY;
        }
    }

    private static String stackTitle(ItemStack s) {
        return s == MISSING ? "Предмет не найден" : s.getHoverName().getString() + "\n" + itemId(s);
    }

    private void buildForm() {
        this.form.clear();
        if (this.cur == null) {
            this.form.note("Рецепт не выбран");
        } else if (!this.cur.editable()) {
            this.otherForm();
        } else {
            this.recipeForm();
        }

        if (this.w > 0) {
            this.layout();
        }
    }

    private void otherForm() {
        this.form.header("Рецепт");
        this.form.note(this.cur.id);
        this.form.note("Вид: " + typeTitle(this.cur.type));
        this.form.check("Отключён", "В игре рецепта не будет", () -> this.cur.disabled, v -> this.toggleOff());
    }

    private void recipeForm() {
        RecipeDef d = this.cur;
        this.form.header("Рецепт");
        this.form.note(d.id);
        if (d.own()) {
            this.form.button("Переименовать…", this::renameRecipe);
        }

        this.form.choice("Вид", null, () -> RecipeDef.TYPES, () -> d.type, RecipesTab::typeTitle, v -> {
            d.setType(v);
            this.slot = -1;
            this.changed();
            this.buildForm();
        });
        this.form.check("Отключён", "В игре рецепта не будет", () -> d.disabled, v -> {
            d.disabled = v;
            this.changed();
        });
        this.form.text("Группа", "Рецепты одной группы книга рецептов показывает вместе", () -> d.group, v -> {
            d.group = v.trim();
            this.changed();
        });
        this.form.header("Результат");
        this.form.button(() -> d.result.isEmpty() ? "выбрать предмет…" : d.result.replace("minecraft:", ""), () -> {
            this.slot = 9;
            this.pick(9);
        });
        if (!RecipeDef.cooking(d.type)) {
            this.form.number("Сколько штук", null, () -> d.count, v -> {
                d.count = Math.max(1, Math.min(64, (int)Math.round(v)));
                this.changed();
            });
        }

        if (RecipeDef.cooking(d.type)) {
            this.form.header("Печь");
            this.form.number("Время (тиков)", "20 тиков — одна секунда", () -> d.cookingTime, v -> {
                d.cookingTime = Math.max(1, (int)Math.round(v));
                this.changed();
            });
            this.form.number("Опыт", null, () -> d.experience, v -> {
                d.experience = (float)Math.max(0.0, v);
                this.changed();
            });
        }

        if (this.slot >= 0 && this.slot < 9) {
            this.slotForm(d);
        } else {
            this.form.note("Щёлкните ячейку, чтобы выбрать тег или варианты");
        }
    }

    private void slotForm(RecipeDef d) {
        int i = this.slot;
        this.form.header("Ячейка");
        this.form
            .button(
                () -> d.slots[i].isEmpty()
                    ? "выбрать предмет…"
                    : (
                        !d.slots[i].startsWith("#") && !d.slots[i].contains("|") && !d.slots[i].startsWith("{")
                            ? d.slots[i].replace("minecraft:", "")
                            : "заменить предметом…"
                    ),
                () -> this.pick(i)
            );
        this.form.tag("Тег", "Подойдёт любой предмет этого тега", tag -> {
            d.slots[i] = "#" + tag;
            this.changed();
            this.buildForm();
        }, () -> d.slots[i].startsWith("#") ? d.slots[i].substring(1) : "");
        this.form.text("Точно", "id, #тег или варианты через |: minecraft:oak_log|#minecraft:planks", () -> d.slots[i], v -> {
            d.slots[i] = v.trim();
            this.changed();
        });
    }

    private static String fmt(double v) {
        double r = Math.round(v * 100.0) / 100.0;
        return r == (long)r ? String.valueOf((long)r) : String.valueOf(r);
    }

    @Override
    public void layout() {
        int bx = this.x + 6;

        for (Button b : this.tools) {
            b.bounds(bx, this.y + 4, b.prefW, 20);
            bx += b.prefW + 4;
        }

        int lw = Math.min(330, this.w * 2 / 5);
        int iw = Math.min(300, this.w / 3);
        int top = this.y + 28;
        int half = (lw - 16) / 2;
        this.search.bounds(this.x + 6, top + 4, lw - 12, 18);
        this.modBox.bounds(this.x + 6, top + 26, half, 18);
        this.typeBox.bounds(this.x + 10 + half, top + 26, half, 18);
        this.statusBox.bounds(this.x + 6, top + 48, half, 18);
        this.sortBox.bounds(this.x + 10 + half, top + 48, half - 22, 18);
        this.sortDir.bounds(this.x + lw - 24, top + 48, 18, 18);
        this.list.bounds(this.x, top + 70, lw, this.h - 28 - 70 - 16);
        this.preview.bounds(this.x + lw, top, this.w - lw - iw, this.h - 28);
        this.inspector.bounds(this.x + this.w - iw, top, iw, this.h - 28);
        this.form.layout();
        super.layout();
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.fill(this.x, this.y, this.w, this.h, -15461093);
        c.fill(this.x, this.y, this.w, 28, -14934490);
        c.fill(this.x, this.y + 28 - 1, this.w, 1, -13749952);
        int lw = this.list.w;
        c.fill(this.x, this.y + 28, lw, this.h - 28, -14934490);
        c.fill(this.x + lw - 1, this.y + 28, 1, this.h - 28, -13749952);
        c.fill(this.inspector.x, this.y + 28, this.inspector.w, this.h - 28, -14934490);
        c.fill(this.inspector.x, this.y + 28, 1, this.h - 28, -13749952);
        c.textEllipsized("Показано " + this.list.items().size() + " из " + this.total, this.x + 8, this.y + this.h - 12, lw - 16, -9933435);
        if (!this.notice.isEmpty()) {
            Button last = this.tools.get(this.tools.size() - 1);
            int nx = last.x + last.w + 10;
            c.textEllipsized(this.notice, nx, this.y + 10, this.x + this.w - nx - 8, -870325);
        }
    }

    @Override
    public String title() {
        return "Рецепты";
    }

    @Override
    public Icon icon() {
        return Icon.LIST;
    }

    @Override
    public int iconColor() {
        return -6501275;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
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
        this.search.requestFocus();
    }

    private final class Form extends Widget {
        void header(final String text) {
            this.add(new Widget() {
                {
                    this.prefH = 24;
                }

                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    c.fill(this.x + 8, this.y + 7, this.w - 16, 1, -13749952);
                    c.text(text.toUpperCase(), this.x + 10, this.y + 13, -7639809);
                }
            });
        }

        void note(final String text) {
            this.add(new Widget() {
                {
                    this.prefH = 14;
                }

                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    c.textEllipsized(text, this.x + 10, this.y + 3, this.w - 16, -9933435);
                }
            }).tooltip(text);
        }

        RecipesTab.FormRow row(String label, String doc) {
            RecipesTab.FormRow r = this.add(new RecipesTab.FormRow(label));
            if (doc != null) {
                r.tooltip(doc);
            }

            return r;
        }

        void text(String label, String doc, Supplier<String> get, Consumer<String> set) {
            RecipesTab.FormRow r = this.row(label, doc);
            TextField f = new TextField();
            f.setText(get.get());
            f.onChange = set;
            r.editor = r.add(f);
        }

        void number(String label, String doc, DoubleSupplier get, DoubleConsumer set) {
            RecipesTab.FormRow r = this.row(label, doc);
            TextField f = new TextField();
            f.setText(RecipesTab.fmt(get.getAsDouble()));
            f.filter = ch -> Character.isDigit(ch) || ch == '.';
            f.onChange = s -> {
                try {
                    set.accept(Double.parseDouble(s.trim()));
                } catch (NumberFormatException var3x) {
                }
            };
            r.editor = r.add(f);
        }

        void choice(String label, String doc, Supplier<List<String>> options, Supplier<String> get, Function<String, String> labeler, Consumer<String> set) {
            RecipesTab.FormRow r = this.row(label, doc);
            r.editor = r.add(new Dropdown<>(options, get, labeler, set));
        }

        void tag(String label, String doc, Consumer<String> set, Supplier<String> get) {
            RecipesTab.FormRow r = this.row(label, doc);
            r.editor = r.add(new Dropdown<>(RecipesTab::tagNames, get, t -> t.replace("minecraft:", ""), set).searchable());
        }

        void check(String label, String doc, BooleanSupplier get, Consumer<Boolean> set) {
            RecipesTab.FormRow r = this.row(label, doc);
            r.editor = r.add(new Checkbox("", get, set));
        }

        void button(String text, Runnable run) {
            this.button(() -> text, run);
        }

        void button(Supplier<String> text, Runnable run) {
            RecipesTab.FormRow r = this.row("", null);
            r.wide = true;
            r.editor = r.add(new Button(text, run).icon(Icon.CUBE).style(Button.Style.GHOST));
        }

        @Override
        public void layout() {
            int cy = this.y;

            for (Widget c : this.children()) {
                c.bounds(this.x, cy, this.w, c.prefH);
                c.layout();
                cy += c.prefH;
            }

            this.prefH = cy - this.y + 8;
        }
    }

    private static final class FormRow extends Widget {
        final String label;
        Widget editor;
        boolean wide;

        FormRow(String label) {
            this.label = label;
            this.prefH = 22;
        }

        @Override
        public void layout() {
            if (this.editor != null) {
                int left = this.wide ? 10 : 96;
                this.editor.bounds(this.x + left, this.y + 2, this.w - left - 10, 18);
                this.editor.layout();
            }
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.textEllipsized(this.label, this.x + 10, this.y + 7, 82, -6577995);
        }
    }

    private final class Preview extends Widget implements UI.CursorProvider {
        private static final int S = 30;
        private static final int GAP = 2;
        private final int[] cellX = new int[10];
        private final int[] cellY = new int[10];

        private boolean usesCell(int i) {
            return RecipesTab.this.cur != null && RecipesTab.this.cur.editable() ? i == 9 || i == 0 || RecipeDef.crafting(RecipesTab.this.cur.type) : false;
        }

        private void place() {
            boolean grid = RecipesTab.this.cur != null && RecipeDef.crafting(RecipesTab.this.cur.type);
            int inW = grid ? 94 : 30;
            int arrow = 44;
            int total = inW + arrow + 30;
            int ox = this.x + (this.w - total) / 2;
            int oy = this.y + Math.max(30, (this.h - inW) / 2 - 16);

            for (int i = 0; i < 9; i++) {
                this.cellX[i] = ox + i % 3 * 32;
                this.cellY[i] = oy + i / 3 * 32;
            }

            this.cellX[9] = ox + inW + arrow;
            this.cellY[9] = oy + (inW - 30) / 2;
        }

        private int cellAt(double mx, double my) {
            for (int i = 0; i <= 9; i++) {
                if (this.usesCell(i) && mx >= this.cellX[i] && mx < this.cellX[i] + 30 && my >= this.cellY[i] && my < this.cellY[i] + 30) {
                    return i;
                }
            }

            return -1;
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.gradient(this.x, this.y, this.w, this.h, -14868432, -15921644);
            if (--RecipesTab.this.stampWait <= 0) {
                RecipesTab.this.stampWait = 30;
                if (RecipesTab.stamp(RecipesTab.worldRecipes()) != RecipesTab.this.gameStamp) {
                    RecipesTab.this.refreshGame();
                }
            }

            if (RecipesTab.this.cur == null) {
                c.textCentered(
                    RecipesTab.this.game.isEmpty() ? "Откройте мир, чтобы увидеть рецепты" : "Выберите рецепт слева или создайте новый",
                    this.x + this.w / 2,
                    this.y + this.h / 2 - 4,
                    -9933435
                );
            } else {
                this.place();
                c.textCentered(
                    RecipesTab.typeTitle(RecipesTab.this.cur.type), this.x + this.w / 2, this.y + 10, RecipesTab.this.cur.disabled ? -9933435 : -1841682
                );
                c.textCentered(RecipesTab.this.cur.id, this.x + this.w / 2, this.y + 20, -9933435);
                if (!RecipesTab.this.cur.editable()) {
                    ItemStack out = RecipesTab.stackOf(RecipesTab.this.cur.result);
                    if (!out.isEmpty()) {
                        c.flush();
                        c.g.renderItem(out, this.x + this.w / 2 - 8, this.y + this.h / 2 - 34);
                    }

                    c.textCentered("Этот вид рецепта здесь не редактируется", this.x + this.w / 2, this.y + this.h / 2 - 10, -6577995);
                    c.textCentered("Его можно только отключить", this.x + this.w / 2, this.y + this.h / 2 + 2, -9933435);
                } else {
                    int hovered = this.isHovered() ? this.cellAt(mx, my) : -1;

                    for (int i = 0; i <= 9; i++) {
                        if (this.usesCell(i)) {
                            this.paintCell(c, i, hovered == i);
                        }
                    }

                    int ax = this.cellX[9] - 34;
                    int ay = this.cellY[9] + 15;
                    c.line(ax, ay, ax + 24, ay, 2.0F, -6577995);
                    c.line(ax + 17, ay - 7, ax + 24, ay, 2.0F, -6577995);
                    c.line(ax + 17, ay + 7, ax + 24, ay, 2.0F, -6577995);
                    int below = this.cellY[RecipeDef.crafting(RecipesTab.this.cur.type) ? 6 : 0] + 30 + 14;
                    if (RecipeDef.cooking(RecipesTab.this.cur.type)) {
                        String info = RecipesTab.fmt(RecipesTab.this.cur.cookingTime / 20.0)
                            + " с · опыт "
                            + RecipesTab.fmt((double)RecipesTab.this.cur.experience);
                        c.textCentered(info, this.x + this.w / 2, below, -6577995);
                        below += 12;
                    }

                    if (RecipesTab.this.cur.disabled) {
                        c.textCentered("Рецепт отключён: в игре его нет", this.x + this.w / 2, below, -1023878);
                    }

                    String problem = RecipesTab.this.cur.problem();
                    if (problem != null) {
                        c.textCentered(problem, this.x + this.w / 2, below, -870325);
                    }

                    c.textEllipsized("Щелчок — выбрать, правая кнопка — очистить", this.x + 8, this.y + this.h - 14, this.w - 16, -9933435);
                }
            }
        }

        private void paintCell(Canvas c, int i, boolean hovered) {
            int cx = this.cellX[i];
            int cy = this.cellY[i];
            int fill = i == RecipesTab.this.slot ? 1435200767 : (hovered ? -14210506 : -15592678);
            c.roundBorder(cx, cy, 30, 30, 3, fill, i == RecipesTab.this.slot ? -7639809 : -13749952);
            ItemStack stack = this.stackAt(i);
            if (!stack.isEmpty()) {
                c.flush();
                PoseStack pose = c.g.pose();
                pose.pushPose();
                pose.translate((float)(cx + 3), (float)(cy + 3), 0.0F);
                pose.scale(1.5F, 1.5F, 1.0F);
                c.g.renderItem(stack, 0, 0);
                pose.popPose();
                c.flush();
                c.pushLayer(300.0F);
                if (i == 9 && RecipesTab.this.cur.count > 1) {
                    String n = String.valueOf(RecipesTab.this.cur.count);
                    c.textShadow(n, cx + 30 - 2 - c.width(n), cy + 30 - 10, -1841682);
                } else if (i != 9 && RecipesTab.this.optionsOf(RecipesTab.this.cur.slots[i]).length > 1) {
                    c.textShadow("*", cx + 30 - 7, cy + 2, -7639809);
                }

                c.popLayer();
            }
        }

        private ItemStack stackAt(int i) {
            if (i == 9) {
                return RecipesTab.stackOf(RecipesTab.this.cur.result);
            } else {
                ItemStack[] all = RecipesTab.this.optionsOf(RecipesTab.this.cur.slots[i]);
                return all.length == 0 ? ItemStack.EMPTY : all[(int)(System.currentTimeMillis() / 1000L % all.length)];
            }
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            if (RecipesTab.this.cur != null && RecipesTab.this.cur.editable()) {
                int i = this.cellAt(mx, my);
                if (i < 0) {
                    return false;
                } else if (button == 1) {
                    if (i == 9) {
                        RecipesTab.this.cur.result = "";
                    } else {
                        RecipesTab.this.cur.slots[i] = "";
                    }

                    RecipesTab.this.slot = i;
                    RecipesTab.this.changed();
                    RecipesTab.this.buildForm();
                    return true;
                } else {
                    RecipesTab.this.slot = i;
                    RecipesTab.this.buildForm();
                    if (this.stackAt(i).isEmpty()) {
                        RecipesTab.this.pick(i);
                    }

                    return true;
                }
            } else {
                return false;
            }
        }

        @Override
        public int cursor(int mx, int my) {
            return this.cellAt(mx, my) >= 0 ? 221188 : 221185;
        }

        @Override
        public String tooltipText(int mx, int my) {
            int i = RecipesTab.this.cur == null ? -1 : this.cellAt(mx, my);
            if (i < 0) {
                return super.tooltipText(mx, my);
            } else {
                ItemStack s = this.stackAt(i);
                if (s.isEmpty()) {
                    return i == 9 ? "Результат" : "Пустая ячейка";
                } else {
                    String spec = i == 9 ? "" : RecipesTab.this.cur.slots[i];
                    return RecipesTab.stackTitle(s)
                        + (spec.startsWith("#") ? "\nТег: " + spec.substring(1) : (spec.contains("|") ? "\nЛюбой из вариантов" : ""));
                }
            }
        }
    }

    private static final class Row {
        final String id;
        final String name;
        final String mod;
        final String type;
        final ItemStack icon;
        final RecipesTab.Status status;

        Row(String id, String name, String type, ItemStack icon, RecipesTab.Status status) {
            this.id = id;
            this.name = name;
            this.mod = id.substring(0, Math.max(0, id.indexOf(58)));
            this.type = type;
            this.icon = icon;
            this.status = status;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof RecipesTab.Row r && r.id.equals(this.id);
        }

        @Override
        public int hashCode() {
            return this.id.hashCode();
        }
    }

    private static enum Status {
        GAME("Игры", 0),
        CHANGED("Изменён", -870325),
        MINE("Свой", -11746422),
        OFF("Отключён", -1023878);

        final String title;
        final int color;

        private Status(String title, int color) {
            this.title = title;
            this.color = color;
        }
    }
}
