package com.axnsan.airplanes.util;

public class ActionManager {
	private static ActionResolver action;
	public static void initialize(ActionResolver action) {
		ActionManager.action = action;
	}
	public static void showShortToast(CharSequence toastMessage) {
		action.showShortToast(toastMessage);
	}
	public static  void showLongToast(CharSequence toastMessage) {
		action.showLongToast(toastMessage);
	}
    public static void showAlertBox(String alertBoxTitle, String alertBoxMessage, String alertBoxButtonText) {
    	action.showAlertBox(alertBoxTitle, alertBoxMessage, alertBoxButtonText);
    }
    public static void showProgressDialog(String text) {
    	action.showProgressDialog(text);
    }
    public static  void dismissProgressDialog() {
    	action.dismissProgressDialog();
    }
    public static void openUri(String uri) {
    	action.openUri(uri);
    }
}
