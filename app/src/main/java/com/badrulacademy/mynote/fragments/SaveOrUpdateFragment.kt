package com.badrulacademy.mynote.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.badrulacademy.mynote.R
import com.badrulacademy.mynote.activities.MainActivity
import com.badrulacademy.mynote.databinding.BottomSheetLayoutBinding
import com.badrulacademy.mynote.databinding.FragmentSaveOrUpdateBinding
import com.badrulacademy.mynote.model.Note
import com.badrulacademy.mynote.utils.hideKeyboard
import com.badrulacademy.mynote.viewmodel.NoteActivityViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SaveOrUpdateFragment : Fragment(R.layout.fragment_save_or_update) {

    private lateinit var navController: NavController
    private lateinit var contentBinding: FragmentSaveOrUpdateBinding
    private var note: Note?=null
    private var color = -1
    private lateinit var result: String
    private val noteActivityViewModel: NoteActivityViewModel by activityViewModels()
    private val currentDate = SimpleDateFormat.getInstance().format(Date())
    private val job = CoroutineScope(Dispatchers.Main)
    private val args: SaveOrUpdateFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val animation = MaterialContainerTransform().apply {
            drawingViewId=R.id.fragment_id
            scrimColor = Color.TRANSPARENT
            duration = 300L
        }
        sharedElementEnterTransition = animation
        sharedElementReturnTransition = animation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contentBinding = FragmentSaveOrUpdateBinding.bind(view)
        navController = Navigation.findNavController(view)
        val activity = activity as MainActivity

        ViewCompat.setTransitionName(
            contentBinding.noteContentFragmentParentId,
            "recyclerView_${args.note?.id}"
        )

        contentBinding.backButton.setOnClickListener {
            requireView().hideKeyboard()
            navController.popBackStack()
        }

        contentBinding.saveNoteId.setOnClickListener {
            saveNote()
        }

        try {
            contentBinding.etNoteContentId.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus){
                    contentBinding.bottomBarId.visibility = View.VISIBLE
                    contentBinding.etNoteContentId.setStylesBar(contentBinding.styleBar)
                }else
                    contentBinding.bottomBarId.visibility = View.GONE
            }
        }catch (e: Exception){
            Log.d("LOG", "onViewCreated: "+e.stackTrace.toString())
        }

        contentBinding.fabColorPick.setOnClickListener {
            val bottomSheetDialog = BottomSheetDialog(
                requireContext(),
                R.style.BottomSheetDialogTheme
            )
            val bottomSheetView: View = layoutInflater.inflate(
                R.layout.bottom_sheet_layout, null
            )
            with(bottomSheetDialog){
                setContentView(bottomSheetView)
                show()
            }

            val bottomSheetBinding = BottomSheetLayoutBinding.bind(bottomSheetView)
            bottomSheetBinding.apply {
                colorPickerId.apply {
                    setSelectedColor(color)
                    setOnColorSelectedListener {
                        value->
                        color=value
                        contentBinding.apply {
                            noteContentFragmentParentId.setBackgroundColor(color)
                            toolbarFragmentNoteContent.setBackgroundColor(color)
                            bottomBarId.setBackgroundColor(color)
                            activity.window.statusBarColor = color
                        }
                        bottomSheetBinding.bottomSheetParentId.setCardBackgroundColor(color)
                    }
                }
                bottomSheetParentId.setCardBackgroundColor(color)
            }
            bottomSheetView.post{
                bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        //open with existing item
        setUpNote()

    }

    private fun setUpNote() {
        val note=args.note
        val title=contentBinding.etTitleId
        val content=contentBinding.etNoteContentId
        val lastEdited=contentBinding.lastEdited

        if(note==null){
            contentBinding.lastEdited.text = getString(R.string.edited_on, SimpleDateFormat.getDateInstance().format(Date()))
        }
        if(note!=null){
            title.setText(note.title)
            content.renderMD(note.content)
            lastEdited.text=getString(R.string.edited_on,note.date)
            color= note.color
            contentBinding.apply {
               job.launch {
                   delay(10)
                   noteContentFragmentParentId.setBackgroundColor(color)
               }
                toolbarFragmentNoteContent.setBackgroundColor(color)
                bottomBarId.setBackgroundColor(color)
            }
            activity?.window?.statusBarColor=note.color
        }
    }

    private fun saveNote() {
       if(contentBinding.etNoteContentId.text.toString().isEmpty() ||
           contentBinding.etTitleId.text.toString().isEmpty()){
           Toast.makeText(activity, "Something is empty", Toast.LENGTH_SHORT).show()
       }else{
           note=args.note
           when(note){
               null->{
                   noteActivityViewModel.saveNote(
                       Note(
                           0,
                           contentBinding.etTitleId.text.toString(),
                           contentBinding.etNoteContentId.getMD(),
                           currentDate,
                           color
                       )
                    )
                   result = "Note Saved"
                   setFragmentResult(
                       "key",
                       bundleOf("bundleKey" to result)
                   )

                   navController.navigate(SaveOrUpdateFragmentDirections.actionSaveOrUpdateFragmentToNoteFragment())
               }
               else->{
                   //update note .....
                   updateNote()
                   navController.popBackStack()
               }
           }
       }
    }

    private fun updateNote() {
        if(note!=null){
            noteActivityViewModel.updateNote(
                Note(
                    note!!.id,
                    contentBinding.etTitleId.text.toString(),
                    contentBinding.etNoteContentId.getMD(),
                    currentDate,
                    color,
                )
            )
        }
    }
}