package com.meyersj.explore.explore;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.meyersj.explore.communicate.AdvertisementCommunicator;
import com.meyersj.explore.R;
import com.meyersj.explore.communicate.Protocol;
import com.meyersj.explore.communicate.ProtocolMessage;
import com.meyersj.explore.nearby.NearbyBeacon;
import com.meyersj.explore.utilities.Cons;
import com.meyersj.explore.utilities.Utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ExploreFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private final String TAG = getClass().getCanonicalName();

    @Bind(R.id.start_button) Button startButton;
    @Bind(R.id.stop_button) Button stopButton;
    @Bind(R.id.status_text) TextView statusText;
    @Bind(R.id.nearby_list) ListView nearbyList;
    @Bind(R.id.message) EditText messageText;
    @Bind(R.id.save_message_icon) ImageView saveMessageButton;
    @Bind(R.id.message_layout) LinearLayout messageLayout;
    //@Bind(R.id.save_message_button) Button saveMessageButton;


    private com.meyersj.explore.communicate.AdvertisementCommunicator communicator;
    private ExploreBeaconAdapter exploreBeaconAdapter;
    private byte[] selectedAdvertisement;

        public static ExploreFragment newInstance(int sectionNumber) {
        ExploreFragment fragment = new ExploreFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public ExploreFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "create view");
        View rootView = inflater.inflate(R.layout.fragment_explore, container, false);
        ButterKnife.bind(this, rootView);
        ArrayList<NearbyBeacon> resultsList = new ArrayList<>();
        exploreBeaconAdapter = new ExploreBeaconAdapter(getContext(), resultsList);
        nearbyList.setAdapter(exploreBeaconAdapter);
        communicator = new AdvertisementCommunicator(getContext(), new ExploreHandler(this));
        communicator.start();
        setViewListeners();
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        Log.d(TAG, "save instance state");
        state.putString("status", statusText.getText().toString());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        communicator.stopScan();
        communicator.stop();
    }

    private void setViewListeners() {
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(getActivity(), "Start Exploring", Toast.LENGTH_SHORT).show();
                communicator.startScan();
                statusText.setText("Scan started");
                exploreBeaconAdapter.clear();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(getActivity(), "Stop Exploring", Toast.LENGTH_SHORT).show();
                communicator.stopScan();
                statusText.setText("Scan stopped");
                //exploreBeaconAdapter.clear();
            }
        });

        nearbyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                NearbyBeacon selectedBeacon = exploreBeaconAdapter.getItem(i);
                exploreBeaconAdapter.setActiveBeacon(selectedBeacon);
                selectedAdvertisement = selectedBeacon.advertisement;
                if (selectedAdvertisement != null) {
                    byte[] payload = Protocol.getMessages(selectedAdvertisement);
                    ProtocolMessage protocolMessage = new ProtocolMessage();
                    protocolMessage.payload = payload;
                    protocolMessage.payloadFlag = Protocol.GET_MESSAGE;
                    communicator.addMessage(protocolMessage);
                }
            }
        });

        saveMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get text
                String message = messageText.getText().toString();
                if (message.isEmpty()) {
                    statusText.setText("Message is empty");
                }
                else {
                    if (selectedAdvertisement != null) {
                        byte[] device = Utils.getDeviceID(getContext()).getBytes();
                        byte[] beacon = selectedAdvertisement;
                        byte[] user = Utils.getUser(getContext()).getBytes();
                        byte[] payload = Protocol.sendMessage(device, user, message.getBytes(), beacon);
                        ProtocolMessage protocolMessage = new ProtocolMessage();
                        protocolMessage.payload = payload;
                        protocolMessage.payloadFlag = Protocol.PUT_MESSAGE;
                        communicator.addMessage(protocolMessage);
                    }
                }
            }
        });
    }

    // callback function from ExploreHandler
    // returns response received from socket
    public void update(Message message) {
        Log.d(TAG, "update");
        Bundle data = message.getData();
        if (data != null) {
            boolean registered = false;
            byte[] advertisement = data.getByteArray(Cons.ADVERTISEMENT);
            String  name = data.getString(Cons.BEACON_KEY);
            Integer rssi = data.getInt(Cons.RSSI);
            byte[] flags = data.getByteArray(Cons.RESPONSE_FLAGS);
            Log.d(TAG, "FLAG: " + flags[0]);

            byte[] response;
            switch(data.getByte(Cons.PAYLOAD_FLAGS)) {
                case Protocol.PUT_MESSAGE:
                    response = data.getByteArray(Cons.RESPONSE);
                    if (response != null) {
                        try {
                            String responseString = new String(response, "UTF-8");
                            statusText.setText(responseString);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.d(TAG, "protocol message");
                case Protocol.CLIENT_UPDATE:
                    switch (flags[0]) {
                        case 0x00:
                            registered = true;
                            response = data.getByteArray(Cons.RESPONSE);
                            if (response != null) {
                                try {
                                    name = new String(response, "UTF-8");
                                    name = ProtocolMessage.parseBeaconName(name);
                                    Log.d(TAG, name);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        case 0x01:
                            break;
                    }
                    exploreBeaconAdapter.add(new NearbyBeacon(registered, advertisement, name, rssi));
                    break;
                case Protocol.GET_MESSAGE:
                    Log.d(TAG,"get messsage");
                    switch (flags[0]) {
                        case 0x00:
                            response = data.getByteArray(Cons.RESPONSE);
                            if (response != null) {
                                try {
                                    String responseMessage = new String(response, "UTF-8");
                                    responseMessage = ProtocolMessage.parseBeaconName(responseMessage);
                                    Log.d(TAG, responseMessage);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        case 0x01:
                            break;
                    }
                    break;
            }


        }
    }
}