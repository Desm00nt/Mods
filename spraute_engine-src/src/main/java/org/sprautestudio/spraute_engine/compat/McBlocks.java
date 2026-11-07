package org.sprautestudio.spraute_engine.compat;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.PressurePlateBlock.Sensitivity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class McBlocks {
    private McBlocks() {
    }

    public static Properties properties(String material) {
        Properties p = Properties.of();

        return switch (material) {
            case "wood" -> p.mapColor(MapColor.WOOD).ignitedByLava();
            case "metal" -> p.mapColor(MapColor.METAL);
            case "dirt" -> p.mapColor(MapColor.DIRT);
            case "sand" -> p.mapColor(MapColor.SAND);
            case "plant" -> p.mapColor(MapColor.PLANT).pushReaction(PushReaction.DESTROY);
            case "glass" -> p.mapColor(MapColor.NONE);
            case "wool" -> p.mapColor(MapColor.WOOL).ignitedByLava();
            case "ice" -> p.mapColor(MapColor.ICE);
            default -> p.mapColor(MapColor.STONE);
        };
    }

    public abstract static class Button extends ButtonBlock {
        protected Button(Properties properties) {
            super(properties, BlockSetType.STONE, 20, false);
        }
    }

    public abstract static class Gate extends FenceGateBlock {
        protected Gate(Properties properties) {
            super(properties, WoodType.OAK);
        }
    }

    public abstract static class Plate extends PressurePlateBlock {
        protected Plate(Properties properties) {
            super(Sensitivity.EVERYTHING, properties, BlockSetType.STONE);
        }
    }

    public abstract static class Trapdoor extends TrapDoorBlock {
        protected Trapdoor(Properties properties) {
            super(properties, BlockSetType.OAK);
        }
    }
}
