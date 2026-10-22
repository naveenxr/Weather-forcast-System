import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

// retrieve weather data from API - this backend logic will fetch the latest weather
// data from the external API and return it. The GUI will
// display this data to the user
public class WeatherApp {
    // fetch weather data for given location
    public static JSONObject getWeatherData(String locationName){
        // get location coordinates using the geolocation API
        JSONArray locationData = getLocationData(locationName);

        // check if locationData is null or empty
        if(locationData == null || locationData.isEmpty()){
            System.out.println("Error: Could not find location.");
            return null;
        }

        // extract latitude and longitude data
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        // build API request URL with location coordinates
        // Requesting hourly + daily data with auto-timezone
        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,relativehumidity_2m,weather_code,windspeed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum" +
                "&timezone=auto";

        try{
            // call api and get response
            HttpURLConnection conn = fetchApiResponse(urlString);

            // check for response status
            if(conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            }

            // store resulting json data
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while(scanner.hasNext()){
                resultJson.append(scanner.nextLine());
            }

            // close scanner
            scanner.close();

            // close url connection
            conn.disconnect();

            // parse through our data
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            // get UTC offset seconds to calculate the correct timezone local time
            long utcOffsetSeconds = 0L;
            if(resultJsonObj.get("utc_offset_seconds") != null){
                utcOffsetSeconds = ((Number) resultJsonObj.get("utc_offset_seconds")).longValue();
            }

            // retrieve hourly data
            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");

            // find current hour index in target timezone
            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time, utcOffsetSeconds);

            // get current weather stats
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = ((Number) temperatureData.get(index)).doubleValue();

            JSONArray weathercode = (JSONArray) hourly.get("weather_code");
            long currentWeatherCode = ((Number) weathercode.get(index)).longValue();
            String weatherCondition = convertWeatherCode(currentWeatherCode);

            JSONArray relativeHumidity = (JSONArray) hourly.get("relativehumidity_2m");
            long humidity = ((Number) relativeHumidity.get(index)).longValue();

            JSONArray windspeedData = (JSONArray) hourly.get("windspeed_10m");
            double windspeed = ((Number) windspeedData.get(index)).doubleValue();

            // ===== HOURLY FORECAST: next 6 hours =====
            JSONArray hourlyForecast = new JSONArray();
            for(int i = 1; i <= 6; i++){
                int hIndex = index + i;
                if(hIndex >= time.size()) break;

                JSONObject hourData = new JSONObject();
                String timeStr = (String) time.get(hIndex);
                // Extract just the hour part (e.g. "14:00" from "2026-07-13T14:00")
                String hourLabel = timeStr.substring(timeStr.indexOf("T") + 1);
                hourData.put("time", hourLabel);
                hourData.put("temperature", ((Number) temperatureData.get(hIndex)).doubleValue());
                hourData.put("weather_condition", convertWeatherCode(((Number) weathercode.get(hIndex)).longValue()));
                hourlyForecast.add(hourData);
            }

            // ===== TEMPERATURE TREND: next 12 hours for mini-chart =====
            JSONArray tempTrend = new JSONArray();
            for(int i = 0; i <= 12; i++){
                int tIndex = index + i;
                if(tIndex >= temperatureData.size()) break;
                tempTrend.add(((Number) temperatureData.get(tIndex)).doubleValue());
            }

            // ===== DAILY FORECAST: next 4 days with humidity =====
            JSONObject daily = (JSONObject) resultJsonObj.get("daily");
            JSONArray dailyTimeList = (JSONArray) daily.get("time");
            JSONArray dailyWeatherCodeList = (JSONArray) daily.get("weather_code");
            JSONArray dailyTempMaxList = (JSONArray) daily.get("temperature_2m_max");
            JSONArray dailyTempMinList = (JSONArray) daily.get("temperature_2m_min");
            JSONArray dailyPrecipList = (JSONArray) daily.get("precipitation_sum");

            JSONArray forecastArray = new JSONArray();
            // index 0 = today, 1 = tomorrow, etc. We include today + next 3 = 4 total
            int maxDays = Math.min(4, dailyTimeList.size());
            for(int i = 0; i < maxDays; i++) {
                JSONObject forecastDay = new JSONObject();
                String dateStr = (String) dailyTimeList.get(i);
                long code = ((Number) dailyWeatherCodeList.get(i)).longValue();
                double tempMax = ((Number) dailyTempMaxList.get(i)).doubleValue();
                double tempMin = ((Number) dailyTempMinList.get(i)).doubleValue();
                double precip = ((Number) dailyPrecipList.get(i)).doubleValue();

                // Calculate average humidity for this day from hourly data
                // Each day has 24 hours of data
                long dayHumidity = 0;
                int dayStart = i * 24;
                int count = 0;
                for(int h = dayStart; h < dayStart + 24 && h < relativeHumidity.size(); h++){
                    dayHumidity += ((Number) relativeHumidity.get(h)).longValue();
                    count++;
                }
                if(count > 0) dayHumidity = dayHumidity / count;

                forecastDay.put("date", dateStr);
                forecastDay.put("weather_condition", convertWeatherCode(code));
                forecastDay.put("temp_max", tempMax);
                forecastDay.put("temp_min", tempMin);
                forecastDay.put("humidity", dayHumidity);
                forecastDay.put("precipitation", precip);

                forecastArray.add(forecastDay);
            }

            // ===== LOCAL DATE/TIME =====
            String localDateTime = getFormattedLocalDateTime(utcOffsetSeconds);

            // ===== WEATHER ALERTS =====
            JSONArray alerts = generateAlerts(currentWeatherCode, temperature, windspeed, dailyWeatherCodeList, dailyPrecipList);

            // build the weather json data object
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);
            weatherData.put("forecast", forecastArray);
            weatherData.put("hourly_forecast", hourlyForecast);
            weatherData.put("temp_trend", tempTrend);
            weatherData.put("local_datetime", localDateTime);
            weatherData.put("alerts", alerts);

