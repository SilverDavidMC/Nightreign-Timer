package com.silver_david.nightreign_timer.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.silver_david.nightreign_timer.NightreignTimer;

public class GuiGraphics
{
	final Graphics2D graphics;
	Supplier<Integer> fontSize;
	Supplier<String> fontFamily;
	Color textColor = Color.black;
	final int width, height;
	int xOffset = 0, yOffset = 0;

	public GuiGraphics(Graphics2D graphics, int width, int height)
	{
		this.graphics = graphics;
		this.fontSize = this.graphics.getFont()::getSize;
		this.fontFamily = this.graphics.getFont()::getFamily;
		this.width = width;
		this.height = height;
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	public static GuiGraphics ghost()
	{
		BufferedImage i = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		return new GuiGraphics(i.createGraphics(), 1, 1);
	}

	public void translate(double xOffset, double yOffset)
	{
		this.graphics.translate(xOffset, yOffset);
		this.xFromZero += xOffset;
		this.yFromZero += yOffset;
	}

	double xFromZero = 0, yFromZero = 0;

	public void setOffset(double xOffset, double yOffset)
	{
		this.graphics.translate(-xFromZero + xOffset, -yFromZero + yOffset);
		this.xFromZero = xOffset;
		this.yFromZero = yOffset;
	}

	public int getWidth()
	{
		return this.width;
	}

	public int getCenterX()
	{
		return this.width / 2;
	}

	public int getHeight()
	{
		return this.height;
	}

	public int getCenterY()
	{
		return this.height / 2;
	}

	public void setFontSize(int fontSize)
	{
		this.fontSize = () -> fontSize;
	}

	public void setTextColor(Color color)
	{
		this.textColor = color;
	}

	public void setFont(String font)
	{
		this.fontFamily = () -> font;
	}

	/**
	 * @return The number of lines not drawn
	 */
	public int drawString(String str, int x, int y, StringProperties props)
	{
		int maxLine = props.maxLineWidth;

		List<String> lines = new ArrayList<>();
		Iterator<String> paragraphs = Stream.of(str.split("\n")).iterator();
		while (paragraphs.hasNext())
		{
			String paragraph = paragraphs.next();
			if (maxLine > -1)
			{
				if (paragraph.isBlank())
				{
					lines.add(paragraph);
				}
				else
				{
					String[] words = Stream.of(paragraph.split(" ")).filter(s -> !s.isBlank()).map(String::trim).toArray(String[]::new);
					int wordIndex = 0;
					while (wordIndex < words.length)
					{
						String line = words[wordIndex];
						wordIndex++;
						for (int w = wordIndex; w < words.length; w++)
						{
							String combinedLine = line + " " + words[w];
							// Word can fit on the line, add it
							if (this.getWidth(combinedLine, props) <= maxLine)
							{
								wordIndex++;
								line = combinedLine;
							}
							// Word can't fit, end this line and let a new one start
							else
								break;
						}
						lines.add(line);
					}
				}
			}
			else
			{
				lines.add(paragraph);
			}
		}

		int lineCount = lines.size();
		int drawnLines = 0;
		graphics.setFont(props.font.get());
		graphics.setColor(props.color.get());
		int maxLines = props.maxLines - 1;
		for (int i = 0; i < lineCount; i++)
		{
			boolean isLastLine = i == maxLines && lineCount - 1 > maxLines;
			String line = lines.get(i);
			int lineOffset = props.alignment.align(this, props, line);
			int lineX = x + this.xOffset + lineOffset;
			int lineY = y + this.yOffset + i * this.getHeight(line, props);
			graphics.drawString(isLastLine ? props.finalLine : line, lineX, lineY);
			if (props.underline)
				graphics.fillRect(lineX, lineY + 1, this.getWidth(line, props), 1);
			if (!isLastLine)
				drawnLines++;
			else
				break;
		}
		return lineCount - drawnLines;
	}

	public StringProperties string()
	{
		return new StringProperties(this);
	}

	public void fillRect(Color color, int x, int y, int width, int height)
	{
		graphics.setColor(color);
		graphics.fillRect(x + this.xOffset, y + this.yOffset, width, height);
	}

	private static final Set<String> KNOWN_ERRORED_IMAGES = new HashSet<>();

	public static BufferedImage loadImage(String fileName)
	{
		try
		{
			// Load from root directory
			return loadImageInternal("src/resources/" + fileName, false);
		}
		catch (Exception e)
		{
			try
			{
				// Load from packaged jar
				return loadImageInternal("/src/resources/" + fileName, true);
			}
			catch (IllegalArgumentException e1)
			{}
			catch (IIOException e2)
			{
				// Couldn't find the image
			}
			catch (Exception e3)
			{}
		}
		return MISSING_TEXTURE;
	}

	static final String PNG = "png", PNG_EXTENSION = "." + PNG;

	private static BufferedImage loadImageInternal(String fileName, boolean packaged) throws IOException
	{
		int extensionIndex = fileName.lastIndexOf('.');
		String extension = extensionIndex >= 0 ? fileName.substring(extensionIndex + 1) : "";
		if (extension.isBlank())
			fileName = fileName + PNG_EXTENSION;
		else if (!extension.equals(PNG))
			throw new IIOException(fileName + " is not a " + PNG);
		
		BufferedImage image;
		if (packaged)
		{
			InputStream stream = GuiGraphics.class.getResourceAsStream(fileName);
			image = ImageIO.read(stream);
		}
		else
		{
			image = ImageIO.read(new File(fileName));
		}
		KNOWN_ERRORED_IMAGES.remove(fileName);
		return image;
	}

	public static final BufferedImage MISSING_TEXTURE = missingTexture();

	private static BufferedImage missingTexture()
	{
		int w = 2, h = w / 2;
		var image = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
		GuiGraphics graphics = new GuiGraphics(image.createGraphics(), w, w);
		graphics.fillRect(Color.magenta, 0, 0, w, w);
		graphics.fillRect(Color.black, 0, 0, h, h);
		graphics.fillRect(Color.black, h, h, h, h);
		return image;
	}

	public void drawImage(String image, int x, int y, int width, int height)
	{
		this.drawImage(loadImage(image), x, y, width, height);
	}
	
	public void drawImage(BufferedImage image, int x, int y, int width, int height)
	{
		graphics.drawImage(image, x + this.xOffset, y + this.yOffset, width, height, null);
	}

	public void drawImage(String image, int x, int y)
	{
		drawImage(image, x, y, this.width, this.height);
	}

	public static void playSound(String fileName)
	{
		try
		{
			// Load from root directory
			playSoundInternal("src/resources/" + fileName, false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			try
			{
				// Load from packaged jar
				playSoundInternal("/src/resources/" + fileName, true);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	static final String WAV = "wav", WAV_EXTENSION = "." + WAV;

	private static void playSoundInternal(String fileName, boolean packaged) throws UnsupportedAudioFileException, IOException, LineUnavailableException
	{
		int extensionIndex = fileName.lastIndexOf('.');
		String extension = extensionIndex >= 0 ? fileName.substring(extensionIndex + 1) : "";
		if (extension.isBlank())
			fileName = fileName + WAV_EXTENSION;
		else if (!extension.equals(WAV))
			throw new UnsupportedAudioFileException(fileName + " is not a " + WAV);

		AudioInputStream audioStream;
		if (packaged)
		{
			BufferedInputStream stream = new BufferedInputStream(GuiGraphics.class.getResourceAsStream(fileName));
			audioStream = AudioSystem.getAudioInputStream(stream);
		}
		else
		{
			audioStream = AudioSystem.getAudioInputStream(new File(fileName));
		}
		Clip clip = AudioSystem.getClip();
		clip.open(audioStream);
		FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		gainControl.setValue(-(50 - NightreignTimer.settings.volume.get()));
		clip.start();
	}

	public Font getFont()
	{
		return graphics.getFont();
	}

	public int getWidth(String str, StringProperties props)
	{
		return (int) getBounds(str, props).getWidth();
	}

	public int getHeight(String str, StringProperties props)
	{
		return (int) getBounds(str, props).getHeight();
	}

	public Rectangle2D getBounds(String str, StringProperties props)
	{
		return graphics.getFontMetrics(props.font.get()).getStringBounds(str, graphics);
	}

	public static final int ONE_INCH = 72;

	public static int inchesToPixels(double inches)
	{
		return (int) (ONE_INCH * inches);
	}

	public static double pixelsToInches(int pixels)
	{
		return pixels / (double) ONE_INCH;
	}

	public void setScale(double x, double y)
	{
		this.graphics.scale(x, y);
	}

	public Color brighter(Color color, float brightnessFactor)
	{
		return new Color(brighter(color.getRed(), brightnessFactor), brighter(color.getGreen(), brightnessFactor), brighter(color.getBlue(), brightnessFactor));
	}

	public int brighter(int colorChannel, float brightnessFactor)
	{
		return (int) ((colorChannel * (1.0F - brightnessFactor)) + (255 * brightnessFactor));
	}

	public static class StringProperties
	{
		public final GuiGraphics graphics;
		public Supplier<Font> font;
		public Supplier<Color> color;
		public Alignment alignment = Alignment.LEFT;
		public int maxLineWidth = -1, maxLines = -1;
		public String finalLine = "...";
		boolean underline = false;

		private Optional<Color> textColor = Optional.empty();
		private Optional<String> fontFamily = Optional.empty();
		private Optional<Integer> fontSize = Optional.empty();

		private StringProperties(GuiGraphics graphics)
		{
			this.graphics = graphics;
			this.font = () ->
			{
				Font baseFont = graphics.graphics.getFont();
				return new Font(this.fontFamily.orElse(graphics.fontFamily.get()), baseFont.getStyle(), this.fontSize.orElse(graphics.fontSize.get()));
			};
			this.color = () ->
			{
				Color baseColor = graphics.textColor;
				return this.textColor.orElse(baseColor);
			};
		}

		public StringProperties copy()
		{
			var ret = new StringProperties(graphics);
			ret.color = color;
			ret.alignment = alignment;
			ret.maxLineWidth = maxLineWidth;
			ret.maxLines = maxLines;
			ret.fontFamily = fontFamily;
			ret.fontSize = fontSize;
			ret.underline = underline;
			return ret;
		}

		public StringProperties color(Color color)
		{
			this.textColor = Optional.ofNullable(color);
			return this;
		}

		public StringProperties centered()
		{
			return alignment(Alignment.CENTERED);
		}

		public StringProperties alignment(Alignment alignment)
		{
			this.alignment = alignment;
			return this;
		}

		public StringProperties maxLineWidth(int maxWidth)
		{
			this.maxLineWidth = maxWidth;
			return this;
		}

		public StringProperties maxLines(int maxLines)
		{
			return this.maxLines(maxLines, ". . .");
		}

		public StringProperties maxLines(int maxLines, String finalLine)
		{
			this.maxLines = maxLines;
			this.finalLine = finalLine;
			return this;
		}

		public StringProperties font(String font)
		{
			this.fontFamily = Optional.of(font);
			return this;
		}

		public StringProperties fontSize(int size)
		{
			this.fontSize = Optional.of(size);
			return this;
		}

		public StringProperties underline()
		{
			this.underline = true;
			return this;
		}

		public static enum Alignment
		{
			RIGHT
			{
				@Override
				int align(GuiGraphics g, StringProperties props, String str)
				{
					return -g.getWidth(str, props);
				}
			},
			CENTERED
			{
				@Override
				int align(GuiGraphics g, StringProperties props, String str)
				{
					return -g.getWidth(str, props) / 2;
				}
			},
			LEFT
			{
				@Override
				int align(GuiGraphics g, StringProperties props, String str)
				{
					return 0;
				}
			};

			abstract int align(GuiGraphics g, StringProperties props, String str);
		}
	}
}
