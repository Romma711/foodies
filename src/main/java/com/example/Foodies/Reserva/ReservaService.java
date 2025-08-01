package com.example.Foodies.Reserva;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Cliente.ClienteRepository;
import com.example.Foodies.Enums.EstadoReserva;
import com.example.Foodies.Exception.*;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Reserva.dtos.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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
    private ClienteRepository clienteRepo;

    @Autowired
    private RestaurantRepository restauranteRepo;

    public ReservaDetailDTO createReserva(ReservaRequesDTO reserva) {
        Cliente cliente = clienteRepo.findById(reserva.getIdCliente())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

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


        Reserva reservanueva = reservaRepo.save(new Reserva(reserva.getCantidad(), fecha, horaParseada, EstadoReserva.PENDIENTE, cliente, restaurante));

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
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));

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
                r.getCliente().getNombre(),
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
                        r.getHorariollegada(),
                        r.getEstadoReserva()
                )).toList();
        if (reserva.isEmpty()) {
            throw new ListNoContentException("Este restaurante no tiene reservas");
        }
        return reserva;
    }

    public List<ReservaListDTO> getAllByCliente(Long id) {
        List<ReservaListDTO> resevas = reservaRepo.findAllByCliente_Id(id).stream()
                .map(r -> new ReservaListDTO(
                        r.getId(),
                        r.getCantidad(),
                        r.getHorariollegada(),
                        r.getEstadoReserva()
                )).toList();
        if(resevas.isEmpty()){
            throw  new ListNoContentException("Este cliente no tiene reservas");
        }
        return resevas;
    }

}
