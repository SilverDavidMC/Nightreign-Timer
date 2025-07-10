package com.silver_david.nightreign_timer.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SpinnerModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;

import com.silver_david.nightreign_timer.NightreignTimer;
import com.silver_david.nightreign_timer.util.Util;

public class Gui
{
	public final JPanel panel;
	public final JMenuBar menuBar;

	final BufferedImage imageForGraphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

	public Gui(JPanel panel, JMenuBar menuBar)
	{
		this.panel = panel;
		this.menuBar = menuBar;
	}

	public static Gui ghost()
	{
		return new Gui(null, null)
		{
			@Override
			public <T extends Component> T add(T component)
			{
				return component;
			}

			@Override
			public void remove(Component component)
			{
			}

			@Override
			public Component[] getComponents()
			{
				return new Component[0];
			}
		};
	}

	public static <T extends GuiFrame> T initFrame(T guiFrame)
	{
		JFrame frame = guiFrame.getFrame();
		JMenuBar menuBar = guiFrame.getMenuBar();

		BufferedImage icon = GuiGraphics.loadSourceImage("icon.png");
		frame.setIconImage(icon);

		if (menuBar != null)
		{
			menuBar.setBackground(NightreignTimer.MENU_COLOR);
			menuBar.setBorderPainted(false);
			frame.setJMenuBar(menuBar);
		}

		UIManager.put("swing.boldMetal", Boolean.FALSE);
		JPanel panel = new JPanel();
		panel.setBackground(NightreignTimer.BG_COLOR);
		panel.setLayout(null);
		BiConsumer<Gui, Dimension> initFunc = (gui, dimension) ->
		{
			int menuHeight = gui.getMenuHeight();
			int width = dimension.width;
			int height = dimension.height + menuHeight;
			for (var c : panel.getComponents())
				c.invalidate();
			panel.removeAll();
			for (var m : panel.getMouseListeners())
				panel.removeMouseListener(m);
			for (var t : gui.repaintTimers)
				t.stop();
			gui.repaintTimers.clear();
			if (menuBar != null)
			{
				for (var c : menuBar.getComponents())
					c.invalidate();
				menuBar.removeAll();
			}

			int margin = 10, xOffset = 25, yOffset = 48;
			int initWidth = width - margin - xOffset;
			int initHeight = height - margin - yOffset;
			guiFrame.initGui(gui, initWidth, initHeight - menuHeight * 2, initWidth / 2, initHeight / 2);
			int maxWidth = 0;
			int maxHeight = 0;
			for (var comp : panel.getComponents())
			{
				int x = comp.getX(), y = comp.getY(), w = comp.getWidth(), h = comp.getHeight();
				comp.setBounds(x + margin, y + margin, w, h);
				maxWidth = Math.max(maxWidth, x + w);
				maxHeight = Math.max(maxHeight, y + h);
			}
			panel.setSize(maxWidth + margin + xOffset, maxHeight + margin + yOffset);
			if (menuBar != null)
				menuBar.revalidate();

			frame.toFront();
			frame.requestFocus();
		};

		Gui gui = new Gui(panel, menuBar);
		Dimension defaultSize = guiFrame.getDefaultSize();
		if (defaultSize == null)
			defaultSize = new Dimension(0, 0);
		initFunc.accept(gui, defaultSize);
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				guiFrame.onClose();
			}
		});
		frame.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				if (guiFrame.isResizable())
					initFunc.accept(gui, e.getComponent().getSize());
			}
		});
		frame.add(panel);
		frame.setAlwaysOnTop(NightreignTimer.settings.pinToTop.get());
		frame.setResizable(guiFrame.isResizable());
		frame.setMinimumSize(new Dimension(panel.getWidth(), panel.getHeight() + gui.getMenuHeight() * 2));
		panel.setMinimumSize(new Dimension(panel.getWidth(), panel.getHeight() + gui.getMenuHeight() * 2));
		frame.setSize(panel.getWidth(), panel.getHeight() + gui.getMenuHeight() * 2);
		Point launchPos = guiFrame.getLaunchPosition();
		if (launchPos != null)
			frame.setLocation(launchPos);

		return guiFrame;
	}

	public int getMenuHeight()
	{
		return this.menuBar != null ? this.menuBar.getHeight() : 0;
	}

	public static Point getRelativeMousePos(Component component)
	{
		Point mousePos = MouseInfo.getPointerInfo().getLocation();
		if (!component.isShowing())
			return new Point(0, 0);
		Point compPoint = component.getLocationOnScreen();
		return new Point(mousePos.x - compPoint.x, mousePos.y - compPoint.y);
	}

	public void remove(Component component)
	{
		this.panel.remove(component);
	}

	public Component[] getComponents()
	{
		return this.panel.getComponents();
	}

	public List<Timer> repaintTimers = new ArrayList<>();

	public <T extends Component> T add(T component)
	{
		this.panel.add(component);
		if (component instanceof GuiEventListener listener)
		{
			int x = component.getX(), y = component.getY();
			int w = component.getWidth(), h = component.getHeight();

			int repaintTime = listener.autoRepaintTimer();
			if (repaintTime > -1)
			{
				Timer repaintTimer = new Timer(repaintTime, event ->
				{
					Point mousePos = getRelativeMousePos(component);
					listener.refresh(mousePos.x, mousePos.y);
					component.repaint();
				});
				repaintTimer.start();
				repaintTimers.add(repaintTimer);
			}

			panel.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseReleased(MouseEvent e)
				{
					int ex = e.getX(), ey = e.getY();
					if (ex >= x && ex <= w + x && ey >= y && ey <= h + y)
						listener.mouseClicked(ex - x, ey - y, e.getButton());
				}
			});
		}
		return component;
	}

	public <T extends Component> T add(T component, int x, int y, int width, int height)
	{
		component.setBounds(x, y, width, height);
		return add(component);
	}

	public JLabel label(String text, int x, int y, int width, int height)
	{
		var c = new JLabel(text);
		return add(c, x, y, width, height);
	}

	public JLabel labelAbove(String text, Component comp)
	{
		var c = new JLabel(text);
		c.setHorizontalAlignment(JLabel.CENTER);
		c.setVerticalAlignment(JLabel.CENTER);
		var graphics = new GuiGraphics(GraphicsEnvironment.getLocalGraphicsEnvironment().createGraphics(imageForGraphics), 1, 1);
		c.setOpaque(true);
		c.setBackground(Color.LIGHT_GRAY);
		Rectangle2D bounds = graphics.getBounds(text, graphics.string());
		return add(c, comp.getX(), above(comp, 15), (int) bounds.getWidth() + 5, (int) bounds.getHeight());
	}

	public JCheckBox checkBox(String name, int x, int y, int width, int height, Consumer<JCheckBox> onClick)
	{
		var c = new JCheckBox(name);
		c.addActionListener(e -> onClick.accept(c));
		return add(c, x, y, width, height);
	}

	public JSpinner spinner(SpinnerModel model, int x, int y, int width, int height, Consumer<JSpinner> onChange)
	{
		var c = new JSpinner(model);
		c.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				onChange.accept(c);
			}
		});
		c.addMouseWheelListener(new MouseWheelListener()
		{
			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				if (!c.isEnabled())
					return;
				Object val = e.getWheelRotation() > 0 ? c.getPreviousValue() : c.getNextValue();
				if (val != null)
					c.setValue(val);
			}
		});
		return add(c, x, y, width, height);
	}

	public JSlider slider(int min, int max, int value, int x, int y, int width, int height)
	{
		var c = new JSlider(SwingConstants.HORIZONTAL, min, max, value);
		c.setMajorTickSpacing(10);
		c.addMouseWheelListener(new MouseWheelListener()
		{
			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				if (!c.isEnabled())
					return;
				c.setValue(c.getValue() + (e.getWheelRotation() > 0 ? -1 : 1));
			}
		});
		return add(c, x, y, width, height);
	}

	public JTextField textInput(String defaultText, int x, int y, int width, int height)
	{
		var c = new JTextField(defaultText);
		return add(c, x, y, width, height);
	}

	public ParentedGuiElement<JScrollPane, JTextPane> textPane(String defaultText, int x, int y, int width, int height, KeyListener keyListener, Consumer<DocumentEvent> documentListener)
	{
		JTextPane text = new JTextPane();
		if (defaultText != null)
			text.setText(defaultText);
		var undoManager = new UndoManager();
		text.getDocument().addUndoableEditListener(undoManager);
		text.addKeyListener(new KeyListener()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (keyListener != null)
					keyListener.keyPressed(e);

				int key = e.getKeyCode();
				int modifiers = e.getModifiersEx();
				boolean ctrl = (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0;
				if (key == KeyEvent.VK_Z && ctrl && undoManager.canUndo())
					undoManager.undo();
				if (key == KeyEvent.VK_Y && ctrl && undoManager.canRedo())
					undoManager.redo();
			}

			@Override
			public void keyReleased(KeyEvent e)
			{
				if (keyListener != null)
					keyListener.keyReleased(e);
			}

			@Override
			public void keyTyped(KeyEvent e)
			{
				if (keyListener != null)
					keyListener.keyTyped(e);
			}
		});
		if (documentListener != null)
		{
			text.getDocument().addDocumentListener(new DocumentListener()
			{
				@Override
				public void removeUpdate(DocumentEvent e)
				{
					documentListener.accept(e);
				}

				@Override
				public void insertUpdate(DocumentEvent e)
				{
					documentListener.accept(e);
				}

				@Override
				public void changedUpdate(DocumentEvent e)
				{
					documentListener.accept(e);
				}
			});
		}
		JScrollPane scrollPane = add(new JScrollPane(text), x, y, width, height);
		return new ParentedGuiElement<>(scrollPane, text);
	}

	public JButton button(String name, int x, int y, int width, int height, Consumer<JButton> onPress)
	{
		var c = new JButton(name);
		c.addActionListener(e -> onPress.accept(c));
		return add(c, x, y, width, height);
	}

	public <T> JComboBox<T> selection(T[] values, T initialValue, int x, int y, int width, int height, Consumer<JComboBox<T>> onSelect)
	{
		var c = new JComboBox<>(values);
		if (initialValue != null)
			c.setSelectedItem(initialValue);
		c.addActionListener(e -> onSelect.accept(c));
		c.addMouseWheelListener(new MouseWheelListener()
		{
			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				if (!c.isEnabled())
					return;
				c.setSelectedIndex(Util.wrap(c.getSelectedIndex() + (e.getWheelRotation() > 0 ? 1 : -1), 0, c.getModel().getSize() - 1));
			}
		});
		return add(c, x, y, width, height);
	}

	public <T> JComboBox<T> selection(T[] values, int x, int y, int width, int height, Consumer<JComboBox<T>> onSelect)
	{
		return selection(values, null, x, y, width, height, onSelect);
	}

	public JMenu menu(String name)
	{
		var c = new JMenu(name);
		if (this.menuBar != null)
			this.menuBar.add(c);
		return c;
	}

	public JMenuItem menuItem(String name, Runnable onClick)
	{
		var c = new JMenuItem(name);
		c.addActionListener(e -> onClick.run());
		return c;
	}

	public final int defaultMargin = 10;

	public int above(Component component)
	{
		return above(component, defaultMargin);
	}

	public int above(Component component, int margin)
	{
		return component.getY() - margin;
	}

	public int below(Component component)
	{
		return below(component, defaultMargin);
	}

	public int below(Component component, int margin)
	{
		return component.getY() + component.getHeight() + margin;
	}

	public int right(Component component)
	{
		return right(component, defaultMargin);
	}

	public int right(Component component, int margin)
	{
		return component.getX() + component.getWidth() + margin;
	}

	public int bottom()
	{
		return bottom(defaultMargin);
	}

	public int bottom(int margin)
	{
		int lowest = 0;
		for (var comp : this.getComponents())
		{
			int bottom = this.below(comp, margin);
			if (bottom > lowest)
				lowest = bottom;
		}
		return lowest;
	}

	public int farRight()
	{
		return farRight(defaultMargin);
	}

	public int farRight(int margin)
	{
		int farthest = 0;
		for (var comp : this.getComponents())
		{
			int right = this.right(comp, margin);
			if (right > farthest)
				farthest = right;
		}
		return farthest;
	}
}