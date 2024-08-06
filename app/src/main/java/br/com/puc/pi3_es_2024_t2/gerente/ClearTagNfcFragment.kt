package br.com.puc.pi3_es_2024_t2.gerente

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import br.com.puc.pi3_es_2024_t2.FunctionsNuvem
import br.com.puc.pi3_es_2024_t2.R
import br.com.puc.pi3_es_2024_t2.databinding.FragmentClearTagNfcBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Fragmento responsável por limpar os dados da tag NFC.
 * - Caso haja sucesso o usuário será direcionado para **RentalEndedActivity**
 * @author Guilherme
 * @author Vinícius
 **/
class ClearTagNfcFragment : Fragment(), NfcAdapter.ReaderCallback {

    private lateinit var _binding: FragmentClearTagNfcBinding
    private val binding get() = _binding
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var functions: FirebaseFunctions
    private lateinit var functionsNuvem: FunctionsNuvem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentClearTagNfcBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        functions = Firebase.functions("southamerica-east1")
        functionsNuvem = FunctionsNuvem()

        // inicializando hardware NFC do dispositivo
        nfcAdapter = NfcAdapter.getDefaultAdapter(requireActivity())

        binding.btnArrowBack.setOnClickListener {
            requireActivity().finish()
        }

        // navegação da bottom bar
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home -> {
                    // Inicia uma nova tarefa a partir da GerenteActivity
                    // Ao mesmo tempo limpa a fila de tarefas atual
                    startActivity(Intent(requireActivity(), GerenteActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    true
                }

                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val options = Bundle()
        // delay para a leitura NFC detectar a ação
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)

        // ativar o modo de leitura NFC
        nfcAdapter.enableReaderMode(requireActivity(), this, NfcAdapter.FLAG_READER_NFC_A, options)
    }

    override fun onPause() {
        super.onPause()

        // desativar o modo de leitura NFC
        nfcAdapter.disableReaderMode(requireActivity())
    }

    override fun onTagDiscovered(tag: Tag?) {

        clearTag(Ndef.get(tag))
    }

    /**
     * Função responsável por limpar os dados da tag NFC.
     *
     * @param ndef objeto que permite realizar operações Ndef na tag.
     * @author Erico
     * @author Guilherme
     * @author Vinícius
     * */
    private fun clearTag(ndef: Ndef) {
        try {

            // habilitando operações ndef
            if (!ndef.isConnected) {
                ndef.connect()
            }

            val ndefMessage = ndef.ndefMessage

            if (ndefMessage == null ) {
                val snackbar = Snackbar.make(
                    requireView(),
                    "Dados da locação não encontrado",
                    Snackbar.LENGTH_LONG
                )
                snackbar.setBackgroundTint(getResources().getColor(R.color.red_error))
                val view = snackbar.view
                val params = view.layoutParams as FrameLayout.LayoutParams
                params.gravity = Gravity.TOP
                view.layoutParams = params
                snackbar.show()
                return
            } else {

                val records = ndefMessage.records
                val docId = String(records[2].payload, StandardCharsets.UTF_8)

                val mimeRecord =
                    NdefRecord.createMime("text/plain", " ".toByteArray(Charsets.UTF_8))
                val arr = NdefRecord.createApplicationRecord(requireActivity().packageName)
                val newNdefMessage = NdefMessage(arrayOf(mimeRecord, arr))

                // Detectar se a Tag aceita escrita
                if (ndef.isWritable) {

                    // Escrita da NdefMessage na tag
                    ndef.writeNdefMessage(newNdefMessage)

                    functionsNuvem.opLocacao(functions, docId, 2) { message ->

                        // Mostrar um loading para o usuário
                        prepareLoadingContainer()

                        // message.elementAt(1) = qtdTag = Quantidade de tags da locação
                        // message.elementAt(2) = qtdTagAx = Variável auxiliar
                        // O usuário tem que escanear e limpar novamente, pois nem todas as
                        // pulseiras da locação foram limpas
                        if (message?.elementAt(2) != 0) {

                            startActivity(Intent(requireActivity(), NfcHostActivity::class.java).apply {
                                putExtra("uid", docId)
                                putExtra("typeAction", 3)
                            })
                            requireActivity().finish()
                        } else {

                            // iniciar a Activity de locação encerrada
                            functionsNuvem.encerrarLocacao(functions, docId) {
                                if(it != null){
                                    Log.i("AAA", " it 1 ${it[0]} it 2 ${it[1]}")
                                    Log.i("A", "ANtes do intent")
                                    startActivity(
                                        Intent(
                                            requireActivity(),
                                            RentalEndedActivity::class.java
                                        ).putExtra("valorReembolso", it[1].toString())
                                    )
                                    Log.i("A", "Depois do intent")
                                    requireActivity().finish()
                                }
                            }
                        }
                    }

                    // Requerer a Activity host e rodar na main thread
                    requireActivity().runOnUiThread {
                        val snackbar = Snackbar.make(
                            requireView(),
                            "Tag limpa com sucesso.",
                            Snackbar.LENGTH_LONG
                        )
                        snackbar.setBackgroundTint(getResources().getColor(R.color.green_correct))
                        val view = snackbar.view
                        val params = view.layoutParams as FrameLayout.LayoutParams
                        params.gravity = Gravity.TOP
                        view.layoutParams = params
                        snackbar.show()
                    }

                } else {

                    // Requerer a Activity host e rodar na main thread
                    requireActivity().runOnUiThread {
                        val snackbar = Snackbar.make(
                            requireView(),
                            "Não foi possível limpar a Tag.",
                            Snackbar.LENGTH_LONG
                        )
                        snackbar.setBackgroundTint(getResources().getColor(R.color.red_error))
                        val view = snackbar.view
                        val params = view.layoutParams as FrameLayout.LayoutParams
                        params.gravity = Gravity.TOP
                        view.layoutParams = params
                        snackbar.show()
                    }
                }
            }

        } catch (e: IOException) {

            // Requerer a Activity host e rodar na main thread
            requireActivity().runOnUiThread {
                val snackbar = Snackbar.make(
                    requireView(),
                    "Falha ao limpar os dados. Tente Novamente.",
                    Snackbar.LENGTH_LONG
                )
                snackbar.setBackgroundTint(getResources().getColor(R.color.red_error))
                val view = snackbar.view
                val params = view.layoutParams as FrameLayout.LayoutParams
                params.gravity = Gravity.TOP
                view.layoutParams = params
                snackbar.show()
            }
        } finally {

            // Sempre realizar o fechamento da conexão Ndef
            ndef.close()
        }
    }

    private fun prepareLoadingContainer() {
        binding.toolbar.visibility = View.GONE
        binding.containerClearTag.visibility = View.GONE
        binding.containerLoading.visibility = View.VISIBLE
    }

}