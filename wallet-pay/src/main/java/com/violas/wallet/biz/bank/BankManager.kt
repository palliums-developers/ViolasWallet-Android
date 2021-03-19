package com.violas.wallet.biz.bank

import com.palliums.content.ContextProvider
import com.palliums.violas.error.ViolasException
import com.palliums.violas.smartcontract.ViolasBankContract
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
import org.palliums.violascore.transaction.storage.TypeTagStructTag
import org.palliums.violascore.wallet.Account

class BankManager {
    private val mViolasRPCService by lazy { DataRepository.getViolasChainRpcService() }
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

        val typeTagFrom = TypeTagStructTag(
            StructTag(
                AccountAddress(assetMark.address.hexStringToByteArray()),
                assetMark.module,
                assetMark.name,
                arrayListOf()
            )
        )

        val optionBorrowTransactionPayload = mViolasBankContract.optionBorrowTransactionPayload(
            typeTagFrom,
            amount
        )

        val generateTransaction = mViolasRPCService.generateTransaction(
            optionBorrowTransactionPayload,
            Account(KeyPair.fromSecretKey(payerPrivateKey)),
            gasCurrencyCode = typeTagFrom.value.module,
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

        val typeTagFrom = TypeTagStructTag(
            StructTag(
                AccountAddress(assetMark.address.hexStringToByteArray()),
                assetMark.module,
                assetMark.name,
                arrayListOf()
            )
        )

        val optionRepayBorrowTransactionPayload =
            mViolasBankContract.optionRepayBorrowTransactionPayload(
                typeTagFrom,
                amount
            )

        val generateTransaction = mViolasRPCService.generateTransaction(
            optionRepayBorrowTransactionPayload,
            Account(KeyPair.fromSecretKey(payerPrivateKey)),
            gasCurrencyCode = typeTagFrom.value.module,
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

        val typeTagFrom = TypeTagStructTag(
            StructTag(
                AccountAddress(assetMark.address.hexStringToByteArray()),
                assetMark.module,
                assetMark.name,
                arrayListOf()
            )
        )

        val optionLockBorrowTransactionPayload =
            mViolasBankContract.optionLockTransactionPayload(
                typeTagFrom,
                amount
            )

        val generateTransaction = mViolasRPCService.generateTransaction(
            optionLockBorrowTransactionPayload,
            Account(KeyPair.fromSecretKey(payerPrivateKey)),
            gasCurrencyCode = typeTagFrom.value.module,
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

        val typeTagFrom = TypeTagStructTag(
            StructTag(
                AccountAddress(assetMark.address.hexStringToByteArray()),
                assetMark.module,
                assetMark.name,
                arrayListOf()
            )
        )

        val optionRedeemBorrowTransactionPayload =
            mViolasBankContract.optionRedeemTransactionPayload(
                typeTagFrom,
                amount
            )

        val generateTransaction = mViolasRPCService.generateTransaction(
            optionRedeemBorrowTransactionPayload,
            Account(KeyPair.fromSecretKey(payerPrivateKey)),
            gasCurrencyCode = typeTagFrom.value.module,
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

        mViolasRPCService.sendTransaction(
            payload = withdrawRewardTransactionPayload,
            payerAccount = Account(KeyPair.fromSecretKey(privateKey)),
            chainId = getViolasChainId()
        )
    }
}