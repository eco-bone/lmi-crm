package com.lmi.crm.util;

public class FuzzyMatchUtil {

    private FuzzyMatchUtil() {}

    public static double similarity(String s1, String s2) {
        if (s1 == null || s1.isBlank() || s2 == null || s2.isBlank()) {
            return 0.0;
        }
        String a = s1.toLowerCase().trim();
        String b = s2.toLowerCase().trim();
        int distance = levenshtein(a, b);
        return 1.0 - ((double) distance / Math.max(a.length(), b.length()));
    }

    public static boolean isMatch(String s1, String s2, double threshold) {
        return similarity(s1, s2) >= threshold;
    }

    private static int levenshtein(String a, String b) {
        int m = a.length();
        int n = b.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[m][n];
    }
}
