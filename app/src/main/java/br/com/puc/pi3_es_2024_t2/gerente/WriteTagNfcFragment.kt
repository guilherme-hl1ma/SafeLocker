package br.com.puc.pi3_es_2024_t2.gerente

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import br.com.puc.pi3_es_2024_t2.FunctionsNuvem
import br.com.puc.pi3_es_2024_t2.R
import br.com.puc.pi3_es_2024_t2.databinding.FragmentWriteTagNfcBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import java.io.IOException

/**
 * Fragmento responsável por escrever os dados na tag NFC.
 * - Caso haja sucesso o usuário poderá clicar em FINALIZAR e liberar a locação.
 * @author Guilherme
 * @author Vinícius
 * */
class WriteTagNfcFragment : Fragment(), NfcAdapter.ReaderCallback {

    private lateinit var _binding: FragmentWriteTagNfcBinding
    private val binding get() = _binding
    private lateinit var functions: FirebaseFunctions
    private lateinit var functionsNuvem: FunctionsNuvem
    private lateinit var endImage: String
    private var uid: String? = null
    private lateinit var nfcAdapter: NfcAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentWriteTagNfcBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        functions = Firebase.functions("southamerica-east1")
        functionsNuvem = FunctionsNuvem()

        // inicializando a variável que contém o diretório da imagem
        endImage = arguments?.getString("dir-photo").toString()
        uid = arguments?.getString("uid").toString()

        binding.btnArrowBack.setOnClickListener{
            requireActivity().finish()
        }

        // Button para ir para a Activity que mostra o armário e a porta que foi reservada
        binding.btnFinish.setOnClickListener {
            functionsNuvem.opLocacao(functions, uid!!, 1) { message ->
                if (message?.elementAt(1) != message?.elementAt(2)) {
                    startActivity(Intent(requireActivity(), TakePhotoActivity::class.java).putExtra("uid", uid))
                    requireActivity().finish()

                } else {
                    startActivity(Intent(requireActivity(), RentalReleasedConfirmationActivity::class.java))
                    requireActivity().finish()
                }
            }
        }

        // navegação da bottom bar
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.bottom_home -> {
                    requireActivity().finish()
                    true
                }

                else -> false
            }
        }

        // inicializando hardware NFC do dispositivo
        nfcAdapter = NfcAdapter.getDefaultAdapter(requireActivity())
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

        writeTag(Ndef.get(tag))
    }

    /**
     * Função responsável por escrever uma NdefMessage na tag NFC.
     *
     * @param ndef objeto que permite realizar operações Ndef na tag.
     * @author Erico
     * @author Guilherme
     * @author Vinícius
     * */
    private fun writeTag(ndef: Ndef) {

        try {
            // habilitando operações ndef
            ndef.connect()

            val mimeRecord =
                NdefRecord.createMime("text/plain", endImage.toByteArray(Charsets.UTF_8))
            val arr = NdefRecord.createApplicationRecord(requireActivity().packageName)

            val docId = NdefRecord.createMime("text/plain", uid!!.toByteArray(Charsets.UTF_8))

            /** NdefMessage que contém diretório e o pacote da aplicação.*/
            val newNdefMessage = NdefMessage(arrayOf(mimeRecord, arr, docId))

            // Detectar se a Tag aceita escrita
            if (ndef.isWritable) {

                // Escrita da NdefMessage na tag
                ndef.writeNdefMessage(newNdefMessage)

                // Requerer a Activity host e rodar na main thread
                requireActivity().runOnUiThread {
                    val snackbar = Snackbar.make(
                        requireView(),
                        "Mensagem escrita com sucesso!",
                        Snackbar.LENGTH_LONG
                    )
                    snackbar.setBackgroundTint(getResources().getColor(R.color.green_correct))
                    val view = snackbar.view
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    view.layoutParams = params
                    snackbar.show()

                    prepareFinishContainer()

                }
            } else {

                // Requerer a Activity host e rodar na main thread
                requireActivity().runOnUiThread {
                    val snackbar =
                        Snackbar.make(requireView(), "Tag não é gravável.", Snackbar.LENGTH_LONG)
                    snackbar.setBackgroundTint(getResources().getColor(R.color.red_error))
                    val view = snackbar.view
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    view.layoutParams = params
                    snackbar.show()
                }
            }
        } catch (e: IOException) {

            // Requerer a Activity host e rodar na main thread
            requireActivity().runOnUiThread {

                val snackbar = Snackbar.make(
                    requireView(),
                    "Falha ao escrever na Tag. Tente Novamente.",
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

    /**
     * Função responsável por desabilitar a visiblidade do container que contém a interface de
     * Escrever na tag NFC e habilitar o container que contém o botão de finalizar/liberar a locação.
     * @author Guilherme
     * */
    private fun prepareFinishContainer () {
        binding.containerEscreverNfc.visibility = View.GONE
        binding.containerFinalizar.visibility = View.VISIBLE
        binding.toolbar.visibility = View.GONE
    }

}