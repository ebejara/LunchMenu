
package com.enrique.lunchmenu;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import com.enrique.lunchmenu.model.DailyMenu;
import com.enrique.lunchmenu.model.MenuItem;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
record ApiMenuItem(String name, String description, Double price) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record ApiDailyMenu(String date, java.util.List<ApiMenuItem> items) {}

public class JsonMenuClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public DailyMenu fetch(String url, String sourceName) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new RuntimeException("HTTP " + resp.statusCode());

        ApiDailyMenu api = mapper.readValue(resp.body(), ApiDailyMenu.class);
        var items = api.items().stream()
                .map(i -> new MenuItem(i.name(), i.description(), i.price()))
                .toList();

        return new DailyMenu(LocalDate.parse(api.date()), items, sourceName, url);
    }
}
