package edu.dhbw.andar.sample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import edu.dhbw.andar.ARToolkit;
import edu.dhbw.andar.AndARActivity;
import edu.dhbw.andar.exceptions.AndARException;

/**
 * Example of an application that makes use of the AndAR toolkit.
 * @author Tobi
 *
 */
public class CustomActivity extends AndARActivity {

    private String m_userName;
    private String m_userId;

    /** BT begin
     */
    private enum ServerOrCilent{
        NONE,
        SERVER,
        CILENT
    }

    private ServerThread m_serverThread = null;
    private ClientThread m_clientThread = null;
    private ConnectedThread m_connectedThread = null;

    private BluetoothAdapter m_bluetoothAdapter = null;
    private BluetoothDevice m_device = null;

    private BluetoothServerSocket m_serverSocket = null;
    private BluetoothSocket m_socket = null;

    private UUID m_UUID = UUID.fromString("8bb345b0-712a-400a-8f47-6a4bda472638");

    private InputStream m_inStream;
    private OutputStream m_outStream;

    private ServerOrCilent m_mode = ServerOrCilent.CILENT;

    private static int REQUEST_ENABLE_BLUETOOTH = 1;

    private ArrayList m_messageList = new ArrayList();

    private boolean isConnected;

    /** BT end
     */

    /**
     * experiment begin
     */
    private Button m_startBtn;
    private Button m_continueBtn;
    /**
     * experiment end
     */

 	CustomObject someObject;
	ARToolkit artoolkit;
    DrawView m_drawView;

    long startTime = 0;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            //long millis = System.currentTimeMillis() - startTime;
            //int seconds = (int) (millis/1000);
            //int minutes = seconds / 60;
            //seconds = seconds % 60;

