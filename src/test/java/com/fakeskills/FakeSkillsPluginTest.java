package com.fakeskills;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FakeSkillsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FakeSkillsPlugin.class);
		RuneLite.main(args);
	}
}