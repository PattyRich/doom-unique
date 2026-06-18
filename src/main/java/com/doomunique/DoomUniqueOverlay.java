package com.doomunique;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.ArrayList;
import javax.inject.Inject;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class DoomUniqueOverlay extends Overlay
{
	private final DoomUniquePlugin plugin;
	private final DoomUniqueConfig config;

	@Inject
	DoomUniqueOverlay(DoomUniquePlugin plugin, DoomUniqueConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.testOnNormalHole())
		{
			return null;
		}

		for (TileObject object : new ArrayList<>(plugin.getTestOverlayObjects()))
		{
			Shape tile = object.getCanvasTilePoly();
			if (tile == null)
			{
				tile = object.getClickbox();
			}
			if (tile != null)
			{
				renderGlow(graphics, tile, config.uniqueHoleColor());
			}
		}

		return null;
	}

	private void renderGlow(Graphics2D graphics, Shape shape, Color color)
	{
		Color previousColor = graphics.getColor();
		Stroke previousStroke = graphics.getStroke();

		graphics.setColor(withAlpha(color, 18));
		graphics.fill(shape);

		graphics.setStroke(new BasicStroke(7.0f));
		graphics.setColor(withAlpha(color, 55));
		graphics.draw(shape);

		graphics.setStroke(new BasicStroke(4.0f));
		graphics.setColor(withAlpha(color, 95));
		graphics.draw(shape);

		graphics.setStroke(new BasicStroke(1.5f));
		graphics.setColor(withAlpha(color, 190));
		graphics.draw(shape);

		graphics.setColor(previousColor);
		graphics.setStroke(previousStroke);
	}

	private Color withAlpha(Color color, int alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}
}
