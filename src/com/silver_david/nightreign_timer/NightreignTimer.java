package com.silver_david.nightreign_timer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.lept;
import org.bytedeco.javacpp.lept.PIX;
import org.bytedeco.javacpp.tesseract.TessBaseAPI;

import com.silver_david.nightreign_timer.gui.Gui;
import com.silver_david.nightreign_timer.gui.GuiFrame;
import com.silver_david.nightreign_timer.gui.SettingsScreen;
import com.silver_david.nightreign_timer.gui.TimerPanel;
import com.silver_david.nightreign_timer.util.JsonFile;
import com.silver_david.nightreign_timer.util.Util;

public class NightreignTimer implements GuiFrame
{
	public static final String VERSION = "1.1.0";
	public static NightreignTimer instance;
	private static final List<JsonFile> CONFIG_FILES = new ArrayList<>();
	public static Settings settings = Util.make(Settings::new, CONFIG_FILES::add);
	
	public static final Color BG_COLOR = new Color(67, 67, 153), MENU_COLOR = new Color(104, 104, 153),
			UNSELECTED_COLOR = new Color(BG_COLOR.getRed(), BG_COLOR.getGreen(), BG_COLOR.getBlue(), 80),
			SELECTED_COLOR = new Color(107, 107, 244, 120);
	public static final TessBaseAPI TESS_API = new TessBaseAPI();
	public static BytePointer TESS_OUTPUT = null;

	public static void main(String[] args)
	{
		CONFIG_FILES.forEach(JsonFile::load);
		settings.version.setToDefault();
		instance = new NightreignTimer();
		Gui.initFrame(instance);
	}

	final JFrame frame;
	final JMenuBar menuBar;
	public Map<Class<?>, GuiFrame> children = new HashMap<>();

	NightreignTimer()
	{
		this.frame = new JFrame("Nightreign Timer");
		this.menuBar = new JMenuBar();
	}

	/**
	 * https://github.com/bytedeco/javacpp-presets/tree/master/tesseract
	 * https://github.com/tesseract-ocr/tessdata
	 */
	public static String readText(BufferedImage buffImage)
	{
		if (TESS_API.Init(settings.languageDatasetDirectory.get(), settings.languageDataset.get()) != 0)
		{
			System.err.println("Could not initialize tesseract.");
			System.exit(1);
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try
		{
			ImageIO.write(buffImage, "png", baos);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		byte[] imageInBytes = baos.toByteArray();
		try
		{
			baos.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		PIX image = lept.pixReadMemPng(imageInBytes, imageInBytes.length);
		TESS_API.SetImage(image);

		// Get OCR result
		TESS_OUTPUT = TESS_API.GetUTF8Text();
		lept.pixDestroy(image);
		String output = TESS_OUTPUT.getString();
		TESS_OUTPUT.deallocate();
		TESS_API.End();
		return output;
	}

	@Override
	public JFrame getFrame()
	{
		return frame;
	}

	@Override
	public JMenuBar getMenuBar()
	{
		return menuBar;
	}

	@Override
	public void onClose()
	{
		CONFIG_FILES.forEach(JsonFile::save);
		System.exit(0);
	}

	@Override
	public Point getLaunchPosition()
	{
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Point center = env.getCenterPoint();
		return new Point(center.x - frame.getWidth() / 2, center.y - frame.getHeight() / 2);
	}
	
	@Override
	public Dimension getDefaultSize()
	{
		return new Dimension(450, 450);
	}

	@Override
	public boolean isResizable()
	{
		return true;
	}
	
	@Override
	public void initGui(Gui gui, int width, int height, int centerX, int centerY)
	{
		gui.add(new TimerPanel(0, 0, width, height));

		JMenu fileMenu = gui.menu("File");
		fileMenu.add(gui.menuItem("Settings", () ->
		{
			openSettingsDialog();
		}));
	}

	void openSettingsDialog()
	{
		this.addChild(new SettingsScreen(settings.guiOptions));
	}

	public void addChild(GuiFrame gui)
	{
		GuiFrame existing = this.children.get(gui.getGuiClass());
		if (existing != null)
		{
			existing.getFrame().toFront();
		}
		else
		{
			Gui.initFrame(gui);
			this.children.put(gui.getGuiClass(), gui);
		}
	}

	public void removeChild(GuiFrame gui)
	{
		this.children.remove(gui.getGuiClass());
	}

	@SuppressWarnings("unchecked")
	public <T> T getChild(Class<T> clazz)
	{
		return (T) this.children.get(clazz);
	}
	
	public static void setAlwaysOnTop(boolean alwaysOnTop)
	{
		if (instance != null)
		{
			instance.children.forEach((c, g) -> g.getFrame().setAlwaysOnTop(alwaysOnTop));
			instance.getFrame().setAlwaysOnTop(alwaysOnTop);
		}
	}
	
	public static void showError(Exception e)
	{
		JOptionPane.showMessageDialog(instance.frame, e, "Error", JOptionPane.ERROR_MESSAGE);
	}
}
