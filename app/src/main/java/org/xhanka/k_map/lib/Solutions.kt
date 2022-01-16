package org.xhanka.k_map.lib

import java.util.*


class Solutions internal constructor() {
    var essentialsPI: ArrayList<Implicant>? = null
    var primeI: ArrayList<ArrayList<Implicant>>? = null

    fun setEssentialPI(essentialsPI: ArrayList<Implicant>?) {
        this.essentialsPI = essentialsPI
    }

    fun setPiSize(s: Int) {
        primeI = ArrayList(s)
    }

    fun addPI(PI: ArrayList<Implicant>) {
        primeI!!.add(PI)
    }

    fun addSolution(size: Int): Boolean {
        return primeI!!.add(ArrayList(size))
    }

    fun addPI(lst: Int, p: Implicant): Boolean {
        return primeI!![lst].add(p)
    }
}
