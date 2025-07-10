package com.silver_david.nightreign_timer.gui;

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import com.silver_david.nightreign_timer.NightreignTimer;

public interface GuiFrame
{
	JFrame getFrame();

	JMenuBar getMenuBar();

	void onClose();

	Point getLaunchPosition();
	
	Dimension getDefaultSize();

	default boolean isResizable()
	{
		return false;
	}
	
	void initGui(Gui gui, int width, int height, int centerX, int centerY);

	default Class<?> getGuiClass()
	{
		return this.getClass();
	}

	public static interface Child extends GuiFrame
	{
		GuiFrame getParent();

		@Override
		default void onClose()
		{
			if (NightreignTimer.instance != null)
				NightreignTimer.instance.removeChild(this);
		}

		@Override
		default Point getLaunchPosition()
		{
			JFrame parent = getParent().getFrame();
			Point center = new Point(parent.getX() + parent.getWidth() / 2, parent.getY() + parent.getHeight() / 2);
			JFrame frame = this.getFrame();
			return new Point(center.x - frame.getWidth() / 2, center.y - frame.getHeight() / 2);
		}
	}
}
