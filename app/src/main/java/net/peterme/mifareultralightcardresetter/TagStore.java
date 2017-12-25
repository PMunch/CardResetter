package net.peterme.mifareultralightcardresetter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TagStore {
    public static final String KEY_CARDNAME = "cardname";
    public static final String KEY_CARDID = "cardid";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_LOCKED = "locked";
    public static final String KEY_DATA = "data";
    public static final String[] COLUMNS_PAGE = { KEY_CARDID, KEY_ROWID, KEY_LOCKED, KEY_DATA };
    public static final String[] COLUMNS_TAG = { KEY_CARDID, KEY_CARDNAME };

    private static final String DATABASE_NAME = "TagDB";

    private static final String DATABASE_TABLE_CARD = "CardTable";
    private static final String DATABASE_TABLE_PAGE = "PageTable";
    private static final int DATABASE_VERSION = 2;

    private DbHelper helper;
    private final Context ourContext;
    private SQLiteDatabase database;

    private static class DbHelper extends SQLiteOpenHelper {
        public DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + DATABASE_TABLE_CARD + " (" + KEY_CARDID + " LONG PRIMARY KEY, " + KEY_CARDNAME
                    + " TEXT);");

            db.execSQL("CREATE TABLE " + DATABASE_TABLE_PAGE + " (" + KEY_CARDID + " LONG, " + KEY_ROWID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_LOCKED + " INTEGER, " + KEY_DATA + " INTEGER, "
                    + "FOREIGN KEY (" + KEY_CARDID + ") REFERENCES " + DATABASE_TABLE_CARD + "(" + KEY_CARDID + "));");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO: Write proper update migration!!
            //db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CARD);
            //db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_PAGE);
            //Log.d("upgrade","SQL table drops on upgrade");
            // onCreate(db);
            try {
                Cursor c = db.query("TagTable", new String[] { "_id", "locked", "data" }, null, null, null, null, null);
                PageModel[] pages = new PageModel[16];
                int iLocked = c.getColumnIndex("locked");
                int iData = c.getColumnIndex("data");
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    pages[c.getPosition()] = new PageModel(c.getInt(iLocked) == 0 ? false : true, c.getInt(iData));
                }
                c.close();
                TagModel tag = new TagModel("Tag", pages);
                db.execSQL("DROP TABLE IF EXISTS TagTable");
                onCreate(db);

                ContentValues cv = new ContentValues();
                for (int i = 0; i < tag.pages.length; i++) {
                    cv.clear();
                    cv.put(KEY_LOCKED, tag.pages[i].locked);
                    cv.put(KEY_DATA, tag.pages[i].data);
                    cv.put(KEY_CARDID, tag.id);
                    db.insert(DATABASE_TABLE_PAGE, null, cv);
                }

                cv.clear();
                cv.put(KEY_CARDID, tag.id);
                cv.put(KEY_CARDNAME, tag.name);
                db.insert(DATABASE_TABLE_CARD, null, cv);
            } catch (SQLException e) {
                db.execSQL("DROP TABLE IF EXISTS TagTable");
                db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CARD);
                db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_PAGE);
                onCreate(db);
            }
        }

    }

    public TagStore(Context c) {
        ourContext = c;
    }

    public TagStore open() throws SQLException {
        helper = new DbHelper(ourContext);
        database = helper.getWritableDatabase();
        //database.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CARD);
        //database.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_PAGE);
        return this;

    }

    public TagStore close() {
        helper.close();
        return this;
    }

    /*public long createEntry(Boolean locked, String alias) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_ALIAS, alias);
        cv.put(KEY_DESTINATION, destination);
        return database.insert(tagUUID, null, cv);
    }*/
    public int setTag(TagModel tag) {
        //database.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
        //helper.onCreate(database);

        ContentValues cv = new ContentValues();
        int retval = 1;

        for (int i = 0; i < tag.pages.length; i++) {
            cv.clear();
            cv.put(KEY_LOCKED, tag.pages[i].locked);
            cv.put(KEY_DATA, tag.pages[i].data);
            cv.put(KEY_CARDID, tag.id);
            retval *= database.insert(DATABASE_TABLE_PAGE, null, cv) % 1 % -1;
        }

        cv.clear();
        cv.put(KEY_CARDID, tag.id);
        cv.put(KEY_CARDNAME, tag.name);
        retval *= database.insert(DATABASE_TABLE_CARD, null, cv) % 1 % -1;

        return retval;
    }

    public TagModel getTag(long tagid) {
        //Cursor c = database.query(DATABASE_TABLE_CARD, columns, null, null, null, null ,null);
        //String iCardName = c.getColumnIndex(KEY_CARDNAME);
        //long iCardID = c.getColumnIndex(KEY_CARDID);
        Cursor c = database.query(DATABASE_TABLE_CARD, COLUMNS_TAG, KEY_CARDID + "=?",
                new String[] { Long.toString(tagid) }, null, null, null, null);

        String name = null;
        if (c.moveToFirst()) {
            /*contact = new Contact(Integer.parseInt(cursor.getString(0)),
                    cursor.getString(1), cursor.getString(2));*/
            name = c.getString(1);
        }
        c.close();

        if (name != null) {
            PageModel[] pages = new PageModel[16];
            c = database.query(DATABASE_TABLE_PAGE, COLUMNS_PAGE, KEY_CARDID + "=?",
                    new String[] { Long.toString(tagid) }, null, null, KEY_ROWID, null);
            int iLocked = c.getColumnIndex(KEY_LOCKED);
            int iData = c.getColumnIndex(KEY_DATA);
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                pages[c.getPosition()] = new PageModel(c.getInt(iLocked) == 0 ? false : true, c.getInt(iData));
            }
            return new TagModel(name, pages);
        }
        return null;
    }

    public TagModel[] getAllTags() {
        Cursor c = database.query(DATABASE_TABLE_CARD, COLUMNS_TAG, null, null, null, null, null);
        TagModel[] tags = new TagModel[c.getCount()];
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Cursor c2 = database.query(DATABASE_TABLE_PAGE, COLUMNS_PAGE, KEY_CARDID + "=?",
                    new String[] { Long.toString(c.getLong(0)) }, null, null, KEY_ROWID, null);
            PageModel[] pages = new PageModel[16];
            for (c2.moveToFirst(); !c2.isAfterLast(); c2.moveToNext()) {
                pages[c2.getPosition()] = new PageModel(c2.getInt(2) == 0 ? false : true, c2.getInt(3));
            }
            tags[c.getPosition()] = new TagModel(c.getString(1), pages);
            c2.close();
        }
        c.close();
        return tags;
    }

    public void deleteTag(long tagId) {
        database.delete(DATABASE_TABLE_PAGE, KEY_CARDID + "=?", new String[] { Long.toString(tagId) });
        database.delete(DATABASE_TABLE_CARD, KEY_CARDID + "=?", new String[] { Long.toString(tagId) });
    }
}
