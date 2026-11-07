package org.sprautestudio.spraute_engine.runtime;

import java.util.function.Supplier;
import org.sprautestudio.spraute_engine.core.script.ScriptPerformer;
import org.sprautestudio.spraute_engine.core.script.ScriptPerformerAction;

public class SpeechNameAction extends ScriptPerformerAction {
    private final int slot;
    private final Supplier<String> name;
    private final Supplier<Integer> color;
    private final String icon;

    public SpeechNameAction(ScriptPerformer performer, int slot, Supplier<String> name, Supplier<Integer> color, String icon) {
        super(performer);
        this.slot = slot;
        this.name = name;
        this.color = color;
        this.icon = icon;
    }

    @Override
    public boolean update() {
        this.getPerformer().speech().setEntityName(this.slot, this.name.get(), this.color.get(), this.icon);
        return true;
    }
}
