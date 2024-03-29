package com.quincysx.crypto.bip39;

import com.quincysx.crypto.bip39.validation.InvalidChecksumException;
import com.quincysx.crypto.bip39.validation.InvalidWordCountException;
import com.quincysx.crypto.bip39.validation.UnexpectedWhiteSpaceException;
import com.quincysx.crypto.bip39.validation.WordNotFoundException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

/**
 * @author QuincySx
 * @date 2018/5/15 上午11:14
 */
public final class MnemonicValidator {
    private final WordAndIndex[] words;
    private final CharSequenceSplitter charSequenceSplitter;
    private final NFKDNormalizer normalizer;

    private MnemonicValidator(final WordList wordList) {
        normalizer = new WordListMapNormalization(wordList);
        words = new WordAndIndex[1 << 11];
        for (int i = 0; i < 1 << 11; i++) {
            words[i] = new WordAndIndex(i, wordList.getWord(i));
        }
        charSequenceSplitter = new CharSequenceSplitter(wordList.getSpace(), Normalization.normalizeNFKD(wordList.getSpace()));
        Arrays.sort(words, wordListSortOrder);
    }

    /**
     * Get a Mnemonic validator for the given word list.
     * No normalization is currently performed, this is an open issue: https://github.com/NovaCrypto/BIP39/issues/13
     *
     * @param wordList A WordList implementation
     * @return A validator
     */
    public static MnemonicValidator ofWordList(final WordList wordList) {
        return new MnemonicValidator(wordList);
    }

    /**
     * Check that the supplied mnemonic fits the BIP0039 spec.
     *
     * @param mnemonic The memorable list of words
     * @throws InvalidChecksumException      If the last bytes don't match the expected last bytes
     * @throws InvalidWordCountException     If the number of words is not a multiple of 3, 24 or fewer
     * @throws WordNotFoundException         If a word in the mnemonic is not present in the word list
     * @throws UnexpectedWhiteSpaceException Occurs if one of the supplied words is empty, e.g. a double space
     */
    public void validate(final CharSequence mnemonic) throws
            InvalidChecksumException,
            InvalidWordCountException,
            WordNotFoundException,
            UnexpectedWhiteSpaceException {
        validate(charSequenceSplitter.split(mnemonic));
    }

    /**
     * Check that the supplied mnemonic fits the BIP0039 spec.
     * <p>
     * The purpose of this method overload is to avoid constructing a mnemonic String if you have gathered a list of
     * words from the user.
     *
     * @param mnemonic The memorable list of words
     * @throws InvalidChecksumException      If the last bytes don't match the expected last bytes
     * @throws InvalidWordCountException     If the number of words is not a multiple of 3, 24 or fewer
     * @throws WordNotFoundException         If a word in the mnemonic is not present in the word list
     * @throws UnexpectedWhiteSpaceException Occurs if one of the supplied words is empty
     */
    public void validate(final Collection<? extends CharSequence> mnemonic) throws
            InvalidChecksumException,
            InvalidWordCountException,
            WordNotFoundException,
            UnexpectedWhiteSpaceException {
        final int[] wordIndexes = findWordIndexes(mnemonic);
        try {
            validate(wordIndexes);
        } finally {
            Arrays.fill(wordIndexes, 0);
        }
    }

    private static void validate(final int[] wordIndexes) throws
            InvalidWordCountException,
            InvalidChecksumException {
        final int ms = wordIndexes.length;

        final int entPlusCs = ms * 11;
        final int ent = (entPlusCs * 32) / 33;
        final int cs = ent / 32;
        if (entPlusCs != ent + cs)
            throw new InvalidWordCountException();
//        final byte[] entropyWithChecksum = new byte[(entPlusCs + 7) / 8];
//
//        wordIndexesToEntropyWithCheckSum(wordIndexes, entropyWithChecksum);
//        Arrays.fill(wordIndexes, 0);
//
//        final byte[] entropy = Arrays.copyOf(entropyWithChecksum, entropyWithChecksum.length - 1);
//        final byte lastByte = entropyWithChecksum[entropyWithChecksum.length - 1];
//        Arrays.fill(entropyWithChecksum, (byte) 0);

//        final byte sha = MnemonicGenerator.firstByteOfSha256(entropy);
//
//        final byte mask = maskOfFirstNBits(cs);
//
//        if (((sha ^ lastByte) & mask) != 0)
//            throw new InvalidChecksumException();
    }

    private int[] findWordIndexes(final Collection<? extends CharSequence> split) throws
            UnexpectedWhiteSpaceException,
            WordNotFoundException {
        final int ms = split.size();
        final int[] result = new int[ms];
        int i = 0;
        for (final CharSequence buffer : split) {
            if (buffer.length() == 0) {
                throw new UnexpectedWhiteSpaceException();
            }
            result[i++] = findWordIndex(buffer);
        }
        return result;
    }

    private int findWordIndex(final CharSequence buffer) throws WordNotFoundException {
        final WordAndIndex key = new WordAndIndex(-1, buffer);
        final int index = Arrays.binarySearch(words, key, wordListSortOrder);
        if (index < 0) {
            final int insertionPoint = -index - 1;
            int suggestion = insertionPoint == 0 ? insertionPoint : insertionPoint - 1;
            if (suggestion + 1 == words.length) suggestion--;
            throw new WordNotFoundException(buffer, words[suggestion].word, words[suggestion + 1].word);
        }
        return words[index].index;
    }

    private static void wordIndexesToEntropyWithCheckSum(final int[] wordIndexes, final byte[] entropyWithChecksum) {
        for (int i = 0, bi = 0; i < wordIndexes.length; i++, bi += 11) {
            ByteUtils.writeNext11(entropyWithChecksum, wordIndexes[i], bi);
        }
    }

    private static byte maskOfFirstNBits(final int n) {
        return (byte) ~((1 << (8 - n)) - 1);
    }

    private static final Comparator<WordAndIndex> wordListSortOrder = new Comparator<WordAndIndex>() {
        @Override
        public int compare(final WordAndIndex o1, final WordAndIndex o2) {
            return CharSequenceComparators.ALPHABETICAL.compare(o1.normalized, o2.normalized);
        }
    };

    private class WordAndIndex {
        final CharSequence word;
        final String normalized;
        final int index;

        WordAndIndex(final int i, final CharSequence word) {
            this.word = word;
            normalized = normalizer.normalize(word);
            index = i;
        }
    }
}
