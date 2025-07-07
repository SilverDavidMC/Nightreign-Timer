package com.silver_david.nightreign_timer.gui;

import java.awt.Point;

public abstract class Button
{
	int x, y, width, height;
	String name;
	
	public Button(String name, int x, int y, int width, int height)
	{
		this.name = name;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	public abstract void repaint(int mouseX, int mouseY, GuiGraphics g);
	
	public abstract void onClick(int mouseX, int mouseY);
	
	public boolean isMouseOver(int mouseX, int mouseY)
	{
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}
	
	public Point getCenter()
	{
		return new Point(x + width / 2, y + height / 2);
	}
	
	@Override
	public String toString()
	{
		return String.format("%s: x=%d, y=%d, width=%d, height=%d", name, x, y, width, height);
	}
}
