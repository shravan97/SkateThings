package com.nitt.skate.skate_things;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by shravan on 4/2/18.
 */

public class ServerThread extends Thread {
    private final BluetoothServerSocket socket;
    private final BluetoothAdapter adapter;
    private BluetoothSocket bluetoothSocket;
    private ReadWriteThread readWriteThread;
    private Gpio mGpio;
    private static String GPIO_NAME = "GPIO2_IO05";
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                byte[] readBuf = (byte[]) msg.obj;
                String txt = new String(readBuf, 0, msg.arg1);
                Log.e("Server-Thread-Handler", txt);

                if (txt.equals("Start")) {
                    try {
                        // High voltage is considered active
                        mGpio.setActiveType(Gpio.ACTIVE_HIGH);
                        mGpio.setValue(true);

                    } catch (IOException e) {
                        Log.w("Message-Handler", "Unable to access GPIO", e);
                    }
//                    PeripheralManager manager = PeripheralManager.getInstance();
//                    List<String> portList = manager.getGpioList();
//                    if (portList.isEmpty()) {
//                        Log.i("ReadWrite-Start", "No GPIO port available on this device.");
//                    } else {
//                        Log.i("ReadWrite-Start", "List of available ports: " + portList);
//                    }
                } else if (txt.equals("Stop")) {
                    try {
                        stopPin();
                    } catch (IOException e) {
                        Log.e("Message-Handler", "Stop error", e);
                    }
                }
            } else
                Log.e("Server-Thread-Handler", "Lol !");
            super.handleMessage(msg);
        }
    };

    public ServerThread() {
        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.
        try {
            PeripheralManager manager = PeripheralManager.getInstance();
            mGpio = manager.openGpio(GPIO_NAME);
            // Initialize the pin as an input
            mGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            // High voltage is considered active
            mGpio.setActiveType(Gpio.ACTIVE_HIGH);
        } catch (IOException e) {
            Log.e("Server-Thread-GPio", "Unable to set GPio", e);
        }

        adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.setName("My Android Things device");
        BluetoothServerSocket tmpSocket = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code.
            String uuid = "SkateProj";
            tmpSocket = adapter.listenUsingRfcommWithServiceRecord("Skate", UUID.nameUUIDFromBytes(uuid.getBytes()));
        } catch (IOException e) {
            Log.e("Server-Thread-Init", "Socket's listen() method failed", e);
        }
        socket = tmpSocket;
        readWriteThread = null;
    }

    public BluetoothSocket getSocket() {
        return bluetoothSocket;
    }

    public void stopPin() throws IOException {
        mGpio.setActiveType(Gpio.ACTIVE_LOW);
        mGpio.setValue(true);
    }

    public void run() {

        Pwm mPwm;
// Important : Don't delete the below. It's reqd. fr pwm
//        try {
//            PeripheralManager manager = PeripheralManager.getInstance();
//            mPwm = manager.openPwm("PWM1");
//            mPwm.setPwmFrequencyHz(120);
//            mPwm.setEnabled(true);
//            Log.w("Server-Thread-Run", "accessed PWM !");
//        } catch (IOException e) {
//            Log.w("Server-Thread-Run", "Unable to access PWM", e);
//        }

        BluetoothSocket tmpSocket = null;
        // Keep listening until exception occurs or a socket is returned.
        while (true) {
            try {
                tmpSocket = socket.accept();
            } catch (IOException e) {
                Log.e("Server-Thread-Run", "Socket's accept() method failed", e);
                break;
            }

            if (tmpSocket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                Log.e("Server-Thread-Run", "Socket Returned !");
                bluetoothSocket = tmpSocket;
                readWriteThread = new ReadWriteThread(bluetoothSocket, mHandler);
                readWriteThread.start();
//                try {
//                    socket.close();
//                }
//                catch (IOException e) {
//                    Log.e("Server-Thread-Run", "Check exception", e);
//                }
//
//                break;
            }
        }
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e("Server-Thread-Close", "Could not close the connect socket", e);
        }
    }

}
