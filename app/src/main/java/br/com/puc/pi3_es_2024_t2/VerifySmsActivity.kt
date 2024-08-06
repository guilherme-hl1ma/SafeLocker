package br.com.puc.pi3_es_2024_t2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import br.com.puc.pi3_es_2024_t2.databinding.ActivityVerifySmsBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import java.lang.Exception

class VerifySmsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifySmsBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifySmsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.btnVerificarSms.setOnClickListener {
            binding.progressBarVerifySMS.visibility = View.VISIBLE
            binding.btnVerificarSms.visibility = View.GONE

            // obter o storedVerificationId - necessário para gerar a credencial
            val storedVerificationId = intent.getStringExtra("storedVerificationId")
            val verificationCode = binding.etCodigoVerificao.text?.trim().toString()
            if (verificationCode.isNotEmpty()) {
                // criação da credencial
                try {
                    val credential =
                        PhoneAuthProvider.getCredential(storedVerificationId!!, verificationCode)
                    linkWithCredential(credential)
                } catch (e: Exception) {
                    binding.progressBarVerifySMS.visibility = View.GONE
                    binding.btnVerificarSms.visibility = View.VISIBLE
                    showSnackbarError("Erro ao verificar o código. Tente novamente")
                    binding.tilSms.error = "Tente digitar o código novamente"
                }
            } else {
                binding.progressBarVerifySMS.visibility = View.GONE
                binding.btnVerificarSms.visibility = View.VISIBLE
                binding.tilSms.error = "Digite o código enviado para seu celular."
            }
        }

        binding.btnVoltar.setOnClickListener{
            startActivity(Intent(this, InitialActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    /**
     * Não deixar o usuário clicar em voltar com o botão Android, para evitar
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
    * Vincular a credencial gerada pelo objeto PhoneAuthCredential com a credencial criada na
    * SignUpActivity.
    * @param credential [PhoneAuthCredential] - credencial gerada ao verificar o código enviado
     * no SMS
     * @author Guilherme, Thiago
    * */
    private fun linkWithCredential(credential: PhoneAuthCredential) {
        auth.currentUser!!.linkWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                binding.progressBarVerifySMS.visibility = View.GONE
                binding.btnVerificarSms.visibility = View.VISIBLE
                if (task.isSuccessful) {
                    setSMSVerified(credential)
                    startActivity(Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                } else {
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        binding.tilSms.error = "Código Inválido"
                        showSnackbarError("Código Inválido. Tente Novamente")
                    } else if (task.exception is FirebaseAuthUserCollisionException) {
                        binding.tilSms.error = "Número já existe. Confirme sua conta com email."
                        showSnackbarError("O número já existe. Tente verificar com email.")
                    }
                }
            }
    }

    /**
     * Verificar a situação das opções de verificação de conta, ou seja, analisar se possui
     * ao menos uma opção assinalada.
     * @return [Boolean] se as opções de verificação de conta estão assinaladas.
     * @author Guilherme
     */
    private fun setSMSVerified(phoneCredential: PhoneAuthCredential) {
        val user = Firebase.auth.currentUser

        user!!.updatePhoneNumber(phoneCredential)
    }

    /**
     * Exibir um feedback mostrando o erro para o usuário.
     * @param message mensagem que será exibida na snackbar
     * @author Guilherme
     * */
    private fun showSnackbarError(message: String) {
        val snackbar = Snackbar
            .make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.red_error))
        val view = snackbar.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        view.layoutParams = params
        snackbar.show()
    }
}