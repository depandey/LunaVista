package models;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.CreatedTimestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;
import play.data.format.Formats;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by 609108084 on 04/04/2016.
 */
@Entity
@Table(name = "user_relationship")
public class UserRelationship extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique=true, nullable=false)
    public int id;

    @CreatedTimestamp
    @Formats.DateTime(pattern = "dd-MM-yyyy")
    public Date created;

    //bi-directional many-to-one association to User
    @ManyToOne
    @JoinColumn(name="follower", nullable=false)
    @JsonBackReference
    public User follower;

    //bi-directional many-to-one association to User
    @ManyToOne
    @JoinColumn(name="following", nullable=false)
    @JsonBackReference
    public User following;
}
