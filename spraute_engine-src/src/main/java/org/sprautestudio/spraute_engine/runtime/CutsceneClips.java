package org.sprautestudio.spraute_engine.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sprautestudio.spraute_engine.core.script.ScriptPerformer;
import org.sprautestudio.spraute_engine.lang.Ast;

final class CutsceneClips {
    private final SprauteScriptInstance script;
    private final ScriptPerformer by;
    private final List<CutsceneClips.Entry> entries = new ArrayList<>();

    CutsceneClips(SprauteScriptInstance script, ScriptPerformer by) {
        this.script = script;
        this.by = by;
    }

    void add(String key, ScriptPerformer performer, boolean always) {
        this.entries.add(new CutsceneClips.Entry(key, performer, always));
    }

    private void start(CutsceneClips.Entry e) {
        if (!e.started) {
            e.started = true;
            if (e.performer != null) {
                this.script.startParallel(e.key, e.performer, this.by);
            }
        }
    }

    void reached(String index) {
        try {
            int i = Integer.parseInt(index);
            if (i >= 0 && i < this.entries.size()) {
                this.start(this.entries.get(i));
            }
        } catch (NumberFormatException var3) {
        }
    }

    void skipped() {
        for (CutsceneClips.Entry e : this.entries) {
            if (e.always) {
                this.start(e);
            } else if (e.started) {
                this.script.stopParallel(e.key);
            } else {
                e.started = true;
            }
        }
    }

    void ended() {
        for (CutsceneClips.Entry e : this.entries) {
            if (e.always) {
                this.start(e);
            }
        }
    }

    static void reserveCopies(Ast.Program program, String project) {
        Map<String, String> files = SprauteRuntime.get().allGuis().get(project);
        if (files != null) {
        }
    }

    private static final class Entry {
        final String key;
        final ScriptPerformer performer;
        final boolean always;
        boolean started;

        Entry(String key, ScriptPerformer performer, boolean always) {
            this.key = key;
            this.performer = performer;
            this.always = always;
        }
    }
}
