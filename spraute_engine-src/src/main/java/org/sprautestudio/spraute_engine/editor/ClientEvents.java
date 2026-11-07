package org.sprautestudio.spraute_engine.editor;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import org.sprautestudio.spraute_engine.editor.assets.ProjectImages;
import org.sprautestudio.spraute_engine.editor.cutscene.CutsceneRecorder;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.export.BundledProjects;
import org.sprautestudio.spraute_engine.mcp.McpServer;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.ClientPlatform;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;

public final class ClientEvents {
    public static final KeyMapping OPEN_EDITOR = ClientPlatform.get().gameKey("key.spraute_engine.open", 78, "key.categories.spraute_engine");
    private static boolean openNextTick;
    private static boolean hintShown;

    private ClientEvents() {
    }

    public static boolean editorMode() {
        return Workspace.settings().editorMode;
    }

    public static void setEditorMode(boolean on) {
        Workspace.settings().editorMode = on;
        Workspace.saveSettings();
    }

    public static boolean isEditorKey(int key) {
        Key k = ClientPlatform.get().boundKey(OPEN_EDITOR);
        return k.getType() == Type.KEYSYM && k.getValue() == key;
    }

    private static Component keyName() {
        return OPEN_EDITOR.getTranslatedKeyMessage();
    }

    public static final class ForgeBus {
        public static void registerHooks() {
            ClientHooks.CLIENT_TICK.add(ClientEvents.ForgeBus::clientTick);
            ClientHooks.LOGGING_IN.add(ClientEvents.ForgeBus::loggedIn);
            ClientHooks.CLIENT_COMMANDS.add(ClientEvents.ForgeBus::clientCommands);
        }

        public static void clientTick(ClientHooks.ClientTick event) {
            if (event.phase == TickPhase.END) {
                if (!BundledProjects.editorHidden()) {
                    Minecraft mc = Minecraft.getInstance();
                    if (ClientEvents.openNextTick) {
                        ClientEvents.openNextTick = false;
                        SprauteScreen.open();
                    } else {
                        while (ClientEvents.OPEN_EDITOR.consumeClick()) {
                            if (mc.screen == null) {
                                if (ClientEvents.editorMode()) {
                                    SprauteScreen.open();
                                } else if (!ClientEvents.hintShown && mc.player != null) {
                                    ClientEvents.hintShown = true;
                                    mc.player
                                        .displayClientMessage(
                                            Component.literal("Режим редактора Spraute выключен. Включите: /editor on").withStyle(ChatFormatting.GRAY), true
                                        );
                                }
                            }
                        }
                    }
                }
            }
        }

        public static void loggedIn(ClientHooks.LoggingIn event) {
            ProjectImages.registerAll();
            ModelLibrary.scan();
        }

