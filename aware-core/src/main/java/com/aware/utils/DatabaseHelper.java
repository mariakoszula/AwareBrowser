
package com.aware.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.aware.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * ContentProvider database helper<br/>
 * This class is responsible to make sure we have the most up-to-date database structures from plugins and sensors
 * @author denzil
 *
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	private final boolean DEBUG = true;
	private final String TAG = "DatabaseHelper";

	private final String database_name;
	private final String[] database_tables;
	private final String[] table_fields;
	private final int new_version;

	private SQLiteDatabase database = null;

	public DatabaseHelper(Context context, String database_name, CursorFactory cursor_factory, int database_version, String[] database_tables, String[] table_fields) {
		super(context, database_name, cursor_factory, database_version);

		this.database_name = database_name;
		this.database_tables = database_tables;
		this.table_fields = table_fields;
		this.new_version = database_version;

		//Create the folder where all the databases will be stored on external storage
		File folders = new File(Environment.getExternalStorageDirectory() + "/AWARE/");
		folders.mkdirs();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		if (DEBUG) Log.w(TAG, "Database in use: " + db.getPath());

		for (int i = 0; i < database_tables.length; i++) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + database_tables[i] + " (" + table_fields[i] + ");");
		}
		db.setVersion(new_version);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (DEBUG) Log.w(TAG, "Upgrading database: " + db.getPath());

		if (db.inTransaction()) {
			if (DEBUG) Log.w(TAG, "Upgrade in progress...");
			return;
		}

		db.beginTransaction();
		for (int i = 0; i < database_tables.length; i++) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + database_tables[i] + " (" + table_fields[i] + ");");

			List<String> columns = getColumns(db, database_tables[i]);
			db.execSQL("ALTER TABLE " + database_tables[i] + " RENAME TO temp_" + database_tables[i] + ";");
			db.execSQL("CREATE TABLE " + database_tables[i] + " (" + table_fields[i] + ");");
			columns.retainAll(getColumns(db, database_tables[i]));

			String cols = TextUtils.join(",", columns);
			//restore old data back
			db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from temp_%s", database_tables[i], cols, cols, database_tables[i]));
			db.execSQL("DROP TABLE temp_" + database_tables[i] + ";");
		}
		db.setTransactionSuccessful();
		db.setVersion(newVersion);
	}

	/**
	 * Creates a String of a JSONArray representation of a database cursor result
	 *
	 * @param crs
	 * @return String
	 */
	public static String cursorToString(Cursor crs) {
		JSONArray arr = new JSONArray();
		crs.moveToFirst();
		while (!crs.isAfterLast()) {
			int nColumns = crs.getColumnCount();
			JSONObject row = new JSONObject();
			for (int i = 0; i < nColumns; i++) {
				String colName = crs.getColumnName(i);
				if (colName != null) {
					try {
						switch (crs.getType(i)) {
							case Cursor.FIELD_TYPE_BLOB:
								row.put(colName, crs.getBlob(i).toString());
								break;
							case Cursor.FIELD_TYPE_FLOAT:
								row.put(colName, crs.getDouble(i));
								break;
							case Cursor.FIELD_TYPE_INTEGER:
								row.put(colName, crs.getLong(i));
								break;
							case Cursor.FIELD_TYPE_NULL:
								row.put(colName, null);
								break;
							case Cursor.FIELD_TYPE_STRING:
								row.put(colName, crs.getString(i));
								break;
						}
					} catch (JSONException e) {
					}
				}
			}
			arr.put(row);
			if (!crs.moveToNext()) break;
		}
		return arr.toString();
	}

	public static List<String> getColumns(SQLiteDatabase db, String tableName) {
		List<String> ar = null;
		Cursor c = null;
		try {
			c = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 1", null);
			if (c != null) {
				ar = new ArrayList<>(Arrays.asList(c.getColumnNames()));
			}
		} catch (Exception e) {
			Log.v(tableName, e.getMessage(), e);
			e.printStackTrace();
		} finally {
			if (c != null) c.close();
		}
		return ar;
	}

	@Override
	public SQLiteDatabase getWritableDatabase() {

		if (database != null) {
			if (!database.isOpen()) {
				database = null;
			} else if (!database.isReadOnly()) {
				//Database is ready, return it for efficiency
				return database;
			}
		}

		//Get reference to database file, we might not have it.
		File database_file = new File(database_name);
		try {
			SQLiteDatabase current_database = SQLiteDatabase.openDatabase(database_file.getPath(), null, SQLiteDatabase.CREATE_IF_NECESSARY);
			int current_version = current_database.getVersion();

			if (current_version != new_version) {
				current_database.beginTransaction();
				try {
					if (current_version == 0) {
						onCreate(current_database);
					} else {
						onUpgrade(current_database, current_version, new_version);
					}
					current_database.setVersion(new_version);
					current_database.setTransactionSuccessful();
				} finally {
					current_database.endTransaction();
				}
			}
			onOpen(current_database);
			database = current_database;
			return database;
		} catch (SQLException e) {
			return null;
		}
	}

	@Override
	public SQLiteDatabase getReadableDatabase() {
		if (database != null) {
			if (!database.isOpen()) {
				database = null;
			} else if (!database.isReadOnly()) {
				//Database is ready, return it for efficiency
				return database;
			}
		}

		try {
			return getWritableDatabase();
		} catch (SQLException e) {
			//we will try to open it read-only as requested.
		}

		//Get reference to database file, we might not have it.
		File database_file = new File(database_name);
		SQLiteDatabase current_database = SQLiteDatabase.openDatabase(database_file.getPath(), null, SQLiteDatabase.OPEN_READONLY);
		onOpen(current_database);
		database = current_database;
		return database;
	}
}