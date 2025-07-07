package com.silver_david.nightreign_timer.gui;

public interface GuiEventListener
{
	default void mouseClicked(int x, int y, int clickType)
	{
	}

	default int autoRepaintTimer()
	{
		return 10;
	}

	default void refresh(int mouseX, int mouseY)
	{
	}
}
