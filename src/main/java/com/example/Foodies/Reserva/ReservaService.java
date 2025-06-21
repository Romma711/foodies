package com.example.Foodies.Reserva;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Cliente.ClienteRepository;
import com.example.Foodies.Exception.BusinessException;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Reserva.dtos.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    private ClienteRepository clienteRepo;

    @Autowired
    private RestaurantRepository restauranteRepo;

    @Transactional
    public ReservaDetailDTO createReserva(ReservaRequesDTO reserva) {
        Cliente cliente = clienteRepo.findById(reserva.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Restaurant restaurante = restauranteRepo.findById(reserva.getIdRestaurant())
                .orElseThrow(() -> new RuntimeException("Restaurante no encontrado"));
        //Esto verifica si el restaurant esta activo
        ReservaValidations.validateRestaurantIsActive(restaurante);

        //Esto formatea de string a respectivos tipos de datos de tiempo (tanto la fecha, como la hora)
        LocalTime horaParseada = LocalTime.parse(reserva.getHorarioLlegada());
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date fecha = null;
        try {
            fecha = formatter.parse(reserva.getFechaReserva());
        } catch (ParseException e) {
            throw new BusinessException("Fecha no valida, Tiene que ser yyyy-MM-dd. Ejemplo: 2022-01-01.");
        }
        //Aca valida si el cupo del dia no esta lleno
        ReservaValidations.validateCupo(
                reserva.getCantidad(),
                reservaRepo.findAllByFechaReserva(fecha)
                        .stream()
                        .mapToInt(Reserva::getCantidad)
                        .sum(),
                restaurante.getCupoMaximo());

        Reserva reservanueva = reservaRepo.save(new Reserva(reserva.getCantidad(), fecha, horaParseada, reserva.getEstadoReserva(), cliente, restaurante));

        return new ReservaDetailDTO(
                reservanueva.getId(),
                reservanueva.getCantidad(),
                reservanueva.getFechaReserva(),
                reservanueva.getHorariollegada(),
                reservanueva.getEstadoReserva(),
                reservanueva.getCliente().getNombre(),
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
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        return new ReservaDetailDTO(
                r.getId(),
                r.getCantidad(),
                r.getFechaReserva(),
                r.getHorariollegada(),
                r.getEstadoReserva(),
                r.getCliente().getNombre(),
                r.getRestaurant().getNombre()
        );
    }

    public ReservaDetailDTO updateReserva(Long id, ReservaPatchDTO dto) {
        Reserva r = reservaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (dto.getCantidad() != null) r.setCantidad(dto.getCantidad());
        if (dto.getEstadoReserva() != null) r.setEstadoReserva(dto.getEstadoReserva());

        r = reservaRepo.save(r);

        return new ReservaDetailDTO(
                r.getId(),
                r.getCantidad(),
                r.getFechaReserva(),
                r.getHorariollegada(),
                r.getEstadoReserva(),
                r.getCliente().getNombre(),
                r.getRestaurant().getNombre()
        );
    }

    public void deleteReserva(Long id) {
        reservaRepo.deleteById(id);
    }


}
