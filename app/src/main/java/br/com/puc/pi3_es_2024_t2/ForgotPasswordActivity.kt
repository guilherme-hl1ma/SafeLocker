package br.com.puc.pi3_es_2024_t2

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.Toast
import br.com.puc.pi3_es_2024_t2.databinding.ActivityForgotPasswordBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    private companion object {
        private val TAG = "esqueciSenha"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.btnEsqueciSenha.setOnClickListener {
            if(verifyStatus()){
                sendPasswordEmail(binding.email.text.toString())
                val intent = Intent(this, SignInActivity::class.java)
                intent.putExtra("EXTRA_SNACKBAR_MESSAGE", "Email enviado para resetar a senha.")
                intent.putExtra("EXTRA_SNACKBAR_COLOR", R.color.green_correct)
                startActivity(intent)
            }else{

                binding.tilEmail.error = "Email inválido"

                val snackbar = Snackbar.make(binding.root, "Informe um email válido para restaurar a senha.", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(resources.getColor(R.color.red_error))
                val view = snackbar.view
                val params = view.layoutParams as FrameLayout.LayoutParams
                params.gravity = Gravity.TOP
                view.layoutParams = params
                snackbar.show()
            }
        }

        binding.btnArrowBack.setOnClickListener{
            finish()
        }
    }

    // Função que envia email de recuperação
    private fun sendPasswordEmail(email: String) {
        Firebase.auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    Log.w(TAG, "Send email")
                }
                else {
                    Log.e(TAG,"Error to send email")
                }
            }
    }

    private fun verifyStatus(): Boolean{
        return !binding.email.text.isNullOrEmpty()
    }
}