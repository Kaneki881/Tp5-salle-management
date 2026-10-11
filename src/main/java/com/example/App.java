package com.example;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.repository.SalleRepository;
import com.example.repository.SalleRepositoryImpl;
import com.example.service.SalleService;
import com.example.service.SalleServiceImpl;
import com.example.util.PaginationResult;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import java.time.LocalDateTime;
import java.util.List;

public class App {

    public static void main(String[] args) {
        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory(
                        "SallePU"
                );

        try {
            EntityManager em =
                    emf.createEntityManager();

            try {
                creerDonneesExemple(em);

                SalleRepository repository =
                        new SalleRepositoryImpl(em);

                SalleService service =
                        new SalleServiceImpl(repository);

                LocalDateTime debut =
                        LocalDateTime.of(
                                2026, 10, 11, 10, 0
                        );
                LocalDateTime fin =
                        LocalDateTime.of(
                                2026, 10, 11, 11, 0
                        );

                List<Salle> disponibles =
                        service.getAvailableRooms(
                                debut, fin
                        );

                System.out.println(
                        "Salles disponibles :"
                );

                for (Salle salle : disponibles) {
                    System.out.println(
                            "- " + salle.getNom()
                                    + " (capacité : "
                                    + salle.getCapacite() + ")"
                    );
                }

                PaginationResult<Salle> page =
                        service.searchRooms(
                                null,   // nom
                                10,     // capacité minimale
                                debut,
                                fin,
                                0,      // première page
                                1       // une salle par page
                        );

                System.out.println(
                        "Total filtré : "
                                + page.getTotalItems()
                );
                System.out.println(
                        "Nombre de pages : "
                                + page.getTotalPages()
                );

                for (Salle salle : page.getItems()) {
                    System.out.println(
                            "Page 0 : " + salle.getNom()
                    );
                }

            } finally {
                em.close();
            }
        } finally {
            emf.close();
        }
    }

    private static void creerDonneesExemple(
            EntityManager em
    ) {
        Salle alpha = new Salle();
        alpha.setNom("Alpha");
        alpha.setCapacite(20);

        Salle beta = new Salle();
        beta.setNom("Beta");
        beta.setCapacite(30);

        Salle gamma = new Salle();
        gamma.setNom("Gamma");
        gamma.setCapacite(8);

        Reservation reservationAlpha =
                new Reservation();
        reservationAlpha.setSalle(alpha);
        reservationAlpha.setDebut(
                LocalDateTime.of(
                        2026, 10, 11, 9, 0
                )
        );
        reservationAlpha.setFin(
                LocalDateTime.of(
                        2026, 10, 11, 10, 0
                )
        );

        Reservation reservationBeta =
                new Reservation();
        reservationBeta.setSalle(beta);
        reservationBeta.setDebut(
                LocalDateTime.of(
                        2026, 10, 11, 10, 0
                )
        );
        reservationBeta.setFin(
                LocalDateTime.of(
                        2026, 10, 11, 11, 0
                )
        );

        em.getTransaction().begin();

        try {
            em.persist(alpha);
            em.persist(beta);
            em.persist(gamma);

            em.persist(reservationAlpha);
            em.persist(reservationBeta);

            em.getTransaction().commit();

        } catch (RuntimeException exception) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw exception;
        }

        em.clear();
    }
}