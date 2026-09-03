package com.omega7.messenger.domain

interface MessageRepository {
    fun list(): List<Message>
    fun append(message: Message)
    fun clear()
}
