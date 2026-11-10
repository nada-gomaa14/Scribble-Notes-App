package com.example.notesapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.notesapp.database.NoteDao
import com.example.notesapp.database.NoteDatabase
import com.example.notesapp.database.NoteEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class homePage : AppCompatActivity() {

    private lateinit var notesContainer: LinearLayout
    private lateinit var noNotesText: TextView
    private lateinit var noteDao: NoteDao
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String = ""

    private var currentNoteToExport: NoteEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)


        // Firebase initialization
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""


        // Room database initialization
        val db = NoteDatabase.getDatabase(this)
        noteDao = db.noteDao()


        val profileButton = findViewById<ImageButton>(R.id.profileButton)
        val newNoteButton = findViewById<ImageButton>(R.id.NewNote)
        val importButton = findViewById<Button>(R.id.btn_import)
        val exportButton = findViewById<Button>(R.id.btn_export)
        notesContainer = findViewById(R.id.notesContainer)
        noNotesText = findViewById(R.id.textView8)


        // Navigate to profile page
        profileButton.setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }

        // Open dialog to add a new note
        newNoteButton.setOnClickListener { showAddNoteDialog() }

        // Import note from text file
        importButton.setOnClickListener { openFilePicker() }

        // Export a selected note to text file
        exportButton.setOnClickListener { exportSingleNote() }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        loadNotes()
    }

    // File picker launcher
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleFileUri(it) }
    }

    // Open file picker to select a text file
    private fun openFilePicker() {
        importLauncher.launch(arrayOf("text/plain"))
    }

    // Read file content and save it as a note
    private fun handleFileUri(uri: Uri) {
        val text = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
        text?.let {
            saveNote(it)
            Toast.makeText(this, "File imported", Toast.LENGTH_SHORT).show()
        }
    }

    // File creation launcher
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let { writeNoteToFile(it) }
    }

    // Allow the user to select a single note for export
    private fun exportSingleNote() {
        lifecycleScope.launch {

            /// Fetch notes from database
            val notes = withContext(Dispatchers.IO) {
                noteDao.getNotesForUser(currentUserId) }


            if (notes.isEmpty()) {
                Toast.makeText(this@homePage, "No notes to export", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Display note previews for selection
            val noteTitles = notes.map {
                it.content.take(30) + if (it.content.length > 30) "..." else ""
            }.toTypedArray()

            AlertDialog.Builder(this@homePage)
                .setTitle("Select a note to export")
                .setItems(noteTitles) { _, index ->
                    currentNoteToExport = notes[index]
                    exportLauncher.launch("note_${currentNoteToExport?.id}.txt")
                }
                .show()
        }
    }

    // Write selected note content to file
    private fun writeNoteToFile(uri: Uri) {
        currentNoteToExport?.let { note ->
            try {
                contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(note.content.toByteArray())
                }
                Toast.makeText(this, "Note exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to export note", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load notes from database and display them
    private fun loadNotes() {
        lifecycleScope.launch {
            val notes = withContext(Dispatchers.IO) { noteDao.getNotesForUser(currentUserId) }
            notesContainer.removeAllViews()

            noNotesText.visibility =
                if (notes.isEmpty()) TextView.VISIBLE else TextView.GONE

            notes.forEach { addNoteToUI(it) }
        }
    }

    // Show dialog to add a new note
    private fun showAddNoteDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null)
        val noteEditText = dialogView.findViewById<EditText>(R.id.noteEditText)

        AlertDialog.Builder(this)
            .setTitle("Add Note")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val text = noteEditText.text.toString().trim()
                if (text.isNotEmpty()) saveNote(text)
                else Toast.makeText(this, "Note cannot be empty", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Save note to room database
    private fun saveNote(text: String) {
        lifecycleScope.launch {
            val note = NoteEntity(content = text, userId = currentUserId)
            withContext(Dispatchers.IO) { noteDao.insert(note) }
            loadNotes()
        }
    }

    // Add a note view dynamically to the screen
    private fun addNoteToUI(note: NoteEntity) {
        val noteView = TextView(this).apply {
            text = note.content
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.black, theme))
            setBackgroundResource(R.drawable.note_card_background)
            setPadding(32, 24, 32, 24)
            setLineSpacing(6f, 1.1f)

            // Long press to edit or delete note
            setOnLongClickListener {
                showNoteOptionsDialog(note)
                true
            }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 16, 0, 16)
        noteView.layoutParams = params

        notesContainer.addView(noteView, 0)
    }

    // Show options dialog for selected note
    private fun showNoteOptionsDialog(note: NoteEntity) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle("Note Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(note)
                    1 -> deleteNote(note)
                }
            }
            .show()
    }

    // Show dialog to edit existing node
    private fun showEditDialog(note: NoteEntity) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null)
        val editText = dialogView.findViewById<EditText>(R.id.noteEditText)
        editText.setText(note.content)

        AlertDialog.Builder(this)
            .setTitle("Edit Note")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val updatedText = editText.text.toString().trim()
                if (updatedText.isNotEmpty()) updateNote(note.copy(content = updatedText))
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Update note in database
    private fun updateNote(note: NoteEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { noteDao.update(note) }
            loadNotes()
        }
    }

    // Delete note from database
    private fun deleteNote(note: NoteEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { noteDao.delete(note) }
            loadNotes()
        }
    }
}
