package net.minecraftforge.fml;
import java.util.*;
import net.minecraftforge.forgespi.language.IModFileInfo;
public class ModList {
    public static ModList get() { throw new RuntimeException(); }
    public List<IModFileInfo> getModFiles() { throw new RuntimeException(); }
    public IModFileInfo getModFileById(String modid) { throw new RuntimeException(); }
    public boolean isLoaded(String modTarget) { throw new RuntimeException(); }
    public Optional<? extends ModContainer> getModContainerById(String modId) { throw new RuntimeException(); }
}
