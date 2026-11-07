package org.sprautestudio.spraute_engine.addon;

public interface SprauteAddon {
    void register(AddonContext var1);

    default void registerClient(AddonClient client) {
    }
}
