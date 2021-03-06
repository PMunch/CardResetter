package net.peterme.mifareultralightcardresetter;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	private NfcAdapter mAdapter;
	private PendingIntent pendingIntent;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;
	private TextView statusText;
	private TextView tagStatus;
	private ToggleButton lockStatus;
	
	private TagStore tagStore;
	private Page[] tag;
	
	private byte[] payload;
	
	private boolean locked;
	//private long UID;
	//private int page1214;
	//private int page1315;
	
	private SharedPreferences settings;
	private SharedPreferences.Editor editor;
	
	private AlertDialog.Builder alt_bld;
	
	private static final String PREFS = "MyPrefs";

	private static final String TAG = MainActivity.class.getSimpleName();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.activity_main);
		statusText = (TextView) findViewById(R.id.text);
		tagStatus = (TextView) findViewById(R.id.tagStatus);
		lockStatus = (ToggleButton) findViewById(R.id.lockStatus);
		
		settings = getSharedPreferences(PREFS,0);
		editor = settings.edit();
		locked = settings.getBoolean("locked",false);
		tagStore = new TagStore(this);
		tagStore.open();
		tag = tagStore.getTag();
		tagStore.close();
		
		//UID = settings.getLong("UID",0);
		//page1214 = settings.getInt("page1214",0);
		//page1315 = settings.getInt("page1315",0);
		
		statusText.setText(R.string.stateReady);
		if (tag.length!=0){ //(UID!=0){
			tagStatus.setText(R.string.storeFull);
		}else{
			tagStatus.setText(R.string.storeEmpty);
		}
		lockStatus.setChecked(locked);
		
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

	@Override
	public void onResume()
	{
		super.onResume();
		settings = getSharedPreferences(PREFS,0);
		editor = settings.edit();
		locked = settings.getBoolean("locked",false);
		/*UID	= settings.getLong("UID",0);
		page1214 = settings.getInt("page1214",0);
		page1315 = settings.getInt("page1315",0);*/
		mAdapter.enableForegroundDispatch(this, pendingIntent, mFilters, mTechLists);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		editor.putBoolean("locked",locked);
		//editor.putLong("UID",UID);
		//editor.putInt("page1214",page1214);
		//editor.putInt("page1315",page1315);
		editor.commit();
		mAdapter.disableForegroundDispatch(this);
	}

	@Override
	public void onNewIntent(Intent intent){
		// fetch the tag from the intent
		statusText.setText(R.string.stateFound);
		Tag t = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

		final MifareUltralight mifare = MifareUltralight.get(t);
		
		if (locked==false){
			alt_bld = new AlertDialog.Builder(this);
			if (tag.length!=0)//(UID!=0)
				alt_bld.setMessage(R.string.overwriteMsg);
			else
				alt_bld.setMessage(R.string.loadMsg);
			alt_bld.setCancelable(false);
			alt_bld.setPositiveButton("Yes", new OnClickListener() { @Override
			public void onClick(DialogInterface dialog, int which) {
				try{
					mifare.connect();
					Page[] pages= new Page[16];
					ByteBuffer wrappedPayload;
					Boolean[] lockbits ={};
					int pageCount;
					switch(mifare.getType()){
						case MifareUltralight.TYPE_ULTRALIGHT:
							pageCount = 16;
							break;
						case MifareUltralight.TYPE_ULTRALIGHT_C:
							pageCount = 16; //Currently no support for any longer than 16 pages //48;
							break;
						default:
							pageCount = 0;
					}
					payload=mifare.readPages(0);
					wrappedPayload = ByteBuffer.wrap(payload);
					pages[0]=new Page(true,wrappedPayload.getInt(0));
					pages[1]=new Page(true,wrappedPayload.getInt(4));
					lockbits = MifareUltralightLockArray(wrappedPayload.getInt(8));
					pages[2]=new Page(false,wrappedPayload.getInt(8));
					pages[3]=new Page(lockbits[0],wrappedPayload.getInt(12));
					for(int i=4;i<pageCount;i+=4){
						payload=mifare.readPages(i);
						wrappedPayload = ByteBuffer.wrap(payload);
						for(int j=0;j<4;j++)
							pages[i+j]=new Page(lockbits[i-3+j],wrappedPayload.getInt(j*4));
					}
					tagStore.open();
					tagStore.setTag(pages);
					logPages(tagStore.getTag());
					tagStore.close();
					mifare.close();
					statusText.setText(R.string.stateRead);
					tagStatus.setText(R.string.storeFull);
				}catch (IOException e){
					Log.e(TAG,"Error",e);
					statusText.setText(R.string.stateError);
				}catch (NullPointerException e){
					Log.e(TAG,"Error",e);
					statusText.setText(R.string.stateError);
				}
			}});
			alt_bld.setNegativeButton("No", new OnClickListener() { @Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}}); 
			alt_bld.show();
		}else{
			try{
				mifare.connect();
				payload=mifare.readPages(0);
				ByteBuffer wrappedPayload = ByteBuffer.wrap(payload);
				if(wrappedPayload!=null){
					tagStore.open();
					Page[] tag = tagStore.getTag();
					tagStore.close();
					if(wrappedPayload.getInt(0)==tag[0].data && wrappedPayload.getInt(4)==tag[1].data){
						for(int i=3;i<16;i++){
							if(!tag[i].locked)
								mifare.writePage(i, ByteBuffer.allocate(4).putInt(tag[i].data).array());
						}
						Boolean correct = true;
						for(int i=0;i<16;i+=4){
							payload = mifare.readPages(i);
							wrappedPayload = ByteBuffer.wrap(payload);
							for(int j=0;j<4;j++){
								correct = correct && (wrappedPayload.getInt(j*4)==tag[i+j].data);
							}
						}
						if(correct){
							statusText.setText(R.string.stateWritten);
						}else{
							statusText.setText(R.string.stateError);
						}
					}else{
						statusText.setText(R.string.stateUnknown);
					}
				}else{
					statusText.setText(R.string.stateError);
				}
				/*if (UID==ByteBuffer.wrap(payload).getLong(0)){
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
					{statusText.setText(R.string.stateWritten);}else{statusText.setText(R.string.stateError);}
				}else{
					statusText.setText(R.string.stateUnknown);
				}*/
				mifare.close();
			}catch (IOException e){
				Log.e(TAG,"Error",e);
				statusText.setText(R.string.stateError);
			}
		}
	}
	/* MifareUltralightLockArray
	 * Converts the Mifare Ultralight lock byte page to an array of booleans from page 3 onwards
	 * */
	public Boolean[] MifareUltralightLockArray(int number){
		Boolean[] booleans=new Boolean[13];
		for(int i=0;i<5;i++){
			booleans[i]=(number>>11+i & 1) == 1;
		}
		for(int i=0;i<8;i++){
			booleans[5+i]=(number>>i & 1) == 1;
		}
		return booleans;
	}
	public void logPages(Page[] pages){
		for(int i=0;i<pages.length;i++){
			Log.d(TAG,"Page #"+i+" is "+(pages[i].locked ? "locked" : "un-locked")+ " and contains data "+Integer.toHexString(pages[i].data));
		}
	}
	public void onToggleClicked(View view){
		if (((ToggleButton) view).isChecked()){
			locked=true;
		}else{
			locked=false;
		}
	}
}

