
package com.enrique.lunchmenu;

import com.enrique.lunchmenu.model.DailyMenu;

public class Main {
    public static void main(String[] args) throws Exception {
        // Replace with a real lunch page URL you want to parse
        String url = "https://districtone.se/lunch.html";
        String sourceName = "District One";

        LunchScraper scraper = new LunchScraper();
        for (DailyMenu dm : scraper.fetchWeeklyMenu(url, sourceName)) {
            System.out.println(dm.date() + " — " + dm.sourceName());
            dm.items().forEach(i ->
                System.out.printf("  • %s%s%s%n",
                    i.title(),
                    i.description() != null ? " — " + i.description() : "",
                    i.priceSEK() != null ? " (" + i.priceSEK().intValue() + " kr)" : "")
            );
        }

        // If you want scheduled fetching, see Scheduler usage later
    }
}
``
