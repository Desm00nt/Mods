package org.sprautestudio.spraute_engine.runtime;

import com.google.gson.JsonObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.script.Character;
import org.sprautestudio.spraute_engine.core.script.Characters;
import org.sprautestudio.spraute_engine.core.script.ScriptEnvironment;
import org.sprautestudio.spraute_engine.core.script.ScriptInstance;
import org.sprautestudio.spraute_engine.core.script.ScriptPerformerBuilder;
import org.sprautestudio.spraute_engine.core.world.Location;
import org.sprautestudio.spraute_engine.fight.Fights;
import org.sprautestudio.spraute_engine.fx.Casts;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.lang.Ast;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.runtime.models.EntityLibrary;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;

public class SprauteScriptInstance extends ScriptInstance {
    final SprauteRuntime.Deployed deployed;
    final Eval eval = new Eval(this);
    final Map<String, Object> vars = new LinkedHashMap<>();
    public final Map<String, SprauteScriptInstance.Handler> handlers = new LinkedHashMap<>();
    final Map<String, Character<?>> characters = new HashMap<>();
    final Map<String, Ast.LocationDecl> locationDecls = new HashMap<>();
    final Map<String, Integer> priorities = new HashMap<>();
    final Map<String, String> speechIcons = new HashMap<>();
    final Map<String, Integer> nameColors = new HashMap<>();
    final Map<String, Integer> cooldowns = new HashMap<>();
    final Map<String, String> speechNames = new HashMap<>();
    final Map<String, ResourceLocation> portraits = new HashMap<>();
    final Map<String, Deque<String>> callStacks = new HashMap<>();
    int lastAnswer;
    int choiceSeq;
    private List<String> paragraphAnims = List.of();
    private Entity dialogSpeaker;
    UUID choicePlayer;
    UUID guiPlayer;
    private final Map<String, String> overlays = new LinkedHashMap<>();
    private final Map<String, String> cameraNow = new LinkedHashMap<>();
    String openGui;
    String lastButton = "";
    int buttonSeq;
    int guiCloseSeq;
    final Map<String, String> guiInputs = new HashMap<>();
    final Map<String, String> guiZones = new HashMap<>();
    final Map<String, ItemStack> guiSlots = new HashMap<>();
    GuiDoc openGuiDoc;
    String lastSlot = "";
    final EventLog events = new EventLog();
    final Map<String, String> characterModels = new HashMap<>();
    String cutscene = "";
    CutsceneClips clips;
    Map<String, Object> event = new HashMap<>();
    int slotSeq;

    public Eval eval() {
        return this.eval;
    }

    @Override
    public void handleStop() {
        super.handleStop();

        try {
            SprauteNetwork.sendToAll(new Packets.FxStop("", this.getId()));
            SprauteNetwork.sendToAll(new Packets.OverlayHide(this.getId(), ""));
            SprauteNetwork.sendToAll(new Packets.GizmoRemove(this.getId(), ""));
            Fights.stopScript(this.getId());
            Casts.stopScript(this.getId());
            BossMoves.stopScript(this.getId());
        } catch (Throwable var2) {
        }
    }

    public Map<String, Object> variables() {
        return Collections.unmodifiableMap(this.vars);
    }

    public void setVariable(String name, Object value) {
        this.vars.put(name, value);
    }

    void openDialog(Entity speaker, List<String> anims) {
        this.dialogSpeaker = speaker;
        this.paragraphAnims = anims;
        this.playParagraph(0);
    }

    private void playParagraph(int i) {
        if (this.dialogSpeaker != null && i >= 0 && i < this.paragraphAnims.size() && !this.paragraphAnims.get(i).isBlank()) {
            Fights.play(this.dialogSpeaker, this.paragraphAnims.get(i).trim(), false, false);
        }
    }

    public static void paragraph(int scriptId, int choiceId, int index) {
        SprauteRuntime.env().ifPresent(env -> {
            if (env.scriptInstances.get(scriptId) instanceof SprauteScriptInstance s && s.choiceSeq == choiceId) {
                s.playParagraph(index);
            }
        });
    }

