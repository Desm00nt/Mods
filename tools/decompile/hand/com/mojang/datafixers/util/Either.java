package com.mojang.datafixers.util;
import java.util.Optional;
import java.util.function.*;
public abstract class Either<L, R> {
    public abstract <C, D> Either<C, D> mapBoth(Function<? super L, ? extends C> f1, Function<? super R, ? extends D> f2);
    public abstract <T> T map(Function<? super L, ? extends T> l, Function<? super R, ? extends T> r);
    public abstract Optional<L> left();
    public abstract Optional<R> right();
    public static <L, R> Either<L, R> left(L value) { throw new RuntimeException(); }
    public static <L, R> Either<L, R> right(R value) { throw new RuntimeException(); }
    public <T> Either<T, R> mapLeft(Function<? super L, ? extends T> l) { throw new RuntimeException(); }
    public <T> Either<L, T> mapRight(Function<? super R, ? extends T> l) { throw new RuntimeException(); }
    public Either<L, R> ifLeft(Consumer<? super L> consumer) { throw new RuntimeException(); }
    public Either<L, R> ifRight(Consumer<? super R> consumer) { throw new RuntimeException(); }
    public L orThrow() { throw new RuntimeException(); }
}
