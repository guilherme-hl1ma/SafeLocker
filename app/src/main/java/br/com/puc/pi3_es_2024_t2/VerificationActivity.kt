package br.com.puc.pi3_es_2024_t2

import android.content.Intent
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.puc.pi3_es_2024_t2.databinding.ActivityVerificationBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class VerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationBinding

    private lateinit var auth: FirebaseAuth
    private var storedVerificationId: String? = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        // apertou em voltar na Action Bar - Vai retornar para InitialActivity
        binding.btnArrowBack.setOnClickListener() {
            finish()
        }

        // selecionou a opção de verificar por SMS
        binding.telefoneRadio.setOnClickListener {
            binding.emailRadio.isChecked = false
            binding.tvEmail.visibility = View.GONE
            binding.tilTelefone.visibility = View.VISIBLE
            binding.btnEnviar.visibility = View.VISIBLE
            binding.btnEnviar.text = "Enviar SMS"
        }

        // selecionou a opção de verificar por email
        binding.emailRadio.setOnClickListener {
            binding.telefoneRadio.isChecked = false
            binding.tilTelefone.visibility = View.GONE
            binding.tvEmail.visibility = View.VISIBLE
            binding.btnEnviar.visibility = View.VISIBLE
            binding.btnEnviar.text = "Enviar Email"
        }

        binding.btnEnviar.setOnClickListener {
            if (binding.emailRadio.isChecked) {
                sendEmailVerification()
            }

            if(binding.telefoneRadio.isChecked) {
                startPhoneNumberVerification("+55${binding.etTelefone.text.toString()}")
            }
        }

        /*
        * Conjunto de funções de retorno fornecidas pela classe PhoneAuthProvider.
        * Os callbacks são responsáveis por monitorar o estado da solicitação de
        * verificação de código e lidar com esses estados.
        * Referência do código: https://acervolima.com/verificacao-de-numero-de-telefone-usando-firebase-no-android/
        * */
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {}

            override fun onVerificationFailed(e: FirebaseException) {}

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                storedVerificationId = verificationId
                resendToken = token

                val intent = Intent(applicationContext, VerifySmsActivity::class.java)
                intent.putExtra("storedVerificationId", verificationId)
                startActivity(intent)
                finish()
            }
        }

        // Evento de mudança do campo telefone - Função pronta que formata o número de telefone com
        // base no código do país.
        binding.etTelefone.addTextChangedListener(PhoneNumberFormattingTextWatcher("BR"))
        // Evento de mudança do campo telefone - Formatação do campo
        binding.etTelefone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            // Evitar que o dígito do "+55 " seja incluído por questões estéticas apenas, pois o
            // código do país é necessário para verificar por SMS
            override fun afterTextChanged(s: Editable?) {
                val currentPhoneNumber = s.toString()
                if (currentPhoneNumber.startsWith("+55 ")) {
                    s?.delete(0, 4)
                }
            }
        })
    }

    /**
     * Enviar email de verificação para o email do usuário
     *
     * @author Firebase
     * */
    private fun sendEmailVerification() {
        val user = auth.currentUser!!
        user.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                if(task.isSuccessful) {
                    startActivity(Intent(this, EmailSentConfirmationActivity::class.java)
                        .putExtra("emailUser", user.email))
                    finish()
                }
                else {
                    Toast.makeText(this, "Erro inesperado", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Enviar o código de verificação para o número de telefone do usuário.
     * Usando o Firebase Phone Authetication
     * @param [String] number O número de telefone no qual será enviado o código
     * de verificação
     * @author Firebase
     */
    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

}