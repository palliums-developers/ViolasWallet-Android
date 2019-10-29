package com.violas.wallet.ui.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.violas.wallet.R
import com.violas.wallet.utils.start
import com.zhy.view.flowlayout.FlowLayout
import com.zhy.view.flowlayout.TagAdapter
import kotlinx.android.synthetic.main.activity_show_mnemonic.*

/**
 * Created by elephant on 2019-10-21 15:17.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份助记词页面
 */
class ShowMnemonicActivity : BaseBackupMnemonicActivity() {

    lateinit var words: ArrayList<MnemonicWordModel>
    lateinit var adapter: TagAdapter<MnemonicWordModel>

    override fun getLayoutResId(): Int {
        return R.layout.activity_show_mnemonic
    }

    override fun getTitleStyle(): Int {
        return TITLE_STYLE_GREY_BACKGROUND
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.show_mnemonic_title)
        tv_show_mnemonic_next_step.setOnClickListener(this)

        init()
    }

    private fun init() {
        words = arrayListOf()
        mnemonicWords!!.forEachIndexed { index, word ->
            words.add(MnemonicWordModel(word, index))
        }

        adapter = object : TagAdapter<MnemonicWordModel>(words) {
            override fun getView(
                parent: FlowLayout,
                position: Int,
                wordModel: MnemonicWordModel
            ): View {
                val view = LayoutInflater.from(this@ShowMnemonicActivity)
                    .inflate(R.layout.item_tag_mnemonic, fl_show_mnemonic_words, false)

                val word = view.findViewById<TextView>(R.id.tv_word)
                word.text = wordModel.word

                return view
            }
        }
        fl_show_mnemonic_words.adapter = adapter
    }

    override fun onViewClick(view: View) {
        when (view) {
            tv_show_mnemonic_next_step -> {
                getBackupIntent(ConfirmMnemonicActivity::class.java)
                    .start(this, BACKUP_REQUEST_CODE)
            }
        }
    }
}