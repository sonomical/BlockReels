package com.blockreels.app.engine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Main-thread submissions retain only the newest pending signal. Node handles stay in read(). */
internal class ScreenScanQueue<S, R>(
    scope: CoroutineScope,
    worker: CoroutineDispatcher,
    read: (S, isCurrent: () -> Boolean) -> R,
    deliver: (S, R) -> Unit
) {
    private data class Request<S>(val signal: S, val generation: Long)
    private val requests = Channel<Request<S>>(Channel.CONFLATED)
    @Volatile private var generation = 0L

    init {
        scope.launch {
            for (request in requests) {
                val result = withContext(worker) {
                    read(request.signal) { generation == request.generation }
                }
                if (generation == request.generation) deliver(request.signal, result)
            }
        }
    }

    fun submit(signal: S) {
        requests.trySend(Request(signal, generation))
    }

    fun invalidate() {
        generation++
        requests.tryReceive()
    }
}
