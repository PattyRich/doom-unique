package com.doomunique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Test;

public class ObjectIdParserTest
{
	@Test
	public void parsesCommaSpaceAndNewlineSeparatedIds()
	{
		Set<Integer> ids = ObjectIdParser.parse("123, 456\n789;1000");

		assertEquals(4, ids.size());
		assertTrue(ids.contains(123));
		assertTrue(ids.contains(456));
		assertTrue(ids.contains(789));
		assertTrue(ids.contains(1000));
	}

	@Test
	public void ignoresInvalidTokens()
	{
		Set<Integer> ids = ObjectIdParser.parse("abc, 321, nope");

		assertEquals(1, ids.size());
		assertTrue(ids.contains(321));
	}
}
