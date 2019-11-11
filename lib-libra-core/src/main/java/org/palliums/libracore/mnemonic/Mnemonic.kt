package org.palliums.libracore.mnemonic;

import org.palliums.libracore.utils.ByteUtility
import org.spongycastle.jcajce.provider.digest.SHA256
import org.spongycastle.util.Strings
import java.security.SecureRandom
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.experimental.and
import kotlin.experimental.xor


class Mnemonic(private val wordList: WordList) {
    companion object {
        fun English(): Mnemonic {
            return Mnemonic(English.INSTANCE)
        }
    }

    private val words: Array<WordAndIndex?>

    private val normalizer: NFKDNormalizer
    private val map = HashMap<CharSequence, CharArray>()
    private val random: Random = SecureRandom()

    init {
        normalizer = WordListMapNormalization(wordList)
        for (i in 0 until (1 shl 11)) {
            val word = normalizer.normalize(wordList.getWord(i))
            map[word] = word.toCharArray()
        }

        words = arrayOfNulls(1 shl 11)
        for (i in 0 until (1 shl 11)) {
            words[i] = WordAndIndex(i, wordList.getWord(i))
        }
    }

    fun toByteArray(mnemonic: List<String>): ByteArray? {
        return toCharArray(mnemonic)?.let { Strings.toUTF8ByteArray(it) }
    }

    fun toCharArray(mnemonics: List<String>): CharArray? {
        val words = mnemonics.size
        val chars = arrayOfNulls<CharArray>(words)
        val toClear = LinkedList<CharArray>()
        var count = 0
        for ((wordIndex, word) in mnemonics.withIndex()) {
            var wordChars = map[normalizer.normalize(word)]
            if (wordChars == null) {
                wordChars = normalizer.normalize(word).toCharArray()
                toClear.add(wordChars)
            }
            chars[wordIndex] = wordChars
            count += wordChars.size
        }
        count += words - 1
        try {
            val mnemonicChars = CharArray(count)
            var index = 0
            for (i in chars.indices) {
                chars[i]?.let {
                    System.arraycopy(it, 0, mnemonicChars, index, it.size)
                    index += it.size
                    if (i < chars.size - 1) {
                        mnemonicChars[index++] = ' '
                    }
                }
            }
            return mnemonicChars
        } finally {
            Arrays.fill(chars, null)
            for (charsToClear in toClear)
                Arrays.fill(charsToClear, '\u0000')
        }
    }

    fun generate(words: WordCount = WordCount.TWELVE): ArrayList<String> {
        val randomSeed = randomSeed(words)
        val wordIndexes = wordIndexes(randomSeed)

        val mnemonicList = ArrayList<String>()
        for (i in wordIndexes.indices) {
            mnemonicList.add(wordList.getWord(wordIndexes[i]))
        }
        return mnemonicList
    }

    private fun randomSeed(words: WordCount): ByteArray {
        val randomSeed = ByteArray(words.byteLength())
        random.nextBytes(randomSeed)
        return randomSeed
    }

    private fun wordIndexes(entropy: ByteArray): IntArray {
        val ent = entropy.size * 8

        val entropyWithChecksum = entropy.copyOf(entropy.size + 1)
        entropyWithChecksum[entropy.size] = firstByteOfSha256(entropy)

        //checksum length
        val cs = ent / 32
        //mnemonic length
        val ms = (ent + cs) / 11

        //get the indexes into the word list
        val wordIndexes = IntArray(ms)
        var i = 0
        var wi = 0
        while (wi < ms) {
            wordIndexes[wi] = ByteUtility.next11Bits(entropyWithChecksum, i)
            i += 11
            wi++
        }
        return wordIndexes
    }

    private fun findWordIndexes(split: Collection<CharSequence>): IntArray {
        val ms = split.size
        val result = IntArray(ms)
        var i = 0
        for (buffer in split) {
            result[i++] = findWordIndex(buffer) ?: 0
        }
        return result
    }

    private fun findWordIndex(buffer: CharSequence): Int? {
        val key = WordAndIndex(-1, buffer)
        val index = Arrays.binarySearch<WordAndIndex>(words, key, wordListSortOrder)
        if (index < 0) {
            val insertionPoint = -index - 1
            var suggestion = if (insertionPoint == 0) insertionPoint else insertionPoint - 1
            if (suggestion + 1 == words.size) suggestion--
        }
        return words[index]?.index
    }

    private fun firstByteOfSha256(
        entropy: ByteArray,
        offset: Int = 0,
        size: Int = entropy.size
    ): Byte {
        val shA256 = SHA256.Digest()
        shA256.update(entropy, offset, size)
        val hash = shA256.digest()
        val firstByte = hash[0]
        Arrays.fill(hash, 0.toByte())
        return firstByte
    }

    fun validation(mnemonics: List<String>): Boolean {
        val wordIndexes = findWordIndexes(mnemonics)
        val ms = wordIndexes.size

        val entPlusCs = ms * 11
        val ent = entPlusCs * 32 / 33
        val cs = ent / 32
        if (entPlusCs != ent + cs)
            return false

        val entropyWithChecksum = ByteArray((entPlusCs + 7) / 8)

        wordIndexesToEntropyWithCheckSum(wordIndexes, entropyWithChecksum)
        Arrays.fill(wordIndexes, 0)

        val lastByte = entropyWithChecksum[entropyWithChecksum.size - 1]
        val sha = firstByteOfSha256(entropyWithChecksum, 0, entropyWithChecksum.size - 1)
        Arrays.fill(entropyWithChecksum, 0.toByte())

        val mask = maskOfFirstNBits(cs)

        val result = sha xor lastByte and mask
        if (result != 0.toByte())
            return false
        return true
    }

    private fun wordIndexesToEntropyWithCheckSum(
        wordIndexes: IntArray,
        entropyWithChecksum: ByteArray
    ) {
        var i = 0
        var bi = 0
        while (i < wordIndexes.size) {
            ByteUtility.writeNext11(entropyWithChecksum, wordIndexes[i], bi)
            i++
            bi += 11
        }
    }

    private fun maskOfFirstNBits(n: Int): Byte {
        return ((1 shl (8 - n)) - 1).inv().toByte()
    }

    private val wordListSortOrder =
        Comparator<WordAndIndex> { o1, o2 ->
            CharSequenceComparators.ALPHABETICAL.compare(
                o1.normalized,
                o2.normalized
            )
        }

    private inner class WordAndIndex internal constructor(
        internal val index: Int,
        internal val word: CharSequence
    ) {
        internal val normalized: String

        init {
            normalized = normalizer.normalize(word)
        }
    }
}
