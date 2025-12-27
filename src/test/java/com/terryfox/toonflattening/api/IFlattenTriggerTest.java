package com.terryfox.toonflattening.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class IFlattenTriggerTest {

    @Test
    void flattenRequest_storesAllFields() {
        Entity source = Mockito.mock(Entity.class);

        FlattenRequest request = new FlattenRequest(6.0f, source);

        assertEquals(6.0f, request.damage(), 0.001f);
        assertSame(source, request.source());
    }

    @Test
    void flattenRequest_allowsNullSource() {
        FlattenRequest request = new FlattenRequest(4.0f, null);

        assertEquals(4.0f, request.damage(), 0.001f);
        assertNull(request.source());
    }

    @Test
    void flattenRequest_equality() {
        Entity source = Mockito.mock(Entity.class);

        FlattenRequest request1 = new FlattenRequest(4.0f, source);
        FlattenRequest request2 = new FlattenRequest(4.0f, source);
        FlattenRequest request3 = new FlattenRequest(5.0f, source);

        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void iFlattenTrigger_contractImplementation() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("testmod", "test_trigger");
        ServerPlayer player = Mockito.mock(ServerPlayer.class);
        Entity source = Mockito.mock(Entity.class);

        IFlattenTrigger trigger = new IFlattenTrigger() {
            @Override
            public FlattenRequest shouldTriggerFlatten(ServerPlayer player) {
                return new FlattenRequest(10.0f, source);
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }
        };

        FlattenRequest result = trigger.shouldTriggerFlatten(player);
        assertNotNull(result);
        assertEquals(10.0f, result.damage(), 0.001f);
        assertSame(source, result.source());

        assertEquals(id, trigger.getId());
    }

    @Test
    void iFlattenTrigger_canReturnNull() {
        IFlattenTrigger trigger = new IFlattenTrigger() {
            @Override
            public FlattenRequest shouldTriggerFlatten(ServerPlayer player) {
                return null; // No-op
            }

            @Override
            public ResourceLocation getId() {
                return ResourceLocation.fromNamespaceAndPath("testmod", "noop");
            }
        };

        ServerPlayer player = Mockito.mock(ServerPlayer.class);
        assertNull(trigger.shouldTriggerFlatten(player));
    }

    @Test
    void iFlattenTrigger_uniqueIds() {
        ResourceLocation id1 = ResourceLocation.fromNamespaceAndPath("mod1", "trigger");
        ResourceLocation id2 = ResourceLocation.fromNamespaceAndPath("mod2", "trigger");

        IFlattenTrigger trigger1 = createDummyTrigger(id1);
        IFlattenTrigger trigger2 = createDummyTrigger(id2);

        assertNotEquals(trigger1.getId(), trigger2.getId());
    }

    private IFlattenTrigger createDummyTrigger(ResourceLocation id) {
        return new IFlattenTrigger() {
            @Override
            public FlattenRequest shouldTriggerFlatten(ServerPlayer player) {
                return null;
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }
        };
    }
}
