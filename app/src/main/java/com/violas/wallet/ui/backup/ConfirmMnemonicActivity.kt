package com.violas.wallet.ui.backup

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.violas.wallet.R
import com.zhy.view.flowlayout.FlowLayout
import com.zhy.view.flowlayout.TagAdapter
import kotlinx.android.synthetic.main.activity_confirm_mnemonic.*

/**
 * Created by elephant on 2019-10-21 15:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 确认助记词页面
 */
class ConfirmMnemonicActivity : BaseBackupMnemonicActivity() {

    lateinit var words: ArrayList<MnemonicModel>
    lateinit var adapter: TagAdapter<MnemonicModel>
    lateinit var wordsSel: ArrayList<MnemonicModel>
    lateinit var adapterSel: TagAdapter<MnemonicModel>

    override fun getLayoutResId(): Int {
        return R.layout.activity_confirm_mnemonic
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.confirm_mnemonic_title)
        tv_confirm_mnemonic_complete.setOnClickListener(this)

        init()
    }

    private fun init() {
        Log.e("ConfirmMnemonicActivity", "mnemonic words: ${mnemonicWords!!.joinToString(" ")}")

        tv_confirm_mnemonic_complete.isEnabled = false

        words = ArrayList()
        wordsSel = ArrayList()
        mnemonicWords!!.forEachIndexed { index, word ->
            words.add(MnemonicModel(word, index))
        }

        adapter = object : TagAdapter<MnemonicModel>(words) {
            override fun getView(
                parent: FlowLayout,
                position: Int,
                model: MnemonicModel
            ): View {
                val view = LayoutInflater.from(this@ConfirmMnemonicActivity)
                    .inflate(R.layout.item_tag_mnemonic, fl_confirm_mnemonic_words, false)

                val word = view.findViewById<TextView>(R.id.tv_word)
                word.isEnabled = !wordsSel.contains(model)
                word.text = model.word
                model.index = position

                return view
            }
        }
        fl_confirm_mnemonic_words.adapter = adapter
        fl_confirm_mnemonic_words.setOnTagClickListener { view, position, parent ->

            if (!wordsSel.contains(words[position])) {
                wordsSel.add(words[position])
                adapterSel.notifyDataChanged()

                /*(((view as ViewGroup).getChildAt(0) as ViewGroup).getChildAt(0) as TextView)
                    .isEnabled = false*/
                adapter.notifyDataChanged()
            }

            if (words.size == wordsSel.size) {
                tv_confirm_mnemonic_complete.isEnabled = true
            }

            false
        }


        adapterSel = object : TagAdapter<MnemonicModel>(wordsSel) {
            override fun getView(
                parent: FlowLayout,
                position: Int,
                model: MnemonicModel
            ): View {
                val view = LayoutInflater.from(this@ConfirmMnemonicActivity)
                    .inflate(R.layout.item_tag_mnemonic_sel, fl_confirm_mnemonic_words_sel, false)
                val word = view.findViewById<TextView>(R.id.tv_word)
                word.text = model.word
                return view
            }
        }
        fl_confirm_mnemonic_words_sel.adapter = adapterSel
        fl_confirm_mnemonic_words_sel.setOnTagClickListener { view, position, parent ->

            wordsSel.remove(wordsSel[position])
            adapterSel.notifyDataChanged()

            adapter.notifyDataChanged()

            tv_confirm_mnemonic_complete.isEnabled = false

            false
        }

    }

    override fun onViewClick(view: View) {
        when (view) {
            tv_confirm_mnemonic_complete -> {
                // 验证助记词顺序
                if (checkMnemonic()) {
                    // TODO 验证结果通知或回调
                }
            }
        }
    }

    private fun checkMnemonic(): Boolean {
        if (words.size != wordsSel.size) {
            Toast.makeText(this, R.string.confirm_mnemonic_tips_01, Toast.LENGTH_LONG).show()
            return false
        }

        words.forEachIndexed { index, mnemonicModel ->
            if (mnemonicModel != wordsSel[index]) {
                Toast.makeText(this, R.string.confirm_mnemonic_tips_01, Toast.LENGTH_LONG).show()
                return false
            }
        }

        return true
    }
}