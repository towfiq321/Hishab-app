package com.hishab.finance.ui.navigation

/** Every route in one place. Arguments are simple query params so deep links stay readable. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val REPORTS = "reports"
    const val MORE = "more"

    const val ENTRY = "entry?type={type}&id={id}"
    fun entry(type: String, id: Long = -1L) = "entry?type=$type&id=$id"

    const val SOURCES = "sources"
    const val SOURCE_DETAIL = "source/{id}"
    fun sourceDetail(id: Long) = "source/$id"
    const val SOURCE_EDIT = "sourceEdit?id={id}"
    fun sourceEdit(id: Long = -1L) = "sourceEdit?id=$id"

    const val BUDGETS = "budgets"
    const val RECURRING = "recurring"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"
}
