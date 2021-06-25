package com.example.livechat.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.livechat.Constants.DEFAULT_ROOM
import com.example.livechat.models.Message

class ChatViewModel : ViewModel() {

    private val messagesList = mutableListOf<Message>()

    private val _messages = MutableLiveData(messagesList)
    val messages: LiveData<MutableList<Message>> = _messages

    var nickname = ""
    var room = DEFAULT_ROOM

    fun clearMessages(){
        messagesList.clear()
        _messages.postValue(messagesList)
    }

    fun removeInfoMessageBySenderId(id: String){
        val items = messagesList.filter { message ->
           message.senderId == id && message.type == Message.MESSAGE_INFO
        }

        items.forEach { item ->
            messagesList.remove(item)
        }

        _messages.postValue(messagesList)
    }

    fun addMessage(message: Message){
        messagesList.add(message)
        _messages.postValue(messagesList)
    }
}