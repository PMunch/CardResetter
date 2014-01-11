package net.peterme.mifarecardresetter;

import java.util.Iterator;
import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class Write extends Activity {
	private byte[] payload;

	private long UID;
	private int page1214;
	private int page1315;

	private SharedPreferences settings;
	
	private Set<String> keys;
	private Bundle bundle;
	
	private static final String PREFS = "MyPrefs";

	private static final String TAG = Write.class.getSimpleName();
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		settings = getSharedPreferences(PREFS,0);
		UID = settings.getLong("UID",0);
		page1214 = settings.getInt("page1214",0);
		page1315 = settings.getInt("page1315",0);
		
		Log.d(TAG,"This is a test");
		Log.d(TAG,savedInstanceState==null ? "true" : "false");
	}
}
