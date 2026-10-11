package com.example.repository;

import com.example.model.Reservation;
import com.example.model.Salle;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SalleRepositoryImpl implements SalleRepository {

    private final EntityManager entityManager;

    public SalleRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Salle> findAvailableRooms(
            LocalDateTime debut,
            LocalDateTime fin
    ) {
        String jpql =
                "SELECT s FROM Salle s " +
                        "WHERE NOT EXISTS (" +
                        "  SELECT r.id FROM Reservation r " +
                        "  WHERE r.salle = s " +
                        "    AND r.debut < :fin " +
                        "    AND r.fin > :debut" +
                        ") " +
                        "ORDER BY s.nom, s.id";

        return entityManager
                .createQuery(jpql, Salle.class)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    @Override
    public List<Salle> searchRooms(
            String nom,
            Integer capaciteMin,
            LocalDateTime debut,
            LocalDateTime fin,
            int page,
            int size
    ) {
        CriteriaBuilder cb =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<Salle> query =
                cb.createQuery(Salle.class);

        Root<Salle> salle = query.from(Salle.class);

        query.select(salle)
                .where(filtres(
                        cb, query, salle,
                        nom, capaciteMin, debut, fin
                ))
                .orderBy(
                        cb.asc(salle.get("nom")),
                        cb.asc(salle.get("id"))
                );

        long decalage = (long) page * size;
        if (decalage > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Numéro de page trop élevé"
            );
        }

        return entityManager
                .createQuery(query)
                .setFirstResult((int) decalage)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public long countRooms(
            String nom,
            Integer capaciteMin,
            LocalDateTime debut,
            LocalDateTime fin
    ) {
        CriteriaBuilder cb =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> query =
                cb.createQuery(Long.class);

        Root<Salle> salle = query.from(Salle.class);

        query.select(cb.count(salle))
                .where(filtres(
                        cb, query, salle,
                        nom, capaciteMin, debut, fin
                ));

        return entityManager
                .createQuery(query)
                .getSingleResult();
    }

    private Predicate filtres(
            CriteriaBuilder cb,
            CommonAbstractCriteria query,
            Root<Salle> salle,
            String nom,
            Integer capaciteMin,
            LocalDateTime debut,
            LocalDateTime fin
    ) {
        List<Predicate> conditions =
                new ArrayList<>();

        if (nom != null && !nom.trim().isEmpty()) {
            conditions.add(
                    cb.like(
                            cb.lower(salle.<String>get("nom")),
                            "%" + nom.trim()
                                    .toLowerCase(Locale.ROOT) + "%"
                    )
            );
        }

        if (capaciteMin != null) {
            conditions.add(
                    cb.greaterThanOrEqualTo(
                            salle.<Integer>get("capacite"),
                            capaciteMin
                    )
            );
        }

        if (debut != null && fin != null) {
            Subquery<Long> reservationChevauchante =
                    query.subquery(Long.class);

            Root<Reservation> reservation =
                    reservationChevauchante.from(
                            Reservation.class
                    );

            reservationChevauchante
                    .select(reservation.<Long>get("id"))
                    .where(
                            cb.equal(
                                    reservation.get("salle"),
                                    salle
                            ),
                            cb.lessThan(
                                    reservation
                                            .<LocalDateTime>get("debut"),
                                    fin
                            ),
                            cb.greaterThan(
                                    reservation
                                            .<LocalDateTime>get("fin"),
                                    debut
                            )
                    );

            conditions.add(
                    cb.not(cb.exists(
                            reservationChevauchante
                    ))
            );
        }

        return cb.and(
                conditions.toArray(new Predicate[0])
        );
    }
}