
package com.enrique.lunchmenu.model;

import java.time.LocalDate;
import java.util.List;

public record DailyMenu(LocalDate date, List<MenuItem> items, String sourceName, String sourceUrl) {}
