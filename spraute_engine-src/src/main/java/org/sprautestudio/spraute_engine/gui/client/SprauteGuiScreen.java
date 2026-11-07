package org.sprautestudio.spraute_engine.gui.client;

import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.engine.client.ProjectSounds;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public final class SprauteGuiScreen extends UI {
    private final GuiView view;
    private final int scriptId;
    private final String guiName;
    private final BiConsumer<String, String> localListener;
    private final Runnable afterClose;
    private boolean closedReported;
    private static SprauteGuiScreen current;

    private SprauteGuiScreen(
        GuiDoc doc, String project, Project local, int scriptId, String guiName, BiConsumer<String, String> localListener, Runnable afterClose
    ) {
        super(Component.literal(doc.get("title")));
        this.scriptId = scriptId;
        this.guiName = guiName;
        this.localListener = localListener;
        this.afterClose = afterClose;
        this.view = new GuiView(doc, project);
        this.view.localProject = local;
        this.view.listener = this::onEvent;
        this.root.add(new Widget() {
            @Override
            public void layout() {
                SprauteGuiScreen.this.view.bounds(0, 0, SprauteGuiScreen.this.vw, SprauteGuiScreen.this.vh);
            }
        });
        this.root.add(this.view);
    }

    public static void openFromServer(Packets.GuiOpen msg) {
        GuiDoc doc = GuiDoc.fromJson(msg.json());
        SprauteGuiScreen s = new SprauteGuiScreen(doc, msg.project(), null, msg.scriptId(), msg.name(), null, null);
        s.show();
    }

    public static void openLocal(GuiDoc doc, Project project, BiConsumer<String, String> events, Runnable afterClose) {
        SprauteGuiScreen s = new SprauteGuiScreen(GuiDoc.fromJson(doc.toJson()), project.name(), project, -1, doc.get("title"), events, afterClose);
        s.show();
    }

    private void show() {
        current = this;
        Minecraft.getInstance().setScreen(this);
        String sound = this.view.doc.get("openSound");
        ProjectSounds.ui(this.view.project, sound);
    }

    public static SprauteGuiScreen current() {
        return current != null && Minecraft.getInstance().screen == current ? current : null;
    }

    public static void applyUpdate(Packets.GuiUpdate msg) {
        SprauteGuiScreen s = current();
        if (s != null && s.scriptId == msg.scriptId()) {
            s.view.update(msg.element(), msg.prop(), msg.value());
        }
    }

    public static void updateLocal(String project, String element, String prop, String value) {
        SprauteGuiScreen s = current();
        if (s != null && s.localListener != null && Objects.equals(s.view.project, project)) {
            s.view.update(element, prop, value);
        }
    }

    public String input(String element) {
        return this.view.inputs().get(element);
    }

    public static void closeFromServer(Packets.GuiClose msg) {
        SprauteGuiScreen s = current();
        if (s != null && (msg.scriptId() < 0 || s.scriptId == msg.scriptId())) {
            s.closedReported = true;
            s.onClose();
        }
    }

    private void onEvent(String element, String kind) {
        if (kind.equals("close")) {
            this.onClose();
        } else {
            if (this.localListener != null) {
                this.localListener.accept(element, kind);
            } else {
                SprauteNetwork.sendToServer(new Packets.GuiEvent(this.scriptId, this.guiName, element, kind, this.view.inputs()));
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return GuiDoc.toBool(this.view.doc.get("pause"));
    }

    @Override
    protected boolean onShortcut(int key, int mods) {
        if (key == 256 && GuiDoc.toBool(this.view.doc.get("closeOnEsc"))) {
            this.onClose();
            return true;
        } else {
            return this.view.keyPressed(key, 0, mods);
        }
    }

    @Override
    public void onClose() {
        if (!this.closedReported) {
            this.closedReported = true;
            if (this.localListener != null) {
                this.localListener.accept("", "close");
            } else {
                SprauteNetwork.sendToServer(new Packets.GuiEvent(this.scriptId, this.guiName, "", "close", this.view.inputs()));
            }
        }

        super.onClose();
        if (current == this) {
            current = null;
        }

        if (this.afterClose != null) {
            this.afterClose.run();
        }
    }
}
