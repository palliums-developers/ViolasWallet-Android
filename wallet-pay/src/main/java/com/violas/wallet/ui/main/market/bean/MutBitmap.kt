import org.palliums.libracore.crypto.Bitmap

/**
 * 可变的 Bitmap 可以记录状态
 */
class MutBitmap() : Iterable<Boolean> {
    companion object {
        private const val BITMAP_NUM_OF_BYTES = 4
        private const val DATA_TYPE_LENGTH = 8
        private const val BITMAP_MARK = 128
    }

    private var bitmap: ByteArray = ByteArray(Bitmap.BITMAP_NUM_OF_BYTES) { 0.toByte() }

    /**
     * 对 Bitmap 数组进行扩容
     * @param index 想要访问的索引下标
     */
    private fun capacity(index: Int) {
        while (indexOutOfBounds(index)) {
            capacity()
        }
    }

    private fun capacity() {
        val newBitmap = ByteArray(bitmap.size * 2)
        System.arraycopy(bitmap, bitmap.size, newBitmap, 0, newBitmap.size)
        bitmap = newBitmap
    }

    /**
     * 检查要访问的 index 是否超出
     */
    private fun indexOutOfBounds(index: Int): Boolean {
        return index > size()
    }

    fun setBit(index: Int) {
        val bucketIndex = index / DATA_TYPE_LENGTH
        capacity(bucketIndex)
        val innerIndex = index % DATA_TYPE_LENGTH
        bitmap[bucketIndex] = (BITMAP_MARK.ushr(innerIndex) or bitmap[bucketIndex].toInt()).toByte()
    }

    fun getBit(index: Int): Boolean {
        val bucketIndex = index / DATA_TYPE_LENGTH
        if (indexOutOfBounds(bucketIndex)) {
            return false
        }
        val innerIndex = index % DATA_TYPE_LENGTH
        return (BITMAP_MARK.ushr(innerIndex) and bitmap[bucketIndex].toInt()) != 0
    }

    fun size() = bitmap.size * DATA_TYPE_LENGTH

    fun countOnes(): Int {
        var count = 0
        for (i in BITMAP_NUM_OF_BYTES - 1 downTo 0) {
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

    override fun iterator(): Iterator<Boolean> {
        return object : Iterator<Boolean> {
            var cursor = 0
            val countMax = size()

            override fun hasNext(): Boolean {
                return cursor < countMax
            }

            override fun next(): Boolean {
                val tmp = getBit(cursor)
                cursor++
                return tmp
            }
        }
    }
}