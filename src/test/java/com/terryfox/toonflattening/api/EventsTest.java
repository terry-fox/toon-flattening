package com.terryfox.toonflattening.api;

import com.terryfox.toonflattening.api.event.PreFlattenEvent;
import com.terryfox.toonflattening.api.event.PostFlattenEvent;
import com.terryfox.toonflattening.api.event.PreReformEvent;
import com.terryfox.toonflattening.api.event.PostReformEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class EventsTest {

    @Test
    void preFlattenEvent_storesAllFields() {
        ServerPlayer player = Mockito.mock(ServerPlayer.class);
        Entity source = Mockito.mock(Entity.class);

        PreFlattenEvent event = new PreFlattenEvent(player, 4.0f, source);

        assertSame(player, event.getPlayer());
        assertEquals(4.0f, event.getDamage(), 0.001f);
        assertSame(source, event.getSource());
    }

    @Test
    void preFlattenEvent_allowsNullSource() {
        ServerPlayer player = Mockito.mock(ServerPlayer.class);

        PreFlattenEvent event = new PreFlattenEvent(player, 2.0f, null);

        assertSame(player, event.getPlayer());
        assertEquals(2.0f, event.getDamage(), 0.001f);
        assertNull(event.getSource());
    }

    @Test
    void preFlattenEvent_isCancellable() {
        ServerPlayer player = Mockito.mock(ServerPlayer.class);
        PreFlattenEvent event = new PreFlattenEvent(player, 4.0f, null);

        assertFalse(event.isCanceled());

        event.setCanceled(true);
        assertTrue(event.isCanceled());
    }

    @Test
    void postFlattenEvent_storesAllFields() {
        ServerPlayer player = Mockito.mock(ServerPlayer.class);

        PostFlattenEvent event = new PostFlattenEvent(player, 4.0f, 2.6f);

        assertSame(player, event.getPlayer());
        assertEquals(4.0f, event.getAppliedDamage(), 0.001f);
        assertEquals(2.6f, event.getSpreadMultiplier(), 0.001f);
    }

    @Test
    void postFlattenEvent_isNotCancellable() {
        ServerPlayer player = Mockito.mock(ServerPlayer.class);
        PostFlattenEvent event = new PostFlattenEvent(player, 4.0f, 1.8f);

        // PostFlattenEvent should not be cancellable - this is informational only
        assertFalse(event instanceof net.neoforged.bus.api.ICancellableEvent);
    }

    @Test
    void preReformEvent_storesPlayer() {
        ServerPlayer player = Mockito.mock(ServerPlayer.class);

        PreReformEvent event = new PreReformEvent(player);

        assertSame(player, event.getPlayer());
    }

    @Test
    void preReformEvent_isCancellable() {
        ServerPlayer player = Mockito.mock(ServerPlayer.class);
        PreReformEvent event = new PreReformEvent(player);

        assertFalse(event.isCanceled());

        event.setCanceled(true);
        assertTrue(event.isCanceled());
    }

    @Test
    void postReformEvent_storesPlayer() {
        ServerPlayer player = Mockito.mock(ServerPlayer.class);

        PostReformEvent event = new PostReformEvent(player);

        assertSame(player, event.getPlayer());
    }

    @Test
    void postReformEvent_isNotCancellable() {
        ServerPlayer player = Mockito.mock(ServerPlayer.class);
        PostReformEvent event = new PostReformEvent(player);

        // PostReformEvent should not be cancellable - this is informational only
        assertFalse(event instanceof net.neoforged.bus.api.ICancellableEvent);
    }
}
