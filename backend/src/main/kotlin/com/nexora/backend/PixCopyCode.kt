package com.nexora.backend

import java.text.Normalizer
import java.util.Locale

object PixCopyCode {
    fun build(platformPixKey: String, amountCents: Long, txid: String): String {
        val merchantAccount = tag("00", "br.gov.bcb.pix") +
            tag("01", platformPixKey.trim()) +
            tag("02", "NEXORA $txid")
        val payloadWithoutCrc = tag("00", "01") +
            tag("01", "12") +
            tag("26", merchantAccount) +
            tag("52", "0000") +
            tag("53", "986") +
            tag("54", "%.2f".format(Locale.US, amountCents / 100.0)) +
            tag("58", "BR") +
            tag("59", cleanText("NEXORA", 25)) +
            tag("60", cleanText("SAO PAULO", 15)) +
            tag("62", tag("05", cleanTxid(txid))) +
            "6304"
        return payloadWithoutCrc + crc16(payloadWithoutCrc)
    }

    private fun tag(id: String, value: String): String {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.size <= 99) { "Pix EMV tag $id is too long." }
        return id + bytes.size.toString().padStart(2, '0') + value
    }

    private fun cleanTxid(value: String): String =
        value.uppercase(Locale.US)
            .filter { it.isLetterOrDigit() }
            .ifBlank { "NEXORA" }
            .take(25)

    private fun cleanText(value: String, max: Int): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .uppercase(Locale.US)
        return normalized
            .filter { it in 'A'..'Z' || it in '0'..'9' || it == ' ' || it == '.' || it == '-' }
            .trim()
            .ifBlank { "NEXORA" }
            .take(max)
    }

    private fun crc16(payload: String): String {
        var crc = 0xFFFF
        payload.toByteArray(Charsets.UTF_8).forEach { raw ->
            crc = crc xor ((raw.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor 0x1021) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }
        return crc.toString(16).uppercase(Locale.US).padStart(4, '0')
    }
}
