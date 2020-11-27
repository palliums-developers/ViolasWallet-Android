package com.violas.wallet.ui.addressBook

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.WorkerThread
import androidx.lifecycle.ViewModelProvider
import com.palliums.base.BaseViewHolder
import com.palliums.listing.ListingViewAdapter
import com.palliums.listing.ListingViewModel
import com.palliums.utils.coroutineExceptionHandler
import com.palliums.utils.start
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseListingActivity
import com.violas.wallet.biz.AddressBookManager
import com.violas.wallet.repository.database.entity.AddressBookDo
import com.violas.wallet.ui.addressBook.add.AddAddressBookActivity
import com.violas.wallet.widget.dialog.DeleteAddressDialog
import kotlinx.android.synthetic.main.item_address_book.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddressBookActivity : BaseListingActivity<AddressBookDo>() {
    companion object {
        private const val EXT_COIN_TYPE = "a1"
        private const val EXT_IS_SELECTOR = "a2"
        const val RESULT_SELECT_ADDRESS = "a3"
        private const val REQUEST_ADD_COIN = 1
        fun start(
            context: Activity,
            coinType: Int = Int.MIN_VALUE,
            isSelector: Boolean = false,
            requestCode: Int = -1
        ) {
            if (isSelector) {
                Intent(context, AddressBookActivity::class.java).apply {
                    putExtra(EXT_COIN_TYPE, coinType)
                    putExtra(EXT_IS_SELECTOR, true)
                }.start(context, requestCode)
            } else {
                Intent(context, AddressBookActivity::class.java).apply {
                    putExtra(EXT_COIN_TYPE, coinType)
                }.start(context)
            }

        }
    }

    private var mCoinType = Int.MIN_VALUE
    private var mSelector = false

    private val mViewModel by lazy {
        ViewModelProvider(this).get(AddressBookViewModel::class.java)
    }

    private val mViewAdapter by lazy {
        MyAdapter(
            mCallback = {
                if (mSelector) {
                    setResult(
                        Activity.RESULT_OK,
                        Intent().apply {
                            putExtra(RESULT_SELECT_ADDRESS, it.address)
                        }
                    )
                    finish()
                } else {
                    // todo 编辑
                }
            },
            mLongClickCallback = { position, addressBook ->
                DeleteAddressDialog().setConfirmListener {
                    it.dismiss()

                    showProgress()
                    launch(Dispatchers.IO + coroutineExceptionHandler()) {
                        mViewModel.removeAddress(addressBook)
                        withContext(Dispatchers.Main) {
                            mViewModel.execute(mCoinType)
                            dismissProgress()
                        }
                    }

                }.show(supportFragmentManager, "delete")
            }
        )
    }

    override fun getListingViewModel(): ListingViewModel<AddressBookDo> {
        return mViewModel
    }

    override fun getListingViewAdapter(): ListingViewAdapter<AddressBookDo> {
        return mViewAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = getString(R.string.title_address_book)
        setTitleRightImageResource(R.drawable.icon_add_address)

        mCoinType = intent.getIntExtra(EXT_COIN_TYPE, Int.MIN_VALUE)
        mSelector = intent.getBooleanExtra(EXT_IS_SELECTOR, false)

        mListingHandler.init()
        getStatusLayout()?.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            getString(R.string.tips_no_address)
        )
        getDrawable(R.mipmap.ic_no_address)?.let {
            getStatusLayout()?.setImageWithStatus(IStatusLayout.Status.STATUS_EMPTY, it)
        }

        mViewModel.execute(mCoinType)
    }

    override fun onTitleRightViewClick() {
        var coinType = mCoinType
        AddAddressBookActivity.start(this, REQUEST_ADD_COIN, coinType)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_COIN && resultCode == Activity.RESULT_OK) {
            mViewModel.execute(mCoinType)
        }
    }
}

class AddressBookViewModel : ListingViewModel<AddressBookDo>() {

    private val mAddressBookManager by lazy {
        AddressBookManager()
    }

    @WorkerThread
    fun removeAddress(addressBook: AddressBookDo) {
        mAddressBookManager.remove(addressBook)
    }

    override suspend fun loadData(vararg params: Any): List<AddressBookDo> {
        return mAddressBookManager.loadAddressBook(params[0] as Int)
    }

    override fun checkNetworkBeforeExecute(): Boolean {
        return false
    }
}

class MyAdapter(
    private val mCallback: (AddressBookDo) -> Unit,
    private val mLongClickCallback: (position: Int, AddressBookDo) -> Unit
) : ListingViewAdapter<AddressBookDo>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_address_book,
                parent,
                false
            ),
            mCallback,
            mLongClickCallback
        )
    }

    class ViewHolder(
        item: View,
        private val mCallback: (AddressBookDo) -> Unit,
        private val mLongClickCallback: (position: Int, AddressBookDo) -> Unit
    ) : BaseViewHolder<AddressBookDo>(item) {

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener {
                itemData?.let {
                    mLongClickCallback.invoke(adapterPosition, it)
                }

                true
            }
        }

        override fun onViewBind(itemPosition: Int, itemData: AddressBookDo?) {
            itemData?.let {
                itemView.tvTitle.text = it.note
                itemView.tvAddress.text = it.address
                itemView.tvCoinType.text = CoinTypes.parseCoinType(it.coin_number).fullName()
            }
        }

        override fun onViewClick(view: View, itemPosition: Int, itemData: AddressBookDo?) {
            when (view) {
                itemView -> {
                    itemData?.let { mCallback.invoke(it) }
                }
            }
        }
    }
}
