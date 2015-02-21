package com.infinity.wefriends.apis;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBaseHelper extends SQLiteOpenHelper {
	

	public DataBaseHelper(Context context, String dataBaseName) {
		this(context, dataBaseName, null, 1);
	}

	public DataBaseHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE usercache(_id INTEGER PRIMARY KEY AUTOINCREMENT, accesstoken TEXT, wefriendsid TEXT, nickname TEXT, avatar TEXT, whatsup TEXT, friends TEXT, phone TEXT, email TEXT, intro TEXT, gender INTEGER, region TEXT, collegeid TEXT)");
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}

}