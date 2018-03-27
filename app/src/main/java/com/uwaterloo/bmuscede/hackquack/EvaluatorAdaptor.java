package com.uwaterloo.bmuscede.hackquack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.uwaterloo.bmuscede.solver.CheapDetector;

import java.util.List;

/**
 * Created by bmuscede on 27/03/18.
 */

public class EvaluatorAdaptor extends ArrayAdapter<CheapDetector.Type> {
    public EvaluatorAdaptor(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public EvaluatorAdaptor(Context context, int resource, List<CheapDetector.Type> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.layout_evaluators, null);
        }

        CheapDetector.Type eval = getItem(position);

        if (eval != null) {
            CheckBox chkE = (CheckBox) v.findViewById(R.id.chkEvaluator);
            TextView lblE = (TextView) v.findViewById(R.id.lblEvaluatorDescription);

            if (chkE != null) {
                chkE.setText(eval.getName());
            }

            if (lblE != null) {
                lblE.setText(eval.getDesc());
            }
        }

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox chkE = (CheckBox) v.findViewById(R.id.chkEvaluator);
                if (chkE.isChecked()){
                    chkE.setChecked(false);
                } else {
                    chkE.setChecked(true);
                }
            }
        });

        return v;
    }
}
