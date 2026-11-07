package net.minecraftforge.event;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
public class RegisterCommandsEvent extends net.minecraftforge.eventbus.api.Event {
    public CommandDispatcher<CommandSourceStack> getDispatcher() { throw new RuntimeException(); }
    public Commands.CommandSelection getCommandSelection() { throw new RuntimeException(); }
    public CommandBuildContext getBuildContext() { throw new RuntimeException(); }
}
