package org.sprautestudio.spraute_engine.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.sprautestudio.spraute_engine.gui.PanelLook;

public final class Packets {
    private static final int MAX_SOURCE = 1048576;

    private Packets() {
    }

    private static void writeLines(FriendlyByteBuf buf, List<String> lines) {
        buf.writeVarInt(lines.size());

        for (String l : lines) {
            buf.writeUtf(l, 1024);
        }
    }

    private static List<String> readLines(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> out = new ArrayList<>(Math.min(n, 4096));

        for (int i = 0; i < n; i++) {
            out.add(buf.readUtf(1024));
        }

        return out;
    }

    public record BigPart(int transfer, int kind, int part, int total, byte[] data) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.transfer);
            buf.writeByte(this.kind);
            buf.writeVarInt(this.part);
            buf.writeVarInt(this.total);
            buf.writeByteArray(this.data);
        }

        public static Packets.BigPart read(FriendlyByteBuf buf) {
            return new Packets.BigPart(buf.readVarInt(), buf.readByte(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(65536));
        }
    }

    public record BlocksShake(long from, long to, boolean round, float power, float seconds, boolean together) {
        public void write(FriendlyByteBuf buf) {
            buf.writeLong(this.from);
            buf.writeLong(this.to);
            buf.writeBoolean(this.round);
            buf.writeFloat(this.power);
            buf.writeFloat(this.seconds);
            buf.writeBoolean(this.together);
        }

        public static Packets.BlocksShake read(FriendlyByteBuf buf) {
            return new Packets.BlocksShake(buf.readLong(), buf.readLong(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readBoolean());
        }
    }

    public record BlocksWave(double x, double y, double z, float radius, float height, float step, float rise, float tilt) {
        public void write(FriendlyByteBuf buf) {
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeFloat(this.radius);
            buf.writeFloat(this.height);
            buf.writeFloat(this.step);
            buf.writeFloat(this.rise);
            buf.writeFloat(this.tilt);
        }

        public static Packets.BlocksWave read(FriendlyByteBuf buf) {
            return new Packets.BlocksWave(
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()
            );
        }
    }

    public record BoneWatch(int entity) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.entity);
        }

        public static Packets.BoneWatch read(FriendlyByteBuf buf) {
            return new Packets.BoneWatch(buf.readVarInt());
        }
    }

    public record Branch(String name, String doing, int count) {
        void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.name, 256);
            buf.writeUtf(this.doing, 512);
            buf.writeVarInt(this.count + 1);
        }

        static Packets.Branch read(FriendlyByteBuf buf) {
            return new Packets.Branch(buf.readUtf(256), buf.readUtf(512), buf.readVarInt() - 1);
        }
    }

    public record BuildFly(int flight, int ease, boolean reverse, boolean spinning, float swirl, long[] pos, int[] states, float[] from) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.flight);
            buf.writeVarInt(this.ease);
            buf.writeBoolean(this.reverse);
            buf.writeBoolean(this.spinning);
            buf.writeFloat(this.swirl);
            buf.writeVarInt(this.pos.length);

            for (int i = 0; i < this.pos.length; i++) {
                buf.writeLong(this.pos[i]);
                buf.writeVarInt(this.states[i]);
                buf.writeFloat(this.from[i * 3]);
                buf.writeFloat(this.from[i * 3 + 1]);
                buf.writeFloat(this.from[i * 3 + 2]);
            }
        }

        public static Packets.BuildFly read(FriendlyByteBuf buf) {
            int flight = buf.readVarInt();
            int ease = buf.readVarInt();
            boolean reverse = buf.readBoolean();
            boolean spinning = buf.readBoolean();
            float swirl = buf.readFloat();
            int n = buf.readVarInt();
            long[] pos = new long[n];
            int[] states = new int[n];
            float[] from = new float[n * 3];

            for (int i = 0; i < n; i++) {
                pos[i] = buf.readLong();
                states[i] = buf.readVarInt();
                from[i * 3] = buf.readFloat();
                from[i * 3 + 1] = buf.readFloat();
                from[i * 3 + 2] = buf.readFloat();
            }

            return new Packets.BuildFly(flight, ease, reverse, spinning, swirl, pos, states, from);
        }
    }

    public record CameraEvent(int scriptId, String kind, String name) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.kind, 32);
            buf.writeUtf(this.name, 256);
        }

        public static Packets.CameraEvent read(FriendlyByteBuf buf) {
            return new Packets.CameraEvent(buf.readVarInt(), buf.readUtf(32), buf.readUtf(256));
        }
    }

    public record CameraOp(int scriptId, String json) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.json, 8192);
        }

        public static Packets.CameraOp read(FriendlyByteBuf buf) {
            return new Packets.CameraOp(buf.readVarInt(), buf.readUtf(8192));
        }
    }

    public record CameraPlay(int scriptId, String name, String json, double ox, double oy, double oz, Map<String, Integer> looks) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.name, 256);
            buf.writeUtf(this.json, 1048576);
            buf.writeDouble(this.ox);
            buf.writeDouble(this.oy);
            buf.writeDouble(this.oz);
            buf.writeVarInt(this.looks.size());
            this.looks.forEach((k, v) -> {
                buf.writeUtf(k, 256);
                buf.writeVarInt(v);
            });
        }

        public static Packets.CameraPlay read(FriendlyByteBuf buf) {
            int id = buf.readVarInt();
            String name = buf.readUtf(256);
            String json = buf.readUtf(1048576);
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            int n = buf.readVarInt();
            Map<String, Integer> looks = new HashMap<>();

            for (int i = 0; i < n && i < 256; i++) {
                looks.put(buf.readUtf(256), buf.readVarInt());
            }

            return new Packets.CameraPlay(id, name, json, x, y, z, looks);
        }
    }

    public record CameraPos(double x, double y, double z, boolean active) {
        public void write(FriendlyByteBuf buf) {
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeBoolean(this.active);
        }

        public static Packets.CameraPos read(FriendlyByteBuf buf) {
            return new Packets.CameraPos(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean());
        }
    }

    public record ChoiceAnswer(int scriptId, int choiceId, int answer) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeVarInt(this.choiceId);
            buf.writeVarInt(this.answer);
        }

        public static Packets.ChoiceAnswer read(FriendlyByteBuf buf) {
            return new Packets.ChoiceAnswer(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }
    }

    public record ChoiceOpen(
        int scriptId,
        int choiceId,
        String speaker,
        String question,
        List<String> answers,
        int entityId,
        List<Boolean> locked,
        List<String> emotions,
        String emotion,
        List<Boolean> centered,
        String portrait
    ) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeVarInt(this.choiceId);
            buf.writeUtf(this.speaker, 256);
            buf.writeUtf(this.question, 8192);
            buf.writeVarInt(this.answers.size());

            for (int i = 0; i < this.answers.size(); i++) {
                buf.writeUtf(this.answers.get(i), 1024);
                buf.writeBoolean(this.locked.get(i));
                buf.writeUtf(this.emotions.get(i), 64);
                buf.writeBoolean(this.centered.get(i));
            }

            buf.writeVarInt(this.entityId + 1);
            buf.writeUtf(this.emotion, 64);
            buf.writeUtf(this.portrait, 512);
        }

        public static Packets.ChoiceOpen read(FriendlyByteBuf buf) {
            int s = buf.readVarInt();
            int c = buf.readVarInt();
            String sp = buf.readUtf(256);
            String q = buf.readUtf(8192);
            int n = buf.readVarInt();
            List<String> answers = new ArrayList<>();
            List<String> emotions = new ArrayList<>();
            List<Boolean> locked = new ArrayList<>();
            List<Boolean> centered = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                answers.add(buf.readUtf(1024));
                locked.add(buf.readBoolean());
                emotions.add(buf.readUtf(64));
                centered.add(buf.readBoolean());
            }

            int entity = buf.readVarInt() - 1;
            return new Packets.ChoiceOpen(s, c, sp, q, answers, entity, locked, emotions, buf.readUtf(64), centered, buf.readUtf(512));
        }
    }

    public record ChoiceParagraph(int scriptId, int choiceId, int index) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeVarInt(this.choiceId);
            buf.writeVarInt(this.index);
        }

        public static Packets.ChoiceParagraph read(FriendlyByteBuf buf) {
            return new Packets.ChoiceParagraph(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }
    }

    public record CoinsSync(int value, int need) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.value);
            buf.writeVarInt(this.need + 1);
        }

        public static Packets.CoinsSync read(FriendlyByteBuf buf) {
            return new Packets.CoinsSync(buf.readVarInt(), buf.readVarInt() - 1);
        }
    }

    public record ContentData(String project, String path, int index, int total, byte[] data) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.project, 256);
            buf.writeUtf(this.path, 1024);
            buf.writeVarInt(this.index);
            buf.writeVarInt(this.total);
            buf.writeByteArray(this.data);
        }

        public static Packets.ContentData read(FriendlyByteBuf buf) {
            return new Packets.ContentData(buf.readUtf(256), buf.readUtf(1024), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(1048576));
        }
    }

    public record ContentIndex(String project, List<String> entries) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.project, 256);
            Packets.writeLines(buf, this.entries);
        }

        public static Packets.ContentIndex read(FriendlyByteBuf buf) {
            return new Packets.ContentIndex(buf.readUtf(256), Packets.readLines(buf));
        }
    }

    public record ContentOffer(String project, List<String> entries) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.project, 256);
            Packets.writeLines(buf, this.entries);
        }

        public static Packets.ContentOffer read(FriendlyByteBuf buf) {
            return new Packets.ContentOffer(buf.readUtf(256), Packets.readLines(buf));
        }
    }

    public record ContentRequest(String project, List<String> paths) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.project, 256);
            Packets.writeLines(buf, this.paths);
        }

        public static Packets.ContentRequest read(FriendlyByteBuf buf) {
            return new Packets.ContentRequest(buf.readUtf(256), Packets.readLines(buf));
        }
    }

    public record ContentWant(String project, List<String> paths) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.project, 256);
            Packets.writeLines(buf, this.paths);
        }

        public static Packets.ContentWant read(FriendlyByteBuf buf) {
            return new Packets.ContentWant(buf.readUtf(256), Packets.readLines(buf));
        }
    }

    public record DebugSet(int scriptId, String scope, String name, String value) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.scope, 16);
            buf.writeUtf(this.name, 256);
            buf.writeUtf(this.value, 4096);
        }

        public static Packets.DebugSet read(FriendlyByteBuf buf) {
            return new Packets.DebugSet(buf.readVarInt(), buf.readUtf(16), buf.readUtf(256), buf.readUtf(4096));
        }
    }

    public record Deploy(String project, String source, boolean start, Map<String, String> guis) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.project, 256);
            buf.writeUtf(this.source, 1048576);
            buf.writeBoolean(this.start);
            buf.writeVarInt(this.guis == null ? -1 : this.guis.size());
            if (this.guis != null) {
                this.guis.forEach((k, v) -> {
                    buf.writeUtf(k, 256);
                    buf.writeUtf(v, 1048576);
                });
            }
        }

        public static Packets.Deploy read(FriendlyByteBuf buf) {
            String project = buf.readUtf(256);
            String source = buf.readUtf(1048576);
            boolean start = buf.readBoolean();
            int n = buf.readVarInt();
            if (n < 0) {
                return new Packets.Deploy(project, source, start, null);
            } else {
                Map<String, String> guis = new LinkedHashMap<>();

                for (int i = 0; i < n && i < 256; i++) {
                    guis.put(buf.readUtf(256), buf.readUtf(1048576));
                }

                return new Packets.Deploy(project, source, start, guis);
            }
        }
    }

    public record DeployResult(boolean ok, int id, List<String> messages) {
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(this.ok);
            buf.writeVarInt(this.id);
            buf.writeVarInt(this.messages.size());

            for (String m : this.messages) {
                buf.writeUtf(m, 4096);
            }
        }

        public static Packets.DeployResult read(FriendlyByteBuf buf) {
            boolean ok = buf.readBoolean();
            int id = buf.readVarInt();
            int n = buf.readVarInt();
            List<String> list = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                list.add(buf.readUtf(4096));
            }

            return new Packets.DeployResult(ok, id, list);
        }
    }

    public record EngineSet(String json) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.json, 1048576);
        }

        public static Packets.EngineSet read(FriendlyByteBuf buf) {
            return new Packets.EngineSet(buf.readUtf(1048576));
        }
    }

    public record EngineSync(String json) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.json, 1048576);
        }

        public static Packets.EngineSync read(FriendlyByteBuf buf) {
            return new Packets.EngineSync(buf.readUtf(1048576));
        }
    }

    public record FxLaunch(
        String id, String project, String json, double x, double y, double z, double vx, double vy, double vz, double g, float scale, int scriptId
    ) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId + 1);
            buf.writeUtf(this.id, 256);
            buf.writeUtf(this.project, 256);
            buf.writeUtf(this.json, 1048576);
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeDouble(this.vx);
            buf.writeDouble(this.vy);
            buf.writeDouble(this.vz);
            buf.writeDouble(this.g);
            buf.writeFloat(this.scale);
        }

        public static Packets.FxLaunch read(FriendlyByteBuf buf) {
            int script = buf.readVarInt() - 1;
            return new Packets.FxLaunch(
                buf.readUtf(256),
                buf.readUtf(256),
                buf.readUtf(1048576),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                script
            );
        }
    }

    public record FxPlay(String id, String project, String json, double x, double y, double z, int entity, float scale, int scriptId, String bone) {
        public FxPlay(String id, String project, String json, double x, double y, double z, int entity, float scale, int scriptId) {
            this(id, project, json, x, y, z, entity, scale, scriptId, "");
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId + 1);
            buf.writeUtf(this.id, 256);
            buf.writeUtf(this.project, 256);
            buf.writeUtf(this.json, 1048576);
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeVarInt(this.entity + 1);
            buf.writeFloat(this.scale);
            buf.writeUtf(this.bone, 128);
        }

        public static Packets.FxPlay read(FriendlyByteBuf buf) {
            int script = buf.readVarInt() - 1;
            return new Packets.FxPlay(
                buf.readUtf(256),
                buf.readUtf(256),
                buf.readUtf(1048576),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt() - 1,
                buf.readFloat(),
                script,
                buf.readUtf(128)
            );
        }
    }

    public record FxStop(String id, int scriptId) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.id, 256);
            buf.writeVarInt(this.scriptId + 1);
        }

        public static Packets.FxStop read(FriendlyByteBuf buf) {
            return new Packets.FxStop(buf.readUtf(256), buf.readVarInt() - 1);
        }
    }

    public record GizmoRemove(int scriptId, String id) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.id, 256);
        }

        public static Packets.GizmoRemove read(FriendlyByteBuf buf) {
            return new Packets.GizmoRemove(buf.readVarInt(), buf.readUtf(256));
        }
    }

    public record GizmoSet(int scriptId, String id, String json) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.id, 256);
            buf.writeUtf(this.json, 16384);
        }

        public static Packets.GizmoSet read(FriendlyByteBuf buf) {
            return new Packets.GizmoSet(buf.readVarInt(), buf.readUtf(256), buf.readUtf(16384));
        }
    }

    public record GuiClose(int scriptId) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
        }

        public static Packets.GuiClose read(FriendlyByteBuf buf) {
            return new Packets.GuiClose(buf.readVarInt());
        }
    }

    public record GuiEvent(int scriptId, String gui, String element, String kind, Map<String, String> inputs) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.gui, 256);
            buf.writeUtf(this.element, 256);
            buf.writeUtf(this.kind, 32);
            buf.writeVarInt(this.inputs.size());
            this.inputs.forEach((k, v) -> {
                buf.writeUtf(k, 256);
                buf.writeUtf(v, 4096);
            });
        }

        public static Packets.GuiEvent read(FriendlyByteBuf buf) {
            int id = buf.readVarInt();
            String gui = buf.readUtf(256);
            String el = buf.readUtf(256);
            String kind = buf.readUtf(32);
            int n = buf.readVarInt();
            Map<String, String> inputs = new LinkedHashMap<>();

            for (int i = 0; i < n && i < 128; i++) {
                inputs.put(buf.readUtf(256), buf.readUtf(4096));
            }

            return new Packets.GuiEvent(id, gui, el, kind, inputs);
        }
    }

    public record GuiOpen(int scriptId, String project, String name, String json) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.project, 256);
            buf.writeUtf(this.name, 256);
            buf.writeUtf(this.json, 1048576);
        }

        public static Packets.GuiOpen read(FriendlyByteBuf buf) {
            return new Packets.GuiOpen(buf.readVarInt(), buf.readUtf(256), buf.readUtf(256), buf.readUtf(1048576));
        }
    }

    public record GuiUpdate(int scriptId, String element, String prop, String value) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.element, 256);
            buf.writeUtf(this.prop, 64);
            buf.writeUtf(this.value, 32767);
        }

        public static Packets.GuiUpdate read(FriendlyByteBuf buf) {
            return new Packets.GuiUpdate(buf.readVarInt(), buf.readUtf(256), buf.readUtf(64), buf.readUtf(32767));
        }
    }

    public record HitboxSync(int entityId, int flags) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.entityId);
            buf.writeVarInt(this.flags);
        }

        public static Packets.HitboxSync read(FriendlyByteBuf buf) {
            return new Packets.HitboxSync(buf.readVarInt(), buf.readVarInt());
        }
    }

    public record KeyPress(String key) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.key, 64);
        }

        public static Packets.KeyPress read(FriendlyByteBuf buf) {
            return new Packets.KeyPress(buf.readUtf(64));
        }
    }

    public record Log(int level, String source, String text) {
        public static final int INFO = 0;
        public static final int SUCCESS = 1;
        public static final int WARNING = 2;
        public static final int ERROR = 3;
        public static final int OUTPUT = 4;

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.level);
            buf.writeUtf(this.source, 256);
            buf.writeUtf(this.text, 32767);
        }

        public static Packets.Log read(FriendlyByteBuf buf) {
            return new Packets.Log(buf.readVarInt(), buf.readUtf(256), buf.readUtf(32767));
        }
    }

    public record NavUpdate(String id, String title, int color, double x, double y, double z, boolean remove) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.id, 256);
            buf.writeUtf(this.title, 256);
            buf.writeInt(this.color);
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeBoolean(this.remove);
        }

        public static Packets.NavUpdate read(FriendlyByteBuf buf) {
            return new Packets.NavUpdate(
                buf.readUtf(256), buf.readUtf(256), buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean()
            );
        }
    }

    public record Notify(String text, String icon, String item, String project, boolean sound) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.text, 1024);
            buf.writeUtf(this.icon, 512);
            buf.writeUtf(this.item, 256);
            buf.writeUtf(this.project, 256);
            buf.writeBoolean(this.sound);
        }

        public static Packets.Notify read(FriendlyByteBuf buf) {
            return new Packets.Notify(buf.readUtf(1024), buf.readUtf(512), buf.readUtf(256), buf.readUtf(256), buf.readBoolean());
        }
    }

    public record OverlayHide(int scriptId, String name) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.name, 256);
        }

        public static Packets.OverlayHide read(FriendlyByteBuf buf) {
            return new Packets.OverlayHide(buf.readVarInt(), buf.readUtf(256));
        }
    }

    public record OverlayShow(int scriptId, String project, String name, String json) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.project, 256);
            buf.writeUtf(this.name, 256);
            buf.writeUtf(this.json, 1048576);
        }

        public static Packets.OverlayShow read(FriendlyByteBuf buf) {
            return new Packets.OverlayShow(buf.readVarInt(), buf.readUtf(256), buf.readUtf(256), buf.readUtf(1048576));
        }
    }

    public record PartPoses(int entity, List<String> bones, float[] data) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.entity);
            buf.writeVarInt(this.bones.size());

            for (int i = 0; i < this.bones.size(); i++) {
                buf.writeUtf(this.bones.get(i), 128);

                for (int k = 0; k < 7; k++) {
                    buf.writeFloat(this.data[i * 7 + k]);
                }
            }
        }

        public static Packets.PartPoses read(FriendlyByteBuf buf) {
            int entity = buf.readVarInt();
            int n = Math.min(32, buf.readVarInt());
            List<String> bones = new ArrayList<>();
            float[] data = new float[n * 7];

            for (int i = 0; i < n; i++) {
                bones.add(buf.readUtf(128));

                for (int k = 0; k < 7; k++) {
                    data[i * 7 + k] = buf.readFloat();
                }
            }

            return new Packets.PartPoses(entity, bones, data);
        }
    }

    public record PlayerAnim(UUID player, String anim, float speed, float fade, int times, int mode) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(this.player);
            buf.writeUtf(this.anim, 512);
            buf.writeFloat(this.speed);
            buf.writeFloat(this.fade);
            buf.writeVarInt(this.times);
            buf.writeByte(this.mode);
        }

        public static Packets.PlayerAnim read(FriendlyByteBuf buf) {
            return new Packets.PlayerAnim(buf.readUUID(), buf.readUtf(512), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readByte());
        }
    }

    public record PlayerRulesSync(String[] names, double[] values) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.names.length);

            for (int i = 0; i < this.names.length; i++) {
                buf.writeUtf(this.names[i], 64);
                buf.writeDouble(this.values[i]);
            }
        }

        public static Packets.PlayerRulesSync read(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            String[] names = new String[n];
            double[] values = new double[n];

            for (int i = 0; i < n; i++) {
                names[i] = buf.readUtf(64);
                values[i] = buf.readDouble();
            }

            return new Packets.PlayerRulesSync(names, values);
        }
    }

    public record PlayerSkin(String player, String texture) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.player, 64);
            buf.writeUtf(this.texture, 512);
        }

        public static Packets.PlayerSkin read(FriendlyByteBuf buf) {
            return new Packets.PlayerSkin(buf.readUtf(64), buf.readUtf(512));
        }
    }

    public record PlayerSlotsSync(boolean own, long locked, int size, String order) {
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(this.own);
            buf.writeLong(this.locked);
            buf.writeVarInt(this.size);
            buf.writeUtf(this.order, 16);
        }

        public static Packets.PlayerSlotsSync read(FriendlyByteBuf buf) {
            return new Packets.PlayerSlotsSync(buf.readBoolean(), buf.readLong(), buf.readVarInt(), buf.readUtf(16));
        }
    }

    public record PostFx(String op, String layer, String data, int in, int hold, int out, float power) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.op, 16);
            buf.writeUtf(this.layer, 512);
            buf.writeUtf(this.data);
            buf.writeVarInt(this.in);
            buf.writeInt(this.hold);
            buf.writeVarInt(this.out);
            buf.writeFloat(this.power);
        }

        public static Packets.PostFx read(FriendlyByteBuf buf) {
            return new Packets.PostFx(buf.readUtf(16), buf.readUtf(512), buf.readUtf(), buf.readVarInt(), buf.readInt(), buf.readVarInt(), buf.readFloat());
        }
    }

    public record QuestSync(String json, boolean open) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.json, 1048576);
            buf.writeBoolean(this.open);
        }

        public static Packets.QuestSync read(FriendlyByteBuf buf) {
            return new Packets.QuestSync(buf.readUtf(1048576), buf.readBoolean());
        }
    }

    public record QuestTrack(String key) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.key, 512);
        }

        public static Packets.QuestTrack read(FriendlyByteBuf buf) {
            return new Packets.QuestTrack(buf.readUtf(512));
        }
    }

    public record RequestStatus() {
        public void write(FriendlyByteBuf buf) {
        }

        public static Packets.RequestStatus read(FriendlyByteBuf buf) {
            return new Packets.RequestStatus();
        }
    }

    public record RunCommand(String command) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.command, 4096);
        }

        public static Packets.RunCommand read(FriendlyByteBuf buf) {
            return new Packets.RunCommand(buf.readUtf(4096));
        }
    }

    public record ScreenFx(String kind, int in, int hold, int out, int argb) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.kind, 16);
            buf.writeVarInt(this.in);
            buf.writeVarInt(this.hold);
            buf.writeVarInt(this.out);
            buf.writeInt(this.argb);
        }

        public static Packets.ScreenFx read(FriendlyByteBuf buf) {
            return new Packets.ScreenFx(buf.readUtf(16), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readInt());
        }
    }

    public static final class ScriptEntry {
        public int id;
        public String title = "";
        public String project = "";
        public boolean spraute;
        public boolean active;
        public boolean paused;
        public String stage = "";
        public int actionIndex;
        public int actionCount;
        public String doing = "";
        public final List<String> labels = new ArrayList<>();
        public final List<Packets.Branch> branches = new ArrayList<>();
        public final List<Packets.Value> vars = new ArrayList<>();

        void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.id);
            buf.writeUtf(this.title, 512);
            buf.writeUtf(this.project, 256);
            buf.writeBoolean(this.spraute);
            buf.writeBoolean(this.active);
            buf.writeBoolean(this.paused);
            buf.writeUtf(this.stage, 512);
            buf.writeVarInt(this.actionIndex);
            buf.writeVarInt(this.actionCount);
            buf.writeUtf(this.doing, 512);
            buf.writeVarInt(this.labels.size());

            for (String l : this.labels) {
                buf.writeUtf(l, 256);
            }

            buf.writeVarInt(this.branches.size());

            for (Packets.Branch b : this.branches) {
                b.write(buf);
            }

            buf.writeVarInt(this.vars.size());

            for (Packets.Value v : this.vars) {
                v.write(buf);
            }
        }

        static Packets.ScriptEntry read(FriendlyByteBuf buf) {
            Packets.ScriptEntry e = new Packets.ScriptEntry();
            e.id = buf.readVarInt();
            e.title = buf.readUtf(512);
            e.project = buf.readUtf(256);
            e.spraute = buf.readBoolean();
            e.active = buf.readBoolean();
            e.paused = buf.readBoolean();
            e.stage = buf.readUtf(512);
            e.actionIndex = buf.readVarInt();
            e.actionCount = buf.readVarInt();
            e.doing = buf.readUtf(512);
            int n = buf.readVarInt();

            for (int i = 0; i < n; i++) {
                e.labels.add(buf.readUtf(256));
            }

            int b = buf.readVarInt();

            for (int i = 0; i < b; i++) {
                e.branches.add(Packets.Branch.read(buf));
            }

            int v = buf.readVarInt();

            for (int i = 0; i < v; i++) {
                e.vars.add(Packets.Value.read(buf));
            }

            return e;
        }
    }

    public record ShopDeal(int scriptId, String key, String offer, int count, int entityId) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId + 1);
            buf.writeUtf(this.key, 512);
            buf.writeUtf(this.offer, 256);
            buf.writeVarInt(this.count);
            buf.writeVarInt(this.entityId + 1);
        }

        public static Packets.ShopDeal read(FriendlyByteBuf buf) {
            return new Packets.ShopDeal(buf.readVarInt() - 1, buf.readUtf(512), buf.readUtf(256), buf.readVarInt(), buf.readVarInt() - 1);
        }
    }

    public record ShopOpen(String json, boolean refresh) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.json, 1048576);
            buf.writeBoolean(this.refresh);
        }

        public static Packets.ShopOpen read(FriendlyByteBuf buf) {
            return new Packets.ShopOpen(buf.readUtf(1048576), buf.readBoolean());
        }
    }

    public record SkillAction(String key, String node, String action) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.key);
            buf.writeUtf(this.node);
            buf.writeUtf(this.action);
        }

        public static Packets.SkillAction read(FriendlyByteBuf buf) {
            return new Packets.SkillAction(buf.readUtf(), buf.readUtf(), buf.readUtf());
        }
    }

    public record SkillsView(String json, int mode) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.json, 1048576);
            buf.writeVarInt(this.mode);
        }

        public static Packets.SkillsView read(FriendlyByteBuf buf) {
            return new Packets.SkillsView(buf.readUtf(1048576), buf.readVarInt());
        }
    }

    public record SoundPlay(
        String project, String name, double x, double y, double z, boolean positional, float volume, float pitch, boolean loop, String id, float fade
    ) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.project, 256);
            buf.writeUtf(this.name, 256);
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeBoolean(this.positional);
            buf.writeFloat(this.volume);
            buf.writeFloat(this.pitch);
            buf.writeBoolean(this.loop);
            buf.writeUtf(this.id, 256);
            buf.writeFloat(this.fade);
        }

        public static Packets.SoundPlay read(FriendlyByteBuf buf) {
            return new Packets.SoundPlay(
                buf.readUtf(256),
                buf.readUtf(256),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readUtf(256),
                buf.readFloat()
            );
        }
    }

    public record SoundStop(String id, float fade) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.id, 256);
            buf.writeFloat(this.fade);
        }

        public static Packets.SoundStop read(FriendlyByteBuf buf) {
            return new Packets.SoundStop(buf.readUtf(256), buf.readFloat());
        }
    }

    public record SourcesSet(String project, Map<String, String> guis) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.project, 256);
            buf.writeVarInt(this.guis.size());
            this.guis.forEach((k, v) -> {
                buf.writeUtf(k, 256);
                buf.writeUtf(v, 1048576);
            });
        }

        public static Packets.SourcesSet read(FriendlyByteBuf buf) {
            String project = buf.readUtf(256);
            int n = buf.readVarInt();
            Map<String, String> guis = new LinkedHashMap<>();

            for (int i = 0; i < n && i < 256; i++) {
                guis.put(buf.readUtf(256), buf.readUtf(1048576));
            }

            return new Packets.SourcesSet(project, guis);
        }
    }

    public record Speech(int scriptId, String name, int color, String icon, String text, String hint, boolean think, boolean waits, boolean historyOnly) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.name, 256);
            buf.writeInt(this.color);
            buf.writeUtf(this.icon, 512);
            buf.writeUtf(this.text, 1048576);
            buf.writeUtf(this.hint, 64);
            buf.writeBoolean(this.think);
            buf.writeBoolean(this.waits);
            buf.writeBoolean(this.historyOnly);
        }

        public static Packets.Speech read(FriendlyByteBuf buf) {
            return new Packets.Speech(
                buf.readVarInt(),
                buf.readUtf(256),
                buf.readInt(),
                buf.readUtf(512),
                buf.readUtf(1048576),
                buf.readUtf(64),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
            );
        }
    }

    public record SpeechClear(int scriptId) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
        }

        public static Packets.SpeechClear read(FriendlyByteBuf buf) {
            return new Packets.SpeechClear(buf.readVarInt());
        }
    }

    public record SpeechHead(int scriptId, String character, int entityId, String model) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.character, 256);
            buf.writeVarInt(this.entityId + 1);
            buf.writeUtf(this.model, 512);
        }

        public static Packets.SpeechHead read(FriendlyByteBuf buf) {
            return new Packets.SpeechHead(buf.readVarInt(), buf.readUtf(256), buf.readVarInt() - 1, buf.readUtf(512));
        }
    }

    public record SpeechNewest(int scriptId) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId + 1);
        }

        public static Packets.SpeechNewest read(FriendlyByteBuf buf) {
            return new Packets.SpeechNewest(buf.readVarInt() - 1);
        }
    }

    public record Status(boolean ready, List<Packets.ScriptEntry> scripts, List<String> categories, List<Packets.Value> playerVars, int coins) {
        public Status(boolean ready, List<Packets.ScriptEntry> scripts, List<String> categories) {
            this(ready, scripts, categories, List.of(), -1);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(this.ready);
            buf.writeVarInt(this.scripts.size());

            for (Packets.ScriptEntry e : this.scripts) {
                e.write(buf);
            }

            buf.writeVarInt(this.categories.size());

            for (String c : this.categories) {
                buf.writeUtf(c, 256);
            }

            buf.writeVarInt(this.playerVars.size());

            for (Packets.Value v : this.playerVars) {
                v.write(buf);
            }

            buf.writeVarInt(this.coins + 1);
        }

        public static Packets.Status read(FriendlyByteBuf buf) {
            boolean ready = buf.readBoolean();
            int n = buf.readVarInt();
            List<Packets.ScriptEntry> list = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                list.add(Packets.ScriptEntry.read(buf));
            }

            int c = buf.readVarInt();
            List<String> cats = new ArrayList<>();

            for (int i = 0; i < c; i++) {
                cats.add(buf.readUtf(256));
            }

            int pv = buf.readVarInt();
            List<Packets.Value> pvars = new ArrayList<>();

            for (int i = 0; i < pv; i++) {
                pvars.add(Packets.Value.read(buf));
            }

            return new Packets.Status(ready, list, cats, pvars, buf.readVarInt() - 1);
        }
    }

    public record StealthSync(String json) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.json, 1048576);
        }

        public static Packets.StealthSync read(FriendlyByteBuf buf) {
            return new Packets.StealthSync(buf.readUtf(1048576));
        }
    }

    public record Undeploy(int id) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.id);
        }

        public static Packets.Undeploy read(FriendlyByteBuf buf) {
            return new Packets.Undeploy(buf.readVarInt());
        }
    }

    public record Value(String name, String value, String kind) {
        void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.name, 256);
            buf.writeUtf(this.value, 1024);
            buf.writeUtf(this.kind, 32);
        }

        static Packets.Value read(FriendlyByteBuf buf) {
            return new Packets.Value(buf.readUtf(256), buf.readUtf(1024), buf.readUtf(32));
        }
    }

    public record WorldPanelHide(int scriptId, String id) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.id, 256);
        }

        public static Packets.WorldPanelHide read(FriendlyByteBuf buf) {
            return new Packets.WorldPanelHide(buf.readVarInt(), buf.readUtf(256));
        }
    }

    public record WorldPanelShow(
        String id,
        int scriptId,
        String project,
        String name,
        String json,
        String dimension,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        float width,
        float reach,
        float range,
        boolean face,
        PanelLook look,
        int entity,
        String bone,
        float offsetX,
        float offsetY,
        float offsetZ,
        boolean self
    ) {
        public static final int NO_ENTITY = -1;

        public Packets.WorldPanelShow onEntity(int number) {
            return new Packets.WorldPanelShow(
                this.id,
                this.scriptId,
                this.project,
                this.name,
                this.json,
                this.dimension,
                this.x,
                this.y,
                this.z,
                this.yaw,
                this.pitch,
                this.width,
                this.reach,
                this.range,
                this.face,
                this.look,
                number,
                this.bone,
                this.offsetX,
                this.offsetY,
                this.offsetZ,
                this.self
            );
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.id, 256);
            buf.writeVarInt(this.scriptId);
            buf.writeUtf(this.project, 256);
            buf.writeUtf(this.name, 256);
            buf.writeUtf(this.json, 1048576);
            buf.writeUtf(this.dimension, 256);
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeFloat(this.yaw);
            buf.writeFloat(this.pitch);
            buf.writeFloat(this.width);
            buf.writeFloat(this.reach);
            buf.writeFloat(this.range);
            buf.writeBoolean(this.face);
            buf.writeUtf(this.look.turn(), 16);
            buf.writeFloat(this.look.opacity());
            buf.writeInt(this.look.tint());
            buf.writeBoolean(this.look.through());
            buf.writeBoolean(this.look.keepSize());
            buf.writeFloat(this.look.fade());
            buf.writeFloat(this.look.smooth());
            buf.writeFloat(this.look.bob());
            buf.writeVarInt(this.entity);
            buf.writeUtf(this.bone, 256);
            buf.writeFloat(this.offsetX);
            buf.writeFloat(this.offsetY);
            buf.writeFloat(this.offsetZ);
            buf.writeBoolean(this.self);
        }

        public static Packets.WorldPanelShow read(FriendlyByteBuf buf) {
            return new Packets.WorldPanelShow(
                buf.readUtf(256),
                buf.readVarInt(),
                buf.readUtf(256),
                buf.readUtf(256),
                buf.readUtf(1048576),
                buf.readUtf(256),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean(),
                new PanelLook(
                    buf.readUtf(16), buf.readFloat(), buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat()
                ),
                buf.readVarInt(),
                buf.readUtf(256),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean()
            );
        }
    }

    public record ZoneShow(String json) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.json, 1048576);
        }

        public static Packets.ZoneShow read(FriendlyByteBuf buf) {
            return new Packets.ZoneShow(buf.readUtf(1048576));
        }
    }

    public record ZoneTitle(String title, String subtitle) {
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.title, 512);
            buf.writeUtf(this.subtitle, 512);
        }

        public static Packets.ZoneTitle read(FriendlyByteBuf buf) {
            return new Packets.ZoneTitle(buf.readUtf(512), buf.readUtf(512));
        }
    }
}
