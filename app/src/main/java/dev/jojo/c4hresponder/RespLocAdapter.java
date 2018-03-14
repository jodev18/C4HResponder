package dev.jojo.c4hresponder;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

/**
 * Created by myxroft on 05/03/2018.
 */

public class RespLocAdapter extends BaseAdapter {

    private List<RespLocObject> list;
    private Activity at;

    public RespLocAdapter(Activity act, List<RespLocObject> locObjectList){
        this.list = locObjectList;
        this.at = act;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public RespLocObject getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return (long)position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = this.at.getLayoutInflater().inflate(R.layout.list_history_adapter,null);
        }

        TextView lat = (TextView)convertView.findViewById(R.id.tvLatitude);
        TextView longh = (TextView)convertView.findViewById(R.id.tvLongitude);
        TextView timestamp = (TextView)convertView.findViewById(R.id.tvTimestamp);
        TextView emtype = (TextView)convertView.findViewById(R.id.tvEmergency);

        lat.setText("");


        return convertView;
    }
}
