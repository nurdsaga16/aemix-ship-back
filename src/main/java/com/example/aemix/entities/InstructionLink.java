package com.example.aemix.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "instruction_links", uniqueConstraints = {
        @UniqueConstraint(columnNames = "link_key")
})
public class InstructionLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_key", nullable = false, unique = true)
    private String linkKey;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(nullable = false, length = 2048)
    private String link;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
