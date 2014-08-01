package com.axnsan.airplanes.util;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public class TTFFontManager implements FontManagerInterface {
	private SortedMap<Integer, BitmapFont> fonts = new TreeMap<Integer, BitmapFont>(Collections.reverseOrder());
	private boolean init = false;
	private final Integer sizes[] = { 15, 16, 17, 18, 20, 22, 25, 30, 35, 42, 55 }, defaultSize = 15;
	private BitmapFont defaultFont;
	
	/**Initialize the font manager*/
	private void init() {
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("data/segoeui.ttf"));
		FreeTypeFontParameter par = new FreeTypeFontParameter();
		par.characters = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM" +
				"ĂÎȘȚÂăîâșț -;'.[]\\(),?!\"/@#$%^&*1234567890<>_:+";
		for (Integer size : sizes) {
			par.size = size;
			BitmapFont font = generator.generateFont(par);
			font.setColor(Color.BLACK);
			fonts.put(size, font);
		}
		par.size = defaultSize;
		defaultFont = generator.generateFont(par);
		generator.dispose();
		init = true;
	}
	
	/**Returns the largest font that is smaller than the given size*/
	@Override
	public BitmapFont getFontForHeight(Integer size) {
		if (!init)
			init();
		
		for(SortedMap.Entry<Integer, BitmapFont> entry : fonts.entrySet()) {
			if (size >= entry.getKey()) {
				BitmapFont ret = entry.getValue();
				ret.setColor(Color.BLACK);
				return ret;
			}
		}
		
		return defaultFont;
	}
	
	@Override
	public BitmapFont getFontForHeight(float size) {
		return getFontForHeight(new Integer((int)size));
	}
	
	/**Dispose of the generated fonts*/
	@Override
	public void dispose() {
		for(SortedMap.Entry<Integer, BitmapFont> entry : fonts.entrySet()) {
			BitmapFont font = entry.getValue();
			if (font != null)
				font.dispose();
		}
		fonts.clear();
		if (defaultFont != null)
			defaultFont.dispose();
		defaultFont = null;
		init = false;
	}
}
