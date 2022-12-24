package com.badrulacademy.mynote.activities

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.badrulacademy.mynote.R
import com.badrulacademy.mynote.databinding.ActivityMainBinding
import com.badrulacademy.mynote.db.NoteDatabase
import com.badrulacademy.mynote.repository.NoteRepository
import com.badrulacademy.mynote.viewmodel.NoteActivityViewModel
import com.badrulacademy.mynote.viewmodel.NoteActivityViewModelFactory

class MainActivity : AppCompatActivity() {

    lateinit var noteActivityViewModel: NoteActivityViewModel
    private lateinit var  binding:ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        try {
            setContentView(binding.root)
            val noteRepository = NoteRepository(NoteDatabase(this))
            val noteActivityModelFactory=NoteActivityViewModelFactory(noteRepository)
            noteActivityViewModel = ViewModelProvider(this,
            noteActivityModelFactory)[NoteActivityViewModel::class.java]
        }catch (e: Exception){
            Log.d("TAG", "Error: ....")
        }
    }
}