package com.example.livechat.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.livechat.Constants.BASE_URL
import com.example.livechat.Constants.DEFAULT_ROOM
import com.example.livechat.Constants.EXTRA_NICKNAME
import com.example.livechat.Constants.TYPING_TIMEOUT
import com.example.livechat.R
import com.example.livechat.adapters.MessagesAdapter
import com.example.livechat.databinding.ActivityChatBinding
import com.example.livechat.models.Message
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.net.URI
import java.util.*

class ChatActivity : AppCompatActivity() {

    private val TAG = "ChatActivity"

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!

    private var typing = false

    private lateinit var menu: Menu
    private lateinit var viewModel: ChatViewModel
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var socket: Socket

    private val typingHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent

        viewModel = ViewModelProvider(this).get(ChatViewModel::class.java)

        viewModel.nickname = intent.getStringExtra(EXTRA_NICKNAME)!!
        messagesAdapter = MessagesAdapter()

        viewModel.messages.observe(this, { messages ->
            messagesAdapter.submitList(messages)
        })

        binding.apply {

            messagesRecyclerview.apply {
                setHasFixedSize(true)
                adapter = messagesAdapter
                layoutManager = LinearLayoutManager(this@ChatActivity)
            }
            
            messageEdittext.addTextChangedListener(object : TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    if(!socket.connected()) return

                    if(!typing) {
                        typing = true
                        socket.emit("typing", viewModel.room)
                    }

                    typingHandler.removeCallbacks(typingTimeout)
                    typingHandler.postDelayed(typingTimeout, TYPING_TIMEOUT)
                }

                override fun afterTextChanged(s: Editable?) = Unit

            })

            sendButton.setOnClickListener {
                val text = messageEdittext.text.toString()

                if(text.isNotEmpty()){
                    val message = Message(text.trim(), viewModel.nickname, socket.id())
                    socket.emit("chat-message", message.toJsonObj(), viewModel.room)
                    messageEdittext.setText("")
                    viewModel.addMessage(message)

                    messagesRecyclerview.scrollToPosition(messagesAdapter.itemCount-1)
                }
            }
        }

        socket = IO.socket(URI.create(BASE_URL))

        socket.on("chat-message", onNewMessageListener)
        socket.on("user-join", onUserJoinOrLeave)
        socket.on("user-disconnect", onUserJoinOrLeave)
        socket.on("user-typing", onUserTyping)
        socket.on("user-stop-typing", onUserStopTyping)
        socket.on("join-room", onUserJoinOrLeave)
        socket.on("leave-room", onUserJoinOrLeave)

        socket.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "onCreate: Connected")
            socket.emit("user-join", viewModel.nickname)
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d(TAG, "onCreate -> Socket connect error: retrying to connect")
            socket.connect() // retrying to connect...
        }

        socket.connect()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_chat_menu, menu)
        menu?.let {
            this.menu = it
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.create_new_room -> {

                val room = UUID.randomUUID().toString()

                socket.emit("join-room", room)
                viewModel.room = room

                item.isEnabled = false
                menu.findItem(R.id.join_room).isEnabled = false
                menu.findItem(R.id.leave_room).isEnabled = true

                val dialogInterface = AlertDialog.Builder(this@ChatActivity)
                dialogInterface.apply {
                    val textView = TextView(this@ChatActivity)
                    textView.apply {
                        setTextIsSelectable(true)
                        text = room
                    }

                    setView(textView)

                    setPositiveButton("Close"){ dialog, _ ->
                        dialog.dismiss()
                    }
                }

                dialogInterface.show()
                viewModel.clearMessages()

                true
            }
            R.id.join_room -> {
                val dialogBuilder = AlertDialog.Builder(this@ChatActivity)
                dialogBuilder.apply {
                    setTitle("Insert Room id")

                    val editText = EditText(this@ChatActivity)
                    editText.apply {
                        inputType = InputType.TYPE_CLASS_TEXT
                        hint = "Insert room id..."
                    }

                    setView(editText)

                    setPositiveButton("Join") { dialog, _ ->
                        if(editText.text.isNotEmpty()){
                            menu.findItem(R.id.create_new_room).isEnabled = false
                            menu.findItem(R.id.leave_room).isEnabled = true
                            item.isEnabled = false

                            val room = editText.text.toString().trim()

                            socket.emit("join-room", room)
                            viewModel.room = room
                            Toast.makeText(this@ChatActivity, "Joining...", Toast.LENGTH_SHORT).show()
                        }
                        dialog.dismiss()
                    }

                    setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                }
                viewModel.clearMessages()

                dialogBuilder.show()
                true
            }
            R.id.leave_room -> {
                menu.findItem(R.id.create_new_room).isEnabled = true
                menu.findItem(R.id.join_room).isEnabled = true
                item.isEnabled = false

                socket.emit("leave-room", viewModel.room)
                viewModel.room = DEFAULT_ROOM
                viewModel.clearMessages()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val typingTimeout = Runnable {
        if(!typing) return@Runnable
        typing = false
        socket.emit("stop-typing", viewModel.room)
    }

    private val onUserJoinOrLeave = Emitter.Listener { args ->
        val infoText = args[0] as String
        val info = Message(
            text = infoText,
            type = Message.MESSAGE_INFO
        )
        viewModel.addMessage(info)
    }

    private val onNewMessageListener = Emitter.Listener { args ->
        val jsonObjMessage = args[0] as JSONObject
        val message = Message.jsonToMessage(jsonObjMessage)
        viewModel.addMessage(message)
    }

    private val onUserTyping = Emitter.Listener { args ->
        val jsonObj = args[0] as JSONObject
        val nickname = jsonObj.getString("nickname")
        val id = jsonObj.getString("id")

        val info = Message(
            text = "$nickname typing...",
            senderId = id,
            type = Message.MESSAGE_INFO
        )

        viewModel.addMessage(info)
    }

    private val onUserStopTyping = Emitter.Listener { args ->
        val id = args[0] as String
        viewModel.removeInfoMessageBySenderId(id)
    }

    override fun onDestroy() {
        super.onDestroy()

        socket.off(Socket.EVENT_CONNECT_ERROR)
        socket.off(Socket.EVENT_CONNECT)

        socket.off("chat-message", onNewMessageListener)
        socket.off("user-join", onUserJoinOrLeave)
        socket.off("user-disconnect", onUserJoinOrLeave)
        socket.off("user-typing", onUserTyping)
        socket.off("user-stop-typing", onUserStopTyping)
        socket.off("join-room", onUserJoinOrLeave)
        socket.off("leave-room", onUserJoinOrLeave)

        socket.disconnect()
    }
}