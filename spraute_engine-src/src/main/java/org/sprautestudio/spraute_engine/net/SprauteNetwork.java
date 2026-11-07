package org.sprautestudio.spraute_engine.net;

import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.sprautestudio.spraute_engine.build.client.BlockQuakeFx;
import org.sprautestudio.spraute_engine.build.client.BuildFx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.content.ContentClient;
import org.sprautestudio.spraute_engine.content.ProjectContent;
import org.sprautestudio.spraute_engine.core.system.CameraState;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.EngineServer;
import org.sprautestudio.spraute_engine.fx.FxDef;
import org.sprautestudio.spraute_engine.fx.client.FxEngine;
import org.sprautestudio.spraute_engine.gizmo.GizmoClient;
import org.sprautestudio.spraute_engine.gui.client.WorldPanelsClient;
import org.sprautestudio.spraute_engine.nav.NavClient;
import org.sprautestudio.spraute_engine.platform.PacketChannel;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.quest.Quests;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.Mechanics;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;
import org.sprautestudio.spraute_engine.runtime.SprauteScriptInstance;
import org.sprautestudio.spraute_engine.runtime.models.PartShape;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;
import org.sprautestudio.spraute_engine.shop.Shops;
import org.sprautestudio.spraute_engine.skills.Skills;
import org.sprautestudio.spraute_engine.zone.Zones;
import org.sprautestudio.spraute_engine.zone.client.ZoneClient;

public final class SprauteNetwork {
    private static final String PROTOCOL = "2";
    private static final PacketChannel CHANNEL = Platform.get().channel(new ResourceLocation("spraute_engine", "main"), "2");
    private static final int PART = 30000;
    private static final int MAX_BIG = 33554432;
    private static int nextTransfer;
    private static final Map<String, ByteArrayOutputStream> BIG = new HashMap<>();

    private SprauteNetwork() {
    }

