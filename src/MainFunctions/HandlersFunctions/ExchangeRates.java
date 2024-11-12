package MainFunctions.HandlersFunctions;

import java.util.HashMap;
import java.util.Map;

public class ExchangeRates {
    public static final Map<String, Double> exchangeRates = new HashMap<>();

    static {
        exchangeRates.put("RUB", 0.013);
        exchangeRates.put("UAH", 0.027);
        exchangeRates.put("TRY", 0.060);
        exchangeRates.put("KZT", 0.0023);
        exchangeRates.put("PLN", 0.24);
        exchangeRates.put("CNY", 0.14);
        exchangeRates.put("USD", 1.0);
    }
}
