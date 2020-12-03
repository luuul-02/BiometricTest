package com.example.biometrictest;

// DB의 벨 기록 읽기 클래스
public class EnteranceData {

    String date;
    int temperature;

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

}
