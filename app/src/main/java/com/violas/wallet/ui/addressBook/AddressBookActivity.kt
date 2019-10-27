package com.violas.wallet.ui.addressBook

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.biz.AddressBookManager
import com.violas.wallet.repository.database.entity.AddressBookDo
import com.violas.wallet.ui.addressBook.add.AddAddressBookActivity
import com.violas.wallet.utils.start
import kotlinx.android.synthetic.main.activity_address_book.*
import kotlinx.android.synthetic.main.item_address_book.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddressBookActivity : BaseActivity() {
    companion object {
        private const val EXT_COIN_TYPE = "a1"
        private const val REQUEST_ADD_COIN = 1
        fun start(context: Context, coinType: Int = -1) {
            Intent(context, AddressBookActivity::class.java).apply {
                putExtra(EXT_COIN_TYPE, coinType)
            }.start(context)
        }
    }

    private val mAddressBookManager by lazy {
        AddressBookManager()
    }

    private val mAddressBookList = mutableListOf<AddressBookDo>()
    private val mAdapter by lazy {
        MyAdapter(mAddressBookList)
    }
    private var mCoinType = -1
    override fun getLayoutResId() = R.layout.activity_address_book

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_address_book)
        setTitleRightImage(R.drawable.icon_add_address)
        mCoinType = intent.getIntExtra(EXT_COIN_TYPE, -1)

        recyclerView.adapter = mAdapter
        loadAddressList(mCoinType)
    }

    override fun onTitleRightViewClick() {
        var coinType = mCoinType
        if (mCoinType == -1) {
            coinType = CoinTypes.Libra.coinType()
        }
        AddAddressBookActivity.start(this, REQUEST_ADD_COIN, coinType)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_COIN && resultCode == Activity.RESULT_OK) {
            loadAddressList(mCoinType)
        }
    }

    private fun loadAddressList(coinType: Int) {
        launch(Dispatchers.IO) {
            val list = mAddressBookManager.loadAddressBook(coinType)
            mAddressBookList.clear()
            mAddressBookList.addAll(list)
            withContext(Dispatchers.Main) {
                mAdapter.notifyDataSetChanged()
            }
        }
    }
}

class MyAdapter(private val data: List<AddressBookDo>) :
    RecyclerView.Adapter<MyAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_address_book,
                parent,
                false
            )
        )
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.itemView.tvTitle.text = item.note
        holder.itemView.tvAddress.text = item.address
        holder.itemView.tvCoinType.text = CoinTypes.parseCoinType(item.coin_number).coinName()
    }

    class ViewHolder(item: View) : RecyclerView.ViewHolder(item)
}
