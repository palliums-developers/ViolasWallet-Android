package com.violas.wallet.ui.main.message

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.palliums.net.RequestException
import com.palliums.utils.DensityUtility
import com.palliums.utils.getColor
import com.palliums.widget.dividers.RecycleViewItemDividers
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.event.RefreshGovernorApplicationProgressEvent
import com.violas.wallet.ui.applyForLicence.ApplyForLicenceActivity
import com.violas.wallet.ui.governorApproval.GovernorApprovalActivity
import com.violas.wallet.ui.governorMint.GovernorMintActivity
import com.violas.wallet.widget.dialog.PasswordInputDialog
import kotlinx.android.synthetic.main.fragment_apply_message.*
import kotlinx.android.synthetic.main.fragment_apply_message_governor.*
import kotlinx.android.synthetic.main.fragment_apply_message_sso.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.wallet.Account

/**
 * 消息首页，包含州长牌照申请进度视图 和 SSO发币申请消息列表视图
 */
class ApplyMessageFragment : BaseFragment() {

    /**
     * 更新数据标志，刷新中会置为false，刷新完成后再置为true
     */
    private var mUpdateSSOMsgDataFlag: Boolean = true
    /**
     * [mUpdateSSOMsgDataFlag]为false期间缓存的刷新数据
     */
    private var mCacheSSOMsgPagedList: PagedList<SSOApplicationMsgVO>? = null

    private val mViewModel by lazy {
        ViewModelProvider(this).get(ApplyMessageViewModel::class.java)
    }
    private val mSSOMsgViewAdapter by lazy {
        SSOApplicationMsgViewAdapter(retryCallback = { mViewModel.retry() }) { msg ->
            activity?.let {
                mViewModel.observeChangedSSOApplicationMsg(msg)
                if (msg.applicationStatus == 3) {
                    GovernorMintActivity.start(it, msg)
                } else {
                    GovernorApprovalActivity.start(it, msg)
                }
            }
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_message
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        initView()
        observeSSOMsgList()
        observeLoadStateAndTipsMsg()
        observeAccount()
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        setStatusBarMode(true)
    }

    override fun onDetach() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }

        super.onDetach()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshGovernorApplicationProgressEvent(event: RefreshGovernorApplicationProgressEvent) {
        loadGovernorApplicationProgress()
    }

    private fun initView() {
        setStatusBarMode(true)

        dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)

