package com.violas.wallet.ui.main.quotes

import android.animation.ObjectAnimator
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.violas.wallet.R
import com.violas.wallet.ui.main.quotes.tokenList.TokenBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_quotes.*
import kotlinx.android.synthetic.main.fragment_quotes_content.*
import kotlinx.android.synthetic.main.fragment_quotes_did_not_open.*


class QuotesFragment : Fragment() {
    private val mConstraintSet = ConstraintSet()
    private val mConstraintSet2 = ConstraintSet()

    private val mQuotesViewModel by lazy {
        ViewModelProvider(this).get(QuotesViewModel::class.java)
    }

    private val mAllOrderAdapter by lazy {
        AllOrderAdapter()
    }
    private val mMeOrderAdapter by lazy {
        MeOrderAdapter()
    }

    private var mIvEntrustOthersAnim: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quotes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAnim()
        initViewEvent()
        handleIsEnableObserve()
        handleExchangeCoinObserve()
        handlePositiveChangeObserve()
        handleExchangeRateObserve()
        handleMeOrderObserve()
        handleAllOrderObserve()
        handleIvEntrustOthersAnimObserve()
        handleCurrentExchangeCoinObserve()
    }

    private fun handleIvEntrustOthersAnimObserve() {
        mQuotesViewModel.isShowMoreAllOrderLiveData.observe(viewLifecycleOwner, Observer {
            if (it) {
                mIvEntrustOthersAnim?.reverse()
            } else {
                mIvEntrustOthersAnim?.start()
            }
        })
    }

    private fun handleIsEnableObserve() {
        mQuotesViewModel.isEnable.observe(viewLifecycleOwner, Observer {
            try {
                layoutNotOpen.inflate()
            } catch (e: Exception) {
            }
            if (it) {
                layoutNotOpenRoot.visibility = View.GONE
                layoutQuotesContent.visibility = View.VISIBLE
            } else {
                layoutNotOpenRoot.visibility = View.VISIBLE
                layoutQuotesContent.visibility = View.GONE
            }
        })
    }

    private fun initAnim() {
        mConstraintSet.clone(layoutCoinConversion)
        mConstraintSet2.clone(context, R.layout.fragment_quotes_coin_anim)

        mIvEntrustOthersAnim = ObjectAnimator.ofFloat(ivEntrustOthers, "rotation", 0F, 180F)
            .setDuration(400)
//        mIvEntrustOthersAnim?.interpolator = OvershootInterpolator()
    }

    private fun initViewEvent() {
        ivConversion.setOnClickListener { mQuotesViewModel.clickPositiveChange() }
        recyclerViewMeOrder.adapter = mMeOrderAdapter
        recyclerViewAllOrder.adapter = mAllOrderAdapter
        layoutFromCoin.setOnClickListener { showTokenFragment(it) }
        layoutToCoin.setOnClickListener { showTokenFragment(it) }
        layoutEntrustOthers.setOnClickListener { mQuotesViewModel.clickShowMoreAllOrder() }
    }

    private fun showTokenFragment(view: View?) {
        val sheetDialogFragment = TokenBottomSheetDialogFragment.newInstance()
        sheetDialogFragment.show(childFragmentManager, mQuotesViewModel.getTokenList()) {
            sheetDialogFragment.dismiss()
            when (view?.id) {
                R.id.layoutFromCoin -> {
                    mQuotesViewModel.currentFormCoinLiveData.postValue(it)
                }
                R.id.layoutToCoin -> {
                    mQuotesViewModel.currentToCoinLiveData.postValue(it)
                }
            }
        }
    }

    private fun handleCurrentExchangeCoinObserve() {
        mQuotesViewModel.currentExchangeCoinLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                tvMeOrderEntrustNumber.text =
                    "${getString(R.string.label_number)}(${it.tokenUnit()})"
                tvMeOrderEntrustPrice.text = "${getString(R.string.label_price)}(${it.tokenUnit()})"
                tvAllOrderEntrustPrice.text =
                    "${getString(R.string.label_price)}(${it.tokenUnit()})"
            }
        })
    }

    private fun handleExchangeCoinObserve() {
        mQuotesViewModel.currentFormCoinLiveData.observe(viewLifecycleOwner, Observer {
            tvFromCoin.text = it.tokenName()
        })
        mQuotesViewModel.currentToCoinLiveData.observe(viewLifecycleOwner, Observer {
            tvToCoin.text = it.tokenName()
        })
    }

    private fun handlePositiveChangeObserve() {
        mQuotesViewModel.isPositiveChangeLiveData.observe(viewLifecycleOwner, Observer {
            val transition = AutoTransition()
            transition.duration = 500
            TransitionManager.beginDelayedTransition(layoutCoinConversion, transition)
            if (it) {
                mConstraintSet2.applyTo(layoutCoinConversion)
            } else {
                mConstraintSet.applyTo(layoutCoinConversion)
            }
        })
    }

    private fun handleExchangeRateObserve() {
        mQuotesViewModel.exchangeRateLiveData.observe(viewLifecycleOwner, Observer {
            tvParitiesInfo.text = it
        })
    }

    private fun handleMeOrderObserve() {
        mQuotesViewModel.meOrdersLiveData.observe(viewLifecycleOwner, Observer {
            mMeOrderAdapter.submitList(it)
            if (it == null || it.isEmpty()) {
                viewMeOrderNull.visibility = View.VISIBLE
                layoutMeOrder.visibility = View.GONE
            } else {
                viewMeOrderNull.visibility = View.GONE
                layoutMeOrder.visibility = View.VISIBLE
            }
        })
    }

    private fun handleAllOrderObserve() {
        mQuotesViewModel.allDisplayOrdersLiveData.observe(viewLifecycleOwner, Observer {
            mAllOrderAdapter.submitList(it)
            if (it == null || it.isEmpty()) {
                viewAllOrderNull.visibility = View.VISIBLE
                layoutAllOrder.visibility = View.GONE
            } else {
                viewAllOrderNull.visibility = View.GONE
                layoutAllOrder.visibility = View.VISIBLE
            }
        })
    }
}

