package com.example.notesapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.*

class Profile : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private var usersRef: DatabaseReference? = null

    private var editName: EditText? = null
    private var usernameTv: TextView? = null
    private var emailTv: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)


        // Initialize firebase reference
        usersRef = FirebaseDatabase
            .getInstance("https://scribble-902ea-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("users")



        editName = findViewById(R.id.editprofileName)
        usernameTv = findViewById(R.id.username)
        emailTv = findViewById(R.id.email)

        // Check if user is logged in
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadUserProfile(user.uid)

        // Navigate back to home page
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            startActivity(Intent(this, homePage::class.java))
        }

        // Log out button
        findViewById<Button>(R.id.logoutProfile).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Save username
        findViewById<Button>(R.id.saveChanges).setOnClickListener {
            saveUsername()
        }

        // Open change password dialog
        findViewById<Button>(R.id.changePass).setOnClickListener {
            showChangePasswordDialog()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    // load user profile data from firebase realtime database
    private fun loadUserProfile(uid: String) {
        usersRef?.child(uid)
            ?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("fullName").getValue(String::class.java) ?: ""

                    // Update views with fetched data
                    usernameTv?.text = name
                    emailTv?.text = auth.currentUser?.email ?: "No email found"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Profile, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Save updated username to firebase
    private fun saveUsername() {
        val newName = editName?.text.toString().trim()
        if (newName.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: return

        // Update display name in firebase authentication
        val profileUpdates = userProfileChangeRequest { displayName = newName }

        user.updateProfile(profileUpdates)
            .addOnSuccessListener {

                // Update username in Realtime Database
                usersRef?.child(user.uid)
                    ?.child("fullName")
                    ?.setValue(newName)
                    ?.addOnSuccessListener {

                        // Update views
                        usernameTv?.text = newName
                        editName?.setText(newName)
                        Toast.makeText(this, "Username updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(this, "Failed to update username in database", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update username", Toast.LENGTH_SHORT).show()
            }
    }

    // Show dialog to change password
    private fun showChangePasswordDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_change_password, null)

        val currentPass = view.findViewById<EditText>(R.id.currentPassword)
        val newPass = view.findViewById<EditText>(R.id.newPassword)
        val confirmPass = view.findViewById<EditText>(R.id.confirmPassword)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(view)
            .setPositiveButton("Save") { dialog, _ ->
                val oldP = currentPass.text.toString()
                val newP = newPass.text.toString()
                val confirmP = confirmPass.text.toString()

                if (oldP.isEmpty() || newP.isEmpty() || confirmP.isEmpty()) {
                    Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newP != confirmP) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Update user password
                updatePassword(oldP, newP)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }

    // Re-authenticate user and update password
    private fun updatePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return

        // Create credential using current password
        val credential = com.google.firebase.auth.EmailAuthProvider
            .getCredential(user.email!!, currentPassword)

        // Re-authenticate user before changing password
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Password update failed", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Wrong current password", Toast.LENGTH_SHORT).show()
            }
    }
}
