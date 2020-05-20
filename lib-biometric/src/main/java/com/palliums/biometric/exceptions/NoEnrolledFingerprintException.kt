package com.palliums.biometric.exceptions

/**
 * Created by elephant on 2020/5/19 15:49.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Thrown if the user has no enrolled fingerprints.
 */
class NoEnrolledFingerprintException : Exception("User has no enrolled fingerprint.")