package org.openjdk.nashorn.api.scripting;
import javax.script.ScriptEngine;
public final class NashornScriptEngineFactory implements javax.script.ScriptEngineFactory {
    public NashornScriptEngineFactory() { }
    public String getEngineName() { throw new RuntimeException(); }
    public String getEngineVersion() { throw new RuntimeException(); }
    public java.util.List<String> getExtensions() { throw new RuntimeException(); }
    public String getLanguageName() { throw new RuntimeException(); }
    public String getLanguageVersion() { throw new RuntimeException(); }
    public String getMethodCallSyntax(String obj, String m, String... args) { throw new RuntimeException(); }
    public java.util.List<String> getMimeTypes() { throw new RuntimeException(); }
    public java.util.List<String> getNames() { throw new RuntimeException(); }
    public String getOutputStatement(String toDisplay) { throw new RuntimeException(); }
    public Object getParameter(String key) { throw new RuntimeException(); }
    public String getProgram(String... statements) { throw new RuntimeException(); }
    public ScriptEngine getScriptEngine() { throw new RuntimeException(); }
    public ScriptEngine getScriptEngine(String... args) { throw new RuntimeException(); }
    public ScriptEngine getScriptEngine(ClassLoader appLoader) { throw new RuntimeException(); }
    public ScriptEngine getScriptEngine(ClassFilter classFilter) { throw new RuntimeException(); }
    public ScriptEngine getScriptEngine(String[] args, ClassLoader appLoader) { throw new RuntimeException(); }
    public ScriptEngine getScriptEngine(String[] args, ClassLoader appLoader, ClassFilter classFilter) { throw new RuntimeException(); }
}
