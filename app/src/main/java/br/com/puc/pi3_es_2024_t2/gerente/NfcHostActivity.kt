package br.com.puc.pi3_es_2024_t2.gerente

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import br.com.puc.pi3_es_2024_t2.R
import br.com.puc.pi3_es_2024_t2.databinding.ActivityNfcHostBinding

/**
 * Uma Activity responsável por hospedar os 3 Fragments, os quais são responsáveis cada uma por uma
 * função: Escrever, Ler e Apagar os dados.
 * Essa Activity recebe intents para saber qual Fragment ela irá acionar.
 * E a varíável responsável por esse controle é a **typeAction** que funciona no seguinte:
 * - Caso o **typeAction** seja 1, significa que a Activity recebeu uma intenção de escrever na tag NFC,
 * logo, deve ser redirecionado para o WriteTagNfcFragment()
 * - Caso o **typeAction** seja 2, significa que a Activity recebeu uma intenção de ler a tag NFC,
 * logo, deve ser redirecionado para a ReadTagNfcFragment()
 * - Por fim, caso o **typeAction** seja 3, significa que a Activity recebeu uma intenção de limpar
 * a tag NFC, deve ser redirecionado para a ClearTagNfcFragment()
 * */
class NfcHostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNfcHostBinding
    private lateinit var endImage: String
    private var typeAction: Int? = null
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNfcHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        uid = intent.getStringExtra("uid").toString()

        endImage = intent.getStringExtra("dir-photo").toString()
        typeAction = intent.getIntExtra("typeAction", 1)

        // preparando os fragmentos
        prepareFragments()
    }

    /**
     * Função responsável por preparar os fragmentos de acordo com o tipo da ação.
     *
     * - Caso o typeAction seja 1, significa que a Activity recebeu uma intenção de escrever na tag NFC,
     * logo, deve ser redirecionado para o WriteTagNfcFragment()
     * - Caso o typeAction seja 2, significa que a Activity recebeu uma intenção de ler a tag NFC,
     * logo, deve ser redirecionado para a ReadTagNfcFragment()
     * - Por fim, caso o typeAction seja 3, significa que a Activity recebeu uma intenção de limpar
     * a tag NFC, deve ser redirecionado para a ClearTagNfcFragment()
     * @author Vinícius
     * @author Guilherme
     * */
    private fun prepareFragments() {
        when (typeAction) {

            // type action = 1 -> Escrever na Tag
            1 -> {
                /** WriteTagNfcFragment que contém no Bundle o diretório da imagem.*/
                val writeTagNfcFragment = WriteTagNfcFragment().apply {
                    arguments = Bundle().apply {
                        putString("dir-photo", endImage)
                        putString("uid", uid)
                    }
                }
                replaceFragments(writeTagNfcFragment)
            }

            // type action = 2 -> Ler a Tag
            2 -> replaceFragments(ReadTagNfcFragment())

            // type action = 3 -> Limpar a Tag
            3 -> {
                val clearTagNfcFragment = ClearTagNfcFragment().apply {
                    arguments = Bundle().apply {
                        putString("uid", uid)
                    }
                }
                replaceFragments(clearTagNfcFragment)
            }
        }
    }

    /**
     * Inicializar o reposicionamento de fragmento.
     * @param fragment é o Fragment que será recebido para ser alocado no container do fragment.
     * @author Guilherme
     * */
    private fun replaceFragments(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
