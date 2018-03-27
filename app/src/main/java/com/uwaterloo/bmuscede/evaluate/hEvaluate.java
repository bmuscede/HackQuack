package com.uwaterloo.bmuscede.evaluate;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;

import com.uwaterloo.bmuscede.hackquack.EvaluatorAdaptor;
import com.uwaterloo.bmuscede.hackquack.R;
import com.uwaterloo.bmuscede.hackquack.StartActivity;
import com.uwaterloo.bmuscede.settings.evSettings;
import com.uwaterloo.bmuscede.settings.hqSettings;
import com.uwaterloo.bmuscede.solver.CheapDetector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class hEvaluate extends AppCompatActivity {
    private Context curContext;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h_evaluate);
        curContext = this;

        //Manages toolbar settings.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView nView = findViewById(R.id.nav_view);
        nView.getMenu().getItem(1).setChecked(true);
        nView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        //TODO: Figure out if we can change states validly.

                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();

                        //See which item was tapped.
                        if (menuItem.getItemId() == R.id.hq_menu) {
                            Intent hqIntent = new Intent(curContext, StartActivity.class);
                            hqIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivityForResult(hqIntent, 0);
                        } else if (menuItem.getItemId() == R.id.gs_menu) {
                            Intent evIntent = new Intent(curContext, evSettings.class);
                            evIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivityForResult(evIntent, 0);
                        } else if (menuItem.getItemId() == R.id.hqS_menu) {
                            Intent hqSIntent = new Intent(curContext, hqSettings.class);
                            hqSIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivityForResult(hqSIntent, 0);
                        }

                        return true;
                    }
                });

        //Sets the listview system.
        EvaluatorAdaptor adaptor = new EvaluatorAdaptor(this, R.layout.layout_evaluators,
                new ArrayList<CheapDetector.Type>(Arrays.asList(CheapDetector.Type.values())));
        ListView view = (ListView) findViewById(R.id.lstEval);
        view.setAdapter(adaptor);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
