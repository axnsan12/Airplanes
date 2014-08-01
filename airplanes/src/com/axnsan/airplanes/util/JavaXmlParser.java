package com.axnsan.airplanes.util;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class JavaXmlParser implements StringXmlParser {
	
	@Override
	public Map<String, String> loadStringsForLocale(String locale, String STRING_FILE) {
		HashMap<String, String> ret = new HashMap<String, String>();
		
		try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            FileHandle fileHandle = Gdx.files.internal(STRING_FILE);
            Document doc = db.parse(fileHandle.read());
           
            Element root = doc.getDocumentElement();
            NodeList languages = root.getElementsByTagName("language");
            if (languages.getLength() <= 0)
            	throw new RuntimeException("The " + STRING_FILE + " file must contain one or more " +
            			"language elements");
            
            for (int i = 0; i < languages.getLength(); ++i) {                
                Node lang = languages.item(i);
                
                if (lang.getAttributes().getNamedItem("name").getTextContent().equals(locale)) 
                {
                	
                    Element langRoot = (Element) lang;
                    NodeList strings = langRoot.getElementsByTagName("string");
                    
                    for (int j = 0;j < strings.getLength();++j) {
                    	Node string = strings.item(j);
                    	
                    	String name = string.getAttributes().getNamedItem("name").getTextContent();
                    	String value = string.getTextContent();
                    	
                    	ret.put(name, value);
                    }
                }
            }
		} catch (Exception e) {
			throw new RuntimeException("Error parsing strings XML from file " + STRING_FILE + " for locale " + locale);
		}
		
		return ret;
	}

	@Override
	public Map<String, String> loadStringsForCurrentLocale(String STRING_FILE) {
		return loadStringsForLocale(getCurrentLocale(), STRING_FILE);
	}

	@Override
	public String getCurrentLocale() {
		return java.util.Locale.getDefault().toString().substring(0, 5);
	}
}
