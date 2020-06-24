package com.rodionov.testassets.bluetooth

import android.util.Log
import com.rodionov.testassets.xmodem.*
import org.joda.time.DateTime
import org.joda.time.Minutes
import org.joda.time.Seconds
import java.util.*
import kotlin.experimental.inv

class BluetoothXModemReceiver(
    private val client: BLEScanClient
) {

    fun initTransmission(transmissionType: ControlChars) {
        client.sendData(byteArrayOf(transmissionType.char))
//        outputStream.write(byteArrayOf(transmissionType.char))
    }

    fun start(transmissionTypeChar: ControlChars, byteBuffer: ByteArray) {
        val now = DateTime.now()
        val end = now + Minutes.minutes(1)
        var nextTry = now + Seconds.seconds(10)
        var tries = 1
        var isGood = false

        initTransmission(transmissionTypeChar)

        // get header
        while (DateTime.now() > end && tries < 10) {
            if(isSomethingCame(byteBuffer)){
                isGood = true
                break
            }
            // send again if time elapsed
            if (DateTime.now() > nextTry) {
                nextTry += Seconds.seconds(10)
                tries++
                initTransmission(transmissionTypeChar)
            }
        }
        if(isGood)
            collectData(transmissionTypeChar, byteBuffer)
        else
            println("Cannot start transmission!")
    }

    fun collectData(transmissionTypeChar: ControlChars, byteBuffer: ByteArray){
        var packetNumber = 1
        val msg = LinkedList<Byte>()

        do{
            val rec = getData(packetNumber, transmissionTypeChar, byteBuffer)

            if(rec != null){
                if(packetNumber == 256){
                    packetNumber = 1
                }
                msg.addAll(rec)
                packetNumber++
            }
        }
        while(rec != null)
        client.sendData(byteArrayOf(ControlChars.ACK.char))
//        outputStream.write(byteArrayOf(ControlChars.ACK.char))
        writeToFile("./test.txt", msg)
    }

    fun getData(packetNumber: Int, transmissionTypeChar: ControlChars, byteBuffer: ByteArray): LinkedList<Byte>?{

        // if EOT arrived return null if SOH continue
        val firstHeaderByte = receiveBytes(byteBuffer, 1)

        if(firstHeaderByte[0] == ControlChars.EOT.char){
            return null
        }

        else if(firstHeaderByte[0] == ControlChars.SOH.char){

            // read header packet number, packet number completion
            val header = receiveBytes(byteBuffer, 2)

            if(transmissionTypeChar == ControlChars.C){
                val (controlSum, msg) = getDataWithAlgebraicSum(byteBuffer)
                val receivedControlSum = receiveBytes(byteBuffer, 1)

                // check control sum
                if(controlSum != receivedControlSum[0]){
                    // incorrect checksum
                    client.sendData(byteArrayOf(ControlChars.NAK.char))
//                    outputStream.write(byteArrayOf(ControlChars.NAK.char))
                    return getData(packetNumber, transmissionTypeChar, byteBuffer)

                }

                return if(!isHeaderProper(header, packetNumber)){
                    // incorrect checksum
                    client.sendData(byteArrayOf(ControlChars.NAK.char))
//                    outputStream.write(byteArrayOf(ControlChars.NAK.char))
                    getData(packetNumber, transmissionTypeChar, byteBuffer)
                }

                else{
                    client.sendData(byteArrayOf(ControlChars.ACK.char))
//                    outputStream.write(byteArrayOf(ControlChars.ACK.char))
                    return msg
                }
            }

            // for CRC
            else if(transmissionTypeChar == ControlChars.NAK){

                val (controlSum, msg) = getDataWithCRC(byteBuffer)
                val receivedControlSum = receiveBytes(byteBuffer, 2)

                // check control sum
//        val buffer = ByteBuffer.wrap(receivedControlSum)

                if(!controlSum.contentEquals(receivedControlSum)){
                    // incorrect checksum
                    client.sendData(byteArrayOf(ControlChars.NAK.char))
//                    outputStream.write(byteArrayOf(ControlChars.NAK.char))
                    return getData(packetNumber, transmissionTypeChar, byteBuffer)
                }

                return if(!isHeaderProper(header, packetNumber)){
                    // incorrect header
                    client.sendData(byteArrayOf(ControlChars.NAK.char))
//                    outputStream.write(byteArrayOf(ControlChars.NAK.char))
                    getData(packetNumber, transmissionTypeChar, byteBuffer)
                }

                else{
                    client.sendData(byteArrayOf(ControlChars.ACK.char))
//                    outputStream.write(byteArrayOf(ControlChars.ACK.char))
                    return msg
                }
            }
        }

        return null
    }


    fun getDataWithAlgebraicSum(byteBuffer: ByteArray): Pair<Byte, LinkedList<Byte>>{
        var algebraicSum: Byte = 0
        val msg = LinkedList<Byte>()

        while (msg.size <= 128) {
            val rec = getByte(byteBuffer)
            if (rec != null) {
                msg.add(rec)
                algebraicSum = algebraicSum.plus(rec).toByte()
            }
        }
        Log.d("LOG_TAG", "getDataWithAlgebraicSum msg = $msg")
        return Pair(algebraicSum, msg)
    }


    fun getDataWithCRC(byteBuffer: ByteArray): Pair<ByteArray, LinkedList<Byte>>{
        val msg = LinkedList<Byte>()

        while (msg.size <= 128) {
            val rec = getByte(byteBuffer)
            if (rec != null) {
                msg.add(rec)
            }
        }
        Log.d("LOG_TAG", "getDataWithCRC msg = $msg")
        val crc = CRC16.get(msg.toByteArray())
        return Pair(crc, msg)
    }


    fun isHeaderProper(msg: ByteArray, packetNumber: Int): Boolean {
        if (msg.size != 2)
            return false

        if(msg[0] != packetNumber.toByte())
            return false

        if (msg[0] != msg[1].inv())
            return false

        return true
    }

}