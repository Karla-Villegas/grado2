package io.gripxtech.odoojsonrpcclient.fragments.miembros

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.reflect.TypeToken
import io.gripxtech.odoojsonrpcclient.*
import io.gripxtech.odoojsonrpcclient.core.Odoo
import io.gripxtech.odoojsonrpcclient.databinding.FragmentMiembrosBinding
import io.gripxtech.odoojsonrpcclient.fragments.miembros.entities.Miembros
import io.gripxtech.odoojsonrpcclient.viewModel.viewModel
import kotlinx.android.synthetic.main.detalles_miembros.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class Fragment_ListaMiembros: Fragment() {

    private var _binding: FragmentMiembrosBinding? = null
    private val binding get() = _binding!!
    private val MiembrosListType = object : TypeToken<ArrayList<Miembros>>() {}.type
    private val MiembroType = object : TypeToken<Miembros>() {}.type
    private lateinit var activity: NewActivityPrincipal private set
    private var believer_id: Long = 0
    private var rol_id: Int = 0
    private lateinit var IcMiembros: Any
    private var items = ArrayList<Miembros>()
    private var list_name_departament: ArrayList<String> = arrayListOf()
    private lateinit var progressBar: ProgressBar
    private lateinit var viewModel: viewModel


    val adapter: AdapterMiembros by lazy {
        AdapterMiembros(this, arrayListOf())
    }

    val adapterMnisterio: Adapter_MinisterioB by lazy {
        Adapter_MinisterioB(this, arrayListOf())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMiembrosBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity = getActivity() as NewActivityPrincipal
        viewModel = ViewModelProvider(this).get(io.gripxtech.odoojsonrpcclient.viewModel.viewModel::class.java)
        progressBar = ProgressBar()
        binding.rvMiembros.layoutManager = LinearLayoutManager(requireContext())
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.rvMiembros.layoutManager = layoutManager
        binding.rvMiembros.adapter = adapter

        adapter.setupScrollListener(binding.rvMiembros)

        binding.srlMiembros.setOnRefreshListener {
            items.clear()
            adapter.clear()
            if (!adapter.hasMoreListener()) {
                adapter.showMore()
                fetchMiembros()
            }
            binding.srlMiembros.post {
                binding.srlMiembros.isRefreshing = false
            }
        }
        fethRol()
      /*  fetchMiembros()

        binding.button.setOnClickListener {
            findNavController().navigate(R.id.action_nav_miembros_to_registro)
        }*/
    }

    private fun fethRol() {
        CoroutineScope(IO).launch {
            val db = App.database.userInfoDao().getUser()
            withContext(Main){
                Log.e("ID USER", "MI CUENTA ${db}" )
                val part_id = db?.serverId
                Log.e("ID part_id", "MI CUENTA ${part_id}" )
                if (part_id != null ){
                    roles(part_id)
                }

            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun roles(partId: Int) {
        Odoo.route("/roles/$partId", "", args = "") {
            this.onNext {
                if (it.isSuccessful) {
                    val call = it.body()!!
                    if (it.isSuccessful) {
                        val roles = call.result.asString.toJsonObject().get("is_admin")
                        val boo: Boolean = roles.asBoolean
                        Log.e("resultado is_admin", "roles: ${boo}")


                        if(!boo.equals(false)){
                            Log.e("IF", "roles: IF", )
                            fetchMiembros()
                            binding.button.visibility = View.VISIBLE
                            binding.button.setOnClickListener {
                                findNavController().navigate(R.id.action_nav_miembros_to_registro)
                            }

                        }else{
                            fetchMiembros()
                            binding.button.visibility = View.GONE
                        }



                    } else {
                        Timber.w("callkw() failed with ${it.errorBody()}")
                    }
                } else {
                    Timber.w("request failed with ${it.code()}:${it.message()}")
                }
            }
            this.onError { error ->
                error.printStackTrace()
            }
            this.onComplete { }
        }
    }

    private fun fetchMiembros() {
        binding.shimmerLayout.startShimmer()
        Odoo.route("/believers", "", args = "") {
            this.onNext {
                if (it.isSuccessful) {
                    val call = it.body()!!
                    if (it.isSuccessful) {
                        showRecyclerView()
                        adapter.hideEmpty()
                        adapter.hideError()
                        adapter.hideMore()
                        binding.srlMiembros.isEnabled = true
                        val result = call.result.asString.toJsonObject()
                        val icMiembros = result.get("records")
                        items = gson.fromJson<ArrayList<Miembros>>(icMiembros, MiembrosListType )

                        if (binding.rvMiembros != null) { OnClick() }
                        adapter.addRowItems(items)

                        Timber.e("ITEMS believers--->  ${items}")
                    } else {
                        Timber.w("callkw() failed with ${it.errorBody()}")
                    }
                } else {
                    Timber.w("request failed with ${it.code()}:${it.message()}")
                }
            }
            this.onError { error ->
                error.printStackTrace()
            }
            this.onComplete { }
        }
    }

    private fun OnClick(){
        binding.rvMiembros.onItemClick{recyclerView, position, v ->
            list_name_departament.clear()
            adapterMnisterio.clear()
            progressBar.progressbar(requireContext(), "Cargando...")
            if(!items.isEmpty()){
                if(adapter.starClick){
                    adapter.starClick = false
                    IcMiembros = items.get(position)
                    believer_id = (IcMiembros as Miembros).serverId
                    Timber.w("OnClick id miembro ${believer_id}")
                    detalleBeliever(believer_id.toInt(), v)
                }
            }
        }
    }

    private fun detalleBeliever(believerId: Int, v: View) {
        Odoo.route("believer/$believerId", "", args = "") {
            this.onNext {
                if (it.isSuccessful) {
                    val call = it.body()!!
                    if (it.isSuccessful) {
                        list_name_departament.clear()
                        adapterMnisterio.clear()
                        val result = call.result.asString.toJsonObject().get("record")
                        val item = gson.fromJson<Miembros>(result, MiembroType)
                        Timber.e("item --->  ${item}")
                        val AlertDialog = AlertDialog.Builder(requireContext()).create()
                        val view = layoutInflater.inflate(R.layout.detalles_miembros, null)
                        view.rv_miembro_ministerio.layoutManager = LinearLayoutManager(requireContext())
                        view.rv_miembro_ministerio.adapter = adapterMnisterio
                        view.rv_miembro_ministerio.setHasFixedSize(true)
                        progressBar.finishProgressBar()

                        /** lógica para adaptar los campos de los detalles del believer*/
                        val name = item.name
                        val identity = item.identity
                        val state = JSONObject(item.state.toString()).optString("name")
                        val municipality = JSONObject(item.municipality.toString()).optString("name")
                        val parish = JSONObject(item.parish.toString()).optString("name")
                        val sector = item.sector
                        val street = item.street
                        val building = item.building
                        val house = item.house
                        val localphone_number = item.localphone_number
                        val cellphone_number = item.cellphone_number


                        if(name != "false") view.DET_nombreMiembro.text = name else view.DET_nombreMiembro.text = ""
                        if(identity != "false") view.DET_cedulaMiembro.text = identity else view.DET_cedulaMiembro.text = ""
                        if(localphone_number != "false") view.DET_telefonoMiembro.text = localphone_number else view.DET_telefonoMiembro.text = ""
                        if(state != "false") view.DET_estadoMiembro.text = state else view.DET_estadoMiembro.text = ""
                        if(municipality != "false") view.DET_municipioMiembro.text = municipality else view.DET_municipioMiembro.text = ""
                        if(parish != "false") view.DET_parroquiaMiembro.text = parish else view.DET_parroquiaMiembro.text = ""
                        if(sector != "false") view.DET_sectorMiembro.text = sector else view.DET_sectorMiembro.text = ""
                        if(street != "false") view.DET_calleMiembro.text = street else view.DET_calleMiembro.text = ""
                        if(building != "false") view.DET_edificioMiembro.text = building else view.DET_edificioMiembro.text = ""
                        if(house != "false") view.DET_NumeroEdificioMiembro.text = house else view.DET_NumeroEdificioMiembro.text = ""


                        val department_ids = JSONArray(item.department_ids.toString())
                        for (i in 0 until department_ids.length()){
                            val nameMinisterio = department_ids.getJSONObject(i).optString("name")
                            list_name_departament.add(nameMinisterio)
                        }

                        if(list_name_departament.size > 0){
                            adapterMnisterio.addRowItems(list_name_departament as List<JSONArray>)
                        }else{
                            val rv_miembro_ministerio = view.findViewById<RecyclerView>(R.id.rv_miembro_ministerio)
                            val textChat = view.findViewById<TextView>(R.id.textChat)
                            rv_miembro_ministerio.visibility = View.GONE
                            textChat.visibility = View.VISIBLE
                        }
                        /****************************************************************/

                        AlertDialog.setView(view)
                        AlertDialog.setCancelable(true)
                        AlertDialog.setOnDismissListener { adapter.starClick = true }
                        val idIconReturn = view.findViewById<ImageView>(R.id.idIconReturn)
                        idIconReturn.setOnClickListener {
                            AlertDialog.dismiss()
                            adapter.setCanStart(true)
                            list_name_departament.clear()
                            adapterMnisterio.clear()
                            Log.e("DETALLE list 3--->", "${  list_name_departament}")
                        }
                        AlertDialog.show()
                        AlertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        AlertDialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

                    } else {
                        progressBar.finishProgressBar()
                        Timber.w("callkw() failed with ${it.errorBody()}")

                    }
                } else {
                    progressBar.finishProgressBar()
                    Timber.w("request failed with ${it.code()}:${it.message()}")
                }
            }
            this.onError { error ->
                progressBar.finishProgressBar()
                error.printStackTrace()
            }
            this.onComplete { }
        }
    }

    private fun showRecyclerView() {
        binding.shimmerLayout.apply {
            stopShimmer()
            visibility = View.GONE
        }
        binding.rvMiembros.visibility = View.VISIBLE
        binding.srlMiembros.visibility = View.VISIBLE
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}