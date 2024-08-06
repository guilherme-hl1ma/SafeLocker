package br.com.puc.pi3_es_2024_t2

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.com.puc.pi3_es_2024_t2.databinding.ActivityEmailSentConfirmationBinding

class EmailSentConfirmationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailSentConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailSentConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val emailUser = intent.getStringExtra("emailUser")

        binding.tvSubtitle.text = "O email de verificação da conta foi enviado para $emailUser " +
                ". Por favor verifique para começar a utilizar o aplicativo."

        binding.btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}