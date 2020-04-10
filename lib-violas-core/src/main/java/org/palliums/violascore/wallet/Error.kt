package org.palliums.violascore.wallet

import java.lang.RuntimeException

class CryptoMaterialError {
    class ValidationError : RuntimeException()
    class WrongLengthError : RuntimeException()
    class BitVecError(msg: String) : RuntimeException(msg)
}