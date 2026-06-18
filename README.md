# Doom Unique Colors

Customize the color of the special-loot hole that appears after killing Doom of Mokhaiotl.

The Doom ID is intentionally configurable. RuneLite's current game values name `50940` as `DOM_DESCEND_HOLE_UNIQUE`, so that ID is prefilled as the real unique-hole target. The object's own definition supplies the animation used by the temporary swap helper.

By default, **Recolor mode** is **Gold/yellow faces only**. This avoids turning the whole hole model into one solid color. **Entire model** is available as a debug fallback, but it will usually look like a blob.

## Testing with a normal hole

1. In **Testing**, leave **Normal hole object IDs** as `57285`, or replace it if discovery finds a different normal hole ID.
2. Enable **Test on normal hole**.
3. Pick a visible **Unique hole color**.
4. You should see a colored overlay glow at the normal hole location, not a recolored hole model.
5. Disable **Test on normal hole** once the golden/special-loot hole ID has been confirmed.

Do not put `57285` in **Unique hole object IDs** unless you intentionally want to recolor the entire burrow-hole model.

## Testing without doing the boss

For quick color testing, use the **Testing** section:

1. Enable **Log nearby object IDs** and stand near any object you do not mind recoloring in the dev client.
2. Put that object's ID in **Temporary test object IDs**.
3. Set **Temporary test object type** to the RuneLite scene-object type you want to target. Start with **Game object** when testing the Doom hole.
4. Enable **Recolor test objects**.
5. Change **Unique hole color** until the recolor behavior looks right.
6. Disable **Recolor test objects** when you are done.

For a closer stand-in, enable **Swap test objects to unique hole** instead. The plugin will hide each configured temporary test object and spawn a client-side model built from **Swap target object ID** on the same tile. **Swap target object ID** defaults to `50940` and uses that object's own animation.

This swap is temporary and client-side only. It does not change the actual game object or send anything to Jagex; disabling the setting or reloading the scene removes the spawned model.

Enable **Recolor swapped test hole** to apply **Unique hole color** to the swapped `50940` stand-in. Keep **Recolor mode** on **Gold/yellow faces only** for normal testing; **Entire model** is only for debugging.

RuneLite devtools may show game objects as `ID: ... A: ...`. The `A` value is an object animation/sequence ID. The plugin's discovery messages include `animation`, `frame`, and `cycle` for animated game objects.

If the replacement appears but the old object remains visible, reload the scene by restarting the dev client or changing areas. The original object can only be hidden when RuneLite rebuilds/uploads the scene.

## Finding IDs

The plugin includes a small discovery mode so you can capture IDs in the RuneLite dev client.

1. Run the plugin with `./gradlew run` or `gradlew.bat run`.
2. Enable **Log nearby object IDs** in the plugin config.
3. Stand near Doom's reward hole.
4. Keep **Object name filter** set to `hole`, or clear it briefly if no scene objects log.
5. Check the RuneLite debug console/log for lines beginning with `Doom Unique discovery`.

If console access is inconvenient, enable **Discovery messages in chat** while testing. It only reports each nearby object/location once per scene load/config change.

## Plugin Hub notes

This project follows RuneLite's external plugin template:

- Java 11 target
- `runelite-plugin.properties`
- `build=standard`
- no third-party dependencies beyond RuneLite template dependencies
