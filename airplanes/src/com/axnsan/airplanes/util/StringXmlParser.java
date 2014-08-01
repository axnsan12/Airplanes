package com.axnsan.airplanes.util;

import java.util.Map;

public interface StringXmlParser {
	public Map<String, String> loadStringsForLocale(String locale, String STRING_FILE);
	public Map<String, String> loadStringsForCurrentLocale(String STRING_FILE);
	public String getCurrentLocale();
}
