package com.axnsan.airplanes;

import com.axnsan.airplanes.util.ActionResolver;
import com.axnsan.airplanes.util.JavaXmlParser;
import com.axnsan.airplanes.util.TTFFontManager;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class Main implements ActionResolver {
	public static void main(String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "Airplanes";
		cfg.useGL20 = false;
		cfg.width = 320;
		cfg.height = 480;
		cfg.addIcon("data/airplanes-128.png", Files.FileType.Internal);
		cfg.addIcon("data/airplanes-32.png", Files.FileType.Internal);
		cfg.addIcon("data/airplanes-16.png", Files.FileType.Internal);
		
		Airplanes a =	new Airplanes(new TTFFontManager(), new JavaXmlParser(), new Main());
		Application app = new LwjglApplication(a, cfg);
		Airplanes.application = app;
	}

	@Override
	public void showShortToast(CharSequence toastMessage) {
		System.out.println("Short toast: " + toastMessage);
	}

	@Override
	public void showLongToast(CharSequence toastMessage) {
		System.out.println("Long toast: " + toastMessage);
	}

	@Override
	public void showAlertBox(String alertBoxTitle, String alertBoxMessage,
			String alertBoxButtonText) {
		System.out.println("Alert box: " + alertBoxTitle + ", " + alertBoxMessage + ", " + alertBoxButtonText);
	}

	@Override
	public void showProgressDialog(String text) {
		System.out.println("Show progress dialog: " + text);
	}

	@Override
	public void dismissProgressDialog() {
		System.out.println("Dismiss progress dialog");
	}

	@Override
	public void openUri(String uri) {
		System.out.println("Open URL: " + uri);
	}
}
