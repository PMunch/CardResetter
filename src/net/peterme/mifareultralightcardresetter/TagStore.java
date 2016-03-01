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
	public static final String KEY_ROWID = "_id";
	public static final String KEY_LOCKED = "locked";
	public static final String KEY_DATA = "data";
	public static final String[] columns ={KEY_ROWID,KEY_LOCKED,KEY_DATA};
	
	private static final String DATABASE_NAME = "TagDB";
	private static final String DATABASE_TABLE = "TagTable";
	private static final int DATABASE_VERSION = 1;
	
	private DbHelper helper;
	private final Context ourContext;
	//private static String tagUUID;
	private SQLiteDatabase database;
	
	private static class DbHelper extends SQLiteOpenHelper{
		public DbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			// TODO Auto-generated constructor stub 
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + DATABASE_TABLE + " (" +
				KEY_ROWID + 		" INTEGER PRIMARY KEY AUTOINCREMENT, " +
				KEY_LOCKED + 		" INTEGER, " +
				KEY_DATA + 			" INTEGER);"					
			);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
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
	public int setTag(Page[] pages){
		database.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
		helper.onCreate(database);
		
		ContentValues cv = new ContentValues();
		int retval = 1;
		
		for(int i=0;i<pages.length;i++){
			cv.clear();
			cv.put(KEY_LOCKED, pages[i].locked);
			cv.put(KEY_DATA, pages[i].data);
			retval *= database.insert(DATABASE_TABLE, null, cv) % 1 % -1;
		}
		
		return retval;
	}
	/*public ArrayList<String[]> getData() {
		String[] columns = new String[]{KEY_ROWID,KEY_ALIAS,KEY_DESTINATION};
		Cursor c = database.query(DATABASE_TABLE, columns,null,null,null,null,null);
		ArrayList<String[]> result = new ArrayList<String[]>();
		int iRow = c.getColumnIndex(KEY_ROWID);
		int iAlias = c.getColumnIndex(KEY_ALIAS);
		int iDestination = c.getColumnIndex(KEY_DESTINATION);
		
		for (c.moveToFirst();!c.isAfterLast();c.moveToNext()){
			//result.add(c.getString(iRow) + " " + c.getString(iAlias) + " " + c.getString(iDestination));
			result.add(new String[]{c.getString(iRow),c.getString(iDestination),c.getString(iAlias)});
			Log.d("List","Text: '"+c.getString(iAlias)+"' "+ c.getString(iDestination));
		}
		return result;
	}*/
	public Page[] getTag() {
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
