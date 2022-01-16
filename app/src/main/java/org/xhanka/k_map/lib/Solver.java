/*
 *<JQM - Java Quine McCluskey>
 *Copyright (C) <2016>  <Luis Paulo Laus, laus@utfpr.edu.br>
 *
 *This program is free software: you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation, either version 3 of the License, or
 *(at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


///////////////////////////////////////////////////////////////////////

/*
 * class solver has the functions that minimize the truth table
 * using Quine–McCluskey algorithm and call the output class to
 * format the answer in a readable string
 */
package org.xhanka.k_map.lib;

import android.graphics.Color;
import android.util.Log;
import android.util.Pair;

import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Solver implements Runnable {

    public static final char DONT_CARE_CHAR = '-';

    private static final int MAX_VAR_MEM = 10;       // used to allocate memory in an amount sufficient to guaranty
    // this number of input variables, note that 10 variables requites 59.049 registries and 16 variables, 43.046.721

    private final char[][] values;

    private static int number_of_in_var;
    private final int number_of_out_var;
    private final int[] KarnaughInOrder;


    private final String[] in_var_names;
    private final String[] out_var_names;

    private final boolean sum_of_products_not_product_of_sums;
    private final boolean all_possible_not_just_one;
    private final boolean optimize_number_of_terms_not_variables;

    private final boolean expression;
    private final boolean expressionSorted;
    private final boolean QuineMcCluskey;

    private final StringBuilder solution;
    private final StringBuilder QMinternals;

    private static long[] t1Time, t2Time, t3Time;

    ArrayList<Solutions> solutions;

    public Solver(
            char[][] values,
            int number_of_in_var,
            String[] in_var_names,
            int number_of_out_var,
            String[] out_var_names,
            boolean sum_of_products_not_product_of_sums,
            boolean all_possible_not_just_one,
            boolean optimize_number_of_terms_not_variables,
            boolean expression,
            boolean expressionSorted,
            boolean QuineMcCluskey,
            int[] KarnaughInOrder
    ) {
        this.values = values;
        Solver.number_of_in_var = number_of_in_var;
        this.in_var_names = in_var_names;
        this.number_of_out_var = number_of_out_var;
        this.out_var_names = out_var_names;
        this.sum_of_products_not_product_of_sums = sum_of_products_not_product_of_sums;
        this.all_possible_not_just_one = all_possible_not_just_one;
        this.optimize_number_of_terms_not_variables = optimize_number_of_terms_not_variables;
        this.expression = expression;
        this.expressionSorted = expressionSorted;
        this.QuineMcCluskey = QuineMcCluskey;
        this.KarnaughInOrder = KarnaughInOrder;

        solution = new StringBuilder();
        QMinternals = new StringBuilder();

        t1Time = new long[Implicant.MAX_OUT_VAR];
        t2Time = new long[Implicant.MAX_OUT_VAR];
        t3Time = new long[Implicant.MAX_OUT_VAR];
    }

    /*
     * This version uses array instead of ArrayList. As consequences, it
     * optimizes the use of memory and CPU running much faster than the
     * original version.
     */
    boolean perfectTruth = true;

    public void Solve() {
        solutions = new ArrayList<>(number_of_out_var);
        int maxs; // maximum number of implicants
        Implicant[] lstPrimes;

        if (QuineMcCluskey) {
            Implicant.startExpression(number_of_in_var, in_var_names);
        }

        for (int f = 0; f < number_of_out_var; f++) {
            long startTime = System.nanoTime(); // time test
            ArrayList<Integer> lstOnes = new ArrayList<>(1 << number_of_in_var);
            ArrayList<Integer> lstOnesAux = null;
            solutions.add(new Solutions());
            if (number_of_in_var > MAX_VAR_MEM) {
                maxs = (int) Math.pow(3, MAX_VAR_MEM);
            } else {
                maxs = (int) Math.pow(3, number_of_in_var);
            }
            lstPrimes = new Implicant[maxs];
            // fill arrays with data from the truth table
            int mx = 1 << number_of_in_var;
            int ct1 = 0;   // number of ones
            int ctdc1 = 0;  // mumber of dont care and one
            for (int i = 0; i < mx; i++) {
                if (sum_of_products_not_product_of_sums) {
                    if (values[i][f] == '1') {
                        lstOnes.add(i);
                        ct1++;
                    }
                    if (values[i][f] != '0') {
                        lstPrimes[ctdc1] = new Implicant(i);
                        ctdc1++;
                    } else
                        perfectTruth = false;
                } else {
                    if (values[i][f] == '0') {
                        lstOnes.add(i);
                        ct1++;
                    }
                    if (values[i][f] != '1') {
                        lstPrimes[ctdc1] = new Implicant(i);
                        ctdc1++;
                    } else
                        perfectTruth = false;
                }
            }

            if ((ctdc1 == mx) || (ct1 == 0) || perfectTruth) {
                continue;
            }

            // sort the list of implicants by the number of ones, then value
            Arrays.sort(lstPrimes, 0, ctdc1, (Implicant r1, Implicant r2) ->
            {
                int r1b = r1.bitCount_v();
                int r2b = r2.bitCount_v();
                if (r1b != r2b)
                    return r1b - r2b;
                return r1.getV() - r2.getV();
            });
            // begin and end of each sublist
            int[] e = new int[number_of_in_var + 2];
            int epos = 1;
            int ct = 0;
            e[0] = 0;
            for (int i = 0; i < ctdc1; i++) {
                int ctt = lstPrimes[i].bitCount_v();
                while (ctt > ct) {
                    ct++;
                    e[epos] = i;
                    epos++;
                }
            }
            e[epos] = ctdc1;

            t1Time[f] = System.nanoTime() - startTime; // time test
//            System.out.println("Sorting time: " + (endTime - startTime) + " for " + number_of_in_var + " variables");

            boolean grouped = true;
            ct = number_of_in_var;
            while (grouped) {
                grouped = false;
                for (int i = 0; i < ct; i++) {
                    if (e[i] == e[i + 1])
                        continue;
                    int newstart = ctdc1;
//                    System.out.println("at " + i + " from " + e[i] + " to " + (e[i+1]-1) + " and from " + e[i+1] + " to " + (e[i+2]-1));
                    for (int j = e[i]; j < e[i + 1]; j++) {
                        Implicant tempj = lstPrimes[j];
                        for (int k = e[i + 1]; k < e[i + 2]; k++) {
                            if (tempj.getM() == lstPrimes[k].getM()) { // same mask?
                                int d = tempj.getV() ^ lstPrimes[k].getV();
                                if (Integer.bitCount(d) == 1) { // Hamming distance is 1?
                                    Implicant np = new Implicant(tempj.getV(), tempj.getM() | d);
                                    // look for this prime implicant in the already found list of
                                    // prime implicants, but only in this section of the list
/*
                                    // linear search
                                    int x;
                                    for (x = newstart; x < ctdc1; x++) {
                                        if (np.equals(lstPrimes[x])) {
                                            break;
                                        }
                                    }
                                    if(x == ctdc1) { // new prime implicant found
                                            if (maxs == ctdc1)
                                                throw new OutOfMemoryError(java.util.ResourceBundle.getBundle("QMC").getString("LSTPRIME"));

                                            lstPrimes[ctdc1] = np;
                                            ctdc1++;
                                    }
*/
                                    d = Arrays.binarySearch(lstPrimes, newstart, ctdc1, np, (Implicant r1, Implicant r2) ->
                                    {
//                                        System.out.println("Comp " + r1 + " with " + r2);
                                        return ((r1.getV() - r2.getV()) << Implicant.MAX_IN_VAR) + r1.getM() - r2.getM();
                                    });
//                                    System.out.print("trying " + tempj + " with " + lstPrimes[k] + " " + d);
                                    if (d < 0) { // new prime implicant found
                                        if (maxs == ctdc1)
                                            throw new OutOfMemoryError(java.util.ResourceBundle.getBundle("QMC").getString("LSTPRIME"));

                                        lstPrimes[ctdc1] = np;
                                        ctdc1++;
                                        // sort again, no big deal because the list is already sorted with the exception of the new element
                                        Arrays.sort(lstPrimes, newstart, ctdc1, (Implicant r1, Implicant r2) ->
                                        {
                                            return ((r1.getV() - r2.getV()) << Implicant.MAX_IN_VAR) + r1.getM() - r2.getM();
                                        });
//                                            System.out.println(" OK " + ctdc1);
                                    }
//                                    else
//                                        System.out.println("");
                                    tempj.setC(true);
                                    lstPrimes[k].setC(true);
                                    grouped = true;
                                }
                            }
                        }
                    }
                    e[i] = newstart;
                }
                e[ct] = ctdc1;
                ct--;
            }
//            System.out.println("Implicantes " + ctdc1);
            // list all prime implicants
            ArrayList<Implicant> lstPrime = new ArrayList<>(number_of_in_var);
            for (int i = ctdc1 - 1; i >= 0; i--)
                if (lstPrimes[i].isPrime())
                    lstPrime.add(lstPrimes[i]);
            t2Time[f] = System.nanoTime() - startTime; // time test
//            endTime = System.nanoTime(); // time test
//            System.out.println("Execution time: " + (endTime - startTime) + " for " + number_of_in_var + " variables");

            /*
             * Look for essential prime implicants
             * Not of cardinal importance because Petrick’s Method can deal with
             * essentials and non-essentials prime implicants. But, if the
             * essential prime implicants are removed, Petrick’s Method will
             * deal only with non-essentials prime implicants. Since this
             * implemententations of Petrick’s Method is limited to 64 prime
             * implicant, removing the essentials ones is important for very
             * big functions.
             */
            if (QuineMcCluskey) {
//                Collections.sort(lstPrime); // test
                lstOnesAux = (ArrayList<Integer>) lstOnes.clone();
            }
            ArrayList<Implicant> lstPrimeEssentials = new ArrayList<>(lstOnes.size());

            for (int i = 0; i < lstOnes.size(); i++) {
                int pos = -1;
                int j;
                for (j = 0; j < lstPrime.size(); j++) {
//                    System.out.println("("+i+","+j+") "+lstPrime.get(j) + " " + lstOnes.get(i) + " " + lstPrime.get(j).isTrue(lstOnes.get(i)));
                    if (lstPrime.get(j).isTrue(lstOnes.get(i))) {
                        if (pos >= 0) {
                            break;
                        }
                        pos = j;
                    }
                }
                if ((j == lstPrime.size()) && (pos >= 0)) {
                    Implicant epi = lstPrime.remove(pos);
                    // remove all ones covered by the essential prime implicant
                    for (j = lstOnes.size() - 1; j >= 0; j--) {
                        if (epi.isTrue(lstOnes.get(j))) {
                            lstOnes.remove(j);
                        }
                    }
                    lstPrimeEssentials.add(epi);
                    i = -1;
                }
            }
            if (QuineMcCluskey) {
                boolean essentialCover[] = new boolean[lstOnesAux.size()];
                for (int i = 0; i < lstOnesAux.size(); i++) {
                    essentialCover[i] = false;
                    for (int j = 0; j < lstPrimeEssentials.size(); j++) {
                        if (lstPrimeEssentials.get(j).isTrue(lstOnesAux.get(i))) {
                            essentialCover[i] = true;
                            break;
                        }
                    }
                }

                // QMinternals.append(resourceBundle.getString("QMCIMPLICANTS"));
                QMinternals.append(out_var_names[f]);
                QMinternals.append("<br><table cellspacing=\"1\" cellpadding=\"5\" margin-bottom=\"20px\" margin-top=\"20px\" border=\"0\" border-collapse=\"collapse\" align=\"center\" font=\"#DEFAULT\" color=\"blue\" bgcolor=\"blue\"><tr><td bgcolor=\"#FAF0E6\" align=\"center\">#</td>");
                for (int i = 0; i < number_of_in_var; i++) {
                    QMinternals.append("<td bgcolor=\"#FAF0E6\" align=\"center\">");
                    QMinternals.append(in_var_names[i]);
                    QMinternals.append("</td>");
                }
                QMinternals.append("<td bgcolor=\"#FAF0E6\"></td></tr>");

                for (int lbc = -1, i = 0; i < ctdc1; i++) {
                    if (lbc != lstPrimes[i].bitCount_v()) {
                        lbc = lstPrimes[i].bitCount_v();
                        QMinternals.append("<tr><td bgcolor=\"#FAF0E6\"></td><td bgcolor=\"#CDD2E6\" align=\"center\" colspan=\""); // CDD2E6
                        QMinternals.append(number_of_in_var);
                        QMinternals.append("\">");
                        QMinternals.append(lbc);
                        QMinternals.append("</td><td bgcolor=\"#FAF0E6\"></td></tr>");
                    }
                    QMinternals.append("<tr><td bgcolor=\"#FAF0E6\" align=\"right\">");
                    QMinternals.append(lstPrimes[i].toStringSimp());
                    QMinternals.append("</td>");
                    for (int j = 0; j < number_of_in_var; j++) {
                        QMinternals.append("<td bgcolor=\"#FFFFFF\" align=\"center\">");
                        QMinternals.append((lstPrimes[i].getM() & (1 << (number_of_in_var - j - 1))) > 0 ? '-' : (lstPrimes[i].getV() & (1 << (number_of_in_var - j - 1))) > 0 ? '1' : '0');
                        QMinternals.append("</td>");
                    }
                    QMinternals.append("<td bgcolor=\"#FAF0E6\" align=\"center\">");
                    QMinternals.append(lstPrimes[i].isPrime() ? '*' : ' ');
                    QMinternals.append("</td></tr>");
                }
                QMinternals.append("</table>");

                //QMinternals.append(resourceBundle.getString("QMCIMPLICANTSCHART"));


                QMinternals.append("<br><table cellspacing=\"1\" cellpadding=\"5\" margin-bottom=\"20px\" margin-top=\"20px\" border=\"0\" border-collapse=\"collapse\" align=\"center\" font=\"#DEFAULT\" color=\"blue\" bgcolor=\"blue\"><tr><td bgcolor=\"#FAF0E6\" align=\"center\" colspan=\"2\">#</td>");
                for (int i = 0; i < number_of_in_var; i++) {
                    QMinternals.append("<td bgcolor=\"#FAF0E6\" align=\"center\">");
                    QMinternals.append(in_var_names[i]);
                    QMinternals.append("</td>");
                }
                for (int i = 0; i < lstOnesAux.size(); i++) {
                    QMinternals.append("<td bgcolor=\"#");
                    QMinternals.append(essentialCover[i] ? "CDD2E6" : "C4E0E0");
                    QMinternals.append("\" align=\"center\">");
                    QMinternals.append(lstOnesAux.get(i));
                    QMinternals.append("</td>");
                }
                QMinternals.append("<td bgcolor=\"#FAF0E6\"></td></tr>");

                for (int i = 0; i < lstPrimeEssentials.size(); i++) {
                    QMinternals.append("<tr><td bgcolor=\"#FAF0E6\"></td><td bgcolor=\"#FAF0E6\" align=\"right\">");
                    QMinternals.append(lstPrimeEssentials.get(i).toStringSimp());
                    QMinternals.append("</td>");

                    for (int j = 0; j < number_of_in_var; j++) {
                        QMinternals.append("<td bgcolor=\"#FFFFFF\" align=\"center\">");
                        QMinternals.append((lstPrimeEssentials.get(i).getM() & (1 << (number_of_in_var - j - 1))) > 0 ? '-' : (lstPrimeEssentials.get(i).getV() & (1 << (number_of_in_var - j - 1))) > 0 ? '1' : '0');
                        QMinternals.append("</td>");
                    }
                    for (int j = 0; j < lstOnesAux.size(); j++) {
                        QMinternals.append("<td bgcolor=\"#");
                        QMinternals.append(essentialCover[j] ? "CDD2E6" : "C4E0E0");
                        QMinternals.append("\" align=\"center\">");
                        if (lstPrimeEssentials.get(i).isTrue(lstOnesAux.get(j))) {
                            boolean cv = true;
                            for (int k = 0; k < lstPrimeEssentials.size(); k++) {
                                if (k != i) {
                                    if (lstPrimeEssentials.get(k).isTrue(lstOnesAux.get(j))) {
                                        cv = false;
                                        break;
                                    }
                                }
                            }
                            for (int k = 0; k < lstPrime.size(); k++) {
                                if (lstPrime.get(k).isTrue(lstOnesAux.get(j))) {
                                    cv = false;
                                    break;
                                }
                            }
                            QMinternals.append(cv ? "&#9679;" : "&#9675;");
                        }
                        QMinternals.append("</td>");
                    }

                    QMinternals.append("<td bgcolor=\"#FFFFFF\"align=\"left\"><font color=\"blue\">");
                    if (sum_of_products_not_product_of_sums)
                        QMinternals.append(lstPrimeEssentials.get(i).toExpressionProd());
                    else
                        QMinternals.append(lstPrimeEssentials.get(i).toExpressionSum());
                    QMinternals.append("</font></td>");
                }

                for (int i = 0; i < lstPrime.size(); i++) {
                    QMinternals.append("<tr><td bgcolor=\"#FAF0E6\">");
                    QMinternals.append((char) ('A' + i));
                    QMinternals.append("</td><td bgcolor=\"#FAF0E6\" align=\"right\">");
                    QMinternals.append(lstPrime.get(i).toStringSimp());
                    QMinternals.append("</td>");

                    for (int j = 0; j < number_of_in_var; j++) {
                        QMinternals.append("<td bgcolor=\"#FFFFFF\" align=\"center\">");
                        QMinternals.append((lstPrime.get(i).getM() & (1 << (number_of_in_var - j - 1))) > 0 ? '-' : (lstPrime.get(i).getV() & (1 << (number_of_in_var - j - 1))) > 0 ? '1' : '0');
                        QMinternals.append("</td>");
                    }
                    for (int j = 0; j < lstOnesAux.size(); j++) {
                        QMinternals.append("<td bgcolor=\"#");
                        QMinternals.append(essentialCover[j] ? "CDD2E6" : "C4E0E0");
                        QMinternals.append("\" align=\"center\">");
                        QMinternals.append(lstPrime.get(i).isTrue(lstOnesAux.get(j)) ? "&#9675;" : " ");
                        QMinternals.append("</td>");
                    }

                    QMinternals.append("<td bgcolor=\"#FFFFFF\" align=\"left\"><font color=\"green\">");
                    if (sum_of_products_not_product_of_sums)
                        QMinternals.append(lstPrime.get(i).toExpressionProd());
                    else
                        QMinternals.append(lstPrime.get(i).toExpressionSum());
                    QMinternals.append("</font></td>");
                }

                QMinternals.append("</table>");
            }


            // register solution (up to now)
            solutions.get(f).setEssentialPI(lstPrimeEssentials);
            // if there is any prime implicant left, Petrick's Method
            // is used to find the minimum solution(s)
            if ((lstPrime.size() > 0) && (lstOnes.size() > 0)) {
                if (lstPrime.size() > Long.SIZE)
                    throw new OutOfMemoryError();//resourceBundle.getString("NONESSENTIALPRIMES"));

                ArrayList<Long> M0 = new ArrayList<>(lstPrime.size());
                ArrayList<Long> M1 = new ArrayList<>(lstPrime.size());
                if (QuineMcCluskey) {
                    //QMinternals.append(resourceBundle.getString("QMPMPETRICKSM"));
                    boolean f1 = false;
                    for (int k = 0; k < lstOnes.size(); k++) {
                        grouped = false;
                        if (f1) {
                            QMinternals.append(" . ");
                        } else {
                            f1 = true;
                        }
                        for (int i = 0; i < lstPrime.size(); i++) {
                            if (lstPrime.get(i).isTrue(lstOnes.get(k))) {
                                if (grouped) {
                                    QMinternals.append(" + ");
                                } else {
                                    QMinternals.append('(');
                                    grouped = true;
                                }
                                QMinternals.append((char) ('A' + i));
                            }
                        }
                        if (grouped) {
                            QMinternals.append(')');
                        }
                    }
                }
                for (int i = 0; i < lstPrime.size(); i++) {
                    if (lstPrime.get(i).isTrue(lstOnes.get(0))) {
                        M0.add(1L << i);
                    }
                }
                for (int k = 1; k < lstOnes.size(); k++) {
                    M1.clear();
                    for (int i = 0; i < lstPrime.size(); i++) {
                        if (lstPrime.get(i).isTrue(lstOnes.get(k))) {
                            M1.add(1L << i);
                        }
                    }
                    M0 = mul(M0, M1);
                }
//                System.out.println("Expressões: " + M0.size());
                if (QuineMcCluskey) {
                    grouped = false;
                    //QMinternals.append(resourceBundle.getString("QMPMALLSOL"));
                    for (int j = 0; j < M0.size(); j++) {
                        if (grouped) {
                            QMinternals.append(" + ");
                        } else {
                            grouped = true;
                        }
                        boolean f1 = false;
                        for (int i = 0; i < lstPrime.size(); i++) {
                            if ((M0.get(j) & (1L << i)) != 0L) {
                                if (f1) {
                                    QMinternals.append('.');
                                } else {
                                    f1 = true;
                                }
                                QMinternals.append((char) ('A' + i));
                            }
                        }
                    }
                    //QMinternals.append(resourceBundle.getString("QMPMOPTIM"));
                }
                // compute weights according to optimization criterion
                M1.clear();
                long min = Long.MAX_VALUE;
                for (int j = 0; j < M0.size(); j++) {
                    long cr = 0L;
                    for (int i = 0; i < lstPrime.size(); i++) {
                        if ((M0.get(j) & (1L << i)) != 0L) {
                            // prime i belongs to this solution
                            cr += optimize_number_of_terms_not_variables
                                    ? (1L << Implicant.MAX_IN_VAR) + number_of_in_var - lstPrime.get(i).bitCount_m()
                                    : (((long) (number_of_in_var - lstPrime.get(i).bitCount_m())) << Implicant.MAX_IN_VAR) + 1L;
                        }
                    }
                    M1.add(cr);
                    if (cr < min) {
                        min = cr;
                    }
                }
                // remove solution worst than optimum
                for (int j = M0.size() - 1; j >= 0; j--) {
                    if (M1.get(j) > min) {
                        M0.remove(j);
                    }
                }
                if (QuineMcCluskey) {
                    grouped = false;
                    for (int j = 0; j < M0.size(); j++) {
                        if (grouped) {
                            QMinternals.append(" + ");
                        } else {
                            grouped = true;
                        }
                        boolean f1 = false;
                        for (int i = 0; i < lstPrime.size(); i++) {
                            if ((M0.get(j) & (1L << i)) != 0L) {
                                if (f1) {
                                    QMinternals.append('.');
                                } else {
                                    f1 = true;
                                }
                                QMinternals.append((char) ('A' + i));
                            }
                        }
                    }
                    QMinternals.append("<br><br>");
                }
                // save solution as a list of prime implicants
                if (all_possible_not_just_one) {
                    solutions.get(f).setPiSize(M0.size());
                    for (int j = 0; j < M0.size(); j++) {
                        solutions.get(f).addSolution(Long.bitCount(M0.get(j)));
                        for (int i = 0; i < lstPrime.size(); i++) {
                            if ((M0.get(j) & (1L << i)) != 0L) {
                                // prime i belongs to this solution
                                solutions.get(f).addPI(j, lstPrime.get(i));
                            }
                        }
                    }
                } else {
                    solutions.get(f).setPiSize(1);
                    solutions.get(f).addSolution(Long.bitCount(M0.get(0)));
                    for (int i = 0; i < lstPrime.size(); i++) {
                        if ((M0.get(0) & (1L << i)) != 0L) {
                            // prime i belongs to this solution
                            solutions.get(f).addPI(0, lstPrime.get(i));
                        }
                    }
                }
            }
            t3Time[f] = System.nanoTime() - startTime; // time test
        }
    }


    public ArrayList<Long> mul(ArrayList<Long> a, ArrayList<Long> b) {
        ArrayList<Long> v = new ArrayList<>(10);
        for (int i = 0; i < a.size(); i++) {
            for (int j = 0; j < b.size(); j++) {
                long z = a.get(i) | b.get(j);  // expand "and" over "or"
                if (!v.contains(z)) {
                    v.add(z);
                }
            }
        }

        for (int i = 0; i < v.size() - 1; i++) {
            for (int j = v.size() - 1; j > i; j--) {
                long z = v.get(i) & v.get(j);
                if (z == v.get(i)) {
                    v.remove(j);
                } else if (z == v.get(j)) {
                    v.set(i, z);
                    v.remove(j);
                    j = v.size();
                }
            }
        }
        return v;
    }

    /*
     * Generetes an expression: essential prime implicants in blue and
     * non-essentials in green. First eesential then non-essentials
     */
    void FormatExpressionHTML() {
        for (int f = 0; f < number_of_out_var; f++) {
            ArrayList<Implicant> epi = solutions.get(f).getEssentialsPI();
            if (epi == null) {
                //solution.append(Gui.resourceBundle.getString("SOLUTION TRIVIAL1"));
                solution.append(out_var_names[f]);
                //solution.append(Gui.resourceBundle.getString("SOLUTION TRIVIAL2"));
                continue;
            }
            if (sum_of_products_not_product_of_sums) {
                Collections.sort(epi);
            } else {
                Collections.sort(epi, (Implicant r1, Implicant r2) ->
                {
                    int r1b = r1.bitCount_m();
                    int r2b = r2.bitCount_m();
                    if (r1b != r2b)
                        return r2b - r1b;
                    r1b = r1.getM();
                    r2b = r2.getM();
                    if (r1b != r2b)
                        return r1b - r2b;
                    return r1.getV() - r2.getV();
                });
            }
            StringBuilder solutioneq = new StringBuilder();
            boolean fl = false;
            ArrayList<ArrayList<Implicant>> primeI = solutions.get(f).getPrimeI();
            solutioneq.append(out_var_names[f]);
            solutioneq.append(" = ");
            for (int j = 0; j < epi.size(); j++) {
                if (sum_of_products_not_product_of_sums) {
                    if (fl)
                        solutioneq.append(" + ");
                    solutioneq.append(epi.get(j).toExpressionProd());
                } else {
                    if (fl)
                        solutioneq.append(" . ");
                    if (((number_of_in_var - epi.get(j).bitCount_m()) > 1) &&
                            ((epi.size() > 1) || (primeI != null))) {
                        solutioneq.append('(');
                        solutioneq.append(epi.get(j).toExpressionSum());
                        solutioneq.append(')');
                    } else {
                        solutioneq.append(epi.get(j).toExpressionSum());
                    }
                }
                fl = true;
            }

            int mx;
            if (primeI == null) {
                mx = 1;
            } else {
                mx = primeI.size();
            }
            for (int i = 0; i < mx; i++) {
                solution.append(solutioneq);
                boolean fll = fl;
                if (primeI != null) {
                    if (sum_of_products_not_product_of_sums) {
                        Collections.sort(primeI.get(i));
                    } else {
                        Collections.sort(primeI.get(i), (Implicant r1, Implicant r2) ->
                        {
                            int r1b = r1.bitCount_m();
                            int r2b = r2.bitCount_m();
                            if (r1b != r2b)
                                return r2b - r1b;
                            r1b = r1.getM();
                            r2b = r2.getM();
                            if (r1b != r2b)
                                return r1b - r2b;
                            return r1.getV() - r2.getV();
                        });
                    }
                    solution.append("</font><font color=green>");
                    for (int j = 0; j < primeI.get(i).size(); j++) {
                        if (sum_of_products_not_product_of_sums) {
                            if (fll)
                                solution.append(" + ");
                            solution.append(primeI.get(i).get(j).toExpressionProd());
                        } else {
                            if (fll)
                                solution.append(" . ");
                            if (((number_of_in_var - primeI.get(i).get(j).bitCount_m()) > 1) &&
                                    ((fl) || (primeI.get(i).size() > 1))) {
                                solution.append('(');
                                solution.append(primeI.get(i).get(j).toExpressionSum());
                                solution.append(')');
                            } else {
                                solution.append(primeI.get(i).get(j).toExpressionSum());
                            }
                        }
                        fll = true;
                    }
                    solution.append("</font><font color=blue>");
                }
                solution.append("<br>");
            }
        }
    }

    /*
     * Generetes an expression sorted according to the number of terms, then
     * MSB variable, then complemented or not: essential prime implicants in
     * blue and non-essentials in green
     */
    void FormatExpressionHTMLSorted() {

        for (int f = 0; f < number_of_out_var; f++) {
            ArrayList<Implicant> epi = solutions.get(f).getEssentialsPI();
            if (epi == null) {
                solution.append(out_var_names[f]);
                if (perfectTruth)
                    solution.append(" = 1");
                else
                    solution.append("TRIVIAL SOLUTION");
                continue;
            }

            ArrayList<ArrayList<Implicant>> primeI = solutions.get(f).getPrimeI();
            int mx;
            if (primeI == null) {
                mx = 1;
            } else {
                mx = primeI.size();
            }
            for (int i = 0; i < mx; i++) {
                ArrayList<Implicant> im = (ArrayList<Implicant>) epi.clone();
                if (primeI != null) {
                    im.addAll(primeI.get(i));
                }
                if (sum_of_products_not_product_of_sums) {
                    Collections.sort(im);
                } else {
                    Collections.sort(im, (Implicant r1, Implicant r2) ->
                    {
                        int r1b = r1.bitCount_m();
                        int r2b = r2.bitCount_m();
                        if (r1b != r2b)
                            return r2b - r1b;
                        r1b = r1.getM();
                        r2b = r2.getM();
                        if (r1b != r2b)
                            return r1b - r2b;
                        return r1.getV() - r2.getV();
                    });
                }
                solution.append(out_var_names[f]);
                solution.append(" = ");
                boolean fl = false;
                boolean le = true;
                for (int j = 0; j < im.size(); j++) {
                    if (epi.contains(im.get(j))) {
                        if (!le) {
                            //solution.append("</font><font color=blue>");
                            le = true;
                        }
                    } else {
                        if (le) {
                            //solution.append("</font><font color=green>");
                            le = false;
                        }
                    }
                    if (sum_of_products_not_product_of_sums) {
                        if (fl)
                            solution.append(" + ");
                        solution.append(im.get(j).toExpressionProd());
                    } else {
                        if (fl)
                            solution.append(" \u0067 ");
                        if (((number_of_in_var - im.get(j).bitCount_m()) > 1) && (im.size() > 1)) {
                            solution.append('(');
                            solution.append(im.get(j).toExpressionSum());
                            solution.append(')');
                        } else {
                            solution.append(im.get(j).toExpressionSum());
                        }
                    }
                    fl = true;
                }
                /*if (!le) {
                    //solution.append("</font><font color=blue>");
                }*/
                solution.append("\n");
            }
        }
    }


    public String getTruthTableHTML() {
        StringBuilder temp = new StringBuilder();

        temp.append("<table cellspacing=\"1\" cellpadding=\"5\" margin-bottom=\"20px\" margin-top=\"20px\" border=\"0\" border-collapse=\"collapse\" align=\"center\" font color=\"blue\" bgcolor=\"blue\"><tbody><tr><th bgcolor=\"#FAF0E6\" align=\"center\">#</th>");

        for (int i = 0; i < number_of_in_var; i++) {
            temp.append("<th bgcolor=\"#FAF0E6\" align=\"center\">");
            temp.append(in_var_names[i]);
            temp.append("</th>");
        }

        for (int f = 0; f < number_of_out_var; f++) {
            ArrayList<ArrayList<Implicant>> primeI = solutions.get(f).getPrimeI();
            if (primeI == null) {
                temp.append("<th bgcolor=\"#FAF0E6\" align=\"center\">");
                // temp.append(out_var_names[f]); temp.append("</th>");
            } else {
                temp.append("<th bgcolor=\"#FAF0E6\" align=\"center\" colspan=\"");
                temp.append(primeI.size());
                temp.append("\">");
            }
            temp.append(out_var_names[f]);
            temp.append("</th>");
        }
        temp.append("</tr>");
        for (int x = 0; x < (1 << number_of_in_var); x++) {
            temp.append("<tr><td bgcolor=\"#FAF0E6\" align=\"right\">");
            temp.append(x);
            temp.append("</td>");
            for (int i = 0; i < number_of_in_var; i++) {
                temp.append("<td bgcolor=\"#FFFFFF\" align=\"center\">");
                if ((x & (1 << number_of_in_var - i - 1)) > 0)
                    temp.append('1');
                else
                    temp.append('0');
                temp.append("</td>");
            }
            for (int f = 0; f < number_of_out_var; f++) {
                ArrayList<Implicant> epi = solutions.get(f).getEssentialsPI();
                ArrayList<ArrayList<Implicant>> primeI = solutions.get(f).getPrimeI();
                if (epi == null) {
                    temp.append("<td>&nbsp;</td>");
                    continue;
                }

                int mx;
                if (primeI == null) {
                    mx = 1;
                } else {
                    mx = primeI.size();
                }
                for (int i = 0; i < mx; i++) {
                    ArrayList<Implicant> im = (ArrayList<Implicant>) epi.clone();
                    if (primeI != null) {
                        im.addAll(primeI.get(i));
                    }
                    boolean fl = false;
                    for (int j = 0; j < im.size(); j++) {
                        if (im.get(j).isTrue(x)) {
                            fl = true;
                            break;
                        }
                    }
                    if (!sum_of_products_not_product_of_sums) {
                        fl = !fl;
                    }
                    if (fl) {
                        if (values[x][f] == '1') {
                            temp.append("<td bgcolor=\"#FFFFFF\" align=\"center\">1</td>");
                        } else {
                            temp.append("<td bgcolor=\"#FAECF0\" align=\"center\"><font color=red>1</font></td>");
                        }
                    } else {
                        if (values[x][f] == '0') {
                            temp.append("<td bgcolor=\"#FFFFFF\" align=\"center\">0</td>");
                        } else {
                            temp.append("<td bgcolor=\"#FAECF0\" align=\"center\"><font color=red>0</font></td>");
                        }
                    }
                }
            }
            temp.append("</tr>");
        }
        temp.append("</tbody></table></font><br>");

        return temp.toString();
    }

    /*
     * Create Karnaugh Map using HTML table
     *
     * Converts the number of row and column to cell number in a Karnaugh Map.
     * Cell numbering correspond to latex package karnaugh.
     */
    public static int mat2bin(int i, int j) {
        int gc = (i >>> 1) ^ i; // Converting to Gray code
        int res = 0;
        int pos = 1;
        while (gc > 0) {
            res |= (gc & 1) << pos;
            gc >>>= 1;
            pos += 2;
        }
        pos = 0;
        gc = (j >>> 1) ^ j; // Converting to Gray code
        while (gc > 0) {
            res |= (gc & 1) << pos;
            gc >>>= 1;
            pos += 2;
        }
        return res;
    }

    /*
     * Blend the background color for cells covered by more them one prime
     * implicant
     */
    public static int blend(int color, int color2) {
        return ColorUtils.blendARGB(color, color2, 0.5f);
    }

    /*
     * Format a colour code in HTML
     */
    public static String formatHTML(int color) {
        return "#" + String.format("%08x", color);
    }

    public void FormatKarnaughHTML(int f, ArrayList<Implicant> im) {

        OO(im);

        solution.delete(0, solution.length());

        // Begin formatting output according to grouping color
        solution.append(out_var_names[f]);
        solution.append(" = ");
        boolean fl = false;
        for (int j = 0; j < im.size(); j++) {
            if (sum_of_products_not_product_of_sums) {
                if (fl)
                    solution.append(" + ");
                solution.append("<font color=\"");
                solution.append(formatHTML(K_BACKGROUND_COLOR[j % K_BACKGROUND_COLOR.length]));
                solution.append("\">");
                solution.append(im.get(j).toExpressionProd());
                solution.append("</font>");
            } else {
                if (fl)
                    solution.append(" . ");
                if (((number_of_in_var - im.get(j).bitCount_m()) > 1) && (im.size() > 1)) {
                    solution.append("<font color=\"");
                    solution.append(formatHTML(K_BACKGROUND_COLOR[j % K_BACKGROUND_COLOR.length]));
                    solution.append("\">(");
                    solution.append(im.get(j).toExpressionSum());
                    solution.append(")</font>");
                } else {
                    solution.append("<font color=\"");
                    solution.append(formatHTML(K_BACKGROUND_COLOR[j % K_BACKGROUND_COLOR.length]));
                    solution.append("\">");
                    solution.append(im.get(j).toExpressionSum());
                    solution.append("</font>");
                }
            }
            fl = true;
        }

        // Log.d("TAG", "FORMATTED:\t"+solution.toString());

    }

    void FormatKarnaughHTML() {
        for (int f = 0; f < number_of_out_var; f++) {
            ArrayList<Implicant> epi = solutions.get(f).getEssentialsPI();
            if (epi == null) {
                if (perfectTruth) {
                    solution.delete(0, solution.length()); // clear every thing first
                    solution.append(out_var_names[f]);
                    solution.append("<font color=\"");
                    solution.append(formatHTML(K_BACKGROUND_COLOR[2]));
                    solution.append("\">");
                    solution.append(" = 1 ");
                    solution.append("</font>");
                    handlePerfectOnes();
                } else
                    solution.append(out_var_names[f]).append("SOLUTION NON-TRIVIAL");
                continue;
            }
            ArrayList<ArrayList<Implicant>> primeI = solutions.get(f).getPrimeI();
            int mx;
            if (primeI == null) {
                mx = 1;
            } else {
                mx = primeI.size();
            }
            for (int i = 0; i < mx; i++) {
                ArrayList<Implicant> im = (ArrayList<Implicant>) epi.clone();
                if (primeI != null) {
                    im.addAll(primeI.get(i));
                }
                if (sum_of_products_not_product_of_sums) {
                    Collections.sort(im);
                } else {
                    Collections.sort(im, (Implicant r1, Implicant r2) ->
                    {
                        int r1b = r1.bitCount_m();
                        int r2b = r2.bitCount_m();
                        if (r1b != r2b)
                            return r2b - r1b;
                        r1b = r1.getM();
                        r2b = r2.getM();
                        if (r1b != r2b)
                            return r1b - r2b;
                        return r1.getV() - r2.getV();
                    });
                }
                Solver.this.FormatKarnaughHTML(f, im);
            }
        }
        solution.append("</font><br>");
    }

    void Format() {
        if (expression || expressionSorted) {
            Implicant.startExpression(number_of_in_var, in_var_names);
        }

        for (int i = 0; i < number_of_in_var; i++) {
            newKarnaughOrder[i] = number_of_in_var - 1 - KarnaughInOrder[number_of_in_var - 1 - i];
            //newKarnaughOrderInv[newKarnaughOrder[i]] = i;
        }

        if (expressionSorted) {
            FormatExpressionHTMLSorted();
        }

        FormatKarnaughHTML();
    }

    public ArrayList<Implicant> getSolutionImplicant() {
        return solutions.get(0).getEssentialsPI();
    }

    @Override
    public void run() {
        try {
            Solve();
            Format();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            Log.d("TAG", "ERROR");
        }

    }

    /*
     *getSolution return the solution.
     */
    public String getSolution() {
        return solution.toString();
    }

    private final int[] K_BACKGROUND_COLOR = new int[]{
            Color.argb(100, 56, 83, 164),
            Color.argb(100, 237, 31, 36),
            Color.argb(100, 245, 128, 32),
            Color.argb(100, 12, 129, 128),
            Color.argb(100, 125, 40, 126),
            Color.argb(100, 192, 129, 64),
            Color.argb(100, 236, 0, 140),
            Color.argb(100, 0, 174, 239),
            Color.argb(100, 150, 141, 0),
            Color.argb(100, 193, 31, 68),
            Color.argb(100, 105, 189, 69),
            Color.argb(100, 99, 100, 102)
    };

    private final Pair<ArrayList<Integer>,
            ArrayList<Integer>> BORD_COLORS = new Pair<>(new ArrayList<>(), new ArrayList<>());


    private void handlePerfectOnes() {
        int nvar = number_of_in_var >> 1;
        int nrows = 1 << nvar;
        int ncols = 1 << (number_of_in_var - nvar);

        for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
                int ed = reorderAdd(mat2bin(i, j));

                BORD_COLORS.first.add(ed);
                BORD_COLORS.second.add(K_BACKGROUND_COLOR[2]);
            }

        }
    }

    private void OO(ArrayList<Implicant> im) {
        int nvar = number_of_in_var >> 1;
        int nrows = 1 << nvar;
        int ncols = 1 << (number_of_in_var - nvar);

        for (int i = 0; i < nrows; i++) {

            for (int j = 0; j < ncols; j++) {
                int ed = reorderAdd(mat2bin(i, j));
                int bg = Color.TRANSPARENT;  //Color.argb(100, 255, 255, 255);
                for (int k = 0; k < im.size(); k++) {
                    if (im.get(k).isTrue(ed)) {
                        // avoid blending with transparent, colors lose alpha
                        if (bg == Color.TRANSPARENT)
                            bg = K_BACKGROUND_COLOR[k % K_BACKGROUND_COLOR.length];
                        else
                            bg = blend(bg, K_BACKGROUND_COLOR[k % K_BACKGROUND_COLOR.length]);
                    }
                }

                BORD_COLORS.first.add(ed);
                BORD_COLORS.second.add(bg);
                // Log.d("TAG", "["+i+", "+j+"] :: "+bg+"  ::  "+ed);
            }
        }
        // Log.d("TAG", "COLORS:\t"+BORD_COLORS.toString());
    }

    int[] newKarnaughOrder = new int[Implicant.MAX_IN_VAR];

    int reorderAdd(int ed) {
        int r = 0;
        for (int i = 0; (i < number_of_in_var) && (ed != 0); i++) {
            r |= (ed & 1) << newKarnaughOrder[i];
            ed >>>= 1;
        }
        return r;
    }

    public Pair<ArrayList<Integer>, ArrayList<Integer>> getBORD_COLORS() {
        return BORD_COLORS;
    }
}
