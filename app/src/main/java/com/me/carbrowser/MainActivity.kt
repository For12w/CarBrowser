package com.me.carbrowser
import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 80, 40, 40) }
        val t = TextView(this).apply { text = "Car Browser remote\nOpen Android Auto, launch this app, then enter a URL here." }
        val et = EditText(this).apply { hint = "URL or search"; setSingleLine() }
        val go = Button(this).apply { text = "Go" }
        fun load() {
            val q = et.text.toString().trim(); if (q.isEmpty()) return
            val url = if (q.contains(".") && !q.contains(" ")) (if (q.startsWith("http")) q else "https://$q")
                      else "https://www.google.com/search?q=" + java.net.URLEncoder.encode(q, "UTF-8")
            Bus.lastUrl = url; Bus.listener?.invoke(url)
        }
        go.setOnClickListener { load() }
        listOf(t, et, go).forEach { root.addView(it, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)) }
        setContentView(root)
    }
}
