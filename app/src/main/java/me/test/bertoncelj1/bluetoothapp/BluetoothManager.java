package me.test.bertoncelj1.bluetoothapp;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager extends Thread {
    private BluetoothAdapter mBlueAdapter = null;
    private BluetoothSocket mBlueSocket = null;
    private BluetoothDevice mBlueDevice = null;
    OutputStream mOut;
    InputStream mIn;
    private boolean deviceFound = false;
    private boolean connected = false;
    private int REQUEST_BLUE_ATIVAR = 10;
    private String deviceName, deviceAddress;
    private List<String> mMessages = new ArrayList<String>();
    private String TAG = "BluetoothManager";
    private char DELIMITER = '#';

    ConnectionStatus asd;

    interface ConnectionStatus{
        void connectionConnected();
        void connectionFailed(String message);
        void messageReceived(String message);
    }

    private static BluetoothManager __blue = null;

    //todo rename asd
    public static BluetoothManager getInstance(String name, String address, ConnectionStatus asd){
        return __blue == null ? new BluetoothManager(name, address, asd) : __blue;
    }

    private  BluetoothManager(String Name, String Address, ConnectionStatus asd){
        __blue = this;
        try {
            for(int i = 0; i < 2048; i++){
                mMessages.add("");
            }
            deviceName = Name;
            deviceAddress = Address;
            this.asd = asd;
            mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBlueAdapter == null) {
                LogError("\t\t[#]Phone does not support bluetooth!!");
                return;
            }
            if (!isBluetoothEnabled()) {
                LogError("[#]Bluetooth is not activated!!");
            }

            Set<BluetoothDevice> paired = mBlueAdapter.getBondedDevices();
            if (paired.size() > 0) {
                for (BluetoothDevice d : paired) {
                    if (d.getName().equals(deviceName) && d.getAddress().equals(deviceAddress)) {
                        mBlueDevice = d;
                        deviceFound = true;
                        break;
                    }
                }
            }

            if (!deviceFound)
                LogError("\t\t[#]There is not robot paired!!");

        }catch (Exception e){
            LogError("\t\t[#]Erro creating Bluetooth! : " + e.getMessage());
        }

    }

    public boolean isBluetoothEnabled(){
        return mBlueAdapter.isEnabled();
    }

    public boolean Connect(){
        if(!deviceFound)
            return false;
        try{
            LogMessage("\t\tConncting to the robot...");

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            mBlueSocket = mBlueDevice.createRfcommSocketToServiceRecord(uuid);
            mBlueSocket.connect();
            mOut = mBlueSocket.getOutputStream();
            mIn = mBlueSocket.getInputStream();
            connected = true;
            this.start();

            asd.connectionConnected();
            SendMessage("Hello Bluetooth, we are connected1");
            LogMessage("\t\t\t" + mBlueAdapter.getName());
            LogMessage("\t\tOk!!");
            return true;

        }catch (Exception e){
            asd.connectionFailed("Unable to connect");
            LogError("\t\t[#]Error while connecting: " + e.getMessage());
            return false;
        }
    }


    public void pairDevice() {
        try {
            LogMessage("Start Pairing...");
            if(!deviceFound){
                LogMessage("No robo no money");
                return;
            }

            Method m = mBlueDevice.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(mBlueDevice, (Object[]) null);

            LogMessage("Pairing finished.");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void unpairDevice() {
        try {
            LogMessage("Start unpair...");
            if(!deviceFound){
                LogMessage("No robo no money");
                return;
            }


            Method m = mBlueDevice.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(mBlueDevice, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }


    public void run() {

        while (true) {
            if (connected) {
                try {
                    int ch;
                    byte buffer[] = new byte[1024];
                    int i = 0;

                    String s = "";
                    //while ((ch = mIn.read()) > 0) {
                        buffer[i++] = (byte)mIn.read();
                    //}

                    final String msg = new String(buffer, 0, i);

                    MessageReceived(msg);

                    Thread.sleep(10);
                } catch (IOException e) {
                    LogError("->[#]Failed to receive message: " + e.getMessage());
                    LogError("-> connected = false");
                    connected = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private void MessageReceived(String msg){
        try {
            SendMessage(msg); //echo
            mMessages.add(msg);
            asd.messageReceived(msg);
        } catch (Exception e){
            LogError("->[#] Failed to receive message: " + e.getMessage());
        }
    }

    public boolean hasMensage(int i){
        try{
            String s = mMessages.get(i);
            if(s.length() > 0)
                return true;
            else
                return false;
        } catch (Exception e){
            return false;
        }
    }

    public String getMenssage(int i){
        return mMessages.get(i);
    }

    public void clearMessages(){
        mMessages.clear();
    }

    public int countMessages(){
        return mMessages.size();
    }

    public String getLastMessage(){
        if(countMessages() == 0)
            return "";
        return mMessages.get(countMessages()-1);
    }

    public void SendMessage(String msg){
        try {
            if(connected) {
                mOut.write(msg.getBytes());
            }

        } catch (IOException e){
            LogError("->[#]Error while sending message: " + e.getMessage());
        }
    }

    public void LogMessage(String msg){
        Log.d(TAG, msg);
    }

    public void LogError(String msg){
        Log.e(TAG, msg);
    }

    public void setDelimiter(char d){
        DELIMITER = d;
    }
    public char getDelimiter(){
        return DELIMITER;
    }

}

