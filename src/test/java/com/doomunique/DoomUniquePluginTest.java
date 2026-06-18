package com.doomunique;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DoomUniquePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DoomUniquePlugin.class);
		RuneLite.main(args);
	}
}
