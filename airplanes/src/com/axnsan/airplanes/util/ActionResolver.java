package com.axnsan.airplanes.util;

public interface ActionResolver {
	 public void showShortToast(CharSequence toastMessage);
	 public void showLongToast(CharSequence toastMessage);
     public void showAlertBox(String alertBoxTitle, String alertBoxMessage, String alertBoxButtonText);
     public void showProgressDialog(String text);
     public void dismissProgressDialog();
     public void openUri(String uri);
}
