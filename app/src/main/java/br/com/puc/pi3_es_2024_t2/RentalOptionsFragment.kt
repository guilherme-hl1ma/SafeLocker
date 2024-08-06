package br.com.puc.pi3_es_2024_t2

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.puc.pi3_es_2024_t2.models.Locker
import br.com.puc.pi3_es_2024_t2.models.LockerLocation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RentalOptionsFragment : Fragment(), OnRentLocker {

    private lateinit var functions: FirebaseFunctions
    private val functionsNuvem: FunctionsNuvem = FunctionsNuvem()
    private lateinit var auth: FirebaseAuth

    private lateinit var rvPrice: RecyclerView
    private lateinit var btnBack: ImageButton
    private lateinit var marker: LockerLocation

    private lateinit var alertDialogBuilder: MaterialAlertDialogBuilder
    private lateinit var inflater: LayoutInflater
    private lateinit var dialogView: View
    private lateinit var message: AppCompatTextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rental_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // inicializando o objeto FirebaseFunctions
        functions = Firebase.functions("southamerica-east1")
        auth = Firebase.auth

        // inicializando o objeto Gson
        val gson = Gson()

        alertDialogBuilder = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_App_MaterialAlertDialog
        )
        inflater = LayoutInflater.from(requireContext())
        dialogView = inflater.inflate(R.layout.dialog_layout, null)
        message = dialogView.findViewById(R.id.tvMessage)

        btnBack = view.findViewById(R.id.btnArrowBack)

        prepareRecyclerView()

        message.text = "Caro usuário, todas as vezes que for realizada uma locação será cobrada " +
                "a caução de uma diária e, estornada proporcionalmente de acordo com o " +
                "período utilizado."
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setPositiveButton("Prosseguir") { dialog, _ ->
            dialog.dismiss()
        }
        alertDialogBuilder.create()
        alertDialogBuilder.show()

        // incializando uma string que recebe os dados enviados pelo RentLockerFragment
        // que no caso será a Latitude e Longitude do ponto de locação escolhido pelo
        // usuário
        val markerJson = arguments?.getString("lockerPosition")

        // Por meio do objeto Gson, é realizado a conversão de Json para o
        // tipo LockerLocation
        marker = gson.fromJson(markerJson, LockerLocation::class.java)

        // buscando o armário
        functionsNuvem.getArmarioByLatLng(
            marker.latArmario,
            marker.lotArmario,
            functions
        ) { payload ->
            if (payload != null) {
                val armarioList = mutableListOf<Locker>()

                // Iterar sobre cada item na lista payload
                for (item in payload) {
                    if (item is Map<*, *>) {
                        val nomeArmario = item["nomeArmario"] as String
                        val descricao = item["descricao"] as String
                        val latArmario = item["latArmario"] as Double
                        val lotArmario = item["lotArmario"] as Double
                        val precos = item["precos"] as List<Double>

                        // Criar um objeto Locker e adicionar à lista
                        val locker = Locker(nomeArmario, descricao, latArmario, lotArmario, precos)
                        armarioList.add(locker)
                    }
                }

                initItems(armarioList)
            } else {
                Toast.makeText(requireContext(), "Erro desconhecido", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        // retornando para o fragmento anterior da pilha de fragments
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * Preparar a RecyclerView para serem introduzidos os dados.
     * @author Thiago, Guilherme
     * */

    private fun prepareRecyclerView() {
        rvPrice = requireView().findViewById(R.id.rvPrice)
        rvPrice.layoutManager = LinearLayoutManager(requireContext())
        rvPrice.setHasFixedSize(true)
    }

    /**
     * Passar a lista de preço armário requisitado pelo usuário a fim de
     * adaptar esses dados para o ViewHolder e vinculá-los à RecyclerView
     * @param armario - É uma lista que com os dados do armário buscado na cloud
     * function de acordo com o ponto escolhido pelo usuário.
     * @author Thiago, Guilherme
     * */
    private fun initItems(armario: List<Locker>) {
        val adapter = AdapterListPrice(armario[0].precos, this)
        rvPrice.adapter = adapter
    }

    override fun rentLocker(precos: Number, valorDiaria: Number) {
        val calendar: Calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateTimeString = dateFormat.format(calendar.time)

        var docId: String?

        functionsNuvem.createLocacao(
            auth,
            functions,
            marker.latArmario,
            marker.lotArmario,
            dateTimeString.toString(),
            valorDiaria,
            precos
        ) {
            docId = it

            val intentQR = Intent(requireContext(), QRActivity::class.java)
            intentQR.putExtra("data", docId)

            startActivity(intentQR)
            requireActivity().finish()
        }
    }
}