package com.anomalydev.inventorytracker.login

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.anomalydev.inventorytracker.R
import com.anomalydev.inventorytracker.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import timber.log.Timber


class LoginFragment : Fragment(), View.OnClickListener {

    // Initialize Firebase Auth
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentLoginBinding.inflate(
            inflater,
            container,
            false
        )

        auth = Firebase.auth

        binding.emailSignInButton.setOnClickListener(this)
        binding.emailCreateAccountButton.setOnClickListener(this)
        binding.signOutButton.setOnClickListener(this)
        binding.verifyEmailButton.setOnClickListener(this)
        binding.reloadButton.setOnClickListener(this)

        return binding.root
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.emailCreateAccountButton -> {
                createAccount(
                    binding.fieldEmail.text.toString(),
                    binding.fieldPassword.text.toString()
                )
            }
            R.id.emailSignInButton -> signIn(
                binding.fieldEmail.text.toString(),
                binding.fieldPassword.text.toString()
            )
            R.id.signOutButton -> signOut()
            R.id.verifyEmailButton -> sendEmailVerification()
            R.id.reloadButton -> reload()
        }
    }

    /**
     * This method is in charge of creating a user account with the users email and password
     */
    private fun createAccount(email: String, password: String) {
        Timber.d("CreateAccount: $email")
        if (!validateForm()) {
            return
        }
        showProgressBar()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Sign in success, update UI with the signed-in user's information
                    Timber.d("createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Timber.w("createUserWithEmail:failure ${task.exception} ")
                    Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }

                hideProgressBar()
            }

    }

    /**
     * This method is in charge of signing in the user with their email and password
     */
    private fun signIn(email: String, password: String) {
        if (!validateForm()) {
            return
        }

        showProgressBar()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Timber.d("signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Timber.w("signInWithEmail:failure ${task.exception} ")
                    Toast.makeText(
                        context, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }

                if (!task.isSuccessful) {
                    binding.status.text = getString(R.string.auth_failed)
                }
                hideProgressBar()
            }
    }

    /**
     * This method is in charge of signing out the user when they  click sign out
     */
    private fun signOut() {
        auth.signOut()
        updateUI(null)
    }

    /**
     * This method is in charge of sending an email verification to the user to verify their email
     */
    private fun sendEmailVerification() {
        binding.verifyEmailButton.isEnabled = false

        val user = auth.currentUser!!
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                binding.verifyEmailButton.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(
                        context,
                        "Verification email sent to ${user.email} ",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Failed to send verification email.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    /**
     * This method is in charge of reloading the UI given if there is a user logged in
     */
    private fun reload() {
        auth.currentUser!!.reload().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateUI(auth.currentUser)
                Toast.makeText(
                    context,
                    "Reload successful!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Failed to reload user.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * This method is in charge of validating inputs for email and password
     */
    private fun validateForm(): Boolean {
        var valid = true

        val email = binding.fieldEmail.text.toString()
        if (TextUtils.isEmpty(email)) {
            binding.fieldEmail.error = "Required"
            valid = false
        } else {
            binding.fieldEmail.error = null
        }

        val password = binding.fieldPassword.text.toString()
        if (TextUtils.isEmpty(password)) {
            binding.fieldPassword.error = "Required"
            valid = false
        } else {
            binding.fieldPassword.error = null
        }

        return valid
    }

    /**
     * This method is in charge of updating the UI is the user is logged in or email was verified
     */
    private fun updateUI(user: FirebaseUser?) {
        hideProgressBar()
        if (user != null) {
            binding.status.text = getString(R.string.emailpassword_status_fmt, user.email, user.isEmailVerified)
            binding.detail.text = getString(R.string.firebase_status_fmt, user.uid)

            binding.emailPasswordButtons.visibility = View.GONE
            binding.emailPasswordFields.visibility = View.GONE
            binding.signedInButtons.visibility = View.VISIBLE

            if (user.isEmailVerified) {
                binding.verifyEmailButton.visibility = View.GONE
            } else {
                binding.verifyEmailButton.visibility = View.VISIBLE
            }
        } else {
            binding.status.text = getString(R.string.signed_out)
            binding.detail.text = null

            binding.emailPasswordButtons.visibility = View.VISIBLE
            binding.emailPasswordFields.visibility = View.VISIBLE
            binding.signedInButtons.visibility = View.GONE
        }

    }

    /**
     *  Hides the progress bar UI
     */
    private fun hideProgressBar() {
        binding.progressBar.visibility = View.INVISIBLE
    }

    /**
     *  Shows the progress bar UI
     */
    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()

        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
    }

    override fun onStop() {
        super.onStop()

        // Hides the progress bar if fragment is away from user
        hideProgressBar()
    }
}