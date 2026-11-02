package com.example.Foodies.Moderacion;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Request cuyo cuerpo se puede leer de nuevo después de que un filtro de la Fase 3 lo inspeccionó,
 * así el controlador existente lo recibe intacto.
 */
final class CuerpoReleible extends HttpServletRequestWrapper {

    private final byte[] cuerpo;

    CuerpoReleible(HttpServletRequest request) throws IOException {
        super(request);
        this.cuerpo = request.getInputStream().readAllBytes();
    }

    /** @return el cuerpo leído una sola vez del request original */
    byte[] cuerpo() {
        return cuerpo;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream datos = new ByteArrayInputStream(cuerpo);
        return new ServletInputStream() {
            @Override
            public int read() {
                return datos.read();
            }

            @Override
            public boolean isFinished() {
                return datos.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException("Lectura asíncrona no soportada");
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
