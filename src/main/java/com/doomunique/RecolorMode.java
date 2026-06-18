package com.doomunique;

public enum RecolorMode
{
	GOLD_FACES("Gold/yellow faces only"),
	ALL_FACES("Entire model");

	private final String name;

	RecolorMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
