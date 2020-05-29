package com.violas.wallet.ui.backup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.palliums.utils.start
import com.violas.wallet.R
import kotlinx.android.synthetic.main.activity_show_mnemonic.*

/**
 * Created by elephant on 2019-10-21 15:17.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份助记词页面
 */
class ShowMnemonicActivity : BaseBackupMnemonicActivity() {

    companion object {
        const val INTENT_KET_JUST_SHOW = "JUST_SHOW"

        fun start(
            context: Activity,
            mnemonicWords: List<String>,
            requestCode: Int = -1
        ) {
            Intent(context, ShowMnemonicActivity::class.java).apply {
                val mnemonics = if (mnemonicWords is ArrayList<String>)
                    mnemonicWords
                else
                    ArrayList(mnemonicWords)
                putStringArrayListExtra(INTENT_KET_MNEMONIC_WORDS, mnemonics)
                putExtra(INTENT_KET_MNEMONIC_FROM, BackupMnemonicFrom.ONLY_SHOW_MNEMONIC)
                putExtra(INTENT_KET_JUST_SHOW, true)
            }.start(context, requestCode)
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_show_mnemonic
    }

    override fun getTitleStyle(): Int {
        return TITLE_STYLE_GREY_BACKGROUND
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.show_mnemonic_title)
        if (intent.getBooleanExtra(INTENT_KET_JUST_SHOW, false)) {
            vNextStep.visibility = View.GONE
        } else {
            vNextStep.setOnClickListener(this)
        }

        init()
    }

    private fun init() {
        val words: ArrayList<WordVO> = arrayListOf()
        mnemonicWords!!.forEachIndexed { index, word ->
            words.add(WordVO(word, index))
        }

        vSourceWords.layoutManager = GridLayoutManager(this, 3)
        vSourceWords.adapter = WordViewAdapter(words)
    }

    override fun onViewClick(view: View) {
        when (view) {
            vNextStep -> {
                getBackupIntent(ConfirmMnemonicActivity::class.java)
                    .start(this, BACKUP_REQUEST_CODE)
            }
        }
    }
}