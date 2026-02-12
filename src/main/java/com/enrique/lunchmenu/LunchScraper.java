package com.enrique.lunchmenu;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.enrique.lunchmenu.model.DailyMenu;
import com.enrique.lunchmenu.model.MenuItem;

public class LunchScraper {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36";

    // Swedish weekday mapping
    private static final Map<String, DayOfWeek> SV_WEEKDAYS = Map.ofEntries(
            Map.entry("Måndag", DayOfWeek.MONDAY),
            Map.entry("Tisdag", DayOfWeek.TUESDAY),
            Map.entry("Onsdag", DayOfWeek.WEDNESDAY),
            Map.entry("Torsdag", DayOfWeek.THURSDAY),
            Map.entry("Fredag", DayOfWeek.FRIDAY),
            Map.entry("Lördag", DayOfWeek.SATURDAY),
            Map.entry("Söndag", DayOfWeek.SUNDAY)
    );

    public List<DailyMenu> fetchWeeklyMenu(String url, String sourceName) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

        // ───────────────────────────────────────────────
        // Special handling for District One
        // ───────────────────────────────────────────────
        if ("Spira GC".equals(sourceName) || url.contains("spirafood.se")) {
            System.out.println("Spira Food detected, using parseDistrictOne method.");
            return parseDistrictOne(doc, sourceName, url);
        }

        //Original generic parsing logic can go here for other restaurants
        Element weekly = Optional.ofNullable(
                doc.selectFirst("#weekly-menu, .weekly-menu, section:matchesOwn((?i)veckans|lunch)")
        ).orElse(doc.body());

        List<DailyMenu> results = new ArrayList<>();

        Elements headers = weekly.select("h2, h3, strong, .day-title");

        Pattern dayPattern = Pattern.compile("(?i)^(måndag|tisdag|onsdag|torsdag|fredag|lördag|söndag)");

        for (Element header : headers) {
            String text = header.text().trim();
            Matcher m = dayPattern.matcher(text);
            if (!m.find()) continue;

            String daySv = m.group(1).toLowerCase(Locale.ROOT);
            DayOfWeek dow = SV_WEEKDAYS.get(daySv);
            if (dow == null) continue;

            LocalDate date = dateForUpcoming(dow);

            Element section = nextSiblingsUntil(header, "h2, h3, strong, .day-title");
            List<MenuItem> items = parseMenuItems(section);

            if (!items.isEmpty()) {
                results.add(new DailyMenu(date, items, sourceName, url));
            }
        }

        return results;
    }

    private Element nextSiblingsUntil(Element start, String stopSelectors) {
        Element container = new Element("div");
        for (Element sib = start.nextElementSibling(); sib != null; sib = sib.nextElementSibling()) {
            if (sib.is(stopSelectors)) break;
            container.appendChild(sib.clone());
        }
        return container;
    }

    private List<MenuItem> parseMenuItems(Element container) {
        List<MenuItem> items = new ArrayList<>();
        if (container == null) return items;

        Elements candidates = container.select("li, p, .menu-item, tr");

        for (Element el : candidates) {
            String txt = el.text().replaceAll("\\s+", " ").trim();
            if (txt.isBlank()) continue;

            Double price = extractPriceSEK(txt);
            String clean = txt.replaceAll("(\\d+[\\.,]?\\d*)\\s*(kr|:-)", "").trim();

            String title = clean;
            String desc = null;

            int dash = clean.indexOf(" – ");
            if (dash < 0) dash = clean.indexOf(" - ");
            if (dash > 0) {
                title = clean.substring(0, dash).trim();
                desc = clean.substring(dash + 3).trim();
            }

            if (title.length() < 3 || title.matches("(?i).*allerg.*|.*öppet.*|.*serveras.*")) continue;

            items.add(new MenuItem(title, desc, price));
        }

        return items;
    }

    private Double extractPriceSEK(String s) {
        Matcher m = Pattern.compile("(\\d+[\\.,]?\\d*)\\s*(kr|:-)", Pattern.CASE_INSENSITIVE).matcher(s);
        if (m.find()) {
            return Double.valueOf(m.group(1).replace(",", "."));
        }
        return null;
    }

    private LocalDate dateForUpcoming(DayOfWeek dow) {
        LocalDate today = LocalDate.now();
        int diff = dow.getValue() - today.getDayOfWeek().getValue();
        if (diff < 0) diff += 7;
        return today.plusDays(diff);
    }

    private List<DailyMenu> parseDistrictOne(Document doc, String sourceName, String url) {
    List<DailyMenu> results = new ArrayList<>();

    // Hämta all text med nya rader bevarade
    String fullText = doc.body().wholeText();
    System.out.println("Fetched text length: ~" + fullText.length());

    String[] lines = fullText.split("\\r?\\n");

    String[] weekdays = {"Måndag", "Tisdag", "Onsdag", "Torsdag", "Fredag"};
    // Vanliga kategorier (lägg till fler om behövs)
    java.util.Set<String> categories = new java.util.HashSet<>(Arrays.asList(
        "Fisk", "Kött", "Sallad", "Vegetarisk", "Poke bowl", "Vegetarisk Poke bowl",
        "Pho", "Fried Chicken", "Asiatisk", "Bao buns"
    ));

    LocalDate currentDate = null;
    List<MenuItem> currentItems = null;
    String pendingCategory = null;

    for (String lineRaw : lines) {
        String line = lineRaw.trim();
        if (line.isEmpty() || line.matches("^\\.+\\s*$")) continue;

        // Ny dag?
        if (Arrays.asList(weekdays).contains(line)) {
            // Spara föregående dag om den har items
            if (currentDate != null && currentItems != null && !currentItems.isEmpty()) {
                results.add(new DailyMenu(currentDate, currentItems, sourceName, url));
            }
            DayOfWeek dow = SV_WEEKDAYS.get(line.toLowerCase(Locale.ROOT));
            if (dow != null) {
                currentDate = dateForUpcoming(dow);
                currentItems = new ArrayList<>();
                System.out.println("Detected day: " + line + " (" + currentDate + ")");
            }
            pendingCategory = null;
            continue;
        }

        if (currentItems == null) continue; // Innehåll innan första dagen ignoreras

        // Är det en kategori?
        if (categories.contains(line) || line.startsWith("Vegetarisk") || line.contains("bowl")) {  // lite flex för variationer
            if (pendingCategory != null) {
                // Om föregående kategori saknade desc → lägg till tom eller skippa
            }
            pendingCategory = line;
            continue;
        }

        // Annars: antag att det är beskrivning till pending kategori
        if (pendingCategory != null) {
            String title = pendingCategory;
            String desc = line;
            Double price = 135.0;

            currentItems.add(new MenuItem(title, desc, price));
            System.out.println("  Added: " + title + " → " + desc.substring(0, Math.min(50, desc.length())) + "...");
            pendingCategory = null;
        }
    }

    // Spara sista dagen
    if (currentDate != null && currentItems != null && !currentItems.isEmpty()) {
        results.add(new DailyMenu(currentDate, currentItems, sourceName, url));
    }

    System.out.println("Total parsed days: " + results.size() + " with ~" + 
        results.stream().mapToInt(d -> d.items().size()).sum() + " items.");
    return results;
}
}