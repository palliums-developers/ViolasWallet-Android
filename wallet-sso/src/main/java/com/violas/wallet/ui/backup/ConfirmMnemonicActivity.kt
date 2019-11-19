package com.violas.wallet.ui.backup

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.violas.wallet.BuildConfig
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.ui.main.MainActivity
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

    private lateinit var words: ArrayList<MnemonicWordModel>
    private lateinit var adapter: TagAdapter<MnemonicWordModel>
    private lateinit var wordsSel: ArrayList<MnemonicWordModel>
    private lateinit var adapterSel: TagAdapter<MnemonicWordModel>

    override fun getLayoutResId(): Int {
        return R.layout.activity_confirm_mnemonic
    }

    override fun getPageStyle(): Int {
        return TITLE_STYLE_PLIGHT_TITLE_SLIGHT_CONTENT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.confirm_mnemonic_title)
        tv_confirm_mnemonic_complete.setOnClickListener(this)

        init()
    }

    private fun init() {
        tv_confirm_mnemonic_complete.isEnabled = false
        tv_confirm_mnemonic_complete.setBackgroundResource(R.drawable.shape_rectangle_gray)

        words = arrayListOf()
        wordsSel = arrayListOf()
        mnemonicWords!!.forEachIndexed { index, word ->
            words.add(MnemonicWordModel(word, index))
        }
        if (!BuildConfig.DEBUG) {
            words.shuffle()
        }

        adapter = object : TagAdapter<MnemonicWordModel>(words) {
            override fun getView(
                parent: FlowLayout,
                position: Int,
                wordModel: MnemonicWordModel
            ): View {
                val view = LayoutInflater.from(this@ConfirmMnemonicActivity)
                    .inflate(R.layout.item_tag_mnemonic, fl_confirm_mnemonic_words, false)

                val word = view.findViewById<TextView>(R.id.tv_word)
                word.isEnabled = !wordsSel.contains(wordModel)
                word.text = wordModel.word

                return view
            }
        }
        fl_confirm_mnemonic_words.adapter = adapter
        fl_confirm_mnemonic_words.setOnTagClickListener { view, position, parent ->

            if (!wordsSel.contains(words[position])) {
                wordsSel.add(words[position])
                adapterSel.notifyDataChanged()

                adapter.notifyDataChanged()

                if (words.size == wordsSel.size) {
                    tv_confirm_mnemonic_complete.isEnabled = true
                    tv_confirm_mnemonic_complete.setBackgroundResource(R.drawable.selector_btn_bg_primary)
                }
            }

            false
        }

        adapterSel = object : TagAdapter<MnemonicWordModel>(wordsSel) {
            override fun getView(
                parent: FlowLayout,
                position: Int,
                wordModel: MnemonicWordModel
            ): View {
                val view = LayoutInflater.from(this@ConfirmMnemonicActivity)
                    .inflate(R.layout.item_tag_mnemonic_sel, fl_confirm_mnemonic_words_sel, false)
                val word = view.findViewById<TextView>(R.id.tv_word)
                word.text = wordModel.word
                return view
            }
        }
        fl_confirm_mnemonic_words_sel.adapter = adapterSel
        fl_confirm_mnemonic_words_sel.setOnTagClickListener { view, position, parent ->

            if (words.size == wordsSel.size) {
                tv_confirm_mnemonic_complete.isEnabled = false
                tv_confirm_mnemonic_complete.setBackgroundResource(R.drawable.shape_rectangle_gray)
            }

            wordsSel.remove(wordsSel[position])
            adapterSel.notifyDataChanged()

            adapter.notifyDataChanged()

            false
        }
    }

    override fun onViewClick(view: View) {
        when (view) {
            tv_confirm_mnemonic_complete -> {
                // 验证助记词顺序
                if (checkMnemonic()) {
                    if (mnemonicFrom != BackupMnemonicFrom.OTHER_WALLET) {
                        // 如果是备份身份钱包的助记词，需要存储备份结果到本地
                        val accountManager = AccountManager()
                        if (!accountManager.isIdentityMnemonicBackup()) {
                            accountManager.setIdentityMnemonicBackup()
                        }

                        // 如果是来自创建身份，完成后需要跳转到App首页
                        if (mnemonicFrom == BackupMnemonicFrom.CREATE_IDENTITY) {
                            MainActivity.start(this)
                            finish()
                            return
                        }
                    }

                    // 如果是来自备份身份钱包和创建钱包，完成后需要返回成功结果
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    private fun checkMnemonic(): Boolean {
        if (words.size != wordsSel.size) {
            showToast(R.string.confirm_mnemonic_tips_01)
            return false
        }

        wordsSel.forEachIndexed { index, model ->
            if (index != model.index) {
                showToast(R.string.confirm_mnemonic_tips_01)
                return false
            }
        }

        return true
    }
}