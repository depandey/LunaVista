package models;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import play.data.format.Formats;
import play.data.validation.Constraints;

import javax.annotation.Generated;
import javax.persistence.*;
import java.util.List;

/**
 * Created by 609108084 on 03/04/2016.
 */
@Entity
@Table(name = "recordingstudio")
public class RecordingStudio extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int id;

    @Constraints.Required
    @Formats.NonEmpty
    @Column(nullable = false,length = 50)
    @Enumerated(EnumType.STRING)
    public Genre genre;

    @Constraints.Required
    @Formats.NonEmpty
    @Column(nullable = false,length = 50)
    public String language;

    @Constraints.Required
    @Formats.NonEmpty
    @Column(nullable = false,length = 50)
    public String type;

    //bi-directional many-to-one association to Recording
    @OneToMany(mappedBy="recordingstudio")
    @JsonManagedReference
    public List<Recording> recordings;
}