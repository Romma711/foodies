package com.example.Foodies.Reserva;

import com.example.Foodies.CupoEspecial.CupoEspecialService;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Exception.*;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Reserva.dtos.*;
import com.example.Foodies.Usuario.Usuario;
import com.example.Foodies.Usuario.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.Exception;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private RestaurantRepository restauranteRepo;
    @Autowired
    private CupoEspecialService cupoEspecialService;

    public ReservaDetailDTO createReserva(ReservaRequesDTO reserva) {
        Usuario usuario = usuarioRepo.findById(reserva.getIdUsuario())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

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
        //Aca valida si el cupo del dia no esta lleno, contando solo las reservas no canceladas de este restaurante
        //El cupo del dia es el cupo especial si el restaurante cargo uno, si no su capacidad normal
        int cupoDelDia = cupoEspecialService.cupoDelDia(restaurante, LocalDate.parse(reserva.getFechaReserva()));
        List<Reserva> reservasDelDia = reservaRepo.findAllByRestaurant_IdAndFechaReserva(restaurante.getId(), fecha);
        ReservaValidations.validateCupo(
                reserva.getCantidad(),
                personas(reservasDelDia, estado -> estado != EstadoReserva.CANCELADA),
                cupoDelDia);

        Reserva reservanueva = new Reserva();
        reservanueva.setCantidad(reserva.getCantidad());
        reservanueva.setFechaReserva(fecha);
        reservanueva.setHorariollegada(horaParseada);
        reservanueva.setEstadoReserva(estadoInicial(
                restaurante,
                cupoDelDia,
                reserva.getCantidad(),
                personas(reservasDelDia, estado -> estado == EstadoReserva.ACEPTADA)));
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

    private static int personas(List<Reserva> reservas, Predicate<EstadoReserva> estado) {
        return reservas.stream()
                .filter(r -> estado.test(r.getEstadoReserva()))
                .mapToInt(Reserva::getCantidad)
                .sum();
    }

    /**
     * Se acepta sola si el restaurante tiene auto-aceptación y, sumando esta reserva, las personas aceptadas del día
     * no superan el porcentaje elegido del cupo del día. La reserva que cruza el límite queda pendiente.
     */
    private static EstadoReserva estadoInicial(Restaurant restaurante, int cupoDelDia, int cantidad,
                                               int aceptadasDelDia) {
        Integer porcentaje = restaurante.getPorcentajeAutoAceptacion();
        if (porcentaje == null || porcentaje <= 0) {
            return EstadoReserva.PENDIENTE;
        }
        int limite = cupoDelDia * porcentaje / 100;
        return aceptadasDelDia + cantidad <= limite ? EstadoReserva.ACEPTADA : EstadoReserva.PENDIENTE;
    }

    public List<ReservaListDTO> getAllReservas() {
        return reservaRepo.findAll().stream()
                .map(r -> new ReservaListDTO(
                        r.getId(),
                        r.getCantidad(),
                        r.getFechaReserva(),
                        r.getHorariollegada(),
                        r.getEstadoReserva(),
                        r.getRestaurant().getNombre()
                )).toList();
    }

    public ReservaDetailDTO getReservaById(Long id) {
        Reserva r = reservaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));

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

        if (dto.getCantidad() != null) r.setCantidad(dto.getCantidad());
        if (dto.getEstadoReserva() != null) r.setEstadoReserva(dto.getEstadoReserva());

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
        reservaRepo.deleteById(id);
    }

    public List<ReservaListDTO> getAllByRestaurant(Long id) {
        List<ReservaListDTO> reserva = reservaRepo.findAllByRestaurant_Id(id).stream()
                .map(r -> new ReservaListDTO(
                        r.getId(),
                        r.getCantidad(),
                        r.getFechaReserva(),
                        r.getHorariollegada(),
                        r.getEstadoReserva(),
                        r.getRestaurant().getNombre()
                )).toList();
        if (reserva.isEmpty()) {
            throw new ListNoContentException("Este restaurante no tiene reservas");
        }
        return reserva;
    }

    public List<ReservaListDTO> getAllByUsuario(Long id) {
        List<ReservaListDTO> resevas = reservaRepo.findAllByUsuario_Id(id).stream()
                .map(r -> new ReservaListDTO(
                        r.getId(),
                        r.getCantidad(),
                        r.getFechaReserva(),
                        r.getHorariollegada(),
                        r.getEstadoReserva(),
                        r.getRestaurant().getNombre()
                )).toList();
        if(resevas.isEmpty()){
            throw  new ListNoContentException("Este usuario no tiene reservas");
        }
        return resevas;
    }

}
