package br.com.puc.pi3_es_2024_t2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import br.com.puc.pi3_es_2024_t2.databinding.ActivitySignInBinding
import br.com.puc.pi3_es_2024_t2.gerente.GerenteActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions
    private lateinit var functionsNuvem: FunctionsNuvem

    private lateinit var typeUser: String

    companion object {
        private const val TAG = "EmailPassword"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = Firebase.functions("southamerica-east1")
        auth = Firebase.auth
        functionsNuvem = FunctionsNuvem()

        // mensagem que deve ser exibida na snackbar
        val message = intent.getStringExtra("EXTRA_SNACKBAR_MESSAGE")

        // cor que vai ser definida como background da snackbar
        val color = intent.getIntExtra("EXTRA_SNACKBAR_COLOR", -1)

        // Email para recuperar a senha foi enviado com sucesso
        successEmailPasswordReset(message, color)

        binding.btnSingIn.setOnClickListener {
            if(verifyStatus()){
                loginAccount(binding.etEmail.text.toString(), binding.etPassword.text.toString())
            }else{
                val snackbar = Snackbar.make(binding.root, "Informe usúario e senha para fazer o login.", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(resources.getColor(R.color.red_error))
                val view = snackbar.view
                val params = view.layoutParams as FrameLayout.LayoutParams
                params.gravity = Gravity.TOP
                view.layoutParams = params
                snackbar.show()

                // mostrando erros
                showErrorEditText()
            }
        }

        // desabilitando erros ao clicar em algum  campo
        disableErrorOnFocusEditText()

        // clicou em cadastrar
        binding.btnSingUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // clicou em esqueceu a senha
        binding.btnEsqueciSenha.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // clicou no botão superior esquerda pra voltar
        binding.btnArrowBack.setOnClickListener{
            finish()
        }

    }

    // Função que loga numa conta ja logada no authentication
    private fun loginAccount(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if(task.isSuccessful) {
                    functionsNuvem.buscarTipoUsuario(auth, functions) {
                        typeUser = it
                        verifyUser()
                    }
                }
                else {
                    Log.w(TAG, "LoginEmail:failure", task.exception)
                    val snackbar = Snackbar.make(binding.root, "Você não possui cadastro. Faça o cadastro.", Snackbar.LENGTH_LONG)
                    snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.red_error))
                    val view = snackbar.view
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    view.layoutParams = params
                    snackbar.show()

                    binding.tilEmail.error = "Email inválido"
                    binding.tilPassword.error = "Senha inválida"

                }
            }
    }
    /**
     * Função responsável por verificar se o usuário tem email confirmado ou um número de celular
     * definido. Se não houver nenhum dos dois ele irá ser direcionado para uma Activity para
     * realizar a confirmação da conta dele. Caso constrário ele irá entrar na Home do aplicativo.
     * @author Guilherme e Thiago
     * */
    private fun verifyUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (currentUser.isEmailVerified || (currentUser.phoneNumber != null && currentUser.phoneNumber != "")) {
                if (typeUser == "cliente") {
                    startActivity(Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                } else {
                    startActivity(Intent(this, GerenteActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
            } else {
                startActivity(Intent(this, VerificationActivity::class.java))
            }
        }
    }

    /**
     * Função responsável por verificar se os campos de email e senha estão vazios ou nulos.
     * - Caso a função retorne **false** os campos não são validos
     * - Se retornar **true** os campos são válidos
     * @author Thiago
     * */
    private fun verifyStatus(): Boolean{
        return !binding.etEmail.text.isNullOrEmpty() && !binding.etPassword.text.isNullOrEmpty()
    }


    /**
     * Função responsável por exibir os erros nos campos de email e senha, caso algum deles estejam
     * nulo ou vazio.
     * @author Guilherme
     * @author Erico
     * */
    private fun showErrorEditText() {

        if(binding.etEmail.text.isNullOrEmpty()){
            binding.tilEmail.error = "Preencha o email"
        }

        if(binding.etPassword.text.isNullOrEmpty()){
            binding.tilPassword.error = "Preencha a senha"
        }
    }

    /**
     * Função responsável por desabilitar os erros que aparecem nos campos de texto quando
     * algum dos campos forem clicados.
     * @author Guilherme
     * */
    private fun disableErrorOnFocusEditText() {
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tilPassword.error = null
                binding.tilEmail.error = null
            }
        }

        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tilPassword.error = null
                binding.tilEmail.error = null
            }
        }
    }

    /**
     * Função responsável por exibir uma snackbar contendo o feedback que o email para resetar a
     * senha foi enviado com sucesso.
     *
     * @author Erico
     * @author Guilherme
     * */
    private fun successEmailPasswordReset(message: String?, color: Int) {
        if (message != null && color != -1) {

            val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)

            // definindo a cor do background
            snackbar.setBackgroundTint(ContextCompat.getColor(this, color))

            val view = snackbar.view
            val params = view.layoutParams as FrameLayout.LayoutParams

            // definido o parâmetro gravity como top
            params.gravity = Gravity.TOP

            // definindo que a snackbar vai aparecer no topo
            view.layoutParams = params

            snackbar.show()
        }
    }
}