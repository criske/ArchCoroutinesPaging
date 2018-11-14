package com.crskdev.app.paging

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TestVH(v: View) : RecyclerView.ViewHolder(v) {

    private val textView by lazy(LazyThreadSafetyMode.NONE) {
        itemView.findViewById<TextView>(android.R.id.text1)
    }

    fun onBind(item: String) {
        textView.text = item
    }

}