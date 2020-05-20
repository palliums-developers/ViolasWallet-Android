package com.palliums.biometric.crypto

import javax.crypto.Cipher

/**
 * Created by elephant on 2020/5/19 16:12.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * @see Crypter
 * @see com.palliums.biometric.crypto.impl.Base64CipherCrypter
 */
interface CipherCrypter : Crypter<Cipher>