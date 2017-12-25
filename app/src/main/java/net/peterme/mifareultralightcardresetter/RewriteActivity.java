package net.peterme.mifareultralightcardresetter;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Build;
import android.os.Bundle;

public class RewriteActivity extends Activity {
    private byte[] payload;

    private boolean locked;
    private TagStore tagStore;

    private SharedPreferences settings;
    private Notification noti;

    private static final String PREFS = "MyPrefs";

    private static final String TAG = "NfcTagReset";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tagStore = new TagStore(this);

        Intent intent = this.getIntent();

        Tag t = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        final MifareUltralight mifare = MifareUltralight.get(t);
        try {
            mifare.connect();
            payload = mifare.readPages(0);

            ByteBuffer wrappedPayload = ByteBuffer.wrap(payload);
            long scannedId = wrappedPayload.getInt(0);
            scannedId = scannedId << 4 * 8;
            scannedId = scannedId | (0x00000000ffffffffL & wrappedPayload.getInt(4));
            tagStore.open();
            TagModel tag = tagStore.getTag(scannedId);
            tagStore.close();
            if (tag != null) {
                for (int i = 3; i < 16; i++) {
                    if (!tag.pages[i].locked)
                        mifare.writePage(i, ByteBuffer.allocate(4).putInt(tag.pages[i].data).array());
                }
                Boolean correct = true;
                for (int i = 0; i < 16; i += 4) {
                    payload = mifare.readPages(i);
                    wrappedPayload = ByteBuffer.wrap(payload);
                    for (int j = 0; j < 4; j++) {
                        correct = correct && (wrappedPayload.getInt(j * 4) == tag.pages[i + j].data);
                    }
                }
                if (correct) {
                    noti = new Notification.Builder(this).setContentTitle(getString(R.string.state_written_short))
                            .setContentText(getString(R.string.state_written))
                            .setSmallIcon(R.drawable.ic_stat_notify_icon).build();
                } else {
                    noti = new Notification.Builder(this).setContentTitle(getString(R.string.state_error_short))
                            .setContentText(getString(R.string.state_error))
                            .setSmallIcon(R.drawable.ic_stat_notify_icon).build();
                }
            } else {
                noti = new Notification.Builder(this).setContentTitle(getString(R.string.state_unknown_short))
                        .setContentText(getString(R.string.state_unknown)).setSmallIcon(R.drawable.ic_stat_notify_icon)
                        .build();
            }
            mifare.close();
        } catch (IOException e) {
            Log.e(TAG, "I/O Error during NFC Tag reset", e);
            noti = new Notification.Builder(this).setContentTitle(getString(R.string.state_error_short))
                    .setContentText(getString(R.string.state_error)).setSmallIcon(R.drawable.ic_stat_notify_icon)
                    .build();
        }
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(0, noti);
        finish();
    }
}