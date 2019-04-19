package com.sqlite_sync.demo;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GridViewAdapter extends BaseAdapter implements View.OnClickListener {
    private Context _context;
    private OnRowActionListener _onRowActionListener;
    private String[] _columns;
    private String[][] _rows;

    public  GridViewAdapter(Context context, String[] columns, String[][] rows, OnRowActionListener onRowActionListener){
        _context = context;
        _columns = columns;
        _rows = rows;
        _onRowActionListener = onRowActionListener;
    }

    @Override
    public int getCount() {
        return (_columns.length + 1) * (_rows.length + 1);
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
        int rowIndex = i / (_columns.length + 1);
        int columnIndex = i % (_columns.length + 1);

        if(rowIndex == 0) {
            if(columnIndex == 0) {
                return new View(_context);
            }
            else{
                TextView textView = (TextView) View.inflate(_context, R.layout.cell_table_view, null);
                textView.setText(_columns[columnIndex - 1]);
                textView.setTypeface(null, Typeface.BOLD);
                return textView;
            }
        }
        else{
            if(columnIndex == 0) {
                LinearLayout buttons = (LinearLayout) View.inflate(_context, R.layout.cell_buttons_table_view, null);

                buttons.findViewById(R.id.btCellEdit).setTag(String.format("%d;%d", 0, rowIndex - 1));
                buttons.findViewById(R.id.btCellEdit).setOnClickListener(this);

                buttons.findViewById(R.id.btCellDelete).setTag(String.format("%d;%d", 1, rowIndex - 1));
                buttons.findViewById(R.id.btCellDelete).setOnClickListener(this);

                return buttons;
            }
            else{
                TextView textView = (TextView) View.inflate(_context, R.layout.cell_table_view, null);
                textView.setText(_rows[rowIndex - 1][columnIndex - 1]);
                return textView;
            }
        }
    }

    @Override
    public void onClick(View view) {
        String[] data = view.getTag().toString().split(";");

        int action = Integer.parseInt(data[0]);
        int index = Integer.parseInt(data[1]);

        for(int i = 0; i < _columns.length; i++){
            if(_columns[i].equalsIgnoreCase("RowId")){
                if(action == 0){
                    _onRowActionListener.onEdit(_rows[index][i]);
                }
                else if(action == 1){
                    _onRowActionListener.onDelete(_rows[index][i]);
                }
                break;
            }
        }
    }

    public interface OnRowActionListener{
        void onEdit(String rowId);
        void onDelete(String rowId);
    }
}
