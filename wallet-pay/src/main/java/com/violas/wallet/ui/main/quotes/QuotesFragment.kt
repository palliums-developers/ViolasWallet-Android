package com.violas.wallet.ui.main.quotes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.violas.wallet.R
import kotlinx.android.synthetic.main.fragment_quotes.*

class QuotesFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quotes, container, false)
    }

    private val mConstraintSet = ConstraintSet()
    private val mConstraintSet2 = ConstraintSet()
    private var isBTCToViolas = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mConstraintSet.clone(layoutCoinConversion)
        mConstraintSet2.clone(context, R.layout.fragment_quotes_coin_anim)

        ivConversion.setOnClickListener {
            val transition = AutoTransition()
            transition.duration = 500
            TransitionManager.beginDelayedTransition(layoutCoinConversion, transition)
            if (isBTCToViolas) {
                mConstraintSet2.applyTo(layoutCoinConversion)
            } else {
                mConstraintSet.applyTo(layoutCoinConversion)
            }
            isBTCToViolas = !isBTCToViolas
        }
    }
}