            //drawView.i = String.format("%d:%02d:%03d", minutes, seconds, millis);
            //drawView.count += 1;
            if (m_messageList.size() == 0) {
                m_drawView.sendLocation();
            }
            m_drawView.invalidate();
            sendMessage();
            timerHandler.postDelayed(this, 500);
        }
    };

    private static boolean m_isExit = false;

    Handler m_exitHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            m_isExit = false;
        }

    };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		CustomRenderer renderer = new CustomRenderer();//optional, may be set to null
		super.setNonARRenderer(renderer);//or might be omited
		try {
			//register a object for each marker type
			artoolkit = super.getArtoolkit();
			someObject = new CustomObject
				("test", "patt.hiro", 150.0, new double[]{0,0});
            someObject.setContext(this);
			artoolkit.registerARObject(someObject);
			/*someObject = new CustomObject
			("test", "android.patt", 80.0, new double[]{0,0});
			artoolkit.registerARObject(someObject);
			someObject = new CustomObject
			("test", "barcode.patt", 80.0, new double[]{0,0});
			artoolkit.registerARObject(someObject);*/
		} catch (AndARException ex){
			//handle the exception, that means: show the user what happened
			System.out.println("");
		}

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        m_userName = bundle.getString("user");
        m_userId = "P" + bundle.getString("id");

        setTitle(m_userId + " : " + m_userName);

        /**
         * BT begin
         */
        setupBluetooth();
        /**
         * BT end
         */
        m_startBtn = new Button(this);
        m_startBtn.setText("Start");

        m_startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_drawView != null && m_drawView.getBallCount() == 0) {
                    if (!m_drawView.isFinished()) {
                        m_drawView.startBlock();
                    }
                    else {
                        finish();
                        System.exit(0);
                    }
                }
            }
        });

        RelativeLayout relativeLayout = new RelativeLayout(this);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        relativeLayout.addView(m_startBtn, layoutParams);

        setStartButtonEnabled(false);

        m_drawView = new DrawView(this);
        m_drawView.setBackgroundColor(Color.TRANSPARENT);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        this.addContentView(m_drawView, new LinearLayout.LayoutParams(displayMetrics.widthPixels, (displayMetrics.heightPixels)));

        this.addContentView(relativeLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        /**
         * experiment begin
         */
        m_continueBtn = new Button(this);
        m_continueBtn.setText("Continue");
        m_continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_drawView != null && m_drawView.getBallCount() == 0) {
                    if (!m_drawView.isFinished()) {
                        m_drawView.nextBlock();
                    } else {
                        showDoneButton();
                    }
                }
            }
        });

        RelativeLayout relativeLayout_con = new RelativeLayout(this);

        RelativeLayout.LayoutParams layoutParams_con = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams_con.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        relativeLayout_con.addView(m_continueBtn, layoutParams_con);

        setContinueButtonEnabled(false);

        this.addContentView(relativeLayout_con, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        /**
         * experiment end
         */

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);

		startPreview();
	}

	/**
	 * Inform the user about exceptions that occurred in background threads.
	 * This exception is rather severe and can not be recovered from.
	 * TODO Inform the user and shut down the application.
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Log.e("AndAR EXCEPTION", ex.getMessage());
		finish();
	}

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        //stopThreads();
    }

	public void updateCoordinates(float x, float y, float z)
    {
        if(m_drawView != null)
            m_drawView.updateXYZ(x, y, z);
    }

    public void updateglmatirx(float[] matrix) {
        if(m_drawView != null)
            m_drawView.updateglMatrix(matrix);
    }

    /**
     * experiment begin
     */
    public void setStartButtonEnabled(boolean enabled) {
        m_startBtn.setEnabled(enabled);
    }

    public void setContinueButtonEnabled(boolean enabled) {
        m_continueBtn.setEnabled(enabled);
    }

    public void showDoneButton() {
        setContinueButtonEnabled(false);
        m_startBtn.setText("Done");
        m_startBtn.setEnabled(true);
    }

    /**
     * experiment end
     */

    /**
     * BT begin
     */
    public void showToast(String message)
    {
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        setupThread();
        super.onResume();
    }

    @Override
    protected void onRestart() {
        setupThread();
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        stopThreads();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void exit() {
        if (!m_isExit) {
            m_isExit = true;
            Toast.makeText(getApplicationContext(), "press back key again to exit", Toast.LENGTH_SHORT).show();
            m_exitHandler.sendEmptyMessageDelayed(0, 2000);
        } else {
            m_drawView.closeLogger();
            finish();
            System.exit(0);
        }
    }

    private void stopThreads() {
        if(m_bluetoothAdapter!=null&&m_bluetoothAdapter.isDiscovering()){
            m_bluetoothAdapter.cancelDiscovery();
        }

        if (m_serverThread != null) {
            m_serverThread.cancel();
            m_serverThread = null;
        }
        if (m_clientThread != null) {
            m_clientThread.cancel();
            m_clientThread = null;
        }
        if (m_connectedThread != null) {
            m_connectedThread.cancel();
            m_connectedThread = null;
        }
    }

    public void showMessageOnDrawView(String msg) {
        if(m_drawView != null)
            m_drawView.setMessage(msg);
    }

    private void setupBluetooth(){
        showMessageOnDrawView("Not Connected");
        isConnected = false;

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(m_bluetoothAdapter != null){  //Device support Bluetooth
            if(!m_bluetoothAdapter.isEnabled()){
                Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
            }
            else {
                setupThread();
            }
        }
        else{   //Device does not support Bluetooth

            Toast.makeText(this,"Bluetooth not supported on device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                setupThread();
            }
            else {
                showToast("Bluetooth is not enable on your device");
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setupThread(){
        if (m_mode == ServerOrCilent.SERVER) {
            if (m_serverThread == null) {
                m_serverThread = new ServerThread();
                m_serverThread.start();
            }
        } else if(m_mode == ServerOrCilent.CILENT) {
            findDevices();
            if (m_clientThread == null) {
                m_clientThread = new ClientThread();
                m_clientThread.start();
            }
        }
    }

    public void findDevices() {
        Set<BluetoothDevice> pairedDevices = m_bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                if (device.getName().contains("btserver"))
                {
                    m_device = device;
                    break;
                }
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    public String getUserName() {
        return m_userName;
    }

    public String getUserId() {
        return m_userId;
    }

    private class ConnectedThread extends Thread {
        public ConnectedThread() {
            try {
                m_inStream = m_socket.getInputStream();
                m_outStream = m_socket.getOutputStream();
                showMessageOnDrawView("Connected");
                isConnected = true;
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    // Read from the InputStream
                    if( m_inStream != null && (bytes = m_inStream.read(buffer)) > 0 )
                    {
                        byte[] buf_data = new byte[bytes];
                        for(int i=0; i<bytes; i++)
                        {
                            buf_data[i] = buffer[i];
                        }
                        String msg = new String(buf_data);
                        receiveBTMessage(msg);
                    }
                } catch (IOException e) {
                        cancel();
                        showMessageOnDrawView("Not Connected");
                        isConnected = false;
                        m_drawView.clearRemotePhoneInfo();
                    break;
                }
            }
        }

        public void write(String msg) {
            try {
                if (m_outStream != null) {
                    m_outStream.write(msg.getBytes());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                if (m_inStream != null) {
                    m_inStream.close();
                    m_inStream = null;
                }
                if (m_outStream != null) {
                    m_outStream.close();
                    m_outStream = null;
                }
                if (m_socket != null) {
                    m_socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServerThread extends Thread {
        public ServerThread () {
            try {
                m_serverSocket = m_bluetoothAdapter.listenUsingRfcommWithServiceRecord("Blue_chat", m_UUID);
            } catch (IOException e) {
                m_serverSocket = null;
            }
        }
        @Override
        public void run() {
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                if (m_serverSocket != null) {
                    try {
                        showMessageOnDrawView("Connecting...");
                        m_socket = m_serverSocket.accept();
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                else {
                    try {
                        m_serverSocket = m_bluetoothAdapter.listenUsingRfcommWithServiceRecord("Blue_chat", m_UUID);
                    } catch (IOException e) {
                        m_serverSocket = null;
                    }
                    continue;
                }

                if (m_socket != null) {

                    //Do work to manage the connection (in a separate thread)
                    m_connectedThread = new ConnectedThread();
                    m_connectedThread.start();

                    try {
                        m_serverSocket.close();
                    } catch (IOException e) {
                        break;
                    }
                    break;
                 }
            }
        }

        public void cancel() {
            try {
                if (m_inStream != null) {
                    m_inStream.close();
                    m_inStream = null;
                }
                if (m_outStream != null) {
                    m_outStream.close();
                    m_outStream = null;
                }
                if (m_serverSocket != null) {
                    m_serverSocket.close();
                }
            } catch (IOException e) {
            }
        }
    };

    private class ClientThread extends Thread {
        public ClientThread() {
            initSocket();
        }

        private void initSocket() {
            try {
                if (m_device != null) {
                    m_socket = m_device.createInsecureRfcommSocketToServiceRecord(m_UUID);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                if (m_socket != null) {
                    m_bluetoothAdapter.cancelDiscovery();

                    try {
                        showMessageOnDrawView("Connecting...");
                        m_socket.connect();
                    } catch (IOException e) {
                        try {
                            m_socket.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                        initSocket();
                        continue;
                    }

                    //Do work to manage the connection (in a separate thread)
                    m_connectedThread = new ConnectedThread();
                    m_connectedThread.start();
                    break;
                } else {
                    initSocket();
                }
            }
        }

        public void cancel() {
            try {
                if (m_inStream != null) {
                    m_inStream.close();
                    m_inStream = null;
                }
                if (m_outStream != null) {
                    m_outStream.close();
                    m_outStream = null;
                }

                if (m_socket != null) {
                    m_socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendBTMessage(String msg) {
        if (m_connectedThread != null) {
            m_connectedThread.write(msg);
        }
    }

    public void addMessage(String msg) {
        m_messageList.add(msg);
    }

    public void sendMessage(){
        if (m_messageList.size() != 0) {
            String msg = (String)m_messageList.get(0);
            m_messageList.remove(0);
            sendBTMessage(msg);
        }
    }

    private void receiveBTMessage(String msg){
        try {
            JSONArray jsonArray = new JSONArray(msg);

            int len = jsonArray.length();

            ArrayList<String> names = new ArrayList<>();

            for (int i=0; i<len; ++i) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                final String senderName = jsonObject.getString("name");
                int senderColor = jsonObject.getInt("color");
                float senderAngle = (float) jsonObject.getDouble("z");

                if (m_drawView != null) {
                    m_drawView.updateRemotePhone(senderName, senderColor, senderAngle);
                }

                boolean isSendingBall = jsonObject.getBoolean("isSendingBall");
                if (isSendingBall && m_drawView != null) {
                    String receiverName = jsonObject.getString("receiverName");
                    if (receiverName.equalsIgnoreCase(m_userName)) {
                        String ballId = jsonObject.getString("ballId");
                        int ballColor = jsonObject.getInt("ballColor");
                        m_drawView.receivedBall(ballId, ballColor);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast("received ball from : " + senderName);
                            }
                        });
                    }
                }

                names.add(senderName);
            }

            ArrayList<DrawView.RemotePhoneInfo> remotePhoneInfos = m_drawView.getRemotePhones();
            ArrayList<DrawView.RemotePhoneInfo> lostPhoneInfos = new ArrayList<>();
            for (DrawView.RemotePhoneInfo phoneInfo : remotePhoneInfos) {
                if (!names.contains(phoneInfo.m_name)) {
                    lostPhoneInfos.add(phoneInfo);
                }
            }

            if (!lostPhoneInfos.isEmpty()) {
                m_drawView.removePhones(lostPhoneInfos);
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected(){
        return isConnected;
    }

    /**
     * BT end
     */
}
