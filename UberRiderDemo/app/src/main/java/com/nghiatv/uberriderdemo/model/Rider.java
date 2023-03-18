package com.nghiatv.uberriderdemo.model;

public class Rider {
    private String name, phone, avatarUrl, rate, carType;

    public Rider() {
    }

    public Rider(String name, String phone, String avatarUrl, String rate, String carType) {
        this.name = name;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.rate = rate;
        this.carType = carType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public String getCarType() {
        return carType;
    }

    public void setCarType(String carType) {
        this.carType = carType;
    }
}
