package com.arcusapp.labolibre.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import com.arcusapp.labolibre.R;

import java.util.ArrayList;
import java.util.List;

public class CustomAdapter extends ArrayAdapter<String> {

    private List<String> cnames;
    private List<Integer> ccolors;


    public CustomAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);

        initCalendarsInfo();
        this.addAll(cnames);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
        view.setBackgroundColor(ccolors.get(position));
        return view;
    }

    private void initCalendarsInfo() {
        // prepare calendar names
        String[] aux_names = this.getContext().getResources().getStringArray(R.array.calendar_names);
        cnames = new ArrayList<String>();
        for (int i = 0; i < aux_names.length; i++) {
            cnames.add(aux_names[i]);
        }

        // prepare calendar colors
        int[] aux_colors = this.getContext().getResources().getIntArray(R.array.calendar_colors);
        ccolors = new ArrayList<Integer>();
        for (int i = 0; i < aux_colors.length; i++) {
            ccolors.add(aux_colors[i]);
        }
    }
}
