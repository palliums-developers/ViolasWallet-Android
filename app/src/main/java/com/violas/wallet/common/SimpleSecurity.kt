package com.violas.wallet.common

import android.content.Context
import com.smallraw.core.crypto.SecurityEngine
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex
import java.security.SecureRandom

class SimpleSecurity private constructor(context: Context) :
    SecurityEngine(object : SecurityEngine.StorageUniqueKey {
        override fun obtainUniqueKey(): ByteArray {
            val sharedPreferences = context.getSharedPreferences("config", Context.MODE_PRIVATE)
            val uniqueCode = sharedPreferences.getString("SecurityUniqueCode", "")
            if (null == uniqueCode || uniqueCode.isEmpty()) {
                val uniqueBytes = ByteArray(64)
                SecureRandom.getInstance("SHA1PRNG").nextBytes(uniqueBytes)
                sharedPreferences.edit().putString("SecurityUniqueCode", uniqueBytes.toHex())
                    .apply()
                return uniqueBytes
            } else {
                return uniqueCode.hexToBytes()
            }
        }
    }) {

    companion object {
        @Volatile
        private var mSimpleSecurity: SimpleSecurity? = null

        fun instance(context: Context): SimpleSecurity {
            return mSimpleSecurity ?: synchronized(this) {
                mSimpleSecurity ?: SimpleSecurity(context).also {
                    mSimpleSecurity = it
                }
            }
        }
    }
}

