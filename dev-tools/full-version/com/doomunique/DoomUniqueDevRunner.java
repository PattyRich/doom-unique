package com.doomunique;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Entry point for ./gradlew runDev. Loads the full-featured dev build of the plugin (this
 * package, compiled only from the devTools source set) instead of the slim production version
 * in src/main/java.
 */
public class DoomUniqueDevRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DoomUniquePlugin.class);
		RuneLite.main(args);
	}
}
