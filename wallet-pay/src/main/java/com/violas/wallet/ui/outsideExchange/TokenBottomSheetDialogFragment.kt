package com.violas.wallet.ui.outsideExchange

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.palliums.utils.CommonViewHolder
import com.violas.wallet.R
import com.violas.wallet.biz.exchangeMapping.ExchangeAssert
import com.violas.wallet.ui.main.quotes.bean.IToken
import kotlinx.android.synthetic.main.fragment_token_sheet_sheet.view.*
import kotlinx.android.synthetic.main.item_token_sheet_quotes.view.*


class TokenBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        fun newInstance(): TokenBottomSheetDialogFragment {
            return TokenBottomSheetDialogFragment()
        }
    }

    private var mCallback: ((ExchangeAssert) -> Unit)? = null
    private var mOnCloseCallback: (() -> Unit)? = null
    private val mMyAdapter by lazy {
        MyAdapter()
    }

    private var mBehavior: BottomSheetBehavior<View>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog =
            super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        val view =
            View.inflate(context, R.layout.fragment_token_sheet_sheet, null)
        dialog.setContentView(view)
        mBehavior =
            BottomSheetBehavior.from(view.parent as View)
        view.recyclerView.adapter = mMyAdapter
        return dialog
    }

    fun setOnCloseListener(callback: (() -> Unit)) {
        mOnCloseCallback = callback
    }

    fun show(
        childFragmentManager: FragmentManager,
        tokenList: List<ExchangeAssert>,
        selectToken: ExchangeAssert?,
        callback: (ExchangeAssert) -> Unit
    ) {
        mBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        if (tokenList.isNotEmpty()) {
            mCallback = callback
            show(childFragmentManager, "sheet")
            mMyAdapter.mSelectToken = selectToken
            mMyAdapter.submitList(tokenList)
        }
    }

    override fun onDestroy() {
        mOnCloseCallback?.invoke()
        mOnCloseCallback = null
        mCallback = null
        super.onDestroy()
    }

    inner class MyAdapter : ListAdapter<ExchangeAssert, CommonViewHolder>(diffUtil) {
        var mSelectToken: ExchangeAssert? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonViewHolder {
            return CommonViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_token_sheet_quotes,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: CommonViewHolder, position: Int) {
            holder.itemView.tvTokenName.text = getItem(position).getName()
            holder.itemView.checkIsEnable.isSelected =
                mSelectToken?.getCoinType() == getItem(position).getCoinType()
                        && mSelectToken?.getName() == getItem(position).getName()
            holder.itemView.setOnClickListener { mCallback?.invoke(getItem(position)) }
        }
    }
}

val diffUtil = object : DiffUtil.ItemCallback<ExchangeAssert>() {
    override fun areContentsTheSame(oldItem: ExchangeAssert, newItem: ExchangeAssert): Boolean {
        return oldItem.getName() > newItem.getName()
    }

    override fun areItemsTheSame(oldItem: ExchangeAssert, newItem: ExchangeAssert): Boolean {
        return oldItem.getName() == newItem.getName()
    }

}