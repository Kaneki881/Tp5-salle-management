package com.example.service;

import com.example.model.Salle;
import com.example.repository.SalleRepository;
import com.example.util.PaginationResult;

import java.time.LocalDateTime;
import java.util.List;

public class SalleServiceImpl implements SalleService {

    private final SalleRepository repository;

    public SalleServiceImpl(SalleRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Salle> getAvailableRooms(
            LocalDateTime debut,
            LocalDateTime fin
    ) {
        verifierCreneau(debut, fin);
        return repository.findAvailableRooms(
                debut, fin
        );
    }

    @Override
    public PaginationResult<Salle> searchRooms(
            String nom,
            Integer capaciteMin,
            LocalDateTime debut,
            LocalDateTime fin,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "La page doit être positive ou nulle"
            );
        }

        if (size < 1) {
            throw new IllegalArgumentException(
                    "La taille de page doit être positive"
            );
        }

        if (capaciteMin != null
                && capaciteMin < 0) {
            throw new IllegalArgumentException(
                    "La capacité minimale doit être positive"
            );
        }

        if ((debut == null) != (fin == null)) {
            throw new IllegalArgumentException(
                    "Indiquer les deux bornes du créneau"
            );
        }

        if (debut != null) {
            verifierCreneau(debut, fin);
        }

        long total = repository.countRooms(
                nom, capaciteMin, debut, fin
        );

        List<Salle> salles = repository.searchRooms(
                nom, capaciteMin, debut, fin,
                page, size
        );

        return new PaginationResult<>(
                salles, page, size, total
        );
    }

    private void verifierCreneau(
            LocalDateTime debut,
            LocalDateTime fin
    ) {
        if (debut == null
                || fin == null
                || !debut.isBefore(fin)) {
            throw new IllegalArgumentException(
                    "Le début doit précéder la fin"
            );
        }
    }
}