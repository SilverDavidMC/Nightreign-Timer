package com.silver_david.nightreign_timer.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.silver_david.nightreign_timer.NightreignTimer;
import com.silver_david.nightreign_timer.gui.GuiGraphics.StringProperties;
import com.silver_david.nightreign_timer.util.RelativeRegion;

public class TimerPanel extends JPanel implements GuiEventListener
{
	private static final long serialVersionUID = 1L;
	List<Button> buttons = new ArrayList<Button>();

	static boolean timerRunning = false;
	static long startTime = 0L;
	static CircleState state = CircleState.NIGHT_BEGINS;
	static int day = 0;

	static BufferedImage lastScreenshot = null;
	static String detectedText = "";
	public static Thread photoThread = new Thread(() ->
	{
		while (NightreignTimer.settings.autoDetectDayStart.get())
		{
			if (!timerRunning)
			{
				day = detectDay();
				if (day > 0 && NightreignTimer.settings.startDayOnImageDetection.get())
					start();
			}
			else
			{
				clearData();
			}

			try
			{
				Thread.sleep(100L);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	});

	public TimerPanel(int x, int y, int width, int height)
	{
		this.setBounds(x, y, width, height);

		int centerX = x + width / 2;
		int startButtonWidth = 200, startButtonHeight = 80;
		buttons.add(new StartButton("Start", centerX - startButtonWidth / 2, height - startButtonHeight - 10, startButtonWidth, startButtonHeight));

		if (NightreignTimer.settings.autoDetectDayStart.get())
			photoThread.start();
	}

	public static void start()
	{
		if (NightreignTimer.settings.showDebug.get())
			playWarningSound();
		startTime = System.currentTimeMillis();
		timerRunning = true;
	}

	public static void stop()
	{
		timerRunning = false;
		clearData();
	}

	public static void clearData()
	{
		day = 0;
		lastScreenshot = null;
		detectedText = "";
	}

	public int getSeconds()
	{
		if (timerRunning)
			return (int) (System.currentTimeMillis() - startTime) / 1000;
		return 0;
	}

	public int getMaxSeconds()
	{
		return CircleState.values()[CircleState.values().length - 1].seconds;
	}

	@Override
	public void refresh(int mouseX, int mouseY)
	{
		int seconds = this.getSeconds();
		CircleState newState = CircleState.getCurrentState(seconds);
		if (newState != state && newState != CircleState.NIGHT_BEGINS)
		{
			if (state.playWarningSound)
				playWarningSound();
			state = newState;
		}
		if (seconds > this.getMaxSeconds())
			stop();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		// Setup and background
		GuiGraphics g = new GuiGraphics((Graphics2D) graphics, this.getWidth(), this.getHeight());
		Point mousePos = Gui.getRelativeMousePos(this);
		int mouseX = mousePos.x, mouseY = mousePos.y;
		int w = g.getWidth(), h = g.getHeight();
		g.fillRect(new Color(120, 110, 120), 0, 0, w, h);
		g.drawImage("background", 0, 0);
		if (NightreignTimer.settings.showDebug.get() && lastScreenshot != null)
			g.drawImage(lastScreenshot, g.width - lastScreenshot.getWidth() / 2, 0, lastScreenshot.getWidth() / 2, lastScreenshot.getHeight() / 2);

		// Timer
		int maxSeconds = this.getMaxSeconds();
		int seconds = Math.min(this.getSeconds(), maxSeconds);
		g.drawString(Time.from(seconds).toString(), g.getCenterX(), g.getCenterY() - 40, g.string().centered().fontSize(80));

		String eventText = "";
		if (timerRunning)
		{
			if (seconds < maxSeconds)
				eventText = CircleState.getUpcomingEvent(seconds);
		}
		else
		{
			if (NightreignTimer.settings.autoDetectDayStart.get())
				eventText = "Auto detection on\nKeep the map closed!";
		}
		g.drawString(eventText, g.getCenterX(), g.getCenterY() + 60, g.string().centered().fontSize(28));

		if (NightreignTimer.settings.showDebug.get())
			g.drawString(String.format("Read Text: %s \nDay: %d", detectedText, day), 3, 15, g.string().color(Color.ORANGE).fontSize(12));

		// Buttons
		for (Button b : this.buttons)
			b.repaint(mouseX, mouseY, g);
	}

	@Override
	public void mouseClicked(int x, int y, int click)
	{
		for (Button b : this.buttons)
			if (b.isMouseOver(x, y))
				b.onClick(x, y);
	}

	public static void playWarningSound()
	{
		if (NightreignTimer.settings.volume.get() > 0)
			GuiGraphics.playSound(NightreignTimer.settings.warningSoundFile.get());
	}

	private static int detectDay()
	{
		try
		{
			Robot robot = new Robot();
			Rectangle screen;
			if (GraphicsEnvironment.isHeadless())
				screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
			else
				screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[NightreignTimer.settings.monitorToWatch.get()].getConfigurations()[0].getBounds();
			RelativeRegion region = NightreignTimer.settings.areaToWatch.get();
			BufferedImage img = robot.createScreenCapture(new Rectangle(screen.x + (int) (screen.width * region.x()), screen.y + (int) (screen.height * region.y()), (int) (screen.width * region.w()), (int) (screen.height * region.h())));
			//RescaleOp rescaleOp = new RescaleOp(1.2F, 0, null);
			RescaleOp rescaleOp = new RescaleOp(NightreignTimer.settings.contrastBrightness.get(), NightreignTimer.settings.contrastDarkness.get(), null);
			rescaleOp.filter(img, img);
			int maskThreshold = 250;
			for (int x = 0; x < img.getWidth(); x++)
			{
				for (int y = 0; y < img.getHeight(); y++)
				{
					Color c = new Color(img.getRGB(x, y));
					if (c.getRed() >= maskThreshold && c.getGreen() >= maskThreshold && c.getBlue() >= maskThreshold)
						img.setRGB(x, y, Color.WHITE.getRGB());
					else
						img.setRGB(x, y, Color.BLACK.getRGB());
				}
			}
			lastScreenshot = img;
			detectedText = NightreignTimer.readText(img).toUpperCase().replace(" ", "");
			if (detectedText.contains(NightreignTimer.settings.day3Text.get().toUpperCase().replace(" ", "")))
				return 0; // Day 3 shouldn't start the timer, so it needs to be cut out here
			if (detectedText.contains(NightreignTimer.settings.day2Text.get().toUpperCase().replace(" ", "")))
				return 2;
			if (detectedText.contains(NightreignTimer.settings.day1Text.get().toUpperCase().replace(" ", "")))
				return 1;
			return 0;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return 0;
	}

	public static enum CircleState
	{
		NIGHT_BEGINS(0, "", false),
		WARN_FIRST(250, "", true),
		FIRST_STARTS(270, "First circle starts in %s", false),
		FIRST_ENDS(450, "", false),
		WARN_SECOND(640, "", true),
		SECOND_STARTS(660, "Second circle starts in %s", false),
		SECOND_ENDS(840, "Night ends in %s", false);

		/*NIGHT_BEGINS(0, "", false),
		SECOND_STARTS(5, "Second circle starts in %s", true),
		SECOND_ENDS(10, "Night ends in %s", false);*/

		final int seconds;
		final String prompt;
		final boolean playWarningSound;

		CircleState(int seconds, String prompt, boolean playWarningSound)
		{
			this.seconds = seconds;
			this.prompt = prompt;
			this.playWarningSound = playWarningSound;
		}

		public static CircleState getCurrentState(int seconds)
		{
			for (var s : values())
				if (seconds < s.seconds)
					return s;
			return values()[0];
		}

		public static String getUpcomingEvent(int seconds)
		{
			CircleState state = getCurrentState(seconds);
			CircleState[] vals = values();
			for (int i = state.ordinal(); i < vals.length; i++)
			{
				var s = vals[i];
				if (!s.prompt.isBlank())
				{
					return String.format(s.prompt, Time.from(s.seconds - seconds).toString());
				}
			}
			return "";
		}
	}

	class StartButton extends Button
	{
		public StartButton(String name, int x, int y, int width, int height)
		{
			super(name, x, y, width, height);
		}

		@Override
		public void onClick(int mouseX, int mouseY)
		{
			if (TimerPanel.timerRunning)
				TimerPanel.stop();
			else
				TimerPanel.start();
		}

		@Override
		public void repaint(int mouseX, int mouseY, GuiGraphics g)
		{
			g.fillRect(this.isMouseOver(mouseX, mouseY) ? NightreignTimer.SELECTED_COLOR : NightreignTimer.UNSELECTED_COLOR, x, y, width, height);
			StringProperties props = g.string().centered().fontSize(40);
			String text = TimerPanel.timerRunning ? "Stop" : "Start";
			g.drawString(text, this.getCenter().x, this.getCenter().y + g.getHeight(text, props) / 3, props);
		}
	}

	static record Time(int minute, int seconds)
	{
		public static Time from(int seconds)
		{
			return new Time(seconds / 60, seconds % 60);
		}

		public int toSeconds()
		{
			return minute * 60 + seconds;
		}

		@Override
		public final String toString()
		{
			return String.format("%02d:%02d", this.minute, this.seconds);
		}
	}
}
