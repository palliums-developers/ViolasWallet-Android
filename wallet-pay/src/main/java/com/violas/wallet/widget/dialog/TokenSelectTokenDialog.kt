package com.violas.wallet.widget.dialog

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

class AssetsVoTokenSelectTokenDialog : TokenSelectTokenDialog<AssetsVo>() {
    override fun getContent(vo: AssetsVo) = vo.getAssetsName()

    override fun getLogoUrl(vo: AssetsVo) = vo.getLogoUrl()

    override fun getName(vo: AssetsVo) = vo.getAssetsName()

    override fun getBalance(vo: AssetsVo) = vo.amountWithUnit.amount

    override fun getUnit(vo: AssetsVo) = vo.amountWithUnit.unit

    interface AssetsDataResourcesBridge : DataResourcesBridge<AssetsVo>
}

/**
 * 选择Token对话框
 */
abstract class TokenSelectTokenDialog<VO> : DialogFragment(), CoroutineScope by CustomMainScope() {

    private var mDataResourcesBridge: DataResourcesBridge<VO>? = null
    private var mCurrCoin: VO? = null
    private var mDisplayTokens: List<VO>? = null
    private var mTokenCallback: ((VO) -> Unit)? = null
    private var mJob: Job? = null
    private var lastSearchEmptyData = false

    private val mTokenAdapter by lazy {
        TokenAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDataResourcesBridge = parentFragment as? DataResourcesBridge<VO>
            ?: parentFragment?.parentFragment as? DataResourcesBridge<VO>
                    ?: activity as? DataResourcesBridge<VO>
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
        mDataResourcesBridge = null
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

        mCurrCoin = mDataResourcesBridge?.getCurrCoin()

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

    private fun handleSwapTokens(tokens: List<VO>?) {
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
            mDataResourcesBridge?.getSupportAssetsTokens()
                ?.observe(this@TokenSelectTokenDialog, Observer {
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

    private suspend fun filterData(marketSupportTokens: List<VO>) {
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
                    getContent(item).contains(searchText, true)
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
        if (recyclerView.visibility != View.GONE) {
            mTokenAdapter.submitList(null)
            recyclerView.visibility = View.GONE
        }

        if (lastSearchEmptyData && searchText.isNullOrEmpty()) {
            statusLayout.setImageWithStatus(
                IStatusLayout.Status.STATUS_EMPTY,
                getResourceId(R.attr.bgLoadEmptyData, requireContext())
            )
        } else if (!lastSearchEmptyData && !searchText.isNullOrEmpty()) {
            statusLayout.setImageWithStatus(
                IStatusLayout.Status.STATUS_EMPTY,
                getResourceId(R.attr.bgSearchEmptyData, requireContext())
            )
        }
        lastSearchEmptyData = !searchText.isNullOrEmpty()

        statusLayout.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            if (searchText.isNullOrEmpty())
                getString(R.string.select_currency_desc_tokens_empty)
            else
                getSearchEmptyTips(searchText)
        )
        statusLayout.showStatus(IStatusLayout.Status.STATUS_EMPTY)
    }

    private fun getSearchEmptyTips(searchText: String) = run {
        val text = getString(R.string.select_currency_desc_search_empty_format, searchText)
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

    private fun handleData(list: List<VO>) {
        statusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
        recyclerView.visibility = View.VISIBLE
        mTokenAdapter.submitList(list)
    }

    fun setCallback(
        tokenCallback: ((VO) -> Unit)? = null
    ): TokenSelectTokenDialog<VO> {
        this.mTokenCallback = tokenCallback
        return this
    }

    inner class TokenAdapter : ListAdapter<VO, CommonViewHolder>(VoDiff<VO>()) {

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
                getLogoUrl(item),
                getResourceId(R.attr.iconCoinDefLogo, holder.itemView.context)
            )
            holder.itemView.tvTokenName.text = getName(item)
            holder.itemView.tvTokenBalance.text = getString(
                R.string.common_label_balance_format,
                getBalance(item),
                getUnit(item)
            )
            holder.itemView.tvSelected.visibility =
                if (item == mCurrCoin) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                close()
                mTokenCallback?.invoke(getItem(position))
            }
        }
    }

    abstract fun getContent(vo: VO): String
    abstract fun getLogoUrl(vo: VO): String
    abstract fun getName(vo: VO): String
    abstract fun getBalance(vo: VO): String
    abstract fun getUnit(vo: VO): String
}

class VoDiff<VO> : DiffUtil.ItemCallback<VO>() {
    override fun areContentsTheSame(oldItem: VO, newItem: VO): Boolean {
        //return oldItem.areContentsTheSame(newItem)
        return false
    }

    override fun areItemsTheSame(oldItem: VO, newItem: VO): Boolean {
        return oldItem == newItem
    }
}

interface DataResourcesBridge<VO> {
    suspend fun getSupportAssetsTokens(): LiveData<List<VO>?>

    fun getCurrCoin(): VO?
}