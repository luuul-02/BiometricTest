package com.example.biometrictest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;




public class MainActivity extends AppCompatActivity {

    // 벨 알림 정보 클래스
    public class Info{
        public String date;
        public int temperature;

        public String getDate(){ // 날짜 불러오는 로직
            // 현재시간을 msec 으로 구한다.
            long now = System.currentTimeMillis();
            // 현재시간을 date 변수에 저장한다.
            Date dates = new Date(now);
            // 시간을 나타냇 포맷을 정한다 ( yyyy/MM/dd 같은 형태로 변형 가능 )
            SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            // nowDate 변수에 값을 저장한다.
            String formatDate = sdfNow.format(dates);

            return formatDate;
        }

        public Info(int temp){
            this.date=getDate();
            this.temperature=temp;
        }
    }




    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    Button btn_door,btn_bluetooth,btn_temperature,btn_person,btn_bell;
    TextView txt_door,txt_temperature,txt_person;

    // ---------------------------------   블루투스용 변수
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    List<String> mListPairedDevices;

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    boolean sendMarker = true; // 데이터 전송 마커
    int tempTen = 0;  // 섭씨 십의자리 일의자리 소수점
    int tempOne = 0;
    int tempFloat = 0;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // --------------------------------------------------


    // -------------------------------- 파이어베이스용 변수
    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference(); // DB 테이블 불러오기
    DatabaseReference temperatureRef = mRootRef.child("벨 신호"); // 컬럼(속성)명 선정
    // --------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_bluetooth = (Button) findViewById(R.id.btn_bluetooth);
        btn_door = (Button) findViewById(R.id.btn_door);
        btn_temperature = (Button) findViewById(R.id.btn_temperature);
        btn_person = (Button) findViewById(R.id.btn_person);
        btn_bell = (Button) findViewById(R.id.btn_bell);
        txt_door = (TextView) findViewById(R.id.txt_door);
        txt_temperature = (TextView) findViewById(R.id.txt_temperature);
        txt_person = (TextView) findViewById(R.id.txt_person);

        //지문 인증 구현
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this,
                executor, new BiometricPrompt.AuthenticationCallback() {

            //지문 인증 에러
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), R.string.auth_error_message, Toast.LENGTH_SHORT).show();
                btn_door.setBackgroundResource(R.drawable.door_close1);
            }
            //지문 인증 성공
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(), R.string.auth_success_message, Toast.LENGTH_SHORT).show();
                mThreadConnectedBluetooth.write("1".toString());

            }
            //지문 인증 실패
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), R.string.auth_fail_message, Toast.LENGTH_SHORT).show();
            }
        });
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("지문 인증")
                .setSubtitle("기기에 등록된 지문을 이용하여 지문을 인증해주세요.")
                .setNegativeButtonText("취소")
                .setDeviceCredentialAllowed(false)
                .build();
        //  사용자가 다른 인증을 이용하길 원할 때 추가하기
        Button biometricLoginButton = findViewById(R.id.btn_door);
        biometricLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                biometricPrompt.authenticate(promptInfo);
            }
        });

        // 블루투스 버튼 리스너
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // 블루투스 어댑터 초기화
        btn_bluetooth.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                listPairedDevices();
            }
        });


        btn_temperature.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                float tempResult = ((tempTen * 10) + tempOne) + ((float)tempFloat*0.1f);
                Toast.makeText(getApplicationContext(), "현재 온도는 섭씨 " + tempResult + "도 입니다.", Toast.LENGTH_SHORT).show();
            }});

        // 블루투스 통신 데이터 관리 함수
        // 전송 데이터 구조(byte) : 벨알림(1), 조도(1), 문열림(1), 온도(1), 섭씨(2)
        mBluetoothHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == BT_MESSAGE_READ) {
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8"); // 수신된 데이터를 string으로 변환

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    btn_bell.setBackgroundResource((readMessage.charAt(0) - '0' > 0) ? R.drawable.bell_pressed : R.drawable.bell_default); // 벨
                    btn_person.setBackgroundResource((readMessage.charAt(1) - '0' > 0) ? R.drawable.door_person_yes : R.drawable.door_person_default); // 조도
                    btn_door.setBackgroundResource((readMessage.charAt(2) - '0' > 0) ? ((readMessage.charAt(2) - '0' == 2) ? R.drawable.door_close1 : R.drawable.door_open1) : R.drawable.door_default); // 문
                    btn_temperature.setBackgroundResource((readMessage.charAt(3) - '0' > 0) ? R.drawable.temperature_hot : R.drawable.temperature_default); // 온도

                    tempTen = readMessage.charAt(4) - '0';
                    tempOne = readMessage.charAt(5) - '0';
                    tempFloat = readMessage.charAt(6) - '0';

                    // 벨 소리가 울리면
                    if (readMessage.charAt(0) - '0' > 0 && sendMarker) {
                        Info infoData = new Info(((readMessage.charAt(4) - '0') * 10) + (readMessage.charAt(5) - '0')); // 온도 셋팅
                        temperatureRef.push().setValue(infoData); // 온도 정보 전송
                        sendMarker = false; // 정보는 1회만 전송
                    }

                    // 벨 소리가 안울리면
                    if (!(readMessage.charAt(0) - '0' > 0)) {
                        sendMarker = true; // marker 초기화
                    }
                }
            }
        };

    }



    // 블루투스 함수
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BT_REQUEST_ENABLE:
                if (resultCode == RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면
                    Toast.makeText(getApplicationContext(), "블루투스 활성화", Toast.LENGTH_LONG).show();
                } else if (resultCode == RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면
                    Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_LONG).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    void listPairedDevices() {
        if (mBluetoothAdapter.isEnabled()) {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();

            if (mPairedDevices.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("장치 선택");

                mListPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                    //mListPairedDevices.add(device.getName() + "\n" + device.getAddress());
                }
                final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        connectSelectedDevice(items[item].toString());
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    void connectSelectedDevice(String selectedDeviceName) {
        for(BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;
                break;
            }
        }
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mBluetoothSocket.connect();
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
            mThreadConnectedBluetooth.start();
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
            Toast.makeText(getApplicationContext(), "블루투스가 연결되었습니다.", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }
    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.available(); // 버퍼에 수신된 데이터가 있는지 확인
                    if (bytes != 0) { // 0을 반환할 때까지 데이터를 읽어들임
                        buffer = new byte[1024]; // 버퍼를 매번 초기화 시켜 줌
                        SystemClock.sleep(200);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes); // 입력 스트림으로부터 매개값으로 주어진 바이트 배열의 길이[1024]만큼 바이트를 읽고 buffer에 저장. 배열의 길이보다 실제 읽은 바이트 수가 적으면 읽을 수 있을 만큼만 읽음
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget(); // 위에 정의된 mBluetoothHandler = new Handler() ~ 이벤트 핸들러를 호출 시킴 (buffer 내용을 인자로)
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(String str) {
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }
}