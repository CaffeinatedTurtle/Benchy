package com.cte.ctbenchy.ui

data class BenchyState (
    var ledMask:Byte = 0x00,
    var mode :Byte = 0x00,
    var throttle:Int = 0,
    var rudder:Int = 0,
    var rudderTrim:Int = 0,
    var throttleTrim :Int= 0
){


}