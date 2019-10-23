package com.violas.wallet.ui.managerAssert

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.base.recycler.RecycleViewItemDivider
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.bean.AssertToken
import com.violas.wallet.utils.DensityUtility
import kotlinx.android.synthetic.main.activity_manager_assert.*
import kotlinx.android.synthetic.main.item_manager_assert.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ManagerAssertActivity : BaseActivity() {
    override fun getLayoutResId() = R.layout.activity_manager_assert

    companion object {
        private const val EXT_ACCOUNT_ID = "0"
        fun start(context: Fragment, accountId: Long, requestId: Int) {
            val intent = Intent(context.activity, ManagerAssertActivity::class.java)
            intent.putExtra(EXT_ACCOUNT_ID, accountId)
            context.startActivityForResult(intent, requestId)
        }

        fun start(context: Activity, accountId: Long, requestId: Int) {
            val intent = Intent(context, ManagerAssertActivity::class.java)
            intent.putExtra(EXT_ACCOUNT_ID, accountId)
            context.startActivityForResult(intent, requestId)
        }
    }


    private val mAccountManager by lazy {
        AccountManager()
    }

    private val mTokenManager by lazy {
        TokenManager()
    }
    private val mSupportTokens = mutableListOf<AssertToken>()
    private val mAdapter by lazy {
        MyAdapter(mSupportTokens) { checked, assertToken ->
            launch(Dispatchers.IO) {
                mTokenManager.insert(checked, assertToken)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("添加币种")

        recyclerView.addItemDecoration(
            RecycleViewItemDivider(
                this,
                DensityUtility.dp2px(this, 12),
                DensityUtility.dp2px(this, 12),
                DensityUtility.dp2px(this, 24),
                DensityUtility.dp2px(this, 24)
            )
        )
        recyclerView.adapter = mAdapter
        launch(Dispatchers.IO) {
            mSupportTokens.clear()
            mSupportTokens.addAll(mTokenManager.loadSupportToken(mAccountManager.currentAccount()))
            mAdapter.notifyDataSetChanged()
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        super.onBackPressed()
    }
 }

class MyAdapter(
    val data: List<AssertToken>,
    private val callbacks: (Boolean, AssertToken) -> Unit
) :
    RecyclerView.Adapter<MyAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_manager_assert,
                parent,
                false
            )
        ).also {
            it.itemView.setOnClickListener { view ->
                val itemData = data[it.layoutPosition]
                if (itemData.isToken) {
                    it.itemView.checkBox.isChecked = !it.itemView.checkBox.isChecked
                    callbacks.invoke(it.itemView.checkBox.isChecked, itemData)
                }
            }
            it.itemView.checkBox.setOnClickListener { view ->
                it.itemView.checkBox.isChecked = !it.itemView.checkBox.isChecked
                val itemData = data[it.layoutPosition]
                callbacks.invoke(it.itemView.checkBox.isChecked, itemData)
            }
        }
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = data[position]
        holder.itemView.name.text = itemData.name
        holder.itemView.fullName.text = itemData.fullName
        if (itemData.isToken) {
            holder.itemView.checkBox.visibility = View.VISIBLE
            holder.itemView.checkBox.isChecked = itemData.enable
        } else {
            holder.itemView.checkBox.visibility = View.GONE
        }
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item)
}
