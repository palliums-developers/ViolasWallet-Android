package com.violas.wallet.biz

import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AddressBookDo

class AddressBookManager {
    fun loadAddressBook(coinType: Int): List<AddressBookDo> {
        return if (coinType == Int.MIN_VALUE) {
            DataRepository.getAddressBookStorage().findAll()
        } else {
            DataRepository.getAddressBookStorage().findByCoinType(coinType)
        }
    }

    fun install(addressBookDo: AddressBookDo): Long {
        return DataRepository.getAddressBookStorage().insert(addressBookDo)
    }

    fun remove(addressBook:AddressBookDo){
        DataRepository.getAddressBookStorage().delete(addressBook)
    }
}