package net.peterme.mifareultralightcardresetter;

import android.app.Activity;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String LOGTAG = "MainActivity";

    final Context context = this;
    final Activity activity = this;
    private NfcAdapter mAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private AlertDialog addTagDialog = null;
    private AlertDialog rewriteTagDialog = null;
    private ListView listView;
    private TagModel[] tags;
    private TagModel currentTag = null;
    private TagStore tagStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);

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
                        populateTagsList();
                        tagStore.close();
                        addTagDialog.dismiss();
                    }
                });
                addTagDialog = builder.create();

                addTagDialog.show();
                addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                ((EditText) addTagDialog.findViewById(R.id.editText)).addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        if (currentTag != null) {
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
                        Log.d("Dialog", "dismissed!");
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

        listView = (ListView) findViewById(R.id.listView);

        tagStore.open();
        populateTagsList();
        tagStore.close();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("Dialog", "Item selected! ");
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(R.layout.activity_rewrite_tag_dialog);
                rewriteTagDialog = builder.create();
                rewriteTagDialog.show();
                rewriteTagDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        Log.d("Dialog", "dismissed!");
                        rewriteTagDialog = null;
                        currentTag = null;
                        mAdapter.disableForegroundDispatch(activity);
                    }
                });
                currentTag = tags[i];
                mAdapter.enableForegroundDispatch(activity, pendingIntent, mFilters, mTechLists);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Do you wish to delete '" + tags[i].name + "'?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int o) {
                        tagStore.open();
                        tagStore.deleteTag(tags[i].id);
                        populateTagsList();
                        tagStore.close();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
                builder.create().show();
                return true;
            }
        });

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
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    public void initNFC() {
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Setup an intent filter for all MIME based dispatches
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        mFilters = new IntentFilter[] { ndef };

        // Setup a tech list for all NfcF tags
        mTechLists = new String[][] { new String[] { MifareUltralight.class.getName(), } };
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.enableForegroundDispatch(this, pendingIntent, mFilters, mTechLists);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Tag t = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        final MifareUltralight mifare = MifareUltralight.get(t);

        if (addTagDialog != null) {
            currentTag = new TagModel();

            try {
                mifare.connect();
                PageModel[] pages = new PageModel[16];
                ByteBuffer wrappedPayload;
                Boolean[] lockbits = {};
                int pageCount;
                switch (mifare.getType()) {
                case MifareUltralight.TYPE_ULTRALIGHT:
                    pageCount = 16;
                    break;
                case MifareUltralight.TYPE_ULTRALIGHT_C:
                    pageCount = 16; //Currently no support for any longer than 16 pages //48;
                    break;
                default:
                    pageCount = 0;
                }
                byte[] payload = mifare.readPages(0);
                wrappedPayload = ByteBuffer.wrap(payload);
                pages[0] = new PageModel(true, wrappedPayload.getInt(0));
                pages[1] = new PageModel(true, wrappedPayload.getInt(4));
                lockbits = MifareUltralightLockArray(wrappedPayload.getInt(8));
                pages[2] = new PageModel(false, wrappedPayload.getInt(8));
                pages[3] = new PageModel(lockbits[0], wrappedPayload.getInt(12));
                for (int i = 4; i < pageCount; i += 4) {
                    payload = mifare.readPages(i);
                    wrappedPayload = ByteBuffer.wrap(payload);
                    for (int j = 0; j < 4; j++)
                        pages[i + j] = new PageModel(lockbits[i - 3 + j], wrappedPayload.getInt(j * 4));
                }
                currentTag.setPages(pages);
                Log.d(LOGTAG, "Card id: " + Long.toHexString(currentTag.id));
                logPages(pages);
                mifare.close();
                tagStore.open();
                if (tagStore.getTag(currentTag.id) == null) {
                    ((TextView) addTagDialog.findViewById(R.id.tagStatus))
                            .setText(getText(R.string.tag_scanned_successfully));
                    ((TextView) addTagDialog.findViewById(R.id.tagStatus)).setCompoundDrawablesWithIntrinsicBounds(
                            getResources().getDrawable(R.drawable.ic_done_black_24dp), null, null, null);
                    if (!((EditText) addTagDialog.findViewById(R.id.editText)).getText().toString().equals("")) {
                        currentTag.setName(((EditText) addTagDialog.findViewById(R.id.editText)).getText().toString());
                        addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                } else {
                    ((TextView) addTagDialog.findViewById(R.id.tagStatus))
                            .setText(getText(R.string.tag_scanned_duplicate));
                    ((TextView) addTagDialog.findViewById(R.id.tagStatus)).setCompoundDrawablesWithIntrinsicBounds(
                            getResources().getDrawable(R.drawable.ic_tap_and_play_black_24dp), null, null, null);
                    addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    currentTag = null;
                }
                tagStore.close();
            } catch (IOException e) {
                Log.e(LOGTAG, "Error", e);
                ((TextView) addTagDialog.findViewById(R.id.tagStatus)).setText(getText(R.string.tag_scanned_error));
                ((TextView) addTagDialog.findViewById(R.id.tagStatus)).setCompoundDrawablesWithIntrinsicBounds(
                        getResources().getDrawable(R.drawable.ic_tap_and_play_black_24dp), null, null, null);
                addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                currentTag = null;
            } catch (NullPointerException e) {
                Log.e(LOGTAG, "Error", e);
                ((TextView) addTagDialog.findViewById(R.id.tagStatus)).setText(getText(R.string.tag_scanned_error));
                ((TextView) addTagDialog.findViewById(R.id.tagStatus)).setCompoundDrawablesWithIntrinsicBounds(
                        getResources().getDrawable(R.drawable.ic_tap_and_play_black_24dp), null, null, null);
                addTagDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                currentTag = null;
            }
        } else if (rewriteTagDialog != null) {
            try {
                mifare.connect();
                byte[] payload = mifare.readPages(0);
                ByteBuffer wrappedPayload = ByteBuffer.wrap(payload);
                if (wrappedPayload != null) {
                    long scannedId = wrappedPayload.getInt(0);
                    scannedId = scannedId << 4 * 8;
                    scannedId = scannedId | (0x00000000ffffffffL & wrappedPayload.getInt(4));
                    if (scannedId == currentTag.id) {
                        for (int i = 3; i < 16; i++) {
                            if (!currentTag.pages[i].locked)
                                mifare.writePage(i, ByteBuffer.allocate(4).putInt(currentTag.pages[i].data).array());
                        }
                        Boolean correct = true;
                        for (int i = 0; i < 16; i += 4) {
                            payload = mifare.readPages(i);
                            wrappedPayload = ByteBuffer.wrap(payload);
                            for (int j = 0; j < 4; j++) {
                                correct = correct && (wrappedPayload.getInt(j * 4) == currentTag.pages[i + j].data);
                            }
                        }
                        if (correct) {
                            ((TextView) rewriteTagDialog.findViewById(R.id.tagStatus))
                                    .setText(getText(R.string.tag_rewrite_success));
                            ((TextView) rewriteTagDialog.findViewById(R.id.tagStatus))
                                    .setCompoundDrawablesWithIntrinsicBounds(
                                            getResources().getDrawable(R.drawable.ic_done_black_24dp), null, null,
                                            null);
                        } else {
                            ((TextView) rewriteTagDialog.findViewById(R.id.tagStatus))
                                    .setText(getText(R.string.tag_rewrite_error));
                            ((TextView) rewriteTagDialog.findViewById(R.id.tagStatus))
                                    .setCompoundDrawablesWithIntrinsicBounds(
                                            getResources().getDrawable(R.drawable.ic_tap_and_play_black_24dp), null,
                                            null, null);
                        }
                    } else {
                        ((TextView) rewriteTagDialog.findViewById(R.id.tagStatus))
                                .setText(getText(R.string.tag_rewrite_notsame));
                        ((TextView) rewriteTagDialog.findViewById(R.id.tagStatus))
                                .setCompoundDrawablesWithIntrinsicBounds(
                                        getResources().getDrawable(R.drawable.ic_tap_and_play_black_24dp), null, null,
                                        null);
                    }
                } else {
                    ((TextView) rewriteTagDialog.findViewById(R.id.tagStatus))
                            .setText(getText(R.string.tag_rewrite_error));
                    ((TextView) rewriteTagDialog.findViewById(R.id.tagStatus)).setCompoundDrawablesWithIntrinsicBounds(
                            getResources().getDrawable(R.drawable.ic_tap_and_play_black_24dp), null, null, null);
                }
                mifare.close();
            } catch (IOException e) {
                Log.e(LOGTAG, "I/O Error during NFC Tag reset", e);
                ((TextView) rewriteTagDialog.findViewById(R.id.tagStatus)).setText(getText(R.string.tag_rewrite_error));
                ((TextView) rewriteTagDialog.findViewById(R.id.tagStatus)).setCompoundDrawablesWithIntrinsicBounds(
                        getResources().getDrawable(R.drawable.ic_tap_and_play_black_24dp), null, null, null);
            }

        } else {
            Log.d("Intent", "Somethings not right");
        }
    }

    public Boolean[] MifareUltralightLockArray(int number) {
        Boolean[] booleans = new Boolean[13];
        for (int i = 0; i < 5; i++) {
            booleans[i] = (number >> 11 + i & 1) == 1;
        }
        for (int i = 0; i < 8; i++) {
            booleans[5 + i] = (number >> i & 1) == 1;
        }
        return booleans;
    }

    public void logPages(PageModel[] pages) {
        for (int i = 0; i < pages.length; i++) {
            Log.d(LOGTAG, "Page #" + i + " is " + (pages[i].locked ? "locked" : "un-locked") + " and contains data "
                    + Integer.toHexString(pages[i].data));
        }
    }

    public void logTags(TagModel[] tags) {
        Log.d(LOGTAG, "Dump of all tags:");
        for (int ii = 0; ii < tags.length; ii++) {
            Log.d(LOGTAG, "Tag " + ii);
            if (tags[ii] != null) {
                Log.d(LOGTAG, "name: " + tags[ii].name);
                Log.d(LOGTAG, "id: " + Long.toHexString(tags[ii].id));
                logPages(tags[ii].pages);
            }
        }
    }

    public void populateTagsList() {
        tags = tagStore.getAllTags();
        ArrayList<String> cardList = new ArrayList<String>();

        for (int i = 0; i < tags.length; i++) {
            if (tags[i] != null)
                cardList.add(tags[i].name);
        }
        String[] items = new String[cardList.size()];
        cardList.toArray(items);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.simple_list_item_1, items);

        listView.setAdapter(adapter);
    }
}
