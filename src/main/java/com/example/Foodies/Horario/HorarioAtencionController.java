package com.example.Foodies.Horario;

import com.example.Foodies.Config.CurrentUser;
import com.example.Foodies.Exception.EntityNotFoundException;
import com.example.Foodies.Horario.Dtos.FranjaHorariaDTO;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Horario de atención: el encargado lo edita desde el panel y el público lo consulta por restaurante. */
@RestController
@RequestMapping("/api/restaurantes")
public class HorarioAtencionController {

    private final HorarioAtencionService horarioService;
    private final RestaurantRepository restaurantRepo;
    private final CurrentUser currentUser;

    public HorarioAtencionController(HorarioAtencionService horarioService, RestaurantRepository restaurantRepo,
                                     CurrentUser currentUser) {
        this.horarioService = horarioService;
        this.restaurantRepo = restaurantRepo;
        this.currentUser = currentUser;
    }

    @GetMapping("/me/horarios")
    public List<FranjaHorariaDTO> getMisHorarios() {
        return horarioService.listar(miRestaurante());
    }

    /** Reemplaza la semana completa; una lista vacía quita el horario. */
    @PutMapping("/me/horarios")
    public List<FranjaHorariaDTO> guardarMisHorarios(@RequestBody List<FranjaHorariaDTO> franjas) {
        return horarioService.reemplazar(miRestaurante(), franjas);
    }

    @GetMapping("/{id}/horarios")
    public List<FranjaHorariaDTO> getHorarios(@PathVariable Long id) {
        return horarioService.listarPublico(id);
    }

    private Restaurant miRestaurante() {
        return restaurantRepo.findByUsuario_Email(currentUser.email())
                .orElseThrow(() -> new EntityNotFoundException("No tenés un restaurante asociado"));
    }
}
