package org.sprautestudio.spraute_engine.recipe;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.lang.ItemText;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntimeException;

public final class RecipeApi {
    private static boolean dirty;

    private RecipeApi() {
    }

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(e -> {
            if (e.phase == TickPhase.END) {
                flush();
            }
        });
    }

    private static MinecraftServer server() {
        MinecraftServer s = Platform.get().server();
        if (s == null) {
            throw new SprauteRuntimeException("Нет сервера");
        } else {
            return s;
        }
    }

    private static void flush() {
        if (dirty) {
            dirty = false;
            MinecraftServer s = Platform.get().server();
            if (s != null) {
                RecipeOverrides.reload(s);
            }
        }
    }

    private static RecipeManager manager() {
        flush();
        return server().getRecipeManager();
    }

    private static String newId(String text) {
        String t = text.trim();
        return checked(t.contains(":") ? t : "spraute:" + t).toString();
    }

    private static ResourceLocation existing(String text) {
        String t = text.trim();
        return checked(t.contains(":") ? t : "minecraft:" + t);
    }

    private static ResourceLocation checked(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            throw new SprauteRuntimeException("Неверный id рецепта: " + id + " (только латиница в нижнем регистре, цифры, _ . / -)");
        } else {
            return key;
        }
    }

    private static Recipe<?> recipe(String text) {
        ResourceLocation key = existing(text);
        return (Recipe<?>)manager().byKey(key).orElseThrow(() -> new SprauteRuntimeException("Нет такого рецепта: " + key));
    }

    private static String result(String item) {
        String id = ItemText.id(item);
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key != null && Regs.ITEM.containsKey(key)) {
            return id;
        } else {
            throw new SprauteRuntimeException("Нет такого предмета: " + item);
        }
    }

    private static String ingredient(String text) {
        if (text != null && !text.isBlank()) {
            List<String> parts = new ArrayList<>();

            for (String raw : text.split("\\|")) {
                String p = raw.trim();
                if (!p.isEmpty()) {
                    if (p.startsWith("#")) {
                        parts.add(p.contains(":") ? p : "#minecraft:" + p.substring(1));
                    } else {
                        parts.add(result(p));
                    }
                }
            }

            return String.join("|", parts);
        } else {
            return "";
        }
    }

    public static void crafting(boolean shaped, String id, String out, int count, String[] slots) {
        RecipeDef d = RecipeDef.create(newId(id), shaped ? "minecraft:crafting_shaped" : "minecraft:crafting_shapeless");

        for (int i = 0; i < 9; i++) {
            d.slots[i] = ingredient(slots[i]);
        }

        d.resultNbt = ItemText.nbt(out);
        d.result = result(out);
        d.count = Math.max(1, Math.min(64, count));
        put(d);
    }

    public static void cooking(String type, String id, String input, String out, int ticks, double xp) {
        RecipeDef d = RecipeDef.create(newId(id), type);
        d.slots[0] = ingredient(input);
        d.result = result(out);
        if (ticks >= 0) {
            d.cookingTime = Math.max(1, ticks);
        }

        d.experience = (float)Math.max(0.0, xp);
        put(d);
    }

    public static void stonecutting(String id, String input, String out, int count) {
        RecipeDef d = RecipeDef.create(newId(id), "minecraft:stonecutting");
        d.slots[0] = ingredient(input);
        d.result = result(out);
        d.count = Math.max(1, Math.min(64, count));
        put(d);
    }

    private static void put(RecipeDef d) {
        String problem = d.problem();
        if (problem != null) {
            throw new SprauteRuntimeException("Рецепт " + d.id + ": " + problem);
        } else {
            write(checked(d.id), d.toObject());
        }
    }

    public static void disable(String id) {
        ResourceLocation key = existing(id);
        JsonObject have = RecipeOverrides.files().get(key);
        if (have != null) {
            JsonObject off = have.deepCopy();
            off.addProperty("spraute_disabled", true);
            write(key, off);
        } else {
            Recipe<?> r = recipe(id);
            RecipeDef placeholder = RecipeDef.other(key.toString(), r.getType().toString(), itemId(resultOf(r).getItem()));
            placeholder.disabled = true;
            write(key, placeholder.toObject());
        }
    }

    public static void enable(String id) {
        ResourceLocation key = existing(id);
        JsonObject have = RecipeOverrides.files().get(key);
        if (have != null && have.has("spraute_disabled")) {
            if (RecipeDef.fromJson(key.toString(), have.toString()).placeholder()) {
                JsonObject back = new JsonObject();
                back.addProperty("spraute_restore", true);
                write(key, back);
            } else {
                JsonObject on = have.deepCopy();
                on.remove("spraute_disabled");
                write(key, on);
            }
        }
    }

    public static void reset(String id) {
        ResourceLocation key = existing(id);

        try {
            if (RecipeOverrides.deleteStoryFile(key)) {
                dirty = true;
            }
        } catch (IOException var3) {
            throw new SprauteRuntimeException("Не удалось сбросить рецепт " + key + ": " + var3.getMessage());
        }
    }

    private static void write(ResourceLocation key, JsonObject json) {
        try {
            RecipeOverrides.writeStoryFile(key, json);
            dirty = true;
        } catch (IOException var3) {
            throw new SprauteRuntimeException("Не удалось записать рецепт " + key + ": " + var3.getMessage());
        }
    }

    public static void learn(ServerPlayer p, String id) {
        p.awardRecipes(List.of(recipe(id)));
    }

    public static void forget(ServerPlayer p, String id) {
        p.resetRecipes(List.of(recipe(id)));
    }

    public static boolean exists(String id) {
        return manager().byKey(existing(id)).isPresent();
    }

    public static boolean known(ServerPlayer p, String id) {
        return p != null && p.getRecipeBook().contains(existing(id));
    }

    public static boolean canCraft(ServerPlayer p, String id) {
        if (p == null) {
            return false;
        } else {
            Recipe<?> r = recipe(id);
            if (r.getIngredients().isEmpty()) {
                return false;
            } else {
                StackedContents have = new StackedContents();
                p.getInventory().fillStackedContents(have);
                return have.canCraft(r, null);
            }
        }
    }

    public static List<Object> all() {
        return sorted(manager().getRecipes(), null, null);
    }

    public static List<Object> making(String item) {
        return sorted(manager().getRecipes(), required(item), null);
    }

    public static List<Object> using(String item) {
        return sorted(manager().getRecipes(), null, new ItemStack(required(item)));
    }

    private static List<Object> sorted(Iterable<Recipe<?>> recipes, Item makes, ItemStack uses) {
        TreeSet<String> out = new TreeSet<>();

        for (Recipe<?> r : recipes) {
            if ((makes == null || resultOf(r).getItem() == makes) && (uses == null || !r.getIngredients().stream().noneMatch(i -> i.test(uses)))) {
                out.add(r.getId().toString());
            }
        }

        return new ArrayList<>(out);
    }

    public static String output(String id) {
        return itemId(resultOf(recipe(id)).getItem());
    }

    public static double outputCount(String id) {
        return resultOf(recipe(id)).getCount();
    }

    public static String type(String id) {
        return recipe(id).getType().toString();
    }

    public static List<Object> ingredients(String id) {
        List<Object> out = new ArrayList<>();

        for (Ingredient i : recipe(id).getIngredients()) {
            ItemStack[] items = i.getItems();
            if (items.length > 0) {
                out.add(itemId(items[0].getItem()));
            }
        }

        return out;
    }

    private static Item required(String item) {
        Item it = ItemSpec.item(item);
        if (it != null && !ItemSpec.stack(item, 1).isEmpty()) {
            return it;
        } else {
            throw new SprauteRuntimeException("Нет такого предмета: " + item);
        }
    }

    private static ItemStack resultOf(Recipe<?> r) {
        try {
            ItemStack s = Mc.recipeResult(r, server().overworld());
            return s == null ? ItemStack.EMPTY : s;
        } catch (RuntimeException var2) {
            return ItemStack.EMPTY;
        }
    }

    private static String itemId(Item item) {
        return String.valueOf(Regs.ITEM.getKey(item));
    }
}
