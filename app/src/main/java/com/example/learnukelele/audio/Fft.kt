package com.example.learnukelele.audio

import kotlin.math.*

class Complex(val r: Double, val i: Double) {

    // Constructor:
    constructor():this(0.0, 0.0) { }

    // Add the argument to this, giving the result as a new complex number:
    operator fun plus(c: Complex):Complex
    {
        return Complex(r + c.r, i + c.i)
    }

    // Subtract the argument from this, giving the result as a new complex number:
    operator fun minus(c: Complex): Complex
    {
        return Complex(r - c.r, i - c.i)
    }

    // Multiply the argument with this, giving the result as a new complex number:
    operator fun times(c: Complex): Complex
    {
        return Complex(r*c.r - i*c.i, r*c.i + i*c.r)
    }

    // Multiply with an scalar, giving the reulst as a new complex number:
    operator fun times(a: Double): Complex
    {
        return Complex(a*r, a*i)
    }

    // Divide this by the argument, giving the result as a new complex number:
    operator fun div(a: Double): Complex
    {
        return Complex(r/a, i/a)
    }

}

// Complex exponential of an angle:
fun Cexp(a: Double): Complex
{
    return Complex(cos(a), sin(a))
}

fun directFT(x: Array<Complex>): Array<Complex>
{
    val N = x.size
    val X = Array<Complex>(N) { _ -> Complex() }       // Accumulate the results;

    val W = Cexp(-2*PI/N.toDouble())                   // Initialize twiddle factors;
    var Wk = Complex(1.0, 0.0)

    for (k in 0 until N) {
        var Wkn = Complex(1.0, 0.0)
        for (n in 0 until N) {
            X[k] = X[k] + Wkn * x[n]
            Wkn *= Wk                             // Update twiddle factor;
        }
        Wk *= W
    }
    return X                                           // Return value;
}

fun factor(n: Int): Int
{
    val rn = ceil(sqrt(n.toDouble())).toInt()      // Search up to the square root of the number;
    for (i in 2..rn) {
        if (n%i == 0) return i                     // If remainder is zero, a factor is found;
    }
    return n
}

fun recursiveFFT(x: Array<Complex>): Array<Complex>
{
    val N = x.size

    val N1 = factor(N)                                 // Smallest prime factor of length;
    if (N == N1) {                                     // If the length is prime itself,
        return directFT(x)                             //   transform is given by the direct form;
    } else {
        val N2 = N / N1                                // Decompose in two factors, N1 being prime;

        val X = Array<Complex>(N) { _ -> Complex() }   // Allocate memory for computation;

        var W = Cexp(-2*PI/N.toDouble())               // Twiddle factor;
        var Wj = Complex(1.0, 0.0)
        for (j in 0 until N1) {                           // Compute every subsequence of size N2;
            val xj = Array<Complex>(N2) { n -> x[n*N1+j] }     // Create the subsequence;
            val Xj = recursiveFFT(xj)                          // Compute the DFT of the subsequence;
            var Wkj = Complex(1.0, 0.0)
            for (k in 0 until N) {
                X[k] = X[k] + Xj[k%N2] * Wkj           // Recombine results;
                Wkj *= Wj                         // Update twiddle factors;
            }
            Wj *= W
        }
        return X
    }
}