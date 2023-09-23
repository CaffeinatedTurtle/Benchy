package com.cte.ctbenchy

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.cte.bluetooth.BluetoothHandler
import com.cte.bluetooth.BluetoothViewModel
import com.cte.bluetooth.IBluetoothListener
import java.util.UUID

class BenchGattImpl() : IBluetoothListener {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHandler: BluetoothHandler? = null
    private lateinit var context: Activity
    private lateinit var model: BluetoothViewModel


    fun initialize(ctx:Context){

    }

    companion object{
        val  SERVICE_UUID = UUID.fromString("7507cee3-db32-4e5a-bd6b-96b62887129e")
        val  RUDDER_CHARACTERISTIC_UUID = UUID.fromString("d7c1861c-beff-430f-9a72-fc05c6cc997d")
        val THROTTLE_CHARACTERISTIC_UUID = UUID.fromString("87607759-37d1-41b5-b2c8-c44b7c746083")
        val MODE_CHARACTERISTIC_UUID = UUID.fromString("16d68508-2fd4-40a9-ba61-aac41cb81e45")
       val LED_CHARACTERISTIC_UUID = UUID.fromString("3a84a192-d522-46ef-b7c8-36b9fc062490")
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    override fun onConnect() {
        TODO("Not yet implemented")
    }

    override fun onDisconnect() {
        TODO("Not yet implemented")
    }
}