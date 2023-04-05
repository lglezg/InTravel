package com.lglez.intravel.utils

object Extensions {
    fun String.isEmail():Boolean{
        return this.matches(Regex("[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"))
    }

    fun String.isNumber():Boolean{
        return this.matches(Regex("[0-9]+"))
    }

    fun String.isDecimalNumber():Boolean{
        return  this.matches(Regex("[0-9]+.[0-9][0-9]"))
    }
}