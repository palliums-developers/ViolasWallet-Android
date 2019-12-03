package com.violas.wallet.event

enum class HomePageType{
    Home,ApplyFor,Me
}

class HomePageModifyEvent(val index:HomePageType)