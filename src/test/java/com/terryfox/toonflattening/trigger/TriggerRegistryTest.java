package com.terryfox.toonflattening.trigger;

import com.terryfox.toonflattening.api.FlattenRequest;
import com.terryfox.toonflattening.api.IFlattenTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TriggerRegistryTest {

    private TriggerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TriggerRegistry();
    }

    @Test
    void register_addsTriggerToList() {
        IFlattenTrigger trigger = createMockTrigger("test", "trigger1");

        registry.register(trigger);

        ServerPlayer player = mock(ServerPlayer.class);
        when(trigger.shouldTriggerFlatten(player)).thenReturn(new FlattenRequest(4.0f, null));

        FlattenRequest result = registry.checkTriggers(player);
        assertNotNull(result);
        verify(trigger, times(1)).shouldTriggerFlatten(player);
    }

    @Test
    void checkTriggers_returnsFirstNonNullResult() {
        ServerPlayer player = mock(ServerPlayer.class);

        IFlattenTrigger trigger1 = createMockTrigger("test", "trigger1");
        IFlattenTrigger trigger2 = createMockTrigger("test", "trigger2");
        IFlattenTrigger trigger3 = createMockTrigger("test", "trigger3");

        when(trigger1.shouldTriggerFlatten(player)).thenReturn(null);
        when(trigger2.shouldTriggerFlatten(player)).thenReturn(new FlattenRequest(6.0f, null));
        when(trigger3.shouldTriggerFlatten(player)).thenReturn(new FlattenRequest(8.0f, null));

        registry.register(trigger1);
        registry.register(trigger2);
        registry.register(trigger3);

        FlattenRequest result = registry.checkTriggers(player);

        assertNotNull(result);
        assertEquals(6.0f, result.damage(), 0.001f);

        // trigger3 should not be called since trigger2 returned non-null
        verify(trigger1, times(1)).shouldTriggerFlatten(player);
        verify(trigger2, times(1)).shouldTriggerFlatten(player);
        verify(trigger3, never()).shouldTriggerFlatten(player);
    }

    @Test
    void checkTriggers_returnsNullWhenAllTriggersReturnNull() {
        ServerPlayer player = mock(ServerPlayer.class);

        IFlattenTrigger trigger1 = createMockTrigger("test", "trigger1");
        IFlattenTrigger trigger2 = createMockTrigger("test", "trigger2");

        when(trigger1.shouldTriggerFlatten(player)).thenReturn(null);
        when(trigger2.shouldTriggerFlatten(player)).thenReturn(null);

        registry.register(trigger1);
        registry.register(trigger2);

        FlattenRequest result = registry.checkTriggers(player);

        assertNull(result);
    }

    @Test
    void checkTriggers_returnsNullWhenNoTriggers() {
        ServerPlayer player = mock(ServerPlayer.class);

        FlattenRequest result = registry.checkTriggers(player);

        assertNull(result);
    }

    @Test
    void unregister_removesTriggerById() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("test", "trigger1");
        IFlattenTrigger trigger = createMockTrigger("test", "trigger1");

        registry.register(trigger);
        registry.unregister(id);

        ServerPlayer player = mock(ServerPlayer.class);
        FlattenRequest result = registry.checkTriggers(player);

        assertNull(result);
        verify(trigger, never()).shouldTriggerFlatten(any());
    }

    @Test
    void unregister_handlesNonExistentId() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("test", "nonexistent");

        // Should not throw exception
        assertDoesNotThrow(() -> registry.unregister(id));
    }

    @Test
    void register_allowsMultipleTriggers() {
        IFlattenTrigger trigger1 = createMockTrigger("mod1", "trigger");
        IFlattenTrigger trigger2 = createMockTrigger("mod2", "trigger");

        registry.register(trigger1);
        registry.register(trigger2);

        ServerPlayer player = mock(ServerPlayer.class);
        when(trigger1.shouldTriggerFlatten(player)).thenReturn(null);
        when(trigger2.shouldTriggerFlatten(player)).thenReturn(new FlattenRequest(5.0f, null));

        FlattenRequest result = registry.checkTriggers(player);

        assertNotNull(result);
        assertEquals(5.0f, result.damage(), 0.001f);
    }

    @Test
    void triggerExecutionOrder_preservesRegistrationOrder() {
        ServerPlayer player = mock(ServerPlayer.class);

        IFlattenTrigger trigger1 = createMockTrigger("test", "first");
        IFlattenTrigger trigger2 = createMockTrigger("test", "second");

        when(trigger1.shouldTriggerFlatten(player)).thenReturn(new FlattenRequest(1.0f, null));
        when(trigger2.shouldTriggerFlatten(player)).thenReturn(new FlattenRequest(2.0f, null));

        registry.register(trigger1);
        registry.register(trigger2);

        FlattenRequest result = registry.checkTriggers(player);

        // Should get first trigger's result
        assertEquals(1.0f, result.damage(), 0.001f);
    }

    private IFlattenTrigger createMockTrigger(String namespace, String path) {
        IFlattenTrigger trigger = Mockito.mock(IFlattenTrigger.class);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        when(trigger.getId()).thenReturn(id);
        return trigger;
    }
}
