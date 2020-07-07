package com.violas.wallet.ui.main.market.selectToken

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.palliums.extensions.close
import com.palliums.utils.CommonViewHolder
import com.palliums.utils.CustomMainScope
import com.palliums.utils.getResourceId
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.ui.main.market.bean.ITokenVo
import com.violas.wallet.utils.convertAmountToDisplayUnit
import kotlinx.android.synthetic.main.dialog_market_select_token.*
import kotlinx.android.synthetic.main.item_market_select_token.view.*
import kotlinx.coroutines.*

/**
 * 选择Token对话框
 */
class SelectTokenDialog : DialogFragment(), CoroutineScope by CustomMainScope() {

    private var tokenCallback: ((ITokenVo) -> Unit)? = null
    private var tokensBridge: TokensBridge? = null
    private var tokens: List<ITokenVo>? = null
    private var searchJob: Job? = null

    private val adapter by lazy {
        TokenAdapter()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        tokensBridge = parentFragment as? TokensBridge
            ?: parentFragment?.parentFragment as? TokensBridge
                    ?: activity as? TokensBridge
        checkNotNull(tokensBridge) { "Parent fragment or activity does not implement TokenBridge" }
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
            if (searchJob != null) {
                searchJob!!.cancel()
                searchJob = null
            }

            val inputText = it?.toString()?.trim()
            if (inputText.isNullOrBlank()) {
                adapter.submitList(tokens)
            } else {
                searchJob = launch {
                    val searchResult = withContext(Dispatchers.IO) {
                        tokens?.filter { item ->
                            item.displayName.contains(inputText, true)
                        }
                    }
                    adapter.submitList(searchResult)
                    searchJob = null
                }
            }
        }
        recyclerView.adapter = adapter

        tokensBridge!!.getTokensLiveData().observe(viewLifecycleOwner, Observer {
            if (it.isNullOrEmpty()) {
                // 加载失败和空数据处理
            } else {
                etSearchBox.isEnabled = true
                tokens = it
                adapter.submitList(it)
            }
        })
    }

    override fun onStart() {
        dialog?.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
        tokenCallback = null
        tokensBridge = null
        super.onDetach()
    }

    fun setCallback(
        tokenCallback: ((ITokenVo) -> Unit)? = null
    ): SelectTokenDialog {
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
            val defLogoResId =
                getResourceId(R.attr.walletHomeDefTokenLogo, holder.itemView.context)
            val item = getItem(position)
            Glide.with(holder.itemView)
                .load(item.logoUrl)
                .error(defLogoResId)
                .placeholder(defLogoResId)
                .into(holder.itemView.ivLogo)
            holder.itemView.tvTokenName.text = item.displayName
            val amountWithUnit =
                convertAmountToDisplayUnit(item.amount, CoinTypes.parseCoinType(item.coinNumber))
            holder.itemView.tvTokenBalance.text = getString(
                R.string.market_select_token_balance_format,
                amountWithUnit.first,
                item.displayName
            )
            holder.itemView.tvSelected.visibility = if (item.selected) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                close()
                tokenCallback?.invoke(getItem(position))
            }
        }
    }
}

val tokenDiffCallback = object : DiffUtil.ItemCallback<ITokenVo>() {
    override fun areContentsTheSame(oldItem: ITokenVo, newItem: ITokenVo): Boolean {
        return oldItem.areContentsTheSame(newItem)
    }

    override fun areItemsTheSame(oldItem: ITokenVo, newItem: ITokenVo): Boolean {
        return oldItem == newItem
    }
}

interface TokensBridge {

    fun getTokensLiveData(): LiveData<List<ITokenVo>?>
}