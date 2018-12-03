# ArchCoroutinesPaging
Coroutine adapter for Android Paging Library

Usage:

```kt
launch {
	myDataSourceFactory()
		.setupPagedListBuilder(5) // page size
		.onPaging { pagedList, _ ->
			myAdapter.submitList(pagedList)
		}
}
```
or using dsl

```kt
launch {
	myDataSourceFactory()
		.setupPagedListBuilder{
                    config{
                        pageSize = TODO()
                        prefetchDistance = TODO()
                        enablePlaceholders = TODO()
                        initialLoadSizeHint = TODO()
                        maxSize = TODO()
                    }
                    fetchDispatcher = TODO() // default is Dispatchers.DEFAULT
                    boundaryCallback = myBoundaryCb
                }
		.onPaging { pagedList, _ ->
			myAdapter.submitList(pagedList)
		}
}
```
