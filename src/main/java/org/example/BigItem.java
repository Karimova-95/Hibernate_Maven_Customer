package org.example;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OptimisticLock;

import javax.persistence.*;

@Data
@Entity
@NoArgsConstructor
@Table(name = "big_items")
public class BigItem {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;

    @Column(name = "val")
    private int val;

    @Column(name = "junkField")
    @OptimisticLock(excluded = true)
    private int junkField;

    @Version
    private long version;

    public BigItem(int val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return "BigItem [ " +
                "id = " + id +
                ", val = " + val +
                ", version = " + version + " ]";
    }
}
