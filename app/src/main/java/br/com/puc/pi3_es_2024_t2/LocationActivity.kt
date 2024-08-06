package br.com.puc.pi3_es_2024_t2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.puc.pi3_es_2024_t2.databinding.ActivityLocationBinding
import br.com.puc.pi3_es_2024_t2.models.LockerLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson

class LocationActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private lateinit var binding: ActivityLocationBinding
    private lateinit var mMap: GoogleMap
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions
    private lateinit var functionsNuvem: FunctionsNuvem
    private lateinit var functionsMapa: FunctionsMapa

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mapFragment: SupportMapFragment

    private lateinit var linha: Polyline
    private var flag = false
    private lateinit var marcador: Marker
    private lateinit var list: List<Map<String, String>>
    private val markers = mutableListOf<Marker>()
    private lateinit var  localUser : Location
    private val codeRequest = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navegationLocation()

        auth = Firebase.auth
        functions = Firebase.functions("southamerica-east1")
        functionsNuvem = FunctionsNuvem()
        functionsMapa = FunctionsMapa()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            binding.tvMapa.visibility = View.VISIBLE
            binding.btnRequest.visibility = View.VISIBLE

            // A permissão não foi concedida, solicite ao usuário.
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                codeRequest)

        } else {
            iniciarMapa()
        }

        binding.btnIr.setOnClickListener {
            val localLT = LatLng(localUser.latitude, localUser.longitude)
            linha = functionsMapa.direcao(mMap, localLT, marcador.position)!!
            flag = true
            binding.btnIr.visibility = View.GONE
            binding.btnCancelar.visibility = View.VISIBLE
        }

        binding.btnCancelar.setOnClickListener {
            linha.remove()
            binding.btnCancelar.visibility = View.GONE
            binding.btnIr.visibility = View.VISIBLE
            flag = false
        }

        binding.btnRequest.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), codeRequest)
        }

        BottomSheetBehavior.from(binding.standardBottomSheet).apply {
            peekHeight = 100
            this.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    /**
     * Função responsável por gerenciar a navegação da bottom bar.
     * O usuário pode ir para HomeActivity e AccountActivity
     * @author Thiago
     * */
    private fun navegationLocation(){
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.bottom_location
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_location -> {
                    true
                }
                R.id.bottom_person -> {
                    startActivity(Intent(applicationContext, AccountActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        recreate()
    }

    /**
     * Com o mapa pronto, essa função tem a responsabilidade de ativar a localização do usuário,
     * adicionar os pontos de locação vindos do Firestore da coleção ARMARIO, mover a câmera para
     * a localização do usuário com 15f de zoom. Posteriormente, há o gerenciamento dos clicks
     * nos markers.
     * @param googleMap a instância do google maps
     * */
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Habilitar botão de localização no mapa (isso não requer permissão em runtime no Manifest)
        mMap.isMyLocationEnabled = true

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Tente obter a última localização conhecida do usuário
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            // Verifica se a localização é não-nula
            location?.let {
                // Atualiza a variável localUser com a localização atual
                localUser = it
                // Cria um LatLng com a latitude e longitude da localização
                val latLngUser = LatLng(it.latitude, it.longitude)
                // Move a câmera do mapa para a localização do usuário com um zoom de 15f
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngUser, 15f))
            }
        }

        functionsNuvem.buscaArmarios(functions) {resultado ->
            resultado.let {
                list = it!!
                functionsMapa.adcionarMarcadores(list, mMap, markers)
            }
        }

        mMap.setOnMarkerClickListener(this)
        mMap.setOnMapClickListener { latLng ->
            // Verificar se o clique não está em cima de um marcador
            val clickedMarker = functionsMapa.getMarkerClicked(latLng, markers)

            // Se não houver marcador clicado, ocultar o botão
            if (clickedMarker == null) {
                // Se houver uma rota presente
                if (flag) {
                    binding.btnIr.visibility = View.GONE
                } else {
                    binding.btnCancelar.visibility = View.GONE
                }
            }

            // Expandir o BottomSheet ao clicar em qualquer lugar do mapa
            BottomSheetBehavior.from(binding.standardBottomSheet).apply {
                this.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            codeRequest -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    iniciarMapa()
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        // Usuário negou com "Não perguntar novamente" ou é a primeira vez que negou.
                        binding.tvMapa.visibility = View.VISIBLE
                        binding.btnRequest.visibility = View.VISIBLE
                        binding.tvMapa.text =
                            getString(R.string.liberar_nas_configuracoes)
                    } else {
                        // Usuário negou sem marcar "Não perguntar novamente".
                        binding.tvMapa.visibility = View.VISIBLE
                        binding.btnRequest.visibility = View.VISIBLE
                        binding.tvMapa.text =
                            getString(R.string.permissao_para_mostrar_o_mapa)
                    }
                }
                return
            }
        }
    }

    private fun iniciarMapa() {

        binding.tvMapa.visibility = View.GONE
        binding.btnRequest.visibility = View.GONE

        mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, mapFragment)
            .commit()

        mapFragment.getMapAsync(this)
    }
    @SuppressLint("MissingPermission")
    private fun finalizaMapa() {
        mMap.clear()
        mMap.isMyLocationEnabled = false
        supportFragmentManager.beginTransaction().remove(mapFragment).commit()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (::linha.isInitialized) {
            linha.remove()
        }

        // variável marcador receber o ponto selecionado
        marcador = marker

        functionsNuvem.buscaArmarios(functions) { resultado ->
            resultado.let {
                list = it!!
                functionsMapa.mostrarLocalizacaoAoClicar(list, binding.tvLockerName,
                    binding.tvLockerReference, marker)
                binding.containerHelper.visibility = View.GONE
                binding.containerInfo.visibility = View.VISIBLE
            }
        }

        BottomSheetBehavior.from(binding.standardBottomSheet).apply {
            this.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.btnCancelar.visibility = View.GONE
        binding.btnIr.visibility = View.VISIBLE

        // há rota traçada no mapa
        if (!flag) {
            val gson = Gson()

            // Clicou em alugar armário
            binding.btnRentThisLocation.setOnClickListener {
                val iHomeActivity = Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                // Instanciar um objeto LockerLocation que captura o marker selecionado
                // no mapa
                val lockerClicked = LockerLocation(marker.position.latitude, marker.position
                    .longitude)

                // Serializar o objeto LockerLocation para JSON
                val markerJson = gson.toJson(lockerClicked)

                // Passar no intent o objeto serializado
                iHomeActivity.putExtra("lockerPosition", markerJson)

                startActivity(iHomeActivity)
                finish()
            }

            return false // Retorna true para indicar que o evento foi consumido
        }
        // não há rota traçada no mapa
        else {
            if (::linha.isInitialized) {
                linha.remove()
            }
        }
        return false
    }
}