package net.peterme.mifarecardresetter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import android.app.Activity;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.util.Log;

public class Write extends Activity {
	private byte[] payload;

	private long UID;
	private boolean locked;
	private int page1214;
	private int page1315;
	private NfcAdapter mAdapter;
	private PendingIntent pendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;

	private SharedPreferences settings;
	private Notification noti;
	
	private static final String PREFS = "MyPrefs";

	private static final String TAG = Write.class.getSimpleName();
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		settings = getSharedPreferences(PREFS,0);
		UID = settings.getLong("UID",0);
		locked = settings.getBoolean("locked",false);
		page1214 = settings.getInt("page1214",0);
		page1315 = settings.getInt("page1315",0);
		
		mAdapter = NfcAdapter.getDefaultAdapter(this);
		pendingIntent = PendingIntent.getActivity(
				this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		// Setup an intent filter for all MIME based dispatches
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			ndef.addDataType("*/*");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}
		mFilters = new IntentFilter[] {
				ndef
		};

		// Setup a tech list for all NfcF tags
		mTechLists = new String[][] { new String[] { 
				MifareUltralight.class.getName(),
		} };
	}
	public void onNewIntent(Intent intent){
		// fetch the tag from the intent
		Tag t = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

		final MifareUltralight mifare = MifareUltralight.get(t);
		try{
			mifare.connect();
			mifare.readPages(0);
			if (UID==ByteBuffer.wrap(payload).getLong(0) && locked==true){
				mifare.writePage(12,ByteBuffer.allocate(4).putInt(page1214).array());
				mifare.writePage(13,ByteBuffer.allocate(4).putInt(page1315).array());
				mifare.writePage(14,ByteBuffer.allocate(4).putInt(page1214).array());
				mifare.writePage(15,ByteBuffer.allocate(4).putInt(page1315).array());
				payload=mifare.readPages(12);
				int p12=ByteBuffer.wrap(new byte[]{payload[0],payload[1],payload[2],payload[3]}).getInt(0);
				int p13=ByteBuffer.wrap(new byte[]{payload[4],payload[5],payload[6],payload[7]}).getInt(0);
				int p14=ByteBuffer.wrap(new byte[]{payload[8],payload[9],payload[10],payload[11]}).getInt(0);
				int p15=ByteBuffer.wrap(new byte[]{payload[12],payload[13],payload[14],payload[15]}).getInt(0);
				if (p12==page1214 && p13==page1315 && p14==page1214 && p15==page1315)
				{
					noti = new Notification.Builder(this)
			         .setContentTitle(getString(R.string.stateWritten))
			         .getNotification();
				}else{
					noti = new Notification.Builder(this)
			         .setContentTitle(getString(R.string.stateError))
			         .getNotification();
				}
				((NotificationManager) getSystemService("NOTIFICATION_SERVICE")).notify(0,noti);
			}
		}catch(IOException e){
			Log.e(TAG,"Error",e);
		}
	}
}
