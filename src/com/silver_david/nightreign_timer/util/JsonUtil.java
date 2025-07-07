package com.silver_david.nightreign_timer.util;

import java.awt.Color;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonUtil
{
	public static String getString(JsonObject json, String key)
	{
		return get(json, key, JsonElement::getAsString);
	}

	public static int getInt(JsonObject json, String key)
	{
		return get(json, key, JsonElement::getAsInt);
	}

	public static boolean getBoolean(JsonObject json, String key)
	{
		return get(json, key, JsonElement::getAsBoolean);
	}

	public static JsonObject getJson(JsonObject json, String key)
	{
		return get(json, key, JsonElement::getAsJsonObject);
	}
	
	public static Color getColor(JsonObject json, String key)
	{
		return get(json, key, j -> Util.colorFromHex(j.getAsString()).orElse(Color.BLACK));
	}

	public static void addColor(JsonObject json, String key, Color color)
	{
		json.addProperty(key, Util.colorToHex(color));
	}

	public static <T> T get(JsonObject json, String key, Function<JsonElement, T> cast)
	{
		return cast.apply(json.get(key));
	}

	public static String getOrDefault(JsonObject json, String key, String defaultVal)
	{
		return getOrDefault(json, key, JsonElement::getAsString, defaultVal);
	}

	public static int getOrDefault(JsonObject json, String key, int defaultVal)
	{
		return getOrDefault(json, key, JsonElement::getAsInt, defaultVal);
	}

	public static boolean getOrDefault(JsonObject json, String key, boolean defaultVal)
	{
		return getOrDefault(json, key, JsonElement::getAsBoolean, defaultVal);
	}

	public static JsonObject getOrDefault(JsonObject json, String key, JsonObject defaultVal)
	{
		return getOrDefault(json, key, JsonElement::getAsJsonObject, defaultVal);
	}

	public static Color getOrDefault(JsonObject json, String key, Color defaultVal)
	{
		return json.has(key) ? getColor(json, key) : defaultVal;
	}

	public static <T> T getOrDefault(JsonObject json, String key, Function<JsonElement, T> cast, T defaultVal)
	{
		return json.has(key) ? get(json, key, cast) : defaultVal;
	}
}
