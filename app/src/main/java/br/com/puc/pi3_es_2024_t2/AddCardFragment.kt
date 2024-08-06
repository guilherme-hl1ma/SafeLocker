package br.com.puc.pi3_es_2024_t2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import br.com.puc.pi3_es_2024_t2.databinding.FragmentAddCardBinding
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.util.Calendar

class AddCardFragment : Fragment() {

    private lateinit var _binding: FragmentAddCardBinding
    private val binding get() = _binding
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    private lateinit var btnArrowBack: AppCompatImageButton
    private lateinit var etNometitular: AppCompatEditText
    private lateinit var etNumCartao: AppCompatEditText
    private lateinit var etDataValidade: AppCompatEditText
    private lateinit var btnCadastrar: AppCompatButton
    private lateinit var progressBarAddCard: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAddCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        functions = Firebase.functions("southamerica-east1")

        btnArrowBack = view.findViewById(R.id.btnArrowBack)
        etNometitular = view.findViewById(R.id.etNometitular)
        etNumCartao = view.findViewById(R.id.etNumCartao)
        etDataValidade = view.findViewById(R.id.etDataValidade)
        btnCadastrar = view.findViewById(R.id.btnCadastrar)
        progressBarAddCard = view.findViewById(R.id.progressBarAddCard)

        btnCadastrar.setOnClickListener {
            btnCadastrar.visibility = View.GONE
            progressBarAddCard.visibility = View.VISIBLE
            if (validarCampos()) {
                addCartaoCredito()
            } else {
                btnCadastrar.visibility = View.VISIBLE
                progressBarAddCard.visibility =  View.GONE
            }
        }

        // formatar o número do cartão em tempo real
        formatCardNumber()

        // formatar a data de validade do cartão em tempo real
        formatCardExpirationDate()

        // desabilitar os erros quando um edittext obtiver foco
        disableErrorOnFocusEditText()

        btnArrowBack.setOnClickListener{
            parentFragmentManager.popBackStack()
        }

