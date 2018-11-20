@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.crskdev.arch.coroutines.paging

import android.annotation.SuppressLint
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.PagedList.Config.MAX_SIZE_UNBOUNDED
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlin.coroutines.CoroutineContext

/**
 * Created by Cristian Pela on 14.11.2018.

 * License: MIT
 * Copyright (c)  Pela Cristian 2018.
 */
inline fun <Key, Value> dataSourceFactory(crossinline block: () -> DataSource<Key, Value>): DataSource.Factory<Key, Value> =
    object : DataSource.Factory<Key, Value>() {
        override fun create(): DataSource<Key, Value> = block()
    }

fun <Key, Value> DataSource.Factory<Key, Value>.setupPagedListBuilder(block: PagedListBuilderDSL<Value>.() -> Unit):
        ReadyPagedListBuilder<Key, Value> =
    ReadyPagedListBuilder.createWith(
        this,
        PagedListBuilderDSL<Value>().apply(block)
    )

fun <Key, Value> DataSource.Factory<Key, Value>.setupPagedListBuilder(pageSize: Int):
        ReadyPagedListBuilder<Key, Value> =
    setupPagedListBuilder {
        config(pageSize)
        fetchDispatcher = Dispatchers.Default
    }


@Suppress("UNCHECKED_CAST")
@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
suspend fun <Key, Value> ReadyPagedListBuilder<Key, Value>.onPaging(job: Job? = null, consumer: (PagedList<Value>, Detachable) -> Unit) =
    coroutineScope {
        val parentContext = coroutineContext

        val sendChannel = actor<PagedList<Value>>(job ?: parentContext) {
            val dis = this
            val detachable = object : Detachable {
                override fun detach() {
                    dis.cancel()
                }
            }
            for (page in channel) {
                consumer(page, detachable)
            }
        }

        var lastPage: PagedList<Value>? = null
        val invalidatedCallback = object : DataSource.InvalidatedCallback {
            override fun onInvalidated() {
                val dis = this
                runBlocking(parentContext + fetchDispatcher) {
                    val lastKey = lastPage?.lastKey as? Key
                    do {
                        lastPage?.dataSource?.removeInvalidatedCallback(dis)
                        lastPage = recreate(lastKey).build(parentContext).apply {
                            dataSource.addInvalidatedCallback(dis)
                        }
                    } while (lastPage?.isDetached == true)
                    lastPage?.let { sendChannel.send(it) }
                    Unit
                }
            }
        }
        sendChannel.send(build(parentContext).apply {
            lastPage = this
            lastPage?.dataSource?.addInvalidatedCallback(invalidatedCallback)
        })

        sendChannel.invokeOnClose {
            lastPage?.dataSource?.removeInvalidatedCallback(invalidatedCallback)
            lastPage?.detach()
        }
    }


interface Detachable {
    fun detach()
}

@SuppressLint("RestrictedApi")
internal fun <Key, Value> ReadyPagedListBuilder<Key, Value>.build(coroutineContext: CoroutineContext):
        PagedList<Value> =
    builder
        .setNotifyExecutor(ArchTaskExecutor.getMainThreadExecutor())
        .setFetchExecutor {
            runBlocking(coroutineContext + fetchDispatcher) { it.run() }
        }
        .setBoundaryCallback(boundaryCallback)
        .build()


class ConfigDSL {
    var enablePlaceholders: Boolean = true
    var initialLoadSizeHint: Int = -1
    var pageSize = -1
    var prefetchDistance = -1
    var maxSize = MAX_SIZE_UNBOUNDED

    operator fun invoke(pageSize: Int = 1, block: ConfigDSL.() -> Unit = {}) {
        this.pageSize = pageSize
        this.block()
    }
}

class PagedListBuilderDSL<Value> {
    var config: ConfigDSL =
        ConfigDSL()
    var boundaryCallback: PagedList.BoundaryCallback<Value>? = null
    var fetchDispatcher: CoroutineDispatcher = Dispatchers.Default
    internal var notifyDispatcher: CoroutineDispatcher = Dispatchers.Main
}


class ReadyPagedListBuilder<Key, Value> internal constructor(
    internal val builder: PagedList.Builder<Key, Value>,
    internal val config: PagedList.Config,
    internal val dataSourceFactory: DataSource.Factory<Key, Value>,
    internal val boundaryCallback: PagedList.BoundaryCallback<Value>?,
    internal val notifyDispatcher: CoroutineDispatcher,
    internal val fetchDispatcher: CoroutineDispatcher) {

    companion object {
        internal fun <Key, Value> createWith(factory: DataSource.Factory<Key, Value>,
                                             pagedListDSL: PagedListBuilderDSL<Value>): ReadyPagedListBuilder<Key, Value> {
            val dataSource = factory.create()
            val config = PagedList.Config.Builder()
                .setEnablePlaceholders(pagedListDSL.config.enablePlaceholders)
                .setInitialLoadSizeHint(pagedListDSL.config.initialLoadSizeHint)
                .setPageSize(pagedListDSL.config.pageSize)
                .setMaxSize(pagedListDSL.config.maxSize)
                .setPrefetchDistance(pagedListDSL.config.prefetchDistance)
                .build()

            val builder = PagedList.Builder<Key, Value>(dataSource, config)

            return ReadyPagedListBuilder(
                builder,
                config,
                factory,
                pagedListDSL.boundaryCallback,
                pagedListDSL.notifyDispatcher,
                pagedListDSL.fetchDispatcher
            )

        }
    }

    internal fun recreate(initialKey: Key? = null): ReadyPagedListBuilder<Key, Value> {
        val dataSource = dataSourceFactory.create()
        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(config.enablePlaceholders)
            .setInitialLoadSizeHint(config.initialLoadSizeHint)
            .setPageSize(config.pageSize)
            .setMaxSize(config.maxSize)
            .setPrefetchDistance(config.prefetchDistance)
            .build()
        val builder = PagedList.Builder<Key, Value>(dataSource, config)
            .setInitialKey(initialKey)
        return ReadyPagedListBuilder(
            builder,
            config,
            dataSourceFactory,
            boundaryCallback,
            notifyDispatcher,
            fetchDispatcher
        )
    }
}