    public static void answer(ServerPlayer from, int scriptId, int choiceId, int answer) {
        SprauteRuntime.env()
            .ifPresent(
                env -> {
                    if (env.scriptInstances.get(scriptId) instanceof SprauteScriptInstance s
                        && s.choiceSeq == choiceId
                        && s.lastAnswer == 0
                        && from != null
                        && from.getUUID().equals(s.choicePlayer)) {
                        s.lastAnswer = answer;
                    }
                }
            );
    }

    void overlayShown(String name, String json) {
        this.overlays.put(name, json);
    }

    void overlayHidden(String name) {
        if (name.isEmpty()) {
            this.overlays.clear();
        } else {
            this.overlays.remove(name);
        }
    }

    void cameraOp(JsonObject o) {
        String op = o.get("op").getAsString();
        if (op.equals("hud") || op.equals("bars") || op.equals("lock")) {
            this.cameraNow.put(op, o.toString());
        }
    }

    @Override
    public void catchUp(ServerPlayer p) {
        this.overlays.forEach((name, json) -> SprauteNetwork.sendTo(p, new Packets.OverlayShow(this.getId(), this.deployed.project, name, json)));
        this.cameraNow.values().forEach(json -> SprauteNetwork.sendTo(p, new Packets.CameraOp(this.getId(), json)));
    }

    @Override
    public boolean solo() {
        return this.deployed.program.header != null && this.deployed.program.header.solo;
    }

    public SprauteScriptInstance(ScriptEnvironment env, int id) {
        super(env, id);
        this.deployed = SprauteRuntime.get().deployed(id);
        if (this.deployed == null) {
            throw new IllegalStateException("Spraute script #" + id + " is not deployed");
        } else {
            Ast.Program program = this.deployed.program;
            CutsceneClips.reserveCopies(program, this.deployed.project);

            for (Ast.LocationDecl l : program.locations) {
                this.locationDecls.put(l.name, l);
            }

            for (Ast.CharacterDecl c : program.characters) {
                Character<?> ch = this.createCharacter(c, c.name);
                if (ch != null) {
                    this.characters.put(c.name, ch);
                }

                int pool = program.copies.getOrDefault(c.name, 1);
                if (pool > 1 && c.existing != null) {
                    SprauteRuntime.get().report(this.getId(), "Персонаж " + c.name + ": чужого персонажа нельзя размножить через count");
                } else {
                    for (int n = 2; n <= pool; n++) {
                        String copy = Ast.Program.copyName(c.name, n);
                        Character<?> other = this.createCharacter(c, copy);
                        if (other != null) {
                            this.characters.put(copy, other);
                        }
                    }
                }

                this.speechIcons.put(c.name, this.speechIcon(c));
                if (c.color != null) {
                    try {
                        Integer col = Colors.parse(Eval.str(this.eval.eval(c.color)));
                        if (col != null) {
                            this.nameColors.put(c.name, col);
                        }
                    } catch (RuntimeException var14) {
                        SprauteRuntime.get().report(this.getId(), "Персонаж " + c.name + ": " + var14.getMessage());
                    }
                }

                try {
                    if (c.portrait != null) {
                        this.portraits.put(c.name, Images.portrait(this.deployed.project, Eval.str(this.eval.eval(c.portrait))));
                    } else if (c.icon != null && !"head".equals(Eval.str(this.eval.eval(c.icon)))) {
                        this.portraits.put(c.name, Images.portrait(this.deployed.project, Eval.str(this.eval.eval(c.icon))));
                    }
                } catch (RuntimeException var13) {
                }
            }

            for (Ast.VarDecl v : program.vars) {
                try {
                    this.vars.put(v.name, this.eval.eval(v.init));
                } catch (RuntimeException var12) {
                    this.vars.put(v.name, 0.0);
                    SprauteRuntime.get().report(id, "Не удалось вычислить начальное значение " + v.name + ": " + var12.getMessage());
                }
            }

            ScriptPerformerBuilder builder = ScriptPerformerBuilder.builder(this);

            try {
                new Emitter(this).emitProgram(builder, program.body);
            } catch (RuntimeException var11) {
                SprauteRuntime.get().report(id, "Сюжет не собран: " + var11.getMessage());
                builder = ScriptPerformerBuilder.builder(this);
            }

            builder.build(x$0 -> this.registerMainPerformer(x$0));
        }
    }