            return weatherData;
        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    // Generate weather alerts based on conditions
    private static JSONArray generateAlerts(long weatherCode, double temp, double windspeed,
                                            JSONArray dailyWeatherCodes, JSONArray dailyPrecip) {
        JSONArray alerts = new JSONArray();

        // Current condition alerts
        if((weatherCode >= 51L && weatherCode <= 67L) || (weatherCode >= 80L && weatherCode <= 99L)){
            if(weatherCode >= 95L){
                alerts.add("\u26A0 Severe Storm Alert: Thunderstorm activity detected");
            } else if(weatherCode >= 80L){
                alerts.add("\uD83C\uDF27 Rain Alert: Heavy precipitation expected");
            } else {
                alerts.add("\uD83C\uDF26 Rain Advisory: Light to moderate rain");
            }
        }

        if(weatherCode >= 71L && weatherCode <= 77L){
            alerts.add("\u2744 Snow Alert: Snowfall expected in your area");
        }

        if(temp > 40.0){
            alerts.add("\uD83C\uDF21 Heat Warning: Extreme temperatures above 40\u00B0C");
        } else if(temp > 35.0){
            alerts.add("\u2600 Heat Advisory: High temperatures above 35\u00B0C");
        }

        if(temp < -10.0){
            alerts.add("\u2744 Freeze Warning: Dangerously cold temperatures");
        } else if(temp < 0.0){
            alerts.add("\uD83E\uDDCA Frost Advisory: Below freezing temperatures");
        }

        if(windspeed > 60.0){
            alerts.add("\uD83C\uDF2A Wind Warning: Severe winds above 60 km/h");
        } else if(windspeed > 40.0){
            alerts.add("\uD83D\uDCA8 Wind Advisory: Strong winds above 40 km/h");
        }

        // Check upcoming daily precipitation
        for(int i = 1; i < Math.min(3, dailyPrecip.size()); i++){
            double precip = ((Number) dailyPrecip.get(i)).doubleValue();
            if(precip > 20.0){
                alerts.add("\u2614 Heavy Rain Forecast: Significant precipitation expected in coming days");
                break;
            }
        }

        return alerts;
    }

    // Format the local date and time for display
    private static String getFormattedLocalDateTime(long utcOffsetSeconds){
        java.time.Instant instant = java.time.Instant.now().plusSeconds(utcOffsetSeconds);
        java.time.ZonedDateTime targetTime = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a, EEE, MMM d, yyyy");
        return targetTime.format(formatter);
    }

    // retrieves geographic coordinates for given location name
    public static JSONArray getLocationData(String locationName){
        // replace any whitespace in location name to + to adhere to API's request format
        locationName = locationName.replaceAll(" ", "+");

        // build API url with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try{
            // call api and get a response
            HttpURLConnection conn = fetchApiResponse(urlString);

            // check response status
            if(conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            }else{
                // store the API results
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(conn.getInputStream());

                while(scanner.hasNext()){
                    resultJson.append(scanner.nextLine());
                }

                // close scanner
                scanner.close();

                // close url connection
                conn.disconnect();

                // parse the JSON string into a JSON obj
                JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                // get the list of location data the API generated from the location name
                JSONArray locationData = (JSONArray) resultsJsonObj.get("results");
                return locationData;
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        // couldn't find location
        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString){
        try{
            // attempt to create connection
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // set request method to get
            conn.setRequestMethod("GET");

            // connect to our API
            conn.connect();
            return conn;
        }catch(IOException e){
            e.printStackTrace();
        }

        // could not make connection
        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList, long utcOffsetSeconds){
        String currentTime = getCurrentTime(utcOffsetSeconds);

        // iterate through the time list and see which one matches our current time
        for(int i = 0; i < timeList.size(); i++){
            String time = (String) timeList.get(i);
            if(time.equalsIgnoreCase(currentTime)){
                return i;
            }
        }

        return 0;
    }

    private static String getCurrentTime(long utcOffsetSeconds){
        // get current date and time in the target city's timezone offset
        java.time.Instant instant = java.time.Instant.now().plusSeconds(utcOffsetSeconds);
        java.time.ZonedDateTime targetTime = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);

        // format date to be 2023-09-02T00:00 (this is how it is read in the API)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        String formattedDateTime = targetTime.format(formatter);

        return formattedDateTime;
    }

    // convert the weather code to something more readable
    public static String convertWeatherCode(long weathercode){
        String weatherCondition = "";
        if(weathercode == 0L){
            weatherCondition = "Clear";
        }else if(weathercode > 0L && weathercode <= 3L){
            weatherCondition = "Cloudy";
        }else if((weathercode >= 51L && weathercode <= 67L)
                    || (weathercode >= 80L && weathercode <= 99L)){
            weatherCondition = "Rain";
        }else if(weathercode >= 71L && weathercode <= 77L){
            weatherCondition = "Snow";
        }

        return weatherCondition;
    }
}
