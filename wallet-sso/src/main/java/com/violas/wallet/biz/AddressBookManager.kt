package com.violas.wallet.biz

import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AddressBookDo

class AddressBookManager {
    fun loadAddressBook(coinType: Int): List<AddressBookDo> {
        return if (coinType == -1) {
            DataRepository.getAddressBookStorage().findAll()
        } else {
            DataRepository.getAddressBookStorage().findByCoinType(coinType)
        }
    }

    fun install(addressBookDo: AddressBookDo): Long {
        return DataRepository.getAddressBookStorage().insert(addressBookDo)
    }
}