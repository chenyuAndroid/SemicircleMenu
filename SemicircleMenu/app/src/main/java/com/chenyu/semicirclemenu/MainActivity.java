package com.chenyu.semicirclemenu;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

    private String[] mItemTexts = new String[] { "应用商店", "WIFI", "短信",
            "计算器","设置","二维码"};

    private int[] mItemImgs = new int[] { R.drawable.android,
            R.drawable.wifi, R.drawable.message,
            R.drawable.calculator, R.drawable.setting,
            R.drawable.qrcode};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SemicircleMenu mSemicircleMenu = (SemicircleMenu) findViewById(R.id.circlemenu);
        mSemicircleMenu.setMenuItemIconsAndTexts(mItemImgs, mItemTexts);
        mSemicircleMenu.setOnMenuItemClickListener(new SemicircleMenu.OnMenuItemClickListener() {
            @Override
            public void itemClick(View view, int pos) {
                Toast.makeText(MainActivity.this, mItemTexts[pos], Toast.LENGTH_SHORT).show();

            }
        });

        mSemicircleMenu.setOnCentralItemCallback(new SemicircleMenu.OnCentralItemCallback() {
            @Override
            public void centralItemOperate(int pos) {
                Log.d("cylog", "The position is " + pos);
            }
        });
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
