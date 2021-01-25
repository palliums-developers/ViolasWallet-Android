package com.violas.wallet.ui.main.me

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.palliums.base.BaseFragment
import com.violas.wallet.R
import com.violas.wallet.ui.account.walletmanager.WalletManagerActivity
import com.violas.wallet.ui.addressBook.AddressBookActivity
import com.violas.wallet.ui.incentive.IncentiveWebActivity
import com.violas.wallet.ui.setting.SettingActivity
import com.violas.wallet.viewModel.WalletAppViewModel
import kotlinx.android.synthetic.main.fragment_me.*
import kotlinx.coroutines.launch

/**
 * 我的页面
 */
class MeFragment : BaseFragment() {

    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(requireContext())
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_me
    }

    override fun onLazyInitViewByResume(savedInstanceState: Bundle?) {
        super.onLazyInitViewByResume(savedInstanceState)
        mivWalletManagement.setOnClickListener(this)
        mivAddressBook.setOnClickListener(this)
        mivMiningReward.setOnClickListener(this)
        mivInvitationReward.setOnClickListener(this)
        mivSettings.setOnClickListener(this)
    }

    override fun onViewClick(view: View) {
        when (view.id) {
            R.id.mivWalletManagement -> {
                if (mWalletAppViewModel.isExistsAccount()) {
                    WalletManagerActivity.start(requireContext())
                } else {
                    showToast(R.string.common_tips_account_empty)
                }
            }

            R.id.mivAddressBook -> {
                AddressBookActivity.start(_mActivity)
            }

            R.id.mivMiningReward -> {
                launch {
                    IncentiveWebActivity.startIncentiveHomePage(requireContext())
                }
            }

            R.id.mivInvitationReward -> {
                launch {
                    IncentiveWebActivity.startInviteHomePage(requireContext())
                }
            }

            R.id.mivSettings -> {
                startActivity(Intent(_mActivity, SettingActivity::class.java))
            }
        }
    }
}