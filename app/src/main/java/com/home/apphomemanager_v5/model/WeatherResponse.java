package com.home.apphomemanager_v5.model;

public class WeatherResponse {

    public Results results;

    public class Results {
        public String temp;
        public String description;
        public String city_name;
    }
}