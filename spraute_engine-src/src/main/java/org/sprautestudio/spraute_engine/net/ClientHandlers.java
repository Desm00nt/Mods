package org.sprautestudio.spraute_engine.net;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.core.client.PlayerSkins;
import org.sprautestudio.spraute_engine.core.client.ScreenFx;
import org.sprautestudio.spraute_engine.core.client.SpeechClient;
import org.sprautestudio.spraute_engine.core.system.Coins;
import org.sprautestudio.spraute_engine.cutscene.CutsceneDoc;
import org.sprautestudio.spraute_engine.cutscene.client.CameraDirector;
import org.sprautestudio.spraute_engine.editor.ClientState;
import org.sprautestudio.spraute_engine.engine.client.ChatHeads;
import org.sprautestudio.spraute_engine.engine.client.ChoiceScreen;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.Notices;
import org.sprautestudio.spraute_engine.engine.client.ProjectSounds;
import org.sprautestudio.spraute_engine.fx.FxDef;
import org.sprautestudio.spraute_engine.fx.client.FxEngine;
import org.sprautestudio.spraute_engine.gui.client.HudOverlays;
import org.sprautestudio.spraute_engine.gui.client.SprauteGuiScreen;
import org.sprautestudio.spraute_engine.gui.client.WorldPanelsClient;
import org.sprautestudio.spraute_engine.hitbox.Hitboxes;
import org.sprautestudio.spraute_engine.playeranim.PlayerAnimsClient;
import org.sprautestudio.spraute_engine.post.PostProfile;
import org.sprautestudio.spraute_engine.post.client.PostEngine;
import org.sprautestudio.spraute_engine.quest.client.QuestClient;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;
import org.sprautestudio.spraute_engine.runtime.PlayerSlots;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;
import org.sprautestudio.spraute_engine.shop.client.ShopScreen;
import org.sprautestudio.spraute_engine.skills.client.SkillsClient;
import org.sprautestudio.spraute_engine.stealth.client.StealthClient;
import org.sprautestudio.spraute_engine.zone.client.ZoneClient;

public final class ClientHandlers {
    private ClientHandlers() {
    }

    public static void playerAnim(Packets.PlayerAnim msg) {
        PlayerAnimsClient.play(msg.player(), msg.anim(), msg.speed(), msg.fade(), msg.times(), msg.mode());
    }

    public static void status(Packets.Status msg) {
        ClientState.get().onStatus(msg);
    }

    public static void log(Packets.Log msg) {
        ClientState.get().onLog(msg);
    }

    public static void deployResult(Packets.DeployResult msg) {
        ClientState.get().onDeployResult(msg);
    }

    public static void guiOpen(Packets.GuiOpen msg) {
        SprauteGuiScreen.openFromServer(msg);
    }

    public static void guiUpdate(Packets.GuiUpdate msg) {
        SprauteGuiScreen.applyUpdate(msg);
        HudOverlays.update(msg);
        WorldPanelsClient.update(msg);
    }

    public static void choiceOpen(Packets.ChoiceOpen msg) {
        ChoiceScreen.open(msg);
    }

    public static void speechHead(Packets.SpeechHead msg) {
        ChatHeads.bind(msg.scriptId(), msg.character(), msg.entityId(), msg.model());
    }

    public static void cameraPlay(Packets.CameraPlay msg) {
        CameraDirector.play(msg.scriptId(), msg.name(), CutsceneDoc.fromJson(msg.json()), new Vec3(msg.ox(), msg.oy(), msg.oz()), msg.looks(), null);
    }

    public static void hitboxSync(Packets.HitboxSync msg) {
        if (msg.flags() == 0) {
            Hitboxes.CLIENT.remove(msg.entityId());
        } else {
            Hitboxes.CLIENT.put(msg.entityId(), msg.flags());
        }
    }

    public static void notify(Packets.Notify msg) {
        Notices.show(msg.text(), msg.icon(), msg.item(), msg.project(), msg.sound());
    }

