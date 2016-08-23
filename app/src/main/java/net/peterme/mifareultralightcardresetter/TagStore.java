package net.peterme.mifareultralightcardresetter;

import java.util.ArrayList;

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
    public static final String[] columns ={KEY_CARDID,KEY_ROWID,KEY_LOCKED,KEY_DATA};

    private static final String DATABASE_NAME = "TagDB";

    private static final String DATABASE_TABLE_CARD = "CardTable";
    private static final String DATABASE_TABLE_PAGE = "PageTable";
    private static final int DATABASE_VERSION = 2;

    private DbHelper helper;
    private final Context ourContext;
    private SQLiteDatabase database;

    private static class DbHelper extends SQLiteOpenHelper{
        public DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + DATABASE_TABLE_CARD + " (" +
                    KEY_CARDID +        " LONG PRIMARY KEY, " +
                    KEY_CARDNAME + 			" TEXT);"
            );

            db.execSQL("CREATE TABLE " + DATABASE_TABLE_PAGE + " (" +
                    KEY_CARDID +        " LONG, " +
                    KEY_ROWID + 		" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_LOCKED + 		" INTEGER, " +
                    KEY_DATA + 			" INTEGER, "+
                    "FOREIGN KEY (" + KEY_CARDID + ") REFERENCES " + DATABASE_TABLE_CARD + "(" + KEY_CARDID + "));"
            );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO: Write proper update migration!!
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CARD);
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_PAGE);
            Log.d("upgrade","SQL table drops on upgrade");
            onCreate(db);
        }

    }
    public TagStore(Context c){
        ourContext = c;
    }
    public TagStore open() throws SQLException{
        helper = new DbHelper(ourContext);
        database = helper.getWritableDatabase();
        return this;

    }
    public TagStore close(){
        helper.close();
        return this;
    }
    /*public long createEntry(Boolean locked, String alias) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_ALIAS, alias);
        cv.put(KEY_DESTINATION, destination);
        return database.insert(tagUUID, null, cv);
    }*/
    public int setTag(Tag tag){
        //database.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
        //helper.onCreate(database);

        ContentValues cv = new ContentValues();
        int retval = 1;

        for(int i=0;i<tag.pages.length;i++){
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
    public Page[] getTag() {
        Cursor c = database.query(DATABASE_TABLE_CARD, columns, null, null, null, null ,null);
        



        Cursor c = database.query(DATABASE_TABLE, columns, null,null,null,null,null);
        Page[] returnTag = new Page[16];
        int iLocked = c.getColumnIndex(KEY_LOCKED);
        int iData = c.getColumnIndex(KEY_DATA);
        for(c.moveToFirst();!c.isAfterLast();c.moveToNext()){
            returnTag[c.getPosition()]=new Page(c.getInt(iLocked)==0 ? false : true, c.getInt(iData));
        }
        return returnTag;
    }
	/*public void deleteEntry(int row) {
		database.delete(DATABASE_TABLE, KEY_ROWID+"="+row, null);
	}*/
}