    public static void register() {
        CHANNEL.register(Packets.Deploy.class, Packets.Deploy::write, Packets.Deploy::read, SprauteNetwork::handleDeploy, PacketChannel.Direction.TO_SERVER);
        CHANNEL.register(
            Packets.SourcesSet.class, Packets.SourcesSet::write, Packets.SourcesSet::read, SprauteNetwork::handleSourcesSet, PacketChannel.Direction.TO_SERVER
        );
        CHANNEL.register(
            Packets.Undeploy.class, Packets.Undeploy::write, Packets.Undeploy::read, SprauteNetwork::handleUndeploy, PacketChannel.Direction.TO_SERVER
        );
        CHANNEL.register(
            Packets.RunCommand.class, Packets.RunCommand::write, Packets.RunCommand::read, SprauteNetwork::handleCommand, PacketChannel.Direction.TO_SERVER
        );
        CHANNEL.register(
            Packets.RequestStatus.class,
            Packets.RequestStatus::write,
            Packets.RequestStatus::read,
            SprauteNetwork::handleStatusRequest,
            PacketChannel.Direction.TO_SERVER
        );
        CHANNEL.register(
            Packets.Status.class,
            Packets.Status::write,
            Packets.Status::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.status(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.Log.class,
            Packets.Log::write,
            Packets.Log::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.log(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.DeployResult.class,
            Packets.DeployResult::write,
            Packets.DeployResult::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.deployResult(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.GuiOpen.class,
            Packets.GuiOpen::write,
            Packets.GuiOpen::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.guiOpen(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.GuiUpdate.class,
            Packets.GuiUpdate::write,
            Packets.GuiUpdate::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.guiUpdate(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.GuiClose.class,
            Packets.GuiClose::write,
            Packets.GuiClose::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.guiClose(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.GuiEvent.class, Packets.GuiEvent::write, Packets.GuiEvent::read, SprauteNetwork::handleGuiEvent, PacketChannel.Direction.TO_SERVER
        );
        CHANNEL.register(
            Packets.OverlayShow.class,
            Packets.OverlayShow::write,
            Packets.OverlayShow::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.overlayShow(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.OverlayHide.class,
            Packets.OverlayHide::write,
            Packets.OverlayHide::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.overlayHide(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.SpeechHead.class,
            Packets.SpeechHead::write,
            Packets.SpeechHead::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.speechHead(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.CameraPlay.class,
            Packets.CameraPlay::write,
            Packets.CameraPlay::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.cameraPlay(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.CameraOp.class,
            Packets.CameraOp::write,
            Packets.CameraOp::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.cameraOp(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.CameraEvent.class,
            Packets.CameraEvent::write,
            Packets.CameraEvent::read,
            SprauteNetwork::handleCameraEvent,
            PacketChannel.Direction.TO_SERVER
        );
        CHANNEL.register(
            Packets.ChoiceOpen.class,
            Packets.ChoiceOpen::write,
            Packets.ChoiceOpen::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.choiceOpen(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.ChoiceAnswer.class,
            Packets.ChoiceAnswer::write,
            Packets.ChoiceAnswer::read,
            SprauteNetwork::handleChoiceAnswer,
            PacketChannel.Direction.TO_SERVER
        );
        CHANNEL.register(Packets.KeyPress.class, Packets.KeyPress::write, Packets.KeyPress::read, (msg, ctx) -> {
            ServerPlayer p = ctx.sender();
            ctx.enqueue(() -> {
                if (p != null) {
                    Mechanics.keyPressed(p, msg.key());
                }
            });
        }, PacketChannel.Direction.TO_SERVER);
        CHANNEL.register(
            Packets.FxLaunch.class,
            Packets.FxLaunch::write,
            Packets.FxLaunch::read,
            (msg, ctx) -> client(
                ctx,
                () -> () -> FxEngine.launch(
                    msg.id(),
                    msg.project(),
                    FxDef.fromJson(msg.json()),
                    msg.x(),
                    msg.y(),
                    msg.z(),
                    msg.vx(),
                    msg.vy(),
                    msg.vz(),
                    msg.g(),
                    msg.scale(),
                    msg.scriptId()
                )
            ),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.FxPlay.class,
            Packets.FxPlay::write,
            Packets.FxPlay::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.fxPlay(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.FxStop.class,
            Packets.FxStop::write,
            Packets.FxStop::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.fxStop(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.ShopOpen.class,
            Packets.ShopOpen::write,
            Packets.ShopOpen::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.shopOpen(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.ShopDeal.class, Packets.ShopDeal::write, Packets.ShopDeal::read, SprauteNetwork::handleShopDeal, PacketChannel.Direction.TO_SERVER
        );
        CHANNEL.register(
            Packets.HitboxSync.class,
            Packets.HitboxSync::write,
            Packets.HitboxSync::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.hitboxSync(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.ZoneShow.class,
            Packets.ZoneShow::write,
            Packets.ZoneShow::read,
            (msg, ctx) -> client(ctx, () -> () -> ZoneClient.receiveShown(msg.json())),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.ZoneTitle.class,
            Packets.ZoneTitle::write,
            Packets.ZoneTitle::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.zoneTitle(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.QuestSync.class,
            Packets.QuestSync::write,
            Packets.QuestSync::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.questSync(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.QuestTrack.class, Packets.QuestTrack::write, Packets.QuestTrack::read, SprauteNetwork::handleQuestTrack, PacketChannel.Direction.TO_SERVER
        );
        CHANNEL.register(
            Packets.EngineSync.class,
            Packets.EngineSync::write,
            Packets.EngineSync::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.engineSync(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.EngineSet.class, Packets.EngineSet::write, Packets.EngineSet::read, SprauteNetwork::handleEngineSet, PacketChannel.Direction.TO_SERVER
        );
        CHANNEL.register(Packets.PartPoses.class, Packets.PartPoses::write, Packets.PartPoses::read, (msg, ctx) -> {
            ServerPlayer p = ctx.sender();
            ctx.enqueue(() -> {
                if (p != null) {
                    Entity en = Mc.level(p).getEntity(msg.entity());
                    if (en instanceof SprauteModelEntity me && !(en.distanceToSqr(p) > 9216.0)) {
                        long now = System.currentTimeMillis();
                        float[] d = msg.data();

                        for (int i = 0; i < msg.bones().size(); i++) {
                            Vector3f pos = new Vector3f(d[i * 7], d[i * 7 + 1], d[i * 7 + 2]);
                            if (!(pos.lengthSquared() > 4096.0F) && Float.isFinite(pos.lengthSquared())) {
                                Quaternionf q = new Quaternionf(d[i * 7 + 3], d[i * 7 + 4], d[i * 7 + 5], d[i * 7 + 6]).normalize();
                                me.bonePoses.put(msg.bones().get(i), new PartShape.BonePose(pos, q, now));
                            }
                        }
                    }
                }
            });
        }, PacketChannel.Direction.TO_SERVER);
        CHANNEL.register(
            Packets.Notify.class,
            Packets.Notify::write,
            Packets.Notify::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.notify(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.SoundPlay.class,
            Packets.SoundPlay::write,
            Packets.SoundPlay::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.soundPlay(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.SoundStop.class,
            Packets.SoundStop::write,
            Packets.SoundStop::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.soundStop(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.Speech.class,
            Packets.Speech::write,
            Packets.Speech::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.speech(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.SpeechClear.class,
            Packets.SpeechClear::write,
            Packets.SpeechClear::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.speechClear(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(Packets.CameraPos.class, Packets.CameraPos::write, Packets.CameraPos::read, (msg, ctx) -> {
            ServerPlayer p = ctx.sender();
            ctx.enqueue(() -> {
                if (p != null) {
                    CameraState.report(p, msg.x(), msg.y(), msg.z(), msg.active());
                }
            });
        }, PacketChannel.Direction.TO_SERVER);
        CHANNEL.register(
            Packets.ChoiceParagraph.class,
            Packets.ChoiceParagraph::write,
            Packets.ChoiceParagraph::read,
            (msg, ctx) -> ctx.enqueue(() -> SprauteScriptInstance.paragraph(msg.scriptId(), msg.choiceId(), msg.index())),
            PacketChannel.Direction.TO_SERVER
        );
        CHANNEL.register(
            Packets.SpeechNewest.class,
            Packets.SpeechNewest::write,
            Packets.SpeechNewest::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.speechNewest(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.ScreenFx.class,
            Packets.ScreenFx::write,
            Packets.ScreenFx::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.screenFx(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.BuildFly.class,
            Packets.BuildFly::write,
            Packets.BuildFly::read,
            (msg, ctx) -> client(ctx, () -> () -> BuildFx.receive(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.BoneWatch.class,
            Packets.BoneWatch::write,
            Packets.BoneWatch::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.boneWatch(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.WorldPanelShow.class,
            Packets.WorldPanelShow::write,
            Packets.WorldPanelShow::read,
            (msg, ctx) -> client(ctx, () -> () -> WorldPanelsClient.show(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.WorldPanelHide.class,
            Packets.WorldPanelHide::write,
            Packets.WorldPanelHide::read,
            (msg, ctx) -> client(ctx, () -> () -> WorldPanelsClient.hide(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.BlocksShake.class,
            Packets.BlocksShake::write,
            Packets.BlocksShake::read,
            (msg, ctx) -> client(ctx, () -> () -> BlockQuakeFx.shake(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.BlocksWave.class,
            Packets.BlocksWave::write,
            Packets.BlocksWave::read,
            (msg, ctx) -> client(ctx, () -> () -> BlockQuakeFx.wave(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.PlayerSlotsSync.class,
            Packets.PlayerSlotsSync::write,
            Packets.PlayerSlotsSync::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.playerSlots(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.PlayerRulesSync.class,
            Packets.PlayerRulesSync::write,
            Packets.PlayerRulesSync::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.playerRules(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.PostFx.class,
            Packets.PostFx::write,
            Packets.PostFx::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.postFx(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.CoinsSync.class,
            Packets.CoinsSync::write,
            Packets.CoinsSync::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.coins(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(Packets.BigPart.class, Packets.BigPart::write, Packets.BigPart::read, SprauteNetwork::handleBig, PacketChannel.Direction.TO_SERVER);
        CHANNEL.register(
            Packets.ContentOffer.class,
            Packets.ContentOffer::write,
            Packets.ContentOffer::read,
            (msg, ctx) -> server(ctx, player -> ProjectContent.offer(player, msg)),
            PacketChannel.Direction.TO_SERVER
        );
        CHANNEL.register(
            Packets.ContentWant.class,
            Packets.ContentWant::write,
            Packets.ContentWant::read,
            (msg, ctx) -> client(ctx, () -> () -> ContentClient.wanted(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.ContentIndex.class,
            Packets.ContentIndex::write,
            Packets.ContentIndex::read,
            (msg, ctx) -> client(ctx, () -> () -> ContentClient.index(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(Packets.ContentRequest.class, Packets.ContentRequest::write, Packets.ContentRequest::read, (msg, ctx) -> {
            ServerPlayer p = ctx.sender();
            ctx.enqueue(() -> {
                if (p != null) {
                    ProjectContent.requested(p, msg);
                }
            });
        }, PacketChannel.Direction.TO_SERVER);
        CHANNEL.register(
            Packets.ContentData.class, Packets.ContentData::write, Packets.ContentData::read, SprauteNetwork::content, PacketChannel.Direction.BOTH
        );
        CHANNEL.register(
            Packets.SkillsView.class,
            Packets.SkillsView::write,
            Packets.SkillsView::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.skillsView(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(Packets.SkillAction.class, Packets.SkillAction::write, Packets.SkillAction::read, (msg, ctx) -> {
            ServerPlayer p = ctx.sender();
            ctx.enqueue(() -> {
                if (p != null) {
                    Skills.action(p, msg);
                }
            });
        }, PacketChannel.Direction.TO_SERVER);
        CHANNEL.register(
            Packets.StealthSync.class,
            Packets.StealthSync::write,
            Packets.StealthSync::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.stealthSync(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.PlayerSkin.class,
            Packets.PlayerSkin::write,
            Packets.PlayerSkin::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.playerSkin(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(Packets.DebugSet.class, Packets.DebugSet::write, Packets.DebugSet::read, (msg, ctx) -> server(ctx, player -> {
            String problem = SprauteRuntime.get().debugSet(player, msg);
            sendTo(player, problem == null ? SprauteRuntime.get().status(player) : new Packets.Log(3, "Spraute", problem));
        }), PacketChannel.Direction.TO_SERVER);
        CHANNEL.register(
            Packets.GizmoSet.class,
            Packets.GizmoSet::write,
            Packets.GizmoSet::read,
            (msg, ctx) -> client(ctx, () -> () -> GizmoClient.set(msg.scriptId(), msg.id(), msg.json())),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.GizmoRemove.class,
            Packets.GizmoRemove::write,
            Packets.GizmoRemove::read,
            (msg, ctx) -> client(ctx, () -> () -> GizmoClient.remove(msg.scriptId(), msg.id())),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.NavUpdate.class,
            Packets.NavUpdate::write,
            Packets.NavUpdate::read,
            (msg, ctx) -> client(ctx, () -> () -> NavClient.receive(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
        CHANNEL.register(
            Packets.PlayerAnim.class,
            Packets.PlayerAnim::write,
            Packets.PlayerAnim::read,
            (msg, ctx) -> client(ctx, () -> () -> ClientHandlers.playerAnim(msg)),
            PacketChannel.Direction.TO_CLIENT
        );
    }

    private static void content(Packets.ContentData msg, PacketChannel.Context ctx) {
        if (ctx.sender() == null) {
            client(ctx, () -> () -> ContentClient.received(msg));
        } else {
            server(ctx, player -> ProjectContent.received(player, msg));
        }
    }

    private static void handleCameraEvent(Packets.CameraEvent msg, PacketChannel.Context ctx) {
        ServerPlayer player = ctx.sender();
        ctx.enqueue(() -> {
            if (player != null) {
                GameEvents.camera(player, msg.scriptId(), msg.kind(), msg.name());
            }
        });
    }

    private static void handleQuestTrack(Packets.QuestTrack msg, PacketChannel.Context ctx) {
        ServerPlayer player = ctx.sender();
        ctx.enqueue(() -> {
            if (player != null) {
                Quests.track(player, msg.key());
            }
        });
    }

    private static void handleShopDeal(Packets.ShopDeal msg, PacketChannel.Context ctx) {
        ServerPlayer player = ctx.sender();
        ctx.enqueue(() -> {
            if (player != null) {
                Shops.deal(player, msg);
            }
        });
    }

    private static void handleChoiceAnswer(Packets.ChoiceAnswer msg, PacketChannel.Context ctx) {
        ServerPlayer from = ctx.sender();
        ctx.enqueue(() -> SprauteScriptInstance.answer(from, msg.scriptId(), msg.choiceId(), msg.answer()));
    }

    private static void handleEngineSet(Packets.EngineSet msg, PacketChannel.Context ctx) {
        server(ctx, player -> engineSet(player, msg));
    }

    private static void engineSet(ServerPlayer player, Packets.EngineSet msg) {
        EngineServer.set(player.getServer(), EngineConfig.fromJson(msg.json()));
        sendTo(player, new Packets.Log(1, "Spraute", "Настройки движка применены в мире"));
    }

    public static void sendToAll(Object msg) {
        for (ServerPlayer p : SprauteRuntime.audience()) {
            sendTo(p, msg);
        }
    }

    public static void sendToAudience(int scriptId, Object msg) {
        for (ServerPlayer p : SprauteRuntime.audience(scriptId)) {
            sendTo(p, msg);
        }
    }

    public static void sendToServer(Object msg) {
        int kind = msg instanceof Packets.Deploy
            ? 0
            : (msg instanceof Packets.SourcesSet ? 1 : (msg instanceof Packets.EngineSet ? 2 : (msg instanceof Packets.ContentOffer ? 3 : -1)));
        if (kind < 0) {
            CHANNEL.sendToServer(msg);
        } else {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            switch (kind) {
                case 0:
                    ((Packets.Deploy)msg).write(buf);
                    break;
                case 1:
                    ((Packets.SourcesSet)msg).write(buf);
                    break;
                case 2:
                    ((Packets.EngineSet)msg).write(buf);
                    break;
                default:
                    ((Packets.ContentOffer)msg).write(buf);
            }

            byte[] all = new byte[buf.readableBytes()];
            buf.readBytes(all);
            int total = Math.max(1, (all.length + 30000 - 1) / 30000);
            int transfer = nextTransfer++;

            for (int i = 0; i < total; i++) {
                CHANNEL.sendToServer(new Packets.BigPart(transfer, kind, i, total, Arrays.copyOfRange(all, i * 30000, Math.min(all.length, (i + 1) * 30000))));
            }
        }
    }

    private static void handleBig(Packets.BigPart msg, PacketChannel.Context ctx) {
        server(ctx, player -> {
            String key = player.getUUID() + "/" + msg.transfer();
            ByteArrayOutputStream part = BIG.computeIfAbsent(key, k -> new ByteArrayOutputStream());
            part.writeBytes(msg.data());
            if (part.size() > 33554432) {
                BIG.remove(key);
            } else if (msg.part() + 1 >= msg.total()) {
                BIG.remove(key);
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(part.toByteArray()));
                switch (msg.kind()) {
                    case 0:
                        deploy(player, Packets.Deploy.read(buf));
                        break;
                    case 1:
                        sourcesSet(player, Packets.SourcesSet.read(buf));
                        break;
                    case 2:
                        engineSet(player, Packets.EngineSet.read(buf));
                        break;
                    case 3:
                        ProjectContent.offer(player, Packets.ContentOffer.read(buf));
                }
            }
        });
    }

    public static void sendToEveryone(Object msg) {
        CHANNEL.sendToEveryone(msg);
    }

    public static void sendTo(ServerPlayer player, Object msg) {
        CHANNEL.sendTo(player, msg);
    }

    private static void client(PacketChannel.Context ctx, Supplier<Runnable> action) {
        ctx.enqueue(() -> {
            if (Platform.get().isClient()) {
                action.get().run();
            }
        });
    }

    private static void server(PacketChannel.Context ctx, Consumer<ServerPlayer> action) {
        ServerPlayer player = ctx.sender();
        ctx.enqueue(() -> {
            if (player != null) {
                if (!SprauteRuntime.canEdit(player)) {
                    sendTo(player, new Packets.Log(3, "Spraute", "Нужны права оператора (включите читы)"));
                } else {
                    action.accept(player);
                }
            }
        });
    }

    private static void handleSourcesSet(Packets.SourcesSet msg, PacketChannel.Context ctx) {
        server(ctx, player -> sourcesSet(player, msg));
    }

    private static void sourcesSet(ServerPlayer player, Packets.SourcesSet msg) {
        SprauteRuntime.get().putGuis(msg.project(), msg.guis());
        Zones.sourcesSaved(msg.project());
    }

    private static void handleDeploy(Packets.Deploy msg, PacketChannel.Context ctx) {
        server(ctx, player -> deploy(player, msg));
    }

    private static void deploy(ServerPlayer player, Packets.Deploy msg) {
        SprauteRuntime.get().putGuis(msg.project(), msg.guis());
        SprauteRuntime.DeployResult r = SprauteRuntime.get().deploy(msg.project(), msg.source());
        sendTo(player, new Packets.DeployResult(r.ok(), r.id(), r.messages()));
        if (r.ok() && msg.start()) {
            SprauteRuntime.env().ifPresent(env -> env.stop(r.id()));
            runCommand(player, "script start " + r.id());
        }

        sendTo(player, SprauteRuntime.get().status(player));
    }

    private static void handleUndeploy(Packets.Undeploy msg, PacketChannel.Context ctx) {
        server(
            ctx,
            player -> {
                boolean removed = SprauteRuntime.get().undeploy(msg.id());
                sendTo(
                    player,
                    new Packets.Log(
                        removed ? 1 : 2, "Spraute", removed ? "Сюжет #" + msg.id() + " выгружен из игры" : "Сюжет #" + msg.id() + " не был загружен"
                    )
                );
                sendTo(player, SprauteRuntime.get().status(player));
            }
        );
    }

    private static void handleCommand(Packets.RunCommand msg, PacketChannel.Context ctx) {
        server(ctx, player -> {
            runCommand(player, msg.command());
            sendTo(player, SprauteRuntime.get().status(player));
        });
    }

    private static void handleGuiEvent(Packets.GuiEvent msg, PacketChannel.Context ctx) {
        ServerPlayer player = ctx.sender();
        ctx.enqueue(() -> {
            if (player != null) {
                SprauteRuntime.get().onGuiEvent(player, msg);
            }
        });
    }

    private static void handleStatusRequest(Packets.RequestStatus msg, PacketChannel.Context ctx) {
        ServerPlayer player = ctx.sender();
        ctx.enqueue(() -> {
            if (player != null) {
                if (!SprauteRuntime.canEdit(player)) {
                    sendTo(player, new Packets.Status(false, List.of(), List.of()));
                } else {
                    sendTo(player, SprauteRuntime.get().status(player));
                }
            }
        });
    }

    public static void runCommand(ServerPlayer player, String command) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            final String line = command.startsWith("/") ? command.substring(1) : command;
            final List<Packets.Log> output = new ArrayList<>();
            CommandSource capture = new CommandSource() {
                @Override
                public void sendSystemMessage(Component message) {
                    TextColor color = message.getStyle().getColor();
                    boolean failure = color != null && color.equals(TextColor.fromLegacyFormat(ChatFormatting.RED));
                    output.add(new Packets.Log(failure ? 3 : 4, "/" + line, message.getString()));
                }

                @Override
                public boolean acceptsSuccess() {
                    return true;
                }

                @Override
                public boolean acceptsFailure() {
                    return true;
                }

                @Override
                public boolean shouldInformAdmins() {
                    return false;
                }
            };
            CommandSourceStack source = player.createCommandSourceStack().withSource(capture).withPermission(4);
            sendTo(player, new Packets.Log(0, "Spraute", "> /" + line));
            server.getCommands().performPrefixedCommand(source, line);

            for (Packets.Log l : output) {
                sendTo(player, l);
            }
        }
    }
}
