package com.example.Foodies.Resena.dtos;

public class ResenaPatchDTO {
   private Long id;
    private String comentario;
    private int calificacion;

    public ResenaPatchDTO(Long id, String comentario, int calificacion) {
        this.id = id;
        this.comentario = comentario;
        this.calificacion = calificacion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public int getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(int calificacion) {
        this.calificacion = calificacion;
    }
}
