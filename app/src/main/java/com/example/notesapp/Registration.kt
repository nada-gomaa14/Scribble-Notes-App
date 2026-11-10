package com.example.notesapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.Color
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth



class Registration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)

        // Initialize Firebase Realtime Database
        val database = FirebaseDatabase.getInstance("https://scribble-902ea-default-rtdb.europe-west1.firebasedatabase.app/")
        val usersRef = database.getReference("users")

        // Initialize Firebase Authentication
        val auth = FirebaseAuth.getInstance()


        // Button to navigate back to the MainActivity
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Labels
        val nameLabel = findViewById<TextView>(R.id.RegName)
        val emailLabel = findViewById<TextView>(R.id.Email)
        val passLabel = findViewById<TextView>(R.id.Pass1)
        val passLabel2 = findViewById<TextView>(R.id.Pass2)

        // Input Fields
        val nameField = findViewById<EditText>(R.id.editName)
        val emailField = findViewById<EditText>(R.id.editRegEmail)
        val passField = findViewById<EditText>(R.id.editRegPassword)
        val passField2 = findViewById<EditText>(R.id.editRegPassword2)

        // Registration Button
        val regButton = findViewById<Button>(R.id.regButton)

        regButton.setOnClickListener {

            // Validate name and email fields
            val nameValid = validateFields(nameLabel, nameField, "Full Name")
            val emailValid = validatEmail(emailLabel, emailField)

            // Validate password field
            val passwordText = passField.text.toString().trim()
            val passwordMessage = isPasswordValid(passwordText)

            val passValid = if(passwordText.isEmpty()){
                passLabel.text = "Password: This Field is required"
                passLabel.setTextColor(Color.RED)
                false
            }
            else if (passwordMessage != null){
                passLabel.text = passwordMessage
                passLabel.setTextColor(Color.RED)
                false
            }
            else {
                passLabel.text = "Password"
                passLabel.setTextColor(Color.parseColor("#0F4D2E"))
                true
            }


            // Validate confirm password field
            val pass2Text = passField2.text.toString().trim()
            val pass2Valid = if(pass2Text.isEmpty()){

                passLabel2.text = "Confirm Password: This field is required"
                passLabel2.setTextColor(Color.RED)
                false
            }
            else if (passwordText != pass2Text){
                passLabel2.text = "Passwords do not match"
                passLabel2.setTextColor(Color.RED)
                false
            }
            else {
                passLabel2.text = "Confirm Password"
                passLabel2.setTextColor(Color.parseColor("#0F4D2E"))
                true
            }


            // Create user
            if(nameValid && emailValid && passValid && pass2Valid){


                val sharedPref = getSharedPreferences("userData", MODE_PRIVATE)
                val editor = sharedPref.edit()

                val fullName = nameField.text.toString()
                val email = emailField.text.toString()
                val password = passField.text.toString()


                editor.putString("fullName", fullName)
                editor.putString("email", email)
                editor.apply()


                val userData = HashMap<String, Any>()
                userData["fullName"] = fullName
                userData["email"] = email
                userData["password"] = password


                // Create user using Firebase Authentication
                auth.createUserWithEmailAndPassword(email,password)
                    .addOnCompleteListener { task ->
                        if(task.isSuccessful){

                            // Store user data in Firebase Realtime Database
                             val userId = auth.currentUser!!.uid
                            usersRef.child(userId).setValue(userData)

                            // Navigate to home page
                            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, homePage::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Database Failure", Toast.LENGTH_SHORT).show()

                        }

                    }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Validate empty input fields
    private fun validateFields(
        label: TextView,
        editText: EditText,
        labelText: String
    ): Boolean {

        return if (editText.text.toString().trim().isEmpty()) {
            label.text = "$labelText: This Field is required"
            label.setTextColor(Color.RED)
            false
        } else {
            label.text = labelText
            label.setTextColor(Color.parseColor("#0F4D2E"))
            true
        }
    }

    // Password Validation
    private fun isPasswordValid(password: String): String? {
        val minLength = 8
        val specialChar = "[!@#\$%^&*()_+\\-=\\[\\]{}:;\"'|,.<>/?]"

        if (password.length < minLength)
            return "Password must be at least $minLength characters"
        if (!password.any { it.isUpperCase() })
            return "Password must contain at least one uppercase letter"
        if (!password.any { it.isDigit() })
            return "Password must contain at least one number"
        if (!Regex(specialChar).containsMatchIn(password))
            return "Password must contain at least one special character"

        return null
    }

    // Validate email format
    private fun validatEmail(label: TextView, editText: EditText): Boolean{
        val emailText = editText.text.toString().trim()

        return if(emailText.isEmpty()){
            label.text = "Email: This field is required"
            label.setTextColor(Color.RED)
            false
        }
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            label.text = "Email: Invalid email format"
            label.setTextColor(Color.RED)
            false
        }
        else {
            label.text = "Email"
            label.setTextColor(Color.parseColor("#0F4D2E"))
            true
        }
    }
}