    @Override
    public int priorityOf(Character<?> c) {
        return this.priorities.getOrDefault(c.getId(), 0);
    }

    boolean controls(Character<?> c) {
        return Characters.controls(this, c);
    }

    List<Entity> copiesOf(String name) {
        List<Entity> out = new ArrayList<>();
        int pool = this.deployed.program.copies.getOrDefault(name, 1);

        for (int n = 1; n <= pool; n++) {
            Character<?> ch = this.characters.get(Ast.Program.copyName(name, n));
            if (ch != null) {
                this.optional(ch).ifPresent(out::add);
            }
        }

        return out;
    }

    private Character<?> createCharacter(Ast.CharacterDecl c, String name) {
        try {
            if (c.existing != null) {
                String ref = Eval.str(this.eval.eval(c.existing));
                Character<?> found = Character.getById(ref);
                if (found == null) {
                    throw new SprauteRuntimeException("Персонаж '" + ref + "' не найден");
                } else {
                    return found;
                }
            } else {
                String type = Eval.str(this.eval.eval(c.type));
                if (type.equals("spraute:npc") || type.equals("spraute:npc_slim")) {
                    this.characterModels.put(name, type.equals("spraute:npc") ? "builtin/humanoid" : "builtin/humanoid_slim");
                    type = "spraute_engine:model";
                } else if (type.startsWith("spraute:")) {
                    EntityLibrary.Type et = EntityLibrary.find(this.deployed.project, type);
                    if (et != null) {
                        this.characterModels.put(name, et.key());
                    } else {
                        ModelLibrary.Model model = ModelLibrary.find(this.deployed.project, type);
                        if (model == null) {
                            throw new SprauteRuntimeException("Сущность «" + type.substring(8) + "» не найдена (папка «Сущности» проекта)");
                        }

                        this.characterModels.put(name, model.key());
                    }

                    type = "spraute_engine:model";
                }

                String display = c.displayName != null ? Eval.str(this.eval.eval(c.displayName)) : c.name;
                Double speed = c.speed != null ? Eval.num(this.eval.eval(c.speed)) : null;
                String skin = c.skin != null ? Eval.str(this.eval.eval(c.skin)) : null;
                String copyMark = name.substring(c.name.length());
                String refId = c.sharedId != null ? Eval.str(this.eval.eval(c.sharedId)) + copyMark : "spraute_" + this.getId() + "_" + name;
                this.priorities.put(refId, c.priority != null ? (int)Eval.num(this.eval.eval(c.priority)) : 0);
                String entityType = type;
                String key = refId + "|" + type + "|" + display + "|" + speed + "|" + skin;
                return SprauteRuntime.get().characterCache.computeIfAbsent(key, k -> {
                    Character.CharacterBuilder<?> b = Character.builder(refId, new ResourceLocation(entityType));
                    b.name(display);
                    if (speed != null) {
                        b.standardMoveSpeed(speed);
                    }

                    if (skin != null) {
                        b.textured(new ResourceLocation(skin));
                    }

                    return b.build();
                });
            }
        } catch (RuntimeException var11) {
            SprauteRuntime.get().report(this.getId(), "Персонаж " + c.name + ": " + var11.getMessage());
            return null;
        }
    }

    private String speechIcon(Ast.CharacterDecl c) {
        try {
            if (c.icon != null) {
                String v = Eval.str(this.eval.eval(c.icon));
                if (!v.equals("head")) {
                    return Images.speechIcon(this.deployed.project, v);
                }
            }

            return this.headIcon(
                c.name,
                c.headScale != null ? Eval.num(this.eval.eval(c.headScale)) : 1.0,
                c.headX != null ? Eval.num(this.eval.eval(c.headX)) : 0.0,
                c.headY != null ? Eval.num(this.eval.eval(c.headY)) : 0.0,
                c.headYaw != null ? Eval.num(this.eval.eval(c.headYaw)) : 0.0,
                c.headBone != null ? Eval.str(this.eval.eval(c.headBone)) : "auto"
            );
        } catch (RuntimeException var3) {
            return "generic_man";
        }
    }

