package com.enrique.lunchmenu;

import java.io.IOException;
import java.util.List;

import com.enrique.lunchmenu.model.DailyMenu;

public class Main {
    public static void main(String[] args) {
        LunchScraper scraper = new LunchScraper();

        // Test with 1–3 real restaurants (update URLs if needed!)
        String[][] restaurants = {
            {"Bistrot", "https://bistrot.se/"},
            {"Kooperativet", "https://www.kooperativet.se/meny/"},
            {"District One", "https://districtone.se/lunch.html"}
        };

        for (String[] restaurant : restaurants) {
            String name = restaurant[0];
            String url = restaurant[1];

            System.out.println("\n=== Scraping: " + name + " ===");
            System.out.println("URL: " + url);

            try {
                List<DailyMenu> weeklyMenu = scraper.fetchWeeklyMenu(url, name);

                if (weeklyMenu.isEmpty()) {
                    System.out.println("→ No menu items found (selectors may need update)");
                } else {
                    for (DailyMenu day : weeklyMenu) {
                        System.out.println(day.date() + " (" + day.sourceName() + "):");
                        for (var item : day.items()) {
                            System.out.printf("  • %s  %.0f kr%n     %s%n",
                                item.title(),
                                item.priceSEK() != null ? item.priceSEK() : 0,
                                item.description() != null ? item.description() : "(no desc)");
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e);
            }
        }

        System.out.println("\nDone! Add more restaurants or improve selectors for better results.");
    }
}