package com.doomunique;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class ObjectIdParser
{
	private ObjectIdParser()
	{
	}

	static Set<Integer> parse(String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return Collections.emptySet();
		}

		Set<Integer> ids = new HashSet<>();
		String[] tokens = value.split("[,;\\s]+");
		for (String token : tokens)
		{
			addToken(ids, token.trim());
		}
		return ids;
	}

	private static void addToken(Set<Integer> ids, String token)
	{
		if (token.isEmpty())
		{
			return;
		}

		try
		{
			ids.add(Integer.parseInt(token));
		}
		catch (NumberFormatException ignored)
		{
			// Invalid config entries are ignored so one typo does not disable the whole list.
		}
	}
}
