package org.palliums.violascore.utils

private fun getIndex(pat: ByteArray, c: Byte): Int {
    for (i in pat.size - 1 downTo 0) {
        if (pat[i] == c) return i
    }
    return -1
}

fun sundaySearch(txt: ByteArray, pat: ByteArray): Int {
    val m = txt.size
    val n = pat.size
    var i = 0
    var j: Int
    var skip = -1
    while (i <= m - n) {
        j = 0
        while (j < n) {
            if (txt[i + j] != pat[j]) {
                if (i == m - n) break
                skip = n - getIndex(pat, txt[i + n])
                break
            }
            j++
        }
        if (j == n) return i
        i += skip
    }
    return -1
}