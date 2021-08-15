package org.example;


import lombok.Data;
import org.hibernate.annotations.Formula;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@Table(name = "manufacturers")
public class Manufacturer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @OneToMany(mappedBy = "manufacturer")
    private List<Product> products;

    @Formula("(SELECT avg(p.price) FROM products p WHERE p.manufacturer_id = id)")
    private BigDecimal avgProductsPrice;

    @Override
    public String toString() {
        return "Manufacturer: " + "title = " + title;
    }
}
