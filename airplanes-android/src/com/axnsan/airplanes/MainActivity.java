package com.axnsan.airplanes;

import com.axnsan.airplanes.util.ActionResolver;
import com.axnsan.airplanes.util.JavaXmlParser;
import com.axnsan.airplanes.util.TTFFontManager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class MainActivity extends AndroidApplication implements ActionResolver {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        cfg.useGL20 = false;
        
        RelativeLayout layout = new RelativeLayout(this);
        View gdxView = initializeForView(new Airplanes(new TTFFontManager(), new JavaXmlParser(), this), true);
        Airplanes.application = this;
        layout.addView(gdxView);
        setContentView(layout);
    }
    
    Handler uiThread = new Handler();
    Context context = this;
    
    @Override
    public void showShortToast(final CharSequence toastMessage) {
        uiThread.post(new Runnable() {
            public void run() {
                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT)
                                .show();
            }
        });
    }


    @Override
    public void showLongToast(final CharSequence toastMessage) {
        uiThread.post(new Runnable() {
            public void run() {
                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG)
                                .show();
            }
        });
    }


    @Override
    public void showAlertBox(final String alertBoxTitle,
                    final String alertBoxMessage, final String alertBoxButtonText) {
        uiThread.post(new Runnable() {
            public void run() {
                new AlertDialog.Builder(context)
	                    .setTitle(alertBoxTitle)
	                    .setMessage(alertBoxMessage)
	                    .setNeutralButton(alertBoxButtonText,
	                                    new DialogInterface.OnClickListener() {
	                                            public void onClick(DialogInterface dialog,
	                                                            int whichButton) {
	                                           }
	                                    }).create().show();
            }
        });
    }


    @Override
    public void openUri(String uri) {
        Uri myUri = Uri.parse(uri);
        Intent intent = new Intent(Intent.ACTION_VIEW, myUri);
        context.startActivity(intent);
    }

    
   	private synchronized void wake() {
   		this.notifyAll();
    }
    
    ProgressDialog pdHandle = null;
	@Override
	public synchronized void showProgressDialog(final String text) {
		uiThread.post(new Runnable() {
			@Override
			public void run() {
				if (pdHandle == null)
					pdHandle = ProgressDialog.show(context, "", text, true, false);
				MainActivity.this.wake();
			}
		});
		while (pdHandle == null) {
			try {
				wait();
			} catch (InterruptedException e) {}
		}
	}
	
	
	@Override
	public synchronized void dismissProgressDialog() {
		if (pdHandle != null)
		{
			pdHandle.dismiss();
			pdHandle = null;
		}
	}

}