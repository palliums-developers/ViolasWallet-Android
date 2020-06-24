package com.violas.wallet.biz.command

interface ICommand {
    fun exec()
}

interface ISingleCommand : ICommand {
    fun getIdentity(): String
}