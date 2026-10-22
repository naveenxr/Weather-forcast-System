package com.weather.app.service;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

@Service
public class WeatherService {

    public JSONObject getWeatherData(String locationName) {
        JSONArray locationData = getLocationData(locationName);

        if (locationData == null || locationData.isEmpty()) {
            return null;
        }

        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");
        String cityName = (String) location.get("name");
        String country = (String) location.get("country");

        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,relativehumidity_2m,weather_code,windspeed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum" +
                "&timezone=auto";

        try {
            HttpURLConnection conn = fetchApiResponse(urlString);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while (scanner.hasNext()) {
                resultJson.append(scanner.nextLine());
            }
            scanner.close();
            conn.disconnect();

            JSONParser parser = new JSONParser();
            JSONObject resultObject = (JSONObject) parser.parse(resultJson.toString());

            JSONObject hourly = (JSONObject) resultObject.get("hourly");

            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            JSONArray weathercode = (JSONArray) hourly.get("weather_code");
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            JSONArray relativehumidity = (JSONArray) hourly.get("relativehumidity_2m");
            long humidity = (long) relativehumidity.get(index);

            JSONArray windspeedData = (JSONArray) hourly.get("windspeed_10m");
            double windspeed = (double) windspeedData.get(index);

            JSONObject weatherData = new JSONObject();
            weatherData.put("city", cityName);
            weatherData.put("country", country != null ? country : "");
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);
            weatherData.put("latitude", latitude);
            weatherData.put("longitude", longitude);
            weatherData.put("raw_data", resultObject);

            return weatherData;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public JSONArray getLocationData(String locationName) {
        locationName = locationName.replaceAll("\\s+", "+");

        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                URLEncoder.encode(locationName, StandardCharsets.UTF_8) + "&count=10&language=en&format=json";

        try {
            HttpURLConnection conn = fetchApiResponse(urlString);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while (scanner.hasNext()) {
                resultJson.append(scanner.nextLine());
            }
            scanner.close();
            conn.disconnect();

            JSONParser parser = new JSONParser();
            JSONObject resultsJsonObj = (JSONObject) parser.parse(resultJson.toString());

            return (JSONArray) resultsJsonObj.get("results");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private HttpURLConnection fetchApiResponse(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        return conn;
    }

    private int findIndexOfCurrentTime(JSONArray timeList) {
        String currentTime = getCurrentTime();

        for (int i = 0; i < timeList.size(); i++) {
            String time = (String) timeList.get(i);
            if (time.equalsIgnoreCase(currentTime)) {
                return i;
            }
        }

        return 0;
    }

    private String getCurrentTime() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");
        return currentDateTime.format(formatter);
    }

    private String convertWeatherCode(long weathercode) {
        if (weathercode == 0L) {
            return "Clear";
        } else if (weathercode <= 3L && weathercode > 0L) {
            return "Cloudy";
        } else if ((weathercode >= 51L && weathercode <= 67L) || (weathercode >= 80L && weathercode <= 99L)) {
            return "Rain";
        } else if (weathercode >= 71L && weathercode <= 77L) {
            return "Snow";
        }
        return "Clear";
    }
}
