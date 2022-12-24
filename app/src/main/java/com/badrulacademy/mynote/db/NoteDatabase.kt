package com.badrulacademy.mynote.db

import android.content.Context
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.badrulacademy.mynote.model.Note
import kotlinx.coroutines.internal.synchronized

@Database(
    entities = [Note::class],
    version = 1,
    exportSchema = false
)
abstract class NoteDatabase: RoomDatabase() {

    abstract fun getNoteDao(): DAO

    companion object{
        @Volatile
        private var instance : NoteDatabase?= null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: kotlin.synchronized(LOCK){
            instance ?: createDatabase(context).also {
                instance = it
            }
        }


        private fun createDatabase(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            NoteDatabase::class.java,
            "note_database"
        ).build()
    }
}