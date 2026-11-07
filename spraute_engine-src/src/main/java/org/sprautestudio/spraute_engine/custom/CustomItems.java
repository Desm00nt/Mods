package org.sprautestudio.spraute_engine.custom;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.ArmorSpec;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.recipe.RecipeDef;

public final class CustomItems {
    public static final String FOLDER = "items";
    public static final String TABS_FOLDER = "tabs";
    private static final Map<String, CustomItems.RegisteredTab> TABS = new LinkedHashMap<>();
    private static final Map<String, CustomItems.Registered> REGISTERED = new LinkedHashMap<>();

    private CustomItems() {
    }

    public static Map<String, CustomItems.RegisteredTab> tabs() {
        return TABS;
    }

    public static Map<String, CustomItems.Registered> registered() {
        return REGISTERED;
    }

    public static Path projectsDir() {
        return Platform.get().gameDir().resolve("spraute").resolve("projects");
    }

    public static void register() {
        Path root = projectsDir();
        if (Files.isDirectory(root)) {
            try (Stream<Path> projects = Files.list(root)) {
                List<Path> all = projects.filter(x$0 -> Files.isDirectory(x$0)).sorted().toList();

                for (Path project : all) {
                    registerTabs(project);
                }

                for (Path project : all) {
                    CustomBlocks.registerProject(project);
                }

                CustomBlocks.registerStorage();
                CustomBlocks.registerModels();
                CustomParts.register(CustomBlocks.registered());

                for (Path project : all) {
                    registerProject(project);
                }
            } catch (IOException var7) {
                SprauteEngine.LOGGER.error("Spraute: cannot list the projects for their items", (Throwable)var7);
            }

            if (!REGISTERED.isEmpty()) {
                SprauteEngine.LOGGER.info("Spraute: {} items of projects are in the game", REGISTERED.size());
            }

            if (!CustomBlocks.registered().isEmpty()) {
                SprauteEngine.LOGGER.info("Spraute: {} blocks of projects are in the game", CustomBlocks.registered().size());
            }
        }
    }

    private static void registerTabs(Path project) throws IOException {
        for (TabDef t : readTabs(project)) {
            String problem = t.problem();
            if (problem != null) {
                SprauteEngine.LOGGER.error("Spraute: the tab {} of {} is not added: {}", t.id, project.getFileName(), problem);
            } else if (TABS.containsKey(t.id)) {
                SprauteEngine.LOGGER
                    .error(
                        "Spraute: the tab {} is in two projects, {} and {}: the first is used",
                        t.id,
                        TABS.get(t.id).project().getFileName(),
                        project.getFileName()
                    );
            } else {
                Object handle = Platform.get().ownTab(t.id, () -> tabIcon(t), () -> tabItems(t));
                TABS.put(t.id, new CustomItems.RegisteredTab(t, project, t.toJson(), handle));
            }
        }
    }

    private static ItemStack tabIcon(TabDef t) {
        Item icon = itemOf(t.icon);
        return new ItemStack(icon != null ? icon : Items.CHEST);
    }

    private static List<ItemStack> tabItems(TabDef t) {
        List<ItemStack> out = new ArrayList<>();

        for (CustomBlocks.Registered r : CustomBlocks.registered().values()) {
            if (r.def().tab.equals(t.key())) {
                out.add(new ItemStack(r.item().get()));
            }
        }

        for (CustomItems.Registered rx : REGISTERED.values()) {
            if (rx.def().tab.equals(t.key())) {
                out.add(new ItemStack(rx.item().get()));
            }
        }

        for (String id : t.items) {
            Item item = itemOf(id);
            if (item != null) {
                out.add(new ItemStack(item));
            }
        }

        return out;
    }

    public static List<TabDef> readTabs(Path project) throws IOException {
        List<TabDef> out = new ArrayList<>();
        return out;
    }

