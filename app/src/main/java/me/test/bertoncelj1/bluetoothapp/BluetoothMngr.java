package me.test.bertoncelj1.bluetoothapp;


import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothMngr extends Thread {
    private BluetoothDevice mBlueDevice = null;
    OutputStream mOut;
    InputStream mIn;
    private boolean connected = false;
    private boolean connecting = false;
    private List<String> mMessages = new ArrayList<>();
    private final String DEBUG_TAG = "infoBlue";
    private char DELIMITER = '\n';
    ConnectThread connectThread;

    connectionCallback alertClass;
    Activity alertActivity;


    private static BluetoothMngr __blue = null;

    public static BluetoothMngr getInstance(connectionCallback alertClass, Activity alertActivity){
        //if(bluDev == null && __blue == null) return null;
        return (__blue == null)? new BluetoothMngr(alertClass, alertActivity) : __blue;
    }

    public boolean isConnecting(){return connecting;};


    private BluetoothMngr(connectionCallback alertClass,Activity alertActivity){
        this.alertActivity = alertActivity;
        this.alertClass = alertClass;
        __blue = this;

        for(int i = 0; i < 2048; i++) {
            mMessages.add("");
        }

    }


    public void Connect(BluetoothDevice bluDev){
        mBlueDevice = bluDev;
        try{
            LogMessage("\t\tConnecting to the device " + bluDev.getName() + " adress: " + bluDev.getAddress());
            alertClass.alertStart();
            connecting = true;

            connectThread = new ConnectThread(mBlueDevice);
            connectThread.start();


        }catch (Exception e) {
            LogError("\t\t[#]Error while connecting: " + e.getMessage());
            alertClass.alertError(e.getMessage());
        }
    }

    //prekiliče povezovanju.. ubistvu samo ignorira če se je že povezal in ne prikaže errorje
    public void stopConnecting(){
        if(!connecting)return;
        connecting = false;
        connectThread.cancel();
        alertClass.alertCancelled();
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        private final UUID MY_UUID = UUID.fromString("1ed81c30-1a89-11e5-b60b-1697f925ec7b");

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                connecting = false;
                alertErrorToMain(e.getMessage());
            }
            mmSocket = tmp;
        }

        public void run() {
            try {
                Log.d(DEBUG_TAG, "connect start");

                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                Log.d(DEBUG_TAG, "connect over");

                //če je uporabnik preklicou connecting
                if(!connecting){
                    mmSocket.close();
                    return;
                }
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    Log.d(DEBUG_TAG, "connect connectException");
                    if(!connecting)return;
                    connecting = false;

                    alertErrorToMain(connectException.getMessage());

                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
                connecting = false;
            } catch (IOException e) { }
        }
    }

    //pošlej alert glavnemu threadu tako da ta lahko updejta view
    private void alertErrorToMain(final String error){
        alertActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertClass.alertError(error);
            }
        });

    }

    //pošlej alert glavnemu threadu tako da ta lahko updejta view
    private void alertConnectedToMain(){
        alertActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertClass.alertConnected();
            }
        });

    }

    public void manageConnectedSocket(BluetoothSocket socket){
        try {
            connecting = false;
            mOut = socket.getOutputStream();
            mIn = socket.getInputStream();
            connected = true;
            this.start();
            LogMessage("\t\t\t" + mBlueDevice.getName());
            LogMessage("\t\tOk!!");
            alertConnectedToMain();

        } catch (IOException e) {
            Log.d(DEBUG_TAG, "connect manageConnectedSocket");
            alertClass.alertError(e.getMessage());
        }

    }


    public void run(){
        byte ch, buffer[] = new byte[1024];
        int i;

        while (true) {
            if(connected) try {

                i = 0;
                while ((ch = (byte) mIn.read()) != DELIMITER) {
                    buffer[i++] = ch;
                }
                buffer[i] = '\0';

                final String msg = new String(buffer);

                MessageReceived(msg.trim());
                LogMessage("[Blue]:" + msg);

            } catch (IOException e) {
                LogError("->[#]Failed to receive message: " + e.getMessage());
            }
        }
    }

    private void MessageReceived(String msg){
        try {

            mMessages.add(msg);
                this.notify();
                //todo triger callback
        } catch (Exception e){
            LogError("->[#] Failed to receive message: " + e.getMessage());
        }
    }

    public boolean hasMessages(int i){
        try{
            String s = mMessages.get(i);
            return (s.length() > 0);
        } catch (Exception e){
            return false;
        }
    }

    public String getDevName(){return mBlueDevice.getName();}
    public String getDevAddress(){return mBlueDevice.getAddress();}


    public String getMessage(int i){
        return mMessages.get(i);
    }

    public void clearMessages(){mMessages.clear();}

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

    private void LogMessage(String msg){
        Log.d(DEBUG_TAG, msg);
    }

    private void LogError(String msg){
        Log.e(DEBUG_TAG, msg);
    }

    public void setDelimiter(char d){
        DELIMITER = d;
    }
    public char getDelimiter(){
        return DELIMITER;
    }

    interface connectionCallback{
        void alertStart();
        void alertConnected();
        void alertError(String error);
        void alertCancelled();
    }

}


