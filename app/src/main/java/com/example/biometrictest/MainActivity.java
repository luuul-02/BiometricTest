package com.example.biometrictest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.Executor;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends AppCompatActivity {

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    Button btn_Fingerprint,btn_bluetooth;
    TextView txt_certify,txt_bluetooth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_Fingerprint = (Button) findViewById(R.id.btn_Fingerprint);
        btn_bluetooth = (Button) findViewById(R.id.btn_bluetooth);
        txt_certify = (TextView) findViewById(R.id.txt_certify);
        txt_bluetooth = (TextView) findViewById(R.id.txt_bluetooth);
        

        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this,
                executor, new BiometricPrompt.AuthenticationCallback() {

            //지문 인증 에러
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), R.string.auth_error_message, Toast.LENGTH_SHORT).show();
                btn_Fingerprint.setBackgroundResource(R.drawable.fingerprint_fail1);
            }
            //지문 인증 성공
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(), R.string.auth_success_message, Toast.LENGTH_SHORT).show();
                btn_Fingerprint.setBackgroundResource(R.drawable.fingerprint_pass1);
                txt_certify.setText("인증 성공");
            }
            //지문 인증 실패
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), R.string.auth_fail_message, Toast.LENGTH_SHORT).show();
                btn_Fingerprint.setBackgroundResource(R.drawable.fingerprint_fail1);
                txt_certify.setText("인증 실패");
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("지문 인증")
                .setSubtitle("기기에 등록된 지문을 이용하여 지문을 인증해주세요.")
                .setNegativeButtonText("취소")
                .setDeviceCredentialAllowed(false)
                .build();

        //  사용자가 다른 인증을 이용하길 원할 때 추가하기
        Button biometricLoginButton = findViewById(R.id.btn_Fingerprint);
        biometricLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                biometricPrompt.authenticate(promptInfo);
            }
        });
    }
}