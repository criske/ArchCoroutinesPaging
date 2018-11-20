/*
 * License: MIT
 * Copyright (c)  Pela Cristian 2018.
 */

package com.crskdev.arch.coroutines.paging

import androidx.paging.PositionalDataSource

/**
 * Created by Cristian Pela on 12.11.2018.
 */
class InMemoryPagedListDataSource<T>(private val list: List<T>) : PositionalDataSource<T>() {

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
        val endPosition =
            (params.startPosition + params.loadSize).coerceAtMost(list.size)
        val subList = list.subList(params.startPosition, endPosition)
        callback.onResult(subList)
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
        val totalCount = list.size
        val firstLoadPosition =
            PositionalDataSource.computeInitialLoadPosition(
                params,
                totalCount
            )
        val firstLoadSize =
            PositionalDataSource.computeInitialLoadSize(
                params,
                firstLoadPosition,
                totalCount
            )
        val lastLoadPosition = firstLoadPosition + firstLoadSize
        val sublist = list.subList(firstLoadPosition, lastLoadPosition)

        callback.onResult(
            sublist,
            firstLoadPosition,
            totalCount
        )
    }
}