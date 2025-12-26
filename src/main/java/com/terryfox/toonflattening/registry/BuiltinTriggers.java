package com.terryfox.toonflattening.registry;

import com.terryfox.toonflattening.trigger.AnvilFlattenTrigger;

/**
 * Registration for builtin triggers.
 */
public class BuiltinTriggers {

    public static void register() {
        FlattenTriggerRegistry.register(new AnvilFlattenTrigger());
    }
}
