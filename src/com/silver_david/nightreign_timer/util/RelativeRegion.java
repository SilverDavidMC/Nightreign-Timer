package com.silver_david.nightreign_timer.util;

public record RelativeRegion(float x, float y, float w, float h)
{
	public static RelativeRegion fromString(String string)
	{
		String[] vals = string.split(",");
		if (vals.length < 4)
			throw new IllegalArgumentException("Invalid text format for a RelativeRegion " + string);
		float[] floats = new float[4];
		for (int i = 0; i < 4; i++)
		{
			String val = vals[i].trim();
			floats[i] = Float.valueOf(val);
		}
		return new RelativeRegion(floats[0], floats[1], floats[2], floats[3]);
	}
	
	@Override
	public final String toString()
	{
		return String.format("%.3f, %.3f, %.3f, %.3f", x, y, w, h);
	}
}
