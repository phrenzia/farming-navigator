package com.farmingnavigator.guided;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Bind sidebar actions to the displayed checklist revision; validate again on the client thread. */
public final class ChecklistCommandGuard
{
    private ChecklistCommandGuard() { }
    public static <C> Consumer<C> bind(long revision, BiConsumer<C, Long> receiver)
    {
        return command -> receiver.accept(command, revision);
    }
    public static boolean dispatch(long expected, long current, Runnable action)
    {
        if (expected < 0 || expected != current) { return false; }
        action.run(); return true;
    }
}
