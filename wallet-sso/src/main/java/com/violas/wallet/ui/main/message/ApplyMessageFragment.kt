package com.violas.wallet.ui.main.message

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import com.palliums.base.BaseFragment
import com.palliums.net.LoadState
import com.palliums.utils.DensityUtility
import com.palliums.utils.getColor
import com.palliums.widget.dividers.RecycleViewItemDividers
import com.palliums.widget.status.IStatusLayout
import com.violas.wallet.R
import com.violas.wallet.event.RefreshGovernorApplicationProgressEvent
import com.violas.wallet.ui.applyForLicence.ApplyForLicenceActivity
import kotlinx.android.synthetic.main.fragment_apply_message.*
import kotlinx.android.synthetic.main.fragment_apply_message_governor.*
import kotlinx.android.synthetic.main.fragment_apply_message_sso.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

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
        SSOApplicationMsgViewAdapter(retryCallback = { mViewModel.retry() }) {
            // TODO
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_apply_message
    }

    override fun onLazyInitView(savedInstanceState: Bundle?) {
        super.onLazyInitView(savedInstanceState)

        initSettings()
        observeGovernorLicenceStatus()
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
        // 刷新时原有数据会被清空，造成短暂的页面闪屏或页面空白
        // 刷新时先做个标记不更新数据，刷新完成后再做处理
        mUpdateSSOMsgDataFlag = false
        mViewModel.refresh()
    }

    private fun initSettings() {
        setStatusBarMode(true)

        srlRefreshLayout.isEnabled = false
        srlRefreshLayout.setOnRefreshListener {
            // 刷新时原有数据会被清空，造成短暂的页面闪屏或页面空白
            // 刷新时先做个标记不更新数据，刷新完成后再做处理
            mUpdateSSOMsgDataFlag = false
            mViewModel.refresh()
        }
    }

    private fun observeGovernorLicenceStatus() {
        mViewModel.mApplyGovernorLicenceStatusLD.observe(this, Observer { status ->
            if (status >= 4) {
                // 州长牌照已批准，显示SSO发币申请消息视图，隐藏州长牌照申请进度视图
                if (drlSSOMsgRefreshLayout == null) {
                    // 初始化SSO发币申请消息视图
                    try {
                        vsSSOMsgLayout.inflate()
                    } catch (ignore: Exception) {
                    }

                    // 初始化SSO发币申请消息视图的控件
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
                }
                clGovernorLicenceLayout?.visibility = View.GONE
                drlSSOMsgRefreshLayout.visibility = View.VISIBLE

                return@Observer
            }

            // 州长牌照还未批准，显示州长牌照申请进度视图，隐藏SSO发币申请消息视图
            if (clGovernorLicenceLayout == null) {
                // 初始州长牌照申请进度视图
                try {
                    vsGovernorLicenceLayout.inflate()
                } catch (ignore: Exception) {
                }
            }
            drlSSOMsgRefreshLayout?.visibility = View.GONE
            clGovernorLicenceLayout.visibility = View.VISIBLE

            // -1: no application; 2: not pass
            if (status == -1 || status == 2) {
                mivGovernorLicenceStatus.setEndDescText(R.string.desc_authentication_failed)
                mivGovernorLicenceStatus.setEndDescTextColor(getColor(R.color.def_text_warn))
                btnGovernorLicenceGoto.visibility = View.VISIBLE
                if (!btnGovernorLicenceGoto.hasOnClickListeners()) {
                    if (!EventBus.getDefault().isRegistered(this)) {
                        EventBus.getDefault().register(this)
                    }

                    btnGovernorLicenceGoto.setOnClickListener {
                        activity?.let {
                            ApplyForLicenceActivity.start(it)
                        }
                    }
                }
            } else {
                mivGovernorLicenceStatus.setEndDescText(R.string.desc_authenticating)
                mivGovernorLicenceStatus.setEndDescTextColor(getColor(R.color.color_00D1AF))
                btnGovernorLicenceGoto.visibility = View.GONE
            }
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
                LoadState.Status.RUNNING -> {
                    if (clGovernorLicenceLayout?.visibility != View.VISIBLE
                        && drlSSOMsgRefreshLayout?.visibility != View.VISIBLE
                        && dslStatusLayout.visibility != View.VISIBLE
                    ) {
                        // 首次加载
                        dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_LOADING)
                    }
                }

                LoadState.Status.SUCCESS,
                LoadState.Status.SUCCESS_NO_MORE -> {
                    handleSSOMsgDataUpdate(true)

                    if (srlRefreshLayout.isRefreshing) {
                        srlRefreshLayout.isRefreshing = false
                    } else if (drlSSOMsgRefreshLayout?.state?.isOpening == true) {
                        drlSSOMsgRefreshLayout.finishRefresh(true)
                    }
                    srlRefreshLayout.isEnabled = false
                    drlSSOMsgRefreshLayout?.setEnableRefresh(true)

                    dslStatusLayout.showStatus(IStatusLayout.Status.STATUS_NONE)
                }

                LoadState.Status.SUCCESS_EMPTY -> {
                    handleSSOMsgDataUpdate(true)

                    if (srlRefreshLayout.isRefreshing) {
                        srlRefreshLayout.isRefreshing = false
                    } else if (drlSSOMsgRefreshLayout?.state?.isOpening == true) {
                        drlSSOMsgRefreshLayout.finishRefresh(true)
                    }
                    srlRefreshLayout.isEnabled = true
                    drlSSOMsgRefreshLayout?.setEnableRefresh(false)

                    dslStatusLayout.showStatus(
                        if (mViewModel.mApplyGovernorLicenceStatusLD.value != null
                            && mViewModel.mApplyGovernorLicenceStatusLD.value!! < 4
                        )
                            IStatusLayout.Status.STATUS_NONE
                        else
                            IStatusLayout.Status.STATUS_EMPTY
                    )
                    if (drlSSOMsgRefreshLayout?.visibility == View.VISIBLE) {
                        drlSSOMsgRefreshLayout.visibility = View.INVISIBLE
                    }
                }

                LoadState.Status.FAILURE -> {
                    handleSSOMsgDataUpdate(false)

                    if (srlRefreshLayout.isRefreshing) {
                        srlRefreshLayout.isRefreshing = false
                    } else if (drlSSOMsgRefreshLayout?.state?.isOpening == true) {
                        drlSSOMsgRefreshLayout.finishRefresh(
                            300, false, false
                        )
                    }

                    if (clGovernorLicenceLayout?.visibility != View.VISIBLE
                        && drlSSOMsgRefreshLayout?.visibility != View.VISIBLE
                    ) {
                        // 首次加载失败, 或在状态视图刷新失败
                        srlRefreshLayout.isEnabled = true
                        dslStatusLayout.showStatus(
                            if (it.peekData().isNoNetwork())
                                IStatusLayout.Status.STATUS_NO_NETWORK
                            else
                                IStatusLayout.Status.STATUS_FAILURE
                        )
                    }
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

    private fun observeAccount() {
        mViewModel.mAccountLD.observe(this, Observer {
            mViewModel.start()
        })
    }
}
