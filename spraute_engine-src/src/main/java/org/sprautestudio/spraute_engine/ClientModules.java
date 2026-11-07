package org.sprautestudio.spraute_engine;

import org.sprautestudio.spraute_engine.build.client.BlockQuakeFx;
import org.sprautestudio.spraute_engine.build.client.BuildFx;
import org.sprautestudio.spraute_engine.camera.client.CamOcclusion;
import org.sprautestudio.spraute_engine.camera.client.PlayerCam;
import org.sprautestudio.spraute_engine.content.ContentClient;
import org.sprautestudio.spraute_engine.core.client.PlayerRulesClient;
import org.sprautestudio.spraute_engine.core.client.ScreenFx;
import org.sprautestudio.spraute_engine.core.client.SlotsClient;
import org.sprautestudio.spraute_engine.core.client.SpeechClient;
import org.sprautestudio.spraute_engine.custom.CustomBlocksClient;
import org.sprautestudio.spraute_engine.cutscene.client.CameraDirector;
import org.sprautestudio.spraute_engine.editor.ClientEvents;
import org.sprautestudio.spraute_engine.editor.DebugPlate;
import org.sprautestudio.spraute_engine.editor.cutscene.CutsceneRecorder;
import org.sprautestudio.spraute_engine.editor.path.PathWorldEditor;
import org.sprautestudio.spraute_engine.editor.zone.ZoneWorldEditor;
import org.sprautestudio.spraute_engine.engine.client.ChatHeads;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.Notices;
import org.sprautestudio.spraute_engine.engine.client.OrbRenderer;
import org.sprautestudio.spraute_engine.fx.client.FxEngine;
import org.sprautestudio.spraute_engine.general.client.GeneralRuntime;
import org.sprautestudio.spraute_engine.gizmo.GizmoClient;
import org.sprautestudio.spraute_engine.gui.client.HudOverlays;
import org.sprautestudio.spraute_engine.gui.client.WorldPanelsClient;
import org.sprautestudio.spraute_engine.nav.NavClient;
import org.sprautestudio.spraute_engine.post.client.PostEngine;
import org.sprautestudio.spraute_engine.quest.client.QuestClient;
import org.sprautestudio.spraute_engine.runtime.KeyReporter;
import org.sprautestudio.spraute_engine.runtime.models.PartPoseSync;
import org.sprautestudio.spraute_engine.zone.client.ZoneClient;

final class ClientModules {
    private ClientModules() {
    }

    static void client() {
        ContentClient.registerHooks();
        CustomBlocksClient.registerHooks();
        ScreenFx.registerHooks();
        SlotsClient.registerHooks();
        PlayerRulesClient.registerHooks();
        WorldPanelsClient.registerHooks();
        SpeechClient.Keys.registerHooks();
        CameraDirector.registerHooks();
        SpeechClient.registerHooks();
        ClientEvents.ForgeBus.registerHooks();
        ClientEvents.ModBus.registerHooks();
        DebugPlate.registerHooks();
        NavClient.registerHooks();
        GizmoClient.registerHooks();
        CutsceneRecorder.registerHooks();
        PathWorldEditor.registerHooks();
        ZoneWorldEditor.registerHooks();
        PostEngine.registerHooks();
        PlayerCam.registerHooks();
        BuildFx.registerHooks();
        BlockQuakeFx.registerHooks();
        CamOcclusion.registerHooks();
        ChatHeads.Setup.registerHooks();
        EngineClient.Overlays.registerHooks();
        EngineClient.registerHooks();
        Notices.registerHooks();
        OrbRenderer.Registration.registerHooks();
        FxEngine.registerHooks();
        HudOverlays.registerHooks();
        QuestClient.ModBus.registerHooks();
        QuestClient.registerHooks();
        KeyReporter.registerHooks();
        PartPoseSync.registerHooks();
        ZoneClient.registerHooks();
        GeneralRuntime.registerHooks();
    }
}
