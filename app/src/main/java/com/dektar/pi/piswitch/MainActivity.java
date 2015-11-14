package com.dektar.pi.piswitch;

import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        ToggleFragment toggleFragment = new ToggleFragment();
        fragmentTransaction.replace(android.R.id.content, toggleFragment);
        fragmentTransaction.commit();

        Intent intent = getIntent();
        if (AlarmClock.ACTION_SET_ALARM.equals(intent.getAction())) {
            if (intent.hasExtra(AlarmClock.EXTRA_HOUR)) {
                int hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, 0);
                Log.d("intent set", "user set the alarm intent with hour " + hour);
                if (hour % 2 == 0) {
                    toggleFragment.tryTurningOn();
                } else {
                    toggleFragment.tryTurningOff();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            this.startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
