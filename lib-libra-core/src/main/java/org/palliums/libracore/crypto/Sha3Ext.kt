package org.palliums.libracore.crypto

import org.spongycastle.jcajce.provider.digest.SHA3

fun ByteArray.sha3(): ByteArray {
    val sha3256 = SHA3.Digest256()
    sha3256.update(this)
    return sha3256.digest()
}