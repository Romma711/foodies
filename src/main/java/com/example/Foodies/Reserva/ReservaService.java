package com.example.Foodies.Reserva;

import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Enums.Role;
import com.example.Foodies.Exception.*;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Reserva.dtos.*;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.lang.Exception;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private RestaurantRepository restauranteRepo;

    public ReservaDetailDTO createReserva(ReservaRequesDTO reserva) {
        Usuario usuario = resolverUsuarioParaReserva(reserva.getIdUsuario());

        Restaurant restaurante = restauranteRepo.findById(reserva.getIdRestaurant())
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));

        //Esto verifica si el restaurant esta activo
        ReservaValidations.validateRestaurantIsActive(restaurante);

        ReservaValidations.validateTime(reserva.getHorarioLlegada());

        //Esto formatea de string a respectivos tipos de datos de tiempo (tanto la fecha, como la hora)
        LocalTime horaParseada = LocalTime.parse(reserva.getHorarioLlegada());
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        //Valida la fecha si es antes o despues de la fecha de hoy
        ReservaValidations.validateDate(reserva.getFechaReserva());

        Date fecha = null;
        try {
            fecha = formatter.parse(reserva.getFechaReserva());
        }catch (ParseException e) {
            throw new BusinessException("Fecha no valida, Tiene que ser yyyy-MM-dd. Ejemplo: 2022-01-01.");
        }catch (Exception e) {
            // Captura cualquier otra excepción relacionada con fechas
            throw new BusinessException("Error al procesar la fecha. Verifique el formato yyyy-MM-dd");
        }
        //Aca valida si el cupo del dia no esta lleno
        try{
            ReservaValidations.validateCupo(
                    reserva.getCantidad(),
                    reservaRepo.findAllByFechaReserva(fecha)
                            .stream()
                            .mapToInt(Reserva::getCantidad)
                            .sum(),
                    restaurante.getCupoMaximo());
        }catch (NotValidCupoException e){
                throw new NotValidCupoException(e.getMessage());
        }


        Reserva reservanueva = new Reserva();
        reservanueva.setCantidad(reserva.getCantidad());
        reservanueva.setFechaReserva(fecha);
        reservanueva.setHorariollegada(horaParseada);
        reservanueva.setEstadoReserva(EstadoReserva.PENDIENTE);
        reservanueva.setUsuario(usuario);
        reservanueva.setRestaurant(restaurante);
        reservanueva = reservaRepo.save(reservanueva);

        return new ReservaDetailDTO(
                reservanueva.getId(),
                reservanueva.getCantidad(),
                reservanueva.getFechaReserva(),
                reservanueva.getHorariollegada(),
                reservanueva.getEstadoReserva(),
                reservanueva.getUsuario().getNombre(),
                reservanueva.getRestaurant().getNombre()
        );
    }

    public List<ReservaListDTO> getAllReservas() {
        return reservaRepo.findAll().stream()
                .map(r -> new ReservaListDTO(
                        r.getId(),
                        r.getCantidad(),
                        r.getHorariollegada(),
                        r.getEstadoReserva()
                )).toList();
    }

    public ReservaDetailDTO getReservaById(Long id) {
        Reserva r = reservaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));

        verificarAccesoReserva(r);

        return new ReservaDetailDTO(
                r.getId(),
                r.getCantidad(),
                r.getFechaReserva(),
                r.getHorariollegada(),
                r.getEstadoReserva(),
                r.getUsuario().getNombre(),
                r.getRestaurant().getNombre()
        );
    }

    public ReservaDetailDTO updateReserva(Long id, ReservaPatchDTO dto) {
        Reserva r = reservaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));

        Usuario usuarioActual = getCurrentUser();

        if (dto.getCantidad() != null) {
            verificarAccesoReserva(r);
            validarCupoActualizado(r, dto.getCantidad());
            r.setCantidad(dto.getCantidad());
        }
        if (dto.getEstadoReserva() != null) {
            if (!puedeGestionarEstado(r, usuarioActual)) {
                throw new AccessDeniedException(
                        "Solo el encargado del restaurante o el admin pueden cambiar el estado de la reserva");
            }
            r.setEstadoReserva(dto.getEstadoReserva());
        }

        r = reservaRepo.save(r);

        return new ReservaDetailDTO(
                r.getId(),
                r.getCantidad(),
                r.getFechaReserva(),
                r.getHorariollegada(),
                r.getEstadoReserva(),
                r.getUsuario().getNombre(),
                r.getRestaurant().getNombre()
        );
    }

    public void deleteReserva(Long id) {
        Reserva reserva = reservaRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        verificarAccesoReserva(reserva);
        reservaRepo.deleteById(id);
    }

    public List<ReservaListDTO> getAllByRestaurant(Long id) {
        Restaurant restaurante = restauranteRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurante no encontrado"));
        verificarAccesoRestaurante(restaurante);

        List<ReservaListDTO> reserva = reservaRepo.findAllByRestaurant_Id(id).stream()
                .map(r -> new ReservaListDTO(
                        r.getId(),
                        r.getCantidad(),
                        r.getHorariollegada(),
                        r.getEstadoReserva()
                )).toList();
        if (reserva.isEmpty()) {
            throw new ListNoContentException("Este restaurante no tiene reservas");
        }
        return reserva;
    }

    public List<ReservaListDTO> getAllByUsuario(Long id) {
        Usuario usuarioActual = getCurrentUser();
        if (!isAdmin(usuarioActual) && !id.equals(usuarioActual.getId())) {
            throw new AccessDeniedException("No podés ver las reservas de otro usuario");
        }

        List<ReservaListDTO> resevas = reservaRepo.findAllByUsuario_Id(id).stream()
                .map(r -> new ReservaListDTO(
                        r.getId(),
                        r.getCantidad(),
                        r.getHorariollegada(),
                        r.getEstadoReserva()
                )).toList();
        if(resevas.isEmpty()){
            throw  new ListNoContentException("Este usuario no tiene reservas");
        }
        return resevas;
    }

    private Usuario resolverUsuarioParaReserva(Long idSolicitado) {
        Usuario usuarioActual = getCurrentUser();
        if (isAdmin(usuarioActual)) {
            return idSolicitado != null
                    ? usuarioRepo.findById(idSolicitado)
                            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"))
                    : usuarioActual;
        }
        if (idSolicitado != null && !idSolicitado.equals(usuarioActual.getId())) {
            throw new AccessDeniedException("No podés crear reservas para otro usuario");
        }
        return usuarioActual;
    }

    private void verificarAccesoReserva(Reserva r) {
        Usuario usuarioActual = getCurrentUser();
        if (!isAdmin(usuarioActual) && !r.getUsuario().getId().equals(usuarioActual.getId())) {
            throw new AccessDeniedException("No podés acceder a esta reserva");
        }
    }

    private void validarCupoActualizado(Reserva r, Integer nuevaCantidad) {
        if (nuevaCantidad < 0) {
            throw new NotValidCupoException("Cupo invalido");
        }
        int sinEsta = reservaRepo.findAllByFechaReserva(r.getFechaReserva()).stream()
                .filter(x -> !x.getId().equals(r.getId()))
                .mapToInt(Reserva::getCantidad)
                .sum();
        if (sinEsta + nuevaCantidad > r.getRestaurant().getCupoMaximo()) {
            throw new NotValidCupoException("El cupo maximo es: " + r.getRestaurant().getCupoMaximo());
        }
    }

    private boolean puedeGestionarEstado(Reserva r, Usuario usuario) {
        if (isAdmin(usuario)) {
            return true;
        }
        if (r.getRestaurant() == null || r.getRestaurant().getUsuario() == null) {
            return false;
        }
        return r.getRestaurant().getUsuario().getId().equals(usuario.getId());
    }

    private void verificarAccesoRestaurante(Restaurant restaurant) {
        Usuario usuarioActual = getCurrentUser();
        if (isAdmin(usuarioActual)) {
            return;
        }
        if (restaurant.getUsuario() == null || !restaurant.getUsuario().getId().equals(usuarioActual.getId())) {
            throw new AccessDeniedException("No podés acceder a este restaurante");
        }
    }

    private Usuario getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof String email) || email.isBlank()) {
            throw new AccessDeniedException("No autenticado");
        }
        return usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    }

    private boolean isAdmin(Usuario usuario) {
        return usuario.getRol() == Role.ROLE_ADMIN;
    }

}