    private static void registerProject(Path project) throws IOException {
        for (ItemDef d : read(project)) {
            String problem = d.problem();
            if (problem != null) {
                SprauteEngine.LOGGER.error("Spraute: the item {} of {} is not added: {}", d.id, project.getFileName(), problem);
            } else if (CustomBlocks.registered().containsKey(d.id)) {
                SprauteEngine.LOGGER.error("Spraute: the item {} of {} has the id of a block: not added", d.id, project.getFileName());
            } else if (REGISTERED.containsKey(d.id)) {
                SprauteEngine.LOGGER
                    .error(
                        "Spraute: the item {} is in two projects, {} and {}: the first is used",
                        d.id,
                        REGISTERED.get(d.id).project().getFileName(),
                        project.getFileName()
                    );
            } else {
                Supplier<Item> item = Platform.get().item(d.id, () -> make(d));
                REGISTERED.put(d.id, new CustomItems.Registered(d, project, d.restartKey(), item));
                if (d.tab.startsWith("custom:")) {
                    if (!TABS.containsKey(d.tab.substring("custom:".length()))) {
                        SprauteEngine.LOGGER.warn("Spraute: the item {} chose a tab that is not there: {}", d.id, d.tab);
                    }
                } else if (!d.tab.equals("none")) {
                    Platform.get().creativeTab(item, d.tab);
                }

                if (d.burnTicks > 0) {
                    Platform.get().fuel(item, d.burnTicks);
                }
            }
        }
    }

    public static List<ItemDef> read(Path project) throws IOException {
        List<ItemDef> out = new ArrayList<>();
        return out;
    }

    public static void showChanges(ItemDef saved) {
        CustomItems.Registered r = REGISTERED.get(saved.id);
        if (r != null) {
            r.def().name = saved.name;
            r.def().lore = new ArrayList<>(saved.lore);
            r.def().glint = saved.glint;
        }
    }

    private static Item make(ItemDef d) {
        Properties var10000 = new Properties();
        String rarity = d.rarity;

        Properties p = var10000.rarity(switch (rarity) {
            case "uncommon" -> Rarity.UNCOMMON;
            case "rare" -> Rarity.RARE;
            case "epic" -> Rarity.EPIC;
            default -> Rarity.COMMON;
        });
        tabOf(p, d.tab);
        if (d.fireResistant) {
            p.fireResistant();
        }

        Item remainder = itemOf(d.craftRemainder);
        if (remainder != null) {
            p.craftRemainder(remainder);
        }

        String var6 = d.kind;

        return (Item)(switch (var6) {
            case "sword" -> new CustomItemKinds.Sword(d, p);
            case "pickaxe" -> new CustomItemKinds.Pickaxe(d, p);
            case "axe" -> new CustomItemKinds.Axe(d, p);
            case "shovel" -> new CustomItemKinds.Shovel(d, p);
            case "hoe" -> new CustomItemKinds.Hoe(d, p);
            case "armor" -> new CustomItemKinds.Armor(d, p);
            default -> new CustomItemKinds.Plain(d, plain(d, p));
        });
    }

    static void tabOf(Properties p, String key) {
        if (key.startsWith("custom:")) {
            CustomItems.RegisteredTab tab = TABS.get(key.substring("custom:".length()));
            if (tab != null) {
                Mc.ownTab(p, tab.handle());
            }
        } else {
            Mc.creativeTab(p, key);
        }
    }

    private static Properties plain(ItemDef d, Properties p) {
        if (d.isFood()) {
            net.minecraft.world.food.FoodProperties.Builder food = new net.minecraft.world.food.FoodProperties.Builder()
                .nutrition(d.nutrition)
                .saturationMod(d.saturation);
            if (d.meat) {
                food.meat();
            }

            if (d.alwaysEat) {
                food.alwaysEat();
            }

            if (d.fastEat) {
                food.fast();
            }

            p.food(food.build());
        }

        if (d.durability > 0) {
            p.durability(d.durability);
        } else {
            p.stacksTo(d.maxStack);
        }

        return p;
    }

