package me.test.bertoncelj1.bluetoothapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by bertoncelj1 on 6/28/15.
 */
class BlueDevicesAdapter extends BaseAdapter implements ParentList {
    private final Context context;

    private List<Section> sections;

    private int devicePairingPosition = -1;

    //CONSTRUCTORS
    public BlueDevicesAdapter(Context context, List<Section> sections){
        this.sections = sections;
        this.context = context;
        refreshList();
    }

    public BlueDevicesAdapter(Context context){
        this(context, new ArrayList<Section>());
    }

    //vrne section number s katerim potem lahko pokličmo določen section
    public void addSection(Section section){
        section.setParent(this);
        sections.add(section);
    }

    public void setType(Types type, int sectionNmbr){
        sections.get(sectionNmbr).setType(type);
    }


    private void refreshList(){
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        int size = 0;

        for(Section section : sections){
            size +=  section.getSize();

        }
        return size;
    }

    //returns section for given position
    public Section getSection(int position){
        for(Section section : sections) {
            int sectionSize = section.getSize();

            if (position < sectionSize) return section;

            position -= sectionSize;
        }
        return null;
    }

    public void setDevicePairing(boolean state, int position){
        devicePairingPosition = position;

        for(Section section : sections) {
            int sectionSize = section.getSize();

            if (position < sectionSize) section.setDevicePairing(state, position - 1);//-1 zaradi headerja

            position -= sectionSize;
        }
    }

    //removes pairing device
    public void removeDevicePairing(){
        int position = devicePairingPosition;
        ListItemDevice device = null;

        setDevicePairingOff();//because device is removed, it is unreasonable for it to be in pairing state

        for(Section section : sections) {
            int sectionSize = section.getSize();

            if (position < sectionSize) {
                section.removeDevice(position - 1);//removes device from old section
                return;
            }
            position -= sectionSize;
        }

    }

    public void setDevicePairingOff(){
        if(devicePairingPosition > 0) {
            setDevicePairing(false, devicePairingPosition);
            devicePairingPosition = -1;
        }
    }

    public boolean isDevicePairing(int position){
        for(Section section : sections) {
            int sectionSize = section.getSize();

            if (position < sectionSize) return section.isDevicePairing(position - 1);//-1 zaradi headerja

            position -= sectionSize;
        }

        return false;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position){
        if(devicePairingPosition > 0)return false;

        for(Section section : sections){
            int sectionSize = section.getSize();

            if(position < sectionSize) return section.isEnabled(position);

            position -= sectionSize;
        }

        //position not in range
        return false;

    }


    //Get the data item associated with the specified position in the data set.
    @Override
    public ListItemDevice getItem(int position) {
        for(Section section : sections){
            int sectionSize = section.getSize();

            if(position < sectionSize) return section.getDevice(position-1);//-1 zarad heeaderja

            position -= sectionSize;
        }
        return null;
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        for(Section section : sections){
            int sectionSize = section.getSize();

            if(position < sectionSize) return section.getView(position, view, parent, context);

            position -= sectionSize;
        }

        return view;
    }


    @Override
    public void requestRefresh() {
        refreshList();
    }
}

interface ParentList{
    void requestRefresh();
}


enum Types {MESSAGE, DEVICES, EMPTY}

class Section{
    //header
    private int headerStevilo = 0;
    private String headerNaslov = "";
    private boolean headerLoading = false;

    //message
    private String message = "";

    //devices
    private List<ListItemDevice> devices = new ArrayList<>();

    //pove tip o temu seznamu
    private Types type = Types.MESSAGE;

    ParentList parent= null;


    public Section(String headerNaslov, String message){
        this.headerNaslov = headerNaslov;
        this.message = message;
    }

    public void setType(Types type){
        this.type = type;
        if(type == Types.MESSAGE)clearDevices();
    }

    public void setParent(ParentList parent){
        this.parent = parent;
    }

    //vrne velikost seznama ... koliko vrstic ima
    public int getSize(){
        switch(type){
            case MESSAGE:
                return 2;

            case DEVICES:
                return devices.size() + 1;

            case EMPTY:
                return 1;
        }

        return 0;
    }


    public View getView(int position, View view, ViewGroup parent, Context context) {

        //todo check if view is not null, recycle the views

        //get header view
        if(position == 0){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_header, parent,false);

            TextView naslov = (TextView)view.findViewById(R.id.tvHeaderNaslov);
            TextView stevilo = (TextView)view.findViewById(R.id.tvHeaderStevilo);
            ProgressBar pb = (ProgressBar) view.findViewById(R.id.pbHeaderSearching);

            naslov.setText(headerNaslov);

            //skrije ali pa prikaže število naprav glede na to ali se nalaga ali ne
            stevilo.setVisibility((!headerLoading)? View.VISIBLE : View.GONE);
            stevilo.setText(headerStevilo + "");

            pb.setVisibility((headerLoading)? View.VISIBLE : View.GONE);

            return view;
        }

