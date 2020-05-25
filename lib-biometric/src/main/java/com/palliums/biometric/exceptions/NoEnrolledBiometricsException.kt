package com.palliums.biometric.exceptions

/**
 * Created by elephant on 2020/5/19 15:49.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 *
 * Thrown if the user has no enrolled biometrics.
 */
class NoEnrolledBiometricsException : Exception("The user does not have any biometrics enrolled.")