    String headIcon(String character, double scale, double dx, double dy, double yaw, String bone) {
        String b = bone != null && !bone.isBlank() ? (!bone.equals("auto") && !bone.equals("all") ? Images.slug(bone) : bone) : "auto";
        return "spraute/head/s"
            + this.getId()
            + "/"
            + Images.slug(character)
            + "/"
            + Math.max(10L, Math.round(scale * 100.0))
            + "/"
            + Math.round(dx)
            + "/"
            + Math.round(dy)
            + "/"
            + Math.round(yaw)
            + "/"
            + b;
    }

    boolean headSpeaker(String character) {
        return this.speechIcons.getOrDefault(character, "").startsWith("spraute/head/");
    }

    Location location(String name) {
        Ast.LocationDecl d = this.locationDecls.get(name);
        Location base = this.eval.position(d.pos);
        if (d.yaw == null && d.pitch == null && d.dimension == null) {
            return base;
        } else {
            float yaw = d.yaw != null ? (float)Eval.num(this.eval.eval(d.yaw)) : 0.0F;
            float pitch = d.pitch != null ? (float)Eval.num(this.eval.eval(d.pitch)) : 0.0F;
            ResourceKey<Level> dim = d.dimension != null ? Mc.dimension(new ResourceLocation(Eval.str(this.eval.eval(d.dimension)))) : Level.OVERWORLD;
            return new Location(dim, base.x, base.y, base.z, yaw, pitch);
        }
    }

    public SprauteRuntime.Deployed getDeployed() {
        return this.deployed;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        CompoundTag data = new CompoundTag();
        CompoundTag varsTag = new CompoundTag();
        this.vars.forEach((k, v) -> {
            if (v instanceof Double d) {
                varsTag.putDouble(k, d);
            } else if (v instanceof Boolean b) {
                varsTag.putBoolean(k, b);
            } else if (!(v instanceof List) && !(v instanceof Map) && !(v instanceof EntityRef)) {
                varsTag.putString(k, String.valueOf(v));
            } else {
                varsTag.putString(k, Values.toJson(v));
            }
        });
        data.put("vars", varsTag);
        CompoundTag calls = new CompoundTag();
        this.callStacks.forEach((k, d) -> {
            ListTag list = new ListTag();

            for (String s : d) {
                list.add(StringTag.valueOf(s));
            }

            calls.put(k, list);
        });
        data.put("calls", calls);
        data.putInt("answer", this.lastAnswer);
        CompoundTag slots = new CompoundTag();
        this.guiSlots.forEach((k, st) -> slots.put(k, st.save(new CompoundTag())));
        data.put("slots", slots);
        tag.put("spraute", data);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
        CompoundTag data = tag.getCompound("spraute");
        CompoundTag varsTag = data.getCompound("vars");

        for (String k : varsTag.getAllKeys()) {
            Tag t = varsTag.get(k);
            if (t != null) {
                switch (t.getId()) {
                    case 1:
                        this.vars.put(k, varsTag.getBoolean(k));
                        break;
                    case 6:
                        this.vars.put(k, varsTag.getDouble(k));
                        break;
                    default:
                        String s = varsTag.getString(k);
                        this.vars.put(k, s.startsWith("§json:") ? Values.fromJson(s) : s);
                }
            }
        }

        this.lastAnswer = data.getInt("answer");
        CompoundTag slots = data.getCompound("slots");
        this.guiSlots.clear();

        for (String kx : slots.getAllKeys()) {
            this.guiSlots.put(kx, ItemStack.of(slots.getCompound(kx)));
        }

        CompoundTag calls = data.getCompound("calls");
        this.callStacks.clear();

        for (String kx : calls.getAllKeys()) {
            Deque<String> d = new ArrayDeque<>();
            ListTag list = calls.getList(kx, 8);

            for (int i = 0; i < list.size(); i++) {
                d.addLast(list.getString(i));
            }

            this.callStacks.put(kx, d);
        }
    }

    public static final class Handler {
        public final String title;
        public int count;

        Handler(String title) {
            this.title = title;
        }
    }
}
