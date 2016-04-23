package psalata.sailboat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Paweł on 03.04.2016.
 */
public class BluetoothHandler {

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    ConnectThread connectThread;
    ConnectedThread connectedThread;

    Activity mainActivity;

    public BluetoothHandler(Activity activity) {
        mainActivity = activity;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            Toast.makeText(mainActivity, "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
        } else {

            bluetoothAdapter.enable();
            while(!bluetoothAdapter.isEnabled()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if(pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    bluetoothDevice = device;
                }
            } else {
                Toast.makeText(mainActivity, "There are no paired devices. Pair device, and then comeback", Toast.LENGTH_LONG).show();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        connectThread = new ConnectThread(bluetoothDevice);
        connectThread.start();
    }

    public void write(byte[] byteArray) {
        connectedThread.write(byteArray);
    }

    public void reset() {
        connectedThread.resetProcessor();
    }


        private class ConnectThread extends Thread {
        private BluetoothSocket mSocket;
        private BluetoothDevice mDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        public ConnectThread(BluetoothDevice device) {

            BluetoothSocket tmp = null;
            mDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException ex) {}
            mSocket = tmp;
            Toast.makeText(mainActivity, "Connected device " + mDevice.getName(),
                    Toast.LENGTH_SHORT).show();
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                mSocket.connect();
            } catch (IOException connectException) {
                try {
                    mSocket.close();
                } catch (IOException closeException) {
                    return;
                }
            }
            connectedThread = new ConnectedThread(mSocket);
            connectedThread.start();
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException cancelException) {}
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException ex) {}

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes = 0;
            while(true) {
                try {
                    //if works - delete text=100% from xml
                    bytes = inputStream.read(buffer);
                    final Integer battery = (int) buffer[0];
                    Log.d("Bluetooth", "Received battery state: " + battery);

                    if(battery <= 100 && battery >= 0) {
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String percentBatteryState = battery.toString() +
                                        mainActivity.getResources().getString(R.string.percent);
                                TextView batteryStateTextView =
                                        (TextView) mainActivity.findViewById(R.id.batteryState);
                                batteryStateTextView.setText(percentBatteryState);
                            }
                        });
                    } else {
                        Toast.makeText(mainActivity, "Receiving battery state out of bounds(0-100)."
                                + "Current battery state: " + battery, Toast.LENGTH_LONG).show();
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView batteryStateTextView =
                                        (TextView) mainActivity.findViewById(R.id.batteryState);
                                batteryStateTextView.setText("-1%");
                            }
                        });
                    }
                } catch (IOException e) {}
            }
        }

        public void write(byte[] bytes) {
            try {
                Log.d("Bluetooth", "Bytes: " + bytes);
                outputStream.write(bytes);
            } catch (IOException ex) {}
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch(IOException ex) {}
        }

        private void resetProcessor() {
            Log.d("Bluetooth", "Reseting processor");
            byte[] reset = ByteBuffer.allocate(4).putInt(255).array();
            write(reset);
        }
    }
}
