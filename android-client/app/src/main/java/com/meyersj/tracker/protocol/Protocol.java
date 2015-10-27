package com.meyersj.tracker.protocol;


import android.util.Log;

import com.meyersj.tracker.Utils;

import java.nio.ByteBuffer;

public class Protocol {

    // Error Flags
    public static final byte SUCCESS = (byte) 0x00;
    public static final byte FAIL = (byte) 0x01;

    // Message Type Flags
    public static final byte CLOSE_CONN = (byte) 0x00;
    public static final byte REGISTER_CLIENT = (byte) 0x01;
    public static final byte REGISTER_BEACON = (byte) 0x02;
    public static final byte CLIENT_UPDATE = (byte) 0x03;


    public static final byte GET_STATUS = (byte) 0x04;
    public static final byte DELIMITER = (byte) 0xFF;


    // prefix length field and append delimiter to payload
    public static byte[] newPayload(byte[] inPayload) {
        byte[] payload = new byte[inPayload.length + 4 + 1];
        byte[] length = ByteBuffer.allocate(4).putInt(inPayload.length + 1).array();

        Log.d("PROTOCOL LENGTH", String.valueOf(inPayload.length + 1) + " " + Utils.getHexString(length));
        // copy length bytes
        for(int i = 0; i < 4; i++) {
            payload[i] = length[i];
        }
        // copy payload bytes
        for(int i = 0; i < inPayload.length; i++) {
            payload[4+i] = inPayload[i];
        }
        // add delimiter
        payload[payload.length - 1] = Protocol.DELIMITER;
        return payload;
    }

    // Close connection to server
    public static byte[] closeConnection() {
        byte[] payload = {Protocol.CLOSE_CONN};
        return newPayload(payload);
    }

    // update received beacon advertisement from client
    public static byte[] clientUpdate(byte[] device, byte[] adv, int rssi) {
        byte[] payload = new byte[5 + device.length + adv.length];
        payload[0] = Protocol.CLIENT_UPDATE;
        // signal strength
        payload[1] = (byte) 1;
        payload[2] = (byte) rssi;
        // device data
        payload[3] = (byte) device.length;
        for(int i = 0; i < device.length; i++) {
            payload[4+i] = device[i];
        }
        // advertisement data
        payload[4+device.length] = (byte) adv.length;
        for(int i = 0; i < adv.length; i++) {
            payload[5+device.length+i] = adv[i];
        }
        return newPayload(payload);
    }

    // register human readable name of beacon
    public static byte[] registerBeacon(byte[] name, byte[] adv, byte[] lat, byte[] lon) {
        byte[] payload = new byte[3+name.length+17+adv.length];
        int index = 0;
        payload[index++] = Protocol.REGISTER_BEACON;
        payload[index++] = (byte) name.length;
        for(int i = 0; i < name.length; i++) {
            payload[index++] = name[i];
        }
        payload[index++] = (byte) 16;
        for(int i = 0; i < 8; i++) {
            payload[index++] = lat[i];
        }
        for(int i = 0; i < 8; i++) {
            payload[index++] = lon[i];
        }
        payload[index++] = (byte) adv.length;
        for(int i = 0; i < adv.length; i++) {
            payload[index++] = adv[i];
        }
        return newPayload(payload);
    }

    // register human readable name of client
    public static byte[] registerClient(byte[] device, byte[] client) {
        byte[] payload = new byte[3 + device.length + client.length];
        payload[0] = Protocol.REGISTER_CLIENT;
        // device id
        payload[1] = (byte) device.length;
        //Log.d("PROTOCOL", Utils.getHexString(payload));
        //Log.d("PROTOCOL DEVICE", String.valueOf(device.length));
        for(int i = 0; i < device.length; i++) {
            payload[2+i] = device[i];
        }
        //Log.d("PROTOCOL", Utils.getHexString(payload));
        // client name
        payload[2+device.length] = (byte) client.length;
        //Log.d("PROTOCOL CLIENT", String.valueOf(client.length));
        //Log.d("PROTOCOL", Utils.getHexString(payload));
        for(int i = 0; i < client.length; i++) {
            payload[3+device.length+i] = client[i];
        }
        //Log.d("PROTOCOL", Utils.getHexString(payload));
        return newPayload(payload);
    }

}
