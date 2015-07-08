package me.test.bertoncelj1.bluetoothapp;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothConnectorAsync extends Thread{

    private BluetoothSocketWrapper bluetoothSocket;
    private BluetoothDevice device;
    private boolean secure;
    private UUID uuidNumber;
    private int candidate;
    private BluetConnAsyncCallback callbackClass;


    /**
     * @param device the device
     * @param secure if connection should be done via a secure socket
     * @param uuidNumber an UUID number, if null predefined number is used
     */
    public BluetoothConnectorAsync(BluetoothDevice device, boolean secure, UUID uuidNumber, BluetConnAsyncCallback callbackClass) {
        this.device = device;
        this.secure = secure;
        this.uuidNumber = uuidNumber;
        this.callbackClass = callbackClass;

        if (this.uuidNumber == null) {
            this.uuidNumber = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//TODO set new UUID
        }
    }

    public void connect(){
        start();
    }

    @Override
    public void run() {
        boolean success = false;

        //pridobi si socket
        try {
            getSocket();
        } catch (IOException e) {
            callbackClass.blueConnEvent(BlueConnEvents.ERROR, null, "Unable to create socket.");
            return;
        };

        try {
            bluetoothSocket.connect();
            success = true;
        } catch (IOException e) {

            //try the fallback
            try {
                Log.e("BT", "napaka PRVA: " + e.getMessage());
                bluetoothSocket = new FallbackBluetoothSocket(bluetoothSocket.getUnderlyingSocket());
                Thread.sleep(500);
                bluetoothSocket.connect();
                success = true;
            } catch (FallbackException e1) {
                Log.w("BT", "Could not initialize FallbackBluetoothSocket classes.", e);
            } catch (InterruptedException e1) {
                Log.w("BT", e1.getMessage(), e1);
            } catch (IOException e1) {
                Log.w("BT", "Fallback failed. Cancelling.", e1);
            }
        }

        if (!success) {
            callbackClass.blueConnEvent(BlueConnEvents.ERROR, null, "Could not connect to device: "+ device.getAddress());
        }else{
            callbackClass.blueConnEvent(BlueConnEvents.CONNECTED, bluetoothSocket.getUnderlyingSocket(), "OK");
        }

    }

    private boolean getSocket() throws IOException {

        BluetoothSocket tmp;

        Log.i("BT", "Attempting to connect to Protocol: "+ uuidNumber);
        if (secure) {
            tmp = device.createRfcommSocketToServiceRecord(uuidNumber);
        } else {
            tmp = device.createInsecureRfcommSocketToServiceRecord(uuidNumber);
        }
        bluetoothSocket = new NativeBluetoothSocket(tmp);

        return true;
    }

    private static interface BluetoothSocketWrapper {

        InputStream getInputStream() throws IOException;

        OutputStream getOutputStream() throws IOException;

        String getRemoteDeviceName();

        void connect() throws IOException;

        String getRemoteDeviceAddress();

        void close() throws IOException;

        BluetoothSocket getUnderlyingSocket();

    }


    private static class NativeBluetoothSocket implements BluetoothSocketWrapper {

        private BluetoothSocket socket;

        public NativeBluetoothSocket(BluetoothSocket tmp) {
            this.socket = tmp;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return socket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return socket.getOutputStream();
        }

        @Override
        public String getRemoteDeviceName() {
            return socket.getRemoteDevice().getName();
        }

        @Override
        public void connect() throws IOException {
            socket.connect();
        }

        @Override
        public String getRemoteDeviceAddress() {
            return socket.getRemoteDevice().getAddress();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }

        @Override
        public BluetoothSocket getUnderlyingSocket() {
            return socket;
        }

    }

    private class FallbackBluetoothSocket extends NativeBluetoothSocket {

        private BluetoothSocket fallbackSocket;

        public FallbackBluetoothSocket(BluetoothSocket tmp) throws FallbackException {
            super(tmp);
            try
            {
                Class<?> clazz = tmp.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[] {Integer.valueOf(1)};
                fallbackSocket = (BluetoothSocket) m.invoke(tmp.getRemoteDevice(), params);
            }
            catch (Exception e)
            {
                throw new FallbackException(e);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return fallbackSocket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return fallbackSocket.getOutputStream();
        }


        @Override
        public void connect() throws IOException {
            fallbackSocket.connect();
        }


        @Override
        public void close() throws IOException {
            fallbackSocket.close();
        }

    }

    private static class FallbackException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public FallbackException(Exception e) {
            super(e);
        }

    }

    public enum BlueConnEvents {CONNECTED,ERROR};

    public interface BluetConnAsyncCallback{
        void blueConnEvent(BlueConnEvents event, BluetoothSocket socket, String message);
    }

}