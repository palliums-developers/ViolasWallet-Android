package com.violas.wallet.biz.bank

import com.palliums.content.ContextProvider
import com.palliums.violas.error.ViolasException
import com.palliums.violas.smartcontract.ViolasBankContract
import com.violas.wallet.biz.transaction.ViolasTxnManager
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.getViolasChainId
import com.violas.wallet.common.isViolasTestNet
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.main.market.bean.DiemCurrencyAssetMark
import com.violas.walletconnect.extensions.hexStringToByteArray
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.transaction.AccountAddress
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTag
import org.palliums.violascore.wallet.Account

class BankManager {
    private val mViolasRPCService by lazy { DataRepository.getViolasRpcService() }
    private val mBankService by lazy { DataRepository.getBankService() }
    private val mViolasBankContract by lazy { ViolasBankContract(isViolasTestNet()) }

    /**
     * 借款
     */
    @Throws(ViolasException::class)
    suspend fun borrow(
        password: ByteArray,
        payerAccountDO: AccountDO,
        productId: String,
        assetMark: DiemCurrencyAssetMark,
        amount: Long
    ): String {
        val payerPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, payerAccountDO.privateKey)!!

        // 检查发送人账户
        val senderAccount = Account(KeyPair.fromSecretKey(payerPrivateKey))
        val violasTxnManager = ViolasTxnManager()
        val senderAccountState = violasTxnManager.getSenderAccountState(senderAccount) {
            mViolasRPCService.getAccountState(it)
        }

        // 计算gas info
        val gasInfo = violasTxnManager.calculateGasInfo(
            senderAccountState,
            null
        )

        val borrowTransactionPayload = mViolasBankContract.optionBorrowTransactionPayload(
            TypeTag.newStructTag(
                StructTag(
                    AccountAddress(assetMark.address.hexStringToByteArray()),
                    assetMark.module,
                    assetMark.name,
                    arrayListOf()
                )
            ),
            amount
        )

