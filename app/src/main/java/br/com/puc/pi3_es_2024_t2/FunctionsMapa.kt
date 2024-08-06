package br.com.puc.pi3_es_2024_t2

import android.graphics.Color
import android.util.Log
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext


class FunctionsMapa {

    fun adcionarMarcadores(list: List<Map<String, *>>, mMap: GoogleMap, markers: MutableList<Marker>) {

        for (armario in list) {
            // Inicialize as variáveis dentro do loop para evitar problemas de escopo
            var nomeArmario: String? = null
            var latArmario: Double? = null
            var lonArmario: Double? = null
            var descricao: String? = null

            // Itera sobre os pares chave-valor de cada armário
            for ((key, value) in armario) {
                when (key) {
                    "nomeArmario" -> nomeArmario = value as String
                    "latArmario" -> latArmario = value as Double
                    "lotArmario" -> lonArmario = value as Double
                    "descricao" -> descricao = value as String
                }
            }

            // Verifica se todos os valores necessários foram encontrados
            if (nomeArmario != null && latArmario != null && lonArmario != null) {
                val position = LatLng(latArmario, lonArmario)
                val marker = mMap.addMarker(MarkerOptions().position(position).title(nomeArmario).snippet(descricao))
                if (marker != null) {
                    markers.add(marker)
                }
            } else {
                Log.e("MarkerError", "Erro ao adicionar marcador: dados de localização inválidos")
            }
        }
    }


    /**
     * Função responsável por personalizar os campos de nome e referência presente na Bottom
     * Sheet com base no ponto de locação selecionado no mapa.
     * @param list Lista dos armários presentes na coleção ARMARIO do Firebase Firestore
     * @param nomeLocalizacao EditText que representa o nome do ponto de locação
     * @param referenciaLocalizacao EditText que representa a referência do ponto de locação
     * @param marker objeto [Marker] capturada por meio do clique do usuário
     * @author Guilherme
     * */
    fun mostrarLocalizacaoAoClicar(list: List<Map<String, *>>,
                                   nomeLocalizacao: TextView,
                                   referenciaLocalizacao: TextView,
                                   marker: Marker) {
        list.forEach {
            location ->
            val latLng = LatLng(location["latArmario"] as Double, location["lotArmario"] as Double)

            if (latLng == marker.position) {
                val nomeArmario = location["nomeArmario"] as String
                val description = location["descricao"] as String

                nomeLocalizacao.text = nomeArmario
                referenciaLocalizacao.text = description

                return
            }
        }
    }

    fun getMarkerClicked(latLng: LatLng, markers: MutableList<Marker>): Marker? {
        for (marker in markers) {
            if (marker.position == latLng) {
                return marker
            }
        }
        return null
    }

    fun direcao(mMap : GoogleMap, origem: LatLng, destino: LatLng): Polyline? {
        val geoApiContext = GeoApiContext.Builder().apiKey("AIzaSyC077_P78qVTGxrvh4-P7P63wKGxw7vis4").build()
        val request = DirectionsApi.newRequest(geoApiContext)
        val direcaoResultado = request.origin(origem.latitude.toString() + "," + origem.longitude.toString())
            .destination(destino.latitude.toString() + "," + destino.longitude.toString()).await()

        if (direcaoResultado.routes != null && direcaoResultado.routes.isNotEmpty()) {
            val rota = direcaoResultado.routes[0] // Pegar a primeira rota, se houver

            // Exibir informações da rota
            Log.d("Rota", "Distância: ${rota.legs[0].distance}")
            Log.d("Rota", "Tempo de viagem: ${rota.legs[0].duration}")

            val pontosLatLng = rota.overviewPolyline.decodePath().map {
                LatLng(it.lat, it.lng)
            }

            // Desenhar polyline da rota no mapa
            val polylineOptions = PolylineOptions()
                .color(Color.RED)
                .width(5f)
                .addAll(pontosLatLng)

            return mMap.addPolyline(polylineOptions)
        } else {
            Log.e("Rota", "Nenhuma rota encontrada")
            return null
        }
    }

}