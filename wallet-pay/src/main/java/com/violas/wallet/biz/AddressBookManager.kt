package com.violas.wallet.biz

import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AddressBookDo

class AddressBookManager {
    private val addressBookStorage by lazy {
        DataRepository.getAddressBookStorage()
    }

    fun loadAddressBook(coinType: Int): List<AddressBookDo> {
        return if (coinType == Int.MIN_VALUE) {
            addressBookStorage.findAll()
        } else {
            addressBookStorage.findByCoinType(coinType)
        }
    }

    fun install(addressBookDo: AddressBookDo): Long {
        return addressBookStorage.insert(addressBookDo)
    }

    fun remove(addressBook: AddressBookDo) {
        addressBookStorage.delete(addressBook)
    }

    fun isAddressAdded(coinNumber: Int, address: String): Boolean {
        return addressBookStorage.query(coinNumber, address) != null
    }

}