    public static void soundPlay(Packets.SoundPlay m) {
        ProjectSounds.play(m.project(), m.name(), m.x(), m.y(), m.z(), m.positional(), m.volume(), m.pitch(), m.loop(), m.id(), m.fade());
    }

    public static void soundStop(Packets.SoundStop m) {
        ProjectSounds.stop(m.id(), m.fade());
    }

    public static void zoneTitle(Packets.ZoneTitle msg) {
        ZoneClient.showTitle(msg.title(), msg.subtitle());
    }

    public static void questSync(Packets.QuestSync msg) {
        QuestClient.receive(msg.json(), msg.open());
    }

    public static void shopOpen(Packets.ShopOpen msg) {
        ShopScreen.show(msg.json(), msg.refresh());
    }

    public static void fxPlay(Packets.FxPlay msg) {
        FxEngine.play(msg.id(), msg.project(), FxDef.fromJson(msg.json()), msg.x(), msg.y(), msg.z(), msg.entity(), msg.scale(), msg.scriptId(), msg.bone());
    }

    public static void boneWatch(Packets.BoneWatch msg) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(msg.entity()) instanceof SprauteModelEntity me) {
            me.boneWatchUntil = System.currentTimeMillis() + 3000L;
        }
    }

    public static void fxStop(Packets.FxStop msg) {
        FxEngine.stop(msg.id(), msg.scriptId(), false);
    }

    public static void cameraOp(Packets.CameraOp msg) {
        CameraDirector.op(msg.json());
    }

    public static void overlayShow(Packets.OverlayShow msg) {
        HudOverlays.show(msg);
    }

    public static void overlayHide(Packets.OverlayHide msg) {
        HudOverlays.hide(msg);
    }

    public static void guiClose(Packets.GuiClose msg) {
        SprauteGuiScreen.closeFromServer(msg);
    }

    public static void skillsView(Packets.SkillsView msg) {
        SkillsClient.receive(msg.json(), msg.mode());
    }

    public static void stealthSync(Packets.StealthSync msg) {
        StealthClient.receive(msg.json());
    }

    public static void engineSync(Packets.EngineSync msg) {
        EngineClient.receive(msg.json());
    }

    public static void speech(Packets.Speech msg) {
        SpeechClient.receive(msg);
    }

    public static void speechClear(Packets.SpeechClear msg) {
        SpeechClient.clear(msg.scriptId());
    }

    public static void speechNewest(Packets.SpeechNewest msg) {
        SpeechClient.newest(msg.scriptId());
    }

    private static boolean isOwnPlayer(Player p) {
        return p == Minecraft.getInstance().player;
    }

    public static void playerSlots(Packets.PlayerSlotsSync msg) {
        PlayerSlots.receive(ClientHandlers::isOwnPlayer, msg);
    }

    public static void playerRules(Packets.PlayerRulesSync msg) {
        PlayerRules.receive(ClientHandlers::isOwnPlayer, msg.names(), msg.values());
    }

    public static void postFx(Packets.PostFx msg) {
        double in = msg.in() / 20.0;
        String var3 = msg.op();
        switch (var3) {
            case "show":
                PostEngine.show(
                    msg.layer(), PostProfile.fromJson(msg.data()).resolved(), in, msg.hold() < 0 ? -1.0 : msg.hold() / 20.0, msg.out() / 20.0, msg.power()
                );
                break;
            case "set":
                int eq = msg.data().indexOf(61);
                String name = msg.data().substring(0, eq);
                float[] v = PostProfile.parse(name, msg.data().substring(eq + 1));
                if (v != null) {
                    PostEngine.set(msg.layer(), name, v, in);
                }
                break;
            case "remove":
                PostEngine.remove(msg.layer(), in);
        }
    }

    public static void screenFx(Packets.ScreenFx msg) {
        ScreenFx.play(msg.kind(), msg.in(), msg.hold(), msg.out(), msg.argb());
    }

    public static void coins(Packets.CoinsSync msg) {
        Coins.receive(msg.value(), msg.need());
    }

    public static void playerSkin(Packets.PlayerSkin msg) {
        PlayerSkins.set(msg.player(), msg.texture());
    }
}
