package com.rodionov.testassets

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.rodionov.testassets.bluetooth.BLEScanClient
import com.rodionov.testassets.bluetooth.BluetoothXModemReceiver
import com.rodionov.testassets.bluetooth.BluetoothXModemTransmitter
import com.rodionov.testassets.xmodem.ControlChars
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    var xModemInputStream: InputStream  = ByteArrayInputStream(byteArrayOf(ControlChars.NAK.char))
    var xModemOutputStream = ByteArrayOutputStream()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btnTransmit.setOnClickListener {
            val bleScanClient = BLEScanClient(this, "94:7B:E7:00:9C:1A")
            val bluetoothXModemTransmitter = BluetoothXModemTransmitter(bleScanClient)
            Thread{
                try {
                    val inputStream: InputStream = assets.open("Bluetooch_Scaner_Stm32.bin")
//            val inputString = inputStream.bufferedReader().use{it.readText()}
                    val inputString = inputStream.readBytes()
                    Log.d("LOG_TAG", "size of source = ${inputString.size}")
                    bluetoothXModemTransmitter.startTransmission(
                        bytes = inputString,
                        byteBuffer = bleScanClient.byteBuffer
                    )
                    Log.d("LOG_TAG", "size of result = ${xModemOutputStream.size()}")
                } catch (e: Exception) {
                    Log.d("LOG_TAG", e.toString())
                }
            }.start()
        }

        btnReceive.setOnClickListener {
            val bleScanClient = BLEScanClient(this, "D4:11:A3:06:87:EC")
            val bluetoothXModemReceiver = BluetoothXModemReceiver(bleScanClient)
            Thread{
                try {
                    Log.d("LOG_TAG", "receive, mode C")
                    bluetoothXModemReceiver.start(ControlChars.C, bleScanClient.byteBuffer)
                } catch (e: Exception) {
                    Log.d("LOG_TAG", e.toString())
                }
            }.start()

        }


//        Log.d("LOG_TAG", "file name = ${file.name}")
//        Log.d("LOG_TAG", "file absolutePath = ${file.absolutePath}")
//        Log.d("LOG_TAG", "file canonicalPath = ${file.canonicalPath}")
//        Log.d("LOG_TAG", "file freeSpace = ${file.freeSpace}")
//        Log.d("LOG_TAG", "file parent = ${file.parent}")
//        Log.d("LOG_TAG", "file totalSpace = ${file.totalSpace}")
//        Log.d("LOG_TAG", "file canRead = ${file.canRead()}")
    }

}