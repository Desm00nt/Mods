package net.minecraftforge.client.settings;
public enum KeyConflictContext implements IKeyConflictContext {
    UNIVERSAL, GUI, IN_GAME;
    public boolean isActive() { throw new RuntimeException(); }
    public boolean conflicts(IKeyConflictContext other) { throw new RuntimeException(); }
}
