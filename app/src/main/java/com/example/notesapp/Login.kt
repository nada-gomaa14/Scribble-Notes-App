package com.example.notesapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth



class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Initialize Firebase authentication
        val auth = FirebaseAuth.getInstance()


        // Navigate to Registration page
        val textView = findViewById<TextView>(R.id.goToReg)
        val text = "Don’t have an account? Register"

        val spannableString = SpannableString(text)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@Login, Registration::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = Color.BLUE
            }
        }

        spannableString.setSpan(clickableSpan, 23, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()



        // Button to go back to home page
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        // Labels for validation messages
        val emailLabel = findViewById<TextView>(R.id.loginEmail)
        val passLabel = findViewById<TextView>(R.id.loginPassword)

        // Input fields
        val emailField = findViewById<EditText>(R.id.editLoginEmail)
        val passField = findViewById<EditText>(R.id.editloginPassword)

        // login button
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)

        buttonLogin.setOnClickListener {

            // Get user input
            val emailText = emailField.text.toString().trim()

            val passwordText = passField.text.toString().trim()

            // Validate that password field is filled
            val passValid = if(passwordText.isEmpty()){
                passLabel.text = "Password: This field is required"
                passLabel.setTextColor(Color.RED)
                false
            }
            else {
                passLabel.text = "Password"
                passLabel.setTextColor(Color.parseColor("#0F4D2E"))
                true
            }

            // Validate that email field is filled
            val emailValid = if(emailText.isEmpty()){
                emailLabel.text = "Email: This field is required"
                passLabel.setTextColor(Color.RED)
                false
            }
            else {
                emailLabel.text = "Email"
                passLabel.setTextColor(Color.parseColor("#0F4D2E"))
                true
            }


            if(emailValid && passValid){

                val email = emailField.text.toString().trim()
                val password = passField.text.toString().trim()

                // Firebase authentication
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if(task.isSuccessful){
                            val user = auth.currentUser
                            val sharedPref = getSharedPreferences("userData", MODE_PRIVATE)
                            val editor = sharedPref.edit()

                            editor.putString("email", user?.email)
                            editor.apply()

                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, homePage::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_SHORT).show()

                        }
                    }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}