        // campo do Nome do Titular apenas maiúsculas
        etNometitular.filters += InputFilter.AllCaps()

    }

    // Função que cadastra cartão de credito
    private fun addCartaoCredito() {
        val data = hashMapOf(
            "docIdUser" to auth.currentUser!!.uid,
            "titular" to etNometitular.text.toString(),
            "numero" to etNumCartao.text.toString(),
            "dataValidade" to etDataValidade.text.toString()
        )
        functions
            .getHttpsCallable("addCartaoCredito")
            .call(data)
            .addOnSuccessListener { result ->
                progressBarAddCard.visibility = View.GONE
                btnCadastrar.visibility = View.VISIBLE
                val dataMap = result.data as? Map<*, *>
                if (dataMap != null) {
                    val status = dataMap["status"] as? String
                    val message = dataMap["message"] as? String
                    if (status == "SUCCESS") {
                        Log.d("addCartaoCredito", "Cartão inserido com sucesso")
                        parentFragmentManager.popBackStack()

                        startActivity(Intent(requireActivity(), HomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            this.putExtra("EXTRA_SNACKBAR_MESSAGE", "Cartão cadastrado com sucesso")
                            this.putExtra("EXTRA_SNACKBAR_COLOR", R.color.green_correct)
                        })
                    } else {
                        Log.w("addCartaoCredito", "Erro ao inserir Cartão: $message")
                    }
                } else {
                    Log.w("addCartaoCredito", "Erro ao processar os dados da Cloud Function")
                }
            }
            .addOnFailureListener { exception ->
                btnCadastrar.visibility = View.VISIBLE
                progressBarAddCard.visibility = View.GONE
                Log.e("addCartaoCredito", "Erro ao chamar a Cloud Function", exception)
            }
    }

    /**
     * Função responsável por validar os campos de cadastro do cartão.
     * @return retornar **true** caso os campos sejam validos, senão vai retornar **false**
     * @author Erico
     * @author Guilherme
     * */
    private fun validarCampos(): Boolean {
        var valid = true

        // Nome do titular nulo
        if(binding.etNometitular.text?.isEmpty() == true){
            binding.tilNomeTitular.error = "Preencha o nome que está no cartão"
            valid = false
        }

        // CVV nulo
        if (binding.etCVV.text?.isEmpty() == true) {
            binding.tilCVV.error = "Preencha o CVV do cartão"
            valid = false
        }

        // CVV não contém 3 dígitos
        if (binding.etCVV.text?.length != 3) {
            binding.tilCVV.error = "O CVV contém 3 dígitos"
            valid = false
        }

        // Número de cartão nulo
        if (binding.etNumCartao.text?.isEmpty() == true) {
            binding.tilNumero.error = "Preencha o número do cartão"
            valid = false
        }

        // Número de cartão não contém 16 dígitos - "19" porque está contabilizado os espaços
        if (binding.etNumCartao.text?.length != 19) {
            binding.tilNumero.error = "Deve conter 16 números"
        }

        if (!validarDataValidadeCartao())   valid = false
        
        return valid
    }

    /**
     * Função responsável por verificar a validade do cartão.
     * @return vai retornar **true** se o cartão não estiver vencido, caso contrário retornará
     * **false**
     * @author Guilherme
     * */
    private fun validarDataValidadeCartao(): Boolean {
        // os dois últimos dígitos do ano atual
        val currentYear = LocalDate.now().year % 100
        val currentMonth = LocalDate.now().monthValue

        val cardDateArray = binding.etDataValidade.text!!.split("/")
        if (cardDateArray.size != 2) {
            binding.tilDataVencimento.error = "Formato de data inválido. Use MM/AA"
            return false
        }

        val cardMonth = cardDateArray[0].toIntOrNull()
        val cardYear = cardDateArray[1].toIntOrNull()

        // Verifique se os valores convertidos são nulos
        if (cardMonth == null || cardYear == null) {
            binding.tilDataVencimento.error = "Formato de data inválido. Use MM/AA"
            return false
        }

        if (cardMonth !in 1..12) {
            binding.tilDataVencimento.error = "O mês deve estar entre 1 e 12"
            return false
        }

        if (cardYear < currentYear || (cardYear == currentYear && cardMonth < currentMonth)) {
            binding.tilDataVencimento.error = "O cartão está vencido"
            return false
        }

        // Limpar a mensagem de erro do edittext se a data for válida
        binding.tilDataVencimento.error = null
        return true
    }


    /**
     * Função responsável por formatar o número do cartão no seguinte formato: XXXX XXXX XXXX XXXX
     * Código usado como base:
     * @see [Validando um CPF utilizando apenas os 3 primeiros dígitos | SQLite Avançado #15]
     * https://www.youtube.com/watch?v=Iw6VMrbeoxM
     * @author Guilherme
     * */
    private fun formatCardNumber() {
        binding.etNumCartao.addTextChangedListener(object : TextWatcher {
            var ultimoCaracterNumCartao: String = ""
            var isFormatting: Boolean = false
            var isDeleting: Boolean = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                val lengthNumCartao = binding.etNumCartao.text!!.length
                if (lengthNumCartao > 1) {
                    ultimoCaracterNumCartao = binding.etNumCartao.text.toString().substring(lengthNumCartao - 1)
                }

                // Está deletando
                isDeleting = count > 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isFormatting) {
                    return
                }

                val lengthNumCartao = binding.etNumCartao.text!!.length
                if (lengthNumCartao == 4 ||
                    lengthNumCartao == 9 ||
                    lengthNumCartao == 14) {
                    isFormatting = true
                    if (ultimoCaracterNumCartao == " " && isDeleting) {
                        binding.etNumCartao.text!!.delete(lengthNumCartao - 1, lengthNumCartao)
                    } else if (!isDeleting) {
                        binding.etNumCartao.append(" ")
                    }
                    isFormatting = false
                }
            }

            override fun afterTextChanged(s: Editable?) {}

        })
    }

    /**
     * Função responsável por formatar a data de validade do cartão para o seguinte padrão: MM/AA
     * Código usado como base:
     * @see [Validando um CPF utilizando apenas os 3 primeiros dígitos | SQLite Avançado #15]
     * https://www.youtube.com/watch?v=Iw6VMrbeoxM
     * @author Guilherme
     * */
    private fun formatCardExpirationDate() {
        // Evento de mudança do campo do Número de cartão - Personalização do campo
        binding.etDataValidade.addTextChangedListener(object : TextWatcher {
            var ultimoCaracterDate: String = ""
            var isFormatting: Boolean = false
            var isDeleting: Boolean = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                val lengthDate = binding.etDataValidade.text!!.length
                if (lengthDate > 1) {
                    ultimoCaracterDate = binding.etDataValidade.text.toString().substring(lengthDate - 1)
                }

                // Está deletando
                isDeleting = count > 0
            }

            // Formatação desejada: XXXX XXXX XXXX XXXX
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isFormatting) {
                    return
                }

                val lengthDate = binding.etDataValidade.text!!.length
                if (lengthDate == 2) {
                    isFormatting = true
                    if (ultimoCaracterDate == "/" && isDeleting) {
                        binding.etDataValidade.text!!.delete(lengthDate - 1, lengthDate)
                    } else if (!isDeleting){
                        binding.etDataValidade.append("/")
                    }

                    isFormatting = false
                }
            }

            override fun afterTextChanged(s: Editable?) {}

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
                binding.tilNomeTitular.error = null
                binding.tilNumero.error = null
                binding.tilDataVencimento.error = null
                binding.tilCVV.error = null
            }
        }

        binding.etNometitular.onFocusChangeListener = clearErrorsOnFocus
        binding.etNumCartao.onFocusChangeListener = clearErrorsOnFocus
        binding.etDataValidade.onFocusChangeListener = clearErrorsOnFocus
        binding.etCVV.onFocusChangeListener = clearErrorsOnFocus

    }
}