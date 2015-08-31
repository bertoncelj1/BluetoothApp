package me.test.bertoncelj1.bluetoothapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

//TODO:disconect when activity leaves

public class MessageActiviy extends ActionBarActivity implements View.OnClickListener,  BluetoothManager.ConnectionStatus{

    private Button bSend;
    private TextView tvConnWith, tvReceive;
    private EditText etMessage;
    private ProgressDialog barProgressDialog;
    ActionBarActivity thisActivity;

    BluetoothManager blueManager;
    String deviceName, deviceAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msg);

        thisActivity = this;
        initElements();

        Intent intent = getIntent();
        deviceName = intent.getStringExtra("blueDeviceName");
        deviceAddress = intent.getStringExtra("blueDeviceAddress");

        startConnecting();
    }

    private void initElements(){
        bSend = (Button) findViewById(R.id.bSend);
        tvConnWith = (TextView) findViewById(R.id.tvConnWith);
        tvReceive = (TextView) findViewById(R.id.tvReceive);
        etMessage = (EditText) findViewById(R.id.etMessage);

        bSend.setOnClickListener(this);
    }


    public void startConnecting() {
        blueManager = BluetoothManager.getInstance(deviceName, deviceAddress, this);

        final ProgressDialog ringProgressDialog = ProgressDialog.show(this, "Connecting ...",	"Connecting with " + deviceName + "\n" + deviceAddress, true);
        ringProgressDialog.setCancelable(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    blueManager.Connect();
                } catch (Exception e) {

                }
                ringProgressDialog.dismiss();
            }
        }).start();
    }

    @Override
    public void connectionConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvConnWith.setText("Connected !!!!");
            }
        });
    }

    @Override
    public void connectionFailed(String message) {
        Log.d("TAG", "connection finished");
        /*
        Handler handler = new Handler();
        handler.post(new Runnable() {
            public void run() {
                thisActivity.finish();
            }
        });
        */
        MessageActiviy.this.finish();
        thisActivity.finish();

    }


    @Override
    public void messageReceived(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvReceive.setText(tvReceive.getText() + message);
            }
        });

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("TAG", "destroy");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("TAG", "stop");

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("TAG", "resume");

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("TAG", "pause");

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
        thisActivity.finish();
    }


}
