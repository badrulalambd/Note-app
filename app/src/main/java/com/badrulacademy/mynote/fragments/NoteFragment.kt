package com.badrulacademy.mynote.fragments

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.badrulacademy.mynote.R
import com.badrulacademy.mynote.activities.MainActivity
import com.badrulacademy.mynote.adapter.RecyclerViewNoteAdapter
import com.badrulacademy.mynote.databinding.FragmentNoteBinding
import com.badrulacademy.mynote.utils.SwipeToDelete
import com.badrulacademy.mynote.utils.hideKeyboard
import com.badrulacademy.mynote.viewmodel.NoteActivityViewModel
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class NoteFragment : Fragment(R.layout.fragment_note) {

    private lateinit var noteBinding: FragmentNoteBinding
    private val noteActivityViewModel: NoteActivityViewModel by activityViewModels()
    private lateinit var recyclerViewAdapter: RecyclerViewNoteAdapter

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        exitTransition = MaterialElevationScale(false).apply {
            duration = 350
        }
        enterTransition = MaterialElevationScale(true).apply {
            duration= 350
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        noteBinding = FragmentNoteBinding.bind(view)
        val activity = activity as MainActivity
        val navControllers = Navigation.findNavController(view)
        requireView().hideKeyboard()
        CoroutineScope(Dispatchers.Main).launch {
            delay(10)
            //activity.window.statusBarColor= Color.WHITE
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            activity.window.statusBarColor = Color.parseColor("#9E9D9D")
        }

        noteBinding.addNoteFabId.setOnClickListener {
            noteBinding.appBarLayoutId.visibility = View.INVISIBLE
            navControllers.navigate(NoteFragmentDirections.actionNoteFragmentToSaveOrUpdateFragment())
        }

        noteBinding.innerFab.setOnClickListener {
            noteBinding.appBarLayoutId.visibility = View.INVISIBLE
            navControllers.navigate(NoteFragmentDirections.actionNoteFragmentToSaveOrUpdateFragment())
        }

        recyclerViewDisplay()
        swipeToDelete(noteBinding.recyclerNoteId)

        //Search functionality implementation.....
        noteBinding.searchId.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                noteBinding.noData.isVisible=false
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString().isNotEmpty()){
                    val text=s.toString()
                    val query="%$text%"
                    if(query.isNotEmpty()){
                        noteActivityViewModel.searchNote(query).observe(viewLifecycleOwner){
                            recyclerViewAdapter.submitList(it)
                        }
                    }else{
                        observerDataChanges()
                    }
                }else{
                    observerDataChanges()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        noteBinding.searchId.setOnEditorActionListener{v, actionId, _->
            if(actionId == EditorInfo.IME_ACTION_SEARCH)
            {
                v.clearFocus()
                requireView().hideKeyboard()
            }
            return@setOnEditorActionListener true
        }



        noteBinding.recyclerNoteId.setOnScrollChangeListener { _, scrollX, scrollY, _, oldScrollY ->
            when{
                scrollY>oldScrollY->{
                    noteBinding.chatFabText.isVisible=false
                }
                scrollX == scrollY->{
                    noteBinding.chatFabText.isVisible=true
                }
                else->{
                    noteBinding.chatFabText.isVisible=true
                 }
            }
        }




    }

    private fun swipeToDelete(recyclerNoteId: RecyclerView) {
        val swipeToDeleteCallback=object : SwipeToDelete(){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position=viewHolder.absoluteAdapterPosition
                val note = recyclerViewAdapter.currentList[position]
                var actionBtnTapped = false
                noteActivityViewModel.deleteNote(note)
                noteBinding.searchId.apply {
                    hideKeyboard()
                    clearFocus()
                }
                if(noteBinding.searchId.text.toString().isEmpty()){
                    observerDataChanges()
                }
                val snackbar=Snackbar.make(
                    requireView(), "Note Deleted", Snackbar.LENGTH_LONG
                ).addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>(){
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                    }

                    override fun onShown(transientBottomBar: Snackbar?) {
                        transientBottomBar?.setAction("UNDO"){
                            noteActivityViewModel.saveNote(note)
                            actionBtnTapped=true
                            noteBinding.noData.isVisible=false
                        }

                        super.onShown(transientBottomBar)
                    }
                }).apply {
                    animationMode=Snackbar.ANIMATION_MODE_FADE
                    setAnchorView(R.id.add_note_fab_id)
                }
                snackbar.setActionTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.yellowOrange
                    )
                )
                snackbar.show()

            }
        }
        val itemTouchHelper=ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerNoteId)

    }

    private fun observerDataChanges() {
        noteActivityViewModel.getAllNotes().observe(viewLifecycleOwner){list->
            noteBinding.noData.isVisible = list.isEmpty()
            recyclerViewAdapter.submitList(list)
        }
    }

    private fun recyclerViewDisplay() {
        when(resources.configuration.orientation){
            Configuration.ORIENTATION_PORTRAIT-> setUpRecyclerView(2)
            Configuration.ORIENTATION_LANDSCAPE-> setUpRecyclerView(3)
        }
    }

    private fun setUpRecyclerView(spanCount: Int) {
        noteBinding.recyclerNoteId.apply {
            layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(true)
            recyclerViewAdapter = RecyclerViewNoteAdapter()
            recyclerViewAdapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            adapter = recyclerViewAdapter
            postponeEnterTransition(300L, TimeUnit.MILLISECONDS)
            viewTreeObserver.addOnPreDrawListener {
                startPostponedEnterTransition()
                true
            }
        }
        observerDataChanges()



    }
}