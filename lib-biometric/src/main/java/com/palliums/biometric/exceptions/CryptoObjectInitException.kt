package com.palliums.biometric.exceptions


/**
 * Created by elephant on 2020/5/19 15:52.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Thrown when value CryptoObject initialization fails.
 * Usually because {@link android.security.keystore.KeyPermanentlyInvalidatedException} is thrown.
 *
 * Also be aware of https://issuetracker.google.com/issues/65578763
 */
class CryptoObjectInitException : Exception("CryptoObject failed to create.")