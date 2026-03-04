package com.zackjp.devicedx.model

import java.math.BigDecimal
import java.math.RoundingMode


@JvmInline
value class Bytes private constructor(val bytes: Long) {
    companion object {
        fun Long.asDataUnit(dataUnit: DataUnit): Bytes = Bytes(
            when (dataUnit) {
                DataUnit.BYTE -> this
                DataUnit.KILOBYTE -> this * KB_SIZE.toLong()
                DataUnit.MEGABYTE -> this * MB_SIZE.toLong()
                DataUnit.GIGABYTE -> this * GB_SIZE.toLong()
                DataUnit.TERABYTE -> this * TB_SIZE.toLong()
            }
        )
    }

    val bestDisplayableUnit: Pair<BigDecimal, DataUnit>
        get() {
            val bigDecimalValue = bytes.toBigDecimal()
            val unit = when {
                bigDecimalValue >= TB_SIZE -> DataUnit.TERABYTE
                bigDecimalValue >= GB_SIZE -> DataUnit.GIGABYTE
                bigDecimalValue >= MB_SIZE -> DataUnit.MEGABYTE
                bigDecimalValue >= KB_SIZE -> DataUnit.KILOBYTE
                else -> DataUnit.BYTE
            }

            val unitValue = when (unit) {
                DataUnit.BYTE -> bigDecimalValue
                DataUnit.KILOBYTE -> bigDecimalValue.divide(KB_SIZE, 2, RoundingMode.HALF_UP)
                DataUnit.MEGABYTE -> bigDecimalValue.divide(MB_SIZE, 2, RoundingMode.HALF_UP)
                DataUnit.GIGABYTE -> bigDecimalValue.divide(GB_SIZE, 2, RoundingMode.HALF_UP)
                DataUnit.TERABYTE -> bigDecimalValue.divide(TB_SIZE, 2, RoundingMode.HALF_UP)
            }
            return Pair(unitValue, unit)
        }
}

enum class DataUnit(
    val displayString: String,
) {
    BYTE("B"),
    KILOBYTE("KB"),
    MEGABYTE("MB"),
    GIGABYTE("GB"),
    TERABYTE("TB"),

}

private fun getBytesString(bytes: Long): Pair<BigDecimal, String> {
    val bigDecimalValue = bytes.toBigDecimal()
    val unitString = when {
        bigDecimalValue >= TB_SIZE -> "TB"
        bigDecimalValue >= GB_SIZE -> "GB"
        bigDecimalValue >= MB_SIZE -> "MB"
        bigDecimalValue >= KB_SIZE -> "KB"
        else -> "B"
    }
    val unitValue = when (unitString) {
        "B" -> bigDecimalValue
        "KB" -> bigDecimalValue.divide(KB_SIZE, 2, RoundingMode.HALF_UP)
        "MB" -> bigDecimalValue.divide(MB_SIZE, 2, RoundingMode.HALF_UP)
        "GB" -> bigDecimalValue.divide(GB_SIZE, 2, RoundingMode.HALF_UP)
        else -> bigDecimalValue.divide(TB_SIZE, 2, RoundingMode.HALF_UP)
    }
    return Pair(unitValue, unitString)
}

private val B_SIZE = 1.toBigDecimal()
private val KB_SIZE = 1024.toBigDecimal()
private val MB_SIZE = 1_048_576.toBigDecimal()
private val GB_SIZE = 1_073_741_824.toBigDecimal()
private val TB_SIZE = 1_099_511_627_776.toBigDecimal()
