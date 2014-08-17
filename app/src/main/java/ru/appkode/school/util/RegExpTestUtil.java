package ru.appkode.school.util;

/**
 * Created by lexer on 17.08.14.
 */
public class RegExpTestUtil {
    public static final String NAME = "[а-яА-ЯёЁa-zA-Z0-9]{2,30}";
    public static final String GROUP = "\\d{1,2}[а-яА-ЯёЁa-zA-Z]{1}";

    public static boolean check(String string, String regExp) {
        return string.matches(regExp);
    }
}
