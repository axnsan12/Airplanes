package com.axnsan.airplanes.util;

import com.badlogic.gdx.graphics.g2d.BitmapFont;

public interface FontManagerInterface {
	/**Returns the largest font that is smaller than the given size*/
	public BitmapFont getFontForHeight(Integer size);
	public BitmapFont getFontForHeight(float size);
	
	
	public void dispose();
}