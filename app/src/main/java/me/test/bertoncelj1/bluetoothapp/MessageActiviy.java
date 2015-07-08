package me.test.bertoncelj1.bluetoothapp;

import android.content.Intent;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MessageActiviy extends ActionBarActivity implements View.OnClickListener {

    private Button bSend;
    private TextView tvConnWith, tvReceive;
    private EditText etMessage;
    private BluetoothMngr blueMngr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msg);

        bSend = (Button) findViewById(R.id.bSend);
        tvConnWith = (TextView) findViewById(R.id.tvConnWith);
        tvReceive = (TextView) findViewById(R.id.tvReceive);
        etMessage = (EditText) findViewById(R.id.etMessage);

        bSend.setOnClickListener(this);

        if(!getBlueMng())finish();
        tvConnWith.setText("Connected with " + blueMngr.getDevName()+ ".");
    }

    //gets Bluetooth manager from previous activity, vrne false ce nemore dobiti ojekta
    private boolean getBlueMng(){

        Intent i = getIntent();
        if(i == null)return false;

        Object par = i.getParcelableExtra("blueMng");

        if(par instanceof BluetoothMngrParcelable){
            blueMngr = ((BluetoothMngrParcelable) par).getBluetoothMngr();
            return true;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message_activiy, menu);
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

    @Override
    public void onClick(View v) {

    }
}
