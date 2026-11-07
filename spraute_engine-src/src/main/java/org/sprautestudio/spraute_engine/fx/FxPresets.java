package org.sprautestudio.spraute_engine.fx;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class FxPresets {
    public static final Map<String, FxPresets.Preset> ALL = new LinkedHashMap<>();

    private FxPresets() {
    }

    public static FxDef get(String name) {
        FxPresets.Preset p = ALL.get(name);
        return p != null ? p.make().get() : null;
    }

    public static List<String> names() {
        return List.copyOf(ALL.keySet());
    }

    public static void add(String name, String title, String doc, Supplier<FxDef> make) {
        preset(name, title, doc, make);
    }

    private static void preset(String name, String title, String doc, Supplier<FxDef> make) {
        ALL.put(name, new FxPresets.Preset(name, title, doc, make));
    }

    private static FxDef.Layer flash(String color, float strength, float time, float range) {
        FxDef.Layer l = FxDef.layer("Вспышка экрана");
        l.type = "flash";
        l.flashColor = color;
        l.strength = strength;
        l.time = time;
        l.range = range;
        return l;
    }

    private static FxDef.Layer shake(float strength, float time, float range, float delay) {
        FxDef.Layer l = FxDef.layer("Тряска камеры");
        l.type = "shake";
        l.strength = strength;
        l.time = time;
        l.range = range;
        l.delay = delay;
        return l;
    }

    private static FxDef fx(float duration, String sound, float volume, float pitch, Object... layers) {
        FxDef d = new FxDef();
        d.duration = duration;
        d.sound = sound;
        d.soundVolume = volume;
        d.soundPitch = pitch;

        for (Object o : layers) {
            d.layers.add(o instanceof FxPresets.L b ? b.l : (FxDef.Layer)o);
        }

        return d;
    }

    private static void more() {
        preset(
            "critical_hit",
            "Критический удар",
            "Белая вспышка, красные искры и короткое кольцо — на место сильного удара.",
            () -> fx(
                1.0F,
                "minecraft:entity.player.attack.crit",
                1.0F,
                1.0F,
                new FxPresets.L("Вспышка")
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(0.2F, 0.0F)
                    .size(1.6F, 0.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF")
                    .glow(2.0F),
                new FxPresets.L("Искры")
                    .burst(45)
                    .dir("random", 7.0F, 0.5F)
                    .drag(2.5F)
                    .gravity(5.0F)
                    .tex("spark")
                    .stretch(0.14F)
                    .life(0.5F, 0.3F)
                    .size(0.16F, 0.03F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#FF5A3A", "#B01010")
                    .glow(1.8F),
                new FxPresets.L("Кольцо")
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .life(0.3F, 0.0F)
                    .size(0.3F, 2.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FF8A6A")
                    .glow(1.4F)
            )
        );
        preset(
            "metal_sparks",
            "Искры от металла",
            "Жёлтые искры от удара меча о щит или броню.",
            () -> fx(
                1.0F,
                "minecraft:item.shield.block",
                0.8F,
                1.2F,
                new FxPresets.L("Искры")
                    .burst(35)
                    .dir("cone", 6.0F, 0.6F)
                    .angle(60.0F)
                    .gravity(10.0F)
                    .drag(0.6F)
                    .tex("spark")
                    .stretch(0.1F)
                    .life(0.6F, 0.4F)
                    .size(0.12F, 0.02F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#FFD060", "#FF8A20")
                    .glow(2.0F)
            )
        );
        preset(
            "blood_splash",
            "Брызги крови",
            "Тёмно-красные капли разлетаются и падают.",
            () -> fx(
                1.5F,
                "minecraft:entity.player.hurt",
                0.6F,
                0.9F,
                new FxPresets.L("Капли")
                    .shape("sphere", 0.2F)
                    .burst(40)
                    .dir("random", 3.5F, 0.6F)
                    .gravity(10.0F)
                    .tex("glow")
                    .blend("alpha")
                    .life(0.7F, 0.4F)
                    .size(0.14F, 0.06F)
                    .alpha(1.0F, 0.0F)
                    .color("#B01010", "#5A0606")
            )
        );
        preset(
            "shockwave",
            "Ударная волна",
            "Плоское кольцо по земле и пыль — от прыжка или удара босса.",
            () -> fx(
                2.0F,
                "minecraft:entity.generic.explode",
                1.0F,
                1.3F,
                shake(0.4F, 0.6F, 25.0F, 0.0F),
                new FxPresets.L("Волна")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.1F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(0.8F, 0.0F)
                    .size(1.0F, 12.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#D8D0C0")
                    .glow(1.2F),
                new FxPresets.L("Пыль")
                    .shape("ring", 1.0F)
                    .at(0.0F, 0.3F, 0.0F)
                    .burst(80)
                    .dir("out", 9.0F, 0.3F)
                    .drag(2.0F)
                    .smoke()
                    .life(1.4F, 0.4F)
                    .size(0.8F, 2.0F)
                    .alpha(0.0F, 0.7F, 0.0F)
                    .color("#A08A70", "#6E604E")
            )
        );
        preset(
            "ground_slam",
            "Удар о землю",
            "Тяжёлый удар: волна, комья земли, пыль и тряска камеры.",
            () -> fx(
                2.5F,
                "minecraft:block.anvil.land",
                1.5F,
                0.6F,
                shake(0.9F, 0.8F, 30.0F, 0.0F),
                new FxPresets.L("Волна")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.1F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(0.9F, 0.0F)
                    .size(1.0F, 9.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#E0D0B0"),
                new FxPresets.L("Комья")
                    .shape("disc", 0.6F)
                    .burst(40)
                    .dir("cone", 8.0F, 0.5F)
                    .angle(55.0F)
                    .gravity(16.0F)
                    .tex("square")
                    .blend("alpha")
                    .life(1.2F, 0.4F)
                    .size(0.25F, 0.2F)
                    .spin(0.0F, 500.0F)
                    .alpha(1.0F, 1.0F, 0.0F)
                    .color("#6A4A30", "#3E2C1E"),
                new FxPresets.L("Пыль")
                    .shape("ring", 0.8F)
                    .at(0.0F, 0.3F, 0.0F)
                    .burst(60)
                    .dir("out", 5.0F, 0.4F)
                    .drag(1.5F)
                    .smoke()
                    .life(1.8F, 0.4F)
                    .size(1.0F, 2.5F)
                    .alpha(0.0F, 0.8F, 0.0F)
                    .color("#9A8468", "#5E5244")
            )
        );
        preset(
            "meteor_impact",
            "Падение метеорита",
            "Огненный шар падает с неба и взрывается у земли.",
            () -> fx(
                4.0F,
                "minecraft:entity.generic.explode",
                3.0F,
                0.6F,
                new FxPresets.L("Метеор")
                    .at(0.0F, 25.0F, 0.0F)
                    .burst(1)
                    .max(1)
                    .dir("down", 50.0F, 0.0F)
                    .tex("flare")
                    .life(0.5F, 0.0F)
                    .size(2.5F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F)
                    .color("#FFE0A0")
                    .glow(2.2F),
                new FxPresets.L("Хвост")
                    .shape("sphere", 0.5F)
                    .at(0.0F, 25.0F, 0.0F)
                    .rate(240.0F)
                    .time(0.5F)
                    .dir("down", 50.0F, 0.05F)
                    .world()
                    .tex("smoke")
                    .life(0.6F, 0.3F)
                    .size(1.5F, 0.3F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#FF9A30", "#802010")
                    .glow(1.6F),
                flash("#FFE0B0", 0.5F, 0.4F, 60.0F),
                shake(1.0F, 1.2F, 60.0F, 0.5F),
                new FxPresets.L("Взрыв")
                    .shape("sphere", 1.0F)
                    .delay(0.5F)
                    .burst(90)
                    .dir("out", 6.0F, 0.5F)
                    .drag(2.0F)
                    .gravity(-1.0F)
                    .tex("smoke")
                    .life(1.2F, 0.4F)
                    .size(2.0F, 4.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#FFB040", "#E04010", "#401008")
                    .glow(1.5F),
                new FxPresets.L("Камни")
                    .delay(0.5F)
                    .burst(50)
                    .dir("cone", 14.0F, 0.5F)
                    .angle(60.0F)
                    .gravity(18.0F)
                    .tex("square")
                    .blend("alpha")
                    .life(1.6F, 0.4F)
                    .size(0.35F, 0.3F)
                    .spin(0.0F, 400.0F)
                    .alpha(1.0F, 1.0F, 0.0F)
                    .color("#3A2A20", "#1E1A18")
            )
        );
        preset(
            "smoke_bomb",
            "Дымовая шашка",
            "Хлопок и плотный серый дым, который долго висит — чтобы скрыться.",
            () -> fx(
                8.0F,
                "minecraft:entity.firework_rocket.blast",
                1.0F,
                0.5F,
                new FxPresets.L("Хлопок")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.5F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(0.2F, 0.0F)
                    .size(3.0F, 0.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF"),
                new FxPresets.L("Дым")
                    .shape("sphere", 1.5F)
                    .at(0.0F, 1.0F, 0.0F)
                    .burst(120)
                    .dir("out", 2.0F, 0.5F)
                    .drag(1.2F)
                    .turb(0.5F)
                    .smoke()
                    .life(6.0F, 0.3F)
                    .size(2.0F, 3.5F)
                    .alpha(0.0F, 0.95F, 0.9F, 0.0F)
                    .color("#9A9690", "#6A6662")
            )
        );
        preset(
            "death_poof",
            "Исчезновение тела",
            "Белое облачко, как когда умирает моб.",
            () -> fx(
                1.5F,
                "minecraft:entity.generic.death",
                0.6F,
                1.2F,
                new FxPresets.L("Облачко")
                    .shape("sphere", 0.5F)
                    .at(0.0F, 0.9F, 0.0F)
                    .burst(30)
                    .dir("out", 1.2F, 0.4F)
                    .drag(1.5F)
                    .smoke()
                    .life(1.0F, 0.3F)
                    .size(0.6F, 1.2F)
                    .alpha(0.0F, 0.9F, 0.0F)
                    .color("#FFFFFF", "#D0D0D0")
            )
        );
        preset(
            "teleport_out",
            "Исчезновение",
            "Свет вспыхивает и рассыпается вихрем вверх — пара к «Появлению».",
            () -> fx(
                1.8F,
                "minecraft:entity.enderman.teleport",
                0.7F,
                0.9F,
                new FxPresets.L("Свет")
                    .at(0.0F, 1.0F, 0.0F)
                    .burst(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(0.4F, 0.0F)
                    .size(1.5F, 0.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF")
                    .glow(2.0F),
                new FxPresets.L("Вихрь")
                    .shape("sphere", 0.5F)
                    .at(0.0F, 1.0F, 0.0F)
                    .burst(65)
                    .dir("up", 2.0F, 0.5F)
                    .orbit(220.0F)
                    .tex("glow")
                    .life(1.2F, 0.3F)
                    .size(0.16F, 0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#8EEFFF", "#C58AFF")
                    .glow(1.6F)
            )
        );
        preset(
            "energy_charge",
            "Зарядка силы",
            "Частицы стягиваются в точку, а в конце — вспышка. Перед сильным ударом или заклинанием.",
            () -> fx(
                2.2F,
                "minecraft:block.beacon.power_select",
                1.0F,
                1.6F,
                new FxPresets.L("Стяжка")
                    .shape("shell", 2.5F)
                    .inner(2.0F)
                    .at(0.0F, 1.0F, 0.0F)
                    .rate(90.0F)
                    .time(1.6F)
                    .dir("none", 0.0F, 0.0F)
                    .attract(6.0F)
                    .tex("glow")
                    .life(0.6F, 0.1F)
                    .size(0.18F, 0.05F)
                    .alpha(0.0F, 1.0F)
                    .color("#BFF1FF", "#5A8AFF")
                    .glow(1.6F),
                new FxPresets.L("Ядро")
                    .at(0.0F, 1.0F, 0.0F)
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(1.7F, 0.0F)
                    .size(0.1F, 1.4F)
                    .sizeRandom(0.0F)
                    .alpha(0.3F, 1.0F)
                    .color("#FFFFFF", "#8FE3FF")
                    .glow(2.0F),
                new FxPresets.L("Вспышка")
                    .at(0.0F, 1.0F, 0.0F)
                    .delay(1.6F)
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(0.35F, 0.0F)
                    .size(3.0F, 0.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF")
                    .glow(2.2F)
            )
        );
        preset(
            "shield",
            "Щит",
            "Мерцающая сфера защиты вокруг персонажа (с follow, до fx stop).",
            () -> fx(
                0.0F,
                "minecraft:block.beacon.activate",
                0.6F,
                1.6F,
                new FxPresets.L("Сфера")
                    .shape("shell", 1.4F)
                    .at(0.0F, 1.0F, 0.0F)
                    .rate(120.0F)
                    .dir("none", 0.0F, 0.0F)
                    .orbit(30.0F)
                    .tex("glow")
                    .life(0.6F, 0.2F)
                    .size(0.18F, 0.0F)
                    .alpha(0.0F, 0.7F, 0.0F)
                    .color("#8FE3FF", "#4A8AFF")
                    .glow(1.4F),
                new FxPresets.L("Блики")
                    .shape("shell", 1.4F)
                    .at(0.0F, 1.0F, 0.0F)
                    .rate(10.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("star")
                    .life(0.5F, 0.2F)
                    .size(0.3F, 0.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFFFFF")
                    .glow(1.8F)
            )
        );
        preset(
            "buff",
            "Усиление",
            "Оранжевые искры и стрелки-блики поднимаются от ног — эффект силы или скорости.",
            () -> fx(
                2.0F,
                "minecraft:block.beacon.power_select",
                0.7F,
                1.4F,
                new FxPresets.L("Подъём")
                    .shape("ring", 0.6F)
                    .rate(60.0F)
                    .time(1.4F)
                    .dir("up", 2.5F, 0.3F)
                    .tex("spark")
                    .stretch(0.2F)
                    .life(0.8F, 0.3F)
                    .size(0.15F, 0.03F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFE6A0", "#FF8A20")
                    .glow(1.8F),
                new FxPresets.L("Кольцо")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.1F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(0.8F, 0.0F)
                    .size(0.5F, 2.5F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFB040")
                    .glow(1.3F)
            )
        );
        preset(
            "debuff",
            "Ослабление",
            "Тёмно-фиолетовые хлопья опускаются на цель — проклятие, слабость.",
            () -> fx(
                2.2F,
                "minecraft:entity.witch.celebrate",
                0.6F,
                0.7F,
                new FxPresets.L("Хлопья")
                    .shape("disc", 0.7F)
                    .at(0.0F, 2.4F, 0.0F)
                    .rate(40.0F)
                    .time(1.6F)
                    .dir("down", 1.2F, 0.4F)
                    .turb(0.4F)
                    .tex("smoke")
                    .life(1.2F, 0.3F)
                    .size(0.25F, 0.5F)
                    .alpha(0.0F, 0.8F, 0.0F)
                    .color("#6A2A8A", "#2A0A3A"),
                new FxPresets.L("Искры")
                    .shape("disc", 0.6F)
                    .at(0.0F, 1.0F, 0.0F)
                    .rate(15.0F)
                    .time(1.6F)
                    .dir("random", 0.6F, 0.5F)
                    .tex("star")
                    .life(0.8F, 0.3F)
                    .size(0.15F, 0.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#B070FF")
                    .glow(1.4F)
            )
        );
        preset(
            "frost_aura",
            "Морозная аура",
            "Снежинки и холодный пар кружат вокруг персонажа (с follow, до fx stop).",
            () -> fx(
                0.0F,
                "",
                1.0F,
                1.0F,
                new FxPresets.L("Снежинки")
                    .shape("ring", 0.8F)
                    .at(0.0F, 0.8F, 0.0F)
                    .rate(25.0F)
                    .dir("up", 0.4F, 0.5F)
                    .orbit(90.0F)
                    .tex("star")
                    .life(1.5F, 0.4F)
                    .size(0.14F, 0.0F)
                    .spin(0.0F, 150.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFFFFF", "#BFEFFF")
                    .glow(1.3F),
                new FxPresets.L("Пар")
                    .shape("ring", 0.6F)
                    .rate(12.0F)
                    .dir("up", 0.3F, 0.5F)
                    .smoke()
                    .life(1.6F, 0.4F)
                    .size(0.4F, 1.0F)
                    .alpha(0.0F, 0.35F, 0.0F)
                    .color("#DDF6FF")
            )
        );
        preset(
            "electric",
            "Электрические разряды",
            "Синие искры-молнии потрескивают вокруг точки (до fx stop).",
            () -> fx(
                0.0F,
                "",
                1.0F,
                1.0F,
                new FxPresets.L("Разряды")
                    .shape("sphere", 0.8F)
                    .at(0.0F, 1.0F, 0.0F)
                    .rate(35.0F)
                    .dir("random", 5.0F, 0.6F)
                    .drag(4.0F)
                    .tex("spark")
                    .stretch(0.25F)
                    .life(0.18F, 0.3F)
                    .size(0.12F, 0.02F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#7FB8FF", "#3A6AFF")
                    .glow(2.2F),
                new FxPresets.L("Свечение")
                    .at(0.0F, 1.0F, 0.0F)
                    .burst(1)
                    .every(0.25F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(0.15F, 0.5F)
                    .size(1.4F, 0.0F)
                    .alpha(0.6F, 0.0F)
                    .color("#9FC8FF")
                    .glow(1.5F)
            )
        );
        preset(
            "summon_circle",
            "Круг призыва",
            "Красный круг на земле, огненные руны и столб искр — кто-то приходит.",
            () -> fx(
                4.0F,
                "minecraft:entity.evoker.prepare_summon",
                1.5F,
                0.8F,
                new FxPresets.L("Круг")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.05F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(4.0F, 0.0F)
                    .size(0.0F, 4.5F, 4.5F, 0.0F)
                    .sizeRandom(0.0F)
                    .spin(60.0F, 0.0F)
                    .alpha(0.0F, 1.0F, 1.0F, 0.0F)
                    .color("#FF4A2A")
                    .glow(1.6F),
                new FxPresets.L("Руны")
                    .shape("ring", 2.0F)
                    .even()
                    .at(0.0F, 0.2F, 0.0F)
                    .burst(6)
                    .max(6)
                    .dir("none", 0.0F, 0.0F)
                    .orbit(-50.0F)
                    .tex("star")
                    .life(4.0F, 0.0F)
                    .size(0.0F, 0.7F, 0.7F, 0.0F)
                    .sizeRandom(0.0F)
                    .spin(120.0F, 0.0F)
                    .alpha(0.0F, 1.0F, 1.0F, 0.0F)
                    .color("#FFB060")
                    .glow(1.8F),
                new FxPresets.L("Столб")
                    .shape("disc", 0.8F)
                    .rate(80.0F)
                    .time(3.5F)
                    .delay(0.5F)
                    .dir("up", 4.0F, 0.3F)
                    .tex("spark")
                    .stretch(0.2F)
                    .life(0.8F, 0.3F)
                    .size(0.14F, 0.02F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFE0A0", "#FF4A1A")
                    .glow(2.0F)
            )
        );
        preset(
            "holy_beam",
            "Свет с небес",
            "Золотой луч опускается сверху, вокруг кружат перья-блики.",
            () -> fx(
                4.0F,
                "minecraft:block.beacon.activate",
                1.5F,
                1.8F,
                new FxPresets.L("Луч")
                    .burst(1)
                    .max(1)
                    .dir("up", 0.0F, 0.0F)
                    .tex("beam")
                    .beam(60.0F)
                    .life(4.0F, 0.0F)
                    .size(0.0F, 2.4F, 2.0F, 2.0F, 0.0F)
                    .sizeRandom(0.0F)
                    .alpha(0.9F)
                    .color("#FFF2BB", "#FFD65A")
                    .glow(1.8F),
                new FxPresets.L("Перья")
                    .shape("ring", 1.0F)
                    .at(0.0F, 3.0F, 0.0F)
                    .rate(30.0F)
                    .time(3.5F)
                    .dir("down", 0.8F, 0.3F)
                    .orbit(60.0F)
                    .tex("star")
                    .life(2.5F, 0.3F)
                    .size(0.2F, 0.0F)
                    .spin(0.0F, 100.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFFFFF", "#FFE88A")
                    .glow(1.6F)
            )
        );
        preset(
            "dark_beam",
            "Тёмный луч",
            "Столб тьмы с фиолетовыми искрами.",
            () -> fx(
                4.0F,
                "minecraft:entity.wither.ambient",
                1.0F,
                0.6F,
                new FxPresets.L("Луч")
                    .burst(1)
                    .max(1)
                    .dir("up", 0.0F, 0.0F)
                    .tex("beam")
                    .beam(60.0F)
                    .life(4.0F, 0.0F)
                    .size(0.0F, 2.4F, 2.0F, 2.0F, 0.0F)
                    .sizeRandom(0.0F)
                    .blend("alpha")
                    .alpha(0.85F)
                    .color("#2A0A3A", "#0A0410"),
                new FxPresets.L("Искры")
                    .shape("ring", 1.2F)
                    .rate(70.0F)
                    .time(3.5F)
                    .dir("up", 3.0F, 0.4F)
                    .orbit(-180.0F)
                    .tex("spark")
                    .stretch(0.15F)
                    .life(1.2F, 0.3F)
                    .size(0.15F, 0.03F)
                    .alpha(1.0F, 0.0F)
                    .color("#C070FF", "#5A1AA0")
                    .glow(1.8F)
            )
        );
        preset(
            "black_hole",
            "Чёрная дыра",
            "Тьма в центре затягивает частицы по спирали (до fx stop).",
            () -> fx(
                0.0F,
                "minecraft:block.portal.ambient",
                1.0F,
                0.5F,
                new FxPresets.L("Центр")
                    .at(0.0F, 1.4F, 0.0F)
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("glow")
                    .blend("alpha")
                    .life(1000000.0F, 0.0F)
                    .size(2.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F)
                    .color("#000000"),
                new FxPresets.L("Диск")
                    .at(0.0F, 1.4F, 0.0F)
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(1000000.0F, 0.0F)
                    .size(4.5F)
                    .sizeRandom(0.0F)
                    .spin(90.0F, 0.0F)
                    .alpha(0.8F)
                    .color("#FF8A40")
                    .glow(1.3F),
                new FxPresets.L("Затягивание")
                    .shape("ring", 5.0F)
                    .inner(3.5F)
                    .at(0.0F, 1.4F, 0.0F)
                    .rate(120.0F)
                    .dir("tangent", 2.0F, 0.3F)
                    .attract(7.0F)
                    .orbit(240.0F)
                    .tex("glow")
                    .life(1.3F, 0.3F)
                    .size(0.2F, 0.02F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFC080", "#A040FF", "#2A0A60")
                    .glow(1.5F)
            )
        );
        preset(
            "soul_release",
            "Души",
            "Голубые огоньки медленно поднимаются и тают в воздухе.",
            () -> fx(
                4.0F,
                "minecraft:particle.soul_escape",
                1.0F,
                1.0F,
                new FxPresets.L("Души")
                    .shape("disc", 1.0F)
                    .rate(14.0F)
                    .time(3.0F)
                    .dir("up", 1.2F, 0.4F)
                    .turb(0.6F)
                    .tex("glow")
                    .life(2.2F, 0.4F)
                    .size(0.3F, 0.1F)
                    .alpha(0.0F, 0.9F, 0.0F)
                    .color("#BFFFFF", "#40C8E0")
                    .glow(1.6F)
            )
        );
        preset(
            "halo",
            "Нимб",
            "Светящееся кольцо над головой (с follow, до fx stop).",
            () -> fx(
                0.0F,
                "",
                1.0F,
                1.0F,
                new FxPresets.L("Нимб")
                    .at(0.0F, 2.25F, 0.0F)
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(1000000.0F, 0.0F)
                    .size(0.8F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F)
                    .color("#FFE88A")
                    .glow(1.8F),
                new FxPresets.L("Искорки")
                    .shape("ring", 0.4F)
                    .at(0.0F, 2.25F, 0.0F)
                    .rate(6.0F)
                    .dir("none", 0.0F, 0.0F)
                    .orbit(120.0F)
                    .tex("star")
                    .life(0.6F, 0.3F)
                    .size(0.1F, 0.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFFFFF")
                    .glow(1.6F)
            )
        );
        preset(
            "acid_splash",
            "Кислота",
            "Ядовито-зелёные брызги шипят и дымятся.",
            () -> fx(
                2.0F,
                "minecraft:block.lava.extinguish",
                0.7F,
                1.4F,
                new FxPresets.L("Брызги")
                    .shape("sphere", 0.3F)
                    .burst(45)
                    .dir("random", 3.5F, 0.6F)
                    .gravity(9.0F)
                    .tex("glow")
                    .life(0.7F, 0.4F)
                    .size(0.14F, 0.04F)
                    .alpha(1.0F, 0.0F)
                    .color("#C8FF40", "#4AA010")
                    .glow(1.4F),
                new FxPresets.L("Дымок")
                    .shape("disc", 0.6F)
                    .rate(25.0F)
                    .time(1.0F)
                    .dir("up", 0.8F, 0.4F)
                    .smoke()
                    .life(1.2F, 0.4F)
                    .size(0.4F, 1.0F)
                    .alpha(0.0F, 0.6F, 0.0F)
                    .color("#9ACC50", "#4A6A2A")
            )
        );
        preset(
            "leaves",
            "Листопад",
            "Оранжевые и зелёные листья кружась опадают сверху (до fx stop).",
            () -> fx(
                0.0F,
                "",
                1.0F,
                1.0F,
                new FxPresets.L("Листья")
                    .shape("disc", 3.0F)
                    .at(0.0F, 5.0F, 0.0F)
                    .rate(10.0F)
                    .dir("down", 0.6F, 0.4F)
                    .turb(1.2F)
                    .tex("square")
                    .blend("alpha")
                    .life(5.0F, 0.4F)
                    .size(0.18F)
                    .spin(0.0F, 200.0F)
                    .alpha(0.0F, 1.0F, 1.0F, 0.0F)
                    .color("#E08A2A", "#C0401A", "#6AA040", "#D8B040")
                    .colorRandom()
            )
        );
        preset(
            "petals",
            "Лепестки сакуры",
            "Розовые лепестки медленно летят по ветру (до fx stop).",
            () -> fx(
                0.0F,
                "",
                1.0F,
                1.0F,
                new FxPresets.L("Лепестки")
                    .shape("disc", 3.0F)
                    .at(0.0F, 4.0F, 0.0F)
                    .rate(14.0F)
                    .dir("down", 0.5F, 0.4F)
                    .turb(1.5F)
                    .tex("square")
                    .blend("alpha")
                    .life(5.0F, 0.4F)
                    .size(0.12F)
                    .spin(0.0F, 250.0F)
                    .alpha(0.0F, 1.0F, 1.0F, 0.0F)
                    .color("#FFC0D8", "#FF90B8", "#FFFFFF")
                    .colorRandom()
            )
        );
        preset(
            "fireflies",
            "Светлячки",
            "Тёплые огоньки медленно кружат над травой (до fx stop).",
            () -> fx(
                0.0F,
                "",
                1.0F,
                1.0F,
                new FxPresets.L("Огоньки")
                    .shape("disc", 4.0F)
                    .at(0.0F, 1.0F, 0.0F)
                    .rate(4.0F)
                    .dir("random", 0.3F, 0.6F)
                    .turb(1.0F)
                    .tex("glow")
                    .life(4.0F, 0.4F)
                    .size(0.12F)
                    .alpha(0.0F, 1.0F, 0.0F, 1.0F, 0.0F)
                    .color("#E8FF80", "#FFD060")
                    .glow(1.8F)
            )
        );
        preset(
            "bubbles",
            "Пузыри",
            "Пузырьки поднимаются вверх, будто под водой.",
            () -> fx(
                3.0F,
                "minecraft:block.bubble_column.upwards_ambient",
                0.8F,
                1.0F,
                new FxPresets.L("Пузыри")
                    .shape("disc", 0.8F)
                    .rate(30.0F)
                    .time(2.5F)
                    .dir("up", 1.5F, 0.5F)
                    .turb(0.6F)
                    .tex("ring")
                    .life(1.5F, 0.4F)
                    .size(0.15F, 0.25F)
                    .alpha(0.9F, 0.0F)
                    .color("#D0F0FF")
            )
        );
        preset(
            "rain_cloud",
            "Тучка с дождём",
            "Маленькая серая туча и капли под ней (до fx stop).",
            () -> fx(
                0.0F,
                "minecraft:weather.rain",
                0.3F,
                1.2F,
                new FxPresets.L("Туча")
                    .shape("disc", 1.5F)
                    .at(0.0F, 4.0F, 0.0F)
                    .rate(12.0F)
                    .dir("random", 0.2F, 0.5F)
                    .smoke()
                    .life(2.5F, 0.3F)
                    .size(1.5F, 2.2F)
                    .alpha(0.0F, 0.9F, 0.0F)
                    .color("#8A8A90", "#5A5A60"),
                new FxPresets.L("Капли")
                    .shape("disc", 1.3F)
                    .at(0.0F, 3.6F, 0.0F)
                    .rate(60.0F)
                    .dir("down", 9.0F, 0.1F)
                    .tex("spark")
                    .stretch(0.3F)
                    .life(0.4F, 0.2F)
                    .size(0.06F)
                    .alpha(0.8F, 0.6F)
                    .color("#9AC8FF")
            )
        );
        preset(
            "steam",
            "Пар",
            "Белый пар поднимается и рассеивается (до fx stop): котёл, гейзер, горячий источник.",
            () -> fx(
                0.0F,
                "",
                1.0F,
                1.0F,
                new FxPresets.L("Пар")
                    .shape("disc", 0.4F)
                    .rate(16.0F)
                    .dir("up", 1.2F, 0.4F)
                    .turb(0.6F)
                    .drag(0.3F)
                    .smoke()
                    .life(2.4F, 0.4F)
                    .size(0.5F, 2.0F)
                    .alpha(0.0F, 0.6F, 0.0F)
                    .color("#FFFFFF", "#D8DCE0")
            )
        );
        preset(
            "fountain",
            "Фонтан",
            "Струя воды бьёт вверх и падает каплями (до fx stop).",
            () -> fx(
                0.0F,
                "minecraft:block.water.ambient",
                0.6F,
                1.0F,
                new FxPresets.L("Струя")
                    .shape("disc", 0.15F)
                    .rate(90.0F)
                    .dir("cone", 7.0F, 0.15F)
                    .angle(10.0F)
                    .gravity(10.0F)
                    .tex("glow")
                    .life(1.4F, 0.2F)
                    .size(0.14F, 0.08F)
                    .alpha(0.9F, 0.6F)
                    .color("#BFE8FF", "#4A90FF")
            )
        );
        preset(
            "wind_vortex",
            "Вихрь ветра",
            "Серо-белый смерч из пыли и воздуха.",
            () -> fx(
                5.0F,
                "minecraft:item.elytra.flying",
                0.8F,
                1.2F,
                new FxPresets.L("Вихрь")
                    .shape("ring", 0.5F)
                    .rate(120.0F)
                    .dir("up", 3.5F, 0.3F)
                    .orbit(420.0F)
                    .tex("smoke")
                    .life(1.4F, 0.3F)
                    .size(0.4F, 1.2F, 1.8F)
                    .alpha(0.0F, 0.6F, 0.0F)
                    .color("#E8E8EC", "#A8A8B0"),
                new FxPresets.L("Пыль")
                    .shape("disc", 1.5F)
                    .rate(25.0F)
                    .dir("up", 2.0F, 0.5F)
                    .orbit(300.0F)
                    .smoke()
                    .life(1.0F, 0.4F)
                    .size(0.5F, 1.0F)
                    .alpha(0.0F, 0.5F, 0.0F)
                    .color("#B8A890")
            )
        );
        preset(
            "star_shower",
            "Звездопад",
            "Сверкающие звёзды с хвостами падают с неба несколько секунд.",
            () -> fx(
                6.0F,
                "minecraft:block.amethyst_block.chime",
                1.0F,
                2.0F,
                new FxPresets.L("Звёзды")
                    .shape("disc", 6.0F)
                    .at(0.0F, 18.0F, 0.0F)
                    .rate(12.0F)
                    .time(5.0F)
                    .dir("cone", 14.0F, 0.2F)
                    .angle(15.0F)
                    .rot(25.0F, 0.0F, 0.0F)
                    .tex("spark")
                    .stretch(0.35F)
                    .life(1.4F, 0.3F)
                    .size(0.35F, 0.1F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#BFE6FF", "#FFE8A0")
                    .glow(2.0F)
            )
        );
        preset(
            "ghost_mist",
            "Призрачный туман",
            "Низкий бледный туман стелется по земле (до fx stop).",
            () -> fx(
                0.0F,
                "",
                1.0F,
                1.0F,
                new FxPresets.L("Туман")
                    .shape("disc", 4.0F)
                    .at(0.0F, 0.3F, 0.0F)
                    .rate(10.0F)
                    .dir("random", 0.15F, 0.5F)
                    .turb(0.4F)
                    .smoke()
                    .life(5.0F, 0.3F)
                    .size(2.0F, 3.5F)
                    .alpha(0.0F, 0.35F, 0.0F)
                    .color("#C8D8E0", "#A8B8C8")
            )
        );
        preset(
            "confetti",
            "Конфетти",
            "Хлопушка: разноцветные бумажки разлетаются и кружась падают.",
            () -> fx(
                4.0F,
                "minecraft:entity.firework_rocket.twinkle",
                1.0F,
                1.2F,
                new FxPresets.L("Конфетти")
                    .at(0.0F, 1.0F, 0.0F)
                    .burst(140)
                    .dir("cone", 8.0F, 0.5F)
                    .angle(35.0F)
                    .gravity(4.0F)
                    .drag(1.5F)
                    .turb(1.0F)
                    .tex("square")
                    .life(3.0F, 0.4F)
                    .size(0.12F)
                    .spin(0.0F, 600.0F)
                    .alpha(1.0F, 1.0F, 0.0F)
                    .color("#FF4A6A", "#FFD040", "#40E0FF", "#9A6AFF", "#60FF80")
                    .colorRandom()
            )
        );
        preset(
            "hearts",
            "Сердечки",
            "Розовое сияние и сердечки поднимаются — влюблённость, дружба.",
            () -> fx(
                2.5F,
                "minecraft:entity.villager.celebrate",
                0.7F,
                1.3F,
                new FxPresets.L("Сердечки")
                    .shape("disc", 0.6F)
                    .at(0.0F, 1.6F, 0.0F)
                    .rate(10.0F)
                    .time(1.8F)
                    .dir("up", 0.8F, 0.4F)
                    .tex("star")
                    .life(1.4F, 0.3F)
                    .size(0.35F, 0.2F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FF6A9A", "#FF3A6A")
                    .glow(1.4F),
                new FxPresets.L("Сияние")
                    .at(0.0F, 1.6F, 0.0F)
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(1.5F, 0.0F)
                    .size(0.5F, 2.0F, 0.0F)
                    .sizeRandom(0.0F)
                    .alpha(0.0F, 0.6F, 0.0F)
                    .color("#FFB8D0")
                    .glow(1.4F)
            )
        );
        preset(
            "loot_burst",
            "Награда",
            "Золотые искры и монетки выпрыгивают вверх и падают — сундук, награда за квест.",
            () -> fx(
                2.5F,
                "minecraft:entity.player.levelup",
                0.6F,
                1.6F,
                new FxPresets.L("Монетки")
                    .at(0.0F, 0.5F, 0.0F)
                    .burst(30)
                    .dir("cone", 6.0F, 0.4F)
                    .angle(30.0F)
                    .gravity(12.0F)
                    .tex("glow")
                    .life(1.4F, 0.3F)
                    .size(0.2F)
                    .spin(0.0F, 300.0F)
                    .alpha(1.0F, 1.0F, 0.0F)
                    .color("#FFD040", "#FFB020")
                    .glow(1.4F),
                new FxPresets.L("Искры")
                    .at(0.0F, 0.5F, 0.0F)
                    .burst(40)
                    .dir("random", 3.0F, 0.5F)
                    .drag(2.0F)
                    .tex("star")
                    .life(1.0F, 0.4F)
                    .size(0.18F, 0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#FFE88A")
                    .glow(1.6F)
            )
        );
        preset(
            "quest_marker",
            "Метка квеста",
            "Золотой столбик света и вращающаяся звезда над точкой (до fx stop) — куда идти.",
            () -> fx(
                0.0F,
                "",
                1.0F,
                1.0F,
                new FxPresets.L("Столб")
                    .burst(1)
                    .max(1)
                    .dir("up", 0.0F, 0.0F)
                    .tex("beam")
                    .beam(6.0F)
                    .life(1000000.0F, 0.0F)
                    .size(0.6F)
                    .sizeRandom(0.0F)
                    .alpha(0.6F)
                    .color("#FFE88A")
                    .glow(1.4F),
                new FxPresets.L("Звезда")
                    .at(0.0F, 2.5F, 0.0F)
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("star")
                    .life(1000000.0F, 0.0F)
                    .size(0.8F)
                    .sizeRandom(0.0F)
                    .spin(90.0F, 0.0F)
                    .alpha(1.0F)
                    .color("#FFD040")
                    .glow(2.0F),
                new FxPresets.L("Искорки")
                    .shape("ring", 0.5F)
                    .rate(8.0F)
                    .dir("up", 1.0F, 0.4F)
                    .tex("glow")
                    .life(1.5F, 0.3F)
                    .size(0.1F, 0.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFE88A")
                    .glow(1.6F)
            )
        );
    }

    static {
        preset(
            "ice_burst",
            "Ледяной всплеск",
            "Голубые осколки и быстро расходящееся морозное кольцо.",
            () -> fx(
                1.8F,
                "minecraft:block.glass.break",
                0.7F,
                1.3F,
                new FxPresets.L("Осколки")
                    .burst(60)
                    .dir("random", 5.0F, 0.5F)
                    .gravity(4.0F)
                    .drag(1.0F)
                    .tex("spark")
                    .life(0.8F, 0.4F)
                    .size(0.18F, 0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#C8F7FF", "#59AAFF")
                    .glow(1.3F),
                new FxPresets.L("Морозное кольцо").burst(1).tex("ring").life(0.6F, 0.0F).size(0.2F, 3.0F).alpha(1.0F, 0.0F).color("#A8EFFF").glow(1.5F)
            )
        );
        preset(
            "water_splash",
            "Всплеск воды",
            "Капли разлетаются и падают вниз.",
            () -> fx(
                1.5F,
                "minecraft:entity.generic.splash",
                0.7F,
                1.1F,
                new FxPresets.L("Капли")
                    .shape("sphere", 0.3F)
                    .burst(65)
                    .dir("random", 4.0F, 0.7F)
                    .gravity(9.0F)
                    .tex("glow")
                    .life(0.7F, 0.4F)
                    .size(0.12F, 0.02F)
                    .alpha(0.9F, 0.0F)
                    .color("#8CDFFF", "#317BFF")
            )
        );
        preset(
            "poison_cloud",
            "Ядовитое облако",
            "Зелёный дым и поднимающиеся пузырьки; затухает через четыре секунды.",
            () -> fx(
                4.0F,
                "minecraft:block.brewing_stand.brew",
                0.6F,
                0.8F,
                new FxPresets.L("Туман")
                    .shape("sphere", 1.0F)
                    .rate(20.0F)
                    .time(2.5F)
                    .dir("random", 0.4F, 0.5F)
                    .tex("smoke")
                    .life(1.4F, 0.5F)
                    .size(0.5F, 1.2F)
                    .alpha(0.0F, 0.4F, 0.0F)
                    .color("#6EAA26", "#38552A"),
                new FxPresets.L("Пузырьки")
                    .shape("disc", 1.0F)
                    .rate(12.0F)
                    .time(2.5F)
                    .dir("up", 0.7F, 0.5F)
                    .tex("ring")
                    .life(0.8F, 0.4F)
                    .size(0.12F, 0.0F)
                    .alpha(0.8F, 0.0F)
                    .color("#BEFF65")
            )
        );
        preset(
            "shadow_smoke",
            "Теневой дым",
            "Тёмный фиолетовый дым с редкими искрами.",
            () -> fx(
                3.0F,
                "minecraft:entity.enderman.teleport",
                0.5F,
                0.7F,
                new FxPresets.L("Тень")
                    .shape("sphere", 0.6F)
                    .burst(45)
                    .dir("random", 0.8F, 0.5F)
                    .tex("smoke")
                    .life(1.5F, 0.5F)
                    .size(0.3F, 1.1F)
                    .alpha(0.0F, 0.7F, 0.0F)
                    .color("#291A38", "#110D1A"),
                new FxPresets.L("Искры")
                    .burst(20)
                    .dir("random", 1.5F, 0.5F)
                    .tex("star")
                    .life(1.0F, 0.4F)
                    .size(0.16F, 0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#B276FF")
                    .glow(1.4F)
            )
        );
        preset(
            "holy_burst",
            "Вспышка света",
            "Тёплое сияние и золотые звёзды без вспышки на весь экран.",
            () -> fx(
                2.0F,
                "minecraft:block.amethyst_block.chime",
                0.8F,
                1.4F,
                new FxPresets.L("Сияние").burst(1).tex("flare").life(0.7F, 0.0F).size(0.2F, 2.5F, 0.0F).alpha(0.0F, 1.0F, 0.0F).color("#FFF2BB").glow(2.0F),
                new FxPresets.L("Звёзды")
                    .burst(45)
                    .dir("random", 2.0F, 0.7F)
                    .drag(2.0F)
                    .tex("star")
                    .life(1.0F, 0.5F)
                    .size(0.2F, 0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#FFD65A")
                    .glow(1.5F)
            )
        );
        preset(
            "level_up",
            "Новый уровень",
            "Золотые искры поднимаются вокруг персонажа, у ног расширяется круг.",
            () -> fx(
                2.8F,
                "minecraft:entity.player.levelup",
                0.8F,
                1.0F,
                new FxPresets.L("Искры")
                    .shape("ring", 0.7F)
                    .rate(45.0F)
                    .time(1.8F)
                    .dir("up", 2.0F, 0.4F)
                    .tex("star")
                    .life(0.9F, 0.3F)
                    .size(0.18F, 0.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFE26A", "#FFFFFF")
                    .glow(1.5F),
                new FxPresets.L("Круг").burst(1).tex("ring").life(1.0F, 0.0F).size(0.3F, 2.5F).alpha(1.0F, 0.0F).color("#FFE26A").glow(1.5F)
            )
        );
        preset(
            "teleport_in",
            "Появление",
            "Световой вихрь стягивается к точке и гаснет.",
            () -> fx(
                1.8F,
                "minecraft:entity.enderman.teleport",
                0.7F,
                1.2F,
                new FxPresets.L("Вихрь")
                    .shape("sphere", 1.5F)
                    .at(0.0F, 1.0F, 0.0F)
                    .burst(65)
                    .dir("none", 0.0F, 0.0F)
                    .attract(5.0F)
                    .orbit(180.0F)
                    .tex("glow")
                    .life(1.1F, 0.3F)
                    .size(0.16F, 0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#8EEFFF", "#C58AFF")
                    .glow(1.6F),
                new FxPresets.L("Свет")
                    .at(0.0F, 1.0F, 0.0F)
                    .burst(1)
                    .delay(0.7F)
                    .tex("flare")
                    .life(0.4F, 0.0F)
                    .size(1.5F, 0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF")
                    .glow(2.0F)
            )
        );
        preset(
            "dust_impact",
            "Удар по земле",
            "Пыль разлетается от места удара и медленно оседает.",
            () -> fx(
                2.0F,
                "minecraft:block.stone.break",
                0.7F,
                0.8F,
                new FxPresets.L("Пыль")
                    .shape("disc", 0.3F)
                    .burst(50)
                    .dir("random", 2.5F, 0.4F)
                    .gravity(1.0F)
                    .drag(2.0F)
                    .tex("smoke")
                    .life(1.0F, 0.5F)
                    .size(0.2F, 0.7F)
                    .alpha(0.6F, 0.0F)
                    .color("#A48B69", "#716552")
            )
        );
        preset(
            "embers",
            "Тлеющие угли",
            "Небольшие угольки поднимаются вверх; работает до fx stop.",
            () -> fx(
                0.0F,
                "",
                0.0F,
                1.0F,
                new FxPresets.L("Угли")
                    .shape("disc", 0.7F)
                    .rate(15.0F)
                    .dir("up", 0.7F, 0.6F)
                    .turb(0.5F)
                    .tex("spark")
                    .life(1.8F, 0.7F)
                    .size(0.08F, 0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFAF36", "#E54B14")
                    .glow(1.5F)
            )
        );
        preset(
            "snowfall",
            "Снежинки",
            "Медленный снег над точкой в течение пяти секунд.",
            () -> fx(
                5.0F,
                "",
                0.0F,
                1.0F,
                new FxPresets.L("Снег")
                    .shape("disc", 2.0F)
                    .at(0.0F, 3.0F, 0.0F)
                    .rate(24.0F)
                    .time(3.0F)
                    .dir("down", 0.7F, 0.3F)
                    .turb(0.3F)
                    .tex("star")
                    .life(2.0F, 0.5F)
                    .size(0.08F)
                    .spin(0.0F, 100.0F)
                    .alpha(0.0F, 0.8F, 0.0F)
                    .color("#E6F5FF")
            )
        );
        preset(
            "nuke",
            "Ядерный взрыв",
            "Вспышка, огненный шар, гриб до неба, ударная волна по земле, искры и пыль. Масштаб 1 — гриб высотой около 35 блоков.",
            () -> fx(
                14.0F,
                "minecraft:entity.generic.explode",
                6.0F,
                0.35F,
                flash("#FFF6E0", 1.0F, 1.6F, 260.0F),
                shake(1.0F, 5.0F, 220.0F, 0.15F),
                new FxPresets.L("Ядро вспышки")
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(1.6F, 0.0F)
                    .size(4.0F, 70.0F, 50.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.7F, 0.0F)
                    .color("#FFFFFF", "#FFF0B0")
                    .glow(2.0F),
                new FxPresets.L("Огненный шар")
                    .shape("sphere", 3.0F)
                    .burst(160)
                    .dir("out", 7.0F, 0.5F)
                    .drag(1.6F)
                    .gravity(-1.2F)
                    .turb(1.5F)
                    .tex("smoke")
                    .life(3.5F, 0.3F)
                    .size(4.0F, 9.0F, 11.0F)
                    .alpha(1.0F, 0.9F, 0.0F)
                    .color("#FFFFFF", "#FFE08A", "#FF8A2A", "#B8380F", "#401008")
                    .glow(1.6F),
                new FxPresets.L("Ножка гриба")
                    .shape("disc", 2.5F)
                    .rate(70.0F)
                    .time(4.5F)
                    .delay(0.3F)
                    .dir("up", 9.0F, 0.3F)
                    .drag(0.25F)
                    .turb(0.8F)
                    .smoke()
                    .life(4.5F, 0.3F)
                    .size(3.0F, 5.0F, 6.0F)
                    .alpha(0.0F, 0.95F, 0.8F, 0.0F)
                    .max(500)
                    .color("#FFB060", "#A05A30", "#5A4036", "#3A3634"),
                new FxPresets.L("Шляпка гриба")
                    .shape("shell", 7.0F)
                    .inner(3.0F)
                    .at(0.0F, 30.0F, 0.0F)
                    .rate(130.0F)
                    .time(4.0F)
                    .delay(1.2F)
                    .dir("out", 2.5F, 0.6F)
                    .orbit(16.0F)
                    .gravity(-0.4F)
                    .drag(0.4F)
                    .turb(1.2F)
                    .smoke()
                    .life(7.0F, 0.3F)
                    .size(6.0F, 11.0F, 13.0F)
                    .alpha(0.0F, 0.95F, 0.9F, 0.0F)
                    .max(700)
                    .color("#FFC27A", "#C26A38", "#6E4A3C", "#3C3836", "#2A2828"),
                new FxPresets.L("Огонь в шляпке")
                    .shape("shell", 5.0F)
                    .at(0.0F, 30.0F, 0.0F)
                    .rate(70.0F)
                    .time(3.5F)
                    .delay(1.2F)
                    .dir("out", 1.5F, 0.6F)
                    .orbit(16.0F)
                    .turb(1.0F)
                    .tex("smoke")
                    .life(2.5F, 0.3F)
                    .size(4.0F, 7.0F)
                    .alpha(0.0F, 0.8F, 0.0F)
                    .color("#FFE6A0", "#FF8A2A", "#802010")
                    .glow(1.4F),
                new FxPresets.L("Ударная волна")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.4F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(2.6F, 0.0F)
                    .size(3.0F, 140.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.8F, 0.0F)
                    .color("#FFFFFF", "#FFE2B8")
                    .glow(1.5F),
                new FxPresets.L("Кольцо конденсации")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 18.0F, 0.0F)
                    .delay(1.4F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(3.0F, 0.0F)
                    .size(6.0F, 46.0F)
                    .sizeRandom(0.0F)
                    .alpha(0.0F, 0.7F, 0.0F)
                    .color("#FFFFFF")
                    .glow(1.0F)
                    .spin(0.0F, 0.0F),
                new FxPresets.L("Пыль по земле")
                    .shape("ring", 3.0F)
                    .burst(320)
                    .dir("out", 26.0F, 0.4F)
                    .drag(1.1F)
                    .gravity(0.0F)
                    .smoke()
                    .life(4.5F, 0.3F)
                    .size(3.0F, 7.0F, 9.0F)
                    .alpha(0.0F, 0.9F, 0.0F)
                    .color("#B09070", "#6E5E50", "#4A4440")
                    .at(0.0F, 0.6F, 0.0F),
                new FxPresets.L("Искры")
                    .shape("sphere", 2.0F)
                    .burst(450)
                    .dir("out", 32.0F, 0.6F)
                    .gravity(10.0F)
                    .drag(0.7F)
                    .tex("spark")
                    .stretch(0.12F)
                    .life(2.6F, 0.4F)
                    .size(0.5F, 0.15F)
                    .alpha(1.0F, 1.0F, 0.0F)
                    .color("#FFFFFF", "#FFC040", "#FF5010")
                    .glow(2.0F),
                new FxPresets.L("Обломки")
                    .shape("sphere", 1.5F)
                    .burst(120)
                    .dir("cone", 22.0F, 0.5F)
                    .angle(70.0F)
                    .gravity(14.0F)
                    .drag(0.3F)
                    .tex("square")
                    .blend("alpha")
                    .life(3.0F, 0.3F)
                    .size(0.35F, 0.3F)
                    .spin(0.0F, 400.0F)
                    .alpha(1.0F, 1.0F, 0.0F)
                    .color("#3A2A20", "#1E1A18")
                    .l
            )
        );
        preset(
            "orbit_orbs",
            "Орбы вокруг игрока",
            "Светящиеся сферы кружат вокруг цели, за ними светящееся кольцо и искорки. Играйте с follow, останавливайте fx stop.",
            () -> fx(
                0.0F,
                "minecraft:block.amethyst_block.chime",
                1.0F,
                1.4F,
                new FxPresets.L("Орбы")
                    .shape("ring", 1.4F)
                    .even()
                    .at(0.0F, 1.1F, 0.0F)
                    .burst(5)
                    .max(5)
                    .dir("none", 0.0F, 0.0F)
                    .orbit(110.0F)
                    .tex("flare")
                    .life(1000000.0F, 0.0F)
                    .size(0.55F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F)
                    .color("#8FE3FF")
                    .glow(1.8F),
                new FxPresets.L("Кольцо света")
                    .shape("ring", 1.4F)
                    .at(0.0F, 1.1F, 0.0F)
                    .rate(70.0F)
                    .dir("none", 0.0F, 0.0F)
                    .orbit(110.0F)
                    .tex("glow")
                    .life(0.7F, 0.2F)
                    .size(0.22F, 0.0F)
                    .alpha(0.0F, 0.8F, 0.0F)
                    .color("#8FE3FF", "#A070FF")
                    .glow(1.3F),
                new FxPresets.L("Искорки")
                    .shape("disc", 1.3F)
                    .rate(14.0F)
                    .dir("up", 0.7F, 0.4F)
                    .drag(0.2F)
                    .tex("star")
                    .life(1.6F, 0.3F)
                    .size(0.18F, 0.0F)
                    .spin(0.0F, 180.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFFFFF", "#8FE3FF")
            )
        );
        preset(
            "sky_beam",
            "Луч в небо",
            "Столб света уходит в небо, вокруг него спираль, у основания волны и искры.",
            () -> fx(
                6.0F,
                "minecraft:block.beacon.activate",
                2.0F,
                1.4F,
                flash("#BFF1FF", 0.35F, 0.5F, 60.0F),
                shake(0.35F, 1.2F, 50.0F, 0.0F),
                new FxPresets.L("Луч")
                    .burst(1)
                    .max(1)
                    .dir("up", 0.0F, 0.0F)
                    .tex("beam")
                    .beam(220.0F)
                    .life(6.0F, 0.0F)
                    .size(0.0F, 3.2F, 2.6F, 2.6F, 0.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F)
                    .color("#7FDFFF", "#BFF1FF")
                    .glow(1.6F),
                new FxPresets.L("Ядро луча")
                    .burst(1)
                    .max(1)
                    .dir("up", 0.0F, 0.0F)
                    .tex("beam")
                    .beam(220.0F)
                    .life(6.0F, 0.0F)
                    .size(0.0F, 1.2F, 0.9F, 0.9F, 0.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F)
                    .color("#FFFFFF")
                    .glow(2.5F),
                new FxPresets.L("Спираль")
                    .shape("ring", 1.3F)
                    .rate(90.0F)
                    .time(5.0F)
                    .dir("up", 9.0F, 0.1F)
                    .drag(0.0F)
                    .orbit(220.0F)
                    .tex("glow")
                    .life(2.6F, 0.2F)
                    .size(0.35F, 0.1F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#BFF1FF", "#3FA0FF")
                    .glow(1.5F),
                new FxPresets.L("Волны у земли")
                    .burst(1)
                    .every(0.7F)
                    .time(5.0F)
                    .max(10)
                    .at(0.0F, 0.15F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(1.3F, 0.0F)
                    .size(1.0F, 9.0F)
                    .sizeRandom(0.0F)
                    .alpha(0.9F, 0.0F)
                    .color("#8FE3FF")
                    .glow(1.3F),
                new FxPresets.L("Искры у основания")
                    .shape("disc", 1.4F)
                    .rate(70.0F)
                    .time(5.0F)
                    .dir("out", 4.0F, 0.5F)
                    .gravity(-3.0F)
                    .drag(0.6F)
                    .tex("spark")
                    .stretch(0.2F)
                    .life(0.9F, 0.3F)
                    .size(0.25F, 0.05F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#7FDFFF")
                    .glow(1.8F)
            )
        );
        preset(
            "fire",
            "Огонь",
            "Костёр: языки пламени, искры-угольки, дым и тёплое свечение. Горит, пока не остановят (fx stop).",
            () -> fx(
                0.0F,
                "minecraft:block.campfire.crackle",
                1.0F,
                1.0F,
                new FxPresets.L("Свечение")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.5F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(1000000.0F, 0.0F)
                    .size(2.6F)
                    .sizeRandom(0.0F)
                    .alpha(0.45F)
                    .color("#FF8A30")
                    .glow(1.0F),
                new FxPresets.L("Пламя")
                    .shape("disc", 0.35F)
                    .rate(70.0F)
                    .dir("up", 1.4F, 0.4F)
                    .drag(0.6F)
                    .gravity(-1.5F)
                    .turb(1.6F)
                    .tex("smoke")
                    .life(0.75F, 0.3F)
                    .size(0.55F, 0.75F, 0.15F)
                    .spin(0.0F, 120.0F)
                    .alpha(0.0F, 1.0F, 0.8F, 0.0F)
                    .color("#FFFFFF", "#FFE07A", "#FF9A2A", "#E04010", "#401008")
                    .glow(1.6F),
                new FxPresets.L("Ядро")
                    .shape("disc", 0.2F)
                    .rate(40.0F)
                    .dir("up", 0.8F, 0.3F)
                    .turb(0.8F)
                    .tex("glow")
                    .life(0.45F, 0.3F)
                    .size(0.45F, 0.1F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFFFFF", "#FFD060")
                    .glow(2.0F),
                new FxPresets.L("Угольки")
                    .shape("disc", 0.4F)
                    .rate(6.0F)
                    .dir("up", 2.2F, 0.6F)
                    .gravity(-0.5F)
                    .drag(0.4F)
                    .turb(2.5F)
                    .tex("spark")
                    .stretch(0.25F)
                    .life(1.6F, 0.5F)
                    .size(0.09F, 0.03F)
                    .alpha(1.0F, 1.0F, 0.0F)
                    .color("#FFE6A0", "#FF7A20")
                    .glow(2.0F),
                new FxPresets.L("Дым")
                    .shape("disc", 0.3F)
                    .at(0.0F, 1.1F, 0.0F)
                    .rate(8.0F)
                    .dir("up", 0.9F, 0.4F)
                    .drag(0.2F)
                    .turb(0.8F)
                    .smoke()
                    .life(3.0F, 0.4F)
                    .size(0.6F, 2.0F)
                    .spin(0.0F, 30.0F)
                    .alpha(0.0F, 0.35F, 0.0F)
                    .color("#5A5450", "#3A3836")
            )
        );
        preset(
            "standing_portal",
            "Портал (стоячий)",
            "Вертикальное кольцо-портал: пример поворота слоя (Поворот X 90).",
            () -> fx(
                0.0F,
                "minecraft:block.portal.ambient",
                0.8F,
                1.0F,
                new FxPresets.L("Рамка")
                    .shape("ring", 1.5F)
                    .even()
                    .rot(90.0F, 0.0F, 0.0F)
                    .at(0.0F, 1.6F, 0.0F)
                    .burst(24)
                    .max(24)
                    .dir("none", 0.0F, 0.0F)
                    .orbit(70.0F)
                    .tex("glow")
                    .life(1000000.0F, 0.0F)
                    .size(0.35F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F)
                    .color("#C08AFF")
                    .glow(1.6F),
                new FxPresets.L("Вихрь")
                    .shape("ring", 1.4F)
                    .inner(0.2F)
                    .rot(90.0F, 0.0F, 0.0F)
                    .at(0.0F, 1.6F, 0.0F)
                    .rate(90.0F)
                    .dir("tangent", 1.5F, 0.3F)
                    .attract(3.0F)
                    .orbit(200.0F)
                    .tex("glow")
                    .life(1.0F, 0.3F)
                    .size(0.22F, 0.05F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#F0C0FF", "#6A2AFF")
                    .glow(1.4F),
                new FxPresets.L("Плёнка")
                    .burst(1)
                    .max(1)
                    .rot(90.0F, 0.0F, 0.0F)
                    .at(0.0F, 1.6F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("glow")
                    .render("flat")
                    .life(1000000.0F, 0.0F)
                    .size(3.2F)
                    .sizeRandom(0.0F)
                    .spin(40.0F, 0.0F)
                    .alpha(0.55F)
                    .color("#7A3AFF")
                    .glow(1.0F)
            )
        );
        preset(
            "explosion",
            "Взрыв",
            "Обычный взрыв: огонь, дым, искры и волна.",
            () -> fx(
                4.0F,
                "minecraft:entity.generic.explode",
                2.0F,
                0.9F,
                flash("#FFE8C0", 0.35F, 0.35F, 24.0F),
                shake(0.55F, 0.9F, 30.0F, 0.0F),
                new FxPresets.L("Огонь")
                    .shape("sphere", 0.6F)
                    .burst(50)
                    .dir("out", 4.0F, 0.5F)
                    .drag(2.5F)
                    .gravity(-1.5F)
                    .tex("smoke")
                    .life(0.9F, 0.3F)
                    .size(1.2F, 2.6F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#FFC060", "#FF5A1A", "#601808")
                    .glow(1.5F),
                new FxPresets.L("Дым")
                    .shape("sphere", 0.8F)
                    .burst(40)
                    .dir("out", 2.0F, 0.5F)
                    .drag(1.5F)
                    .gravity(-1.0F)
                    .turb(1.0F)
                    .smoke()
                    .delay(0.1F)
                    .life(2.6F, 0.4F)
                    .size(1.5F, 3.4F)
                    .alpha(0.0F, 0.8F, 0.0F)
                    .color("#5A5450", "#2E2C2A"),
                new FxPresets.L("Искры")
                    .burst(70)
                    .dir("random", 9.0F, 0.6F)
                    .gravity(9.0F)
                    .drag(0.6F)
                    .tex("spark")
                    .stretch(0.12F)
                    .life(1.0F, 0.4F)
                    .size(0.22F, 0.05F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#FFB040")
                    .glow(1.8F),
                new FxPresets.L("Волна")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.1F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(0.6F, 0.0F)
                    .size(1.0F, 9.0F)
                    .sizeRandom(0.0F)
                    .alpha(0.9F, 0.0F)
                    .color("#FFE6C0")
            )
        );
        preset(
            "magic_circle",
            "Магический круг",
            "Вращающийся круг на земле, руны-звёзды и поднимающийся свет.",
            () -> fx(
                5.0F,
                "minecraft:block.enchantment_table.use",
                1.5F,
                0.8F,
                new FxPresets.L("Круг")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.05F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(5.0F, 0.0F)
                    .size(0.0F, 5.0F, 5.0F, 5.0F, 0.0F)
                    .sizeRandom(0.0F)
                    .spin(40.0F, 0.0F)
                    .alpha(0.0F, 1.0F, 1.0F, 0.0F)
                    .color("#C08AFF")
                    .glow(1.5F),
                new FxPresets.L("Внутренний круг")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.06F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(5.0F, 0.0F)
                    .size(0.0F, 3.0F, 3.0F, 3.0F, 0.0F)
                    .sizeRandom(0.0F)
                    .spin(-70.0F, 0.0F)
                    .alpha(0.0F, 1.0F, 1.0F, 0.0F)
                    .color("#FF9AF0")
                    .glow(1.5F),
                new FxPresets.L("Руны")
                    .shape("ring", 2.2F)
                    .even()
                    .at(0.0F, 0.3F, 0.0F)
                    .burst(8)
                    .max(8)
                    .dir("none", 0.0F, 0.0F)
                    .orbit(40.0F)
                    .tex("star")
                    .life(5.0F, 0.0F)
                    .size(0.0F, 0.6F, 0.6F, 0.0F)
                    .sizeRandom(0.0F)
                    .spin(90.0F, 0.0F)
                    .alpha(0.0F, 1.0F, 1.0F, 0.0F)
                    .color("#FFFFFF", "#C08AFF")
                    .glow(1.6F),
                new FxPresets.L("Свет вверх")
                    .shape("disc", 2.3F)
                    .rate(60.0F)
                    .time(4.5F)
                    .dir("up", 1.6F, 0.4F)
                    .drag(0.1F)
                    .tex("glow")
                    .life(1.8F, 0.3F)
                    .size(0.25F, 0.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFB8F8", "#8A5AFF")
                    .glow(1.4F)
            )
        );
        preset(
            "portal",
            "Портал",
            "Фиолетовый вихрь, затягивающий частицы к центру.",
            () -> fx(
                0.0F,
                "minecraft:block.portal.ambient",
                1.0F,
                1.0F,
                new FxPresets.L("Вихрь")
                    .shape("ring", 2.6F)
                    .inner(1.5F)
                    .at(0.0F, 1.4F, 0.0F)
                    .rate(140.0F)
                    .dir("tangent", 2.0F, 0.3F)
                    .attract(5.0F)
                    .orbit(160.0F)
                    .drag(0.4F)
                    .tex("glow")
                    .life(1.2F, 0.3F)
                    .size(0.3F, 0.05F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#E0A0FF", "#7A2AFF", "#2A0A60")
                    .glow(1.4F),
                new FxPresets.L("Центр")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 1.4F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(1000000.0F, 0.0F)
                    .size(2.4F)
                    .sizeRandom(0.0F)
                    .alpha(0.9F)
                    .color("#B060FF")
                    .glow(1.5F),
                new FxPresets.L("Кольцо")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 1.4F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .life(1000000.0F, 0.0F)
                    .size(5.6F)
                    .sizeRandom(0.0F)
                    .spin(60.0F, 0.0F)
                    .alpha(0.8F)
                    .color("#C080FF")
            )
        );
        preset(
            "fire_tornado",
            "Огненный смерч",
            "Крутящийся столб огня и дыма.",
            () -> fx(
                6.0F,
                "minecraft:item.firecharge.use",
                1.5F,
                0.6F,
                new FxPresets.L("Огонь")
                    .shape("ring", 0.6F)
                    .rate(180.0F)
                    .dir("up", 5.0F, 0.3F)
                    .orbit(360.0F)
                    .drag(0.0F)
                    .tex("smoke")
                    .life(1.6F, 0.3F)
                    .size(0.8F, 1.6F, 0.4F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFFFFF", "#FFC040", "#FF4A10", "#401008")
                    .glow(1.5F),
                new FxPresets.L("Дым")
                    .shape("ring", 1.2F)
                    .at(0.0F, 6.0F, 0.0F)
                    .rate(40.0F)
                    .dir("up", 2.0F, 0.5F)
                    .orbit(200.0F)
                    .turb(1.0F)
                    .smoke()
                    .life(2.5F, 0.3F)
                    .size(1.5F, 3.5F)
                    .alpha(0.0F, 0.7F, 0.0F)
                    .color("#3A3634", "#1E1C1C"),
                new FxPresets.L("Искры")
                    .shape("disc", 1.5F)
                    .rate(60.0F)
                    .dir("up", 7.0F, 0.5F)
                    .orbit(300.0F)
                    .gravity(2.0F)
                    .tex("spark")
                    .stretch(0.15F)
                    .life(1.2F, 0.4F)
                    .size(0.2F, 0.05F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#FF9A30")
                    .glow(1.8F)
            )
        );
        preset(
            "lightning",
            "Удар молнии",
            "Разряд с неба, вспышка, гром и искры.",
            () -> fx(
                2.5F,
                "minecraft:entity.lightning_bolt.thunder",
                3.0F,
                1.0F,
                flash("#E6F0FF", 0.75F, 0.45F, 80.0F),
                shake(0.6F, 0.8F, 60.0F, 0.0F),
                new FxPresets.L("Разряд")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.0F, 0.0F)
                    .dir("up", 0.0F, 0.0F)
                    .tex("beam")
                    .beam(90.0F)
                    .life(0.45F, 0.0F)
                    .size(2.2F, 1.6F, 0.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#9FC8FF")
                    .glow(3.0F),
                new FxPresets.L("Свечение")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.5F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(0.7F, 0.0F)
                    .size(10.0F, 4.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#CFE2FF")
                    .glow(2.0F),
                new FxPresets.L("Искры")
                    .burst(90)
                    .dir("cone", 10.0F, 0.6F)
                    .angle(80.0F)
                    .gravity(12.0F)
                    .drag(0.5F)
                    .tex("spark")
                    .stretch(0.1F)
                    .life(1.0F, 0.4F)
                    .size(0.2F, 0.05F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#8FB8FF")
                    .glow(2.0F)
            )
        );
        preset(
            "fireworks",
            "Салют",
            "Разноцветный залп с хвостами и мерцанием.",
            () -> fx(
                3.5F,
                "minecraft:entity.firework_rocket.large_blast",
                2.0F,
                1.0F,
                new FxPresets.L("Залп")
                    .at(0.0F, 12.0F, 0.0F)
                    .burst(260)
                    .dir("random", 11.0F, 0.15F)
                    .gravity(4.0F)
                    .drag(1.4F)
                    .tex("spark")
                    .stretch(0.18F)
                    .life(2.4F, 0.4F)
                    .size(0.35F, 0.1F)
                    .alpha(1.0F, 1.0F, 0.0F)
                    .color("#FF4A6A", "#FFD040", "#40E0FF", "#9A6AFF", "#60FF80")
                    .colorRandom()
                    .glow(2.0F),
                new FxPresets.L("Мерцание")
                    .at(0.0F, 12.0F, 0.0F)
                    .shape("sphere", 9.0F)
                    .delay(1.0F)
                    .burst(120)
                    .dir("down", 0.6F, 0.5F)
                    .tex("star")
                    .life(1.2F, 0.5F)
                    .size(0.25F, 0.0F)
                    .spin(0.0F, 300.0F)
                    .alpha(0.0F, 1.0F, 0.0F, 1.0F, 0.0F)
                    .color("#FFFFFF", "#FFE8A0")
                    .glow(1.5F),
                new FxPresets.L("Вспышка")
                    .at(0.0F, 12.0F, 0.0F)
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(0.5F, 0.0F)
                    .size(9.0F, 3.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF")
            )
        );
        preset(
            "heal",
            "Лечение",
            "Зелёное свечение и плюсы-искорки поднимаются вокруг цели.",
            () -> fx(
                2.5F,
                "minecraft:entity.player.levelup",
                0.8F,
                1.6F,
                new FxPresets.L("Свет")
                    .shape("ring", 0.9F)
                    .rate(80.0F)
                    .time(1.6F)
                    .dir("up", 1.4F, 0.4F)
                    .drag(0.3F)
                    .orbit(90.0F)
                    .tex("glow")
                    .life(1.3F, 0.3F)
                    .size(0.28F, 0.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#C8FFC0", "#40E060")
                    .glow(1.4F),
                new FxPresets.L("Кресты")
                    .shape("disc", 0.9F)
                    .rate(16.0F)
                    .time(1.6F)
                    .at(0.0F, 0.5F, 0.0F)
                    .dir("up", 0.9F, 0.3F)
                    .tex("star")
                    .life(1.4F, 0.3F)
                    .size(0.35F, 0.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFFFFF", "#7AFF8A")
                    .glow(1.5F),
                new FxPresets.L("Кольцо")
                    .burst(1)
                    .max(1)
                    .at(0.0F, 0.1F, 0.0F)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .render("flat")
                    .life(1.0F, 0.0F)
                    .size(0.5F, 3.5F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#7AFF8A")
                    .glow(1.2F)
            )
        );
        preset(
            "aura",
            "Аура",
            "Пламя силы вокруг персонажа (с follow, до fx stop).",
            () -> fx(
                0.0F,
                "",
                1.0F,
                1.0F,
                new FxPresets.L("Пламя")
                    .shape("ring", 0.55F)
                    .rate(90.0F)
                    .dir("up", 1.8F, 0.4F)
                    .drag(0.4F)
                    .turb(1.2F)
                    .tex("smoke")
                    .life(1.0F, 0.3F)
                    .size(0.6F, 0.9F, 0.1F)
                    .alpha(0.0F, 0.8F, 0.0F)
                    .color("#FFFFFF", "#7FD0FF", "#3A60FF")
                    .glow(1.4F),
                new FxPresets.L("Искры")
                    .shape("ring", 0.7F)
                    .rate(20.0F)
                    .dir("up", 3.0F, 0.5F)
                    .tex("spark")
                    .stretch(0.2F)
                    .life(0.8F, 0.4F)
                    .size(0.15F, 0.03F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#7FD0FF")
                    .glow(1.8F)
            )
        );
        preset(
            "smoke",
            "Клуб дыма",
            "Густой дым, медленно рассеивается.",
            () -> fx(
                4.0F,
                "minecraft:block.fire.extinguish",
                1.0F,
                0.6F,
                new FxPresets.L("Дым")
                    .shape("sphere", 1.0F)
                    .burst(60)
                    .dir("out", 1.2F, 0.5F)
                    .drag(0.8F)
                    .gravity(-0.5F)
                    .turb(0.8F)
                    .smoke()
                    .life(3.5F, 0.4F)
                    .size(1.4F, 3.6F)
                    .alpha(0.0F, 0.85F, 0.0F)
                    .color("#8A8682", "#4A4846")
            )
        );
        preset(
            "fireball",
            "Огненный шар",
            "Шар огня для снарядов: ядро, пламя и искры остаются следом в воздухе. cast \"fireball\".",
            () -> fx(
                0.0F,
                "minecraft:item.firecharge.use",
                1.0F,
                1.0F,
                new FxPresets.L("Ядро")
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(1000000.0F, 0.0F)
                    .size(0.9F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F)
                    .color("#FFD27A")
                    .glow(2.0F),
                new FxPresets.L("Пламя")
                    .shape("sphere", 0.25F)
                    .rate(90.0F)
                    .dir("random", 0.6F, 0.5F)
                    .world()
                    .turb(1.0F)
                    .tex("smoke")
                    .life(0.45F, 0.3F)
                    .size(0.6F, 0.1F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#FFB040", "#E04010")
                    .glow(1.6F),
                new FxPresets.L("Искры")
                    .shape("sphere", 0.2F)
                    .rate(25.0F)
                    .dir("random", 2.0F, 0.5F)
                    .world()
                    .gravity(3.0F)
                    .tex("spark")
                    .stretch(0.12F)
                    .life(0.6F, 0.4F)
                    .size(0.1F, 0.02F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFE6A0", "#FF7A20")
                    .glow(2.0F)
            )
        );
        preset(
            "magic_orb",
            "Магическая сфера",
            "Сияющая сфера со светящимся следом и кружащими искрами — для снарядов и парящих шаров.",
            () -> fx(
                0.0F,
                "minecraft:block.amethyst_block.chime",
                1.0F,
                1.6F,
                new FxPresets.L("Сфера")
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(1000000.0F, 0.0F)
                    .size(0.75F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F)
                    .color("#9FE6FF")
                    .glow(2.2F),
                new FxPresets.L("Ядро")
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("glow")
                    .life(1000000.0F, 0.0F)
                    .size(0.35F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F)
                    .color("#FFFFFF")
                    .glow(2.5F),
                new FxPresets.L("След")
                    .shape("sphere", 0.15F)
                    .rate(70.0F)
                    .dir("none", 0.0F, 0.0F)
                    .world()
                    .tex("glow")
                    .life(0.5F, 0.2F)
                    .size(0.4F, 0.0F)
                    .alpha(0.9F, 0.0F)
                    .color("#BFF1FF", "#7A5AFF")
                    .glow(1.6F),
                new FxPresets.L("Искры")
                    .shape("ring", 0.45F)
                    .rate(30.0F)
                    .dir("none", 0.0F, 0.0F)
                    .orbit(360.0F)
                    .tex("star")
                    .life(0.5F, 0.3F)
                    .size(0.14F, 0.0F)
                    .spin(0.0F, 300.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFFFFF", "#8FE3FF")
                    .glow(1.8F)
            )
        );
        preset(
            "magic_hit",
            "Вспышка попадания",
            "Короткая вспышка: искры во все стороны и кольцо — на место, куда попал снаряд.",
            () -> fx(
                1.2F,
                "minecraft:block.amethyst_cluster.break",
                1.0F,
                1.3F,
                new FxPresets.L("Вспышка")
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("flare")
                    .life(0.25F, 0.0F)
                    .size(2.2F, 0.0F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF")
                    .glow(2.2F),
                new FxPresets.L("Искры")
                    .burst(40)
                    .dir("random", 6.0F, 0.6F)
                    .drag(2.0F)
                    .gravity(4.0F)
                    .tex("spark")
                    .stretch(0.12F)
                    .life(0.6F, 0.4F)
                    .size(0.16F, 0.03F)
                    .alpha(1.0F, 0.0F)
                    .color("#FFFFFF", "#8FE3FF", "#B070FF")
                    .glow(1.8F),
                new FxPresets.L("Кольцо")
                    .burst(1)
                    .max(1)
                    .dir("none", 0.0F, 0.0F)
                    .tex("ring")
                    .life(0.4F, 0.0F)
                    .size(0.3F, 2.4F)
                    .sizeRandom(0.0F)
                    .alpha(1.0F, 0.0F)
                    .color("#BFF1FF")
                    .glow(1.6F)
            )
        );
        preset(
            "sparkles",
            "Блёстки",
            "Мерцающие звёздочки в воздухе.",
            () -> fx(
                3.0F,
                "minecraft:block.amethyst_block.chime",
                1.0F,
                1.2F,
                new FxPresets.L("Блёстки")
                    .shape("sphere", 1.8F)
                    .at(0.0F, 1.0F, 0.0F)
                    .rate(40.0F)
                    .time(2.4F)
                    .dir("random", 0.3F, 0.5F)
                    .tex("star")
                    .life(1.0F, 0.4F)
                    .size(0.3F, 0.0F)
                    .spin(0.0F, 200.0F)
                    .alpha(0.0F, 1.0F, 0.0F)
                    .color("#FFFFFF", "#FFE88A", "#FFB8F0")
                    .colorRandom()
                    .glow(1.6F)
            )
        );
        more();
    }

    private static final class L {
        final FxDef.Layer l;

        L(String name) {
            this.l = FxDef.layer(name);
        }

        FxPresets.L shape(String s, float radius) {
            this.l.shape = s;
            this.l.radius = radius;
            return this;
        }

        FxPresets.L inner(float r) {
            this.l.radiusInner = r;
            return this;
        }

        FxPresets.L height(float h) {
            this.l.height = h;
            return this;
        }

        FxPresets.L angle(float a) {
            this.l.angle = a;
            return this;
        }

        FxPresets.L at(float x, float y, float z) {
            this.l.offsetX = x;
            this.l.offsetY = y;
            this.l.offsetZ = z;
            return this;
        }

        FxPresets.L even() {
            this.l.even = true;
            return this;
        }

        FxPresets.L burst(int n) {
            this.l.burst = n;
            return this;
        }

        FxPresets.L every(float s) {
            this.l.burstEvery = s;
            return this;
        }

        FxPresets.L rate(float r) {
            this.l.rate = r;
            this.l.burst = 0;
            return this;
        }

        FxPresets.L max(int m) {
            this.l.max = m;
            return this;
        }

        FxPresets.L delay(float d) {
            this.l.delay = d;
            return this;
        }

        FxPresets.L time(float t) {
            this.l.time = t;
            return this;
        }

        FxPresets.L dir(String d, float speed, float random) {
            this.l.direction = d;
            this.l.speed = speed;
            this.l.speedRandom = random;
            return this;
        }

        FxPresets.L gravity(float g) {
            this.l.gravity = g;
            return this;
        }

        FxPresets.L drag(float d) {
            this.l.drag = d;
            return this;
        }

        FxPresets.L orbit(float o) {
            this.l.orbit = o;
            return this;
        }

        FxPresets.L turb(float t) {
            this.l.turbulence = t;
            return this;
        }

        FxPresets.L attract(float a) {
            this.l.attract = a;
            return this;
        }

        FxPresets.L world() {
            this.l.local = false;
            return this;
        }

        FxPresets.L tex(String t) {
            this.l.texture = t;
            return this;
        }

        FxPresets.L smoke() {
            this.l.texture = "smoke";
            this.l.blend = "alpha";
            return this;
        }

        FxPresets.L rot(float x, float y, float z) {
            this.l.rotX = x;
            this.l.rotY = y;
            this.l.rotZ = z;
            return this;
        }

        FxPresets.L blend(String b) {
            this.l.blend = b;
            return this;
        }

        FxPresets.L render(String r) {
            this.l.render = r;
            return this;
        }

        FxPresets.L life(float life, float random) {
            this.l.life = life;
            this.l.lifeRandom = random;
            return this;
        }

        FxPresets.L size(float... k) {
            this.l.size = k;
            return this;
        }

        FxPresets.L sizeRandom(float r) {
            this.l.sizeRandom = r;
            return this;
        }

        FxPresets.L alpha(float... k) {
            this.l.alpha = k;
            return this;
        }

        FxPresets.L color(String... c) {
            this.l.color = c;
            return this;
        }

        FxPresets.L colorRandom() {
            this.l.colorRandom = true;
            return this;
        }

        FxPresets.L glow(float i) {
            this.l.intensity = i;
            return this;
        }

        FxPresets.L spin(float s, float random) {
            this.l.spin = s;
            this.l.spinRandom = random;
            return this;
        }

        FxPresets.L stretch(float s) {
            this.l.render = "stretch";
            this.l.stretch = s;
            return this;
        }

        FxPresets.L beam(float length) {
            this.l.render = "beam";
            this.l.beamLength = length;
            return this;
        }
    }

    public record Preset(String name, String title, String doc, Supplier<FxDef> make) {
    }
}
