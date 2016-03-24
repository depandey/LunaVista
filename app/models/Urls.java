package models;

import com.avaje.ebean.Ebean;
import models.utils.AppException;
import models.utils.Hash;
import play.data.format.Formats;
import play.data.validation.Constraints;
import com.avaje.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;
import java.util.List;

/**
 * User: Deepak
 * Date: 20/01/12
 */
@Entity
public class Urls extends Model {

    @Id
    public Long id;

    @Constraints.Required
    @Formats.NonEmpty
    @Column(unique = true)
    public String url;


    // -- Queries (long id, url.class)
    public static Model.Finder<Long, Urls> find = new Model.Finder<Long, Urls>(Long.class, Urls.class);

    /**
     * Retrieve a user from an email.
     *
     * @param url email to search
     * @return a user
     */
    public static Urls findByUrl(String url) {
        return find.where().eq("url", url).findUnique();
    }

    public static List<Urls> getAll(){
        return find.all();
    }
}
