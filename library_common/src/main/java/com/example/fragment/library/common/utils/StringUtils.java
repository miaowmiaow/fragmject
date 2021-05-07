package com.example.fragment.library.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author CuiZhen
 * @date 2019/10/19
 * GitHub: https://github.com/goweii
 */
public class StringUtils {

    public static String removeAllBank(String str) {
        String s = "";
        if (str != null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            s = m.replaceAll("");
        }
        return s;
    }

    public static String removeAllBank(String str, int count) {
        String s = "";
        if (str != null) {
            Pattern p = Pattern.compile("\\s{" + count + ",}|\t|\r|\n");
            Matcher m = p.matcher(str);
            s = m.replaceAll(" ");
        }
        return s;
    }

}
