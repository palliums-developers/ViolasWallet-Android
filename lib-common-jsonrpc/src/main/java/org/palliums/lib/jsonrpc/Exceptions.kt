package org.palliums.lib.jsonrpc

import java.io.IOException

class ResponseExceptions(val code: Int, val msg: String) : IOException()
class RequestException(val e: Exception) : IOException()