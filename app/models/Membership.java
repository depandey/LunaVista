package models;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.List;

/**
 * The persistent class for the membership database table.
 *
 */
@Entity
@Table(name="membership")
public class Membership extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique=true, nullable=false)
    public int id;

    @Column(nullable = false)
    public double price;

    @Column(nullable = false, length = 50)
    public String type;


    //bi-directional many-to-one association to User
    @OneToOne(mappedBy = "membership")
    @JsonBackReference
    public User users;
}
