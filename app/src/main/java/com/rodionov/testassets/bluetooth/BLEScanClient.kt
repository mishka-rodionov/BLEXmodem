package com.rodionov.testassets.bluetooth

import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.Executors

class BLEScanClient(
    private val context: Context,
    private val mac: String
) {

    companion object {
        private val COMMAND_BATTERY = byteArrayOf(0xFE.toByte(), 0xFF.toByte(), 0x01, 0x01)
        private val serviceUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val characteristicUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        private const val VOLTAGE_MAX = 4.2
        private const val VOLTAGE_20 = 3.8
        private const val VOLTAGE_MIN = 3.6
    }

    val title: String
        get() = "Bluetooth BLE scanner"

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var device = bluetoothAdapter.getRemoteDevice(mac)
    private var bluetoothGatt: BluetoothGatt? = null
    private val buffer = StringBuilder()
    var byteBuffer = ByteArray(0)
    private var characteristic: BluetoothGattCharacteristic? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                gatt.discoverServices()
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {

            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(serviceUID)
                if (service == null) {

                    return
                }
                characteristic = service.getCharacteristic(characteristicUID)
                gatt.setCharacteristicNotification(characteristic, true)

            } else {

            }
        }

        private fun isBatteryCommand(bytes: ByteArray): Boolean {
            return bytes[0].toInt() and 0xFF == 0xFE && bytes[1].toInt() and 0xFF == 0xFF && bytes[3].toInt() and 0xFF == 0x01
        }

        private fun getPercentageBattery(bytes: ByteArray): Int {
            val voltage = ((bytes[4].toInt() and 0xFF shl 8) or (bytes[5].toInt() and 0xFF)) / 100.0
            val percentage = when {
                voltage <= VOLTAGE_MIN -> 0.0
                voltage < VOLTAGE_20 -> ((voltage - VOLTAGE_MIN) / (VOLTAGE_20 - VOLTAGE_MIN) * 20)
                voltage < VOLTAGE_MAX -> 20 + ((voltage - VOLTAGE_20) / (VOLTAGE_MAX - VOLTAGE_20) * 80)
                else -> 100.0
            }.toBigDecimal().setScale(0, RoundingMode.HALF_UP).toInt()
            Log.i("Scanner", "Battery: $voltage V. $percentage %")
            infoLogger("Battery: $voltage V. $percentage %")
            return percentage
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic?
        ) {
            byteBuffer = characteristic?.value ?: byteArrayOf()
//            val byteBuffer = characteristic?.value ?: return
//            if (isBatteryCommand(byteBuffer)) {
//
//            } else {
//                val data = characteristic.getStringValue(0) ?: return
//                buffer.append(data)
//                if (buffer.endsWith("\r\n")) {
//                    buffer.split("\r\n").forEach {
//                        if (it.isNotEmpty()) {
//
//                        }
//                    }
//                    buffer.clear()
//                }
//            }
        }
    }



    private fun infoLogger(message: String) {
        Log.d("LOG_TAG", message)
    }

    fun connect() {
        infoLogger("connect to BLE")
        disconnect()
        if (!bluetoothAdapter.isEnabled) {
            bluetoothAdapter.enable()
        }
        bluetoothGatt = device.connectGatt(context, true, gattCallback)
    }

    fun disconnect() {
        infoLogger("disconnect BLE")
        try {
            bluetoothGatt?.close()
            bluetoothGatt?.disconnect()
            bluetoothGatt = null
            characteristic = null
        } catch (ignore: Throwable) {
        }
    }

    fun sendBatteryCommand() {
        if (characteristic == null || bluetoothGatt == null) return
        characteristic?.value = COMMAND_BATTERY
        bluetoothGatt?.writeCharacteristic(characteristic)
    }

    fun sendData(bytes: ByteArray) {
        Log.d("LOG_TAG", "client send data = ${bytes}")
        characteristic?.value = bytes
        bluetoothGatt?.writeCharacteristic(characteristic)
    }
}