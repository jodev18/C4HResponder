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

    //protected


    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
