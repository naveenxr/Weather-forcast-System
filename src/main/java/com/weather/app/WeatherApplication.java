package com.weather.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WeatherApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherApplication.class, args);
        System.out.println("\n========================================================");
        System.out.println(" 🌤️ Weather App Spring Boot Server Started Successfully!");
        System.out.println(" 🌐 Web Frontend: http://localhost:8080");
        System.out.println(" 🔌 REST API:     http://localhost:8080/api/weather?city=Tokyo");
        System.out.println("========================================================\n");
    }
}
