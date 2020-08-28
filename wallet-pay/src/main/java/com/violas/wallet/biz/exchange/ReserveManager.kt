package com.violas.wallet.biz.exchange

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.*
import com.palliums.utils.CustomIOScope
import com.palliums.utils.exceptionAsync
import com.palliums.violas.http.MapRelationDTO
import com.palliums.violas.http.PoolLiquidityReserveInfoDTO
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.BuildConfig
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.utils.str2CoinNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min

open class ExchangeError : RuntimeException() {
    class UnsupportedCurrenciesException : ExchangeError()
    class ExchangePairNotFindException : RuntimeException()
}


class ReserveManager : LifecycleObserver, CoroutineScope by CustomIOScope(), Handler.Callback {
    companion object {
        const val HANDLER_COMMAND = 1
    }

    private val mRefreshInterval = 10 * 1000L

    private val mReserveList = mutableListOf<PoolLiquidityReserveInfoDTO>()
    private val mMappingRealMap = hashMapOf<String, MapRelationDTO>()
    private var isRun = false

    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    private val mHandler = Handler(Looper.getMainLooper(), this)
    val mChangeLiveData = MutableLiveData<Int>()

    fun init(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(this)
    }

    data class Trade(
        val path: MutableList<Int>,
        var amount: Long
    )

    data class ReservesAmount(
        val reserveIn: Long,
        val reserveOut: Long
    )

    private fun getReserve(
        reservePair: List<PoolLiquidityReserveInfoDTO>,
        indexA: Int,
        indexB: Int
    ): ReservesAmount {
        val minIndex = min(indexA, indexB)
        val maxIndex = max(indexA, indexB)
        reservePair.forEach {
            if (it.coinA.marketIndex == minIndex && it.coinB.marketIndex == maxIndex) {
                return if (indexA > indexB) {
                    ReservesAmount(it.coinB.amount.toLong(), it.coinA.amount.toLong())
                } else {
                    ReservesAmount(it.coinA.amount.toLong(), it.coinB.amount.toLong())
                }
            }
        }
        return ReservesAmount(0, 0)
    }

    private fun getInputAmount(amountOut: Long, reserveIn: Long, reserveOut: Long): Long {
        if (amountOut <= 0 || reserveIn <= 0 || reserveOut <= 0) {
            return 0
        }
        val numerator = BigInteger.valueOf(reserveIn).multiply(BigInteger.valueOf(amountOut))
        val denominator = BigInteger.valueOf(reserveOut).subtract(BigInteger.valueOf(amountOut))
        return numerator.divide(denominator).add(BigInteger.valueOf(1)).toLong()
    }

