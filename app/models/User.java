package models;

import com.avaje.ebean.annotation.CreatedTimestamp;
import com.avaje.ebean.annotation.UpdatedTimestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import models.utils.AppException;
import models.utils.Hash;
import play.data.format.Formats;
import play.data.validation.Constraints;
import com.avaje.ebean.Model;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * User: yesnault
 * Date: 20/01/12
 */
@Entity
public class User extends Model {

    @Id
    public Long id;

    @Constraints.Required
    @Formats.NonEmpty
    @Column(unique = true)
    public String email;

    @Column(length = 100, nullable = true)
    @Constraints.Required
    @Constraints.MinLength(2)
    @Constraints.MaxLength(100)
    public String firstname;

    @Column(length = 100, nullable = true)
    @Constraints.Required
    @Constraints.MinLength(2)
    @Constraints.MaxLength(100)
    public String lastname;

    @Column(length = 100, nullable = true)
    @Constraints.Required
    @Constraints.MinLength(2)
    @Constraints.MaxLength(100)
    public String country;


    @Constraints.Required
    @Formats.NonEmpty
    @Column(unique = true)
    public String username;

    public String confirmationtoken;

    @Constraints.Required
    @Formats.NonEmpty
    public String passwordhash;

    @Formats.DateTime(pattern = "dd-MM-yyyy")
    @CreatedTimestamp
    @Column(name = "created")
    public Date dateCreation;

    @Formats.NonEmpty
    public Boolean validated = false;

    @Formats.NonEmpty
    public Boolean admin = false;

    public String auth_key;


    @Column(name="about_me", length=255)
    public String aboutMe;

    @Temporal(TemporalType.DATE)
    public Date birthday;

    @Column(length=100)
    public String city;

    @Column(length=1)
    public String gender;

    @Column(length=100)
    public String name;

    @Lob
    @Column(name="profile_picture", nullable=false)
    public byte[] profilePicture;

    @Temporal(TemporalType.TIMESTAMP)
    @UpdatedTimestamp
    public Date updated;


    //bi-directional many-to-one association to Recording
    @OneToMany(mappedBy="user1")
    @JsonManagedReference
    public List<Recording> recordings;

    //bi-directional many-to-one association to Comment
    @OneToMany(mappedBy="user")
    @JsonManagedReference
    public List<Comment> comments;

    //bi-directional many-to-one association to Membership
    @OneToOne
    @JsonManagedReference
    @JoinColumn(name="membership", nullable=false)
    public Membership membership;

    //bi-directional many-to-one association to UserRelationship
    @OneToMany(mappedBy="follower")
    @JsonManagedReference
    public List<UserRelationship> follower;

    //bi-directional many-to-one association to UserRelationship
    @OneToMany(mappedBy="following")
    @JsonManagedReference
    public List<UserRelationship> following;

    public void deleteAuth_key() {
        this.auth_key = null;
        save();
    }

    // -- Queries (long id, user.class)
    public static Model.Finder<Long, User> find = new Model.Finder<Long, User>(Long.class, User.class);


    public static User findById(Long id){
        return find.byId(id);
    }

    /**
     * Retrieve a user from an email.
     *
     * @param email email to search
     * @return a user
     */
    public static User findByEmail(String email) {
        return find.where().eq("email", email).findUnique();
    }

    /**
     * Retrieve a user from a username.
     *
     * @param userName Full name
     * @return a user
     */
    public static User findByUserName(String userName) {
        return find.where().eq("username", userName).findUnique();
    }

    /**
     * Retrieves a user from a confirmation token.
     *
     * @param token the confirmation token to use.
     * @return a user if the confirmation token is found, null otherwise.
     */
    public static User findByConfirmationToken(String token) {
        return find.where().eq("confirmationtoken", token).findUnique();
    }

    /**
     * Authenticate a User, from a email and clear password.
     *
     * @param email         email
     * @param clearPassword clear password
     * @return User if authenticated, null otherwise
     * @throws AppException App Exception
     */
    public static User authenticate(String email, String clearPassword) throws AppException {

        // get the user with email only to keep the salt password
        User user = find.where().eq("email", email).findUnique();
        if (user != null) {
            // get the hash password from the salt + clear password
            if (Hash.checkPassword(clearPassword, user.passwordhash)) {
                return user;
            }
        }
        else{
            user = find.where().eq("username", email).findUnique();
            if (user != null) {
                // get the hash password from the salt + clear password
                if (Hash.checkPassword(clearPassword, user.passwordhash)) {
                    return user;
                }
            }
        }
        return null;
    }

    public void changePassword(String password) throws AppException {
        this.passwordhash = Hash.createPassword(password);
        this.save();
    }

    /**
     * Confirms an account.
     *
     * @return true if confirmed, false otherwise.
     * @throws AppException App Exception
     */
    public static boolean confirm(User user) throws AppException {
        if (user == null) {
            return false;
        }

        user.confirmationtoken = null;
        user.validated = true;
        user.save();
        return true;
    }

    public static User findByAuthKey(String auth_key) {
        if (auth_key == null) {
            return null;
        }

        try  {
            return find.where().eq("auth_key", auth_key).findUnique();
        }
        catch (Exception e) {
            return null;
        }
    }
    public static class SignUpUserResponseBuilder {
        private String email;
        private String country;
        private String username;
        private String createdon;

        public String getEmail() {
            return email;
        }

        private String auth_key;

        private String getDate(Date date) {
            SimpleDateFormat formatter;
            formatter = new SimpleDateFormat("dd-MM-yyyy");
            return formatter.format(date);
        }
        public SignUpUserResponseBuilder(User user){
            this.email = user.email;
            this.country = user.country;
            this.auth_key = user.auth_key;
            this.createdon = getDate(user.dateCreation);
            this.username = user.username;
        }
    }
}
