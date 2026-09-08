package com.gzu.adminconsole.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间范围过滤工具：把各类展示用时间文案归一化为 yyyy-MM-dd 后判断是否落在 [start, end] 内。
 * 兼容格式：完整日期时间（"2026-09-02 22:06" / ISO "T" 分隔）、纯时间（"14:22:08"，视为今天）、
 * 短日期（"09-03"，补当前年份）。
 */
public final class DateRange {

    private static final Pattern FULL_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern SHORT_DATE = Pattern.compile("^\\d{2}-\\d{2}$");
    private static final Pattern TIME_ONLY = Pattern.compile("^\\d{2}:\\d{2}(:\\d{2})?$");
    /** 时间戳格式：yyyy-MM-dd HH:mm:ss。 */
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateRange() {
    }

    /** 是否落在 [start, end]（yyyy-MM-dd，含边界）内；任一边界为空表示不限制。 */
    public static boolean inRange(String time, String start, String end) {
        boolean noStart = start == null || start.isBlank();
        boolean noEnd = end == null || end.isBlank();
        if (noStart && noEnd) {
            return true;
        }
        String date = toDate(time);
        // 无日期维度的数据（如 "—"）不参与过滤，始终保留
        if (date == null) {
            return true;
        }
        if (!noStart && date.compareTo(start) < 0) {
            return false;
        }
        if (!noEnd && date.compareTo(end) > 0) {
            return false;
        }
        return true;
    }

    /** 把纯时间（"14:22:08"）补上今天日期，便于新登记的告警参与范围过滤；空值取当前时刻，其余格式原样返回。 */
    public static String withDate(String time) {
        String s = time == null ? "" : time.trim();
        if (s.isEmpty()) {
            return LocalDateTime.now().format(STAMP);
        }
        if (TIME_ONLY.matcher(s).matches()) {
            return LocalDate.now() + " " + s;
        }
        return s;
    }

    /** 从时间文案中提取 yyyy-MM-dd；无法识别时返回 null。 */
    private static String toDate(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        String s = time.trim();
        Matcher m = FULL_DATE.matcher(s);
        if (m.find()) {
            return m.group();
        }
        if (TIME_ONLY.matcher(s).matches()) {
            return LocalDate.now().toString();
        }
        if (SHORT_DATE.matcher(s).matches()) {
            return LocalDate.now().getYear() + "-" + s;
        }
        return null;
    }
}
