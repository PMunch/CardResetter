package net.peterme.mifareultralightcardresetter;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.MifareUltralight;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    final Context context = this;
    final Activity activity = this;
    private NfcAdapter mAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private AlertDialog addTagDialog = null;
    private TagModel currentTag = null;
    private TagStore tagStore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //final Dialog dialog = new Dialog(context);
                //dialog.setContentView(R.layout.activity_new_tag_dialog);
                //dialog.setTitle("Title...");
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(R.layout.activity_new_tag_dialog);
                builder.setPositiveButton(getText(R.string.add_complete_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        tagStore.open();
                        tagStore.setTag(currentTag);
                        tagStore.close();
                        addTagDialog.dismiss();
                    }
                });
                addTagDialog = builder.create();

                addTagDialog.show();
                addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                ((EditText)addTagDialog.findViewById(R.id.editText)).addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void afterTextChanged(Editable editable) {
                        if(currentTag!=null) {
                            if (editable.toString().equals("")) {
                                addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            } else {
                                addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            }
                            currentTag.setName(editable.toString());
                        }
                    }
                });

                addTagDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        Log.d("Dialog","dismissed!");
                        addTagDialog = null;
                        mAdapter.disableForegroundDispatch(activity);
                        currentTag = null;
                    }
                });
                mAdapter.enableForegroundDispatch(activity, pendingIntent, mFilters, mTechLists);
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
            }
        });

        tagStore = new TagStore(this);

        ListView listView1 = (ListView) findViewById(R.id.listView);

        ArrayList<String> cardList = new ArrayList<String>();

        tagStore.open();
        TagModel[] tags = tagStore.getAllTags();
        //logTags(tags);
        tagStore.close();
        for(int i=0;i<tags.length;i++){
            if(tags[i]!=null)
                cardList.add(tags[i].name);
        }

        String[] items = new String[cardList.size()];
        cardList.toArray(items);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, items);

        listView1.setAdapter(adapter);

        initNFC();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void initNFC(){
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Setup an intent filter for all MIME based dispatches
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
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
        mAdapter.enableForegroundDispatch(this, pendingIntent, mFilters, mTechLists);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if(addTagDialog!=null){
            currentTag = new TagModel();

            Tag t = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            final MifareUltralight mifare = MifareUltralight.get(t);
            try{
                mifare.connect();
                PageModel[] pages= new PageModel[16];
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
                byte[] payload=mifare.readPages(0);
                wrappedPayload = ByteBuffer.wrap(payload);
                pages[0]=new PageModel(true,wrappedPayload.getInt(0));
                pages[1]=new PageModel(true,wrappedPayload.getInt(4));
                lockbits = MifareUltralightLockArray(wrappedPayload.getInt(8));
                pages[2]=new PageModel(false,wrappedPayload.getInt(8));
                pages[3]=new PageModel(lockbits[0],wrappedPayload.getInt(12));
                for(int i=4;i<pageCount;i+=4){
                    payload=mifare.readPages(i);
                    wrappedPayload = ByteBuffer.wrap(payload);
                    for(int j=0;j<4;j++)
                        pages[i+j]=new PageModel(lockbits[i-3+j],wrappedPayload.getInt(j*4));
                }
                currentTag.setPages(pages);
                Log.d("TAG","Card id: "+Long.toHexString(currentTag.id));
                logPages(pages);
                mifare.close();
                tagStore.open();
                if(tagStore.getTag(currentTag.id)==null) {
                    ((TextView) addTagDialog.findViewById(R.id.tagStatus)).setText(getText(R.string.tag_scanned_successfully));
                    ((TextView) addTagDialog.findViewById(R.id.tagStatus)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_done_black_24dp), null, null, null);
                    if (!((EditText) addTagDialog.findViewById(R.id.editText)).getText().toString().equals("")) {
                        currentTag.setName(((EditText) addTagDialog.findViewById(R.id.editText)).getText().toString());
                        addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                }else{
                    ((TextView)addTagDialog.findViewById(R.id.tagStatus)).setText(getText(R.string.tag_scanned_duplicate));
                    ((TextView)addTagDialog.findViewById(R.id.tagStatus)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_tap_and_play_black_24dp),null,null,null);
                    addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    currentTag = null;
                }
                tagStore.close();
            }catch (IOException e){
                Log.e("TAG","Error",e);
                ((TextView)addTagDialog.findViewById(R.id.tagStatus)).setText(getText(R.string.tag_scanned_error));
                ((TextView)addTagDialog.findViewById(R.id.tagStatus)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_tap_and_play_black_24dp),null,null,null);
                addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                currentTag = null;
            }catch (NullPointerException e){
                Log.e("TAG","Error",e);
                ((TextView)addTagDialog.findViewById(R.id.tagStatus)).setText(getText(R.string.tag_scanned_error));
                ((TextView)addTagDialog.findViewById(R.id.tagStatus)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_tap_and_play_black_24dp),null,null,null);
                addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                currentTag = null;
            }
        }else{
            Log.d("Intent","Somethings not right");
        }
    }

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

    public void logPages(PageModel[] pages){
        for(int i=0;i<pages.length;i++){
            Log.d("TAG","Page #"+i+" is "+(pages[i].locked ? "locked" : "un-locked")+ " and contains data "+Integer.toHexString(pages[i].data));
        }
    }
    public void logTags(TagModel[] tags){
        Log.d("TAG","Dump of all tags:");
        for(int ii=0;ii<tags.length;ii++){
            Log.d("TAG","Tag "+ii);
            if(tags[ii]!=null) {
                Log.d("TAG", "name: " + tags[ii].name);
                Log.d("TAG", "id: " + Long.toHexString(tags[ii].id));
                logPages(tags[ii].pages);
            }
        }
    }
}
