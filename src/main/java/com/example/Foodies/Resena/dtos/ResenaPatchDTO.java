package com.example.Foodies.Resena.dtos;

public class ResenaPatchDTO {
   private Long id;
    private String comentario;
    private Integer calificacion;

    public ResenaPatchDTO(Long id, String comentario, Integer calificacion) {
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

    public Integer getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Integer calificacion) {
        this.calificacion = calificacion;
    }
}
