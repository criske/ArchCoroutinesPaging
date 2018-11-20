package com.crskdev.arch.coroutines.paging

import kotlinx.coroutines.*
import org.junit.Test

/**
 * Created by Cristian Pela on 15.11.2018.
 */
@ExperimentalCoroutinesApi
class PagedlistKtTest {


    @Suppress("UNCHECKED_CAST")
    @Test
    fun request() {
        runBlocking {
            val data =
                (1..101).fold(mutableListOf<String>()) { acc, curr ->
                    acc.apply {
                        acc.add("Book$curr")
                    }
                }
            val job = Job()
            launch(job) {
                dataSourceFactory {
                    InMemoryPagedListDataSource(data)
                }.setupPagedListBuilder(5).onPaging { pagedList, d ->
                    println(pagedList.snapshot())
                }
            }

            launch(Dispatchers.Default) {
                delay(5000)
                job.cancel()
            }
        }

    }
}

