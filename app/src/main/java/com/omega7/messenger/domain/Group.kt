package com.omega7.messenger.domain

data class Group(
    val id: String,
    val name: String,
    val memberCount: Int,
    val maxMembers: Int = 7
) {
    init {
        require(memberCount in 1..maxMembers)
        require(maxMembers == 7)
    }
}
