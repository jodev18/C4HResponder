package dev.jojo.c4hresponder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import dev.jojo.c4hresponder.RespLocObject;

/**
 * Created by myxroft on 04/03/2018.
 *
 * BASE CLASS.
 */

public class ResponseLocationManager extends RespoDB {

    private SQLiteDatabase sq;
    private ContentValues cv;
    private Cursor c;

    public ResponseLocationManager(Context ct){
        super(ct);

        this.sq = getWritableDatabase();
        this.cv = new ContentValues();
    }


    public void cleanUp(){

        if(this.sq != null){
            if(this.sq.isOpen()){
                this.sq.close();
            }
        }

        if(this.c != null){
            if(!this.c.isClosed()){
                this.c.close();
            }
        }
    }

    public long insertLocationData(RespLocObject lObj){

        this.cv.clear();

        this.cv.put(LocationTable.EMERGENCY_TYPE,lObj.LOC_EMERGENCY_TYPE);
        this.cv.put(LocationTable.LOC_LAT,lObj.LOC_LAT);
        this.cv.put(LocationTable.LOC_LONG,lObj.LOC_LONG);
        this.cv.put(LocationTable.TIMESTAMP, lObj.LOC_TIMESTAMP);

        long instat = this.sq.insert(LocationTable.TABLE_NAME, LocationTable.ID, this.cv);

        return instat;
    }

    /**
     * Hehe be careful: returns null
     * @return
     */
    public List<RespLocObject> getAllStoredLocationData(){

        String query = "SELECT * FROM " + LocationTable.TABLE_NAME;

        this.c = this.sq.rawQuery(query,null);

        if(this.c.getCount() > 0){
            List<RespLocObject> loclist = new ArrayList<>();

            while(c.moveToNext()){

                RespLocObject locObject = new RespLocObject();

                locObject.LOC_EMERGENCY_TYPE = c.getString(c.getColumnIndex(LocationTable.EMERGENCY_TYPE));
                locObject.LOC_LAT = c.getString(c.getColumnIndex(LocationTable.LOC_LAT));
                locObject.LOC_LONG = c.getString(c.getColumnIndex(LocationTable.LOC_LONG));
                locObject.LOC_TIMESTAMP = c.getString(c.getColumnIndex(LocationTable.TIMESTAMP));

                loclist.add(locObject);
            }

            return loclist;
        }
        else{
            return null;
        }
    }


}
