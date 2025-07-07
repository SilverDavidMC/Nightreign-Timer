package com.silver_david.nightreign_timer.gui;

import java.awt.Color;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import com.silver_david.nightreign_timer.NightreignTimer;
import com.silver_david.nightreign_timer.Settings.OptionInstance;
import com.silver_david.nightreign_timer.util.Util;

public class SettingsScreen implements GuiFrame.Child
{
	final JFrame frame;
	final Map<String, OptionInstance<?>> options;
	final Map<String, JComponent> components = new HashMap<>();

	public SettingsScreen(Map<String, OptionInstance<?>> options)
	{
		this.frame = new JFrame("Settings");
		this.options = options;
	}

	@Override
	public JFrame getFrame()
	{
		return frame;
	}

	@Override
	public JMenuBar getMenuBar()
	{
		return null;
	}

	@Override
	public GuiFrame getParent()
	{
		return NightreignTimer.instance;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void initGui(Gui gui, int width, int height, int centerX, int centerY)
	{
		components.clear();
		int i = 0;
		int labelX = 0, labelY = 40, labelWidth = 200, labelHeight = 30;
		int inputWidth = 300;
		int labelInputMargin = 10;
		for (var entry : options.entrySet())
		{
			String name = entry.getKey();
			OptionInstance<?> option = entry.getValue();
			int y = labelY * i;
			JLabel label = gui.label(Util.capitalize(name), labelX, y, labelWidth, labelHeight);
			label.setOpaque(true);
			label.setBackground(Color.LIGHT_GRAY);
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setToolTipText(option.comment());
			if (!option.possibleValues().isPresent())
			{
				components.put(name, gui.textInput(option.save(), labelX + labelWidth + labelInputMargin, y, inputWidth, labelHeight));
			}
			else
			{
				if (option.isNumber())
				{
					if (option.getFirst().get() instanceof Integer)
					{
						components.put(name, gui.spinner(new SpinnerNumberModel((Integer) option.get(), (Integer) option.getFirst().get(), (Integer) option.getLast().get(), Integer.valueOf(1)), labelX + labelWidth + labelInputMargin, y, inputWidth, labelHeight, e ->
						{
						}));
					}
					else if (option.getFirst().get() instanceof Float)
					{
						components.put(name, gui.spinner(new SpinnerNumberModel((Float) option.get(), (Float) option.getFirst().get(), (Float) option.getLast().get(), Float.valueOf(0.01F)), labelX + labelWidth + labelInputMargin, y, inputWidth, labelHeight, e ->
						{
						}));
					}
				}
				else
				{
					components.put(name, gui.selection(option.possibleValues().get().toArray(), option.get(), labelX + labelWidth + labelInputMargin, y, inputWidth, labelHeight, c ->
					{
					}));
				}
			}
			i++;
		}

		int buttonWidth = 100, buttonHeight = 30;

		int buttonCount = 2;
		var saveButton = gui.button("Save", ((labelWidth + labelInputMargin + inputWidth) / 2) - ((buttonCount * buttonWidth) + ((buttonCount - 1) * gui.defaultMargin)) / 2, labelY * i, buttonWidth, buttonHeight, b ->
		{
			components.forEach((name, component) ->
			{
				OptionInstance opt = options.get(name);
				if (opt != null)
				{
					try
					{
						if (component instanceof JTextField textField)
							opt.load(textField.getText());
						else if (component instanceof JComboBox comboBox)
							opt.set(comboBox.getSelectedItem());
						else if (component instanceof JSpinner spinner)
							opt.set(spinner.getValue());
					}
					catch (Exception ex)
					{}
				}
			});
			NightreignTimer.settings.save();
			this.frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
		});
		gui.button("Cancel", gui.right(saveButton), saveButton.getY(), buttonWidth, buttonHeight, b ->
		{
			this.frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
		});
	}
}
