package com.silver_david.nightreign_timer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.gson.JsonObject;
import com.silver_david.nightreign_timer.Settings.OptionInstance.Value;
import com.silver_david.nightreign_timer.gui.TimerPanel;
import com.silver_david.nightreign_timer.util.JsonFile;
import com.silver_david.nightreign_timer.util.JsonUtil;
import com.silver_david.nightreign_timer.util.RelativeRegion;

public class Settings implements JsonFile
{
	private final Map<String, OptionInstance<?>> options = new LinkedHashMap<>();
	public final Map<String, OptionInstance<?>> guiOptions = new LinkedHashMap<>();

	// Visible in options menu
	public OptionInstance<Boolean> playWarningSound = create("play_warning_sound", true, "Play a sound when a circle is closing soon.", true);
	public OptionInstance<Integer> volume = create("volume", true, "The general sound volume.", 25, 0, 51);
	public OptionInstance<Boolean> autoDetectDayStart = create("auto_detect_day_start", true, "Detect a day starting by reading the text on your screen. This will not work if you have the map open while the start of day text is trying to display.", true).onValueChange(b ->
	{
		if (b && !TimerPanel.photoThread.isAlive())
			TimerPanel.photoThread.start();
	});
	public OptionInstance<Integer> monitorToWatch = create("monitor_to_watch", true, "The display to watch. This should be the one running Nightreign.", 0);
	public OptionInstance<RelativeRegion> areaToWatch = create("region_to_watch", true, "Values are relative to the display being watched, represented as a percentage: (x, y, width, height)", RelativeRegion::fromString, RelativeRegion::toString, new RelativeRegion(0.41F, 0.45F, 0.19F, 0.22F), null);
	public OptionInstance<Float> contrastBrightness = create("contrast_brightness", true, "The brightness setting for increasing the captured image's constrast.", 100.0F);
	public OptionInstance<Float> contrastDarkness = create("contrast_darkness", true, "The darkness setting for increasing the captured image's constrast.", -20000.0F);
	public OptionInstance<String> languageDataset = create("language_dataset", true, "The language dataset to use for auto detection. https://github.com/tesseract-ocr/tessdata", "english");
	public OptionInstance<String> languageDatasetDirectory = create("language_dataset_folder", true, "The folder with the language datasets. https://github.com/tesseract-ocr/tessdata", "");
	public OptionInstance<String> day1Text = create("day_1_text", true, "The text in the middle of the screen when day 1 starts.", "DAY I");
	public OptionInstance<String> day2Text = create("day_2_text", true, "The text in the middle of the screen when day 2 starts.", "DAY II");
	public OptionInstance<String> day3Text = create("day_3_text", true, "The text in the middle of the screen when day 3 starts.", "DAY III");
	public OptionInstance<Boolean> startDayOnImageDetection = create("start_day_on_detection", true, "Start the day when the \"day x\" text is detected.", true);
	public OptionInstance<Boolean> pinToTop = create("pin_to_top", true, "Keep the application pinned as the top window.", false).onValueChange(NightreignTimer::setAlwaysOnTop);
	public OptionInstance<Boolean> showDebug = create("show_debug", true, "Show debug info.", false);

	// Used for caching data
	public OptionInstance<Integer> version = create("version", false, "The version of the program (Do not change!)", 1);

	public <T> OptionInstance<T> create(String name, boolean guiVisible, String comment, Function<String, T> readFunc, Function<T, String> writeFunc, T defaultValue, List<T> possibleValues)
	{
		var ret = new OptionInstance<>(comment, readFunc, writeFunc, defaultValue, new Value<>(defaultValue), Optional.ofNullable(possibleValues));
		if (this.options.put(name, ret) != null)
			throw new IllegalStateException("An options value already exists under then name " + name);
		if (guiVisible)
			guiOptions.put(name, ret);
		return ret;
	}

	public OptionInstance<String> create(String name, boolean guiVisible, String comment, String defaultValue)
	{
		return create(name, guiVisible, comment, Function.identity(), Function.identity(), defaultValue, null);
	}

	public OptionInstance<Boolean> create(String name, boolean guiVisible, String comment, boolean defaultValue)
	{
		return create(name, guiVisible, comment, Boolean::valueOf, String::valueOf, defaultValue, List.of(true, false));
	}

