 package com.memecloud.data.model
 
 import com.google.gson.annotations.SerializedName
 
 /**
  * 表情包库条目 — 对应后端 Emoji model
  */
 data class EmojiItem(
     val id: Int = 0,
     @SerializedName("file_url") val fileUrl: String = "",
     @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
     val description: String = ""
 )
