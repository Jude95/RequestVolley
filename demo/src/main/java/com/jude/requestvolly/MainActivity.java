package com.jude.requestvolly;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.jude.http.RequestListener;
import com.jude.http.RequestManager;
import com.jude.http.RequestMap;
import com.jude.requestvolly.callback.DataCallback;


public class MainActivity extends ActionBarActivity {
    private TextView text;
    private TextView tvPerson;
    private ImageView image;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.text);
        tvPerson = (TextView) findViewById(R.id.tv_person);
        image = (ImageView) findViewById(R.id.image);
        RequestManager.getInstance().init(this);
        RequestManager.getInstance().get("https://apiview.com/test/408/RequestVolly/getPerson", new RequestListener() {
            @Override
            public void onRequest() {

            }

            @Override
            public void onSuccess(String response) {
                text.setText(response);
            }

            @Override
            public void onError(String errorMsg) {

            }
        });
        RequestMap params = new RequestMap();
        params.put("id","213");
        RequestManager.getInstance().post("https://apiview.com/test/408/RequestVolly/getPerson", params, new DataCallback<Person>() {
            @Override
            public void success(String info, Person data) {
                tvPerson.setText(data.getName()+":"+data.getAge());
            }
        });
        RequestManager.getInstance().img("http://img2.imgtn.bdimg.com/it/u=2660800756,2021530274&fm=21&gp=0.jpg",image);

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
