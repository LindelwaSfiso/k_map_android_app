package org.xhanka.k_map.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.text.Html
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.material.card.MaterialCardView
import org.xhanka.k_map.R
import org.xhanka.k_map.lib.Implicant
import org.xhanka.k_map.lib.Solver
import java.util.*

/**
 *
 * Class for implementing the Karnaugh Map used for boolean functions reduction
 * @author Dlamini Lindelwa <A mailto:sfisolindelwa@gmail.com>[sfisolindelwa@gmail.com]</A>
 */
class KMapView : FrameLayout {
    private lateinit var recyclerView: RecyclerView
    private lateinit var textView: TextView

    constructor(context: Context?) : super(context!!)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        val view = LayoutInflater.from(context).inflate(R.layout.k_map_main, this, true)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 5)

        // recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        recyclerView.addItemDecoration(SpaceDecorator())
        textView = view.findViewById(R.id.display)

        // truthTableButton = view.findViewById(R.id.truthTableButton)

        recyclerView.adapter = Adapter(
            a4Variables, textView, 4, "AB", "CD"
        )
    }

    internal interface CLICK {
        fun click(textView: TextView, number: String?)
    }

    private class Adapter(
        strings: ArrayList<String>,
        textView: TextView?,
        numberOfInputVariables: Int,
        textSide: String,
        textTop: String
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), CLICK {

        var stringArrayList: ArrayList<String>
        var textViewList: MutableList<TextView> = ArrayList()
        var displayTextView: TextView?
        var labelTop: String
        var labelSide: String
        var numberOfInputVariables: Int
        val numberOfOutputVariables = 1
        var charValues: Array<CharArray>
        var inputVariablesNames: Array<String?>
        var karnaughInOrder = IntArray(Implicant.MAX_IN_VAR)

        override fun click(textView: TextView, number: String?) {
            when (textView.text) {
                "1" -> textView.text = Solver.DONT_CARE_CHAR.toString()
                Solver.DONT_CARE_CHAR.toString() -> textView.text = ""
                else -> textView.text = "1"
            }

            textViewList.sortWith { o1: TextView, o2: TextView ->
                o1.id.compareTo(o2.id)
            }

            // initialize charValues with data
            for (i in 0 until (1 shl numberOfInputVariables)) {
                for (j in 0 until numberOfOutputVariables) {
                    var value = "0"
                    try {
                        value = textViewList[i].text.toString()
                    } catch (ignore: IndexOutOfBoundsException) {
                        // An indexOutOfBoundsException can occur if texView has not been inflated yet
                        // to handle this, we assume a view not inflated holds a zero minterm
                    }

                    when (value) {
                        "1" -> charValues[i][j] = '1'
                        Solver.DONT_CARE_CHAR.toString() -> charValues[i][j] = Solver.DONT_CARE_CHAR
                        else -> charValues[i][j] = '0'
                    }
                }
            }
            try {
                val solver = Solver(
                    charValues,
                    numberOfInputVariables,
                    inputVariablesNames,
                    numberOfOutputVariables,
                    OUTPUT_VARIABLES,
                    true,
                    true,
                    false,
                    false,  // true
                    true,
                    false,
                    karnaughInOrder
                )
                solver.Solve()
                solver.run()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    displayTextView!!.text =
                        Html.fromHtml(solver.solution, Html.FROM_HTML_MODE_LEGACY)
                } else displayTextView!!.text =
                    HtmlCompat.fromHtml(solver.solution, HtmlCompat.FROM_HTML_MODE_COMPACT)

                // Paint background according to groups
                val colors = solver.borD_COLORS
                for (i in colors.first.indices) {
                    val id = colors.first[i]
                    val color = colors.second[i]
                    textViewList[id].setBackgroundColor(color)
                }
                //truthTable = solver.getTruthTableHTML();
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                EMPTY_VIEW -> EmptyViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.k_map_empty, parent, false
                    )
                )

                HEADER_VIEW -> HeaderViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.k_map_header, parent, false
                    )
                )

                else -> ContentViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.k_map_content, parent, false
                    )
                )
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewType = getItemViewType(position)

            if (viewType == HEADER_VIEW) configureHeader(
                holder as HeaderViewHolder,
                position
            ) else if (viewType == CONTENT_VIEW) configureContent(
                holder as ContentViewHolder,
                position
            )
        }

        fun configureHeader(holder: HeaderViewHolder, position: Int) {
            if (numberOfInputVariables < 5) {
                holder.sideBinary.text = stringArrayList[position]
                if (position == 1) holder.sideLabel.text = labelTop
                else if (position == 5) holder.sideLabel.text = labelSide
                if (position % 5 == 0) holder.sideContainer.rotation = -90f
            } else {
                holder.sideBinary.text = stringArrayList[position]
                holder.sideContainer.setPadding(4, 4, 4, 4)

                if (numberOfInputVariables > 5) holder.sideLabel.textSize = 14f

                if (if (numberOfInputVariables == 5) position == 3 else position == 7) // (position == 3 && numberOfInputVariables == 5) || (position == 7 && numberOfInputVariables == 6))
                    holder.sideLabel.text = labelTop // side label top
                else if (if (numberOfInputVariables == 5) position == 9 else position == 17) // (numberOfInputVariables == 5 && position == 9) || (numberOfInputVariables == 6 && position == 17))
                    holder.sideLabel.text = labelSide // side label text
                if (if (numberOfInputVariables == 5) (position + 1) % 5 == 0 else (position + 1) % 9 == 0) holder.sideContainer.rotation =
                    90f
            }
        }

        @SuppressLint("ResourceType")
        fun configureContent(holder: ContentViewHolder, position: Int) {
            holder.contentNumber.text = stringArrayList[position]
            holder.contentContent.id = stringArrayList[position].toInt()

            if (!textViewList.contains(holder.contentContent)) textViewList.add(holder.contentContent)

            holder.contentContainer.setOnClickListener {
                click(
                    holder.contentContent,
                    stringArrayList[position]
                )
            }

            // todo move to viewHolder
            if (numberOfInputVariables >= 5) holder.contentContent.setPadding(4, 4, 4, 4)
            if (position >= 25 && numberOfInputVariables == 5) holder.contentContainer.strokeColor =
                Color.argb(100, 63, 81, 181) else if (numberOfInputVariables == 6) {
                val id = holder.contentContent.id

                if (id < 16) holder.contentContainer.strokeColor = Color.argb(
                    255,
                    245,
                    0,
                    87
                ) else if (id < 32) holder.contentContainer.strokeColor = Color.argb(
                    255,
                    0,
                    150,
                    138
                ) else if (id < 48) holder.contentContainer.strokeColor =
                    Color.argb(255, 255, 145, 0) else holder.contentContainer.strokeColor =
                    Color.argb(255, 63, 81, 181)
            }
        }

        override fun getItemCount(): Int {
            return stringArrayList.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItemViewType(position: Int): Int {
            // configure for less than 5 variables

            // configure empty view
            if (numberOfInputVariables < 5 && position == 0 || numberOfInputVariables == 5 && position == 4 || numberOfInputVariables > 5 && position == 8) return EMPTY_VIEW

            // configure header view
            if (if (numberOfInputVariables <= 5) position < 5 else position < 9) return HEADER_VIEW // top header
            if (numberOfInputVariables < 5 && position % 5 == 0 || numberOfInputVariables == 5 && (position + 1) % 5 == 0) return HEADER_VIEW // side header for <= 5 variables
            return if (numberOfInputVariables > 5 && (position + 1) % 9 == 0) HEADER_VIEW else CONTENT_VIEW // side header
        }

        class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            init {
                itemView.visibility = GONE
            }
        }

        class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var sideContainer: LinearLayout = itemView.findViewById(R.id.sideContainer)
            var sideLabel: TextView = itemView.findViewById(R.id.kMapLabelMain)
            var sideBinary: TextView = itemView.findViewById(R.id.kMapLabelBinary)

        }

        class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var contentContainer: MaterialCardView = itemView.findViewById(R.id.contentContainer)
            var contentNumber: TextView = itemView.findViewById(R.id.kMapNumber)
            var contentContent: TextView = itemView.findViewById(R.id.kMapContent)

        }

        init {
            setHasStableIds(true)
            stringArrayList = strings
            displayTextView = textView
            labelTop = textTop
            labelSide = textSide
            this.numberOfInputVariables = numberOfInputVariables
            charValues =
                Array(1 shl numberOfInputVariables) { CharArray(numberOfOutputVariables) } // {'0'}, {'0'}, {'0'}, {'1'}, {'1'}, {'1'}, {'1'}, {'1'}, // {'0'}, {'0'}, {'0'}, {'1'}, {'1'}, {'1'}, {'1'}, {'1'}};
            for (i in 0 until Implicant.MAX_IN_VAR) karnaughInOrder[i] = i
            inputVariablesNames = arrayOfNulls(numberOfInputVariables)
            System.arraycopy(INPUT_VARIABLES, 0, inputVariablesNames, 0, numberOfInputVariables)
        }
    }

    fun changeTo6Variables() {
        (Objects.requireNonNull(recyclerView.layoutManager) as GridLayoutManager).spanCount =
            9
        recyclerView.adapter = Adapter(
            a6Variables,
            textView,
            6,
            "BEF",
            "ACD"
        )
        recyclerView.setItemViewCacheSize(a6Variables.size)
    }

    fun changeTo5Variables() {
        (Objects.requireNonNull(recyclerView.layoutManager) as GridLayoutManager).spanCount =
            5
        recyclerView.adapter = Adapter(
            a5Variables,
            textView,
            5,
            "CDE",
            "AB"
        )
        recyclerView.setItemViewCacheSize(a5Variables.size)
    }

    fun changeTo4Variables() {
        (Objects.requireNonNull(recyclerView.layoutManager) as GridLayoutManager).spanCount = 5
        recyclerView.adapter = Adapter(
            a4Variables,
            textView,
            4,
            "AB",
            "CD"
        )
        recyclerView.setItemViewCacheSize(a4Variables.size)
    }

    fun changeTo3Variables() {
        (Objects.requireNonNull(recyclerView.layoutManager) as GridLayoutManager).spanCount = 5
        recyclerView.adapter = Adapter(
            a3Variables,
            textView,
            3,
            "A",
            "BC"
        )
        recyclerView.setItemViewCacheSize(a3Variables.size)
    }

    fun changeTo2Variables() {
        (Objects.requireNonNull(recyclerView.layoutManager) as GridLayoutManager).spanCount =
            5
        recyclerView.adapter = Adapter(
            a2Variables,
            textView,
            2,
            "",
            "AB"
        )
    }

    private class SpaceDecorator : ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val p = parent.getChildAdapterPosition(view)
            val adapter = parent.adapter as Adapter?

            if (p in 20..24 && adapter?.numberOfInputVariables == 5)
                outRect.bottom += 12
            if (p in 35..43 && adapter?.numberOfInputVariables == 6)
                outRect.bottom += 12

            outRect.right += 3
        }
    }

    companion object {
        private val EMPTY_VIEW = 0
        private val HEADER_VIEW = 1
        private val CONTENT_VIEW = 2

        private val a6Variables = ArrayList(
            listOf(
                "110", "111", "101", "100", "010", "011", "001", "000", "",
                "40", "44", "36", "32", "8", "12", "4", "0", "000",
                "41", "45", "37", "33", "9", "13", "5", "1", "001",
                "43", "47", "39", "35", "11", "15", "7", "3", "011",
                "42", "46", "38", "34", "10", "14", "6", "2", "010",
                "58", "62", "54", "50", "26", "30", "22", "18", "110",
                "59", "63", "55", "51", "27", "31", "23", "19", "111",
                "57", "61", "53", "49", "25", "29", "21", "17", "101",
                "56", "60", "52", "48", "24", "28", "20", "16", "100"
            )
        )
        private val a5Variables = ArrayList(
            listOf(
                "10", "11", "10", "00", "",
                "8", "12", "4", "0", "000",
                "9", "13", "5", "1", "001",
                "11", "15", "7", "3", "011",
                "10", "14", "6", "2", "010",
                "26", "30", "22", "18", "110",
                "27", "31", "23", "19", "111",
                "25", "29", "21", "17", "101",
                "24", "28", "20", "16", "100"
            )
        )
        private val a4Variables = ArrayList(
            listOf(
                "", "00", "01", "11", "10",
                "00", "0", "1", "3", "2",
                "01", "4", "5", "7", "6",
                "11", "12", "13", "15", "14",
                "10", "8", "9", "11", "10"
            )
        )
        private val a3Variables = ArrayList(
            listOf(
                "", "00", "01", "11", "10",
                "0", "0", "1", "3", "2",
                "1", "4", "5", "7", "6"
            )
        )
        private val a2Variables = ArrayList(
            listOf(
                "", "00", "01", "11", "10", "", "0", "1", "3", "2"
            )
        )
        val INPUT_VARIABLES = arrayOf("A", "B", "C", "D", "E", "F")
        val OUTPUT_VARIABLES = arrayOf("G")
    }
}