    static Item itemOf(String id) {
        ResourceLocation key = id != null && !id.isEmpty() ? ResourceLocation.tryParse(id) : null;
        if (key != null && Regs.ITEM.containsKey(key)) {
            Item item = Regs.ITEM.get(key);
            return item == Items.AIR ? null : item;
        } else {
            return null;
        }
    }

    static Ingredient ingredient(String spec) {
        if (spec != null && !spec.isBlank()) {
            try {
                return Ingredient.fromJson(RecipeDef.ingredientJson(spec));
            } catch (RuntimeException var2) {
                SprauteEngine.LOGGER.warn("Spraute: a repair item is wrong: {}", spec);
                return Ingredient.EMPTY;
            }
        } else {
            return Ingredient.EMPTY;
        }
    }

    static int wholeDamage(ItemDef d) {
        return (int)Math.floor(d.attackDamage - 1.0F);
    }

    static Tier tier(final ItemDef d) {
        final float bonus = d.attackDamage - 1.0F - wholeDamage(d);
        return new Tier() {
            private Ingredient repair;

            @Override
            public int getUses() {
                return d.durability;
            }

            @Override
            public float getSpeed() {
                return d.miningSpeed;
            }

            @Override
            public float getAttackDamageBonus() {
                return bonus;
            }

            @Override
            public int getLevel() {
                return d.miningLevel;
            }

            @Override
            public int getEnchantmentValue() {
                return d.enchantability;
            }

            @Override
            public Ingredient getRepairIngredient() {
                if (this.repair == null) {
                    this.repair = CustomItems.ingredient(d.repair);
                }

                return this.repair;
            }
        };
    }

    static ArmorSpec armor(ItemDef d) {
        SoundEvent sound = soundOf(d.equipSound);
        Ingredient[] repair = new Ingredient[1];
        return new ArmorSpec(
            armorName(d),
            d.durability,
            d.defense,
            d.enchantability,
            sound != null ? sound : SoundEvents.ARMOR_EQUIP_GENERIC,
            d.toughness,
            d.knockbackResistance,
            () -> {
                if (repair[0] == null) {
                    repair[0] = ingredient(d.repair);
                }

                return repair[0];
            }
        );
    }

    public static String armorName(ItemDef d) {
        return d.armorLook.equals("image") ? "spraute_" + d.id : d.armorLook;
    }

    static SoundEvent soundOf(String id) {
        ResourceLocation key = id != null && !id.isEmpty() ? ResourceLocation.tryParse(id) : null;
        return key == null ? null : Regs.SOUND_EVENT.get(key);
    }

    static Component name(ItemDef d) {
        return Component.literal(d.name);
    }

    static void tooltip(ItemDef d, List<Component> out) {
        for (String line : d.lore) {
            out.add(Component.literal(line.replace('&', '§')));
        }
    }

    private static void apply(ItemDef.Effect e, LivingEntity to, int ticks) {
        ResourceLocation key = ResourceLocation.tryParse(e.id);
        MobEffect effect = key == null ? null : Regs.MOB_EFFECT.get(key);
        if (effect != null) {
            to.addEffect(new MobEffectInstance(effect, ticks, Math.max(0, e.level - 1), false, !e.hidden));
        }
    }

    static void applyAll(List<ItemDef.Effect> effects, LivingEntity to) {
        for (ItemDef.Effect e : effects) {
            if (e.chance >= 1.0F || to.getRandom().nextFloat() < e.chance) {
                apply(e, to, Math.max(1, Math.round(e.seconds * 20.0F)));
            }
        }
    }

    static void tick(ItemDef d, ItemStack stack, Level level, Entity entity, boolean selected) {
        if (!level.isClientSide && entity instanceof LivingEntity living && entity.tickCount % 20 == 0) {
            if (!d.inventoryEffects.isEmpty()) {
                keep(d.inventoryEffects, living);
            }

            if (!d.heldEffects.isEmpty() && (selected || living.getOffhandItem() == stack)) {
                keep(d.heldEffects, living);
            }

            if (!d.wornEffects.isEmpty() && d.isArmor() && living.getItemBySlot(EquipmentSlot.byName(d.armorSlot)) == stack) {
                keep(d.wornEffects, living);
            }
        }
    }

