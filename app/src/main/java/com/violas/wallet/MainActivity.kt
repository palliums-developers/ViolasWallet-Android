package com.violas.wallet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.smallraw.core.keystoreCompat.KeystoreCompat
import com.violas.wallet.repository.DataRepository
import org.junit.Assert

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
