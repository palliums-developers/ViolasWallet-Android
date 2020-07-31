package com.violas.wallet.ui.main.market.selectToken

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
import androidx.recyclerview.widget.ListAdapter
import com.palliums.extensions.close
import com.palliums.utils.*
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.utils.loadCircleImage
import kotlinx.android.synthetic.main.dialog_market_select_token.*
import kotlinx.android.synthetic.main.item_market_select_token.view.*
import kotlinx.coroutines.*

/**
 * 选择Token对话框
 */
class SwapSelectTokenDialog : DialogFragment(), CoroutineScope by CustomMainScope() {

    companion object {
        const val ACTION_SWAP_SELECT_FROM = 0x01
        const val ACTION_SWAP_SELECT_TO = 0x02

        fun newInstance(action: Int): SwapSelectTokenDialog {
            return SwapSelectTokenDialog().apply {
                arguments = Bundle().apply {
                    putInt(KEY_ONE, action)
                }
            }
        }
    }

    private var action: Int = -1
    private var tokenCallback: ((Int, ITokenVo) -> Unit)? = null
    private var swapTokensDataResourcesBridge: SwapTokensDataResourcesBridge? = null
    private var displayTokens: List<ITokenVo>? = null
    private var job: Job? = null

    private val tokenAdapter by lazy {
        TokenAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        swapTokensDataResourcesBridge = parentFragment as? SwapTokensDataResourcesBridge
            ?: parentFragment?.parentFragment as? SwapTokensDataResourcesBridge
                    ?: activity as? SwapTokensDataResourcesBridge
        if (savedInstanceState != null) {
            action = savedInstanceState.getInt(KEY_ONE, action)
        } else if (arguments != null) {
            action = arguments!!.getInt(KEY_ONE, action)
        }
        checkNotNull(swapTokensDataResourcesBridge) { "Parent fragment or activity does not implement TokenBridge" }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_ONE, action)
    }

    override fun onDetach() {
        tokenCallback = null
        swapTokensDataResourcesBridge = null
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
                handleData(displayTokens!!)
            } else {
                searchToken(inputText)
            }
        }

        recyclerView.adapter = tokenAdapter
        recyclerView.visibility = View.GONE

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

    private fun handleSwapTokens(tokens: List<ITokenVo>?) {
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
        if (action == ACTION_SWAP_SELECT_FROM) {
            withContext(Dispatchers.Main) {
                swapTokensDataResourcesBridge?.getMarketSupportFromTokens()
                    ?.observe(this@SwapSelectTokenDialog, Observer {
                        handleSwapTokens(it)
                    })
            }
        } else {
            swapTokensDataResourcesBridge?.getMarketSupportToTokens().let {
                handleSwapTokens(it)
            }
        }
    }

    private fun cancelJob() {
        if (job != null) {
            job!!.cancel()
            job = null
        }
    }

    private suspend fun filterData(marketSupportTokens: List<ITokenVo>) {
        withContext(Dispatchers.Main) {
            val list = marketSupportTokens

            etSearchBox.isEnabled = true
            displayTokens = list
            if (list.isEmpty()) {
                handleEmptyData(null)
                return@withContext
            }

            val searchText = etSearchBox.text.toString().trim()
            if (searchText.isEmpty()) {
                handleData(list)
            } else {
                searchToken(searchText)
            }
        }
    }

    private fun searchToken(searchText: String) {
        job = launch {
            val searchResult = withContext(Dispatchers.IO) {
                displayTokens?.filter { item ->
                    item.displayName.contains(searchText, true)
                }
            }

            job = null

            if (searchResult.isNullOrEmpty()) {
                handleEmptyData(searchText)
            } else {
                handleData(searchResult)
            }
        }
    }

    private fun handleLoadFailure() {
        tokenAdapter.submitList(null)
        recyclerView.visibility = View.GONE
        statusLayout.showStatus(
            if (isNetworkConnected())
                IStatusLayout.Status.STATUS_FAILURE
            else
                IStatusLayout.Status.STATUS_NO_NETWORK
        )
    }

    private fun handleEmptyData(searchText: String?) {
        tokenAdapter.submitList(null)
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

    private fun handleData(list: List<ITokenVo>) {
        statusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
        recyclerView.visibility = View.VISIBLE
        tokenAdapter.submitList(list)
    }

    fun setCallback(
        tokenCallback: ((Int, ITokenVo) -> Unit)? = null
    ): SwapSelectTokenDialog {
        this.tokenCallback = tokenCallback
        return this
    }

    inner class TokenAdapter : ListAdapter<ITokenVo, CommonViewHolder>(tokenDiffCallback) {

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
                item.logo,
                getResourceId(R.attr.iconCoinDefLogo, holder.itemView.context)
            )
            holder.itemView.tvTokenName.text = item.displayName
            holder.itemView.tvTokenBalance.text = getString(
                R.string.market_select_token_balance_format,
                item.displayAmount.toPlainString(),
                item.displayName
            )
            holder.itemView.tvSelected.visibility = if (item.selected) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                close()
                tokenCallback?.invoke(action, getItem(position))
            }
        }
    }
}

interface SwapTokensDataResourcesBridge {
    suspend fun getMarketSupportFromTokens(): LiveData<List<ITokenVo>?>

    suspend fun getMarketSupportToTokens(): List<ITokenVo>?
}