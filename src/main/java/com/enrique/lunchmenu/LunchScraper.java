package com.enrique.lunchmenu;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
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
            Map.entry("måndag", DayOfWeek.MONDAY),
            Map.entry("tisdag", DayOfWeek.TUESDAY),
            Map.entry("onsdag", DayOfWeek.WEDNESDAY),
            Map.entry("torsdag", DayOfWeek.THURSDAY),
            Map.entry("fredag", DayOfWeek.FRIDAY),
            Map.entry("lördag", DayOfWeek.SATURDAY),
            Map.entry("söndag", DayOfWeek.SUNDAY)
    );

    public List<DailyMenu> fetchWeeklyMenu(String url, String sourceName) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

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
}