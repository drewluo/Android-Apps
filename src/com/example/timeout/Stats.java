package com.example.timeout;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Stats {

	class StatsDBHelper extends SQLiteOpenHelper {

		
		public StatsDBHelper(Context context, String DBName) {
			super(context, DBName, null, VER_NUM);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			System.out.println("onCreate called: version="+db.getVersion());
			dbUpdate(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			System.out.println("onUpgrade called: old="+oldVersion+" new="+newVersion);
			dbUpdate(db);	
		}
		
		private void dbUpdate (SQLiteDatabase db) {
			int newVersion = VER_NUM;
			int oldVersion = db.getVersion();
			if (oldVersion == 0 || (newVersion >= 1 && oldVersion < 1)) {
				String queryFormat = "CREATE TABLE " + DATA_TABLE + "(_id INTEGER PRIMARY KEY," +
				     DATE_COL_NAME+" INTEGER, "+WORK_COL_NAME+" INTEGER,"+BREAK_COL_NAME+" INTEGER);";
				db.execSQL(queryFormat);
			}
			if (oldVersion == 0 || (newVersion >= 2 && oldVersion < 2)) {
				String queryFormat = "CREATE TABLE " + STATE_TABLE + "(_id INTEGER PRIMARY KEY," +
						NAME_COL_NAME+" TEXT," + INT_COL_NAME+" INTEGER);";
				db.execSQL(queryFormat);
			}
		}
		
	}
	
	private final int VER_NUM = 1;
	final static String DB_NAME = "stats.db";
	private static final String DATA_TABLE = "data";
	private static final String STATE_TABLE = "state";
	private static final String LAST_REC_TIME = "LASTREC";


	Context ctx;
		
	public static final long DAY_MS = 1000*3600*24; // save once a day
	public static final long DAY_SECS = 3600*24;
	private long currentDay;
		
	private int loadedSecondsWorked;

	private int loadedSecondsBreaked;
	
	SQLiteDatabase db;
	StatsDBHelper dbHelper;
	private long lastRecTime;
	
	private static long MAX_MS = 86400000;
	final String DATE_COL_NAME = "[date]";
	final String WORK_COL_NAME = "[worked]";
	final String BREAK_COL_NAME = "[breaked]";

	final String[] DATE_COLUMNS = {DATE_COL_NAME};
	final String[] WORKED_COLUMNS = {WORK_COL_NAME};
	final String[] BREAKED_COLUMNS = {BREAK_COL_NAME};
	final String[] WB_COLUMNS = {WORK_COL_NAME, BREAK_COL_NAME};
	final String[] DWB_COLUMNS = {DATE_COL_NAME, WORK_COL_NAME, BREAK_COL_NAME};
	
	final String NAME_COL_NAME = "[name]";
	final String INT_COL_NAME = "[int]";
	final String[] NAME_COLUMNS = {NAME_COL_NAME};
	final String[] INT_COLUMNS = {INT_COL_NAME};
	
	public Stats (Context ctx) {
		this.ctx = ctx;
		dbHelper = new StatsDBHelper(ctx, DB_NAME);
		db = dbHelper.getWritableDatabase();
		loadLastRecTime();
	}

	void saveStats(long day, long worked, long breaked) {
		ContentValues cv = new ContentValues();
		cv.put("DATE", day);
		cv.put("WORKED", worked);
		cv.put("BREAKED", breaked);
		
		if (db.update(DATA_TABLE, cv, "DATE="+day, null) == 0) {
			db.insert(DATA_TABLE, null, cv);
		}
	}
	
	public void checkPoint (long UTC_ms, long msSinceStart, boolean isWorking) {
		long timeGap = UTC_ms - lastRecTime;
		lastRecTime = UTC_ms;
		saveLastRecTime();

		if (msSinceStart > timeGap) {
			//TODO debug only
			//msSinceStart = timeGap;
		}
		long day;
		for (day = UTC_ms/DAY_MS*DAY_MS; ; day-=DAY_MS) {
			//Get number of ms in day within msSinceStart
			timeGap = UTC_ms - day;
			if (msSinceStart < timeGap) {
				timeGap = msSinceStart;
			}
			
			//transfer ms from msSinceStart to stats
			if (isWorking) {
				addWorkStats(day, timeGap/1000);
			} else {
				addBreakStats(day, timeGap/1000);
			}

			UTC_ms-=timeGap;
			msSinceStart-=timeGap;
			if (msSinceStart <=0) {
				break;
			}
		}
	}
	
	private void saveLastRecTime() {
		ContentValues cv = new ContentValues();
		cv.put("NAME", LAST_REC_TIME);
		cv.put("[INT]", lastRecTime);
		
		if (db.update(STATE_TABLE, cv, "NAME='"+LAST_REC_TIME+"'", null) == 0) {
			db.insert(STATE_TABLE, null, cv);
		}
	}

	private void loadLastRecTime() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor results = db.query(STATE_TABLE, INT_COLUMNS, NAME_COL_NAME+"='"+LAST_REC_TIME+"'", null, null, null, null);
		if (results.getCount() >= 1) {
			if (results.moveToFirst()) {
				lastRecTime = results.getInt(0);
			}
		}
	}

	private void addWorkStats(long day, long timeGap) {
		//load day's stats
		loadStats(day);
		//add stats to it
		loadedSecondsWorked+=timeGap;
		//save day's stats
		saveStats(day, loadedSecondsWorked, loadedSecondsBreaked);
	}


	private void addBreakStats(long day, long timeGap) {
		//load day's stats
		loadStats(day);
		//add stats to it
		loadedSecondsBreaked+=timeGap;
		//save day's stats
		saveStats(day, loadedSecondsWorked, loadedSecondsBreaked);
	}
	
	void loadStats (long UTC_day) {
		loadedSecondsWorked = 0;
		loadedSecondsBreaked = 0;
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor results = db.query(DATA_TABLE, WB_COLUMNS, DATE_COL_NAME+"="+UTC_day, null, null, null, null);
		if (results.getCount() >= 1) {
			if (results.moveToFirst()) {
				loadedSecondsWorked = results.getInt(0);
				loadedSecondsBreaked = results.getInt(1);
			}
		}
	}
	void loadCurrentStats () {
		loadStats(currentDay);
	}


	public int getSecondsWorked (long UTC_day) {
		Cursor results = db.query(DATA_TABLE, WORKED_COLUMNS, DATE_COL_NAME+"="+UTC_day, null, null, null, null);
		if (results.getCount() >= 1) {
			if (results.moveToFirst()) {
				return results.getInt(0);
			}
		}
		return 0;
	}
	
	public int getSecondsBreaked (long UTC_day) {
		Cursor results = db.query(DATA_TABLE, BREAKED_COLUMNS, DATE_COL_NAME+"="+UTC_day, null, null, null, null);
		if (results.getCount() >= 1) {
			if (results.moveToFirst()) {
				return results.getInt(0);
			}
		}
		return 0;
	}
	
	public Cursor queryDateRange (long startDayMs, long endDayMs) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor results = db.query(DATA_TABLE, DWB_COLUMNS, DATE_COL_NAME+">="+startDayMs+" AND "+DATE_COL_NAME+"<"+endDayMs, null, null, null, null);
		return results;
	}
}
