package com.palliums.biometric.exceptions

/**
 * Created by elephant on 2020/5/20 15:36.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class InvalidParametersException(errors: List<String>) : Exception(errors.joinToString(" "))