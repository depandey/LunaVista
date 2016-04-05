package models;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

/**
 * Created by 609108084 on 04/04/2016.
 */
@Entity
@Table(name="comment")
public class Comment extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique=true, nullable=false)
    public int id;

    @Column(nullable=false)
    public String message;

    //bi-directional many-to-one association to Recording
    @ManyToOne
    @JoinColumn(name="recording", nullable=false)
    @JsonBackReference
    public Recording recording1;

    //bi-directional many-to-one association to User
    @ManyToOne
    @JoinColumn(name="user", nullable=false)
    @JsonBackReference
    public User user;
}
