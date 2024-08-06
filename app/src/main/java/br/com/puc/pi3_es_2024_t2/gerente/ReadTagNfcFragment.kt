package br.com.puc.pi3_es_2024_t2.gerente

import android.content.Intent
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
import androidx.appcompat.widget.AppCompatImageButton
import br.com.puc.pi3_es_2024_t2.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Fragmento responsável por ler os dados da tag NFC.
 * - Caso haja sucesso o usuário será redirecionado para **PhotoCliente** .
 * @author Guilherme
 * @author Vinícius
 * */
class ReadTagNfcFragment : Fragment(), NfcAdapter.ReaderCallback {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var btnArrowBack : AppCompatImageButton
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_read_tag_nfc, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // inicializando views
        bottomNavigationView = view.findViewById(R.id.bottomNavigationView)
        btnArrowBack = view.findViewById(R.id.btnArrowBack)

        btnArrowBack.setOnClickListener{
            requireActivity().finish()
        }

        // inicializando hardware NFC do dispositivo
        nfcAdapter = NfcAdapter.getDefaultAdapter(requireActivity())

        // navegação da bottom bar
        bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.bottom_home -> {
                    requireActivity().finish()
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

        readTag(Ndef.get(tag))
    }

    /**
     * Função responsável por ler uma NdefMessage da tag NFC.
     *
     * @param ndef objeto que permite realizar operações Ndef na tag.
     * @author Erico
     * @author Guilherme
     * @author Vinícius
     * */
    private fun readTag(ndef: Ndef) {

        try {
            // habilitando operações ndef
            ndef.connect()

            val ndefMessage = ndef.ndefMessage

            // Verificando se há uma ndefMessage
            if (ndefMessage != null) {
                val records = ndefMessage.records
                val payload = String(records[0].payload, StandardCharsets.UTF_8)

                val intent = Intent(requireActivity(), PhotoClienteActivity::class.java)
                intent.putExtra("endImage", payload)
                startActivity(intent)
                requireActivity().finish()
            } else {

                // Requerer a Activity host e rodar na main thread
                requireActivity().runOnUiThread {
                    val snackbar = Snackbar.make(
                        requireView(),
                        "Nenhuma mensagem NDEF encontrada.",
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
        } catch (e: IOException) {

            // Requerer a Activity host e rodar na main thread
            requireActivity().runOnUiThread {

                val snackbar =
                    Snackbar.make(requireView(), "Falha ao ler a Tag.", Snackbar.LENGTH_LONG)
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
}