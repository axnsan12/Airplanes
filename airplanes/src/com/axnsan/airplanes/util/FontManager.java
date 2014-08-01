package com.axnsan.airplanes.util;

import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class FontManager {
	private static FontManagerInterface manager;

	public static void initialize(FontManagerInterface man) {
		dispose();
		manager = man;
	}
	/**Returns the largest font that is smaller than the given size*/
	public static BitmapFont getFontForHeight(Integer size) {
		if (manager == null)
			throw new RuntimeException("Must call initialize before using this function");
		
		return manager.getFontForHeight(size);
	}
	public static BitmapFont getFontForHeight(float size) {
		if (manager == null)
			throw new RuntimeException("Must call initialize before using this function");
		
		return manager.getFontForHeight(size);
	}
	
	public static void dispose() {
		if (manager != null)
			manager.dispose();
		manager = null;
	}
}
