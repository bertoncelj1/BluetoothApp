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

import java.lang.reflect.Method;


public class MainActivity extends ActionBarActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private ListView lvSeznam;
    private TextView tvNapis;
    private Button bSearch, bEnableBlue;
    private BluetoothAdapter mBluetoothAdapter;
    private View llBlueDevices;


    private final String DEBUG_TAG = "infoBlue";


    BlueDevicesAdapter adapterDevices;
    //BluetoothMngr bluetoothMngr = BluetoothMngr.getInstance(this, this);

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

        registerRecievers();
        initElements();
        initList();


        setState(States.INITIALIZING);
        startBluetooth();

    }
    private void registerRecievers(){
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); //for pairing
        filter.addAction(BluetoothDevice.ACTION_FOUND);              //for searching
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//for searching
        registerReceiver(mReceiver, filter);
        isRegisterReciever = true; //pove da je bil registriran
    }
    private void initElements(){
        lvSeznam = (ListView) findViewById(R.id.lvSeznam);
        bSearch = (Button) findViewById(R.id.bSearch);
        bEnableBlue = (Button) findViewById(R.id.bEnableBlue);
        tvNapis = (TextView) findViewById(R.id.tvNapis);
        llBlueDevices = (View) findViewById(R.id.llBlueDevices);

        bSearch.setOnClickListener(this);
        bEnableBlue.setOnClickListener(this);
        lvSeznam.setOnItemClickListener(this);
    }

    private void initList(){
        sectionPaired = new Section("Paired devices", "There are no paired devices.");
        sectionAvailable = new Section("Available devices", "To get new available devices, click on the search button below.");
        sectionAvailable.setType(Types.MESSAGE);

        adapterDevices = new BlueDevicesAdapter(this);
        adapterDevices.addSection(sectionPaired);
        adapterDevices.addSection(sectionAvailable);

        lvSeznam.setAdapter(adapterDevices);
    }


    private boolean startBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.d(DEBUG_TAG, "Bluetooth not supported");
            setState(States.NOT_SUPPORTED);

            return false;
        }else{
            Log.d(DEBUG_TAG, "Bluetooth supported");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(DEBUG_TAG, "Bluetooth not enabled");
            enableBluetooth();
            return false;
        }

        setState(States.ENABLED);
        sectionPaired.setDeviceList(mBluetoothAdapter.getBondedDevices());
        adapterDevices.requestRefresh();
        llBlueDevices.setVisibility(View.VISIBLE);
        bEnableBlue.setVisibility(View.GONE);


        return true;
    }






    private enum States {INITIALIZING, NOT_SUPPORTED, DISABLED, ENABLED, SEARCHING, ENABLING, NOT_ENABLED, PAIRING}

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

            case PAIRING:
                tvNapis.setText("Pairing ...");
                bSearch.setEnabled(false);

                break;

        }

    }

    private void isciNaprave(){
        if(mBluetoothAdapter == null) return;

        bSearch.setEnabled(false);
        Log.d(DEBUG_TAG, "Searching for new devices");
        if(mBluetoothAdapter.startDiscovery()){
            Log.d(DEBUG_TAG, "discovery started ... ");
        }else{
            Log.d(DEBUG_TAG, "discovery failed !");
            bSearch.setEnabled(true);
            return;
        }

        setState(States.SEARCHING);
        adapterDevices.setDevicePairingOff();
        sectionAvailable.clearDeviceList();
        sectionAvailable.setLoading(true);
        sectionAvailable.setType(Types.EMPTY);
        lvSeznam.setSelection(adapterDevices.getCount() - 1); //scrolla na dno
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
        //refreshes list TODO check is list has changed
        sectionPaired.setDeviceList(mBluetoothAdapter.getBondedDevices());
        adapterDevices.requestRefresh();
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

    private void addAvailableDevice(BluetoothDevice device){
        //looks if device is alerady paired
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

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(DEBUG_TAG, "action :" + action);

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d(DEBUG_TAG, "found new one");
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                addAvailableDevice(device);
            }

            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.d(DEBUG_TAG, "search finished");
                mainThreadScrollToBottom();

                //naprava lahko prejme discovery finished takrat ko se začne povezovt
                setState(States.ENABLED);

                if(sectionAvailable.getDevicesSize() == 0){
                    sectionAvailable.setMessage("No nearby devices found");
                    sectionAvailable.setType(Types.MESSAGE);
                }else{
                    sectionAvailable.setType(Types.DEVICES);
                }

                sectionAvailable.setLoading(false);
            }

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Log.d(DEBUG_TAG, "Paired");
                    Toast.makeText(getApplicationContext(), "Paired", Toast.LENGTH_SHORT).show();

                    adapterDevices.removeDevicePairing();

                    //updates section paired list
                    sectionPaired.setDeviceList(mBluetoothAdapter.getBondedDevices());
                    adapterDevices.requestRefresh();
                }

                else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Log.d(DEBUG_TAG, "Unpaired");
                }

                else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDING){
                    Log.d(DEBUG_TAG, "Pairing failed");
                    Toast.makeText(getApplicationContext(), "Pairing failed", Toast.LENGTH_SHORT).show();
                }

                if(state != BluetoothDevice.BOND_BONDING){
                    Log.d(DEBUG_TAG, "Paired over");
                    adapterDevices.setDevicePairingOff();
                    mainThreadRefreshList();

                }else{

                }
            }
        }
    };


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

    void mainThreadRefreshList(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapterDevices.requestRefresh();
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
                isciNaprave();
                break;

            case R.id.bEnableBlue:
                enableBluetooth();
                break;
        }
    }

    public void pairDevice(BluetoothDevice blueDevice) {
        try {
            Log.d(DEBUG_TAG, "Start Pairing...");
            if(mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();


            Method m = blueDevice.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(blueDevice, (Object[]) null);



        } catch (Exception e) {
            Log.d(DEBUG_TAG, "Pairing failed...");
            Log.e(DEBUG_TAG, e.getMessage());
        }
    }

    public void unpairDevice(BluetoothDevice blueDevice) {
        try {
            Log.d(DEBUG_TAG, "Start unpair...");


            Method m = blueDevice.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(blueDevice, (Object[]) null);
        } catch (Exception e) {
            Log.d(DEBUG_TAG, "Unpairing failed...");
            Log.e(DEBUG_TAG, e.getMessage());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Section sectionClicked = adapterDevices.getSection(position);

        if(sectionClicked == sectionAvailable){
            if(!adapterDevices.isDevicePairing(position)) {
                Log.d(DEBUG_TAG, "Section available");
                adapterDevices.setDevicePairing(true, position);
                setState(States.PAIRING);
                mainThreadRefreshList();

                pairDevice(adapterDevices.getItem(position).blueDev);
            }
        }

        else if(sectionClicked == sectionPaired){
            Log.d(DEBUG_TAG, "Section paired");
            BluetoothDevice device = adapterDevices.getItem(position).blueDev;

            if(device != null) {
                //calls new activity
                Intent mIntent = new Intent(this, MessageActiviy.class);
                mIntent.putExtra("blueDeviceName", device.getName());
                mIntent.putExtra("blueDeviceAddress", device.getAddress());
                startActivity(mIntent);
            }
        }

        else{
            Log.w(DEBUG_TAG, "Unknown section");
        }


    }



}