        srlRefreshLayout.isEnabled = false
        srlRefreshLayout.setOnRefreshListener {
            if (mViewModel.mGovernorApplicationStatus == 4) {
                // 刷新时原有数据会被清空，造成短暂的页面闪屏或页面空白
                // 刷新时先做个标记不更新数据，刷新完成后再做处理
                mUpdateSSOMsgDataFlag = false
                mViewModel.refresh()
            } else {
                loadGovernorApplicationProgress()
            }
        }
    }

    private fun observeAccount() {
        mViewModel.mAccountLD.observe(this, Observer {
            loadGovernorApplicationProgress(true)
        })
    }

    private fun loadGovernorApplicationProgress(init: Boolean = false) {
        if (!init && !srlRefreshLayout.isRefreshing) {
            srlRefreshLayout.isRefreshing = true
        }

        mViewModel.loadGovernorApplicationProgress(
            failureCallback = {
                if (srlRefreshLayout.isRefreshing) {
                    srlRefreshLayout.isRefreshing = false
                } else {
                    srlRefreshLayout.isEnabled = true
                }

                if (clGovernorLicenceLayout?.visibility != View.VISIBLE) {
                    dslStatusLayout.showStatus(
                        if (RequestException.isNoNetwork(it))
                            IStatusLayout.Status.STATUS_NO_NETWORK
                        else
                            IStatusLayout.Status.STATUS_FAILURE
                    )
                }
            },
            successCallback = {
                if (it == 4) { // 4: minted
                    // 州长牌照已批准
                    showSSOApplicationMsgView()
                } else {
                    // 州长牌照还未批准
                    if (srlRefreshLayout.isRefreshing) {
                        srlRefreshLayout.isRefreshing = false
                    } else {
                        srlRefreshLayout.isEnabled = true
                        dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
                    }

                    showGovernorApplicationProgressView(it)
                }
            }
        )
    }

    private fun showSSOApplicationMsgView() {
        // 显示SSO发币申请消息视图，隐藏州长牌照申请进度视图
        if (drlSSOMsgRefreshLayout == null) {
            // 初始化SSO发币申请消息视图
            try {
                vsSSOMsgLayout.inflate()
            } catch (ignore: Exception) {
            }

            // 初始化SSO发币申请消息视图的控件设置
            drlSSOMsgRefreshLayout.setEnableLoadMore(false)
            drlSSOMsgRefreshLayout.setEnableOverScrollBounce(true)
            drlSSOMsgRefreshLayout.setEnableOverScrollDrag(true)
            drlSSOMsgRefreshLayout.setOnRefreshListener {
                // 刷新时原有数据会被清空，造成短暂的页面闪屏或页面空白
                // 刷新时先做个标记不更新数据，刷新完成后再做处理
                mUpdateSSOMsgDataFlag = false
                mViewModel.refresh()
            }

            rvSSOMsgList.adapter = mSSOMsgViewAdapter
            rvSSOMsgList.addItemDecoration(
                RecycleViewItemDividers(
                    top = DensityUtility.dp2px(requireContext(), 10),
                    bottom = 0,
                    showFirstTop = true,
                    onlyShowLastBottom = true
                )
            )

            observeChangedSSOApplicationMsg()
        }

        clGovernorLicenceLayout?.visibility = View.GONE
        drlSSOMsgRefreshLayout.visibility = View.VISIBLE

        mViewModel.start()
    }

    private fun showGovernorApplicationProgressView(applicationStatus: Int) {
        // 显示州长牌照申请进度视图，隐藏SSO发币申请消息视图
        if (clGovernorLicenceLayout == null) {
            // 初始州长牌照申请进度视图
            try {
                vsGovernorLicenceLayout.inflate()
            } catch (ignore: Exception) {
            }

            // 初始州长牌照申请进度视图的控件设置
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this)
            }

            btnGovernorLicenceGoto.setOnClickListener {
                if (mViewModel.mGovernorApplicationStatus == -1
                    || mViewModel.mGovernorApplicationStatus == 2
                ) {
                    activity?.let {
                        ApplyForLicenceActivity.start(it)
                    }
                } else if (mViewModel.mGovernorApplicationStatus == 1) {
                    showPasswordInputDialog()
                }
            }
        }

        drlSSOMsgRefreshLayout?.visibility = View.GONE
        clGovernorLicenceLayout.visibility = View.VISIBLE

        // 更新州长牌照申请进度视图
        when (applicationStatus) {
            0 -> {  // 0: not approved
                mivGovernorLicenceStatus.setEndDescText(R.string.desc_governor_application_not_approved)
                mivGovernorLicenceStatus.setEndDescTextColor(getColor(R.color.color_00D1AF))
                btnGovernorLicenceGoto.visibility = View.GONE
            }

            1 -> {  // 1: pass
                mivGovernorLicenceStatus.setEndDescText(R.string.desc_governor_application_pass)
                mivGovernorLicenceStatus.setEndDescTextColor(getColor(R.color.color_3C3848_80))
                btnGovernorLicenceGoto.setText(R.string.action_apply_for_licence)
                btnGovernorLicenceGoto.visibility = View.VISIBLE
            }

            2 -> {  // 2: not pass
                mivGovernorLicenceStatus.setEndDescText(R.string.desc_governor_application_not_pass)
                mivGovernorLicenceStatus.setEndDescTextColor(getColor(R.color.def_text_warn))
                btnGovernorLicenceGoto.setText(R.string.action_goto_apply_for_governor)
                btnGovernorLicenceGoto.visibility = View.VISIBLE
            }

            3 -> {  // 3: published
                mivGovernorLicenceStatus.setEndDescText(R.string.desc_governor_application_published)
                mivGovernorLicenceStatus.setEndDescTextColor(getColor(R.color.color_00D1AF))
                btnGovernorLicenceGoto.visibility = View.GONE
            }

            else -> { // -1: no application
                mivGovernorLicenceStatus.setEndDescText(R.string.desc_governor_application_not_application)
                mivGovernorLicenceStatus.setEndDescTextColor(getColor(R.color.color_3C3848_80))
                btnGovernorLicenceGoto.setText(R.string.action_goto_apply_for_governor)
                btnGovernorLicenceGoto.visibility = View.VISIBLE
            }
        }
    }

    private fun showPasswordInputDialog() {
        PasswordInputDialog()
            .setConfirmListener { password, dialogFragment ->
                dialogFragment.dismiss()
                showProgress()

                launch(Dispatchers.Main) {
                    val account = withContext(Dispatchers.IO) {
                        val simpleSecurity =
                            SimpleSecurity.instance(requireContext().applicationContext)
                        val privateKey = simpleSecurity.decrypt(
                            password, mViewModel.mAccountLD.value!!.privateKey
                        )
                        return@withContext if (privateKey == null)
                            null
                        else
                            Account(KeyPair.fromSecretKey(privateKey))
                    }

                    if (account != null) {
                        publishVStake(account)
                    } else {
                        dismissProgress()
                        showToast(getString(R.string.hint_password_error))
                    }
                }
            }
            .show(childFragmentManager)
    }

    private fun publishVStake(account: Account) {
        mViewModel.publishVStake(
            requireContext(), account,
            failureCallback = {
                dismissProgress()
            },
            successCallback = {
                dismissProgress()
                loadGovernorApplicationProgress()
            })
    }

    private fun observeSSOMsgList() {
        mViewModel.pagedList.observe(this, Observer {
            if (mUpdateSSOMsgDataFlag) {
                mSSOMsgViewAdapter.submitList(it)
            } else {
                mCacheSSOMsgPagedList = it
            }
        })
    }

    private fun observeLoadStateAndTipsMsg() {
        mViewModel.refreshState.observe(this, Observer {
            when (it.peekData().status) {
                LoadState.Status.SUCCESS,
                LoadState.Status.SUCCESS_NO_MORE -> {
                    if (srlRefreshLayout.isRefreshing) {
                        srlRefreshLayout.isRefreshing = false
                        srlRefreshLayout.isEnabled = false
                        drlSSOMsgRefreshLayout.setEnableRefresh(true)
                    } else if (drlSSOMsgRefreshLayout.state.isOpening) {
                        drlSSOMsgRefreshLayout.finishRefresh(true)
                    }

                    dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
                    if (drlSSOMsgRefreshLayout.visibility != View.VISIBLE) {
                        drlSSOMsgRefreshLayout.visibility = View.VISIBLE
                    }

                    handleSSOMsgDataUpdate(true)
                }

                LoadState.Status.SUCCESS_EMPTY -> {
                    if (srlRefreshLayout.isRefreshing) {
                        srlRefreshLayout.isRefreshing = false
                    } else if (drlSSOMsgRefreshLayout.state.isOpening) {
                        drlSSOMsgRefreshLayout.finishRefresh(true)
                        drlSSOMsgRefreshLayout.setEnableRefresh(false)
                        srlRefreshLayout.isEnabled = true
                    }

                    dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_EMPTY)
                    if (drlSSOMsgRefreshLayout.visibility == View.VISIBLE) {
                        drlSSOMsgRefreshLayout.visibility = View.INVISIBLE
                    }

                    handleSSOMsgDataUpdate(true)
                }

                LoadState.Status.FAILURE -> {
                    if (srlRefreshLayout.isRefreshing) {
                        srlRefreshLayout.isRefreshing = false
                    } else if (drlSSOMsgRefreshLayout.state.isOpening) {
                        drlSSOMsgRefreshLayout.finishRefresh(
                            300, false, false
                        )
                    }

                    if (mSSOMsgViewAdapter.itemCount > 0) {
                        dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
                        if (drlSSOMsgRefreshLayout.visibility != View.VISIBLE) {
                            drlSSOMsgRefreshLayout.visibility = View.VISIBLE
                        }
                    } else {
                        dslStatusLayout.showStatus(
                            if (it.peekData().isNoNetwork()) {
                                IStatusLayout.Status.STATUS_NO_NETWORK
                            } else {
                                IStatusLayout.Status.STATUS_FAILURE
                            }
                        )
                        if (drlSSOMsgRefreshLayout.visibility == View.VISIBLE) {
                            drlSSOMsgRefreshLayout.visibility = View.INVISIBLE
                        }
                    }

                    handleSSOMsgDataUpdate(false)
                }
            }
        })

        mViewModel.loadMoreState.observe(this, Observer {
            mSSOMsgViewAdapter.setLoadMoreState(it.peekData())
        })

        mViewModel.pagingTipsMessage.observe(this, Observer {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    showToast(msg)
                }
            }
        })

        mViewModel.mTipsMessageLD.observe(this, Observer {
            it.getDataIfNotHandled()?.let { msg ->
                if (msg.isNotEmpty()) {
                    showToast(msg)
                }
            }
        })
    }

    private fun handleSSOMsgDataUpdate(refreshSuccess: Boolean) {
        if (mUpdateSSOMsgDataFlag) {
            return
        }

        mUpdateSSOMsgDataFlag = true
        mCacheSSOMsgPagedList?.let {
            if (refreshSuccess) {
                mSSOMsgViewAdapter.submitList(it)
            }
            mCacheSSOMsgPagedList = null
        }
    }

    private fun observeChangedSSOApplicationMsg() {
        mViewModel.mChangedSSOApplicationMsgLD.observe(this, Observer { changedMsg ->
            launch(Dispatchers.Main) {
                val needRefreshMsgPosition = withContext(Dispatchers.IO) {
                    val msgList = mSSOMsgViewAdapter.currentList
                    msgList?.let {
                        it.forEachIndexed { index, msg ->
                            if (changedMsg.applicationId == msg.applicationId) {
                                msg.applicationStatus = changedMsg.applicationStatus
                                msg.msgUnread = false
                                return@withContext index
                            }
                        }
                    }
                    return@withContext -1
                }

                if (needRefreshMsgPosition >= 0) {
                    mSSOMsgViewAdapter.notifyItemChanged(needRefreshMsgPosition)
                }
            }
        })
    }
}
