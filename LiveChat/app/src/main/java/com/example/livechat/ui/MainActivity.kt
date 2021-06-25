package com.example.livechat.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.livechat.Constants.EXTRA_NICKNAME
import com.example.livechat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {

            joinButton.setOnClickListener {
                val name = nickname.text.toString()

                if(name.isNotEmpty()){
                    val intent = Intent(
                        this@MainActivity,
                        ChatActivity::class.java
                    ).apply {
                        putExtra(EXTRA_NICKNAME, name.trim())
                    }
                    startActivity(intent)
                    finish()
                }else{
                    Toast.makeText(this@MainActivity, "You must insert a nickname!", Toast.LENGTH_LONG).show()
                }
            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}