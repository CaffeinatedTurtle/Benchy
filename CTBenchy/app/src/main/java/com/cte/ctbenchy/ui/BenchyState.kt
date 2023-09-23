package com.cte.ctbenchy.ui

data class BenchyState (
    val ledMask:Byte = 0x00,
    val mode :Byte = 0x00,
    val throttle:Int = 0,
    val rudder:Int = 0,
    val rudderTrim:Int = 0,
    val throttleTrim :Int= 0
){


}