package com.rodionov.testassets.xmodem

import com.google.common.io.ByteStreams
import java.io.File
import java.io.InputStream
import java.util.*

fun getInput(inputStream: InputStream): ByteArray?{
    if(inputStream.available() > 0) {
        return inputStream.readBytes()
    }
    return null
}

fun getByte(byteBuffer: ByteArray): Byte?{
    if(byteBuffer.isNotEmpty()) {
        return byteBuffer[0]
    }
    return null
}

fun writeToFile(path: String, bytesList: List<Byte>){
    val file = File(path)
    file.writeBytes(bytesList.toByteArray())
}

fun isSomethingCame(byteBuffer: ByteArray): Boolean {
    return byteBuffer.isNotEmpty()
}

fun receiveBytes(byteBuffer: ByteArray, count: Int): ByteArray {
    val bitList = LinkedList<Byte>()

    while (bitList.size <= count) {
        val rec = getByte(byteBuffer)
        if (rec != null) {
            bitList.add(rec)
        }
    }
    return bitList.toByteArray()
}

//fun getInputNextByte(inputStream: InputStream): Byte{
//    if(inputStream.available() > 0) {
//        return inputStream.read()
//    }
//    return null
//}