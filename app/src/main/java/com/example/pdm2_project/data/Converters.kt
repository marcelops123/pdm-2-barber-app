package com.example.pdm2_project.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromFinancialType(t: FinancialType): String = t.name

    @TypeConverter
    fun toFinancialType(value: String): FinancialType = FinancialType.valueOf(value)
}
