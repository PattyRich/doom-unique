package com.doomunique;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

final class ObjectIdParser
{
	private ObjectIdParser()
	{
	}

	static Set<Integer> parse(String text)
	{
		if (text == null || text.trim().isEmpty())
		{
			return Collections.emptySet();
		}

		Set<Integer> ids = new LinkedHashSet<>();
		for (String token : text.split("[^0-9]+"))
		{
			if (token.isEmpty())
			{
				continue;
			}

			try
			{
				ids.add(Integer.parseInt(token));
			}
			catch (NumberFormatException ignored)
			{
				// Ignore malformed or overflowing IDs in local dev config.
			}
		}
		return ids;
	}
}
