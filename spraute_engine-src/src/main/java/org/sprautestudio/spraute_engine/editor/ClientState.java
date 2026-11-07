package org.sprautestudio.spraute_engine.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.sprautestudio.spraute_engine.content.ContentClient;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;

public final class ClientState {
    private static final ClientState INSTANCE = new ClientState();
    private Packets.Status status = new Packets.Status(false, List.of(), List.of());
    private long statusTime;
    private final List<ClientState.LogLine> log = new ArrayList<>();
    private int logVersion;
    private int statusVersion;
    public Consumer<Packets.DeployResult> deployListener;
    private int quietDeploys;

    public static ClientState get() {
        return INSTANCE;
    }

    private ClientState() {
    }

    public Packets.Status status() {
        return this.status;
    }

    public long statusAge() {
        return this.statusTime == 0L ? Long.MAX_VALUE : System.currentTimeMillis() - this.statusTime;
    }

    public int statusVersion() {
        return this.statusVersion;
    }

    public Packets.ScriptEntry script(int id) {
        for (Packets.ScriptEntry e : this.status.scripts()) {
            if (e.id == id) {
                return e;
            }
        }

        return null;
    }

    public List<Integer> scriptIds() {
        List<Integer> ids = new ArrayList<>();

        for (Packets.ScriptEntry e : this.status.scripts()) {
            ids.add(e.id);
        }

        return ids;
    }

    public List<ClientState.LogLine> log() {
        return this.log;
    }

    public int logVersion() {
        return this.logVersion;
    }

    public void clearLog() {
        this.log.clear();
        this.logVersion++;
    }

    public void addLocal(int level, String source, String text) {
        for (String line : text.split("\n")) {
            this.log.add(new ClientState.LogLine(System.currentTimeMillis(), level, source, line));
        }

        while (this.log.size() > 2000) {
            this.log.remove(0);
        }

        this.logVersion++;
    }

    public void onStatus(Packets.Status s) {
        this.status = s;
        this.statusTime = System.currentTimeMillis();
        this.statusVersion++;
    }

    public void onLog(Packets.Log l) {
        this.addLocal(l.level(), l.source(), l.text());
    }

    public void onDeployResult(Packets.DeployResult r) {
        for (String m : r.messages()) {
            this.addLocal(r.ok() ? 1 : 3, "Деплой #" + r.id(), m);
        }

        if (this.quietDeploys > 0) {
            this.quietDeploys--;
        } else {
            if (this.deployListener != null) {
                this.deployListener.accept(r);
            }
        }
    }

    public void requestStatus() {
        if (Minecraft.getInstance().getConnection() != null) {
            SprauteNetwork.sendToServer(new Packets.RequestStatus());
        }
    }

    public void debugSet(int scriptId, String scope, String name, String value) {
        if (Minecraft.getInstance().getConnection() != null) {
            SprauteNetwork.sendToServer(new Packets.DebugSet(scriptId, scope, name, value));
        }
    }

    public void command(String command) {
        if (Minecraft.getInstance().getConnection() == null) {
            this.addLocal(3, "Spraute", "Нет подключения к миру");
        } else {
            SprauteNetwork.sendToServer(new Packets.RunCommand(command));
        }
    }

    public void deploy(String project, String source, boolean start, Map<String, String> guis) {
        if (Minecraft.getInstance().getConnection() == null) {
            this.addLocal(3, "Spraute", "Нет подключения к миру");
        } else {
            ContentClient.upload(project, () -> SprauteNetwork.sendToServer(new Packets.Deploy(project, source, start, guis)));
        }
    }

    public void deployAll(String project, List<String> sources, Map<String, String> guis) {
        if (Minecraft.getInstance().getConnection() != null && !sources.isEmpty()) {
            ContentClient.upload(project, () -> {
                this.quietDeploys = this.quietDeploys + sources.size();

                for (int i = 0; i < sources.size(); i++) {
                    SprauteNetwork.sendToServer(new Packets.Deploy(project, sources.get(i), false, i == 0 ? guis : null));
                }
            });
        }
    }

    public void sources(String project, Map<String, String> guis) {
        if (Minecraft.getInstance().getConnection() != null) {
            ContentClient.upload(project, null);
            SprauteNetwork.sendToServer(new Packets.SourcesSet(project, guis));
        }
    }

    public void engine(String json) {
        if (Minecraft.getInstance().getConnection() != null) {
            String project = EngineConfig.fromJson(json).project;
            ContentClient.upload(project, () -> SprauteNetwork.sendToServer(new Packets.EngineSet(json)));
        }
    }

    public void undeploy(int id) {
        SprauteNetwork.sendToServer(new Packets.Undeploy(id));
    }

    public record LogLine(long time, int level, String source, String text) {
    }
}
