package br.com.puc.pi3_es_2024_t2

import android.app.Activity
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText
import com.google.android.datatransport.runtime.backends.BackendResponse.Status
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

class FunctionsNuvem {

    fun buscarTipoUsuario(auth: FirebaseAuth, functions: FirebaseFunctions, callback: (String) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            val data = hashMapOf(
                "userUid" to user.uid
            )
            functions
                .getHttpsCallable("getUserType")
                .call(data)
                .addOnSuccessListener { result ->
                    val dataMap = result.data as? Map<*, *>
                    val tipoUsuario = dataMap?.get("payload").toString()
                    callback(tipoUsuario)
                }
                .addOnFailureListener {  result ->
                    callback("Error para encontrar ususario ,${result.message}")
                }
        } else {
            callback("Esse usuario não está cadastrado")
        }
    }

    fun buscaCartao(auth: FirebaseAuth, functions: FirebaseFunctions, callback: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            val data = hashMapOf(
                "userid" to user.uid
            )
            functions
                .getHttpsCallable("consultaCartao")
                .call(data)
                .addOnSuccessListener { result ->
                    val dataMap = result.data as? Map<*, *>
                    val possuiCartao = dataMap?.get("message") == "Possui cartão"
                    if (possuiCartao) {
                        Log.w("Credito", "Cartão encontrado")
                    } else {
                        Log.w("Credito", "Cartão não encontrado")
                    }
                    callback(possuiCartao)
                }
                .addOnFailureListener { exception ->
                    Log.e("Credito", "Erro ao chamar a Cloud Function", exception)
                    callback(false)
                }
        } else {
            Log.e("Credito", "Usuário não autenticado")
            callback(false)
        }
    }

    /**
     * Função responsável por buscar todos os armários cadastrados na coleção
     * ARMARIO do Firebase Firestore.
     * @param auth retorna a instância do objeto FirebaseAuth
     * @param functions retorna a instância da região fornecida
     * @param callback função de retorno que recebe os armários encontrados ou null, e
     * não retorna nenhum valor ([Unit]).
     * @author Vinícius
     * */
    fun buscaArmarios(functions: FirebaseFunctions,
                      callback: (List<Map<String, String>>?) -> Unit) {
        functions
            .getHttpsCallable("consultarArmarios")
            .call()
            .addOnSuccessListener { result ->
                val dataMap = result.data as? Map<*, *>
                if (dataMap != null && dataMap["status"] == "SUCCESS") {
                    val payload = dataMap["payload"] as? Map<*, *>
                    val armarios = payload?.get("armarios") as? List<Map<String, String>>
                    Log.w("Armarios", "${armarios!!.size}")
                    if (armarios.isNotEmpty()) {
                        println(armarios)
                    }
                    callback(armarios)
                    Log.w("Armarios", "Armarios encontrados")
                } else {
                    Log.w("Armarios", "Armário não encontrado")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Armarios", "Erro ao chamar a Cloud Function", exception)
                callback(null)
            }
    }

    /**
     * Cloud Function responsável por realizar uma requisição que vai retornar um armário com base
     * nas coordenadas (Latitude,Longitude).
     * @param latArmario Latitude do armário
     * @param lotArmario Longitude do armário
     * @param functions retorna a instância da região fornecida
     * @param callback função de retorno que recebe o armário encontrado ou null, e
     * não retorna nenhum valor[Unit].
     * @author Guilherme
     **/
    fun getArmarioByLatLng(
        latArmario: Double?,
        lotArmario: Double?,
        functions: FirebaseFunctions,
        callback: (List<*>?) -> Unit) {
        if (latArmario == null || lotArmario == null) {

            Log.e("getArmarioByLatLng", "Latitude ou longitude nula")
            callback(null)
            return
        }

        val data = hashMapOf(
            "latArmario" to latArmario,
            "lotArmario" to lotArmario
        )
        functions
            .getHttpsCallable("getArmarioByLatLng")
            .call(data)
            .addOnSuccessListener { document ->
                val result = document.data as? Map<*, *>

                Log.d("Status", "${result?.get("status")}")

                if (result != null && result["status"] == "SUCCESS") {
                    val payload = result["payload"] as? List<*>
                    callback(payload)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener{ e ->
                Log.e("Armario", "$e")
                callback(null)
            }

    }

    fun cadastrarCartao(activity : Activity, auth: FirebaseAuth, functions: FirebaseFunctions,
                        titular: AppCompatEditText, numero: AppCompatEditText, dataValidade : AppCompatEditText
    ) {
        var user = auth.currentUser!!
        val data = hashMapOf(
            "docIdUser" to user.uid,
            "titular" to titular.text.toString(),
            "numero" to numero.text.toString(),
            "dataValidade" to dataValidade.text.toString()
        )
        functions
            .getHttpsCallable("addCartaoCredito")
            .call(data)
            .addOnSuccessListener { result ->
                val dataMap = result.data as? Map<*, *>
                if (dataMap != null) {
                    val status = dataMap["status"] as? String
                    val message = dataMap["message"] as? String
                    if (status == "SUCCESS") {
                        Log.d("Cartao", "Cartão inserido com sucesso")
                        activity.finish()
                    } else {
                        Log.w("Cartao", "Erro ao inserir Cartão: $message")
                    }
                } else {
                    Log.w("Cartao", "Erro ao processar os dados da Cloud Function")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Cartao", "Erro ao chamar a Cloud Function", exception)
            }

    }

    fun inserirTagLocacao(functions: FirebaseFunctions, qtdTags: Int, idLocacao: String, callback: (String) -> Unit) {
        val data = hashMapOf(
            "idLocacao" to idLocacao,
            "qtdTags" to qtdTags
        )
        functions.getHttpsCallable("inserirTagsLocacao").call(data)
            .addOnSuccessListener {
                val dataMap = it.data as? Map<*, *>
                if (dataMap != null) {
                    val status = dataMap["status"] as? String
                    if (status == "SUCCESS") {
                        val payload = dataMap["payload"] as? Map<*, *>
                        val docId = payload?.get("docId") as? String
                        if (docId != null) {
                            callback(docId)
                            Log.i("inserirTagLocacao", "Operação bem-sucedida: $docId")
                        } else {
                            callback("Falha ao obter docId")
                            Log.e("inserirTagLocacao", "Falha ao obter docId")
                        }
                    } else {
                        callback("Falha ao atualizar qtdTags")
                        Log.e("inserirTagLocacao", "Falha ao atualizar qtdTags: $status")
                    }
                } else {
                    callback("Resposta inválida do servidor")
                    Log.e("inserirTagLocacao", "Resposta inválida do servidor: $it")
                }
            }
            .addOnFailureListener { e ->
                callback("Falha: ${e.message}")
                Log.e("inserirTagLocacao", "Erro ao chamar função: ${e.message}", e)
            }
    }

    fun opLocacao(functions: FirebaseFunctions,
                  docId: String,
                  op: Int,
                  callback: (List<*>?) -> Unit) {
        val data = hashMapOf(
            "op" to op,
            "docId" to docId
        )
        var resposta: MutableList<*>
        functions.getHttpsCallable("opTags").call(data).addOnSuccessListener {
            val dataMap = it.data as? Map <*,*>
            if (dataMap != null) {
                val message = dataMap["message"] as? String
                val status = dataMap["status"] as? String
                if(status == "SUCCESS") {
                    val payload = dataMap["payload"] as? Map<*, *>
                    val qtdTag = payload?.get("qtdTags")
                    val qtdTagAx = payload?.get("qtdTagAx")
                    resposta = mutableListOf(message, qtdTag, qtdTagAx)
                    callback(resposta)
                } else {
                    resposta = mutableListOf(message)
                    callback(resposta)
                }
            } else {
                callback(null)
            }
        }
        .addOnFailureListener{
            resposta = mutableListOf("$it")
            callback(resposta)
        }
    }

    fun encerrarLocacao(functions: FirebaseFunctions,
                        docId: String,
                        callback: (List<*>?) -> Unit) {
        val data = hashMapOf(
            "docId" to docId,
        )
        var resposta: MutableList<*>
        functions.getHttpsCallable("encerrarLocacao").call(data).addOnSuccessListener {
            val dataMap = it.data as? Map <*,*>
            if (dataMap != null) {
                val payload = dataMap["payload"] as? Map<*, *>
                val mensagem = dataMap["message"] as? String
                val reembolso = payload?.get("valorReembolso")
                if (mensagem != null) {
                    resposta = mutableListOf(mensagem, reembolso)
                    callback(resposta)
                }
            }
            callback(null)
        }.addOnFailureListener{
            callback(null)
        }
    }



    fun createLocacao(auth: FirebaseAuth,
                      functions: FirebaseFunctions,
                      latitude: Double,
                      longitude: Double,
                      dateLocacao: String,
                      valorDiaria: Number,
                      valorPago: Number,
                      callback: (String?) -> Unit) {
        val data = hashMapOf(
            "userUid" to auth.currentUser?.uid,
            "latArmario" to latitude,
            "lotArmario" to longitude,
            "dateLocacao" to dateLocacao,
            "valorDiaria" to valorDiaria,
            "valorPago" to valorPago
        )
        functions
            .getHttpsCallable("createLocacao")
            .call(data)
            .addOnSuccessListener { result ->
                val dataMap = result.data as? Map<*, *>
                if (dataMap != null) {
                    val status = dataMap["status"] as? String
                    val message = dataMap["message"] as? String
                    val payload = dataMap["payload"] as? Map<*, *>

                    if (status == "SUCCESS") {
                        Log.d("createLocacao","Locação criada")
                        val docId = payload?.get("docId") as? String
                        if (docId != null) {
                            callback(docId)
                        }
                    } else {
                        Log.w("createLocacao", "Erro ao criar locação: $message")
                        callback(null)
                    }
                } else {
                    Log.w("createLocacao", "Erro ao processar os dados da Cloud Function")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->

                Log.e("createLocacao", "Erro ao chamar a Cloud Function", exception)
            }
    }
}