package org.xhanka.k_map.lib

class Implicant : Comparable<Any> {
    val v: Int // value = 1 indicate that non complement and 0 complement variable
    val m: Int // mask = 1 indicate don't care, 0 indicate that the term is important
    private var c: Boolean // covered by a other implicate

    constructor(v: Int, m: Int, covered: Boolean) {
        this.v = v
        this.m = m
        c = covered
    }

    constructor(v: Int, m: Int) {
        this.v = v
        this.m = m
        c = false
    }

    constructor(v: Int) {
        this.v = v
        m = 0
        c = false
    }

    override fun equals(other: Any?): Boolean {
        return other is Implicant && v == other.v && m == other.m
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 79 * hash + v
        hash = 79 * hash + m
        return hash
    }

    fun bitCount_m(): Int {
        return Integer.bitCount(m)
    }

    fun bitCount_v(): Int {
        return Integer.bitCount(v)
    }

    /*
     * check if the implicant is a prime implicant, i.e., cannot be
     * represented by a simpler (shorter) implicant
     */
    val isPrime: Boolean
        get() = !c

    /*
     * check if the implicant is true for attribution x
     */
    fun isTrue(x: Int): Boolean {
        return v xor x and m.inv() and 0x7FFFFFFF == 0 // up to 31 variables
    }

    fun setC(c: Boolean) {
        this.c = c
    }

    /*
     * Test if two implicants overlap, i.e., in a Karnaugh Map the rectangles
     * have a common area. True if they have it, false if they don't.
     * Based on the premise that there will be overlap if two prime implicants
     * have at least one term (variable or complemented variable) in common and
     * none opposite term (the same variable complemented in one prime implicant
     * and not complemented in the other).
    */
    fun overlap(o: Implicant): Boolean {
        return (m or o.m).inv() and (v xor o.v) == 0
    }

    /*
     * Express the implicant as a boolean expression (product)
     */
    fun toExpressionProd(): StringBuilder {
        val buf = StringBuilder()
        for (i in number_of_in_var - 1 downTo 0) { // start at the most significative bit
            if (m and (1 shl i) == 0) {
                if (v and (1 shl i) == 0) {
                    if (buf.isNotEmpty()) buf.append("\u00b7")
                    buf.append(invar_ec[number_of_in_var - i - 1])
                } else {
                    if (buf.isNotEmpty()) buf.append("\u00b7")
                    buf.append(invar_e[number_of_in_var - i - 1])
                }
            }
        }
        return buf
    }

    /*
     * Express the implicant as a boolean expression (sum)
     */
    fun toExpressionSum(): StringBuilder {
        val buf = StringBuilder()
        for (i in number_of_in_var - 1 downTo 0) { // start at the most significative bit
            if (m and (1 shl i) == 0) {
                if (v and (1 shl i) == 0) {
                    if (buf.isNotEmpty()) buf.append('+')
                    buf.append(invar_e[number_of_in_var - i - 1])
                } else {
                    if (buf.isNotEmpty()) buf.append('+')
                    buf.append(invar_ec[number_of_in_var - i - 1])
                }
            }
        }
        return buf
    }

    fun toExpressionLatexProd(): StringBuilder {
        val buf = StringBuilder()
        for (i in number_of_in_var - 1 downTo 0) { // start at the most significative bit
            if (m and (1 shl i) == 0) {
                if (v and (1 shl i) == 0) {
                    if (buf.isNotEmpty()) buf.append("\\,")
                    buf.append(invar_lc[number_of_in_var - i - 1])
                } else {
                    if (buf.isNotEmpty()) buf.append("\\,")
                    buf.append(invar_l[number_of_in_var - i - 1])
                }
            }
        }
        return buf
    }

    fun toExpressionLatexSum(): StringBuilder {
        val buf = StringBuilder()
        for (i in number_of_in_var - 1 downTo 0) { // start at the most significative bit
            if (m and (1 shl i) == 0) {
                if (v and (1 shl i) == 0) {
                    if (buf.isNotEmpty()) buf.append('+')
                    buf.append(invar_l[number_of_in_var - i - 1])
                } else {
                    if (buf.isNotEmpty()) buf.append('+')
                    buf.append(invar_lc[number_of_in_var - i - 1])
                }
            }
        }
        return buf
    }

    fun toExpressionSTprod(): StringBuilder {
        val buf = StringBuilder()
        for (i in number_of_in_var - 1 downTo 0) { // start at the most significative bit
            if (m and (1 shl i) == 0) {
                if (v and (1 shl i) == 0) {
                    if (buf.isNotEmpty()) buf.append(" AND ")
                    buf.append("NOT(")
                    buf.append(invar_e[number_of_in_var - i - 1])
                    buf.append(')')
                } else {
                    if (buf.isNotEmpty()) buf.append(" AND ")
                    buf.append(invar_e[number_of_in_var - i - 1])
                }
            }
        }
        return buf
    }

