package net.minecraftforge.api.distmarker;
public enum Dist { CLIENT, DEDICATED_SERVER; public boolean isClient() { throw new RuntimeException(); } public boolean isDedicatedServer() { throw new RuntimeException(); } }
