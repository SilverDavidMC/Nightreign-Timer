package com.silver_david.nightreign_timer.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.JsonObject;

public interface JsonFile
{
	String fileName();
	
	JsonObject toJson();

	void fromJson(JsonObject json);
	
	default void load()
	{
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(fileName() + Util.JSON_EXTENSION))))
		{
			JsonObject json = Util.GSON.fromJson(Util.GSON.newJsonReader(reader), JsonObject.class);
			reader.close();
			this.fromJson(json);
		}
		catch (FileNotFoundException fileE)
		{
			// Silent fail
		}
		catch (IOException e)
		{
		}
	}

	default void save()
	{
		File file = new File(fileName() + Util.JSON_EXTENSION);
		JsonObject json = this.toJson();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file)))
		{
			Util.GSON.toJson(json, Util.GSON.newJsonWriter(writer));
			writer.close();
		}
		catch (Exception e)
		{
		}
	}
}
