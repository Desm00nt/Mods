package org.sprautestudio.spraute_engine.core.script;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public final class ScriptCommand {
    private static final String ID = "id";

    private ScriptCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(
            Commands.literal("script")
                .requires(s -> s.hasPermission(2))
                .then(
                    Commands.literal("start")
                        .then(Commands.literal("all").then(Commands.argument("ids", StringArgumentType.string()).executes(c -> {
                            int n = 0;

                            for (String s : StringArgumentType.getString(c, "ids").trim().split("\\s+")) {
                                n += start(c, parse(s), null);
                            }

                            return n;
                        })))
                        .then(Commands.literal("category").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                            int n = 0;

                            for (int id : env().category(StringArgumentType.getString(c, "name"))) {
                                n += start(c, id, null);
                            }

                            return n;
                        })))
                        .then(
                            Commands.argument("id", IntegerArgumentType.integer())
                                .executes(c -> start(c, id(c), null))
                                .then(
                                    Commands.argument("label", StringArgumentType.word())
                                        .executes(c -> start(c, id(c), StringArgumentType.getString(c, "label")))
                                )
                        )
                )
                .then(
                    Commands.literal("stop")
                        .then(Commands.literal("all").executes(ScriptCommand::stopAll))
                        .then(Commands.literal("scripts").executes(ScriptCommand::stopAll))
                        .then(Commands.literal("backscripts").executes(c -> 0))
                        .then(Commands.literal("category").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                            for (int id : env().category(StringArgumentType.getString(c, "name"))) {
                                env().stop(id);
                            }

                            return 1;
                        })))
                        .then(Commands.argument("id", IntegerArgumentType.integer()).executes(c -> {
                            env().stop(id(c));
                            return say(c, "Сюжет #" + id(c) + " остановлен");
                        }))
                )
                .then(on("pause", (c, s) -> {
                    s.setPaused(true);
                    return "Сюжет #" + s.getId() + " на паузе";
                }))
                .then(on("resume", (c, s) -> {
                    s.setPaused(false);
                    return "Сюжет #" + s.getId() + " продолжается";
                }))
                .then(
                    on(
                        "phase",
                        (c, s) -> {
                            ScriptPerformer m = s.getMainPerformer();
                            int pct = m.getActionsAmount() == 0 ? 100 : m.getActionIndex() * 100 / m.getActionsAmount();
                            return "Метка: "
                                + (m.getStage().isEmpty() ? "(начало)" : m.getStage())
                                + ", шаг "
                                + m.getActionIndex()
                                + " из "
                                + m.getActionsAmount()
                                + " ("
                                + pct
                                + "%). "
                                + m.describe();
                        }
                    )
                )
                .then(on("actions", (c, s) -> {
                    ScriptPerformer m = s.getMainPerformer();
                    return "Шагов: " + m.getActionsAmount() + ", сейчас " + m.getActionIndex() + ", параллельных: " + s.running.size();
                }))
                .then(on("data", (c, s) -> "Флаги и переменные: " + s.serializeNBT()))
                .then(Commands.literal("setup").then(Commands.argument("id", IntegerArgumentType.integer()).executes(c -> {
                    env().stop(id(c));
                    return start(c, id(c), null);
                })))
                .then(
                    Commands.literal("goto")
                        .then(Commands.argument("id", IntegerArgumentType.integer()).then(Commands.argument("label", StringArgumentType.word()).executes(c -> {
                            ScriptInstance s = env().anyInstance(id(c));
                            String label = StringArgumentType.getString(c, "label");
                            if (s == null) {
                                return fail(c, "Сюжет #" + id(c) + " не запущен");
                            } else {
                                return s.goTo(label) ? say(c, "Сюжет #" + id(c) + ": переход к " + label) : fail(c, "Метка " + label + " не найдена");
                            }
                        })))
                )
                .then(
                    Commands.literal("jump")
                        .then(
                            Commands.argument("id", IntegerArgumentType.integer())
                                .then(Commands.argument("step", IntegerArgumentType.integer(0)).executes(c -> {
                                    ScriptInstance s = env().anyInstance(id(c));
                                    if (s == null) {
                                        return fail(c, "Сюжет #" + id(c) + " не запущен");
                                    } else {
                                        s.getMainPerformer().jumpToIndex(IntegerArgumentType.getInteger(c, "step"));
                                        return 1;
                                    }
                                }))
                        )
                )
                .then(
                    Commands.literal("trigger")
                        .then(Commands.argument("id", IntegerArgumentType.integer()).then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                            ScriptInstance s = env().anyInstance(id(c));
                            if (s == null) {
                                return fail(c, "Сюжет #" + id(c) + " не запущен");
                            } else {
                                Player p = c.getSource().getEntity() instanceof Player pl ? pl : null;
                                s.trigger(StringArgumentType.getString(c, "name"), p);
                                return 1;
                            }
                        })))
                )
                .then(
                    Commands.literal("environment")
                        .executes(c -> say(c, "Загружено сюжетов: " + env().constructors.size() + ", запущено: " + env().scriptInstances.size()))
                        .then(Commands.literal("active").executes(c -> say(c, "Запущены: " + env().scriptInstances.keySet())))
                )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> on(String name, ScriptCommand.Query q) {
        return Commands.literal(name).then(Commands.argument("id", IntegerArgumentType.integer()).executes(c -> {
            ScriptInstance s = env().anyInstance(id(c));
            return s == null ? fail(c, "Сюжет #" + id(c) + " не запущен") : say(c, q.run(c, s));
        }));
    }

    private static ScriptEnvironment env() {
        return SprauteRuntime.env().orElseThrow();
    }

    private static int id(CommandContext<CommandSourceStack> c) {
        return IntegerArgumentType.getInteger(c, "id");
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException var2) {
            return -1;
        }
    }

    private static int start(CommandContext<CommandSourceStack> c, int id, String label) {
        ScriptInstance s = env().startFor(id, c.getSource().getEntity() instanceof ServerPlayer p ? p : null);
        if (s == null) {
            SprauteRuntime.Deployed d = SprauteRuntime.get().deployed(id);
            return d != null && d.program.header != null && d.program.header.solo
                ? fail(c, "Сюжет #" + id + " у каждого игрока свой: его запускает игрок, а не консоль")
                : fail(c, "Сюжет #" + id + " не найден");
        } else {
            return label != null && !s.goTo(label) ? fail(c, "Метка " + label + " не найдена") : say(c, "Сюжет #" + id + " запущен");
        }
    }

    private static int stopAll(CommandContext<CommandSourceStack> c) {
        env().stopAll();
        return say(c, "Все сюжеты остановлены");
    }

    private static int say(CommandContext<CommandSourceStack> c, String text) {
        Mc.success(c.getSource(), Component.literal(text), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private interface Query {
        String run(CommandContext<CommandSourceStack> var1, ScriptInstance var2);
    }
}
