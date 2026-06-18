package com.doomunique;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(DoomUniqueConfig.GROUP)
public interface DoomUniqueConfig extends Config
{
	String GROUP = "doomunique";
	int DOM_DESCEND_HOLE_UNIQUE = 50940;
	int DOM_DESCEND_HOLE = 57285;

	@ConfigSection(
		name = "Colors",
		description = "Hole recolor settings",
		position = 0
	)
	String colorsSection = "colors";

	@ConfigSection(
		name = "Object IDs",
		description = "Doom hole object IDs",
		position = 1
	)
	String objectIdsSection = "objectIds";

	@ConfigSection(
		name = "Discovery",
		description = "Helpers for finding Doom hole object IDs",
		position = 3,
		closedByDefault = true
	)
	String discoverySection = "discovery";

	@ConfigSection(
		name = "Testing",
		description = "Temporary client-side testing helpers",
		position = 2,
		closedByDefault = true
	)
	String testingSection = "testing";

	@ConfigItem(
		keyName = "uniqueHoleColor",
		name = "Unique hole color",
		description = "Color to apply to the special-loot hole",
		position = 0,
		section = colorsSection
	)
	default Color uniqueHoleColor()
	{
		return Color.decode("#DA56F5");
	}

	@ConfigItem(
		keyName = "recolorUniqueHole",
		name = "Recolor unique hole",
		description = "Recolor separate glow objects listed in Unique hole object IDs",
		position = 1,
		section = colorsSection
	)
	default boolean recolorUniqueHole()
	{
		return true;
	}

	@ConfigItem(
		keyName = "removeTextures",
		name = "Remove model textures",
		description = "Temporarily remove textures from recolored targets so the color can show on textured models",
		position = 2,
		section = colorsSection
	)
	default boolean removeTextures()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recolorMode",
		name = "Recolor mode",
		description = "Choose whether to recolor only gold/yellow faces or every face on matched models",
		position = 3,
		section = colorsSection
	)
	default RecolorMode recolorMode()
	{
		return RecolorMode.GOLD_FACES;
	}

	@ConfigItem(
		keyName = "uniqueHoleObjectIds",
		name = "Unique hole object IDs",
		description = "Comma, space, or newline separated object IDs for a separate golden/special-loot glow object",
		position = 0,
		section = objectIdsSection
	)
	default String uniqueHoleObjectIds()
	{
		return String.valueOf(DOM_DESCEND_HOLE_UNIQUE);
	}

	@ConfigItem(
		keyName = "testOnNormalHole",
		name = "Test on normal hole",
		description = "Draw a temporary colored glow overlay on normal hole IDs for testing",
		position = 0,
		section = testingSection
	)
	default boolean testOnNormalHole()
	{
		return false;
	}

	@ConfigItem(
		keyName = "normalHoleObjectIds",
		name = "Normal hole object IDs",
		description = "Comma, space, or newline separated normal hole IDs used as Test on normal hole overlay anchors",
		position = 1,
		section = testingSection
	)
	default String normalHoleObjectIds()
	{
		return String.valueOf(DOM_DESCEND_HOLE);
	}

	@ConfigItem(
		keyName = "recolorTestObjects",
		name = "Recolor test objects",
		description = "Temporarily recolor objects listed in Temporary test object IDs as if they were the unique glow",
		position = 2,
		section = testingSection
	)
	default boolean recolorTestObjects()
	{
		return false;
	}

	@ConfigItem(
		keyName = "swapTestObjects",
		name = "Swap test objects to unique hole",
		description = "Temporarily hide test objects and spawn a client-side unique-hole model on their tile",
		position = 3,
		section = testingSection
	)
	default boolean swapTestObjects()
	{
		return false;
	}

	@ConfigItem(
		keyName = "recolorSwapTestObjects",
		name = "Recolor swapped test hole",
		description = "Apply Unique hole color to the temporary swapped unique-hole model",
		position = 4,
		section = testingSection
	)
	default boolean recolorSwapTestObjects()
	{
		return true;
	}

	@ConfigItem(
		keyName = "testObjectIds",
		name = "Temporary test object IDs",
		description = "Comma, space, or newline separated object IDs to recolor or swap for local testing without doing the boss",
		position = 5,
		section = testingSection
	)
	default String testObjectIds()
	{
		return "";
	}

	@ConfigItem(
		keyName = "testObjectType",
		name = "Temporary test object type",
		description = "Only recolor or swap temporary test objects with this RuneLite scene-object type",
		position = 6,
		section = testingSection
	)
	default TestObjectType testObjectType()
	{
		return TestObjectType.GAME;
	}

	@ConfigItem(
		keyName = "swapTargetObjectId",
		name = "Swap target object ID",
		description = "Object ID to spawn when Swap test objects to unique hole is enabled",
		position = 7,
		section = testingSection
	)
	default int swapTargetObjectId()
	{
		return DOM_DESCEND_HOLE_UNIQUE;
	}

	@ConfigItem(
		keyName = "discoveryMode",
		name = "Log nearby object IDs",
		description = "Log nearby scene objects to help identify Doom hole IDs",
		position = 0,
		section = discoverySection
	)
	default boolean discoveryMode()
	{
		return false;
	}

	@Range(
		min = 1,
		max = 32
	)
	@ConfigItem(
		keyName = "discoveryRadius",
		name = "Discovery radius",
		description = "Tile radius around you to inspect while discovery is enabled",
		position = 1,
		section = discoverySection
	)
	default int discoveryRadius()
	{
		return 8;
	}

	@ConfigItem(
		keyName = "discoveryNameFilter",
		name = "Object name filter",
		description = "Only log objects whose names contain this text. Leave blank to log everything nearby",
		position = 2,
		section = discoverySection
	)
	default String discoveryNameFilter()
	{
		return "";
	}

	@ConfigItem(
		keyName = "discoveryChatMessages",
		name = "Discovery messages in chat",
		description = "Also print discovery results in the chatbox while testing",
		position = 3,
		section = discoverySection
	)
	default boolean discoveryChatMessages()
	{
		return true;
	}
}