        val generateTransaction = mViolasRPCService.generateTransaction(
            borrowTransactionPayload,
            senderAccount,
            sequenceNumber = senderAccountState.sequenceNumber,
            gasCurrencyCode = gasInfo.gasCurrencyCode,
            maxGasAmount = gasInfo.maxGasAmount,
            gasUnitPrice = gasInfo.gasUnitPrice,
            chainId = getViolasChainId()
        )
        mBankService.submitBorrowTransaction(
            generateTransaction.payerAddress,
            productId,
            amount,
            generateTransaction.signTxn
        )
        return generateTransaction.sequenceNumber.toString()
    }

    /**
     * 还款
     */
    @Throws(ViolasException::class)
    suspend fun repayBorrow(
        password: ByteArray,
        payerAccountDO: AccountDO,
        productId: String,
        assetMark: DiemCurrencyAssetMark,
        amount: Long
    ): String {
        val payerPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, payerAccountDO.privateKey)!!

        // 检查发送人账户
        val senderAccount = Account(KeyPair.fromSecretKey(payerPrivateKey))
        val violasTxnManager = ViolasTxnManager()
        val senderAccountState = violasTxnManager.getSenderAccountState(senderAccount) {
            mViolasRPCService.getAccountState(it)
        }

        // 计算gas info
        val gasInfo = violasTxnManager.calculateGasInfo(
            senderAccountState,
            listOf(Pair(assetMark.module, amount))
        )

        val repayBorrowTransactionPayload = mViolasBankContract.optionRepayBorrowTransactionPayload(
            TypeTag.newStructTag(
                StructTag(
                    AccountAddress(assetMark.address.hexStringToByteArray()),
                    assetMark.module,
                    assetMark.name,
                    arrayListOf()
                )
            ),
            amount
        )

        val generateTransaction = mViolasRPCService.generateTransaction(
            repayBorrowTransactionPayload,
            senderAccount,
            sequenceNumber = senderAccountState.sequenceNumber,
            gasCurrencyCode = gasInfo.gasCurrencyCode,
            maxGasAmount = gasInfo.maxGasAmount,
            gasUnitPrice = gasInfo.gasUnitPrice,
            chainId = getViolasChainId()
        )
        mBankService.submitRepayBorrowTransaction(
            generateTransaction.payerAddress,
            productId,
            amount,
            generateTransaction.signTxn
        )
        return generateTransaction.sequenceNumber.toString()
    }

    /**
     * 存款
     */
    @Throws(ViolasException::class)
    suspend fun lock(
        password: ByteArray,
        payerAccountDO: AccountDO,
        productId: String,
        assetMark: DiemCurrencyAssetMark,
        amount: Long
    ): String {
        val payerPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, payerAccountDO.privateKey)!!

        // 检查发送人账户
        val senderAccount = Account(KeyPair.fromSecretKey(payerPrivateKey))
        val violasTxnManager = ViolasTxnManager()
        val senderAccountState = violasTxnManager.getSenderAccountState(senderAccount) {
            mViolasRPCService.getAccountState(it)
        }

        // 计算gas info
        val gasInfo = violasTxnManager.calculateGasInfo(
            senderAccountState,
            listOf(Pair(assetMark.module, amount))
        )

        val lockBorrowTransactionPayload = mViolasBankContract.optionLockTransactionPayload(
            TypeTag.newStructTag(
                StructTag(
                    AccountAddress(assetMark.address.hexStringToByteArray()),
                    assetMark.module,
                    assetMark.name,
                    arrayListOf()
                )
            ),
            amount
        )

        val generateTransaction = mViolasRPCService.generateTransaction(
            lockBorrowTransactionPayload,
            senderAccount,
            sequenceNumber = senderAccountState.sequenceNumber,
            gasCurrencyCode = gasInfo.gasCurrencyCode,
            maxGasAmount = gasInfo.maxGasAmount,
            gasUnitPrice = gasInfo.gasUnitPrice,
            chainId = getViolasChainId()
        )
        mBankService.submitDepositTransaction(
            generateTransaction.payerAddress,
            productId,
            amount,
            generateTransaction.signTxn
        )
        return generateTransaction.sequenceNumber.toString()
    }

    /**
     * 提款
     */
    @Throws(ViolasException::class)
    suspend fun redeem(
        password: ByteArray,
        payerAccountDO: AccountDO,
        productId: String,
        assetMark: DiemCurrencyAssetMark,
        amount: Long
    ): String {
        val payerPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, payerAccountDO.privateKey)!!

        // 检查发送人账户
        val senderAccount = Account(KeyPair.fromSecretKey(payerPrivateKey))
        val violasTxnManager = ViolasTxnManager()
        val senderAccountState = violasTxnManager.getSenderAccountState(senderAccount) {
            mViolasRPCService.getAccountState(it)
        }

        // 计算gas info
        val gasInfo = violasTxnManager.calculateGasInfo(
            senderAccountState,
            null
        )

        val redeemBorrowTransactionPayload = mViolasBankContract.optionRedeemTransactionPayload(
            TypeTag.newStructTag(
                StructTag(
                    AccountAddress(assetMark.address.hexStringToByteArray()),
                    assetMark.module,
                    assetMark.name,
                    arrayListOf()
                )
            ),
            amount
        )

        val generateTransaction = mViolasRPCService.generateTransaction(
            redeemBorrowTransactionPayload,
            senderAccount,
            sequenceNumber = senderAccountState.sequenceNumber,
            gasCurrencyCode = gasInfo.gasCurrencyCode,
            maxGasAmount = gasInfo.maxGasAmount,
            gasUnitPrice = gasInfo.gasUnitPrice,
            chainId = getViolasChainId()
        )
        mBankService.submitRedeemTransaction(
            generateTransaction.payerAddress,
            productId,
            amount,
            generateTransaction.signTxn
        )
        return generateTransaction.sequenceNumber.toString()
    }

    /**
     * 提取挖矿奖励
     */
    @Throws(ViolasException::class)
    suspend fun withdrawReward(
        privateKey: ByteArray,
    ) {
        val withdrawRewardTransactionPayload =
            mViolasBankContract.optionWithdrawRewardTransactionPayload()

        // 检查发送人账户
        val senderAccount = Account(KeyPair.fromSecretKey(privateKey))
        val violasTxnManager = ViolasTxnManager()
        val senderAccountState = violasTxnManager.getSenderAccountState(senderAccount) {
            mViolasRPCService.getAccountState(it)
        }

        // 计算gas info
        val gasInfo = violasTxnManager.calculateGasInfo(
            senderAccountState,
            null
        )

        mViolasRPCService.sendTransaction(
            withdrawRewardTransactionPayload,
            senderAccount,
            sequenceNumber = senderAccountState.sequenceNumber,
            gasCurrencyCode = gasInfo.gasCurrencyCode,
            maxGasAmount = gasInfo.maxGasAmount,
            gasUnitPrice = gasInfo.gasUnitPrice,
            chainId = getViolasChainId()
        )
    }
}