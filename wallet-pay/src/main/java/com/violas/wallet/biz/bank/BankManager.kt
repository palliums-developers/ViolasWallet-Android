package com.violas.wallet.biz.bank

import com.palliums.content.ContextProvider
import com.palliums.violas.error.ViolasException
import com.palliums.violas.smartcontract.ViolasBankContract
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.ui.main.market.bean.LibraTokenAssetsMark
import com.violas.walletconnect.extensions.hexStringToByteArray
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.transaction.AccountAddress
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.optionTransactionPayload
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTagStructTag
import org.palliums.violascore.wallet.Account

class BankManager {
    private val mViolasService by lazy { DataRepository.getViolasService() }
    private val mViolasBankContract by lazy { ViolasBankContract(Vm.TestNet) }

    @Throws(ViolasException::class)
    suspend fun borrow(
        password: ByteArray,
        payerAccountDO: AccountDO,
        assetsMark: LibraTokenAssetsMark,
        amount: Long
    ): String {
        val payerPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, payerAccountDO.privateKey)!!

        val typeTagFrom = TypeTagStructTag(
            StructTag(
                AccountAddress(assetsMark.address.hexStringToByteArray()),
                assetsMark.module,
                assetsMark.name,
                arrayListOf()
            )
        )

        val optionBorrowTransactionPayload = mViolasBankContract.optionBorrowTransactionPayload(
            typeTagFrom,
            amount
        )

        return mViolasService.sendTransaction(
            optionBorrowTransactionPayload,
            Account(KeyPair.fromSecretKey(payerPrivateKey)),
            gasCurrencyCode = typeTagFrom.value.module,
            chainId = Vm.ViolasChainId
        ).sequenceNumber.toString()
    }

    @Throws(ViolasException::class)
    suspend fun repayBorrow(
        password: ByteArray,
        payerAccountDO: AccountDO,
        assetsMark: LibraTokenAssetsMark,
        amount: Long
    ): String {
        val payerPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, payerAccountDO.privateKey)!!

        val typeTagFrom = TypeTagStructTag(
            StructTag(
                AccountAddress(assetsMark.address.hexStringToByteArray()),
                assetsMark.module,
                assetsMark.name,
                arrayListOf()
            )
        )

        val optionRepayBorrowTransactionPayload =
            mViolasBankContract.optionRepayBorrowTransactionPayload(
                typeTagFrom,
                amount
            )

        return mViolasService.sendTransaction(
            optionRepayBorrowTransactionPayload,
            Account(KeyPair.fromSecretKey(payerPrivateKey)),
            gasCurrencyCode = typeTagFrom.value.module,
            chainId = Vm.ViolasChainId
        ).sequenceNumber.toString()
    }

    @Throws(ViolasException::class)
    suspend fun lock(
        password: ByteArray,
        payerAccountDO: AccountDO,
        assetsMark: LibraTokenAssetsMark,
        amount: Long
    ): String {
        val payerPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, payerAccountDO.privateKey)!!

        val typeTagFrom = TypeTagStructTag(
            StructTag(
                AccountAddress(assetsMark.address.hexStringToByteArray()),
                assetsMark.module,
                assetsMark.name,
                arrayListOf()
            )
        )

        val optionLockBorrowTransactionPayload =
            mViolasBankContract.optionLockTransactionPayload(
                typeTagFrom,
                amount
            )

        return mViolasService.sendTransaction(
            optionLockBorrowTransactionPayload,
            Account(KeyPair.fromSecretKey(payerPrivateKey)),
            gasCurrencyCode = typeTagFrom.value.module,
            chainId = Vm.ViolasChainId
        ).sequenceNumber.toString()
    }

    @Throws(ViolasException::class)
    suspend fun redeem(
        password: ByteArray,
        payerAccountDO: AccountDO,
        assetsMark: LibraTokenAssetsMark,
        amount: Long
    ): String {
        val payerPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, payerAccountDO.privateKey)!!

        val typeTagFrom = TypeTagStructTag(
            StructTag(
                AccountAddress(assetsMark.address.hexStringToByteArray()),
                assetsMark.module,
                assetsMark.name,
                arrayListOf()
            )
        )

        val optionRedeemBorrowTransactionPayload =
            mViolasBankContract.optionRedeemTransactionPayload(
                typeTagFrom,
                amount
            )

        return mViolasService.sendTransaction(
            optionRedeemBorrowTransactionPayload,
            Account(KeyPair.fromSecretKey(payerPrivateKey)),
            gasCurrencyCode = typeTagFrom.value.module,
            chainId = Vm.ViolasChainId
        ).sequenceNumber.toString()
    }
}