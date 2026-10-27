package com.me.carbrowser
object Bus { var listener: ((String) -> Unit)? = null; var lastUrl = "https://www.google.com" }
