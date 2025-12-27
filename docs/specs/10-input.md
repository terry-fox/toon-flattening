# Input Module Specification

**Package:** `com.terryfox.toonflattening.input`
**Side:** Client

## Purpose

Keybinding registration and reform input handling.

## Files

```
input/
├── KeybindRegistry.java
└── ReformInputHandler.java
```

## KeybindRegistry

Registers the reform keybinding.

```java
public class KeybindRegistry {
    public static final String CATEGORY = "key.categories.toonflattening";

    public static KeyMapping REFORM_KEY;

    public static void register(RegisterKeyMappingsEvent event) {
        REFORM_KEY = new KeyMapping(
            "key.toonflattening.reform",         // Translation key
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE,                 // Default: SPACE
            CATEGORY
        );

        event.register(REFORM_KEY);
    }
}
```

## ReformInputHandler

Detects keybind press and sends reform request.

```java
public class ReformInputHandler {

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;  // Ignore when GUI open
        if (mc.player == null) return;

        if (KeybindRegistry.REFORM_KEY.consumeClick()) {
            if (FlattenAPI.isFlattened(mc.player)) {
                // Send reform request to server
                ModNetworking.sendReformRequest();
            }
        }
    }
}
```

## Localization

### en_us.json

```json
{
    "key.categories.toonflattening": "Toon Flattening",
    "key.toonflattening.reform": "Reform from Flattened State"
}
```

## Keybind Behavior

| Condition | Behavior |
|-----------|----------|
| Not flattened | Ignored (no packet sent) |
| Flattened, reform blocked | Packet sent, server ignores |
| Flattened, can reform | Packet sent, reformation starts |
| GUI open | Ignored |

## Requirements Traced

- **UI-1**: Keybinding in Options > Controls
- **UI-1.1**: Default SPACE
- **UI-1.2**: Label "Reform from Flattened State"
- **UI-1.3**: Rebindable to any key
- **FR-REFORM.1**: Keybinding triggers reformation
- **FR-REFORM.2**: Ignore when not flattened
- **FR-REFORM.8.2**: Silent ignore when blocked (server handles)
