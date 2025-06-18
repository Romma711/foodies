package com.example.Foodies.Reserva;

import com.example.Foodies.Cliente.Cliente;
import com.example.Foodies.Cliente.ClienteRepository;
import com.example.Foodies.Restaurant.Restaurant;
import com.example.Foodies.Restaurant.RestaurantRepository;
import com.example.Foodies.Reserva.dtos.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public Reserva createReserva(Reserva reserva) {
        Cliente cliente = clienteRepo.findById(reserva.getId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Restaurant restaurante = restauranteRepo.findById(reserva.getId())
                .orElseThrow(() -> new RuntimeException("Restaurante no encontrado"));

        Reserva reservanueva = new Reserva(reserva.getCantidad(),  reserva.getEstadoReserva(), reserva.getCliente(), reserva.getRestaurant());
        reservanueva = reservaRepo.save(reservanueva);

        return new Reserva(
                reservanueva.getId(),
                reservanueva.getCantidad(),
                reservanueva.getHorariollegada(),
                reservanueva.getHorariofin(),
                reservanueva.getEstadoReserva(),
                cliente,
                restaurante
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
                r.getHorariollegada(),
                r.getHorariofin(),
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
                r.getHorariollegada(),
                r.getHorariofin(),
                r.getEstadoReserva(),
                r.getCliente().getNombre(),
                r.getRestaurant().getNombre()
        );
    }

    public void deleteReserva(Long id) {
        reservaRepo.deleteById(id);
    }
}
