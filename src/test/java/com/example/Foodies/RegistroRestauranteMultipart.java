package com.example.Foodies;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Arma el `multipart/form-data` de `POST /api/auth/register/restaurante`: los datos en JSON y las fotos. */
final class RegistroRestauranteMultipart {

    /** Alcanza con la firma: el backend valida el contenido por sus primeros bytes. */
    static final byte[] JPEG = concat(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, "jpeg");
    static final byte[] PNG = concat(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, "png");
    static final byte[] WEBP = concat("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII), "webp");

    private RegistroRestauranteMultipart() {
    }

    /** Registro válido: 1 foto del frente y `salones` fotos del salón. */
    static HttpEntity<MultiValueMap<String, Object>> conFotos(Map<String, Object> datos, int salones) {
        MultiValueMap<String, Object> body = soloDatos(datos);
        body.add("frente", archivo("frente.jpg", JPEG));
        for (int i = 0; i < salones; i++) {
            body.add("salones", archivo("salon-" + i + ".png", PNG));
        }
        return entity(body);
    }

    static MultiValueMap<String, Object> soloDatos(Map<String, Object> datos) {
        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("datos", new HttpEntity<>(datos, json));
        return body;
    }

    static HttpEntity<MultiValueMap<String, Object>> entity(MultiValueMap<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(body, headers);
    }

    static ByteArrayResource archivo(String nombre, byte[] contenido) {
        return new ByteArrayResource(contenido) {
            @Override
            public String getFilename() {
                return nombre;
            }
        };
    }

    private static byte[] concat(byte[] firma, String resto) {
        byte[] extra = resto.getBytes(StandardCharsets.US_ASCII);
        byte[] out = new byte[firma.length + extra.length];
        System.arraycopy(firma, 0, out, 0, firma.length);
        System.arraycopy(extra, 0, out, firma.length, extra.length);
        return out;
    }
}
