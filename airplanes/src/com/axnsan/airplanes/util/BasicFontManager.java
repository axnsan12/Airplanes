package com.axnsan.airplanes.util;

import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class BasicFontManager implements FontManagerInterface {
	private BitmapFont defaultFont = null;
	
	/**Initialize the font manager*/
	private void init() {
		defaultFont = new BitmapFont();
	}
	
	@Override
	public BitmapFont getFontForHeight(Integer size) {
		if (defaultFont == null)
			init();
		
		return defaultFont;
	}

	@Override
	public BitmapFont getFontForHeight(float size) {
		if (defaultFont == null)
			init();
		
		return defaultFont;
	}

	@Override
	public void dispose() {
		if (defaultFont != null)
			defaultFont.dispose();
		defaultFont = null;
	}

}
