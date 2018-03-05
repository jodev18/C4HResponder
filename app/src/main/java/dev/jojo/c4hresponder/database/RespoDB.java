package dev.jojo.c4hresponder.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by myxroft on 23/02/2018.
 */

public class RespoDB extends SQLiteOpenHelper {

    public RespoDB(Context ct){
        super(ct,"c4h.db",null,1);
    }

    protected class LocationTable{

        public static final String TABLE_NAME = "tbl_loc";

        public static final String ID = "_id";

        public static final String LOC_LAT = "loc_lat";

        public static final String LOC_LONG = "loc_long";

        public static final String TIMESTAMP = "loc_timestamp";

        public static final String  EMERGENCY_TYPE = "loc_emergency_type";

        public static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME
                + "(" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LOC_LAT + " TEXT,"
                + LOC_LONG + " TEXT,"
                + EMERGENCY_TYPE + " TEXT,"
                + TIMESTAMP + " TEXT);";

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(LocationTable.TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
