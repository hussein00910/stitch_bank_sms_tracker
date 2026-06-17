package com.stitch.bank.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val emoji: String,
    val colorHex: String
)

object DefaultCategories {
    val seed = listOf(
        CategoryEntity(name = "بقالة وسوبر ماركت", emoji = "🛒", colorHex = "#006C47"),
        CategoryEntity(name = "مطاعم وكافيهات", emoji = "🍔", colorHex = "#BA1A1A"),
        CategoryEntity(name = "فواتير وخدمات", emoji = "🧾", colorHex = "#7A4F01"),
        CategoryEntity(name = "نقل ووقود", emoji = "⛽", colorHex = "#005691"),
        CategoryEntity(name = "صحة وصيدلية", emoji = "💊", colorHex = "#7B1FA2"),
        CategoryEntity(name = "تحويلات", emoji = "🔁", colorHex = "#455A64"),
        CategoryEntity(name = "راتب ودخل", emoji = "💰", colorHex = "#00346F"),
        CategoryEntity(name = "أخرى", emoji = "📦", colorHex = "#757575")
    )
}
