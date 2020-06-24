package com.rodionov.testassets.bluetooth

import android.util.Log
import com.rodionov.testassets.xmodem.CRC16
import com.rodionov.testassets.xmodem.ControlChars
import com.rodionov.testassets.xmodem.getByte
import kotlin.experimental.inv

class BluetoothXModemTransmitter(
    private val client: BLEScanClient
) {

    fun startTransmission(bytes: ByteArray, byteBuffer: ByteArray) {
        val packets = bytes.asIterable().chunked(128)
        var crc = false

        while (true) {
            if (byteBuffer.isNotEmpty()) {
                crc = getByte(byteBuffer) == ControlChars.NAK.char
                break
            }
        }
        var counter = 1

        for (packet in packets) {
            if (counter == 256) {
                counter = 1
            }

            if (send(byteBuffer, counter, packet, crc) == 1) {
                println("Transmission canceled")
                return
            }
            counter++
        }
        // end of file
        // check toInt
        client.sendData(byteArrayOf(ControlChars.EOT.char))
//        outputStream.write(byteArrayOf(ControlChars.EOT.char))
    }

    fun send(
        byteBuffer: ByteArray,
        counter: Int,
        packet: List<Byte>,
        crc: Boolean
    ): Int {
        sendHeader(counter)
        while (true) {
            if (crc)
                transmissionWithCRC(packet)
            else
                transmissionWithSum(packet)

            val rec = getByte(byteBuffer)

            if (rec == ControlChars.NAK.char)
                continue

            if (rec == ControlChars.ACK.char)
                break

            if (rec == ControlChars.CAN.char)
                return 1
        }
        return 0
    }


    fun sendHeader(packetNumber: Int) {
        val header: ByteArray =
            byteArrayOf(ControlChars.SOH.char, packetNumber.toByte(), packetNumber.toByte().inv())
        client.sendData(header)
//        outputStream.write(header)
    }

    fun transmissionWithSum(packet: List<Byte>) {
        var checksum: Byte = 0

        for (byte in packet) {
            checksum = checksum.plus(byte).toByte()
        }
        Log.d("LOG_TAG", "transmissionWithSum send ${packet.toByteArray()}")
        client.sendData(packet.toByteArray())
        client.sendData(byteArrayOf(checksum))
//        outputStream.write(packet.toByteArray())
//        outputStream.write(byteArrayOf(checksum))
    }

    fun transmissionWithCRC(packet: List<Byte>) {
        val checksum = CRC16.get(packet.toByteArray())
        Log.d("LOG_TAG", "transmissionWithCRC send ${packet.toByteArray()}")
        client.sendData(packet.toByteArray())
        client.sendData(checksum)
//        outputStream.write(packet.toByteArray())
//        outputStream.write(checksum)
    }

}