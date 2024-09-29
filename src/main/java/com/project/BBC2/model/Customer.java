package com.project.BBC2.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String customerId;

    private String name;
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;


    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime customer_createdAt;
}
