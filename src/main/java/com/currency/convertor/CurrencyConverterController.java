package com.currency.convertor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController

public class CurrencyConverterController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String exchangeRatesUrl = "https://openexchangerates.org/api/latest.json";
    private final String apiKey;
    private final Map<String, Double> exchangeRatesCache = new HashMap<>();
    private Instant lastExchangeRatesUpdate;

    public CurrencyConverterController(@Value("${openexchangerates.api.key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @GetMapping("/convert")
    public double convertCurrency(@RequestParam("source") String sourceCurrency,
                                  @RequestParam("target") String targetCurrency,
                                  @RequestParam("amount") double amount) {
        // Check if exchange rates cache is stale, and update if necessary
        if (lastExchangeRatesUpdate == null || Duration.between(lastExchangeRatesUpdate, Instant.now()).toHours() >= 1) {
            updateExchangeRatesCache();
        }

        // Check if exchange rate for the requested currency pair is in the cache
        String currencyPair = sourceCurrency + "_" + targetCurrency;
        Double exchangeRate = exchangeRatesCache.get(currencyPair);
        if (exchangeRate == null) {
            throw new IllegalArgumentException("Exchange rate not available for " + currencyPair);
        }

        // Calculate the converted amount and return it
        double convertedAmount = amount * exchangeRate;
        return convertedAmount;
    }

    private void updateExchangeRatesCache() {
        String url = exchangeRatesUrl + "?app_id=" + apiKey + "&base=USD";
        ResponseEntity<ExchangeRatesResponse> response = restTemplate.getForEntity(url, ExchangeRatesResponse.class);

        ExchangeRatesResponse exchangeRates = response.getBody();

        exchangeRatesCache.clear();
        exchangeRates.getRates().forEach((currencyCode, exchangeRate) -> {
            String currencyPair = "USD_" + currencyCode;
            exchangeRatesCache.put(currencyPair, exchangeRate);
        });


        lastExchangeRatesUpdate = Instant.now();
    }

}
