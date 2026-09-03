package com.omega7.messenger.network
interface AuthenticatedTransport{suspend fun send(groupId:String,encryptedPayload:ByteArray,idempotencyKey:String):Result<Unit>;suspend fun sync(cursor:String?):Result<SyncBatch>}
data class SyncBatch(val cursor:String?,val events:List<ByteArray>)
