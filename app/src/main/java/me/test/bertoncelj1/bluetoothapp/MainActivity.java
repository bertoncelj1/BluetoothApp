package me.test.bertoncelj1.bluetoothapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity implements View.OnClickListener, AdapterView.OnItemClickListener, BluetoothMngr.ConnectionCallback {

    private ListView lvSeznam;
    private TextView tvNapis;
    private Button bSearch, bEnableBlue;
    private BluetoothAdapter mBluetoothAdapter;
    private View llBlueDevices;


    private final String DEBUG_TAG = "infoBlue";


    BluetoothArduino mBlue;
    BlueDevicesAdapter adapterDevices;
    BluetoothMngr bluetoothMngr = BluetoothMngr.getInstance(this, this);

    int positionConnecting;
    //BlueDevicesAdapter connAdapter;
    //int sectPairedId, sectAvailableId; //id s katerimi potem lahko dostopa do svojega sectiona
    Section sectionPaired;
    Section sectionAvailable;

    private final int type = 0;

    //če je registriran register za callback funcije ki išče bluetooth naprave
    //to je potrebno ker na ondestroy moramo zapreti reciver in moramo vedeti ali
    //je bil že registriran ali pa ne
    boolean isRegisterReciever = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(DEBUG_TAG, "################################# START #################################");
        lvSeznam = (ListView) findViewById(R.id.lvSeznam);
        bSearch = (Button) findViewById(R.id.bSearch);
        bEnableBlue = (Button) findViewById(R.id.bEnableBlue);
        tvNapis = (TextView) findViewById(R.id.tvNapis);
        llBlueDevices = (View) findViewById(R.id.llBlueDevices);

        bSearch.setOnClickListener(this);
        bEnableBlue.setOnClickListener(this);
        lvSeznam.setOnItemClickListener(this);


        sectionPaired = new Section("Paired devices", "There are no paired devices.");

        sectionAvailable = new Section("Available devices", "To get new available devices, click on the search button below.");

        adapterDevices = new BlueDevicesAdapter(this);
        adapterDevices.addSection(sectionPaired);
        adapterDevices.addSection(sectionAvailable);

        lvSeznam.setAdapter(adapterDevices);

        if(type == 0) {
            //setState(States.INITIALIZING);
            startBluetooth();

            sectionAvailable.setType(Types.MESSAGE);
            //adapterDevices.setType(Types.MESSAGE, sectAvailableId);
        }


        if(type == 1) {
            mBlue = BluetoothArduino.getInstance("bc417naprava");

        }


    }


    private boolean startBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            izpisiInfo("Bluetooth not supported");
            setState(States.NOT_SUPPORTED);

            return false;
        }else{
            izpisiInfo("Bluetooth supported");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            izpisiInfo("Bluetooth not enabled");
            enableBluetooth();
            return false;
        }

        setState(States.ENABLED);
        //adapterDevices.setNewList(mBluetoothAdapter.getBondedDevices(), sectPairedId);
        sectionPaired.setDeviceList(mBluetoothAdapter.getBondedDevices());
        llBlueDevices.setVisibility(View.VISIBLE);
        bEnableBlue.setVisibility(View.GONE);

        return true;
    }




    @Override
    public void alertStart() {
        Log.d(DEBUG_TAG, "alertStart");
        //vse ze nardi funkcija onClick
    }

    @Override
    public void alertConnected() {
        Log.d(DEBUG_TAG, "alertConnected");
        adapterDevices.oneIsConnecting(false, -1);
        adapterDevices.getItem(positionConnecting).connecting = false;
        Toast.makeText(this, "Connection OK, oppening new Activity!", Toast.LENGTH_SHORT).show();
        adapterDevices.oneIsConnecting(false, -1);


        //osvezi list z paired napravami
        sectionPaired.setDeviceList(mBluetoothAdapter.getBondedDevices());

        //poklicce nov activity
        Intent mIntent = new Intent(this, MessageActiviy.class);
        mIntent.putExtra("blueMng", new BluetoothMngrParcelable(bluetoothMngr));
        startActivity(mIntent);
    }

    @Override
    public void alertCancelled() {
        Log.d(DEBUG_TAG, "alertCancelled");

        //naprava se že povezuje zato prekliče povezovanje
        adapterDevices.oneIsConnecting(false, -1);
        adapterDevices.getItem(positionConnecting).connecting = false;
        adapterDevices.requestRefresh();
        setState(States.ENABLED);

    }

    @Override
    public void alertError(String error) {
        Log.d(DEBUG_TAG, "alertError: " + error);

        //naprava se že povezuje zato prekliče povezovanje
        adapterDevices.oneIsConnecting(false, -1);
        adapterDevices.getItem(positionConnecting).connecting = false;
        adapterDevices.requestRefresh();
        setState(States.ENABLED);

        Toast.makeText(this, "Error connecting with " + adapterDevices.getItem(positionConnecting).blueDev.getName() , Toast.LENGTH_LONG).show();
    }

    private enum States {INITIALIZING, NOT_SUPPORTED, DISABLED, ENABLED, SEARCHING, ENABLING, CONNECTING, NOT_ENABLED}

    private void setState(States stanje){
        switch (stanje){
            case NOT_ENABLED:
                tvNapis.setText("You have to enable the bluetooth!");
                bSearch.setEnabled(false);
                bEnableBlue.setEnabled(true);
                break;

            case ENABLING:
                tvNapis.setText("Enabling the bluetooth ...");
                bSearch.setEnabled(false);
                bEnableBlue.setEnabled(false);
                break;

            case INITIALIZING:
                tvNapis.setText("Starting the bluetooth ...");
                bSearch.setEnabled(false);
                break;

            case NOT_SUPPORTED:
                tvNapis.setText("We are sorry, but your device doesn't support bluetooth.");
                bSearch.setEnabled(false);
                bEnableBlue.setVisibility(View.GONE);
                llBlueDevices.setVisibility(View.GONE);
                break;

            case DISABLED:
                tvNapis.setText("Bluetooth not enabled");
                bSearch.setEnabled(false);
                break;

            case ENABLED:
                tvNapis.setText("Select a device");
                bSearch.setEnabled(true);
                break;

            case SEARCHING:
                tvNapis.setText("Searching for devices ...");
                bSearch.setEnabled(false);
                //pbSearching.setVisibility(View.VISIBLE);
                //tvStAvailable.setVisibility(View.GONE);
                break;

            case CONNECTING:
                tvNapis.setText("Connecting ...");
                bSearch.setEnabled(false);
                break;

        }

    }

    private void isciNaprave(){
        if(mBluetoothAdapter == null) return;

        bSearch.setEnabled(false);
        izpisiInfo("Searching for new devices");
        if(mBluetoothAdapter.startDiscovery()){
            izpisiInfo("discovery started ... ");
        }else{
            izpisiInfo("discovery failed !");
            bSearch.setEnabled(true);
            return;
        }

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        isRegisterReciever = true; //pove da je bil registriran

        setState(States.SEARCHING);
        sectionAvailable.clearDeviceList();
        sectionAvailable.setLoading(true);
        sectionAvailable.setType(Types.EMPTY);
        lvSeznam.setSelection(adapterDevices.getCount()-1); //scrolla na dno
    }

    void enableBluetooth(){
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
        onActivityResult(1, 1, enableBtIntent);
        setState(States.ENABLING);
        bEnableBlue.setVisibility(View.VISIBLE);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mBluetoothAdapter != null) {
            if(mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
        }
        if(isRegisterReciever) unregisterReceiver(mReceiver);
        Log.d(DEBUG_TAG, "destroy");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mBluetoothAdapter != null) {
            if(mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
        }
        Log.d(DEBUG_TAG, "stop");
    }

    @Override
    public void onResume() {
        super.onResume();
        checkBlueConnection();
        Log.d(DEBUG_TAG, "resume");
    }


    void checkBlueConnection(){
        if(mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                llBlueDevices.setVisibility(View.VISIBLE);
                bEnableBlue.setVisibility(View.GONE);
                setState(States.ENABLED);
            } else {
                llBlueDevices.setVisibility(View.GONE);
                bEnableBlue.setVisibility(View.VISIBLE);
                bEnableBlue.setEnabled(true);
                setState(States.NOT_ENABLED);
            }
        }
    }


    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            izpisiInfo("action :" + action);
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                izpisiInfo("found new one");
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                boolean paired = false;
                for(BluetoothDevice pairedDev : mBluetoothAdapter.getBondedDevices()){
                    if(pairedDev.equals(device)){
                        paired = true;
                        break;
                    }
                }

                if(!paired){
                    mainThreadAddDevice(device);
                    sectionAvailable.setType(Types.DEVICES);
                    mainThreadScrollToBottom();
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                izpisiInfo("search finished");
                mainThreadScrollToBottom();

                //naprava lahko prejme discovery finished takrat ko se začne povezovt
                if(!bluetoothMngr.isConnecting()) setState(States.ENABLED);

                if(sectionAvailable.getDevicesSize() == 0){
                    sectionAvailable.setMessage("No nearby devices found");
                    sectionAvailable.setType(Types.MESSAGE);
                }else{
                    sectionAvailable.setType(Types.DEVICES);
                }

                sectionAvailable.setLoading(false);
            }
        }
    };

    private void izpisiInfo(String info){
        Log.d(DEBUG_TAG, info);
    }


    void mainThreadScrollToBottom(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lvSeznam.setSelection(adapterDevices.getCount() - 1);
            }
        });
    }

    void mainThreadAddDevice(final BluetoothDevice device){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sectionAvailable.addBluetoothDevice(device);
            }
        });
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
        Log.d(DEBUG_TAG, String.format("Result request:%d, result:%d", requestCode, resultCode));
        if(resultCode == RESULT_OK){
            setState(States.ENABLED);
            llBlueDevices.setVisibility(View.VISIBLE);
            bEnableBlue.setVisibility(View.GONE);
            sectionPaired.setDeviceList(mBluetoothAdapter.getBondedDevices());

        }else if(resultCode == RESULT_CANCELED){
            setState(States.NOT_ENABLED);
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.bSearch:
                if(type == 0) isciNaprave();
                if(type == 1) {
                    bSearch.setEnabled(true);
                    bSearch.setVisibility(View.VISIBLE);
                    mBlue.Connect();
                }
                break;

            case R.id.bEnableBlue:
                if(type == 0) enableBluetooth();
                if(type == 1){
                    mBlue.SendMessage("Hello world  ");
                }
                break;
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        //Log.d(DEBUG_TAG, "position:" + position + " itemName:" + adapterDevices.getItem(position).blueDev.getName());


        //pogleda če se kdo že povezuje
        if (!adapterDevices.oneIsConnecting) {

            Log.d(DEBUG_TAG, "start connecting click");
            if(mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
            adapterDevices.oneIsConnecting(true, position);
            adapterDevices.getItem(position).connecting = true;
            adapterDevices.requestRefresh();
            setState(States.CONNECTING);
            positionConnecting = position;

            bluetoothMngr.Connect(adapterDevices.getItem(position).blueDev);
            //vse ostale stvari uredi alertStart

        }else{
            Log.d(DEBUG_TAG, "stop connecting click");
            //naprava se že povezuje zato samo oprekliče povezovanje
            bluetoothMngr.stopConnecting();
            //vse ostale stvari uredi alertCancelled
        }

    }

}
