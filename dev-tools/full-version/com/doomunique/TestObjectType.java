package com.doomunique;

public enum TestObjectType
{
	ANY("Any"),
	GAME("Game object"),
	GROUND("Ground object"),
	DECORATIVE("Decorative object"),
	WALL("Wall object");

	private final String name;

	TestObjectType(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