    private fun getOutputAmount(amountIn: Long, reserveIn: Long, reserveOut: Long): Long {
        if (amountIn <= 0 || reserveIn <= 0 || reserveOut <= 0) {
            return 0
        }
        val num1 = BigInteger.valueOf(amountIn).multiply(BigInteger.valueOf(reserveOut))
        val num2 = BigInteger.valueOf(reserveIn).add(BigInteger.valueOf(amountIn))
        return num1.divide(num2).toLong()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun startWork() {
        isRun = true
        mHandler.removeMessages(HANDLER_COMMAND)
        mHandler.sendEmptyMessageDelayed(HANDLER_COMMAND, 1500)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun pauseWork() {
        isRun = false
        mHandler.removeMessages(HANDLER_COMMAND)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun stopWork() {
        isRun = false
        mHandler.removeMessages(HANDLER_COMMAND)
    }

    private fun mappingKey(iTokenVo: ITokenVo): String {
        return when (iTokenVo.coinNumber) {
            CoinTypes.Bitcoin.coinType(),
            CoinTypes.BitcoinTest.coinType() -> {
                "${iTokenVo.coinNumber}"
            }
            CoinTypes.Violas.coinType(),
            CoinTypes.Libra.coinType() -> {
                iTokenVo as StableTokenVo
                "${iTokenVo.coinNumber}${iTokenVo.module}"
            }
            else -> {
                iTokenVo as StableTokenVo
                "${iTokenVo.coinNumber}${iTokenVo.module}"
            }
        }
    }

    private fun mappingKey(mappingReal: MapRelationDTO): String {
        return when (str2CoinNumber(mappingReal.chain)) {
            CoinTypes.Bitcoin.coinType(),
            CoinTypes.BitcoinTest.coinType() -> {
                "${str2CoinNumber(mappingReal.chain)}"
            }
            CoinTypes.Violas.coinType(),
            CoinTypes.Libra.coinType() -> {
                "${str2CoinNumber(mappingReal.chain)}${mappingReal.mapName}"
            }
            else -> {
                "${str2CoinNumber(mappingReal.chain)}${mappingReal.mapName}"
            }
        }
    }

    private fun refreshReserve() {
        launch {
            val marketAllReservePairDeferred =
                exceptionAsync { mViolasService.getMarketAllReservePair() }

            if (mMappingRealMap.isEmpty()) {
                val marketMappingRealCoinDeferred =
                    exceptionAsync { mViolasService.getMarketPairRelation() }
                val marketMappingRealCoin = marketMappingRealCoinDeferred.await()
                marketMappingRealCoin?.forEach { mappingReal ->
                    mMappingRealMap[mappingKey(mappingReal)] =
                        mappingReal
                }
            }

            val marketAllReservePair = marketAllReservePairDeferred.await()
            if (marketAllReservePair != null) {
                mReserveList.clear()
                mReserveList.addAll(marketAllReservePair)
            }
            mChangeLiveData.postValue(Random().nextInt())
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        if (msg.what == HANDLER_COMMAND) {
            if (isRun) {
                refreshReserve()
                mHandler.sendEmptyMessageDelayed(HANDLER_COMMAND, mRefreshInterval)
            }
        }
        return true
    }

    private fun getOutputAmountWithFee(amountIn: Long, reserveIn: Long, reserveOut: Long): Long {
        val amountInWithFee = BigInteger.valueOf(amountIn).multiply(BigInteger.valueOf(9997))
        val numerator = amountInWithFee.multiply(BigInteger.valueOf(reserveOut))
        val denominator = BigInteger.valueOf(reserveIn).multiply(BigInteger.valueOf(10000))
            .add(amountInWithFee)
        val amountOut = numerator.divide(denominator)
        return amountOut.toLong()
    }

    private fun getOutputAmountsWithFee(
        amountIn: Long,
        path: MutableList<Int>
    ): MutableList<Long> {
        val amounts = mutableListOf<Long>()
        amounts.add(amountIn)
        for (i in 0 until (path.size - 1)) {
            val (reserveIn, reserveOut) = getReserve(mReserveList, path[i], path[i + 1])
            val amountOut = getOutputAmountWithFee(amounts[i], reserveIn, reserveOut)
            amounts.add(amountOut)
        }
        return amounts
    }

    private fun getMarkIndex(iTokenVo: ITokenVo): Int? {
        return if (iTokenVo is StableTokenVo) {
            if (iTokenVo.coinNumber == CoinTypes.Libra.coinType()) {
                mMappingRealMap[mappingKey(iTokenVo)]?.index
            } else {
                iTokenVo.marketIndex
            }
        } else {
            mMappingRealMap[mappingKey(iTokenVo)]?.index
        }
    }

    data class TradeResult(
        val path: List<Int>,
        val amount: Long,
        val fee: Long = 0
    )

    fun tradeExact(
        fromToken: ITokenVo,
        toToken: ITokenVo,
        inputAmount: Long,
        isInputFrom: Boolean
    ): TradeResult? {
        val inIndex =
            getMarkIndex(fromToken) ?: throw ExchangeError.UnsupportedCurrenciesException()
        val outIndex = getMarkIndex(toToken) ?: throw ExchangeError.UnsupportedCurrenciesException()
        val trades = if (isInputFrom) {
            bestTradeExactIn(mReserveList, inIndex, outIndex, inputAmount)
        } else {
            bestTradeExactOut(mReserveList, inIndex, outIndex, inputAmount)
        }

        val tradeResult = trades?.first()?.let {
            if (isInputFrom) {
                // 由输入价格直接计算输出手续费
                val amount = getOutputAmountsWithFee(inputAmount, it.path).last()
                val fee = it.amount - amount
                TradeResult(it.path, amount, fee)
            } else {
                // 由输出价格计算手续费
                val fee = inputAmount - getOutputAmountsWithFee(it.amount, it.path).last()
                TradeResult(it.path, it.amount, fee)
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d("ReserveManager", "route planning start")
            trades?.forEach {
                Log.d("ReserveManager", it.toString())
            }
            Log.d("ReserveManager", "route planning end")
        }

        return tradeResult
    }

    /**
     * 根据输出金额算输入金额
     */
    private fun bestTradeExactOut(
        reservePair: List<PoolLiquidityReserveInfoDTO>,
        inIndex: Int,
        outIndex: Int,
        amountOut: Long,
        originalAmountOut: Long = amountOut,
        path: MutableList<Int> = mutableListOf(),
        bestTrades: MutableList<Trade> = mutableListOf()
    ): List<Trade>? {
        if (BuildConfig.DEBUG && reservePair.size <= 0) {
            error("Assertion failed")
        }
        if (BuildConfig.DEBUG && !(originalAmountOut == amountOut || path.size > 0)) {
            error("Assertion failed")
        }
        if (path.size == 0) {
            path.add(outIndex)
        }
        if (path.size > 3) {
            return null
        }
        val startPath = ArrayList(path)
        for (i in reservePair.indices) {
            val pair = reservePair[i]
            val tmpIn = if (outIndex == pair.coinA.marketIndex) {
                pair.coinB.marketIndex
            } else if (outIndex == pair.coinB.marketIndex) {
                pair.coinA.marketIndex
            } else {
                continue
            }
            if (path.contains(tmpIn)) {
                continue
            }
            val (reserveIn, reserveOut) = getReserve(reservePair, tmpIn, outIndex)
            if (reserveIn == 0L || reserveOut == 0L) {
                continue
            }

            val amountIn = getInputAmount(amountOut, reserveIn, reserveOut)

            if (amountIn <= 0) {
                continue
            }

            if (inIndex == pair.coinA.marketIndex || inIndex == pair.coinB.marketIndex) {
                path.add(0, inIndex)
                bestTrades.add(Trade(ArrayList(path), amountIn))
                path.clear()
                path.addAll(startPath)
            } else if (reservePair.size > 1) {
                val newReservePair = ArrayList(reservePair)
                newReservePair.removeAt(i)
                val newPath = ArrayList(path)
                newPath.add(0, tmpIn)
                bestTradeExactOut(
                    newReservePair,
                    inIndex,
                    tmpIn,
                    amountIn,
                    originalAmountOut,
                    newPath,
                    bestTrades
                )
            }
        }
        return bestTrades.sortedBy { it.amount }
    }

    /**
     * 根据输入金额算输出金额
     */
    private fun bestTradeExactIn(
        reservePair: List<PoolLiquidityReserveInfoDTO>,
        inIndex: Int,
        outIndex: Int,
        amountIn: Long,
        originalAmountIn: Long = amountIn,
        path: MutableList<Int> = mutableListOf(),
        bestTrades: MutableList<Trade> = mutableListOf()
    ): List<Trade>? {
        if (BuildConfig.DEBUG && reservePair.size <= 0) {
            error("Assertion failed")
        }
        if (BuildConfig.DEBUG && !(amountIn == originalAmountIn || path.size > 0)) {
            error("Assertion failed")
        }
        if (path.isEmpty()) {
            path.add(inIndex)
        }
        if (path.size > 3) {
            return null
        }
        val startPath = ArrayList(path)
        for (i in reservePair.indices) {
            val pair = reservePair[i]
            val tmpOut = if (inIndex == pair.coinA.marketIndex) {
                pair.coinB.marketIndex
            } else if (inIndex == pair.coinB.marketIndex) {
                pair.coinA.marketIndex
            } else {
                continue
            }
            if (path.contains(tmpOut)) {
                continue
            }
            val (reserveIn, reserveOut) = getReserve(reservePair, inIndex, tmpOut)
            if (reserveIn == 0L || reserveOut == 0L) {
                continue
            }
            val amountOut = getOutputAmount(amountIn, reserveIn, reserveOut)

            if (amountOut <= 0) {
                continue
            }

            if (outIndex == pair.coinA.marketIndex || outIndex == pair.coinB.marketIndex) {
                path.add(outIndex)
                bestTrades.add(Trade(ArrayList(path), amountOut))
                path.clear()
                path.addAll(startPath)
            } else if (reservePair.size > 1) {
                val newReservePair = ArrayList(reservePair)
                newReservePair.removeAt(i)
                val newPath = ArrayList(path)
                newPath.add(tmpOut)
                bestTradeExactIn(
                    newReservePair,
                    tmpOut,
                    outIndex,
                    amountOut,
                    originalAmountIn,
                    newPath,
                    bestTrades
                )
            }
        }
        return bestTrades.sortedByDescending { it.amount }
    }
}