package net.minecraftforge.client.event;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandBuildContext;
public class RegisterClientCommandsEvent extends net.minecraftforge.eventbus.api.Event {
    public CommandDispatcher<CommandSourceStack> getDispatcher() { throw new RuntimeException(); }
    public CommandBuildContext getBuildContext() { throw new RuntimeException(); }
}
