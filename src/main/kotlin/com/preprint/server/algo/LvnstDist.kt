package com.preprint.server.algo

import java.lang.Integer.min


object LvnstDist {
    fun findDist(str1 : String, str2 : String) : Int {
        val n = str1.length
        val m = str2.length
        val dp = Array(n + 1, {IntArray(m + 1, {0})})
        for (i in 1 until n + 1) {
            for (j in 1 until m + 1) {
                dp[i][j] = min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                if (str1[i - 1] == str2[j - 1]) {
                    dp[i][j] = min(dp[i][j], dp[i - 1][j - 1])
                }
                else {
                    dp[i][j] = min(dp[i][j], dp[i - 1][j - 1] + 1)
                }
            }
        }
        return dp[n][m]
    }
}