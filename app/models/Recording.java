package models;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.CreatedTimestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import play.data.validation.Constraints;

import javax.persistence.*;
import javax.validation.Constraint;
import java.util.Date;
import java.util.List;

/**
 * Created by 609108084 on 04/04/2016.
 */

@Entity
@Table(name="recording")
public class Recording extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique=true, nullable=false)
    public int id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable=false)
    @CreatedTimestamp
    public Date created;

    @Lob
    @Column(nullable=false)
    public byte[] file;

    @Column(nullable=false)
    public int likes;

    @Column(name="play_count", nullable=false)
    @Constraints.MinLength(1)
    @Constraints.MaxLength(11)
    public int playCount;

    //bi-directional many-to-one association to Comment
    @OneToMany(mappedBy="recording1")
    @JsonManagedReference
    public List<Comment> comments;

    //bi-directional many-to-one association to RecordingStudio
    @ManyToOne
    @JoinColumn(name="recording_studio", nullable=false)
    @JsonBackReference
    public RecordingStudio recordingstudio;

    //bi-directional many-to-one association to User
    @ManyToOne
    @JoinColumn(name="user", nullable=false)
    @JsonBackReference
    public User user1;

}
