package com.mistech.mistechmod.extraplanar;

/** Bloco que se liga a outros pelo mesmo código (senha). Código vazio = desligado. */
public interface CodedBlockEntity {
    String getCode();

    /** Só no servidor: troca o código (religa ao canal novo). */
    void setCode(String code);

    /** Tira espaços das pontas e limita o tamanho. */
    static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (char c : raw.strip().toCharArray()) {
            if (c >= ' ' && c != 127) {
                out.append(c);
            }
        }
        String text = out.toString().strip();
        return text.length() > ExtraplanarData.MAX_CODE_LENGTH ? text.substring(0, ExtraplanarData.MAX_CODE_LENGTH) : text;
    }
}