	public OptionInstance<Integer> create(String name, boolean guiVisible, String comment, int defaultValue)
	{
		return create(name, guiVisible, comment, Integer::valueOf, String::valueOf, defaultValue, null);
	}

	public OptionInstance<Integer> create(String name, boolean guiVisible, String comment, int defaultValue, int min, int max)
	{
		return create(name, guiVisible, comment, Integer::valueOf, String::valueOf, defaultValue, IntStream.range(min, max).boxed().toList());
	}
	
	public OptionInstance<Float> create(String name, boolean guiVisible, String comment, float defaultValue)
	{
		return create(name, guiVisible, comment, Float::valueOf, String::valueOf, defaultValue, null);
	}

	public <T> OptionInstance<List<T>> create(String name, boolean guiVisible, String comment, Function<String, T> fromString, Function<T, String> toString, Predicate<T> isValid, List<T> defaultValue)
	{
		return create(name, guiVisible, comment, s -> Arrays.stream(s.substring(1, s.length() - 1).split(",")).map(String::trim).filter(str -> !str.isBlank()).map(fromString).filter(isValid).collect(Collectors.toList()), l -> "[" + String.join(", ", l.stream().filter(isValid).map(toString).toList()) + "]", defaultValue, null);
	}

	@Override
	public String fileName()
	{
		return "settings";
	}

	@Override
	public JsonObject toJson()
	{
		JsonObject json = new JsonObject();
		this.options.forEach((name, option) ->
		{
			json.addProperty(name, option.save());
		});
		return json;
	}

	@Override
	public void fromJson(JsonObject json)
	{
		for (String optionName : json.keySet())
		{
			OptionInstance<?> option = this.options.get(optionName);
			if (option != null)
			{
				option.load(JsonUtil.getString(json, optionName));
			}
		}
	}

	public static class OptionInstance<T> implements Supplier<T>
	{
		final String comment;
		final Function<String, T> readFunc;
		final Function<T, String> writeFunc;
		final T defaultValue;
		final Value<T> value;
		final Optional<List<T>> possibleValues;
		Optional<Consumer<T>> onValueChange = Optional.empty();

		OptionInstance(String comment, Function<String, T> readFunc, Function<T, String> writeFunc, T defaultValue, Value<T> value, Optional<List<T>> possibleValues)
		{
			this.comment = comment;
			this.readFunc = readFunc;
			this.writeFunc = writeFunc;
			this.defaultValue = defaultValue;
			this.value = value;
			this.possibleValues = possibleValues;
		}

		OptionInstance<T> onValueChange(Consumer<T> valueChange)
		{
			this.onValueChange = Optional.ofNullable(valueChange);
			return this;
		}

		@Override
		public T get()
		{
			return this.value.get();
		}

		public Optional<T> getFirst()
		{
			return possibleValues.map(List::getFirst);
		}

		public Optional<T> getLast()
		{
			return possibleValues.map(List::getLast);
		}

		public void set(T val)
		{
			if (possibleValues.map(p -> p.contains(val)).orElse(true))
			{
				this.value.set(val, onValueChange);
			}
		}

		public void load(String data)
		{
			try
			{
				this.set(readFunc.apply(data));
			}
			catch (Throwable t)
			{
				t.printStackTrace();
				this.set(defaultValue);
			}
		}

		public String save()
		{
			return writeFunc.apply(this.get());
		}

		public String saveDefault()
		{
			return writeFunc.apply(this.defaultValue);
		}

		public boolean isNumber()
		{
			return defaultValue instanceof Number;
		}

		public Optional<List<T>> possibleValues()
		{
			return possibleValues;
		}

		public String comment()
		{
			return String.format("%s Default: %s", this.comment, this.saveDefault());
		}

		static class Value<V>
		{
			private V val;

			private Value(V val)
			{
				this.val = val;
			}

			private void set(V val, Optional<Consumer<V>> onValueChange)
			{
				V oldVal = this.val;
				this.val = val;
				if (onValueChange.isPresent() && !oldVal.equals(this.val))
					onValueChange.get().accept(this.val);
			}

			public V get()
			{
				return this.val;
			}
		}
	}
}
