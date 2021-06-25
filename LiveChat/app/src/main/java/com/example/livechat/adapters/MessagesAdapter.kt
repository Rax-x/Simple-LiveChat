package com.example.livechat.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.livechat.databinding.MessageItemInfoBinding
import com.example.livechat.databinding.MessageItemTextBinding
import com.example.livechat.models.Message

class MessagesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var list = mutableListOf<Message>()

    private val TEXT_VIEW_TYPE = 1
    private val INFO_VIEW_TYPE = 2

    inner class TextViewHolder(
        private val binding: MessageItemTextBinding
    ) : RecyclerView.ViewHolder(binding.root){

        fun bind(message: Message){
            binding.apply {
                senderNickname.text = message.senderNickname
                messageText.text = message.text
            }
        }
    }

    class InfoViewHolder(
        private val binding: MessageItemInfoBinding
    ) : RecyclerView.ViewHolder(binding.root){
        fun bind(message: Message){
            binding.infoText.text = message.text
        }
    }

    fun submitList(newList: List<Message>){
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val message = list[position]
        return if(message.type == Message.MESSAGE_INFO) INFO_VIEW_TYPE else TEXT_VIEW_TYPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == INFO_VIEW_TYPE) {
            InfoViewHolder(
                MessageItemInfoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else {
            TextViewHolder(
                MessageItemTextBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = list[position]
        if(getItemViewType(position) == INFO_VIEW_TYPE){
            (holder as InfoViewHolder).bind(message)
        }else{
            (holder as TextViewHolder).bind(message)
        }
    }

    override fun getItemCount(): Int = list.size
}