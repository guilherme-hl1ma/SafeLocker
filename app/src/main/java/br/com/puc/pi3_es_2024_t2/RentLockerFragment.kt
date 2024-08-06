package br.com.puc.pi3_es_2024_t2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.puc.pi3_es_2024_t2.models.LockerLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.gson.Gson

class RentLockerFragment : Fragment() {

    private lateinit var alertDialogBuilder: MaterialAlertDialogBuilder
    private lateinit var inflater: LayoutInflater
    private lateinit var dialogView: View
    private lateinit var message: AppCompatTextView
    private lateinit var title: AppCompatTextView

    private lateinit var btnGoToRent: ExtendedFloatingActionButton
    private val codeRequest = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rent_locker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnGoToRent = view.findViewById(R.id.btnGoToRent)

        btnGoToRent.setOnClickListener {
            alertDialogBuilder = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            inflater = LayoutInflater.from(requireContext())
            dialogView = inflater.inflate(R.layout.dialog_layout, null)
            message = dialogView.findViewById(R.id.tvMessage)
            title = dialogView.findViewById(R.id.tvTitle)

            // Apertou em Alugar Armário e há alguma dado relacionado à Localização no Bundle
            if(arguments?.getString("lockerPosition") != null){
                // Checando permissão de localização
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                    // A permissão não foi concedida, solicite ao usuário.
                    ActivityCompat.requestPermissions(requireActivity(),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        codeRequest)

                }
                // variavel que terá acesso aos serviços de localização fornecidos pelo Android
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

                // obtendo localização atual
                val currentLocation = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                )
                currentLocation.addOnSuccessListener { location ->
                    val gson = Gson()
                    val markerJson = arguments?.getString("lockerPosition")
                    if (markerJson != null) {
                        val marker = gson.fromJson(markerJson, LockerLocation::class.java)

                        val markerLocation = Location("")
                        markerLocation.latitude = marker.latArmario
                        markerLocation.longitude = marker.lotArmario
                        val distance = location.distanceTo(markerLocation)

                        // distancia <= 1000 m
                        if (distance <= 1000) {
                            // Enviando para a RentalOptionsFragment o dado serializado
                            val rentalOptionsFragment = RentalOptionsFragment().apply {
                                arguments = Bundle().apply {
                                    putString("lockerPosition", markerJson)
                                }
                            }

                            // Iniciar a transação do fragmento
                            parentFragmentManager.beginTransaction()
                                // Substituir o conteúdo do contêiner pelo RentalOptionsFragment
                                .replace(R.id.fragmentContainer, rentalOptionsFragment)
                                // Adicionar a transação à pilha de volta
                                .addToBackStack(null)
                                // Confirmar a transação
                                .commit()
                        } else {
                            title.text = "Aproxime-se do ponto de locação"
                            message.text = "Caro usuário, aproxime-se do ponto de locação para alugar o armário."
                            alertDialogBuilder.setView(dialogView)
                            alertDialogBuilder.setPositiveButton("Entendido") { dialog, _ ->
                                dialog.dismiss()
                            }
                            alertDialogBuilder.create()
                            alertDialogBuilder.show()
                        }
                    }
                }
            }
            // Não tem nenhum dado no Bundle
            else{
                message.text = "Caro usuário, por favor selecione um ponto no mapa."

                alertDialogBuilder.setView(dialogView)

                alertDialogBuilder.setPositiveButton("Fechar") { dialog, _ ->
                    dialog.dismiss()
                }

                alertDialogBuilder.setNegativeButton("Ir para o mapa") { _, _ ->
                    startActivity(Intent(requireContext(), LocationActivity::class.java))
                }

                alertDialogBuilder.create()
                alertDialogBuilder.show()
            }
        }
    }
}