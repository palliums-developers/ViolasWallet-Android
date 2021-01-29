package com.violas.wallet.ui.backup

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.palliums.content.App
import com.violas.wallet.BuildConfig
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.BackupIdentityMnemonicEvent
import com.violas.wallet.ui.main.MainActivity
import kotlinx.android.synthetic.main.activity_confirm_mnemonic.*
import org.greenrobot.eventbus.EventBus

/**
 * Created by elephant on 2019-10-21 15:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 确认助记词页面
 */
class ConfirmMnemonicActivity : BaseBackupMnemonicActivity() {

    private lateinit var adapterSource: WordViewAdapter
    private lateinit var adapterConfirmed: WordViewAdapter

    override fun getLayoutResId(): Int {
        return R.layout.activity_confirm_mnemonic
    }

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_LIGHT_MODE_PRIMARY_TOP_BAR
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.mnemonic_backup_confirm_title)
        vComplete.setOnClickListener(this)

        init()
    }

    private fun init() {
        val wordsSource: ArrayList<WordVO> = arrayListOf()
        val wordsConfirmed: ArrayList<WordVO> = arrayListOf()
        mnemonicWords!!.forEachIndexed { index, word ->
            wordsSource.add(WordVO(word, index))
        }

        if (BuildConfig.SHUFFLE_MNEMONIC) {
            wordsSource.shuffle()
        }

        adapterSource = WordViewAdapter(wordsSource, false) { position, word ->
            if (!adapterConfirmed.getDataList().contains(word)) {
                word.confirmed = true
                adapterSource.notifyDataSetChanged()

                adapterConfirmed.addData(word, false)
                adapterConfirmed.notifyDataSetChanged()
            }
        }
        vSourceWords.layoutManager = GridLayoutManager(this, 3)
        vSourceWords.adapter = adapterSource

        adapterConfirmed = WordViewAdapter(wordsConfirmed, true) { position, word ->

            if (adapterConfirmed.getDataList().size == adapterSource.getDataList().size) {
                vTips.visibility = View.GONE
            }

            word.confirmed = false
            adapterConfirmed.removeData(word, false)
            adapterConfirmed.notifyDataSetChanged()

            adapterSource.notifyDataSetChanged()
        }
        vConfirmedWords.layoutManager = GridLayoutManager(this, 3)
        vConfirmedWords.adapter = adapterConfirmed
    }

    override fun onViewClick(view: View) {
        when (view) {
            vComplete -> {
                // 验证助记词顺序
                if (checkMnemonic()) {
                    if (mnemonicFrom != BackupMnemonicFrom.CREATE_OTHER_WALLET) {
                        // 如果是身份钱包，需要存储备份结果到本地
                        val accountManager = AccountManager()
                        if (!accountManager.isIdentityMnemonicBackup()) {
                            accountManager.setIdentityMnemonicBackup()

                            // 如果是来自备份身份钱包的助记词，需要通知钱包首页关闭安全设置视图
                            if (mnemonicFrom == BackupMnemonicFrom.BACKUP_IDENTITY_WALLET) {
                                EventBus.getDefault().post(BackupIdentityMnemonicEvent())
                            }
                        }

                        // 如果是来自创建身份钱包，完成后需要跳转到App首页
                        if (mnemonicFrom == BackupMnemonicFrom.CREATE_IDENTITY_WALLET) {
                            MainActivity.start(this)
                            App.finishAllActivity()
                            return
                        }
                    }

                    // 如果是来自备份身份钱包或创建非身份钱包，完成后需要返回成功结果
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    private fun checkMnemonic(): Boolean {
        val wordsConfirmed = adapterConfirmed.getDataList()
        val wordsSource = mnemonicWords!!
        if (wordsConfirmed.size != wordsSource.size) {
            showToast(R.string.mnemonic_backup_confirm_tips_1)
            return false
        }

        wordsConfirmed.forEachIndexed { index, vo ->
            if (vo.word != wordsSource[index]) {
                showToast(R.string.mnemonic_backup_confirm_tips_2)
                vTips.visibility = View.VISIBLE
                return false
            }
        }

        return true
    }
}