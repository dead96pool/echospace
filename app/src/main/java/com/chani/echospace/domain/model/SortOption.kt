// domain/model/SortOption.kt
package com.chani.echospace.domain.model

enum class SortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    DATE_NEWEST("Newest First"),
    DATE_OLDEST("Oldest First")
}
