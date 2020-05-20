package com.palliums.biometric.crypto

import javax.crypto.Cipher

/**
 * Created by elephant on 2020/5/19 16:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * @see Factory
 * @see com.palliums.biometric.crypto.impl.AesCipherFactory
 * @see com.palliums.biometric.crypto.impl.UnlockedAesCipherFactory
 */
interface CipherFactory : Factory<Cipher>