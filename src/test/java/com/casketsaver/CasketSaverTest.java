package com.casketsaver;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CasketSaverTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CasketSaverPlugin.class);
		RuneLite.main(args);
	}
}