package com.violas.wallet.biz

import com.violas.wallet.repository.ServiceLocator
import com.violas.wallet.repository.database.entity.AddressBookDo

class AddressBookManager {
    fun loadAddressBook(coinType: Int): List<AddressBookDo> {
        return if (coinType == -1) {
            ServiceLocator.getAddressBookStorage().findAll()
        } else {
            ServiceLocator.getAddressBookStorage().findByCoinType(coinType)
        }
    }

    fun install(addressBookDo: AddressBookDo): Long {
        return ServiceLocator.getAddressBookStorage().insert(addressBookDo)
    }
}