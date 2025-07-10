package com.silver_david.nightreign_timer.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
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
	public Dimension getDefaultSize()
	{
		return null;
	}

	@Override
	public GuiFrame getParent()
	{
		return NightreignTimer.instance;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void initGui(Gui gui, int width, int height, int centerX, int centerY)
	{
		components.clear();
		int i = 0;
		int labelX = 0, labelY = 40, labelWidth = 200, labelHeight = 30;
		int inputWidth = 300;
		int margin = 10;
		for (var entry : options.entrySet())
		{
			String name = entry.getKey();
			OptionInstance<?> option = entry.getValue();
			Optional<Runnable> testAction = option.getTestAction();
			int y = labelY * i;
			JLabel label;
			if (testAction.isPresent())
			{
				int testButtonWidth = 60;
				label = gui.label(Util.capitalize(name), labelX, y, labelWidth - testButtonWidth - margin, labelHeight);
				gui.button("Test", label.getX() + label.getWidth() + margin, label.getY(), testButtonWidth, labelHeight, b ->
				{
					OptionInstance optionInst =  this.options.get(name);
					Object oldVal = optionInst.get();
					this.setOptionFromComponent(name, components.get(name));
					testAction.get().run();
					optionInst.set(oldVal);
				});
			}
			else
			{
				label = gui.label(Util.capitalize(name), labelX, y, labelWidth, labelHeight);
			}
			label.setOpaque(true);
			label.setBackground(Color.LIGHT_GRAY);
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setToolTipText(option.comment());
			if (!option.possibleValues().isPresent())
			{
				components.put(name, gui.textInput(option.save(), labelX + labelWidth + margin, y, inputWidth, labelHeight));
			}
			else
			{
				if (option.isNumber())
				{
					if (option.getFirst().get() instanceof Integer)
					{
						int sLabelWidth = 40;
						var sliderLabel = gui.label(option.get().toString(), labelX + labelWidth + margin, y, sLabelWidth, labelHeight);
						sliderLabel.setBackground(Color.WHITE);
						sliderLabel.setOpaque(true);
						sliderLabel.setHorizontalAlignment(SwingConstants.CENTER);
						var slider = gui.slider((Integer) option.getFirst().get(), (Integer) option.getLast().get(), (Integer) option.get(), labelX + labelWidth + margin + sLabelWidth + margin, y, inputWidth - sLabelWidth - margin, labelHeight);
						components.put(name, slider);
						slider.addChangeListener(c -> sliderLabel.setText(Integer.toString(slider.getValue())));
					}
				}
				else
				{
					components.put(name, gui.selection(option.possibleValues().get().toArray(), option.get(), labelX + labelWidth + margin, y, inputWidth, labelHeight, c ->
					{
					}));
				}
			}
			i++;
		}

		int buttonWidth = 100, buttonHeight = 30;

		int buttonCount = 2;
		var saveButton = gui.button("Save", ((labelWidth + margin + inputWidth) / 2) - ((buttonCount * buttonWidth) + ((buttonCount - 1) * gui.defaultMargin)) / 2, labelY * i, buttonWidth, buttonHeight, b ->
		{
			components.forEach(this::setOptionFromComponent);
			NightreignTimer.settings.save();
			this.frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
		});
		gui.button("Cancel", gui.right(saveButton), saveButton.getY(), buttonWidth, buttonHeight, b ->
		{
			this.frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
		});
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setOptionFromComponent(String name, Component component)
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
				else if (component instanceof JSlider slider)
					opt.set(slider.getValue());
			}
			catch (Exception ex)
			{}
		}
	}
}
