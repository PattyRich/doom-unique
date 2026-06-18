package com.doomunique;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

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
		description = "Recolor matching unique hole object IDs",
		position = 1,
		section = colorsSection
	)
	default boolean recolorUniqueHole()
	{
		return true;
	}

	@ConfigItem(
		keyName = "uniqueHoleObjectIds",
		name = "Unique hole object IDs",
		description = "Comma, space, or newline separated object IDs for local development testing",
		position = 0,
		section = testingSection
	)
	default String uniqueHoleObjectIds()
	{
		return String.valueOf(DOM_DESCEND_HOLE_UNIQUE);
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
}
