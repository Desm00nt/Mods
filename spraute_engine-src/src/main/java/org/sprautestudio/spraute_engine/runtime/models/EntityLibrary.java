package org.sprautestudio.spraute_engine.runtime.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.core.anim.Animation;
import org.sprautestudio.spraute_engine.core.model.SpGeoModel;
import org.sprautestudio.spraute_engine.runtime.Anims;
import org.sprautestudio.spraute_engine.runtime.Images;

public final class EntityLibrary {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String BUILTIN = "builtin/humanoid";
    public static final String BUILTIN_SLIM = "builtin/humanoid_slim";
    public static final String BUILTIN_MODEL = "spraute_engine:entity/classic";
    public static final String BUILTIN_TEXTURE = "spraute_engine:textures/entity/classic.png";
    private static final Map<String, EntityLibrary.Type> TYPES = new LinkedHashMap<>();
    private static final ResourceLocation MISSING = new ResourceLocation("minecraft", "textures/misc/unknown_pack.png");
    private static final Map<String, EntityLibrary.KnownLook> LOOKS = new ConcurrentHashMap<>();
    private static final long LOOK_FOR_MS = 1000L;

    private EntityLibrary() {
    }

    static void scan(List<Path> projects) {
        Map<String, EntityLibrary.Type> found = new LinkedHashMap<>();

        for (Path pd : projects) {
            Path ed = pd.resolve("entities");
            if (Files.isDirectory(ed)) {
                String project = ModelLibrary.projectNameOf(pd);

                try (Stream<Path> files = Files.list(ed)) {
                    for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".sent")).sorted()::iterator) {
                        String name = f.getFileName().toString();
                        name = name.substring(0, name.length() - 5);

                        try {
                            found.put(
                                project + "/" + name,
                                new EntityLibrary.Type(project, name, f, EntityLibrary.Def.fromJson(Files.readString(f, StandardCharsets.UTF_8)))
                            );
                        } catch (IOException var11) {
                        }
                    }
                } catch (IOException var14) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot scan entity types", (Throwable)var14);
                }
            }
        }

        TYPES.clear();
        TYPES.putAll(found);
        LOOKS.clear();
    }

    public static Collection<EntityLibrary.Type> all() {
        return Collections.unmodifiableCollection(TYPES.values());
    }

    public static EntityLibrary.Type byKey(String key) {
        return key == null ? null : TYPES.get(key);
    }

    public static EntityLibrary.Type find(String project, String name) {
        if (name == null) {
            return null;
        } else {
            String n = name.startsWith("spraute:") ? name.substring(8) : name;
            EntityLibrary.Type t = TYPES.get(n);
            if (t == null && project != null) {
                t = TYPES.get(project + "/" + n);
            }

            if (t != null) {
                return t;
            } else {
                for (EntityLibrary.Type o : TYPES.values()) {
                    if (o.name().equalsIgnoreCase(n)) {
                        return o;
                    }
                }

                return null;
            }
        }
    }

    public static EntityLibrary.Look look(String key) {
        String k = key == null ? "" : key;
        long now = System.currentTimeMillis();
        EntityLibrary.KnownLook known = LOOKS.get(k);
        if (known != null && now - known.at < 1000L) {
            return known.look;
        } else {
            EntityLibrary.Look l = findLook(key);
            LOOKS.put(k, new EntityLibrary.KnownLook(l, now));
            return l;
        }
    }

    private static EntityLibrary.Look findLook(String key) {
        if (key != null && key.equals("builtin/humanoid_slim")) {
            key = "builtin/humanoid";
        }

        if (key != null && !key.isEmpty() && !key.equals("builtin/humanoid")) {
            EntityLibrary.Type t = byKey(key);
            if (t != null) {
                EntityLibrary.Def d = t.def();
                ModelLibrary.Model m = null;
                SpGeoModel geo;
                if (d.model.startsWith("st:")) {
                    geo = ModelLibrary.builtinGeometry("classic");
                } else {
                    m = ModelLibrary.find(t.project(), d.model);
                    geo = m != null ? m.geometry() : null;
                }

                ResourceLocation tex;
                if (d.texture.startsWith("st:")) {
                    tex = ResourceLocation.tryParse(d.texture.endsWith("/humanoid.png") ? "spraute_engine:textures/entity/classic.png" : d.texture.substring(3));
                } else if (d.texture.startsWith("image:")) {
                    tex = Images.texture(t.project(), d.texture.substring(6));
                } else {
                    tex = m != null && m.texture() != null ? m.textureId() : null;
                }

                return geo == null ? null : new EntityLibrary.Look(geo, tex != null ? tex : MISSING, m, d);
            } else {
                ModelLibrary.Model mx = ModelLibrary.byKey(key);
                SpGeoModel geox = mx != null ? mx.geometry() : null;
                return geox == null ? null : new EntityLibrary.Look(geox, mx.texture() != null ? mx.textureId() : MISSING, mx, null);
            }
        } else {
            SpGeoModel geox = ModelLibrary.builtinGeometry("classic");
            return geox == null ? null : new EntityLibrary.Look(geox, ModelLibrary.builtinTexture(), null, null);
        }
    }

    public static List<String> builtinModels() {
        return List.of("spraute_engine:entity/classic");
    }

    public static final class Def {
        public String model = "st:spraute_engine:entity/classic";
        public String texture = "st:spraute_engine:textures/entity/classic.png";
        public String idle = "";
        public String walk = "";
        public String run = "";
        public float scale = 1.0F;
        public double speed = 1.0;
        public String itemBone = "";
        public String offhandBone = "";
        public float itemTurnX = 0.0F;
        public float itemTurnY = 0.0F;
        public float itemTurnZ = 0.0F;
        public float itemX = 0.0F;
        public float itemY = 0.0F;
        public float itemZ = 0.0F;
        public float itemScale = 1.0F;
        public float health = 0.0F;
        public String hitbox = "";
        public float width = 0.0F;
        public float height = 0.0F;
        public List<EntityLibrary.Def.Part> parts = new ArrayList<>();
        public float modelY = 0.0F;

        public String toJson() {
            return EntityLibrary.GSON.toJson(this);
        }

        public static EntityLibrary.Def fromJson(String json) {
            EntityLibrary.Def d = null;

            try {
                d = EntityLibrary.GSON.fromJson(json, EntityLibrary.Def.class);
            } catch (Exception var4) {
            }

            if (d == null) {
                d = new EntityLibrary.Def();
            }

            if (d.model == null) {
                d.model = "";
            }

            if (d.texture == null) {
                d.texture = "model";
            }

            if (d.idle == null) {
                d.idle = "";
            }

            if (d.walk == null) {
                d.walk = "";
            }

            if (d.run == null) {
                d.run = "";
            }

            if (d.itemBone == null) {
                d.itemBone = "";
            }

            if (d.hitbox == null) {
                d.hitbox = "";
            }

            if (d.offhandBone == null) {
                d.offhandBone = "";
            }

            if (d.scale <= 0.0F) {
                d.scale = 1.0F;
            }

            if (d.parts == null) {
                d.parts = new ArrayList<>();
            }

            d.parts.removeIf(Objects::isNull);

            for (EntityLibrary.Def.Part p : d.parts) {
                if (p.name == null || p.name.isBlank()) {
                    p.name = "часть";
                }

                if (p.bone == null) {
                    p.bone = "";
                }

                p.depth = Math.max(0.0F, p.depth);
                p.width = Math.max(0.05F, p.width);
                p.height = Math.max(0.05F, p.height);
            }

            return d;
        }

        public static final class Part {
            public String name = "часть";
            public float x = 0.0F;
            public float y = 1.0F;
            public float z = 0.0F;
            public float width = 0.5F;
            public float height = 0.5F;
            public float depth = 0.0F;
            public float rx = 0.0F;
            public float ry = 0.0F;
            public float rz = 0.0F;
            public String bone = "";
            public float damage = 1.0F;
            public boolean enabled = true;
        }
    }

    private record KnownLook(EntityLibrary.Look look, long at) {
    }

    public record Look(SpGeoModel geometry, ResourceLocation texture, ModelLibrary.Model model, EntityLibrary.Def def) {
        public Optional<Animation> animation(String name) {
            if (name != null && !name.isBlank()) {
                if (name.contains(":") || name.contains("/")) {
                    Optional<Animation> a = Anims.resolve(name);
                    if (a.isPresent()) {
                        return a;
                    }
                }

                Optional<Animation> own = this.model != null ? ModelLibrary.animation(this.model, name) : Anims.humanoid(name);
                return own.isPresent() ? own : ModelLibrary.projectAnimation(this.model != null ? this.model.project() : null, name);
            } else {
                return Optional.empty();
            }
        }

        public float scale() {
            return this.def != null ? this.def.scale : 1.0F;
        }
    }

    public record Type(String project, String name, Path file, EntityLibrary.Def def) {
        public String key() {
            return this.project + "/" + this.name;
        }
    }
}
