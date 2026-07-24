package com.taxsettle.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "family_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FamilyMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taxpayer_id", nullable = false)
    @JsonIgnore
    private Taxpayer taxpayer;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String idNumber;

    @Column(nullable = false)
    private String relationship;

    @Column
    private String remark;
}
