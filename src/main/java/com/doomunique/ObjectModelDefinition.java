package com.doomunique;

import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.Set;
import net.runelite.api.IndexDataBase;

final class ObjectModelDefinition
{
	private static final int OBJECT_CONFIG_TYPE = 6;
	private static final int DEFAULT_MODEL_SIZE = 128;

	private int[] modelIds;
	private int[] modelTypes;
	private short[] recolorFrom;
	private short[] recolorTo;
	private short[] retextureFrom;
	private short[] retextureTo;
	private int modelSizeX = DEFAULT_MODEL_SIZE;
	private int modelHeight = DEFAULT_MODEL_SIZE;
	private int modelSizeY = DEFAULT_MODEL_SIZE;
	private int offsetX;
	private int offsetHeight;
	private int offsetY;
	private int animationId = -1;
	private boolean rotated;

	private ObjectModelDefinition()
	{
	}

	static ObjectModelDefinition load(IndexDataBase indexConfig, int objectId)
	{
		byte[] data = indexConfig.loadData(OBJECT_CONFIG_TYPE, objectId);
		if (data == null)
		{
			return null;
		}

		ObjectModelDefinition definition = new ObjectModelDefinition();
		definition.decode(ByteBuffer.wrap(data));
		return definition;
	}

	int[] getModelIds()
	{
		if (modelIds == null || modelIds.length == 0)
		{
			return new int[0];
		}

		if (modelTypes == null)
		{
			return uniqueModelIds(modelIds);
		}

		int[] ids = modelIdsForType(10);
		if (ids.length > 0)
		{
			return ids;
		}

		ids = modelIdsForType(22);
		return ids.length > 0 ? ids : uniqueModelIds(modelIds);
	}

	short[] getRecolorFrom()
	{
		return recolorFrom;
	}

	short[] getRecolorTo()
	{
		return recolorTo;
	}

	short[] getRetextureFrom()
	{
		return retextureFrom;
	}

	short[] getRetextureTo()
	{
		return retextureTo;
	}

	boolean isScaled()
	{
		return modelSizeX != DEFAULT_MODEL_SIZE || modelHeight != DEFAULT_MODEL_SIZE || modelSizeY != DEFAULT_MODEL_SIZE;
	}

	int getModelSizeX()
	{
		return modelSizeX;
	}

	int getModelHeight()
	{
		return modelHeight;
	}

	int getModelSizeY()
	{
		return modelSizeY;
	}

	boolean isTranslated()
	{
		return offsetX != 0 || offsetHeight != 0 || offsetY != 0;
	}

	int getOffsetX()
	{
		return offsetX;
	}

	int getOffsetHeight()
	{
		return offsetHeight;
	}

	int getOffsetY()
	{
		return offsetY;
	}

	boolean isRotated()
	{
		return rotated;
	}

	boolean hasAnimation()
	{
		return animationId >= 0;
	}

	int getAnimationId()
	{
		return animationId;
	}

	private void decode(ByteBuffer buffer)
	{
		while (buffer.hasRemaining())
		{
			int opcode = readUnsignedByte(buffer);
			if (opcode == 0)
			{
				return;
			}

			decodeOpcode(buffer, opcode);
		}
	}

