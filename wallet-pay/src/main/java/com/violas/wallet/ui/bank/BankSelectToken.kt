package com.violas.wallet.ui.bank

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.palliums.extensions.close
import com.palliums.utils.*
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.utils.loadCircleImage
import com.violas.wallet.viewModel.bean.*
import kotlinx.android.synthetic.main.dialog_market_select_token.*
import kotlinx.android.synthetic.main.item_market_select_token.view.*
import kotlinx.coroutines.*

/**
 * 选择Token对话框
 */
class BankSelectToken : DialogFragment(), CoroutineScope by CustomMainScope() {

    companion object {
        fun newInstance(): BankSelectToken {
            return BankSelectToken()
        }
    }

    private var mBankAssetsDataResourcesBridge: BankAssetsDataResourcesBridge? = null
    private var mCurrCoin: AssetsVo? = null
    private var mDisplayTokens: List<AssetsVo>? = null
    private var mTokenCallback: ((AssetsVo) -> Unit)? = null
    private var mJob: Job? = null

    private val mTokenAdapter by lazy {
        TokenAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBankAssetsDataResourcesBridge = parentFragment as? BankAssetsDataResourcesBridge
            ?: parentFragment?.parentFragment as? BankAssetsDataResourcesBridge
                    ?: activity as? BankAssetsDataResourcesBridge
    }

    override fun onStart() {
        dialog?.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setWindowAnimations(R.style.AnimationDefaultBottomDialog)

            val attributes = it.attributes
            attributes.gravity = Gravity.BOTTOM
            it.attributes = attributes

            it.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            it.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            )
        }
        super.onStart()
        etSearchBox.clearFocus()
        recyclerView.requestFocus()
    }

    override fun onDetach() {
        mBankAssetsDataResourcesBridge = null
        super.onDetach()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_market_select_token, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearchBox.isEnabled = false
        etSearchBox.addTextChangedListener {
            cancelJob()

            val inputText = it?.toString()?.trim()
            if (inputText.isNullOrEmpty()) {
                if (mDisplayTokens.isNullOrEmpty()) {
                    handleEmptyData(null)
                } else {
                    handleData(mDisplayTokens!!)
                }
            } else {
                searchToken(inputText)
            }
        }

        recyclerView.adapter = mTokenAdapter
        recyclerView.visibility = View.GONE

        mCurrCoin = mBankAssetsDataResourcesBridge?.getCurrCoin()

        statusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
        statusLayout.setReloadCallback {
            statusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
            launch(Dispatchers.IO) {
                loadSwapTokens()
            }
        }
        launch(Dispatchers.IO) {
            loadSwapTokens()
        }
    }

    private fun handleSwapTokens(tokens: List<AssetsVo>?) {
        when {
            tokens == null -> {
                launch(Dispatchers.Main) {
                    handleLoadFailure()
                }
            }
            tokens.isEmpty() -> {
                launch(Dispatchers.Main) {
                    handleEmptyData(null)
                }
            }
            else -> {
                launch(Dispatchers.IO) {
                    filterData(tokens)
                }
            }
        }
    }

    private suspend fun loadSwapTokens() {
        withContext(Dispatchers.Main) {
            mBankAssetsDataResourcesBridge?.getSupportAssetsTokens()
                ?.observe(this@BankSelectToken, Observer {
                    val filter = it?.filter { asstsVo ->
                        if (asstsVo is AssetsCoinVo && asstsVo.accountType == AccountType.NoDollars) {
                            false
                        } else if (asstsVo is HiddenTokenVo) {
                            false
                        } else {
                            true
                        }
                    }
                    handleSwapTokens(filter)
                })
        }
    }

    private fun cancelJob() {
        if (mJob != null) {
            mJob!!.cancel()
            mJob = null
        }
    }

    private suspend fun filterData(marketSupportTokens: List<AssetsVo>) {
        withContext(Dispatchers.Main) {
            mDisplayTokens = marketSupportTokens
            if (marketSupportTokens.isEmpty()) {
                handleEmptyData(null)
                return@withContext
            }

            etSearchBox.isEnabled = true
            val searchText = etSearchBox.text.toString().trim()
            if (searchText.isEmpty()) {
                handleData(marketSupportTokens)
            } else {
                searchToken(searchText)
            }
        }
    }

    private fun searchToken(searchText: String) {
        mJob = launch {
            val searchResult = withContext(Dispatchers.IO) {
                mDisplayTokens?.filter { item ->
                    item.getAssetsName().contains(searchText, true)
                }
            }

            mJob = null

            if (searchResult.isNullOrEmpty()) {
                handleEmptyData(searchText)
            } else {
                handleData(searchResult)
            }
        }
    }

    private fun handleLoadFailure() {
        mTokenAdapter.submitList(null)
        recyclerView.visibility = View.GONE
        statusLayout.showStatus(
            if (isNetworkConnected())
                IStatusLayout.Status.STATUS_FAILURE
            else
                IStatusLayout.Status.STATUS_NO_NETWORK
        )
    }

    private fun handleEmptyData(searchText: String?) {
        mTokenAdapter.submitList(null)
        recyclerView.visibility = View.GONE
        statusLayout.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            if (searchText.isNullOrEmpty())
                getString(R.string.tips_select_token_list_is_empty)
            else
                getSearchEmptyTips(searchText)
        )
        statusLayout.showStatus(IStatusLayout.Status.STATUS_EMPTY)
    }

    private fun getSearchEmptyTips(searchText: String) = run {
        val text = getString(R.string.tips_select_token_search_is_empty, searchText)
        val spannableStringBuilder = SpannableStringBuilder(text)
        text.indexOf(searchText).let {
            spannableStringBuilder.setSpan(
                ForegroundColorSpan(getColorByAttrId(R.attr.colorPrimary, requireContext())),
                it,
                it + searchText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        spannableStringBuilder
    }

    private fun handleData(list: List<AssetsVo>) {
        statusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
        recyclerView.visibility = View.VISIBLE
        mTokenAdapter.submitList(list)
    }

    fun setCallback(
        tokenCallback: ((AssetsVo) -> Unit)? = null
    ): BankSelectToken {
        this.mTokenCallback = tokenCallback
        return this
    }

    inner class TokenAdapter : ListAdapter<AssetsVo, CommonViewHolder>(tokenDiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonViewHolder {
            return CommonViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_market_select_token,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: CommonViewHolder, position: Int) {
            val item = getItem(position)
            holder.itemView.ivLogo.loadCircleImage(
                item.getLogoUrl(),
                getResourceId(R.attr.iconCoinDefLogo, holder.itemView.context)
            )
            holder.itemView.tvTokenName.text = item.getAssetsName()
            holder.itemView.tvTokenBalance.text = getString(
                R.string.market_select_token_balance_format,
                item.amountWithUnit.amount,
                item.amountWithUnit.unit
            )
            holder.itemView.tvSelected.visibility =
                if (item == mCurrCoin) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                close()
                mTokenCallback?.invoke(getItem(position))
            }
        }
    }
}

val tokenDiffCallback = object : DiffUtil.ItemCallback<AssetsVo>() {
    override fun areContentsTheSame(oldItem: AssetsVo, newItem: AssetsVo): Boolean {
        //return oldItem.areContentsTheSame(newItem)
        return false
    }

    override fun areItemsTheSame(oldItem: AssetsVo, newItem: AssetsVo): Boolean {
        return oldItem == newItem
    }
}

interface BankAssetsDataResourcesBridge {
    suspend fun getSupportAssetsTokens(): LiveData<List<AssetsVo>?>

    fun getCurrCoin(): AssetsVo?
}