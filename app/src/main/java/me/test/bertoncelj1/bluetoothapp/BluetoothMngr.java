package me.test.bertoncelj1.bluetoothapp;


import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class BluetoothMngr extends Thread implements BluetoothConnectorAsync.BluetConnAsyncCallback {
    private BluetoothDevice mBlueDevice = null;
    OutputStream mOut;
    InputStream mIn;
    private boolean connected = false;
    private boolean connecting = false;
    private List<String> mMessages = new ArrayList<>();
    private final String DEBUG_TAG = "infoBlue";
    private char DELIMITER = '\n';
    BluetoothConnectorAsync blueConnector;
    private BluetoothSocket blueSocket;

    ConnectionCallback alertClass;
    Activity alertActivity;


    private static BluetoothMngr __blue = null;

    public static BluetoothMngr getInstance(ConnectionCallback alertClass, Activity alertActivity){
        //if(bluDev == null && __blue == null) return null;
        return (__blue == null)? new BluetoothMngr(alertClass, alertActivity) : __blue;
    }

    public boolean isConnecting(){return connecting;};


    private BluetoothMngr(ConnectionCallback alertClass,Activity alertActivity){
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
            LogMessage("\t\tConnecting to the device " + bluDev.getName() + " address: " + bluDev.getAddress());
            alertClass.alertStart();
            connecting = true;

            blueConnector = new BluetoothConnectorAsync(bluDev, true, null, this);
            blueConnector.connect();

        }catch (Exception e) {
            LogError("\t\t[#]Error while connecting: " + e.getMessage());
            alertClass.alertError(e.getMessage());
        }
    }

    @Override
    public void blueConnEvent(BluetoothConnectorAsync.BlueConnEvents event, BluetoothSocket socket, String message) {
        if(event == BluetoothConnectorAsync.BlueConnEvents.CONNECTED){
            manageConnectedSocket(socket);

        }else if(event == BluetoothConnectorAsync.BlueConnEvents.ERROR){
            alertErrorToMain();
        }
    }

    //prekiliče povezovanju.. ubistvu samo ignorira če se je že povezal in ne prikaže errorje
    public void stopConnecting(){
        /*
        if(!connecting)return;
        connecting = false;
        blueConnector.cancel();
        alertClass.alertCancelled();
        */
        //TODO
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

    //pošlej alert glavnemu threadu tako da ta lahko updejta view
    private void alertErrorToMain(){
        alertActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertClass.alertError("error");
            }
        });
    }


    public void manageConnectedSocket(BluetoothSocket socket){
        try {
            connecting = false;
            blueSocket = socket;
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

        connected = true;

        while (blueSocket.isConnected()) {
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

        //TODO trigger callback
        connected = false;
    }

    private void MessageReceived(String msg){
        try {

            mMessages.add(msg);
                this.notify();
                //todo trigger callback
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


    interface ConnectionCallback {
        void alertStart();
        void alertConnected();
        void alertError(String error);
        void alertCancelled();
    }

}

class BluetoothMngrParcelable implements Parcelable{
    private BluetoothMngr mngr;

    public BluetoothMngrParcelable(BluetoothMngr mngr){
        this.mngr = mngr;
    }

    BluetoothMngr getBluetoothMngr(){return mngr;};

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(mngr);
    }

    public static final Parcelable.Creator<BluetoothMngrParcelable> CREATOR
            = new Parcelable.Creator<BluetoothMngrParcelable>() {
        public BluetoothMngrParcelable createFromParcel(Parcel in) {
            return new BluetoothMngrParcelable(in);
        }

        public BluetoothMngrParcelable[] newArray(int size) {
            return new BluetoothMngrParcelable[size];
        }
    };

    private BluetoothMngrParcelable(Parcel in) {
        mngr = in.readValue();
    }
}


