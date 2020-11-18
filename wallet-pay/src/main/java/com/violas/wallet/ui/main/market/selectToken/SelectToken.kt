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
import androidx.lifecycle.EnhancedMutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.palliums.extensions.close
import com.palliums.extensions.showToast
import com.palliums.utils.*
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.common.KEY_ONE
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.ui.main.market.bean.StableTokenVo
import com.violas.wallet.utils.loadCircleImage
import kotlinx.android.synthetic.main.dialog_market_select_token.*
import kotlinx.android.synthetic.main.item_market_select_token.view.*
import kotlinx.coroutines.*

/**
 * 选择Token对话框
 */
class SelectTokenDialog : DialogFragment(), CoroutineScope by CustomMainScope() {

    companion object {
        const val ACTION_SWAP_SELECT_FROM = 0x01
        const val ACTION_SWAP_SELECT_TO = 0x02
        const val ACTION_POOL_SELECT_A = 0x03
        const val ACTION_POOL_SELECT_B = 0x04
        const val ACTION_MAPPING_SELECT = 0x05

        fun newInstance(action: Int): SelectTokenDialog {
            return SelectTokenDialog().apply {
                arguments = Bundle().apply {
                    putInt(KEY_ONE, action)
                }
            }
        }
    }

    private var action: Int = -1
    private var coinsBridge: CoinsBridge? = null
    private var displayCoins: List<ITokenVo>? = null
    private var currCoin: ITokenVo? = null
    private var job: Job? = null
    private var lastSearchEmptyData = false

    private val coinAdapter by lazy { CoinAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coinsBridge = parentFragment as? CoinsBridge
            ?: parentFragment?.parentFragment as? CoinsBridge
                    ?: activity as? CoinsBridge
        checkNotNull(coinsBridge) { "Parent fragment or activity does not implement TokenBridge" }

        if (savedInstanceState != null) {
            action = savedInstanceState.getInt(KEY_ONE, action)
        } else if (arguments != null) {
            action = arguments!!.getInt(KEY_ONE, action)
        }
        require(action != -1) { "action is not the specified value" }
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
        coinsBridge = null
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
                if (displayCoins.isNullOrEmpty()) {
                    handleEmptyData(null)
                } else {
                    handleData(displayCoins!!)
                }
            } else {
                searchToken(inputText)
            }
        }

        recyclerView.adapter = coinAdapter
        recyclerView.visibility = View.GONE

        statusLayout.setReloadCallback {
            getMarketSupportCoins()
        }

        currCoin = coinsBridge?.getCurrCoin(action)
        coinsBridge?.getTipsMessageLiveData()?.run {
            setValueSupport("")
            observe(viewLifecycleOwner, Observer {
                it.getDataIfNotHandled()?.let { msg ->
                    if (msg.isNotEmpty()) {
                        showToast(msg)
                    }
                }
            })
        }
        coinsBridge?.getMarketSupportCoinsLiveData()?.observe(viewLifecycleOwner, Observer {
            cancelJob()

            if (it.isEmpty()) {
                handleEmptyData(null)
            } else {
                filterData(it)
            }
        })

        getMarketSupportCoins()
    }

    private fun getMarketSupportCoins() {
        statusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
        coinsBridge?.getMarketSupportCoins {
            handleLoadFailure()
        }
    }

    private fun cancelJob() {
        job?.let {
            try {
                it.cancel()
            } catch (ignore: Exception) {
            }
            job = null
        }
    }

    private fun filterData(marketSupportTokens: List<ITokenVo>) {
        launch {
            val list = withContext(Dispatchers.IO) {
                marketSupportTokens.filter {
                    when (action) {
                        ACTION_SWAP_SELECT_FROM -> {
                            // 兑换选择输出币种，展示交易市场支持的所有币种
                            true
                        }
                        ACTION_SWAP_SELECT_TO -> {
                            // 兑换选择输出币种，展示交易市场支持的所有币种
                            val from = coinsBridge?.getCurrCoin(ACTION_SWAP_SELECT_FROM)
                            from?.coinNumber == it.coinNumber
                        }

                        ACTION_MAPPING_SELECT -> {
                            // 映射选择币种，展示映射支持的所有币种
                            true
                        }

                        else -> {
                            // 资金池转入选择币种，只展示交易市场支持的violas稳定币种
                            if (it is StableTokenVo) {
                                it.coinNumber == CoinTypes.Violas.coinType()
                            } else {
                                false
                            }
                        }
                    }
                }
            }

            displayCoins = list
            if (list.isEmpty()) {
                handleEmptyData(null)
                return@launch
            }

            etSearchBox.isEnabled = true
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
                displayCoins?.filter { item ->
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
        if (recyclerView == null) return

        coinAdapter.submitList(null)
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
            coinAdapter.submitList(null)
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
        coinAdapter.submitList(list)
    }

    inner class CoinAdapter : ListAdapter<ITokenVo, CommonViewHolder>(tokenDiffCallback) {

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
            holder.itemView.tvSelected.visibility =
                if (item == currCoin) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                close()
                coinsBridge?.onSelectCoin(action, getItem(position))
            }
        }
    }
}

val tokenDiffCallback = object : DiffUtil.ItemCallback<ITokenVo>() {
    override fun areContentsTheSame(oldItem: ITokenVo, newItem: ITokenVo): Boolean {
        //return oldItem.areContentsTheSame(newItem)
        return false
    }

    override fun areItemsTheSame(oldItem: ITokenVo, newItem: ITokenVo): Boolean {
        return oldItem == newItem
    }
}

interface CoinsBridge {

    fun onSelectCoin(action: Int, coin: ITokenVo)

    fun getMarketSupportCoins(failureCallback: (error: Throwable) -> Unit)

    fun getMarketSupportCoinsLiveData(): LiveData<List<ITokenVo>>

    fun getTipsMessageLiveData(): EnhancedMutableLiveData<String>

    fun getCurrCoin(action: Int): ITokenVo?
}