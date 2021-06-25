package com.example.livechat.models

import org.json.JSONObject

data class Message(
    val text: String = "",
    val senderNickname: String = "",
    val senderId: String = "",
    val type: Int = MESSAGE_TEXT
) {

    companion object{

        const val MESSAGE_TEXT: Int = 1
        const val MESSAGE_INFO: Int = 2

        fun jsonToMessage(jsonObject: JSONObject) : Message {
            val text = jsonObject.getString("messageText")
            val sender = jsonObject.getString("senderNickname")
            val senderId = jsonObject.getString("senderId")
            val type = jsonObject.getInt("messageType")
            return Message(text, sender, senderId, type)
        }
    }

    fun toJsonObj() : JSONObject {
        val jsonObj = JSONObject()
        jsonObj.put("messageText", text)
        jsonObj.put("senderNickname", senderNickname)
        jsonObj.put("senderId", senderId)
        jsonObj.put("messageType", type)
        return jsonObj
    }
}
