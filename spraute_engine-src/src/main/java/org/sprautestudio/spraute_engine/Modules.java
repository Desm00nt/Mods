package org.sprautestudio.spraute_engine;

import org.sprautestudio.spraute_engine.build.BlockMoves;
import org.sprautestudio.spraute_engine.core.script.Behaviours;
import org.sprautestudio.spraute_engine.custom.CustomBlocks;
import org.sprautestudio.spraute_engine.custom.CustomMobs;
import org.sprautestudio.spraute_engine.custom.Dungeons;
import org.sprautestudio.spraute_engine.engine.BodyRules;
import org.sprautestudio.spraute_engine.fight.Fights;
import org.sprautestudio.spraute_engine.fx.Casts;
import org.sprautestudio.spraute_engine.hitbox.Hitboxes;
import org.sprautestudio.spraute_engine.nav.Navigation;
import org.sprautestudio.spraute_engine.path.Walkers;
import org.sprautestudio.spraute_engine.recipe.RecipeApi;
import org.sprautestudio.spraute_engine.recipe.RecipeOverrides;
import org.sprautestudio.spraute_engine.runtime.AttackBoxes;
import org.sprautestudio.spraute_engine.runtime.BossMoves;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.Mechanics;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;
import org.sprautestudio.spraute_engine.runtime.PlayerSlots;
import org.sprautestudio.spraute_engine.runtime.WhenReady;
import org.sprautestudio.spraute_engine.runtime.WorldPanels;

final class Modules {
    private Modules() {
    }

    static void common() {
        Behaviours.registerHooks();
        Fights.registerHooks();
        Navigation.registerHooks();
        BodyRules.registerHooks();
        Casts.registerHooks();
        Hitboxes.registerHooks();
        Walkers.registerHooks();
        CustomBlocks.registerHooks();
        RecipeOverrides.registerHooks();
        RecipeApi.registerHooks();
        AttackBoxes.registerHooks();
        BossMoves.registerHooks();
        GameEvents.registerHooks();
        Mechanics.registerHooks();
        PlayerRules.registerHooks();
        PlayerSlots.registerHooks();
        WorldPanels.registerHooks();
        BlockMoves.registerHooks();
        Dungeons.registerHooks();
        CustomMobs.registerHooks();
        WhenReady.registerHooks();
    }
}
