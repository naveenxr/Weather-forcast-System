package com.weather.app.controller;

import com.weather.app.service.WeatherService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class WeatherController {

    private final WeatherService weatherService;

    @Autowired
    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather")
    public ResponseEntity<?> getWeather(@RequestParam(name = "city", defaultValue = "Tokyo") String city) {
        JSONObject data = weatherService.getWeatherData(city);

        if (data == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Location not found or weather service unavailable for city: " + city);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        return ResponseEntity.ok(data);
    }

    @GetMapping("/weather/compare")
    public ResponseEntity<?> compareWeather(@RequestParam(name = "city1") String city1,
                                            @RequestParam(name = "city2") String city2) {
        JSONObject data1 = weatherService.getWeatherData(city1);
        JSONObject data2 = weatherService.getWeatherData(city2);

        Map<String, Object> response = new HashMap<>();
        response.put("city1", data1);
        response.put("city2", data2);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "Spring Boot Weather Application API");
        return ResponseEntity.ok(status);
    }
}
