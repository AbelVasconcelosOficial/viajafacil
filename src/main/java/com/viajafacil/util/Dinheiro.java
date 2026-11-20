package com.viajafacil.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class Dinheiro {
    private static final NumberFormat FORMATO = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    private Dinheiro() { }

    public static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public static String formatar(double valor) {
        return FORMATO.format(valor);
    }
}
