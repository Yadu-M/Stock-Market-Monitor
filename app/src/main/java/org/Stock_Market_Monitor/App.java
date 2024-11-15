/*
 * Stock Monitor
 * 
 * Note: I am not using Yahoo Finance API as it is now behind a paywall. I am using API from "https://rapidapi.com/letscrape-6bRBa3QguO5/api/real-time-finance-data"
 */
package org.Stock_Market_Monitor;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import static java.util.concurrent.TimeUnit.*;

import java.io.IOException;

import org.json.*;

class Stock {
    private String name;
    private BigDecimal price;
    private String timestamp;

    public Stock(String name, BigDecimal price, String timestamp) {
        this.name = name;
        this.price = price;
        this.timestamp = timestamp;
    }

    public void printStockInfo() {
        System.out.println(
                "Stock Name: " + name + "\n" +
                        "Stock Price: " + price + "\n" +
                        "Timestamp: " + timestamp + "\n");
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getTimestamp() {
        return timestamp;
    }

}

class StockMarket {
    // Please Aquire an API Key from
    // "https://rapidapi.com/letscrape-6bRBa3QguO5/api/real-time-finance-data"
    static final private String API_KEY = "";
    static final private String baseURL = "https://real-time-finance-data.p.rapidapi.com/stock-quote?symbol=";
    static public Queue<Stock> stockDataQueue = new LinkedList<>();

    static Stock getStockData(String symbol) throws Exception {
        if (API_KEY.equals(""))
            throw new Exception("API key is empty");

        BigDecimal price = new BigDecimal(0);
        String timestamp = "";
        String name = "";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseURL + symbol + "&language=en"))
                .header("x-rapidapi-key", API_KEY)
                .header("x-rapidapi-host", "real-time-finance-data.p.rapidapi.com")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofString());
        JSONObject jsonObj = new JSONObject(response.body());
        JSONObject data = jsonObj.getJSONObject("data");

        name = data.getString("name");
        price = data.getBigDecimal("price");
        timestamp = data.getString("last_update_utc");

        return new Stock(name, price, timestamp);
    }

    public static void getScheduledStockData(int seconds, String company) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable stockDataRunnable = new Runnable() {
            public void run() {
                try {
                    Stock stock = getStockData(company);
                    stockDataQueue.add(stock);
                    stock.printStockInfo();
                } catch (IOException e) {
                    System.out.println("Something went wrong while sending or recieveing packets");
                    scheduler.shutdown();
                } catch (InterruptedException e) {
                    System.out.println("Something interrupted the operation");
                    scheduler.shutdown();
                } catch (JSONException e) {
                    System.out.println("Couldn't parse JSON");
                    scheduler.shutdown();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    scheduler.shutdown();
                }

            }
        };
        scheduler.scheduleAtFixedRate(stockDataRunnable, 0, seconds, SECONDS);
    }
}

public class App extends Application {

    @Override
    public void start(Stage stage) {

        String company = ".DJI:INDEXDJX";
        final int seconds = 5;
        Queue<Stock> stockData = new LinkedList<>();

        // Defining the axes
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();  
        xAxis.setLabel("Seconds");
        yAxis.setLabel("Price ($)");

        // Creating the chart
        final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Stock Monitoring");

        // Defining a series
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Dow Jones Industrial");

        // Add the series to the chart
        lineChart.getData().add(series);

        // Scheduled task for updating stock data every 'seconds'
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable stockDataRunnable = new Runnable() {
            public void run() {
                try {
                    // Get stock data
                    Stock stock = StockMarket.getStockData(company);
                    stockData.add(stock);

                    // Update the chart on the JavaFX Application Thread
                    Platform.runLater(() -> {
                        String timeStamp = stock.getTimestamp();
                        String[] splitDateTime = timeStamp.split(" ");
                        String[] splitTime = splitDateTime[1].split(":");

                        Number price = stock.getPrice();
                        Number seconds = Integer.parseInt(splitTime[2]);  // Getting seconds

                        // Add data point to the series
                        stage.setTitle("RealTime Stock Data Updates, Date: " + splitDateTime[0] + " Time: " + splitTime[0] + splitTime[1]);

                        series.getData().add(new XYChart.Data<>(seconds, price));
                    });

                } catch (IOException e) {
                    System.out.println("Something went wrong while sending or receiving packets");
                    scheduler.shutdown();
                } catch (InterruptedException e) {
                    System.out.println("Something interrupted the operation");
                    scheduler.shutdown();
                } catch (JSONException e) {
                    System.out.println("Couldn't parse JSON");
                    scheduler.shutdown();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    scheduler.shutdown();
                }
            }
        };

        // Schedule the task to run every 'seconds'
        scheduler.scheduleAtFixedRate(stockDataRunnable, 0, seconds, SECONDS);

        // Set up the scene and show the stage
        Scene scene = new Scene(lineChart, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
