package com.violas.wallet.ui.main.quotes.tokenList

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
import com.violas.wallet.ui.main.quotes.bean.IToken
import kotlinx.android.synthetic.main.fragment_token_sheet_sheet.view.*
import kotlinx.android.synthetic.main.item_token_sheet_quotes.view.*


class TokenBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        fun newInstance(): TokenBottomSheetDialogFragment {
            return TokenBottomSheetDialogFragment()
        }
    }

    private var mCallback: ((IToken) -> Unit)? = null

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

    fun show(
        childFragmentManager: FragmentManager,
        tokenList: List<IToken>,
        callback: (IToken) -> Unit
    ) {
        mBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        if (tokenList.isNotEmpty()) {
            mCallback = callback
            show(childFragmentManager, "sheet")
            mMyAdapter.submitList(tokenList)
        }
    }

    override fun onDestroy() {
        mCallback = null
        super.onDestroy()
    }

    inner class MyAdapter : ListAdapter<IToken, CommonViewHolder>(diffUtil) {
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
            holder.itemView.tvTokenName.text = getItem(position).tokenName()
            holder.itemView.checkIsEnable.isSelected = getItem(position).isNetEnable()
            holder.itemView.setOnClickListener { mCallback?.invoke(getItem(position)) }
        }
    }
}

val diffUtil = object : DiffUtil.ItemCallback<IToken>() {
    override fun areContentsTheSame(oldItem: IToken, newItem: IToken): Boolean {
        return oldItem.tokenIdx() > newItem.tokenIdx()
    }

    override fun areItemsTheSame(oldItem: IToken, newItem: IToken): Boolean {
        return oldItem.tokenIdx() == newItem.tokenIdx()
    }

}