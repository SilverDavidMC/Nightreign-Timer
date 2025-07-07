package com.silver_david.nightreign_timer.util;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Util
{
	public static final Gson GSON = new GsonBuilder().setFormattingStyle(FormattingStyle.PRETTY.withIndent("\t")).disableHtmlEscaping().create();
	public static final String JSON_EXTENSION = ".json";

	public static List<File> getSubFiles(File directory, Predicate<File> predicate)
	{
		List<File> files = new ArrayList<>();
		addFiles(files, directory, predicate);
		return files;
	}

	private static void addFiles(List<File> files, File directory, Predicate<File> predicate)
	{
		if (directory.isDirectory() && directory.exists())
		{
			for (File file : directory.listFiles())
			{
				addFiles(files, file, predicate);
			}
		}
		else if (predicate.test(directory))
		{
			files.add(directory);
		}
	}

	public static String capitalize(String str)
	{
		if (str.isBlank())
			return str;
		StringBuilder builder = new StringBuilder();
		String[] words = str.split("_");
		for (int i = 0; i < words.length; i++)
		{
			String word = words[i];
			if (word.equals("or") || word.equals("and"))
				builder.append(word);
			else
				builder.append(Util.capitalizeFirstLetter(word));
			if (i < words.length - 1)
				builder.append(" ");
		}
		return builder.toString();
	}

	public static String capitalizeFirstLetter(String str)
	{
		if (str.isBlank())
			return str;
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	public static String encodeText(String str)
	{
		return str.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\"", "\\\"");
	}

	public static String toFileName(String str)
	{
		return str.toLowerCase().replaceAll("[,']", "").replaceAll("[^a-z0-9]", "_").trim();
	}

	public static int wrap(int value, int min, int max)
	{
		if (min > max)
			throw new IllegalArgumentException(String.format("min cannot be greater than max. Min: %d, Max: %d", min, max));

		int range = max - min + 1;
		int mod = (value - min) % range;
		if (mod < 0)
			return mod + 1 + max;
		else
			return mod + min;
	}

	public static <T> T make(Supplier<T> val, Consumer<T> action)
	{
		T v = val.get();
		action.accept(v);
		return v;
	}

	public static String colorToHex(Color color)
	{
		return String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
	}

	public static Optional<Color> colorFromHex(String hex)
	{
		String hash = "#";
		try
		{
			return Optional.ofNullable(Color.decode(hex.startsWith(hash) ? hex : hash + hex));
		}
		catch (NumberFormatException e)
		{
			return Optional.empty();
		}
	}

	private static final String[] NUMBER_SUFFIX = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };

	public static String numberSuffix(int i)
	{
		return switch (i % 100)
		{
		case 11 | 12 | 13 -> i + "th";
		default -> i + NUMBER_SUFFIX[i % 10];
		};
	}
}
