import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class WeatherAppGui extends JFrame {
    private JSONObject weatherData;

    // Unit Conversion state
    private boolean isCelsius = true;
    private JButton unitToggleButton;

    // Navigation and Page State
    private String currentCard = "Dashboard";
    private TransitionPanel transitionPanel;
    private JPanel dashboardPanel;
    private JPanel comparePanel;
    private GlassPanel navBar;

    // Dashboard View Components
    private JTextField searchTextField;
    private JLabel dateTimeLabel;
    private JLabel weatherConditionImage;
    private JLabel temperatureText;
    private JLabel weatherConditionDesc;
    private JLabel humidityText;
    private JLabel windspeedText;
    private LoadingOverlay dashboardLoadingOverlay;

    // Hourly forecast components (6 hours)
    private JLabel[] hourlyTimeLabels = new JLabel[6];
    private JLabel[] hourlyIconLabels = new JLabel[6];
    private JLabel[] hourlyTempLabels = new JLabel[6];

    // Daily forecast components (4 days)
    private JLabel[] forecastDayLabels = new JLabel[4];
    private JLabel[] forecastIconLabels = new JLabel[4];
    private JLabel[] forecastTempLabels = new JLabel[4];
    private JLabel[] forecastHumLabels = new JLabel[4];

    // Weather alerts
    private JLabel alertsLabel;
    private GlassPanel alertsCard;

    // Temperature trend chart
    private TempTrendChart tempTrendChart;

    // Compare View Components
    private JTextField compCity1Field;
    private JTextField compCity2Field;
    private String compareCity1Name;
    private String compareCity2Name;
    private JSONObject compareData1;
    private JSONObject compareData2;
    private LoadingOverlay compareLoadingOverlay;

    // Compare Left City Card Components
    private JLabel compCity1NameLabel;
    private JLabel compCity1IconLabel;
    private JLabel compCity1TempLabel;
    private JLabel compCity1DescLabel;
    private JLabel compCity1HumidityLabel;
    private JLabel compCity1WindLabel;
    private JLabel compCity1MinMaxLabel;

    // Compare Right City Card Components
    private JLabel compCity2NameLabel;
    private JLabel compCity2IconLabel;
    private JLabel compCity2TempLabel;
    private JLabel compCity2DescLabel;
    private JLabel compCity2HumidityLabel;
    private JLabel compCity2WindLabel;
    private JLabel compCity2MinMaxLabel;

    public WeatherAppGui(){
        super("Modern Weather App");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(480, 870);
        setLocationRelativeTo(null);
        setResizable(false);

        // Deep blue-purple gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(20, 24, 46), 0, getHeight(), new Color(49, 44, 84));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        mainPanel.setLayout(null);
        setContentPane(mainPanel);

        addGuiComponents();
        searchWeather("New York");
        compareWeather("London", "Paris");
    }

    private void addGuiComponents(){
        // ===== NAVIGATION BAR =====
        navBar = new GlassPanel(10);
        navBar.setBounds(20, 12, 350, 42);

        JButton btnDashboard = createNavButton("Dashboard", 0, 174);
        JButton btnCompare = createNavButton("Compare", 175, 175);
        navBar.add(btnDashboard);
        navBar.add(btnCompare);
        add(navBar);

        // Unit toggle button
        unitToggleButton = createGlassButton("\u00B0C", 385, 12, 55, 42, 14);
        add(unitToggleButton);

        // ===== TRANSITION PANEL FOR PAGES =====
        transitionPanel = new TransitionPanel();
        transitionPanel.setBounds(0, 65, 480, 770);
        add(transitionPanel);

        buildDashboardPanel();
        buildComparePanel();

        transitionPanel.showPanel(dashboardPanel, true);

        // Tab button actions
        btnDashboard.addActionListener(e -> {
            if (!currentCard.equals("Dashboard")) {
                currentCard = "Dashboard";
                transitionPanel.showPanel(dashboardPanel, false);
                navBar.repaint();
            }
        });
        btnCompare.addActionListener(e -> {
            if (!currentCard.equals("Compare")) {
                currentCard = "Compare";
                transitionPanel.showPanel(comparePanel, true);
                navBar.repaint();
            }
        });
        unitToggleButton.addActionListener(e -> {
            isCelsius = !isCelsius;
            unitToggleButton.setText(isCelsius ? "\u00B0C" : "\u00B0F");
            updateUIWithWeatherData();
            updateCompareLabels();
        });
    }

    // ================ DASHBOARD PANEL ================
    private void buildDashboardPanel() {
        dashboardPanel = new JPanel();
        dashboardPanel.setLayout(null);
        dashboardPanel.setOpaque(false);

        int W = 480;

        // Search field
        searchTextField = createGlassTextField(15, 5, 345, 42);
        dashboardPanel.add(searchTextField);

        // Search button
        JButton searchButton = createGlassIconButton("src/assets/search.png", 375, 5, 48, 42);
        searchButton.addActionListener(e -> searchWeather(searchTextField.getText()));
        dashboardPanel.add(searchButton);

        // Date/Time label
        dateTimeLabel = new JLabel("--");
        dateTimeLabel.setBounds(15, 52, 430, 18);
        dateTimeLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        dateTimeLabel.setForeground(new Color(180, 180, 220));
        dashboardPanel.add(dateTimeLabel);

        // Weather icon
        weatherConditionImage = new JLabel();
        weatherConditionImage.setBounds(0, 72, W, 120);
        weatherConditionImage.setHorizontalAlignment(SwingConstants.CENTER);
        dashboardPanel.add(weatherConditionImage);

        // Temperature
        temperatureText = new JLabel("--");
        temperatureText.setBounds(0, 195, W, 45);
        temperatureText.setFont(new Font("SansSerif", Font.BOLD, 44));
        temperatureText.setForeground(Color.WHITE);
        temperatureText.setHorizontalAlignment(SwingConstants.CENTER);
        dashboardPanel.add(temperatureText);

        // Weather condition description
        weatherConditionDesc = new JLabel("--");
        weatherConditionDesc.setBounds(0, 238, W, 25);
        weatherConditionDesc.setFont(new Font("SansSerif", Font.PLAIN, 18));
        weatherConditionDesc.setForeground(new Color(210, 210, 240));
        weatherConditionDesc.setHorizontalAlignment(SwingConstants.CENTER);
        dashboardPanel.add(weatherConditionDesc);

        // ===== DETAILS CARD (Humidity + Wind) =====
        GlassPanel detailsCard = new GlassPanel(12);
        detailsCard.setBounds(15, 270, 440, 70);

        JLabel humidityIcon = new JLabel(loadScaledImage("src/assets/humidity.png", 32, 32));
        humidityIcon.setBounds(18, 19, 32, 32);
        detailsCard.add(humidityIcon);

        humidityText = new JLabel(buildStatHtml("Humidity", "--"));
        humidityText.setBounds(56, 14, 120, 42);
        humidityText.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detailsCard.add(humidityText);

        JLabel windIcon = new JLabel(loadScaledImage("src/assets/windspeed.png", 32, 32));
        windIcon.setBounds(200, 19, 32, 32);
        detailsCard.add(windIcon);

        windspeedText = new JLabel(buildStatHtml("Windspeed", "--"));
        windspeedText.setBounds(238, 14, 130, 42);
        windspeedText.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detailsCard.add(windspeedText);

        dashboardPanel.add(detailsCard);

        // ===== HOURLY FORECAST CARD =====
        GlassPanel hourlyCard = new GlassPanel(12);
        hourlyCard.setBounds(15, 348, 440, 100);

        JLabel hourlyTitle = new JLabel("HOURLY FORECAST");
        hourlyTitle.setBounds(15, 5, 200, 16);
        hourlyTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        hourlyTitle.setForeground(new Color(160, 160, 200));
        hourlyCard.add(hourlyTitle);

        int hSpacing = 70;
        for (int i = 0; i < 6; i++) {
            int hx = 15 + i * hSpacing;

            hourlyTimeLabels[i] = new JLabel("--", SwingConstants.CENTER);
            hourlyTimeLabels[i].setBounds(hx, 24, 55, 14);
            hourlyTimeLabels[i].setFont(new Font("SansSerif", Font.PLAIN, 11));
            hourlyTimeLabels[i].setForeground(new Color(180, 180, 220));
            hourlyCard.add(hourlyTimeLabels[i]);

            hourlyIconLabels[i] = new JLabel((Icon) null, SwingConstants.CENTER);
            hourlyIconLabels[i].setBounds(hx, 40, 55, 30);
            hourlyCard.add(hourlyIconLabels[i]);

            hourlyTempLabels[i] = new JLabel("--", SwingConstants.CENTER);
            hourlyTempLabels[i].setBounds(hx, 73, 55, 16);
            hourlyTempLabels[i].setFont(new Font("SansSerif", Font.BOLD, 12));
            hourlyTempLabels[i].setForeground(Color.WHITE);
            hourlyCard.add(hourlyTempLabels[i]);
        }
        dashboardPanel.add(hourlyCard);

        // ===== DAILY FORECAST CARD (4 days) =====
        GlassPanel forecastCard = new GlassPanel(12);
        forecastCard.setBounds(15, 456, 440, 135);

        JLabel forecastTitle = new JLabel("DAILY FORECAST");
        forecastTitle.setBounds(15, 5, 200, 16);
        forecastTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        forecastTitle.setForeground(new Color(160, 160, 200));
        forecastCard.add(forecastTitle);

        int dCardW = 95;
        int dSpacing = 10;
        int dStartX = 12;

        for (int i = 0; i < 4; i++) {
            GlassPanel dayCard = new GlassPanel(8);
            dayCard.setBounds(dStartX + i * (dCardW + dSpacing), 26, dCardW, 100);

            forecastDayLabels[i] = new JLabel("--", SwingConstants.CENTER);
            forecastDayLabels[i].setBounds(0, 6, dCardW, 15);
            forecastDayLabels[i].setFont(new Font("SansSerif", Font.BOLD, 12));
            forecastDayLabels[i].setForeground(Color.WHITE);
            dayCard.add(forecastDayLabels[i]);

            forecastIconLabels[i] = new JLabel((Icon) null, SwingConstants.CENTER);
            forecastIconLabels[i].setBounds(0, 22, dCardW, 30);
            dayCard.add(forecastIconLabels[i]);

            forecastTempLabels[i] = new JLabel("--", SwingConstants.CENTER);
            forecastTempLabels[i].setBounds(0, 55, dCardW, 14);
            forecastTempLabels[i].setFont(new Font("SansSerif", Font.PLAIN, 10));
            forecastTempLabels[i].setForeground(new Color(210, 210, 240));
            dayCard.add(forecastTempLabels[i]);

            forecastHumLabels[i] = new JLabel("--", SwingConstants.CENTER);
            forecastHumLabels[i].setBounds(0, 73, dCardW, 18);
            forecastHumLabels[i].setFont(new Font("SansSerif", Font.PLAIN, 10));
            forecastHumLabels[i].setForeground(new Color(160, 190, 230));
            dayCard.add(forecastHumLabels[i]);

            forecastCard.add(dayCard);
        }
        dashboardPanel.add(forecastCard);

        // ===== TEMPERATURE TREND CHART =====
        GlassPanel trendCard = new GlassPanel(12);
        trendCard.setBounds(15, 599, 440, 100);

        JLabel trendTitle = new JLabel("TEMPERATURE TREND (12h)");
        trendTitle.setBounds(15, 5, 250, 16);
        trendTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        trendTitle.setForeground(new Color(160, 160, 200));
        trendCard.add(trendTitle);

        tempTrendChart = new TempTrendChart();
        tempTrendChart.setBounds(10, 24, 420, 68);
        trendCard.add(tempTrendChart);

        dashboardPanel.add(trendCard);

        // ===== WEATHER ALERTS CARD =====
        alertsCard = new GlassPanel(12);
        alertsCard.setBounds(15, 707, 440, 50);

        JLabel alertIcon = new JLabel("\u26A0");
        alertIcon.setBounds(12, 8, 24, 34);
        alertIcon.setFont(new Font("SansSerif", Font.PLAIN, 18));
        alertsCard.add(alertIcon);

        alertsLabel = new JLabel("<html><font color='#90EE90'>No active weather alerts</font></html>");
        alertsLabel.setBounds(38, 8, 390, 34);
        alertsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        alertsCard.add(alertsLabel);

        dashboardPanel.add(alertsCard);

        // Loading overlay
        dashboardLoadingOverlay = new LoadingOverlay();
        dashboardLoadingOverlay.setBounds(0, 0, 480, 770);
        dashboardPanel.add(dashboardLoadingOverlay, 0);
    }

    // ================ COMPARE PANEL ================
    private void buildComparePanel() {
        comparePanel = new JPanel();
        comparePanel.setLayout(null);
        comparePanel.setOpaque(false);

        // City 1 Input
        compCity1Field = createGlassTextField(15, 10, 160, 42);
        compCity1Field.setText("London");
        comparePanel.add(compCity1Field);

        // City 2 Input
        compCity2Field = createGlassTextField(190, 10, 160, 42);
        compCity2Field.setText("Paris");
        comparePanel.add(compCity2Field);

        // Compare button
        JButton btnCompareSearch = createGlassButton("VS", 365, 10, 65, 42, 14);
        btnCompareSearch.addActionListener(e -> compareWeather(compCity1Field.getText(), compCity2Field.getText()));
        comparePanel.add(btnCompareSearch);

        // Left City Card
        GlassPanel cardCity1 = new GlassPanel(15);
        cardCity1.setBounds(15, 65, 210, 500);

        compCity1NameLabel = new JLabel("London", SwingConstants.CENTER);
        compCity1NameLabel.setBounds(10, 15, 190, 25);
        compCity1NameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        compCity1NameLabel.setForeground(Color.WHITE);
        cardCity1.add(compCity1NameLabel);

        compCity1IconLabel = new JLabel((Icon) null, SwingConstants.CENTER);
        compCity1IconLabel.setBounds(0, 50, 210, 80);
        cardCity1.add(compCity1IconLabel);

        compCity1TempLabel = new JLabel("--", SwingConstants.CENTER);
        compCity1TempLabel.setBounds(0, 135, 210, 36);
        compCity1TempLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        compCity1TempLabel.setForeground(Color.WHITE);
        cardCity1.add(compCity1TempLabel);

        compCity1DescLabel = new JLabel("--", SwingConstants.CENTER);
        compCity1DescLabel.setBounds(0, 172, 210, 18);
        compCity1DescLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        compCity1DescLabel.setForeground(new Color(200, 200, 230));
        cardCity1.add(compCity1DescLabel);

        addSeparator(cardCity1, 20, 200, 170);

        JLabel c1HumIcon = new JLabel(loadScaledImage("src/assets/humidity.png", 26, 26));
        c1HumIcon.setBounds(15, 215, 26, 26);
        cardCity1.add(c1HumIcon);
        compCity1HumidityLabel = new JLabel(buildStatHtml("Humidity", "--"));
        compCity1HumidityLabel.setBounds(48, 210, 140, 36);
        compCity1HumidityLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cardCity1.add(compCity1HumidityLabel);

        JLabel c1WindIcon = new JLabel(loadScaledImage("src/assets/windspeed.png", 26, 26));
        c1WindIcon.setBounds(15, 260, 26, 26);
        cardCity1.add(c1WindIcon);
        compCity1WindLabel = new JLabel(buildStatHtml("Windspeed", "--"));
        compCity1WindLabel.setBounds(48, 255, 140, 36);
        compCity1WindLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cardCity1.add(compCity1WindLabel);

        compCity1MinMaxLabel = new JLabel("<html><center><font color='#DCDCPF'>Tomorrow</font><br/><b><font color='white'>--</font></b></center></html>", SwingConstants.CENTER);
        compCity1MinMaxLabel.setBounds(0, 310, 210, 42);
        compCity1MinMaxLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cardCity1.add(compCity1MinMaxLabel);

        comparePanel.add(cardCity1);

        // Right City Card
        GlassPanel cardCity2 = new GlassPanel(15);
        cardCity2.setBounds(240, 65, 210, 500);

        compCity2NameLabel = new JLabel("Paris", SwingConstants.CENTER);
        compCity2NameLabel.setBounds(10, 15, 190, 25);
        compCity2NameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        compCity2NameLabel.setForeground(Color.WHITE);
        cardCity2.add(compCity2NameLabel);

        compCity2IconLabel = new JLabel((Icon) null, SwingConstants.CENTER);
        compCity2IconLabel.setBounds(0, 50, 210, 80);
        cardCity2.add(compCity2IconLabel);

        compCity2TempLabel = new JLabel("--", SwingConstants.CENTER);
        compCity2TempLabel.setBounds(0, 135, 210, 36);
        compCity2TempLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        compCity2TempLabel.setForeground(Color.WHITE);
        cardCity2.add(compCity2TempLabel);

        compCity2DescLabel = new JLabel("--", SwingConstants.CENTER);
        compCity2DescLabel.setBounds(0, 172, 210, 18);
        compCity2DescLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        compCity2DescLabel.setForeground(new Color(200, 200, 230));
        cardCity2.add(compCity2DescLabel);

        addSeparator(cardCity2, 20, 200, 170);

        JLabel c2HumIcon = new JLabel(loadScaledImage("src/assets/humidity.png", 26, 26));
        c2HumIcon.setBounds(15, 215, 26, 26);
        cardCity2.add(c2HumIcon);
        compCity2HumidityLabel = new JLabel(buildStatHtml("Humidity", "--"));
        compCity2HumidityLabel.setBounds(48, 210, 140, 36);
        compCity2HumidityLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cardCity2.add(compCity2HumidityLabel);

        JLabel c2WindIcon = new JLabel(loadScaledImage("src/assets/windspeed.png", 26, 26));
        c2WindIcon.setBounds(15, 260, 26, 26);
        cardCity2.add(c2WindIcon);
        compCity2WindLabel = new JLabel(buildStatHtml("Windspeed", "--"));
        compCity2WindLabel.setBounds(48, 255, 140, 36);
        compCity2WindLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cardCity2.add(compCity2WindLabel);

        compCity2MinMaxLabel = new JLabel("<html><center><font color='#DCDCPF'>Tomorrow</font><br/><b><font color='white'>--</font></b></center></html>", SwingConstants.CENTER);
        compCity2MinMaxLabel.setBounds(0, 310, 210, 42);
        compCity2MinMaxLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cardCity2.add(compCity2MinMaxLabel);

        comparePanel.add(cardCity2);

        // Compare loading overlay
        compareLoadingOverlay = new LoadingOverlay();
        compareLoadingOverlay.setBounds(0, 0, 480, 770);
        comparePanel.add(compareLoadingOverlay, 0);
    }

    // ================ DATA FETCHING ================
    private void searchWeather(String userInput) {
        if(userInput.replaceAll("\\s", "").isEmpty()) return;
        dashboardLoadingOverlay.startLoading();

        new Thread(() -> {
            JSONObject data = WeatherApp.getWeatherData(userInput);
            SwingUtilities.invokeLater(() -> {
                dashboardLoadingOverlay.stopLoading();
                if(data == null){
                    JOptionPane.showMessageDialog(WeatherAppGui.this,
                        "Could not retrieve weather data. Please enter a valid location.",
                        "Location Not Found", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                weatherData = data;
                updateUIWithWeatherData();
            });
        }).start();
    }

    private void compareWeather(String city1, String city2) {
        if(city1.replaceAll("\\s", "").isEmpty() || city2.replaceAll("\\s", "").isEmpty()) return;
        compareLoadingOverlay.startLoading();

        new Thread(() -> {
            JSONObject data1 = WeatherApp.getWeatherData(city1);
            JSONObject data2 = WeatherApp.getWeatherData(city2);
            SwingUtilities.invokeLater(() -> {
                compareLoadingOverlay.stopLoading();
                if(data1 == null || data2 == null){
                    JOptionPane.showMessageDialog(WeatherAppGui.this,
                        "Could not retrieve weather data for one or both cities.",
                        "Comparison Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                compareCity1Name = city1;
                compareCity2Name = city2;
                compareData1 = data1;
                compareData2 = data2;
                updateCompareLabels();
            });
        }).start();
    }

    // ================ UI UPDATE METHODS ================
    private void updateUIWithWeatherData() {
        if (weatherData == null) return;

        // Date/Time
        String localDt = (String) weatherData.get("local_datetime");
        if(localDt != null) dateTimeLabel.setText(localDt);

        // Current weather icon
        String weatherCondition = (String) weatherData.get("weather_condition");
        weatherConditionImage.setIcon(loadScaledImage(getWeatherAssetPath(weatherCondition), 110, 110));

        // Temperature
        double currentTemp = (double) weatherData.get("temperature");
        temperatureText.setText(formatTemp(currentTemp));

        // Condition description
        weatherConditionDesc.setText(weatherCondition);

        // Humidity
        long humidity = (long) weatherData.get("humidity");
        humidityText.setText(buildStatHtml("Humidity", humidity + "%"));

        // Windspeed
        double windspeed = (double) weatherData.get("windspeed");
        windspeedText.setText(buildStatHtml("Windspeed", windspeed + " km/h"));

        // Hourly forecast
        JSONArray hourlyForecast = (JSONArray) weatherData.get("hourly_forecast");
        if(hourlyForecast != null){
            for(int i = 0; i < 6 && i < hourlyForecast.size(); i++){
                JSONObject h = (JSONObject) hourlyForecast.get(i);
                String hTime = (String) h.get("time");
                double hTemp = (double) h.get("temperature");
                String hCond = (String) h.get("weather_condition");

                hourlyTimeLabels[i].setText(formatHour(hTime));
                hourlyIconLabels[i].setIcon(loadScaledImage(getWeatherAssetPath(hCond), 24, 24));
                hourlyTempLabels[i].setText(formatTempShort(hTemp));
            }
        }

        // Daily forecast
        JSONArray forecast = (JSONArray) weatherData.get("forecast");
        if(forecast != null){
            for(int i = 0; i < 4 && i < forecast.size(); i++){
                JSONObject d = (JSONObject) forecast.get(i);
                String dateStr = (String) d.get("date");
                String dCond = (String) d.get("weather_condition");
                double maxT = (double) d.get("temp_max");
                double minT = (double) d.get("temp_min");
                long dHum = (long) d.get("humidity");

                forecastDayLabels[i].setText(i == 0 ? "Today" : getDayOfWeekName(dateStr));
                forecastIconLabels[i].setIcon(loadScaledImage(getWeatherAssetPath(dCond), 28, 28));
                forecastTempLabels[i].setText(formatMinMaxTemp(minT, maxT));
                forecastHumLabels[i].setText("<html><font color='#A0B8E0'>Humidity</font> <b><font color='white'>" + dHum + "%</font></b></html>");
            }
        }

        // Temperature trend chart
        JSONArray tempTrend = (JSONArray) weatherData.get("temp_trend");
        if(tempTrend != null){
            double[] temps = new double[tempTrend.size()];
            for(int i = 0; i < tempTrend.size(); i++){
                temps[i] = ((Number) tempTrend.get(i)).doubleValue();
            }
            tempTrendChart.setData(temps, isCelsius);
        }

        // Alerts
        JSONArray alerts = (JSONArray) weatherData.get("alerts");
        if(alerts != null && !alerts.isEmpty()){
            StringBuilder alertText = new StringBuilder("<html>");
            for(int i = 0; i < alerts.size(); i++){
                if(i > 0) alertText.append(" \u2022 ");
                alertText.append("<font color='#FFB347'>").append(alerts.get(i)).append("</font>");
            }
            alertText.append("</html>");
            alertsLabel.setText(alertText.toString());
        } else {
            alertsLabel.setText("<html><font color='#90EE90'>\u2705 No active weather alerts</font></html>");
        }
    }

    private void updateCompareLabels() {
        if (compareData1 == null || compareData2 == null) return;

        updateSingleCompareCard(compareData1, compareCity1Name,
            compCity1NameLabel, compCity1IconLabel, compCity1TempLabel,
            compCity1DescLabel, compCity1HumidityLabel, compCity1WindLabel, compCity1MinMaxLabel);

        updateSingleCompareCard(compareData2, compareCity2Name,
            compCity2NameLabel, compCity2IconLabel, compCity2TempLabel,
            compCity2DescLabel, compCity2HumidityLabel, compCity2WindLabel, compCity2MinMaxLabel);
    }

    private void updateSingleCompareCard(JSONObject data, String cityName,
            JLabel nameLabel, JLabel iconLabel, JLabel tempLabel,
            JLabel descLabel, JLabel humLabel, JLabel windLabel, JLabel minMaxLabel) {

        nameLabel.setText(capitalizeCity(cityName));
        String cond = (String) data.get("weather_condition");
        iconLabel.setIcon(loadScaledImage(getWeatherAssetPath(cond), 65, 65));
        double temp = (double) data.get("temperature");
        tempLabel.setText(formatTemp(temp));
        descLabel.setText(cond);
        long hum = (long) data.get("humidity");
        humLabel.setText(buildStatHtml("Humidity", hum + "%"));
        double wind = (double) data.get("windspeed");
        windLabel.setText(buildStatHtml("Windspeed", wind + " km/h"));

        JSONArray forecast = (JSONArray) data.get("forecast");
        if(forecast != null && forecast.size() > 1){
            JSONObject day = (JSONObject) forecast.get(1);
            double min = (double) day.get("temp_min");
            double max = (double) day.get("temp_max");
            minMaxLabel.setText("<html><center><font color='#DCDCPF'>Tomorrow</font><br/><b><font color='white'>" + formatMinMaxTemp(min, max) + "</font></b></center></html>");
        }
    }

    // ================ HELPER METHODS ================
    private String buildStatHtml(String label, String value) {
        return "<html><font color='#DCDCPF'>" + label + "</font><br/><b><font color='white' size='4'>" + value + "</font></b></html>";
    }

    private String getWeatherAssetPath(String cond) {
        if (cond == null) return "src/assets/cloudy.png";
        switch(cond){
            case "Clear": return "src/assets/clear.png";
            case "Cloudy": return "src/assets/cloudy.png";
            case "Rain": return "src/assets/rain.png";
            case "Snow": return "src/assets/snow.png";
            default: return "src/assets/cloudy.png";
        }
    }

    private String formatTemp(double celsius) {
        return isCelsius ? String.format("%.1f\u00B0C", celsius) :
                           String.format("%.1f\u00B0F", toF(celsius));
    }
    private String formatTempShort(double celsius) {
        return isCelsius ? String.format("%.0f\u00B0", celsius) :
                           String.format("%.0f\u00B0", toF(celsius));
    }
    private String formatMinMaxTemp(double min, double max) {
        return isCelsius ? String.format("%.0f\u00B0 / %.0f\u00B0C", min, max) :
                           String.format("%.0f\u00B0 / %.0f\u00B0F", toF(min), toF(max));
    }
    private double toF(double c) { return c * 9.0 / 5.0 + 32.0; }

    private String formatHour(String timeStr) {
        try {
            int hour = Integer.parseInt(timeStr.split(":")[0]);
            if(hour == 0) return "12 AM";
            if(hour == 12) return "12 PM";
            return hour > 12 ? (hour - 12) + " PM" : hour + " AM";
        } catch(Exception e){ return timeStr; }
    }

    private String getDayOfWeekName(String dateStr) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            return date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
        } catch (Exception e) { return dateStr; }
    }

    private String capitalizeCity(String city) {
        if (city == null || city.isEmpty()) return "";
        String[] words = city.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0)
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    private void addSeparator(JPanel parent, int x, int y, int width) {
        JSeparator sep = new JSeparator();
        sep.setBounds(x, y, width, 2);
        sep.setForeground(new Color(255, 255, 255, 40));
        parent.add(sep);
    }

    // ================ FACTORY METHODS FOR GLASS COMPONENTS ================
    private JButton createNavButton(String text, int x, int width) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (currentCard.equals(text)) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(new Color(255, 255, 255, 40));
                    g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 8, 8);
                    g2d.dispose();
                }
                super.paintComponent(g);
            }
        };
        btn.setBounds(x, 0, width, 42);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createGlassButton(String text, int x, int y, int w, int h, int fontSize) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2d.setColor(new Color(255, 255, 255, 50));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        btn.setBounds(x, y, w, h);
        btn.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createGlassIconButton(String iconPath, int x, int y, int w, int h) {
        JButton btn = new JButton(loadScaledImage(iconPath, 20, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2d.setColor(new Color(255, 255, 255, 50));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        btn.setBounds(x, y, w, h);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JTextField createGlassTextField(int x, int y, int w, int h) {
        JTextField tf = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2d.setColor(new Color(255, 255, 255, 50));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        tf.setOpaque(false);
        tf.setBounds(x, y, w, h);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 16));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        return tf;
    }

    private ImageIcon loadScaledImage(String resourcePath, int width, int height){
        try{
            BufferedImage image = ImageIO.read(new File(resourcePath));
            Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        }catch(IOException e){
            e.printStackTrace();
        }
        System.out.println("Could not find resource: " + resourcePath);
        return null;
    }

    // ================ INNER CLASSES ================

    // Temperature Trend Chart - draws a smooth line graph
    private class TempTrendChart extends JPanel {
        private double[] data;
        private boolean celsius = true;

        public TempTrendChart() {
            setOpaque(false);
        }

        public void setData(double[] temps, boolean isCelsius) {
            this.data = temps;
            this.celsius = isCelsius;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.length < 2) return;

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int padL = 30, padR = 10, padT = 12, padB = 16;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            // Find min/max for scaling
            double min = data[0], max = data[0];
            for (double d : data) {
                if (d < min) min = d;
                if (d > max) max = d;
            }
            // Convert if needed
            double dispMin = celsius ? min : toF(min);
            double dispMax = celsius ? max : toF(max);
            if(max == min) max += 1;

            // Draw Y-axis labels
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 9));
            g2d.setColor(new Color(160, 160, 200));
            g2d.drawString(String.format("%.0f\u00B0", dispMax), 0, padT + 8);
            g2d.drawString(String.format("%.0f\u00B0", dispMin), 0, h - padB + 4);

            // Build path
            Path2D.Float path = new Path2D.Float();
            Path2D.Float fillPath = new Path2D.Float();
            int n = data.length;

            for (int i = 0; i < n; i++) {
                float x = padL + (float) i / (n - 1) * chartW;
                float y = padT + (float) (1.0 - (data[i] - min) / (max - min)) * chartH;
                if (i == 0) {
                    path.moveTo(x, y);
                    fillPath.moveTo(x, h - padB);
                    fillPath.lineTo(x, y);
                } else {
                    path.lineTo(x, y);
                    fillPath.lineTo(x, y);
                }
            }

            // Close fill path
            fillPath.lineTo(padL + chartW, h - padB);
            fillPath.closePath();

            // Draw gradient fill under the curve
            GradientPaint fillGradient = new GradientPaint(0, padT, new Color(138, 180, 248, 60), 0, h - padB, new Color(138, 180, 248, 5));
            g2d.setPaint(fillGradient);
            g2d.fill(fillPath);

            // Draw the line
            g2d.setColor(new Color(138, 180, 248));
            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(path);

            // Draw data points
            for (int i = 0; i < n; i++) {
                float x = padL + (float) i / (n - 1) * chartW;
                float y = padT + (float) (1.0 - (data[i] - min) / (max - min)) * chartH;
                g2d.setColor(new Color(138, 180, 248));
                g2d.fillOval((int) x - 3, (int) y - 3, 6, 6);
                g2d.setColor(new Color(20, 24, 46));
                g2d.fillOval((int) x - 1, (int) y - 1, 2, 2);
            }

            // Draw hour labels on X-axis
            g2d.setColor(new Color(140, 140, 180));
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 8));
            for(int i = 0; i < n; i += 3){
                float x = padL + (float) i / (n - 1) * chartW;
                g2d.drawString("+" + i + "h", (int) x - 6, h - 2);
            }

            g2d.dispose();
        }
    }

    // Slide transition panel
    private class TransitionPanel extends JPanel {
        private JPanel activePanel;
        private JPanel nextPanel;
        private double progress = 1.0;
        private Timer transitionTimer;
        private boolean slideLeft = true;

        public TransitionPanel() {
            setLayout(null);
            setOpaque(false);
        }

        public void showPanel(JPanel panel, boolean slideLeft) {
            if (activePanel == panel) return;
            if (activePanel == null) {
                activePanel = panel;
                panel.setBounds(0, 0, getWidth(), getHeight());
                add(panel);
                revalidate();
                repaint();
                return;
            }
            if (transitionTimer != null && transitionTimer.isRunning()) {
                transitionTimer.stop();
            }
            this.nextPanel = panel;
            this.slideLeft = slideLeft;
            this.progress = 0.0;
            nextPanel.setBounds(slideLeft ? getWidth() : -getWidth(), 0, getWidth(), getHeight());
            add(nextPanel);

            transitionTimer = new Timer(15, e -> {
                progress += 0.06;
                if (progress >= 1.0) {
                    progress = 1.0;
                    ((Timer)e.getSource()).stop();
                    remove(activePanel);
                    activePanel = nextPanel;
                    nextPanel = null;
                    activePanel.setBounds(0, 0, getWidth(), getHeight());
                } else {
                    int offset = (int) (progress * getWidth());
                    if (slideLeft) {
                        activePanel.setBounds(-offset, 0, getWidth(), getHeight());
                        nextPanel.setBounds(getWidth() - offset, 0, getWidth(), getHeight());
                    } else {
                        activePanel.setBounds(offset, 0, getWidth(), getHeight());
                        nextPanel.setBounds(-getWidth() + offset, 0, getWidth(), getHeight());
                    }
                }
                repaint();
            });
            transitionTimer.start();
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            if (activePanel != null) activePanel.setBounds(0, 0, width, height);
        }
    }

    // Loading overlay with spinning arc
    private class LoadingOverlay extends JPanel {
        private double angle = 0;
        private Timer spinnerTimer;

        public LoadingOverlay() {
            setOpaque(false);
            setLayout(null);
            setVisible(false);
            spinnerTimer = new Timer(30, e -> {
                angle = (angle + 12) % 360;
                repaint();
            });
        }

        public void startLoading() { setVisible(true); spinnerTimer.start(); }
        public void stopLoading() { setVisible(false); spinnerTimer.stop(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(20, 24, 46, 180));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            int size = 48;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.setColor(new Color(255, 255, 255, 50));
            g2d.drawOval(x, y, size, size);
            g2d.setColor(new Color(138, 180, 248));
            g2d.drawArc(x, y, size, size, (int) angle, 120);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fm = g2d.getFontMetrics();
            String text = "Loading...";
            g2d.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, y + size + 30);
            g2d.dispose();
        }
    }

    // Translucent rounded panel
    private class GlassPanel extends JPanel {
        private int cornerRadius;

        public GlassPanel(int radius) {
            this.cornerRadius = radius;
            setOpaque(false);
            setLayout(null);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(255, 255, 255, 20));
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            g2d.setColor(new Color(255, 255, 255, 45));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            g2d.dispose();
        }
    }
}
