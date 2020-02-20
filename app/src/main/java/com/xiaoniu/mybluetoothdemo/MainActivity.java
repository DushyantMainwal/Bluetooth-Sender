package com.xiaoniu.mybluetoothdemo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaoniu.mybluetoothdemo.adapter.BlueToothDeviceAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BluetoothAdapter bTAdatper;
//    private ListView listView;
//    private BlueToothDeviceAdapter adapter;

//    private TextView text_state;
//    private TextView text_msg;

    private final int BUFFER_SIZE = 1024;
    private static final String NAME = "BT_DEMO";
    private static final UUID BT_UUID = UUID.fromString("02001101-0001-1000-8080-00805F9BA9BA");

    public static final String btNameConnection = "Blade_Bluetooth";
    boolean connectSuccess = false;

    private ConnectThread connectThread;
    private ListenerThread listenerThread;
    List<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    private int count = 0;
    private TextView text_number;
    private ImageView decrement_iv, increment_iv;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        bTAdatper = BluetoothAdapter.getDefaultAdapter();
        initReceiver();
        listenerThread = new ListenerThread();
        listenerThread.start();
        openBlueTooth();
        searchDevices();

        connectSuccess = false;
    }

    private void initView() {
        text_number = findViewById(R.id.text_number);
        decrement_iv = findViewById(R.id.decrement_iv);
        increment_iv = findViewById(R.id.increment_iv);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        decrement_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connectThread != null) {
                    count--;
                    connectThread.sendMsg(String.valueOf(count));
                    text_number.setText(String.valueOf(count));
                }
            }
        });

        increment_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connectThread != null) {
                    count++;
                    connectThread.sendMsg(String.valueOf(count));
                    text_number.setText(String.valueOf(count));
                }
            }
        });

//        findViewById(R.id.btn_send).setOnClickListener(this);
//        text_state = (TextView) findViewById(R.id.text_state);
//        text_msg = (TextView) findViewById(R.id.text_msg);
//
//        listView = (ListView) findViewById(R.id.listView);
//        adapter = new BlueToothDeviceAdapter(getApplicationContext(), R.layout.bluetooth_device_list_item);
//        listView.setAdapter(adapter);
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                if (bTAdatper.isDiscovering()) {
//                    bTAdatper.cancelDiscovery();
//                }
//                BluetoothDevice device = (BluetoothDevice) adapter.getItem(position);
//                if (device.getName().equalsIgnoreCase("akshit")) {
//                    connectDevice(Objects.requireNonNull(device));
//                }
//            }
//        });
    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onClick(View v) {
//        if (v.getId() == R.id.btn_send) {
//            if (connectThread != null) {
//                connectThread.sendMsg("1");
//            }
//        }
    }



    private void openBlueTooth() {
        if (bTAdatper == null) {
            Toast.makeText(this, "The current device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        if (!bTAdatper.isEnabled()) {
           /* Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(i);*/
            bTAdatper.enable();
        }
        if (bTAdatper.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(i);
        }
    }


    private void searchDevices() {
        if (bTAdatper.isDiscovering()) {
            bTAdatper.cancelDiscovery();
        }
        getBoundedDevices();
        bTAdatper.startDiscovery();
    }


    private void getBoundedDevices() {
      Set<BluetoothDevice> pairedDevices = bTAdatper.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                bluetoothDevices.add(device);
                if (device.getName().equalsIgnoreCase(btNameConnection)) {
                    connectDevice(Objects.requireNonNull(device));
                }
            }
        }
    }


    private void connectDevice(BluetoothDevice device) {

//        text_state.setText(getResources().getString(R.string.connecting));

        try {
            //Socket
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(BT_UUID);
            connectThread = new ConnectThread(socket, true);
            connectThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bTAdatper != null && bTAdatper.isDiscovering()) {
            bTAdatper.cancelDiscovery();
        }
        unregisterReceiver(mReceiver);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (connectSuccess)
                    return;

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    bluetoothDevices.add(device);
//                    adapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Toast.makeText(MainActivity.this, "Start search", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (connectSuccess)
                    return;
                Toast.makeText(MainActivity.this, "Search completed", Toast.LENGTH_SHORT).show();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < bluetoothDevices.size(); i++) {
                            BluetoothDevice device = bluetoothDevices.get(i);
                            if(device==null||device.getName()==null)
                                continue;
                            if (device.getName().equalsIgnoreCase(btNameConnection)) {
                                connectDevice(Objects.requireNonNull(device));
                                break;
                            }
                        }
                    }
                });
            }
        }
    };


    private class ConnectThread extends Thread {

        private BluetoothSocket socket;
        private boolean activeConnect;
        InputStream inputStream;
        OutputStream outputStream;

        private ConnectThread(BluetoothSocket socket, boolean connect) {
            this.socket = socket;
            this.activeConnect = connect;
        }

        @Override
        public void run() {
            try {
                if (activeConnect) {
                    socket.connect();
                }
//                text_state.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        text_state.setText(getResources().getString(R.string.connect_success));
//                    }
//                });

                connectSuccess = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
                System.out.println("ConnectThread: " + getResources().getString(R.string.connect_success));
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytes;
                while (true) {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        final byte[] data = new byte[bytes];
                        System.arraycopy(buffer, 0, data, 0, bytes);
//                        text_msg.post(new Runnable() {
//                            @SuppressLint("SetTextI18n")
//                            @Override
//                            public void run() {
//                                text_msg.setText(getResources().getString(R.string.get_msg)+new String(data));
//                            }
//                        });
                        System.out.println("ConnectThread: " + getResources().getString(R.string.get_msg)+new String(data));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
//                text_state.post(new Runnable() {
//                    @Override
//                    public void run() {
//                                text_state.setText(getResources().getString(R.string.connect_error));
//                    }
//                });

                System.out.println("ConnectThread: " + getResources().getString(R.string.connect_error));
            }
        }


        private void sendMsg(final String msg) {
            byte[] bytes = msg.getBytes();
            if (outputStream != null) {
                try {
                    outputStream.write(bytes);
//                    text_msg.post(new Runnable() {
//                        @SuppressLint("SetTextI18n")
//                        @Override
//                        public void run() {
//                            text_msg.setText(getResources().getString(R.string.send_msgs)+msg);
//                        }
//                    });
                    System.out.println("ConnectThread: " + getResources().getString(R.string.send_msgs)+msg);

                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("ConnectThread: " + getResources().getString(R.string.send_msg_error)+msg);

//                    text_msg.post(new Runnable() {
//                        @SuppressLint("SetTextI18n")
//                        @Override
//                        public void run() {
//                            text_msg.setText(getResources().getString(R.string.send_msg_error)+msg);
//                        }
//                    });
                }
            }
        }
    }


    private class ListenerThread extends Thread {

        private BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;

        @Override
        public void run() {
            try {
                serverSocket = bTAdatper.listenUsingRfcommWithServiceRecord(
                        NAME, BT_UUID);
                while (true) {
                    socket = serverSocket.accept();
//                    text_state.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            text_state.setText(getResources().getString(R.string.connecting));
//                        }
//                    });
                    System.out.println("Listener: " + getResources().getString(R.string.connecting));
                    connectThread = new ConnectThread(socket, false);
                    connectThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
