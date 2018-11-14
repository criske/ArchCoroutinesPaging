package com.crskdev.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.paging.DataSource
import androidx.recyclerview.widget.DividerItemDecoration
import com.crskdev.app.paging.InMemoryPagedListDataSource
import com.crskdev.app.paging.TestAdapter
import com.crskdev.arch.coroutines.app.R
import com.crskdev.arch.couroutines.paging.dataSourceFactory
import com.crskdev.arch.couroutines.paging.onPaging
import com.crskdev.arch.couroutines.paging.setupPagedListBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext = job + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()
    }

    private fun setupUI() {
        with(recycler) {
            val testAdapter = TestAdapter()
            adapter = testAdapter
            addItemDecoration(
                DividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL
                )
            )
            launch {
                dataSourceFactory {
                    (0..1000).fold(mutableListOf<String>()) { a, c ->
                        a.apply { add("Item$c") }
                    }.let {
                        InMemoryPagedListDataSource(it)
                    }
                }.setupPagedListBuilder {
                    config {
                        pageSize = 10
                    }
                }.onPaging {
                    button.tag = it.dataSource
                    testAdapter.submitList(it)
                }
            }
            Unit
        }

        button.setOnClickListener {
            @Suppress("UNCHECKED_CAST")
            (it.tag as? DataSource<Int, String>)?.invalidate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

}


