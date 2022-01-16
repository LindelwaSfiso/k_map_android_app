package org.xhanka.k_map.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import org.xhanka.k_map.R
import org.xhanka.k_map.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var alertDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        alertDialog = AlertDialog.Builder(view.context)
            .setSingleChoiceItems(
                R.array.change_variables_options,
                2
            ) { dialog: DialogInterface, which: Int ->
                when (which) {
                    0 -> binding.kMapView.changeTo2Variables()
                    1 -> binding.kMapView.changeTo3Variables()
                    2 -> binding.kMapView.changeTo4Variables()
                    3 -> binding.kMapView.changeTo5Variables()
                    4 -> binding.kMapView.changeTo6Variables()
                }
                dialog.dismiss()
            }.setTitle("Change the number of variables").create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.k_map_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true

            R.id.action_change_variables -> {
                alertDialog.show()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}