        public static void clientCommands(ClientHooks.ClientCommands event) {
            if (!BundledProjects.editorHidden()) {
                event.getDispatcher()
                    .register(
                        Commands.literal("editor")
                            .executes(ctx -> {
                                ClientEvents.openNextTick = true;
                                return 1;
                            })
                            .then(Commands.literal("status").executes(ctx -> {
                                status();
                                return 1;
                            }))
                            .then(
                                Commands.literal("mcp")
                                    .executes(ctx -> {
                                        mcpStatus();
                                        return 1;
                                    })
                                    .then(
                                        Commands.argument("state", StringArgumentType.word())
                                            .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("on", "off"), b))
                                            .executes(ctx -> {
                                                String st = StringArgumentType.getString(ctx, "state").toLowerCase();
                                                boolean on = st.equals("on") || st.equals("вкл");
                                                if (!on && !st.equals("off") && !st.equals("выкл")) {
                                                    say(Component.literal("Используйте /editor mcp on или /editor mcp off").withStyle(ChatFormatting.RED));
                                                    return 0;
                                                } else {
                                                    Workspace.settings().mcp = on;
                                                    Workspace.saveSettings();
                                                    if (on) {
                                                        McpServer.start(Workspace.settings().mcpPort);
                                                    } else {
                                                        McpServer.stop();
                                                    }

                                                    mcpStatus();
                                                    return 1;
                                                }
                                            })
                                    )
                            )
                            .then(
                                Commands.argument("mode", StringArgumentType.word())
                                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("on", "off", "status", "mcp"), b))
                                    .executes(
                                        ctx -> {
                                            String mode = StringArgumentType.getString(ctx, "mode").toLowerCase();
                                            switch (mode) {
                                                case "on":
                                                case "вкл":
                                                    ClientEvents.setEditorMode(true);
                                                    say(
                                                        Component.literal("Режим редактора включён. ")
                                                            .withStyle(ChatFormatting.GREEN)
                                                            .append(Component.literal("Клавиша ").withStyle(ChatFormatting.GRAY))
                                                            .append(ClientEvents.keyName().copy().withStyle(ChatFormatting.YELLOW))
                                                            .append(
                                                                Component.literal(" открывает и закрывает редактор (сменить — в Управлении).")
                                                                    .withStyle(ChatFormatting.GRAY)
                                                            )
                                                    );
                                                    break;
                                                case "off":
                                                case "выкл":
                                                    ClientEvents.setEditorMode(false);
                                                    say(
                                                        Component.literal("Режим редактора выключен. /editor по-прежнему открывает редактор.")
                                                            .withStyle(ChatFormatting.GRAY)
                                                    );
                                                    break;
                                                default:
                                                    say(
                                                        Component.literal("Используйте /editor, /editor on | off, /editor status или /editor mcp")
                                                            .withStyle(ChatFormatting.RED)
                                                    );
                                                    return 0;
                                            }

                                            return 1;
                                        }
                                    )
                            )
                    );
            }
        }

        private static void mcpStatus() {
            boolean run = McpServer.running();
            int port = Workspace.settings().mcpPort;
            String url = "http://127.0.0.1:" + port + "/mcp";
            Component line = Component.literal("MCP для ИИ-ассистента: ")
                .withStyle(ChatFormatting.GRAY)
                .append(run ? Component.literal("работает").withStyle(ChatFormatting.GREEN) : Component.literal("выключен").withStyle(ChatFormatting.RED))
                .append(Component.literal(run ? " на " + url : " — /editor mcp on").withStyle(ChatFormatting.GRAY));
            say(line);
            if (run) {
                mcpCopy("Claude Code", "claude mcp add --transport http spraute " + url);
                mcpCopy("Codex", "codex mcp add spraute --url " + url);
                mcpCopy("Gemini CLI", "gemini mcp add --transport http spraute " + url);
                mcpCopy("Cursor (.cursor/mcp.json)", "{\"mcpServers\":{\"spraute\":{\"url\":\"" + url + "\"}}}");
                mcpCopy("Другой MCP-клиент (HTTP)", url);
                String last = McpServer.lastClient();
                long ago = System.currentTimeMillis() - McpServer.lastCall();
                if (!last.isEmpty()) {
                    say(Component.literal("Подключён: " + last + ", последний запрос " + ago / 1000L + " с назад").withStyle(ChatFormatting.GRAY));
                }
            }
        }

        private static void mcpCopy(String client, String value) {
            say(
                Component.literal(client + ": ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(
                        Component.literal(value)
                            .withStyle(
                                s -> s.withColor(ChatFormatting.YELLOW)
                                    .withClickEvent(new ClickEvent(Action.COPY_TO_CLIPBOARD, value))
                                    .withHoverEvent(
                                        new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal("Нажмите, чтобы скопировать"))
                                    )
                            )
                    )
            );
        }

        private static void status() {
            say(
                Component.literal("Режим редактора: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(
                        ClientEvents.editorMode()
                            ? Component.literal("включён").withStyle(ChatFormatting.GREEN)
                            : Component.literal("выключен").withStyle(ChatFormatting.RED)
                    )
                    .append(Component.literal(" — /editor on | off, /editor mcp, клавиша ").withStyle(ChatFormatting.GRAY))
                    .append(ClientEvents.keyName().copy().withStyle(ChatFormatting.YELLOW))
            );
        }

        private static void say(Component c) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(c, false);
            }
        }
    }

    public static final class ModBus {
        public static void registerHooks() {
            ClientHooks.REGISTER_KEYS.add(ClientEvents.ModBus::registerKeys);
        }

        public static void registerKeys(ClientHooks.RegisterKeys event) {
            if (!BundledProjects.editorHidden()) {
                event.register(ClientEvents.OPEN_EDITOR);
                event.register(CutsceneRecorder.FRAME);
            }
        }
    }
}
