package br.com.puc.pi3_es_2024_t2

import android.content.Intent
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import br.com.puc.pi3_es_2024_t2.databinding.ActivitySignUpBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    private var numeroTelefoneFormatado: String = ""

    companion object {
        private const val TAG = "EmailPassword"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        functions = Firebase.functions("southamerica-east1")

        // Escolha Exclusiva do tipo de verificação de conta, no caso SMS
        binding.radioBtnSMS.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.containerMsgError.visibility = View.GONE
                binding.radioBtnEmail.isChecked = false
            }
        }

        // Escolha exclusiva do tipo de verificação de conta, no caso Email
        binding.radioBtnEmail.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.containerMsgError.visibility = View.GONE
                binding.radioBtnSMS.isChecked = false
            }
        }

        // Clicar em Cadastrar
        binding.btnSingUp.setOnClickListener {
            binding.progressBarSignUp.visibility = View.VISIBLE
            binding.btnSingUp.visibility = View.GONE
            Log.i("DATA", binding.etDataNascimento.text.toString().length.toString())

            // Usuário preencheu os campos
            if (validarCampos()) {

                // acrescentar o código do país mais o número de telefone inserido pelo usuário
                numeroTelefoneFormatado = "+55 ${binding.etTelefone.text?.trim().toString()}"

                createAccount(binding.etEmail.text.toString(), binding.etSenha.text.toString())

            } else {

                val snackbar = Snackbar
                    .make(binding.root, "Informe todas as informações para cadastro.", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.red_error))

                val view = snackbar.view
                val params = view.layoutParams as FrameLayout.LayoutParams
                params.gravity = Gravity.TOP
                view.layoutParams = params
                snackbar.show()

                binding.progressBarSignUp.visibility = View.GONE
                binding.btnSingUp.visibility = View.VISIBLE
            }
        }

        binding.btnArrowBack.setOnClickListener {
            finish()
        }

        /*
        * Conjunto de funções de retorno fornecidas pela classe PhoneAuthProvider.
        * Os callbacks são responsáveis por monitorar o estado da solicitação de
        * verificação de código e lidar com esses estados.
        * Referência do código: https://acervolima.com/verificacao-de-numero-de-telefone-usando-firebase-no-android/
        * */
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("onVerificationCompleted", "Success")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.d("onVerificationFailed", "$e")

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Log.d("FirebaseAuthInvalidCredentialsException", "$e")
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Log.d("FirebaseTooManyRequestsException", "$e")
                } else if (e is FirebaseAuthMissingActivityForRecaptchaException) {
                    // reCAPTCHA verification attempted with null Activity
                    Log.d("FirebaseAuthMissingActivityForRecaptchaException", "$e")
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("onCodeSent", "onCodeSent: $verificationId")
                binding.progressBarSignUp.visibility = View.GONE
                binding.btnSingUp.visibility = View.VISIBLE
                storedVerificationId = verificationId
                resendToken = token

                val intent = Intent(applicationContext, VerifySmsActivity::class.java)
                intent.putExtra("storedVerificationId", verificationId)
                startActivity(intent)
                finish()
            }
        }

        // desabilitar os erros do edit text ao ter foco em algum
        disableErrorOnFocusEditText()

        // formatar cpf
        formatCpf()

        // formatar data de nascimento
        formatBirthDate()

        // formatar data de cpf
        formatCellphone()
    }

    /**
     * Verificar se o usuário preencheu o mínimo de 6 caracteres para a senha,
     * exigido para cadastrar um usuário no Firebase Authentication.
     * @return [true] caso o usuário cadastro o mínimo de caracteres e [false]
     * se o usuário não cumpriu a exigência
     * @author Thiago
     * */
    private fun passwordVerification(): Boolean {
        return binding.etSenha.length() >= 6
    }

    /**
     * Cloud Function responsável por criar um dado para a coleção USER, onde está
     * localizado os dados de todos os usuários do aplicativo.
     * @param email [String] - email do usuário que será armazenado no Firebase
     * @author Vinícius
     * */
    private fun createUser(email: String) {
        val data = hashMapOf(
            "userUid" to auth.currentUser?.uid,
            "email" to email,
            "senha" to "",
            "nome" to binding.etNome.text.toString(),
            "cpf" to binding.etCpf.text.toString(),
            "dataNascimento" to binding.etDataNascimento.text.toString(),
            "telefone" to binding.etTelefone.text.toString(),
            "tipoUsuario" to "cliente"
        )
        functions
            .getHttpsCallable("addNewUser")
            .call(data)
            .addOnSuccessListener { result ->
                val dataMap = result.data as? Map<*, *>
                if (dataMap != null) {
                    val status = dataMap["status"] as? String
                    val message = dataMap["message"] as? String

                    if (status == "SUCCESS") {
                        Log.d(TAG, "Usuário criado com sucesso")
                    } else {

                        Log.w(TAG, "Erro ao inserir usuário: $message")
                    }
                } else {
                    Log.w(TAG, "Erro ao processar os dados da Cloud Function")
                    binding.progressBarSignUp.visibility = View.GONE
                    binding.btnSingUp.visibility = View.VISIBLE
                }

            }
            .addOnFailureListener { exception ->
                binding.progressBarSignUp.visibility = View.GONE
                binding.btnSingUp.visibility = View.VISIBLE
                Log.e(TAG, "Erro ao chamar a Cloud Function", exception)
            }
    }

    /**
     * Função responsável pela criação de um usuário no Firebase Authentication.
     * @param email [String] - Email recuperado do formulário de cadastro
     * @param password [String] - Senha recuperada do formulário de cadastro
     * @author Vinícius, Guilherme
     * */
    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    if (user != null) {
                        Log.d(TAG, "Id de usuario ${user.email}d")
                        createUser(email)
                        if (binding.radioBtnEmail.isChecked) {
                            sendEmailVerification()
                        } else {
                            sendVerificationCode(numeroTelefoneFormatado)
                        }
                    }
                } else {
                    binding.progressBarSignUp.visibility = View.GONE
                    binding.btnSingUp.visibility = View.VISIBLE
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)

                    binding.tilEmail.error = "Email não está formatado corretamente"

                    val snackbar = Snackbar.make(binding.root, "Email não está formatado corretamente.", Snackbar.LENGTH_LONG)
                    snackbar.setBackgroundTint(getResources().getColor(R.color.red_error))
                    val view = snackbar.view
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    view.layoutParams = params
                    snackbar.show()

                }
            }
    }

    /**
     * Enviar email de verificação para o email do usuário - função fornecida pelo Firebase.
     * */
    private fun sendEmailVerification() {
        val user = auth.currentUser!!
        user.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, EmailSentConfirmationActivity::class.java)
                        .putExtra("emailUser", user.email))
                    finish()
                } else {
                    Log.e(TAG, "Error in sending email")
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
    private fun sendVerificationCode(number: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Verificar se algum método de verificação de conta foi assinalado.
     * @return retorna [true] se algum método foi assinalado e [false]
     * se nenhum método foi assinalado.
     * @author Guilherme
     * */
    private fun verifyMethodConfirmation(): Boolean {
        return !(!binding.radioBtnEmail.isChecked && !binding.radioBtnSMS.isChecked)
    }

    private fun validarDataNascimentoAntes(dataNascimento: String): Boolean {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return try {
            val date = LocalDate.parse(dataNascimento, formatter)
            // Verifica se a data é depois de 01/01/1940
            date.isAfter(LocalDate.of(1940, 1, 1))
        } catch (e: DateTimeParseException) {
            false
        }
    }
    private fun validarDataNascimentoDepois(dataNascimento: String): Boolean {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return try {
            val date = LocalDate.parse(dataNascimento, formatter)
            // Verifica se a data é antes de 01/01/2010
            date.isBefore(LocalDate.of(2010, 1, 1))
        } catch (e: DateTimeParseException) {
            false
        }
    }

    /**
     * Função responsável por validar os campos de cadastro do usuário.
     * @return retornar **true** caso os campos sejam validos, senão vai retornar **false**
     * @author Erico
     * @author Guilherme
     * @author Vinicius
     * */

    private fun validarCampos(): Boolean {
        var valid = true

        if(binding.etEmail.text.isNullOrEmpty()){
            binding.tilEmail.error = "Preencha o email"
            valid = false
        }

        if(binding.etSenha.text.isNullOrEmpty()){
            binding.tilSenha.error = "Preencha a senha"
            valid = false
        }

        // Verifica se a senha tem no mínimo 6 dígitos
        if(!passwordVerification()){
            binding.tilSenha.error = "A senha deve conter no mínimo 6 dígitos"
            valid = false
        }

        if(binding.etNome.text.isNullOrEmpty()){
            binding.tilNome.error = "Preencha seu nome completo"
            valid = false
        }

        if(binding.etCpf.text.isNullOrEmpty()){
            binding.tilCpf.error = "Preencha seu CPF"
            valid = false
        } else if(binding.etCpf.text.toString().length < 14) {
            binding.tilCpf.error = "Preencha seu CPF corretamente"
            valid = false
        }

        if(binding.etDataNascimento.text.isNullOrEmpty()){
            binding.tilDataNascimento.error = "Preencha sua data de nascimento"
            valid = false
        } else if(binding.etDataNascimento.text.toString().length < 10) {
            binding.tilDataNascimento.error = "Por favor, preencha sua data de nascimento corretamente"
            valid = false
        } else if(!validarDataNascimentoAntes(binding.etDataNascimento.text.toString())) {
            binding.tilDataNascimento.error = "Data de nascimento deve ser maior que 01/01/1940"
            valid = false
        } else if (!validarDataNascimentoDepois(binding.etDataNascimento.text.toString())) {
            binding.tilDataNascimento.error = "Data de nascimento deve ser menor que 01/01/2010"
            valid = false
        }

        if(binding.etTelefone.text.isNullOrEmpty()){
            binding.tilTelefone.error = "Preencha seu número de telefone"
            valid = false
        } else if (binding.etTelefone.text!!.length < 13) {
            binding.tilTelefone.error = "Preencha um numero de telefone valido"
            valid = false
        }

        if (!verifyMethodConfirmation()) {
            // Mostrar que o usuário deve escolher um tipo de verificação
            binding.containerMsgError.visibility = View.VISIBLE
            valid = false
        }

        return valid
    }

    /**
     * Função responsável por formatar o cpf no seguinte formato: XXX.XXX.XXX-XX
     * Código usado como base:
     * @see [Validando um CPF utilizando apenas os 3 primeiros dígitos | SQLite Avançado #15]
     * https://www.youtube.com/watch?v=Iw6VMrbeoxM
     * @author Guilherme
     * */
    private fun formatCpf() {
        binding.etCpf.addTextChangedListener(object : TextWatcher {
            var ultimoCaracterCpf: String = ""
            var isFormatting: Boolean = false
            var isDeleting: Boolean = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                val lengthCpf = binding.etCpf.text!!.length
                if (lengthCpf > 1) {
                    ultimoCaracterCpf = binding.etCpf.text.toString().substring(lengthCpf - 1)
                }

                // Está deletando
                isDeleting = count > 0
            }

            // Formatação desejada: XXX.XXX.XXX-XX
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isFormatting) {
                    return
                }

                val lengthCpf = binding.etCpf.text!!.length
                if (lengthCpf == 3 || lengthCpf == 7) {
                    isFormatting = true
                    if (ultimoCaracterCpf == "." && isDeleting) {
                        binding.etCpf.text!!.delete(lengthCpf - 1, lengthCpf)
                    } else if (!isDeleting) {
                        binding.etCpf.append(".")
                    }

                    isFormatting = false

                } else if (lengthCpf == 11) {
                    isFormatting = true
                    if (ultimoCaracterCpf == "-" && isDeleting) {
                        binding.etCpf.text!!.delete(lengthCpf - 1, lengthCpf)
                    } else if (!isDeleting) {
                        binding.etCpf.append("-")
                    }

                    isFormatting = false

                }
            }

            override fun afterTextChanged(s: Editable?) {}

        })
    }

    /**
     * Função responsável por formatar a data de nascimento no seguinte formato: DD/MM/AAAA
     * Código usado como base:
     * @see [Validando um CPF utilizando apenas os 3 primeiros dígitos | SQLite Avançado #15]
     * https://www.youtube.com/watch?v=Iw6VMrbeoxM
     * @author Guilherme
     * */
    private fun formatBirthDate() {
        binding.etDataNascimento.addTextChangedListener(object : TextWatcher {
            var ultimoCaracterData: String = ""
            var isFormatting: Boolean = false
            var isDeleting: Boolean = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                val lengthDataNascimento = binding.etDataNascimento.text!!.length
                if (lengthDataNascimento > 1) {
                    ultimoCaracterData =
                        binding.etDataNascimento.text.toString().substring(lengthDataNascimento - 1)
                }

                // Está deletando
                isDeleting = count > 0
            }

            // Formatação desejada DD/MM/AAAA
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isFormatting) {
                    return
                }

                val lengthDataNascimento = binding.etDataNascimento.text!!.length
                if (lengthDataNascimento == 2) {
                    isFormatting = true
                    if (ultimoCaracterData == "/" && isDeleting) {
                        binding.etDataNascimento.text!!.delete(
                            lengthDataNascimento - 1,
                            lengthDataNascimento
                        )
                    } else if (!isDeleting) {
                        binding.etDataNascimento.append("/")
                    }

                    isFormatting = false

                } else if (lengthDataNascimento == 5) {
                    isFormatting = true
                    if (ultimoCaracterData == "/" ) {
                        binding.etDataNascimento.text!!.delete(
                            lengthDataNascimento - 1,
                            lengthDataNascimento
                        )
                    } else if (!isDeleting) {
                        binding.etDataNascimento.append("/")
                    }

                    isFormatting = false
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Evento de mudança do campo telefone - Função pronta que formata o número de telefone com
     * base no código do país.
     * */
    private fun formatCellphone() {
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
     * Função responsável por desabilitar os erros que aparecem nos campos de texto quando
     * algum dos campos forem clicados.
     * @author Guilherme
     * */
    private fun disableErrorOnFocusEditText() {
        val clearErrorsOnFocus = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tilEmail.error = null
                binding.tilSenha.error = null
                binding.tilNome.error = null
                binding.tilCpf.error = null
                binding.tilDataNascimento.error = null
                binding.tilTelefone.error = null
            }
        }

        binding.etEmail.onFocusChangeListener = clearErrorsOnFocus
        binding.etSenha.onFocusChangeListener = clearErrorsOnFocus
        binding.etNome.onFocusChangeListener = clearErrorsOnFocus
        binding.etCpf.onFocusChangeListener = clearErrorsOnFocus
        binding.etDataNascimento.onFocusChangeListener = clearErrorsOnFocus
        binding.etTelefone.onFocusChangeListener = clearErrorsOnFocus
    }
}