        //get message
        if(position == 1 && type == Types.MESSAGE){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_message, parent,false);

            TextView tvMessage = (TextView)view.findViewById(R.id.tvMessage);

            tvMessage.setText(message);

            return view;
        }

        //get devices view
        if(position > 0 && type == Types.DEVICES){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_blue_devs, parent,false);

            TextView ime = (TextView)view.findViewById(R.id.tvFirstLine);
            TextView adress = (TextView)view.findViewById(R.id.tvSecondLine);
            ProgressBar pb = (ProgressBar) view.findViewById(R.id.pbConnecting);
            View mark =  view.findViewById(R.id.mark);


            ListItemDevice device = getDevice(position - 1);
            String adressString;


            if(device.pairing){
                pb.setVisibility(View.VISIBLE);
                ime.setEnabled(false);
                adress.setEnabled(false);
                adressString = "pairing ...";
            }else{
                pb.setVisibility(View.GONE);
                ime.setEnabled(true);
                adress.setEnabled(true);
                adressString = device.blueDev.getAddress();
            }

            ime.setText(device.blueDev.getName());
            adress.setText(adressString);

            //prikaze znak v primeru da je ta naprava vrjetno bluetooth device ki ga iščemo
            int markVisible = (device.approved)?  View.VISIBLE : View.GONE;
            mark.setVisibility(markVisible);

            return view;
        }

        return view;
    }

    public void clearDeviceList(){
        devices.clear();
        headerStevilo = 0;
        requestParentRefresh();
    }



    public ListItemDevice getDevice(int position){
        if(position < 0)throw new IndexOutOfBoundsException("position is " + position + " < 0");

        return devices.get(position);
    }


    public void setHeader(String naslov){
        headerNaslov = naslov;
    }

    public void setMessage(String message){
        this.message = message;
    }

    public void setDeviceList(Set<BluetoothDevice> blueDevList){
        clearDeviceList();

        //doda vse naprave
        for(BluetoothDevice device : blueDevList) {
            addDevice(newListItemDevice(device));
        }

        setType(Types.DEVICES);
    }



    public void addBluetoothDevice(BluetoothDevice dev){
        addDevice(newListItemDevice(dev));
        requestParentRefresh();
    }

    private void requestParentRefresh(){
        if(parent == null)return;
        parent.requestRefresh();
    }

    private ListItemDevice newListItemDevice(BluetoothDevice device){
        return new ListItemDevice(device);
    }


    public void addDevice(ListItemDevice device){
        devices.add(device);
        headerStevilo = devices.size();
    }

    public void removeDevice(int position){
        devices.remove(position);
        headerStevilo = devices.size();
        requestParentRefresh();
    }

    private void clearDevices(){
        devices.clear();
        headerStevilo = 0;
    }

    public void setLoading(boolean set){
        headerLoading = set;
        requestParentRefresh();
    }

    public int getDevicesSize(){
        return devices.size();
    }
    public void setHeaderNaslov(String naslov){
        headerNaslov = naslov;
    }

    public void setPairing(boolean set, int position) {devices.get(position).pairing = set;}

    public boolean isEnabled(int position) {
        //is header enabled ?
        if(position == 0){
            return false;
        }

        //is message enabled ?
        if(position == 1 && type == Types.MESSAGE){
            return false;
        }

        //is device enabled
        if(position > 0 && type == Types.DEVICES){
            return true;
        }

        return false;
    }

    public void setDevicePairing(boolean state, int position) {
        getDevice(position).pairing = state;
    }

    public boolean isDevicePairing(int position) {
        return getDevice(position).pairing;
    }
}


class ListItemDevice{
    BluetoothDevice blueDev;
    boolean approved = false; // pove če je ta naprava ta prava .. če je dejansko bluetooth module
    boolean pairing = false;

    private ListItemDevice(BluetoothDevice blueDev, boolean approved){
        this.approved = approved;
        this.blueDev = blueDev;
    }

    public ListItemDevice(BluetoothDevice blueDev) {
        this(blueDev, isBC417Device(blueDev));
    }

    private static boolean isBC417Device(BluetoothDevice device){
        return device.getName().contains("bc417");
    }


}

