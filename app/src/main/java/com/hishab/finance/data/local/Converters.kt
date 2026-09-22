package com.hishab.finance.data.local

import androidx.room.TypeConverter
import com.hishab.finance.data.local.entity.PayFrequency
import com.hishab.finance.data.local.entity.PaymentMethod
import com.hishab.finance.data.local.entity.Recurrence
import com.hishab.finance.data.local.entity.TxType

/** Enums are stored as their names so the database stays readable in any SQLite browser. */
class Converters {
    @TypeConverter fun txTypeToString(value: TxType): String = value.name
    @TypeConverter fun stringToTxType(value: String): TxType = TxType.valueOf(value)

    @TypeConverter fun payMethodToString(value: PaymentMethod): String = value.name
    @TypeConverter fun stringToPayMethod(value: String): PaymentMethod =
        runCatching { PaymentMethod.valueOf(value) }.getOrDefault(PaymentMethod.OTHER)

    @TypeConverter fun recurrenceToString(value: Recurrence): String = value.name
    @TypeConverter fun stringToRecurrence(value: String): Recurrence =
        runCatching { Recurrence.valueOf(value) }.getOrDefault(Recurrence.MONTHLY)

    @TypeConverter fun freqToString(value: PayFrequency): String = value.name
    @TypeConverter fun stringToFreq(value: String): PayFrequency =
        runCatching { PayFrequency.valueOf(value) }.getOrDefault(PayFrequency.IRREGULAR)
}