    private static void keep(List<ItemDef.Effect> effects, LivingEntity to) {
        for (ItemDef.Effect e : effects) {
            apply(e, to, 60);
        }
    }

    static void hit(ItemDef d, LivingEntity target) {
        if (!target.getCommandSenderWorld().isClientSide) {
            applyAll(d.hitEffects, target);
            if (d.hitFireSeconds > 0) {
                target.setSecondsOnFire(d.hitFireSeconds);
            }
        }
    }

    static boolean holds(ItemDef d) {
        return d.useTicks > 0;
    }

    static ItemStack act(ItemDef d, Item item, ItemStack stack, Level level, LivingEntity user, InteractionHand hand) {
        if (level.isClientSide) {
            return stack;
        } else {
            applyAll(d.useEffects, user);
            SoundEvent sound = soundOf(d.useSound);
            if (sound != null) {
                level.playSound(null, user.getX(), user.getY(), user.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            Player player = user instanceof Player pl ? pl : null;
            if (player != null && !d.useCommand.isBlank()) {
                command(d, player);
            }

            if (player != null && d.cooldownTicks > 0) {
                player.getCooldowns().addCooldown(item, d.cooldownTicks);
            }

            boolean creative = player != null && player.getAbilities().instabuild;
            if (d.damageOnUse && stack.isDamageableItem()) {
                stack.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(hand));
            }

            if (d.consumeOnUse && !creative && !d.isFood()) {
                stack.shrink(1);
            }

            return giveBack(d, stack, player, creative);
        }
    }

    private static ItemStack giveBack(ItemDef d, ItemStack stack, Player player, boolean creative) {
        Item back = itemOf(d.useRemainder);
        if (back == null || creative) {
            return stack;
        } else if (stack.isEmpty()) {
            return new ItemStack(back);
        } else {
            if (player != null && !player.getInventory().add(new ItemStack(back))) {
                player.drop(new ItemStack(back), false);
            }

            return stack;
        }
    }

    private static void command(ItemDef d, Player player) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(2).withSuppressedOutput(), d.useCommand);
        }
    }

    static UseAnim useAnim(ItemDef d) {
        try {
            return d.useAnim.isEmpty() ? null : UseAnim.valueOf(d.useAnim.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException var2) {
            return null;
        }
    }

    static Multimap<Attribute, AttributeModifier> modifiers(ItemDef d, Multimap<Attribute, AttributeModifier> base, EquipmentSlot slot) {
        Builder<Attribute, AttributeModifier> all = ImmutableMultimap.builder();
        all.putAll(base);

        for (int i = 0; i < d.attributes.size(); i++) {
            ItemDef.Attr a = d.attributes.get(i);
            if (slot.getName().equals(a.slot)) {
                ResourceLocation key = ResourceLocation.tryParse(a.id);
                Attribute attribute = key == null ? null : Regs.ATTRIBUTE.get(key);
                if (attribute != null) {
                    UUID uuid = UUID.nameUUIDFromBytes(("spraute_item/" + d.id + "/" + i).getBytes(StandardCharsets.UTF_8));
                    String var10 = a.op;

                    Operation op = switch (var10) {
                        case "multiply_base" -> Operation.MULTIPLY_BASE;
                        case "multiply_total" -> Operation.MULTIPLY_TOTAL;
                        default -> Operation.ADDITION;
                    };
                    all.put(attribute, new AttributeModifier(uuid, "Spraute item " + d.id, a.amount, op));
                }
            }
        }

        return all.build();
    }

    public record Registered(ItemDef def, Path project, String restartKey, Supplier<Item> item) {
    }

    public record RegisteredTab(TabDef def, Path project, String json, Object handle) {
    }
}
