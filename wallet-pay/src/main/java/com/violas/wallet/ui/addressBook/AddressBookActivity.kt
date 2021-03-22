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
import com.palliums.utils.getResourceId
import com.palliums.utils.start
import com.palliums.widget.status.IStatusLayout
import com.quincysx.crypto.CoinType
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
        private const val EXT_COIN_NUMBER = "a1"
        private const val EXT_IS_SELECTOR = "a2"
        const val RESULT_SELECT_ADDRESS = "a3"
        private const val REQUEST_ADD_COIN = 1
        fun start(
            context: Activity,
            coinNumber: Int = Int.MIN_VALUE,
            isSelector: Boolean = false,
            requestCode: Int = -1
        ) {
            if (isSelector) {
                Intent(context, AddressBookActivity::class.java).apply {
                    putExtra(EXT_COIN_NUMBER, coinNumber)
                    putExtra(EXT_IS_SELECTOR, true)
                }.start(context, requestCode)
            } else {
                Intent(context, AddressBookActivity::class.java).apply {
                    putExtra(EXT_COIN_NUMBER, coinNumber)
                }.start(context)
            }

        }
    }

    private var mCoinType = Int.MIN_VALUE
    private var mSelector = false

    override fun lazyInitListingViewModel(): ListingViewModel<AddressBookDo> {
        return ViewModelProvider(this).get(AddressBookViewModel::class.java)
    }

    override fun lazyInitListingViewAdapter(): ListingViewAdapter<AddressBookDo> {
        return ViewAdapter(
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
                        getViewModel().removeAddress(addressBook)
                        withContext(Dispatchers.Main) {
                            getViewModel().execute(mCoinType)
                            dismissProgress()
                        }
                    }

                }.show(supportFragmentManager, "delete")
            }
        )
    }

    fun getViewModel(): AddressBookViewModel {
        return getListingViewModel() as AddressBookViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = getString(R.string.common_title_address_book)
        setTitleRightImageResource(getResourceId(R.attr.iconAdd, this))

        mCoinType = intent.getIntExtra(EXT_COIN_NUMBER, Int.MIN_VALUE)
        mSelector = intent.getBooleanExtra(EXT_IS_SELECTOR, false)

        getListingHandler().init()
        getStatusLayout()?.setTipsWithStatus(
            IStatusLayout.Status.STATUS_EMPTY,
            getString(R.string.address_book_desc_addresses_empty)
        )

        getViewModel().execute(mCoinType)
    }

    override fun onTitleRightViewClick() {
        var coinType = mCoinType
        AddAddressBookActivity.start(this, REQUEST_ADD_COIN, coinType)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_COIN && resultCode == Activity.RESULT_OK) {
            getViewModel().execute(mCoinType)
        }
    }

    class ViewAdapter(
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
                itemView.tvCoinType.text = CoinType.parseCoinNumber(it.coin_number).chainName()
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
