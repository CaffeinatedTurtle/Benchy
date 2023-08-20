package com.connectiphy.bluetooth

object Utility {

    fun ByteArrayToMacAddrString(address: ByteArray):String{
        var macAddressStr = ByteArraytoHex(address,"%02X:")
        macAddressStr = macAddressStr.substring(0,17) // truncate trailing : and extraneous values if bytearray > 6
        return macAddressStr
    }
    fun MacAddrStringToByteArray(address: String): ByteArray{
        return hexStringToByteArray(address.replace(":",""))
    }
    fun ByteArraytoHex(bytes: ByteArray?, format: String): String {
        if (bytes != null) {
            val sb = StringBuilder()
            for (b in bytes) {
                sb.append(String.format(format, b))
            }
            return sb.toString()
        }
        return ""
    }

    fun hexStringToByteArray(input: String): ByteArray {
        var s = input.replace("\\s".toRegex(), "")
        val len = s.length
        val data = ByteArray(len / 2)

        var i = 0
        while (i < len) {
            data[i / 2] =
                ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }

        return data
    }




}