	private void decodeOpcode(ByteBuffer buffer, int opcode)
	{
		if (opcode == 1)
		{
			int count = readUnsignedByte(buffer);
			modelIds = new int[count];
			modelTypes = new int[count];
			for (int i = 0; i < count; i++)
			{
				modelIds[i] = readUnsignedShort(buffer);
				modelTypes[i] = readUnsignedByte(buffer);
			}
		}
		else if (opcode == 6)
		{
			int count = readUnsignedByte(buffer);
			modelIds = new int[count];
			modelTypes = new int[count];
			for (int i = 0; i < count; i++)
			{
				modelIds[i] = readInt(buffer);
				modelTypes[i] = readUnsignedByte(buffer);
			}
		}
		else if (opcode == 2)
		{
			readString(buffer);
		}
		else if (opcode == 5)
		{
			int count = readUnsignedByte(buffer);
			modelIds = new int[count];
			modelTypes = null;
			for (int i = 0; i < count; i++)
			{
				modelIds[i] = readUnsignedShort(buffer);
			}
		}
		else if (opcode == 7)
		{
			int count = readUnsignedByte(buffer);
			modelIds = new int[count];
			modelTypes = null;
			for (int i = 0; i < count; i++)
			{
				modelIds[i] = readInt(buffer);
			}
		}
		else if (opcode == 14 || opcode == 15 || opcode == 28 || opcode == 69 || opcode == 81)
		{
			readUnsignedByte(buffer);
		}
		else if (opcode == 17 || opcode == 18 || opcode == 21 || opcode == 22 || opcode == 23 || opcode == 27
			|| opcode == 64 || opcode == 73 || opcode == 74 || opcode == 89 || opcode == 90 || opcode == 94)
		{
			// Flags without payload.
		}
		else if (opcode == 19 || opcode == 29 || opcode == 39 || opcode == 75 || opcode == 91 || opcode == 95
			|| opcode == 96)
		{
			readByte(buffer);
		}
		else if (opcode >= 30 && opcode < 35)
		{
			readString(buffer);
		}
		else if (opcode == 24)
		{
			animationId = normalizeId(readUnsignedShort(buffer));
		}
		else if (opcode == 61 || opcode == 68 || opcode == 82)
		{
			readUnsignedShort(buffer);
		}
		else if (opcode == 40)
		{
			int count = readUnsignedByte(buffer);
			recolorFrom = new short[count];
			recolorTo = new short[count];
			for (int i = 0; i < count; i++)
			{
				recolorFrom[i] = (short) readUnsignedShort(buffer);
				recolorTo[i] = (short) readUnsignedShort(buffer);
			}
		}
		else if (opcode == 41)
		{
			int count = readUnsignedByte(buffer);
			retextureFrom = new short[count];
			retextureTo = new short[count];
			for (int i = 0; i < count; i++)
			{
				retextureFrom[i] = (short) readUnsignedShort(buffer);
				retextureTo[i] = (short) readUnsignedShort(buffer);
			}
		}
		else if (opcode == 62)
		{
			rotated = true;
		}
		else if (opcode == 65)
		{
			modelSizeX = readUnsignedShort(buffer);
		}
		else if (opcode == 66)
		{
			modelHeight = readUnsignedShort(buffer);
		}
		else if (opcode == 67)
		{
			modelSizeY = readUnsignedShort(buffer);
		}
		else if (opcode == 70)
		{
			offsetX = readShort(buffer);
		}
		else if (opcode == 71)
		{
			offsetHeight = readShort(buffer);
		}
		else if (opcode == 72)
		{
			offsetY = readShort(buffer);
		}
		else if (opcode == 77)
		{
			readTransforms(buffer, false);
		}
		else if (opcode == 78)
		{
			readUnsignedShort(buffer);
			readUnsignedByte(buffer);
			readUnsignedByte(buffer);
		}
		else if (opcode == 79)
		{
			readUnsignedShort(buffer);
			readUnsignedShort(buffer);
			readUnsignedByte(buffer);
			readUnsignedByte(buffer);
			int count = readUnsignedByte(buffer);
			for (int i = 0; i < count; i++)
			{
				readUnsignedShort(buffer);
			}
		}
		else if (opcode == 92)
		{
			readTransforms(buffer, true);
		}
		else if (opcode == 93)
		{
			readUnsignedByte(buffer);
			readUnsignedShort(buffer);
			readUnsignedByte(buffer);
			readUnsignedShort(buffer);
		}
		else if (opcode == 100)
		{
			readUnsignedByte(buffer);
			readUnsignedByte(buffer);
			readString(buffer);
		}
		else if (opcode == 101)
		{
			readUnsignedByte(buffer);
			readUnsignedShort(buffer);
			readUnsignedShort(buffer);
			readInt(buffer);
			readInt(buffer);
			readString(buffer);
		}
		else if (opcode == 102)
		{
			readUnsignedByte(buffer);
			readUnsignedShort(buffer);
			readUnsignedShort(buffer);
			readUnsignedShort(buffer);
			readInt(buffer);
			readInt(buffer);
			readString(buffer);
		}
		else if (opcode == 249)
		{
			readParams(buffer);
		}
		else
		{
			throw new IllegalArgumentException("Unsupported object definition opcode " + opcode);
		}
	}

	private int[] modelIdsForType(int type)
	{
		Set<Integer> ids = new LinkedHashSet<>();
		for (int i = 0; i < modelIds.length; i++)
		{
			if (modelTypes[i] == type)
			{
				ids.add(modelIds[i]);
			}
		}
		return toArray(ids);
	}

	private static int[] uniqueModelIds(int[] modelIds)
	{
		Set<Integer> ids = new LinkedHashSet<>();
		for (int modelId : modelIds)
		{
			ids.add(modelId);
		}
		return toArray(ids);
	}

	private static int[] toArray(Set<Integer> ids)
	{
		int[] values = new int[ids.size()];
		int index = 0;
		for (int id : ids)
		{
			values[index++] = id;
		}
		return values;
	}

	private void readTransforms(ByteBuffer buffer, boolean hasFallback)
	{
		readUnsignedShort(buffer);
		readUnsignedShort(buffer);
		if (hasFallback)
		{
			readUnsignedShort(buffer);
		}

		int count = readUnsignedByte(buffer);
		for (int i = 0; i <= count; i++)
		{
			readUnsignedShort(buffer);
		}
	}

	private void readParams(ByteBuffer buffer)
	{
		int count = readUnsignedByte(buffer);
		for (int i = 0; i < count; i++)
		{
			boolean stringValue = readUnsignedByte(buffer) == 1;
			readMedium(buffer);
			if (stringValue)
			{
				readString(buffer);
			}
			else
			{
				readInt(buffer);
			}
		}
	}

	private static int readUnsignedByte(ByteBuffer buffer)
	{
		return buffer.get() & 0xFF;
	}

	private static byte readByte(ByteBuffer buffer)
	{
		return buffer.get();
	}

	private static int readUnsignedShort(ByteBuffer buffer)
	{
		return buffer.getShort() & 0xFFFF;
	}

	private static short readShort(ByteBuffer buffer)
	{
		return buffer.getShort();
	}

	private static int readMedium(ByteBuffer buffer)
	{
		return (readUnsignedByte(buffer) << 16) | (readUnsignedByte(buffer) << 8) | readUnsignedByte(buffer);
	}

	private static int readInt(ByteBuffer buffer)
	{
		return buffer.getInt();
	}

	private static int normalizeId(int id)
	{
		return id == 65535 ? -1 : id;
	}

	private static String readString(ByteBuffer buffer)
	{
		StringBuilder builder = new StringBuilder();
		byte value;
		while ((value = buffer.get()) != 0)
		{
			builder.append((char) value);
		}
		return builder.toString();
	}
}