    fun toExpressionSTsum(): StringBuilder {
        val buf = StringBuilder()
        for (i in number_of_in_var - 1 downTo 0) { // start at the most significative bit
            if (m and (1 shl i) == 0) {
                if (v and (1 shl i) == 0) {
                    if (buf.isNotEmpty()) buf.append(" OR ")
                    buf.append(invar_e[number_of_in_var - i - 1])
                } else {
                    if (buf.isNotEmpty()) buf.append(" OR ")
                    buf.append("NOT(")
                    buf.append(invar_e[number_of_in_var - i - 1])
                    buf.append(')')
                }
            }
        }
        return buf
    }

    override fun toString(): String {
        val ret = StringBuilder("m($v")
        if (m > 0) {
            val nb = Integer.bitCount(m)
            val mx = 1 shl nb
            val ms = IntArray(MAX_IN_VAR)
            var msp = 0
            var j = 0
            while (msp < nb) {
                if (m and 1 shl j != 0) {
                    ms[msp] = 1 shl j
                    msp++
                }
                j++
            }
            for (i in 1 until mx) {
                var ed = 0
                for (jj in 0 until nb)
                    if (1 shl jj and i != 0)
                        ed = ed or ms[jj]

                ed = ed or v
                ret.append(",").append(ed)
            }
        }
        ret.append(")").append(if (c) " " else "*")
        return ret.toString()
    }

    fun toStringSimp(): String {
        var ret = v.toString()
        if (m > 0) {
            val nb = Integer.bitCount(m)
            val mx = 1 shl nb
            val ms = IntArray(MAX_IN_VAR)
            var msp = 0
            var j = 0

            while (msp < nb) {
                if (m and 1 shl j != 0) {
                    ms[msp] = 1 shl j
                    msp++
                }
                j++
            }
            for (i in 1 until mx) {
                var ed = 0
                for (j1 in 0 until nb)
                    if (1 shl j1 and i != 0)
                        ed = ed or ms[j1]
                ed = ed or v
                ret += ", $ed"
            }
        }
        return ret
    }

    override fun compareTo(other: Any): Int {
        if (Integer.bitCount((other as Implicant).m) != Integer.bitCount(m))
            return Integer.bitCount(other.m) - Integer.bitCount(m)
        return if (m != other.m) m - other.m else other.v - v
    }

    companion object {
        /**
         * Maximum number of input variables. To change this will imply in a lot of code review.
         */
        const val MAX_IN_VAR = 16
        const val MAX_OUT_VAR = 16
        private var number_of_in_var = 0
        private val invar_e = arrayOfNulls<StringBuilder>(MAX_IN_VAR)
        private val invar_ec = arrayOfNulls<StringBuilder>(MAX_IN_VAR)
        private val invar_l = arrayOfNulls<StringBuilder>(MAX_IN_VAR)
        private val invar_lc = arrayOfNulls<StringBuilder>(MAX_IN_VAR)

        /*
     * Initializes variable name lists (complemented and non-complemented), so
     * they are ready to be used in the subexpression of the prime implicant.
     */
        @JvmStatic
        fun startExpression(number_of_in_vars: Int, in_var_names: Array<String>) {
            number_of_in_var = number_of_in_vars
            var tmp: String

            for (i in 0 until number_of_in_var) {
                tmp = in_var_names[i].trim { it <= ' ' }
                invar_e[i] = StringBuilder(tmp)

                val vars = tmp.split("_".toRegex()).toTypedArray()
                val tmpc = StringBuilder()

                for (element in vars[0]) {
                    tmpc.append(element)
                    tmpc.append("'")
                }
                for (j in 1 until vars.size) {
                    tmpc.append("_")
                    tmpc.append(vars[j])
                }
                invar_ec[i] = tmpc
            }
        }

        fun startLatex(number_of_in_vars: Int, in_var_names: Array<String>) {
            number_of_in_var = number_of_in_vars
            for (i in 0 until number_of_in_var) {
                val vars = in_var_names[i].trim { it <= ' ' }.split("_".toRegex()).toTypedArray()
                val tmpc = StringBuilder()
                if (vars[0].length > 1) {
                    tmpc.append("\\overline{")
                    tmpc.append(vars[0])
                    tmpc.append('}')
                } else {
                    tmpc.append("\\bar{")
                    tmpc.append(vars[0])
                    tmpc.append('}')
                }
                val tmpf = StringBuilder()
                for (j in 1 until vars.size) {
                    tmpf.append("_{")
                    tmpf.append(vars[j])
                    tmpf.append("}")
                }
                tmpc.append(tmpf)
                tmpf.insert(0, vars[0])
                invar_l[i] = tmpf
                invar_lc[i] = tmpc
            }
        }

        fun latexInName(i: Int): StringBuilder? {
            return invar_l[i]
        }

        fun latexNorm(`var`: String): StringBuilder {
            val vars = `var`.trim { it <= ' ' }.split("_".toRegex()).toTypedArray()
            val tmp = StringBuilder(vars[0])
            for (j in 1 until vars.size) {
                tmp.append("_{")
                tmp.append(vars[j])
                tmp.append("}")
            }
            return tmp
        }
    }
}