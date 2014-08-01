package com.axnsan.airplanes.util;

import java.util.Map;

public class StringManager {
	private static final String STRING_FILE = "data/strings.xml";
	private static Map<String, String> defaultStrings = null;
	private static Map<String, String> localStrings = null;
	private static boolean init = false;
	private static String locale = "default";
	private static StringXmlParser xml = null;
	
	public static String test;
	public static void initialize(StringXmlParser xml)
	{
		StringManager.xml = xml;
	}

	private static void init() 
	{
		//Load the default values for strings
		defaultStrings = xml.loadStringsForLocale("default", STRING_FILE);
		
		//Then load the current system locale
		localStrings = xml.loadStringsForCurrentLocale(STRING_FILE);
		locale = xml.getCurrentLocale();
		init = true;
	}
	
	public static void setLocale(String locale) {
		if (xml == null)
			throw new RuntimeException("Must call initialize() before using this function");
		
		if (!init)
			init();
		
		if (!locale.equals(StringManager.locale)) {
			localStrings = xml.loadStringsForLocale(locale, STRING_FILE);
			StringManager.locale = locale;
		}
	}
	
	
	
	public static String getString(String name) {
		if (xml == null)
			throw new RuntimeException("Must call initialize() before using this function");
		
		if (!init)
			init();
		
		if (localStrings.containsKey(name))
			return localStrings.get(name);
		
		if (defaultStrings.containsKey(name))
			return defaultStrings.get(name);
		
		return name;
	}
	
	private StringManager() {}
}
