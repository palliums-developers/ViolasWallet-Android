package com.violas.wallet.ui.main.market.bean

/**
 * 可变的 Bitmap 可以记录状态
 */
class MutBitmap : Iterable<Int> {
    companion object {
        private const val BITMAP_NUM_OF_BYTES = 4
        private const val DATA_TYPE_LENGTH = 8
        private const val BITMAP_MARK = 128
    }

    private var bitmap: ByteArray = ByteArray(BITMAP_NUM_OF_BYTES) { 0.toByte() }

    /**
     * 对 Bitmap 数组进行扩容
     * @param index 想要访问的索引下标
     */
    private fun capacity(index: Int) {
        while (indexOutOfBounds(index)) {
            capacity()
        }
    }

    /**
     * 对 Bitmap 进行扩容
     */
    private fun capacity() {
        val newBitmap = ByteArray(bitmap.size * 2)
        System.arraycopy(bitmap, 0, newBitmap, 0, bitmap.size)
        bitmap = newBitmap
    }

    /**
     * 检查要访问的 index 是否超出
     */
    private fun indexOutOfBounds(index: Int): Boolean {
        return index > size() - 1
    }

    fun clearBit(index: Int) {
        if (indexOutOfBounds(index)) {
            return
        }
        val bucketIndex = index / DATA_TYPE_LENGTH
        val innerIndex = index % DATA_TYPE_LENGTH
        bitmap[bucketIndex] =
            (BITMAP_MARK.ushr(innerIndex).inv() and bitmap[bucketIndex].toInt()).toByte()
    }

    fun setBit(index: Int) {
        capacity(index)
        val bucketIndex = index / DATA_TYPE_LENGTH
        val innerIndex = index % DATA_TYPE_LENGTH
        bitmap[bucketIndex] = (BITMAP_MARK.ushr(innerIndex) or bitmap[bucketIndex].toInt()).toByte()
    }

    fun getBit(index: Int): Boolean {
        if (indexOutOfBounds(index)) {
            return false
        }
        val bucketIndex = index / DATA_TYPE_LENGTH
        val innerIndex = index % DATA_TYPE_LENGTH
        return (BITMAP_MARK.ushr(innerIndex) and bitmap[bucketIndex].toInt()) != 0
    }

    fun size() = bitmap.size * DATA_TYPE_LENGTH

    fun countOnes(): Int {
        var count = 0
        for (i in bitmap.size - 1 downTo 0) {
            for (j in 0 until DATA_TYPE_LENGTH) {
                if (bitmap[i].toInt().ushr(j) and 0b1 == 1) {
                    count++
                }
            }
        }
        return count
    }

    fun toByteArray(): ByteArray {
        return bitmap.clone()
    }

    override fun iterator(): Iterator<Int> {
        return object : Iterator<Int> {
            var cursor = 0

            override fun hasNext(): Boolean {
                while (!indexOutOfBounds(cursor)) {
                    if (getBit(cursor)) {
                        return true
                    } else {
                        cursor++
                    }
                }
                return false
            }

            override fun next(): Int {
                return cursor++
            }
        }
    }
}