package com.silver_david.nightreign_timer.util;

import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gson.JsonObject;

public interface Serializable
{
	static final String TYPE_KEY = "type";

	void toJson(JsonObject json);
	
	JsonType<? extends Serializable> getType();
	
	default JsonObject toJson()
	{
		JsonObject json = new JsonObject();
		json.addProperty(TYPE_KEY, this.getType().id);
		this.toJson(json);
		return json;
	}

	public static record JsonType<T extends Serializable>(String id, Class<T> clazz, Function<JsonObject, T> fromJson, Consumer<Map<String, Component>> fillGuiComponents)
	{
		@Override
		public final String toString()
		{
			return Util.capitalize(id);
		}
	}
	
	public static class TypeRegistry<T extends Serializable>
	{
		private final Map<String, JsonType<? extends T>> data = new HashMap<>();
		
		public TypeRegistry()
		{
		}
		
		public T fromJson(JsonObject json)
		{
			String typeName = JsonUtil.getString(json, TYPE_KEY);
			JsonType<? extends T> type = data.get(typeName);
			if (type == null)
				throw new NullPointerException("The type " + typeName + " was not registered");
			return type.fromJson.apply(json);
		}
		
		public <V extends T> JsonType<V> register(String id, Class<V> clazz, Function<JsonObject, V> fromJson, Consumer<Map<String, Component>> fillGuiComponents)
		{
			return register(id, new JsonType<>(id, clazz, fromJson, fillGuiComponents));
		}
		
		public <V extends T> JsonType<V> register(String id, JsonType<V> type)
		{
			this.data.put(id, type);
			return type;
		}
		
		public List<JsonType<? extends T>> values()
		{
			return this.data.values().stream().sorted((a, b) -> a.toString().compareTo(b.toString())).toList();
		}
	}
}
