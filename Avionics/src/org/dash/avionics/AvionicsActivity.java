package org.dash.avionics;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

@Fullscreen
@EActivity(R.layout.activity_avionics)
public class AvionicsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().hide();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	        super.onWindowFocusChanged(hasFocus);
	        View decorView = getWindow().getDecorView();
	       if (hasFocus) {
	    	   // TODO: Support older versions
	        decorView.setSystemUiVisibility(
	                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
	                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	        }
	}
}
