package com.mojang.datafixers.util;
public class Pair<F, S> {
    public Pair(F first, S second) { }
    public F getFirst() { throw new RuntimeException(); }
    public S getSecond() { throw new RuntimeException(); }
    public Pair<S, F> swap() { throw new RuntimeException(); }
    public static <F, S> Pair<F, S> of(F first, S second) { throw new RuntimeException(); }
    public <F2> Pair<F2, S> mapFirst(java.util.function.Function<? super F, ? extends F2> function) { throw new RuntimeException(); }
    public <S2> Pair<F, S2> mapSecond(java.util.function.Function<? super S, ? extends S2> function) { throw new RuntimeException(); }
}
