package com.violas.wallet.ui.changeLanguage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.violas.wallet.databinding.ItemLanguageManagerBinding

internal class ChangeLanguageAdapter(private val viewModel: ChangeLanguageViewModel) :
    ListAdapter<LanguageVo, ChangeLanguageAdapter.ViewHolder>(UnitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemLanguageManagerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.apply {
            bind(createOnClickListener(item.type, holder.adapterPosition), item)
            itemView.tag = item
        }
    }

    private fun createOnClickListener(key: Int, adapterPosition: Int): View.OnClickListener {
        return View.OnClickListener {
            for (i in 0 until itemCount) {
                getItem(i).select = false
            }
            getItem(adapterPosition).select = true
            notifyDataSetChanged()
            viewModel.saveCurrentLanguage(key)
        }
    }

    class ViewHolder(
        private val binding: ItemLanguageManagerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        internal fun bind(listener: View.OnClickListener, item: LanguageVo) {
            binding.apply {
                this.clickListener = listener
                this.item = item
                executePendingBindings()
            }
        }
    }
}

private class UnitDiffCallback : DiffUtil.ItemCallback<LanguageVo>() {
    override fun areItemsTheSame(oldItem: LanguageVo, newItem: LanguageVo): Boolean {
        return oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: LanguageVo, newItem: LanguageVo): Boolean {
        return oldItem == newItem
    }
}