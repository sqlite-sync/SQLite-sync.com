package com.sqlitesync.sqlitesyncdemoandroidandroidstudio;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sqlitesync.service.SQLiteSyncCOMDBHelper;

/**
 * Created by konradgardocki on 21.07.2016.
 */
public class GridViewAdapter extends BaseAdapter {
    private Context _context;
    private SQLiteSyncCOMDBHelper.ResultSet _resultSet;

    public  GridViewAdapter(Context context, SQLiteSyncCOMDBHelper.ResultSet resultSet){
        _context = context;
        _resultSet = resultSet;
    }

    @Override
    public int getCount() {
        return _resultSet.ColumnNames.length * (_resultSet.Rows.size() + 1);
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        //TextView textView = new TextView(_context);
        TextView textView = (TextView) View.inflate(_context, R.layout.cell_table_view, null);
//        textView.setWidth(500);
//        textView.setTextColor(Color.parseColor("#47525e"));
//        textView.setBackgroundColor(Color.WHITE);
//        textView.setTextSize(15);
//        textView.setPadding(3,2,3,2);

        int columnsCount = _resultSet.ColumnNames.length;
        int rowIndex = i / columnsCount;
        int columnIndex = i % columnsCount;

        if(rowIndex == 0) {
            textView.setText(_resultSet.ColumnNames[columnIndex]);
            textView.setTypeface(null, Typeface.BOLD);
        }
        else{
            textView.setText(_resultSet.Rows.get(rowIndex-1).Cells.get(columnIndex).Value.toString());
        }

        return textView;
    }
}
