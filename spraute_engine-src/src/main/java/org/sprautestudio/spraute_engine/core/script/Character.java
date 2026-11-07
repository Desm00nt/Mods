package org.sprautestudio.spraute_engine.core.script;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.sprautestudio.spraute_engine.compat.Regs;

public final class Character<T extends Entity> {
    private static final Map<String, Character<?>> BY_ID = new HashMap<>();
    private final String id;
    private final EntityType<T> type;
    private final String name;
    private final Double speed;
    private final ResourceLocation texture;

    private Character(String id, EntityType<T> type, String name, Double speed, ResourceLocation texture) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.speed = speed;
        this.texture = texture;
    }

    public static Character<?> getById(String id) {
        return BY_ID.get(id);
    }

    public static Character.CharacterBuilder<?> builder(String id, ResourceLocation type) {
        if (!Regs.ENTITY_TYPE.containsKey(type)) {
            throw new IllegalArgumentException("Сущность " + type + " не найдена");
        } else {
            return new Character.CharacterBuilder<>(id, Regs.ENTITY_TYPE.get(type));
        }
    }

    public String getId() {
        return this.id;
    }

    public EntityType<T> getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public Double speed() {
        return this.speed;
    }

    public ResourceLocation texture() {
        return this.texture;
    }

    public static final class CharacterBuilder<T extends Entity> {
        private final String id;
        private final EntityType<T> type;
        private String name;
        private Double speed;
        private ResourceLocation texture;

        private CharacterBuilder(String id, EntityType<T> type) {
            this.id = id;
            this.type = type;
            this.name = id;
        }

        public Character.CharacterBuilder<T> name(String n) {
            this.name = n;
            return this;
        }

        public Character.CharacterBuilder<T> standardMoveSpeed(double s) {
            this.speed = s;
            return this;
        }

        public Character.CharacterBuilder<T> textured(ResourceLocation t) {
            this.texture = t;
            return this;
        }

        public Character<T> build() {
            Character<T> c = new Character<>(this.id, this.type, this.name, this.speed, this.texture);
            Character.BY_ID.put(this.id, c);
            return c;
        }
    }
}
