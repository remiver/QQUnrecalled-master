package com.fkzhang.qqunrecalled;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class SupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);


        ListView listView = (ListView) findViewById(R.id.qr_list);
        final int[] drawableIds = {R.drawable.wechat_qr, R.drawable.alipay_qr};
        final ArrayList<Drawable> drawables = new ArrayList<>();
        for (int drawableId : drawableIds) {
            drawables.add(ContextCompat.getDrawable(this, drawableId));
        }

        ArrayList<String> titles = new ArrayList<>();
        titles.add(getString(R.string.wechat));
        titles.add(getString(R.string.alipay));

        listView.setAdapter(new SupportListAdapter(this, drawables, titles));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                switch (position) {
                    case 0:
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://pan.baidu.com/s/1hrxzvCS"));
                        try {
                            startActivity(intent);
                        } catch (Throwable t) {
                        }
                        break;
                    case 1:
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://pan.baidu.com/s/1dDSxCPj"));
                        try {
                            startActivity(intent);
                        } catch (Throwable t) {
                        }
                        break;
                }
            }
        });


    }


}
