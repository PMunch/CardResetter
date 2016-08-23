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
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    final Context context = this;
    final Activity activity = this;
    private NfcAdapter mAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private AlertDialog addTagDialog = null;
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

                    }
                });
                addTagDialog = builder.create();

                addTagDialog.show();
                addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                addTagDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        Log.d("Dialog","dismissed!");
                        addTagDialog = null;
                        mAdapter.disableForegroundDispatch(activity);
                    }
                });
                mAdapter.enableForegroundDispatch(activity, pendingIntent, mFilters, mTechLists);
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
            }
        });

        ListView listView1 = (ListView) findViewById(R.id.listView);

        ArrayList<String> cardList = new ArrayList<String>();

        // Iterator over those elements
        for(int i=0;i<15;i++){
            cardList.add("Item "+i);
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
            ((TextView)addTagDialog.findViewById(R.id.tagStatus)).setText("Tag detected");
            ((TextView)addTagDialog.findViewById(R.id.tagStatus)).setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_done_black_24dp),null,null,null);
        }else{
            Log.d("Intent","Somethings not right");
        }
    }
}
