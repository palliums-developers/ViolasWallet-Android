package org.palliums.libracore.wallet;

import java.nio.charset.Charset

const val MNEMONIC_SALT_DEFAULT = "LIBRA"
const val DERIVED_KEY = "LIBRA WALLET: derived key$"
const val LibraHashSuffix = "@@$\$LIBRA$$@@"
const val MASTER_KEY_SALT = "LIBRA WALLET: main key salt$"
const val MNEMONIC_SALT_PREFIX = "LIBRA WALLET: mnemonic salt prefix$"

//const val RAW_TRANSACTION_HASH_SALT = "libra_types::transaction::RawTransaction$LibraHashSuffix"
const val LIBRA_HASH_PREFIX = "LIBRA::"
const val RAW_TRANSACTION_HASH_SALT = "${LIBRA_HASH_PREFIX}RawTransaction"