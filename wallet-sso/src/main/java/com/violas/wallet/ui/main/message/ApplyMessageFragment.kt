package com.violas.wallet.ui.main.message

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.violas.wallet.R

class ApplyMessageFragment : Fragment() {

    companion object {
        fun newInstance() = ApplyMessageFragment()
    }

    private lateinit var viewModel: ApplyMessageViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.apply_message_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ApplyMessageViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
