package com.smallraw.core

import java.lang.StringBuilder
import java.util.*

class Timing {
    private val stack = Stack<Pair<String, Long>>()

    fun begin() {
        stack.add(Pair("begin", getCurrentTime()))
    }

    fun split(tag: String = "split") {
        stack.add(Pair(tag, getCurrentTime()))
    }

    fun end(tag: String = "end") {
        stack.add(Pair(tag, getCurrentTime()))
    }

    fun splitTime(): String {
        val first = stack.first().second
        stack.reverse()
        val stringBuilder =  StringBuilder()
        while(!stack.empty()){
            val pop = stack.pop()
            stringBuilder.append("${pop.first} ${pop.second - first}\n")
        }
        return stringBuilder.toString()
    }

    private fun getCurrentTime(): Long {
        return System.currentTimeMillis()
    }
}